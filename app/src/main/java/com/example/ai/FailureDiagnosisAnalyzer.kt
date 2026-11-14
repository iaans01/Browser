package com.example.ai

import com.example.browser.CommandIntentResolver
import com.example.model.AgentActionType
import com.example.model.AgentStepLog
import com.example.model.AppLanguage
import com.example.model.AuthIntentKind
import com.example.model.CommandTaskPlan
import com.example.model.FailureCategory
import com.example.model.FailureDiagnosis
import com.example.model.ScreenSnapshot

object FailureDiagnosisAnalyzer {

    /**
     * Detects when execution has stalled into a tight repetition loop or repeated DOM action failures.
     */
    fun detectStallLoop(history: List<AgentStepLog>): String? {
        if (history.size < 3) return null
        val tail3 = history.takeLast(3)

        val allFailed = tail3.all { it.executionResult?.success == false }
        if (allFailed) {
            val msgs = tail3.mapNotNull { it.executionResult?.message?.takeIf(String::isNotBlank) }.distinct()
            val reason = msgs.lastOrNull() ?: "DOM action failed repeatedly"
            return "3 consecutive execution failures ($reason)"
        }

        val actionKeys = tail3.map {
            "${it.decision.action.wireName}:${it.decision.targetAgentId}:${it.url}"
        }
        if (actionKeys.distinct().size == 1 && tail3.first().decision.action != AgentActionType.READ_SCREEN) {
            val sample = tail3.first()
            return "Repeated identical step ${sample.decision.action.wireName} on ${sample.decision.targetAgentId.ifBlank { "page" }} without state change"
        }

        val allScrolls = tail3.all { it.decision.action == AgentActionType.SCROLL }
        if (allScrolls && tail3.map { it.url }.distinct().size == 1) {
            return "Scrolled 3 times on ${tail3.first().url} without finding matching targets"
        }

        return null
    }

    fun analyzeFailure(
        goal: String,
        snapshot: ScreenSnapshot?,
        history: List<AgentStepLog>,
        language: AppLanguage,
        lastWebError: String? = null,
        rawErrorMessage: String? = null,
        hitMaxSteps: Boolean = false,
        taskPlan: CommandTaskPlan? = null
    ): FailureDiagnosis {
        val isAr = language == AppLanguage.ARABIC
        val entities = taskPlan?.entities ?: CommandIntentResolver.extractEntities(goal)
        val pageErrors = snapshot?.pageErrors.orEmpty().filter { it.isNotBlank() }
        val invalidFields = snapshot?.invalidFields.orEmpty().filter { it.isNotBlank() }
        val netSummary = snapshot?.networkErrorSummary?.trim().orEmpty()
        val combinedWebError = lastWebError?.trim().orEmpty().ifBlank { netSummary }
        val rawMsg = rawErrorMessage?.trim().orEmpty()
        val rawLower = rawMsg.lowercase()
        val stallReason = detectStallLoop(history)
        val urlInfo = snapshot?.url.orEmpty().take(90)

        // 1. Network / HTTP / Cloudflare / SSL failure
        if (
            combinedWebError.isNotBlank() ||
            containsAny(
                rawLower,
                listOf("err_name_not_resolved", "err_connection", "http 403", "http 404", "http 500", "http 502", "http 503", "cloudflare", "network error", "ssl", "net::")
            )
        ) {
            val evidence = listOfNotNull(
                combinedWebError.takeIf { it.isNotBlank() },
                rawMsg.takeIf { it.isNotBlank() && it != combinedWebError },
                urlInfo.takeIf { it.isNotBlank() }?.let { "URL: $it" }
            ).joinToString(" | ")
            return FailureDiagnosis(
                category = FailureCategory.NETWORK_HTTP,
                title = if (isAr) "تعذر تحميل الصفحة أو تم حظر الاتصال" else "Page Load Failed or Connection Blocked",
                causeSummary = if (isAr) {
                    val detail = combinedWebError.ifBlank { rawMsg }
                    "توقف تنفيذ المهمة بسبب خطأ في الشبكة أو استجابة الخادم: $detail"
                } else {
                    val detail = combinedWebError.ifBlank { rawMsg }
                    "Execution stopped due to a network or server response error: $detail"
                },
                evidenceDetail = evidence,
                suggestedFixes = if (isAr) {
                    listOf(
                        "تحقق من صحة رابط الموقع وتوفر اتصال الإنترنت.",
                        "إذا ظهر تحدي حماية (مثل Cloudflare)، أكمله داخل المتصفح ثم اضغط «متابعة الوكيل»."
                    )
                } else {
                    listOf(
                        "Verify the website URL and your internet connection.",
                        "If a security challenge (e.g. Cloudflare) appears, solve it in the browser and tap Resume."
                    )
                },
                canResumeAfterFix = true,
                canRetry = true
            )
        }

        // 2. Site inline validation / server alert errors (e.g., WooCommerce alert, aria-invalid, HTML5 validationMessage)
        if (pageErrors.isNotEmpty() || invalidFields.isNotEmpty()) {
            val errorQuote = pageErrors.firstOrNull() ?: invalidFields.firstOrNull().orEmpty()
            val evidenceParts = buildList {
                if (pageErrors.isNotEmpty()) add("Alerts: ${pageErrors.joinToString(" ; ").take(180)}")
                if (invalidFields.isNotEmpty()) add("Invalid fields: ${invalidFields.joinToString(" ; ").take(150)}")
            }
            val isDuplicateEmail = containsAny(
                errorQuote.lowercase(),
                listOf(
                    "already registered", "already exists", "account is already",
                    "مسجل مسبق", "مسجل بالفعل", "حساب مسجل", "مستخدم بالفعل", "لديك حساب"
                )
            )
            return FailureDiagnosis(
                category = FailureCategory.SITE_VALIDATION,
                title = if (isAr) {
                    if (isDuplicateEmail) "رفض الموقع التسجيل: البريد مسجل مسبقاً" else "رفض الموقع الإجراء بسبب رسالة خطأ"
                } else {
                    if (isDuplicateEmail) "Registration Rejected: Email Already Exists" else "Website Rejected Submission with Validation Error"
                },
                causeSummary = if (isAr) {
                    if (rawMsg.isNotBlank() && !pageErrors.any { rawMsg.contains(it.take(20)) }) {
                        "$rawMsg\n• رسالة الموقع: «$errorQuote»"
                    } else {
                        "أظهر الموقع رسالة خطأ تمنع الإكمال: «$errorQuote»"
                    }
                } else {
                    if (rawMsg.isNotBlank() && !pageErrors.any { rawMsg.contains(it.take(20)) }) {
                        "$rawMsg\n• Site message: \"$errorQuote\""
                    } else {
                        "The website displayed an error blocking completion: \"$errorQuote\""
                    }
                },
                evidenceDetail = evidenceParts.joinToString(" | "),
                suggestedFixes = if (isAr) {
                    if (isDuplicateEmail) {
                        listOf(
                            "استخدم بريداً إلكترونياً جديداً غير مسجل في هذا الموقع.",
                            "أو غيّر الأمر إلى «تسجيل الدخول» إذا كنت تملك حساباً بالفعل."
                        )
                    } else {
                        listOf(
                            "راجع الحقول المميزة بالخطأ داخل الصفحة وصحّح البيانات المطلوبة.",
                            "تأكد من تضمين جميع البيانات اللازمة (مثل الاسم أو الهاتف أو كلمة المرور) في أمرك."
                        )
                    }
                } else {
                    if (isDuplicateEmail) {
                        listOf(
                            "Provide a different email address that is not yet registered.",
                            "Or switch the command to Log In if you already have an account."
                        )
                    } else {
                        listOf(
                            "Review the highlighted invalid fields on the page and fix the input.",
                            "Ensure all required fields (name, phone, password) are supplied."
                        )
                    }
                },
                canResumeAfterFix = true,
                canRetry = true
            )
        }

        // 3. Auth / CAPTCHA / OTP / Password blocker
        if (
            snapshot?.hasCaptcha == true ||
            snapshot?.hasOtpField == true ||
            containsAny(rawLower, listOf("captcha", "recaptcha", "turnstile", "otp", "verification code", "كلمة المرور", "التحقق"))
        ) {
            return FailureDiagnosis(
                category = FailureCategory.AUTH_OR_CAPTCHA,
                title = if (isAr) "توقف عند اختبار تحقق أو بيانات سرية" else "Blocked by Verification or Password Requirement",
                causeSummary = rawMsg.ifBlank {
                    if (isAr) "يتطلب الموقع إكمال كلمة المرور أو رمز التحقق أو CAPTCHA يدوياً."
                    else "The website requires manual password, OTP, or CAPTCHA completion."
                },
                evidenceDetail = buildString {
                    if (snapshot?.hasCaptcha == true) append("captcha=true ")
                    if (snapshot?.hasOtpField == true) append("otp=true ")
                    if (snapshot?.hasPasswordField == true) append("password=true ")
                    if (urlInfo.isNotBlank()) append("URL: $urlInfo")
                }.trim(),
                suggestedFixes = if (isAr) {
                    listOf(
                        "أكمل التحقق أو كلمة المرور مباشرة في المتصفح ثم اضغط «متابعة الوكيل»."
                    )
                } else {
                    listOf(
                        "Complete the verification or password directly in the browser, then tap Resume Agent."
                    )
                },
                canResumeAfterFix = true,
                canRetry = false
            )
        }

        // 4. AI service / quota / timeout error
        if (
            containsAny(
                rawLower,
                listOf("quota", "429", "timeout", "timed out", "مهلة", "حصة gemini", "service unavailable", "الخدمة السحابية")
            )
        ) {
            return FailureDiagnosis(
                category = FailureCategory.AI_SERVICE,
                title = if (isAr) "تعذر استجابة محرك الذكاء السحابي" else "Cloud AI Engine Timeout or Quota Limit",
                causeSummary = rawMsg.ifBlank {
                    if (isAr) "انقطعت أو تأخرت استجابة محرك Gemini السحابي أثناء تحليل الخطوة."
                    else "Cloud Gemini timed out or hit a quota limit while planning the step."
                },
                evidenceDetail = rawMsg,
                suggestedFixes = if (isAr) {
                    listOf(
                        "أعد المحاولة أو فعّل المحرك المحلي الخفيف من الإعدادات للعمل دون إنترنت."
                    )
                } else {
                    listOf(
                        "Retry the step or disable Cloud Gemini in Settings to use the offline Local Lite engine."
                    )
                },
                canResumeAfterFix = false,
                canRetry = true
            )
        }

        // 5. Missing user-supplied data (e.g. auth goal with missing email, or empty required inputs without matching entity)
        val missingRequiredInputs = snapshot?.elements.orEmpty().filter {
            it.isVisible && it.isEnabled && it.isRequired && it.value.isBlank() &&
                it.inputType !in setOf("hidden", "submit", "button", "checkbox")
        }
        val missingEmailForAuth = entities.authIntent != AuthIntentKind.NONE && entities.email.isNullOrBlank()
        if (missingEmailForAuth || (missingRequiredInputs.isNotEmpty() && containsAny(rawLower, listOf("missing", "required", "ناقص", "مفقود", "مطلوب")))) {
            val fieldNames = missingRequiredInputs.mapNotNull {
                it.labelText.ifBlank { it.placeholder.ifBlank { it.nameAttr.ifBlank { it.elementId } } }.takeIf(String::isNotBlank)
            }.take(3)
            return FailureDiagnosis(
                category = FailureCategory.MISSING_USER_DATA,
                title = if (isAr) "توجد بيانات مطلوبة غير متوفرة في الأمر" else "Required Input Data Missing from Goal",
                causeSummary = if (isAr) {
                    when {
                        missingEmailForAuth -> "طلبت تنفيذ تسجيل حساب/دخول لكن الأمر لا يحتوي على بريد إلكتروني."
                        fieldNames.isNotEmpty() -> "يحتوي النموذج على حقول مطلوبة فارغة لم تُذكر في الأمر: ${fieldNames.joinToString("، ")}"
                        else -> rawMsg
                    }
                } else {
                    when {
                        missingEmailForAuth -> "An authentication task was requested without an email address in the prompt."
                        fieldNames.isNotEmpty() -> "Required form fields remain empty with no matching data in your prompt: ${fieldNames.joinToString(", ")}"
                        else -> rawMsg
                    }
                },
                evidenceDetail = if (fieldNames.isNotEmpty()) "Required empty: ${fieldNames.joinToString(", ")}" else rawMsg,
                suggestedFixes = if (isAr) {
                    listOf(
                        "أضف البريد الإلكتروني أو البيانات المطلوبة في خانة الهدف ثم أعد التشغيل.",
                        "أو املأ الحقول يدوياً في المتصفح ثم اضغط «متابعة الوكيل»."
                    )
                } else {
                    listOf(
                        "Include the missing email or required details in the goal input and retry.",
                        "Or fill the missing fields in the browser and tap Resume Agent."
                    )
                },
                canResumeAfterFix = true,
                canRetry = true
            )
        }

        // 6. Loop stall or max-steps exhaustion
        if (stallReason != null || hitMaxSteps) {
            val lastAction = history.lastOrNull()
            val lastActionDesc = lastAction?.let {
                "${it.decision.action.wireName} ${it.decision.targetAgentId}".trim()
            }.orEmpty()
            return FailureDiagnosis(
                category = FailureCategory.LOOP_STALLED,
                title = if (isAr) {
                    if (stallReason != null) "توقف الوكيل لتجنب تكرار نفس الخطوة دون تقدم"
                    else "استنفد الوكيل الحد الأقصى للخطوات دون إتمام الهدف"
                } else {
                    if (stallReason != null) "Agent Stopped: Repeated Actions Without Progress"
                    else "Max Step Limit Reached Before Goal Completion"
                },
                causeSummary = if (isAr) {
                    when {
                        stallReason != null -> "لم يتغير وضع الصفحة بعد تكرار نفس الإجراء ($lastActionDesc)، مما يدل على عدم استجابة العنصر أو غياب التغذية الراجعة من الموقع."
                        else -> "وصلت الحلقة للحد الأقصى للخطوات (${history.size}) دون تحقيق حالة الاكتمال المطلوبة."
                    }
                } else {
                    when {
                        stallReason != null -> "Page state did not advance after repeating ($lastActionDesc), indicating an unresponsive control or hidden requirement."
                        else -> "The agent reached the maximum step limit (${history.size}) without reaching a completed state."
                    }
                },
                evidenceDetail = stallReason ?: (rawMsg.ifBlank { "Last step: $lastActionDesc | URL: $urlInfo" }),
                suggestedFixes = if (isAr) {
                    listOf(
                        "افتح «شجرة DOM» للتحقق من المحدد الفعلي للعنصر المستهدف أو وجود عنصر معطّل.",
                        "مرّر الصفحة يدوياً إلى القسم المطلوب أو أعد صياغة الهدف بدقة أكبر ثم أعد التشغيل."
                    )
                } else {
                    listOf(
                        "Open Inspect DOM to verify the target element's selector and enabled state.",
                        "Scroll to the relevant section manually or refine your goal instructions, then retry."
                    )
                },
                canResumeAfterFix = false,
                canRetry = true
            )
        }

        // 7. Element not found / unreachable target
        if (
            containsAny(
                rawLower,
                listOf("not found", "could not locate", "no matching", "unachievable", "disabled", "لم يجد", "تعذر إيجاد", "غير موجود", "معطّل", "لم يتم العثور")
            )
        ) {
            return FailureDiagnosis(
                category = FailureCategory.ELEMENT_NOT_FOUND,
                title = if (isAr) "تعذر العثور على العنصر أو النموذج المطلوب" else "Could Not Locate Target Element or Form",
                causeSummary = rawMsg.ifBlank {
                    if (isAr) "لم يجد الوكيل زراً أو حقلاً مطابقاً للهدف في العناصر المرئية الحالية."
                    else "No visible interactive element on this page matched the requested goal."
                },
                evidenceDetail = "Visible elements: ${snapshot?.elements?.size ?: 0} | URL: $urlInfo",
                suggestedFixes = if (isAr) {
                    listOf(
                        "تأكد من أنك داخل الصفحة الصحيحة للموقع (مثلاً صفحة تسجيل الحساب).",
                        "افتح «شجرة DOM» للبحث عن معرف العنصر أو اسم النموذج الفعلي."
                    )
                } else {
                    listOf(
                        "Make sure the browser is on the intended sub-page (such as the registration page).",
                        "Use Inspect DOM to search for the element's HTML ID or class."
                    )
                },
                canResumeAfterFix = true,
                canRetry = true
            )
        }

        // 8. Fallback unknown failure preserving raw reason
        return FailureDiagnosis(
            category = FailureCategory.UNKNOWN,
            title = if (isAr) "تعذر إتمام المهمة" else "Unable to Complete Task",
            causeSummary = rawMsg.ifBlank {
                if (isAr) "توقفت المهمة بسبب عائق غير متوقع في الصفحة أو التنفيذ."
                else "Execution stopped due to an unexpected page or runtime issue."
            },
            evidenceDetail = urlInfo.takeIf { it.isNotBlank() }?.let { "URL: $it" }.orEmpty(),
            suggestedFixes = if (isAr) {
                listOf("تحقق من محتوى المتصفح وافتح «شجرة DOM» أو أعد المحاولة.")
            } else {
                listOf("Check the browser view, inspect DOM nodes, or retry.")
            },
            canResumeAfterFix = false,
            canRetry = true
        )
    }

    private fun containsAny(haystack: String, needles: List<String>): Boolean =
        needles.any { haystack.contains(it, ignoreCase = true) }
}
