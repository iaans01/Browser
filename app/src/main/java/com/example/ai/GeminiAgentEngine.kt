package com.example.ai

import android.util.Log
import com.example.browser.CommandIntentResolver
import com.example.model.AgentActionDecision
import com.example.model.AgentActionType
import com.example.model.AgentStepLog
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.model.CommandTaskPlan
import com.example.model.PromptSuggestion
import com.example.model.ScreenSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiAgentEngine"

data class HybridStepDecision(
    val decision: AgentActionDecision,
    val engineSource: AiEngineSource,
    val fallbackReason: String? = null
)

data class SummaryChunk(
    val text: String,
    val engineSource: AiEngineSource,
    val fallbackReason: String? = null
)

class GeminiAgentEngine(
    private val localEngine: LocalLiteLanguageEngine = LocalLiteLanguageEngine()
) {
    private val decisionSchema = Schema.obj(
        properties = mapOf(
            "thought" to Schema.string(
                description = "Brief reasoning for current observation and next action"
            ),
            "phase" to Schema.enumeration(
                values = listOf("observe", "think", "act", "recover", "waiting", "finish", "fail"),
                description = "Current loop phase"
            ),
            "action" to Schema.enumeration(
                values = listOf("read_screen", "click", "type", "scroll", "await_user", "finish", "fail"),
                description = "Action to execute on WebView"
            ),
            "target_agent_id" to Schema.string(
                description = "Exact agent_id (e.g. el_3) from read_screen elements for click/type, else empty"
            ),
            "input_text" to Schema.string(
                description = "Text to type when action is type, else empty"
            ),
            "submit" to Schema.boolean(
                description = "True only when typing in a standalone search bar to submit immediately"
            ),
            "scroll_direction" to Schema.enumeration(
                values = listOf("down", "up", "top", "bottom"),
                description = "Scroll direction when action is scroll"
            ),
            "recovery_analysis" to Schema.string(
                description = "Root cause and plan adjustment if previous step failed, else empty"
            ),
            "final_summary" to Schema.string(
                description = "Final summary when action is finish, fail, or instruction prompt when action is await_user"
            )
        )
    )

    private val systemPrompt = content {
        text(
            """
            أنت وكيل تصفح لأندرويد تنفذ أهداف المستخدم عبر: 1. الملاحظة (read_screen) 2. التفكير 3. التنفيذ (click/type/scroll) أو طلب تدخل المستخدم (await_user).
            إذا فشل الإجراء، حلل السبب في recovery_analysis وعدل خطتك تلقائياً. المخرجات JSON فقط.
            Rules:
            - Use decomposed_plan (current_substep) and extracted_entities (email, auth_intent: sign_up vs log_in) precisely.
            - Translate user intent across Arabic and English UI controls:
              * sign_up ("تسجيل جديد" / "إنشاء حساب") = Sign up / Register / Create account, NEVER Sign in / Log in.
              * log_in ("تسجيل الدخول") = Sign in / Log in / Login, NEVER Sign up / Register.
            - When a page has BOTH Login and Sign Up (auth_form_hint="dual_forms" or tabbed), inspect auth_role, container_id, container_heading, container_signature, html_id, name, css_classes, selector, html_tag, and aria_selected on every element:
              * Real-world HTML markers are authoritative even when visible headings/text are generic or ambiguous: e.g. container_signature or css_classes containing "woocommerce-form-register", "register", "signup", "create-account", hidden inputs like "woocommerce-register-nonce", html_id like "reg_email" / "reg_password", or button name="register" / class="woocommerce-form-register__submit" ALWAYS belong to sign_up. Conversely, "woocommerce-form-login", "login-nonce", "user_login", name="login" ALWAYS belong to log_in.
              * ONLY type into or click elements whose auth_role, container_signature, html_id, name, css_classes, and container_id match auth_intent. NEVER type an email into a "login" form when the user requested "sign_up" (or vice versa).
              * For tabbed UIs, if the target tab has aria_selected=false (or auth_form_hint is "tabbed_login_active"/"tabbed_signup_active" opposite to auth_intent), click the matching tab first before typing or submitting.
            - Avoid OAuth social login buttons (Google, Apple, Facebook, GitHub, Microsoft) and "Forgot password" links unless the user explicitly requested them.
            - Use only exact agent_id values from interactive_elements. Never click disabled elements.
            - For registration/login forms, type the exact extracted email with submit=false, accept terms checkboxes if signing up, then click Continue/Next/Submit inside the SAME container_id.
            - If a password field inside the matching container (without user-supplied password), OTP/verification code, or CAPTCHA blocks progress, emit phase="waiting", action="await_user", and put a clear instruction in final_summary telling the user what to complete on the page.
            - Failure & Blocker Recognition:
              * Inspect screen.page_errors, screen.invalid_fields, screen.network_error_summary, and loop_warning BEFORE acting.
              * If page_errors or invalid_fields reflect a recoverable UI oversight (e.g., an unchecked terms box), explain it in recovery_analysis and fix it once.
              * If page_errors report a hard blocker (e.g. email already registered, missing required fields not in user prompt), or network_error_summary / loop_warning shows the page or action is stuck after retries, emit action="fail" (or action="await_user" if user input on-page resolves it) with the exact quoted reason in final_summary.
            - Choose action="finish" with final_summary ONLY when the goal is visibly achieved — NEVER emit finish when blocked or unable to locate requested targets.
            """.trimIndent()
        )
    }

    private val primaryModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = decisionSchema
            temperature = 0.2f
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.LOW
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
            systemInstruction = systemPrompt,
            requestOptions = RequestOptions(timeoutInMillis = 20_000L)
        )
    }

    private val fastFallbackModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = decisionSchema
            temperature = 0.2f
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = config,
            systemInstruction = systemPrompt,
            requestOptions = RequestOptions(timeoutInMillis = 18_000L)
        )
    }

    private val summaryModel by lazy {
        val config = generationConfig {
            temperature = 0.35f
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.LOW
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
            requestOptions = RequestOptions(timeoutInMillis = 25_000L)
        )
    }

    suspend fun decideNextStep(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        allowCloudGemini: Boolean = true,
        taskPlan: CommandTaskPlan? = null
    ): HybridStepDecision = withContext(Dispatchers.IO) {
        if (!allowCloudGemini) {
            return@withContext localOnlyDecision(goal, stepNumber, snapshot, history, language, taskPlan)
        }
        val prompt = buildStepPrompt(goal, stepNumber, snapshot, history, language, taskPlan)
        decideWithCloudAttempt(
            goal = goal,
            stepNumber = stepNumber,
            snapshot = snapshot,
            history = history,
            language = language,
            allowCloudGemini = allowCloudGemini,
            taskPlan = taskPlan
        ) {
            val jsonText = try {
                withTimeout(18_000L) {
                    streamJsonResponse(primaryModel, prompt)
                }
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Primary model timed out on step $stepNumber, trying gemini-3.5-flash-lite", timeout)
                withTimeout(15_000L) {
                    streamJsonResponse(fastFallbackModel, prompt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Primary model error on step $stepNumber (${e.message}), trying gemini-3.5-flash-lite", e)
                withTimeout(15_000L) {
                    streamJsonResponse(fastFallbackModel, prompt)
                }
            }

            if (jsonText.isBlank()) {
                throw IllegalStateException("Gemini returned an empty JSON response.")
            }

            val json = JSONObject(jsonText)
            val prettyJson = json.toString(2)

            HybridStepDecision(
                decision = AgentActionDecision(
                    stepNumber = stepNumber,
                    phase = json.optString("phase", "act"),
                    thought = json.optString("thought"),
                    action = AgentActionType.fromWire(json.optString("action")),
                    targetAgentId = json.optString("target_agent_id"),
                    inputText = json.optString("input_text"),
                    submit = json.optBoolean("submit", false),
                    scrollDirection = json.optString("scroll_direction", "down"),
                    recoveryAnalysis = json.optString("recovery_analysis"),
                    finalSummary = json.optString("final_summary"),
                    rawJson = prettyJson
                ),
                engineSource = AiEngineSource.CLOUD_GEMINI
            )
        }
    }

    internal suspend fun decideWithCloudAttempt(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        allowCloudGemini: Boolean = true,
        taskPlan: CommandTaskPlan? = null,
        cloudAttempt: suspend () -> HybridStepDecision
    ): HybridStepDecision {
        if (!allowCloudGemini) {
            return localOnlyDecision(goal, stepNumber, snapshot, history, language, taskPlan)
        }
        return try {
            cloudAttempt()
        } catch (timeout: TimeoutCancellationException) {
            // IMPORTANT: TimeoutCancellationException extends CancellationException; check it BEFORE CancellationException!
            currentCoroutineContext().ensureActive()
            fallbackDecision(goal, stepNumber, snapshot, history, language, timeout, taskPlan)
        } catch (e: CancellationException) {
            throw e
        } catch (cloudFailure: Exception) {
            fallbackDecision(goal, stepNumber, snapshot, history, language, cloudFailure, taskPlan)
        }
    }

    private fun localOnlyDecision(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        taskPlan: CommandTaskPlan? = null
    ): HybridStepDecision {
        val localDecision = localEngine.decideNextStep(
            goal = goal,
            stepNumber = stepNumber,
            snapshot = snapshot,
            history = history,
            language = language,
            fallbackReason = "",
            taskPlan = taskPlan
        )
        return HybridStepDecision(
            decision = localDecision,
            engineSource = AiEngineSource.LOCAL_LITE_FALLBACK,
            fallbackReason = null
        )
    }

    private fun fallbackDecision(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        cloudFailure: Exception,
        taskPlan: CommandTaskPlan? = null
    ): HybridStepDecision {
        val reason = formatFallbackReason(cloudFailure, language)
        Log.w(TAG, "Cloud Gemini exhausted/timed out on step $stepNumber; switching to LocalLiteLanguageEngine ($reason)", cloudFailure)
        val localDecision = localEngine.decideNextStep(
            goal = goal,
            stepNumber = stepNumber,
            snapshot = snapshot,
            history = history,
            language = language,
            fallbackReason = reason,
            taskPlan = taskPlan
        )
        return HybridStepDecision(
            decision = localDecision,
            engineSource = AiEngineSource.LOCAL_LITE_FALLBACK,
            fallbackReason = reason
        )
    }

    fun streamPageSummary(
        url: String,
        title: String,
        contentText: String,
        language: AppLanguage,
        allowCloudGemini: Boolean = true,
        metaDescription: String = "",
        semanticOutline: String = "",
        structuredDataSummary: String = ""
    ): Flow<SummaryChunk> = flow {
        if (!allowCloudGemini) {
            emit(
                SummaryChunk(
                    text = localEngine.summarizePageOffline(
                        url = url,
                        title = title,
                        contentText = contentText,
                        language = language,
                        metaDescription = metaDescription,
                        semanticOutline = semanticOutline,
                        structuredDataSummary = structuredDataSummary
                    ),
                    engineSource = AiEngineSource.LOCAL_LITE_FALLBACK,
                    fallbackReason = null
                )
            )
            return@flow
        }

        val langInstruction = if (language == AppLanguage.ARABIC) {
            "لخّص محتوى صفحة الويب التالية بدقة باللغة العربية استناداً إلى بنية HTML والبيانات المستخرجة:\n- فكرة الصفحة الرئيسية\n- أهم 3 إلى 5 نقاط رئيسية\n- أي بيانات مهيكلة أو إجراءات أو روابط مهمة"
        } else {
            "Summarize the following webpage accurately in English using its HTML structure and extracted content:\n- Core takeaway\n- 3 to 5 key bullet points\n- Notable structured facts, actions, or links"
        }
        val prompt = buildString {
            appendLine(langInstruction)
            appendLine()
            appendLine("Title: ${title.take(180)}")
            appendLine("URL: ${url.take(300)}")
            if (metaDescription.isNotBlank()) {
                appendLine("Meta Description: ${metaDescription.take(350)}")
            }
            if (semanticOutline.isNotBlank()) {
                appendLine("HTML Semantic Outline:")
                appendLine(semanticOutline.take(1800))
            }
            if (structuredDataSummary.isNotBlank()) {
                appendLine("Structured Data (JSON-LD):")
                appendLine(structuredDataSummary.take(900))
            }
            appendLine("Content:")
            appendLine(contentText.take(9000))
        }

        try {
            val sb = StringBuilder()
            withTimeout(25_000L) {
                summaryModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        sb.append(piece)
                        emit(
                            SummaryChunk(
                                text = sb.toString(),
                                engineSource = AiEngineSource.CLOUD_GEMINI
                            )
                        )
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("Gemini returned an empty summary.")
            }
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            val reason = formatFallbackReason(timeout, language)
            Log.w(TAG, "Cloud summary timed out; falling back to LocalLiteLanguageEngine ($reason)", timeout)
            emit(
                SummaryChunk(
                    text = localEngine.summarizePageOffline(
                        url = url,
                        title = title,
                        contentText = contentText,
                        language = language,
                        metaDescription = metaDescription,
                        semanticOutline = semanticOutline,
                        structuredDataSummary = structuredDataSummary
                    ),
                    engineSource = AiEngineSource.LOCAL_LITE_FALLBACK,
                    fallbackReason = reason
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (cloudFailure: Exception) {
            val reason = formatFallbackReason(cloudFailure, language)
            Log.w(TAG, "Cloud summary failed; falling back to LocalLiteLanguageEngine ($reason)", cloudFailure)
            emit(
                SummaryChunk(
                    text = localEngine.summarizePageOffline(
                        url = url,
                        title = title,
                        contentText = contentText,
                        language = language,
                        metaDescription = metaDescription,
                        semanticOutline = semanticOutline,
                        structuredDataSummary = structuredDataSummary
                    ),
                    engineSource = AiEngineSource.LOCAL_LITE_FALLBACK,
                    fallbackReason = reason
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    fun suggestPrompts(
        query: String,
        currentUrl: String,
        pageTitle: String,
        isHomeMode: Boolean,
        language: AppLanguage
    ): List<PromptSuggestion> = localEngine.suggestPrompts(
        query = query,
        currentUrl = currentUrl,
        pageTitle = pageTitle,
        isHomeMode = isHomeMode,
        language = language
    )

    private suspend fun streamJsonResponse(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String
    ): String {
        val sb = StringBuilder()
        model.generateContentStream(prompt).collect { chunk ->
            sb.append(chunk.text.orEmpty())
        }
        return sb.toString()
    }

    private fun formatFallbackReason(e: Exception, language: AppLanguage): String {
        val msg = e.message.orEmpty()
        val isAr = language == AppLanguage.ARABIC
        return when {
            msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("exhausted", ignoreCase = true) ->
                if (isAr) "انتهاء حصة Gemini" else "Gemini quota exceeded"
            e is TimeoutCancellationException || msg.contains("timeout", ignoreCase = true) ->
                if (isAr) "انتهاء مهلة الاتصال" else "Request timeout"
            msg.contains("network", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) ->
                if (isAr) "عدم توفر اتصال إنترنت" else "Offline / network error"
            else -> if (isAr) "تعذر الخدمة السحابية" else "Cloud service unavailable"
        }
    }

    internal fun buildStepPrompt(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        taskPlan: CommandTaskPlan? = null
    ): String {
        val elementsArray = JSONArray()
        snapshot.elements.take(55).forEach { el ->
            elementsArray.put(
                JSONObject().apply {
                    put("agent_id", el.agentId)
                    put("tag", el.tag)
                    if (el.role.isNotBlank()) put("role", el.role)
                    if (el.elementId.isNotBlank()) put("html_id", el.elementId.take(50))
                    if (el.cssClasses.isNotBlank()) put("css_classes", el.cssClasses.take(100))
                    if (el.selector.isNotBlank()) put("selector", el.selector.take(100))
                    if (el.text.isNotBlank()) put("text", el.text.take(90))
                    if (el.placeholder.isNotBlank()) put("placeholder", el.placeholder.take(60))
                    if (el.ariaLabel.isNotBlank()) put("aria_label", el.ariaLabel.take(60))
                    if (el.labelText.isNotBlank()) put("label", el.labelText.take(60))
                    if (el.nameAttr.isNotBlank()) put("name", el.nameAttr.take(40))
                    if (el.autocomplete.isNotBlank()) put("autocomplete", el.autocomplete)
                    if (el.sectionContext.isNotBlank()) put("section", el.sectionContext.take(30))
                    if (el.containerId.isNotBlank()) put("container_id", el.containerId)
                    if (el.containerHeading.isNotBlank()) put("container_heading", el.containerHeading.take(50))
                    if (el.containerSignature.isNotBlank()) put("container_signature", el.containerSignature.take(120))
                    if (el.htmlTagSummary.isNotBlank()) put("html_tag", el.htmlTagSummary.take(120))
                    if (el.authRole.isNotBlank()) put("auth_role", el.authRole)
                    if (el.ariaSelected != null) put("aria_selected", el.ariaSelected)
                    if (el.value.isNotBlank()) put("value", el.value.take(60))
                    if (el.inputType.isNotBlank()) put("input_type", el.inputType)
                    if (el.isRequired) put("required", true)
                    if (el.inputType == "checkbox" || el.role == "checkbox") put("checked", el.isChecked)
                    if (!el.isEnabled) put("enabled", false)
                }
            )
        }

        val historyArray = JSONArray()
        history.takeLast(4).forEach { log ->
            historyArray.put(
                JSONObject().apply {
                    put("step", log.stepNumber)
                    put("action", log.decision.action.wireName)
                    if (log.decision.targetAgentId.isNotBlank()) {
                        put("target_agent_id", log.decision.targetAgentId)
                    }
                    if (log.decision.inputText.isNotBlank()) {
                        put("input_text", log.decision.inputText.take(60))
                    }
                    put("success", log.executionResult?.success ?: true)
                    val msg = log.executionResult?.message.orEmpty().take(120)
                    if (msg.isNotBlank()) put("result_message", msg)
                }
            )
        }

        val entities = taskPlan?.entities ?: CommandIntentResolver.extractEntities(goal)
        val entitiesJson = JSONObject().apply {
            if (!entities.email.isNullOrBlank()) put("email", entities.email)
            if (!entities.phone.isNullOrBlank()) put("phone", entities.phone)
            if (!entities.personName.isNullOrBlank()) put("person_name", entities.personName)
            if (!entities.targetDomain.isNullOrBlank()) put("target_domain", entities.targetDomain)
            if (entities.authIntent != com.example.model.AuthIntentKind.NONE) {
                put("auth_intent", entities.authIntent.name.lowercase())
            }
        }

        val planArray = JSONArray()
        taskPlan?.steps?.forEach { sub ->
            planArray.put(
                JSONObject().apply {
                    put("step", sub.index)
                    put("kind", sub.kind.name.lowercase())
                    put("status", sub.status.name.lowercase())
                    put("title", sub.titleEn)
                    if (sub.detail.isNotBlank()) put("detail", sub.detail)
                }
            )
        }

        val responseLanguageNote = if (language == AppLanguage.ARABIC) {
            "Use Arabic for thought, recovery_analysis, final_summary."
        } else {
            "Use English for thought, recovery_analysis, final_summary."
        }

        val payload = JSONObject().apply {
            put("step", stepNumber)
            put("goal", goal)
            put("lang", responseLanguageNote)
            if (entitiesJson.length() > 0) put("extracted_entities", entitiesJson)
            if (planArray.length() > 0) {
                put("decomposed_plan", planArray)
                val activeSub = taskPlan?.steps?.getOrNull(taskPlan.activeStepIndex)
                if (activeSub != null) put("current_substep", "${activeSub.index}. ${activeSub.titleEn}")
            }
            put(
                "screen",
                JSONObject().apply {
                    put("url", snapshot.url.take(180))
                    put("title", snapshot.title.take(120))
                    if (snapshot.pageLanguage.isNotBlank()) put("page_lang", snapshot.pageLanguage)
                    if (snapshot.metaDescription.isNotBlank()) put("meta_description", snapshot.metaDescription.take(300))
                    if (snapshot.authFormHint.isNotBlank()) put("auth_form_hint", snapshot.authFormHint)
                    if (snapshot.hasPasswordField) put("has_password_field", true)
                    if (snapshot.hasOtpField) put("has_otp_field", true)
                    if (snapshot.hasCaptcha) put("has_captcha", true)
                    if (snapshot.networkErrorSummary.isNotBlank()) {
                        put("network_error_summary", snapshot.networkErrorSummary.take(160))
                    }
                    if (snapshot.pageErrors.isNotEmpty()) {
                        put("page_errors", JSONArray(snapshot.pageErrors.take(6)))
                    }
                    if (snapshot.invalidFields.isNotEmpty()) {
                        put("invalid_fields", JSONArray(snapshot.invalidFields.take(6)))
                    }
                    put("scroll_pct", snapshot.scrollPercent)
                    if (snapshot.semanticOutline.isNotBlank()) put("semantic_outline", snapshot.semanticOutline.take(1600))
                    if (snapshot.structuredDataSummary.isNotBlank()) put("structured_data", snapshot.structuredDataSummary.take(800))
                    put("text", snapshot.pageTextPreview.take(2400))
                    if (snapshot.htmlSnippet.isNotBlank()) put("html_snippet", snapshot.htmlSnippet.take(1800))
                    put("elements", elementsArray)
                }
            )
            FailureDiagnosisAnalyzer.detectStallLoop(history)?.let { stall ->
                put("loop_warning", stall)
            }
            if (historyArray.length() > 0) {
                put("history", historyArray)
            }
        }

        return payload.toString()
    }
}
