package com.example.ai

import com.example.browser.CommandIntentResolver
import com.example.model.AgentActionDecision
import com.example.model.AgentActionType
import com.example.model.AgentStepLog
import com.example.model.AppLanguage
import com.example.model.AuthIntentKind
import com.example.model.CommandTaskPlan
import com.example.model.DomElement
import com.example.model.PromptSuggestion
import com.example.model.ScreenSnapshot
import org.json.JSONObject
import kotlin.math.ln

class LocalLiteLanguageEngine {

    private val stopWords = setOf(
        // Arabic
        "في", "من", "على", "الى", "إلى", "عن", "مع", "هذا", "هذه", "ذلك", "تلك",
        "التي", "الذي", "الذين", "او", "أو", "ثم", "كما", "لكن", "بل", "ان", "أن",
        "كان", "كانت", "يكون", "هو", "هي", "هم", "نحن", "انا", "أنا", "انت", "أنت",
        "كل", "بعض", "بين", "عند", "منذ", "حتى", "قد", "لقد", "لا", "ما", "لم", "لن",
        // English
        "the", "and", "for", "that", "this", "with", "from", "are", "was", "were",
        "have", "has", "had", "not", "but", "you", "your", "can", "will", "into",
        "about", "more", "than", "then", "also", "when", "what", "which", "their"
    )

    // 1. Offline / Quota-Fallback Agent Step Planner
    fun decideNextStep(
        goal: String,
        stepNumber: Int,
        snapshot: ScreenSnapshot,
        history: List<AgentStepLog>,
        language: AppLanguage,
        fallbackReason: String = "",
        taskPlan: CommandTaskPlan? = null
    ): AgentActionDecision {
        val isAr = language == AppLanguage.ARABIC
        val normGoal = normalizeText(goal)
        val entities = taskPlan?.entities ?: CommandIntentResolver.extractEntities(goal)
        val expandedGoalTokens = expandBilingualTokens(
            tokenize(goal).filter { it.length >= 2 && it !in stopWords }
        )
        val failedTargets = history
            .filter { it.executionResult?.success == false && it.decision.targetAgentId.isNotBlank() }
            .map { it.decision.targetAgentId }
            .toSet()

        val recentTypedTargets = history
            .filter { it.decision.action == AgentActionType.TYPE && it.executionResult?.success == true }
            .map { it.decision.targetAgentId }
            .toSet()

        val recentClickedTargetsOnUrl = history
            .filter {
                it.decision.action == AgentActionType.CLICK &&
                    it.executionResult?.success == true &&
                    it.url == snapshot.url
            }
            .map { it.decision.targetAgentId }
            .toSet()

        val lastFailed = history.lastOrNull()?.executionResult?.success == false
        val recoveryNote = if (lastFailed) {
            val prevMsg = history.lastOrNull()?.executionResult?.message.orEmpty()
            if (isAr) "تبديل العنصر المستهدف تلقائياً بعد فشل الخطوة السابقة ($prevMsg)."
            else "Switched target automatically after previous failure ($prevMsg)."
        } else {
            ""
        }

        val enabledElements = snapshot.elements.filter {
            it.isVisible && it.isEnabled && it.agentId !in failedTargets
        }

        // 0A. Detect repeated stalled loop or repeated execution failures early
        val stallWarning = FailureDiagnosisAnalyzer.detectStallLoop(history)
        if (stallWarning != null) {
            val msg = if (isAr) {
                "توقف التنفيذ لتكرار نفس الإجراء دون تقدم ($stallWarning)."
            } else {
                "Execution stopped due to repeated steps without progress ($stallWarning)."
            }
            return buildDecision(
                stepNumber = stepNumber,
                phase = "fail",
                thought = msg,
                action = AgentActionType.FAIL,
                recoveryAnalysis = stallWarning,
                finalSummary = msg,
                fallbackReason = fallbackReason
            )
        }

        // 0B. Detect explicit HTTP / network / bot-challenge error pages
        if (snapshot.networkErrorSummary.isNotBlank()) {
            val msg = if (isAr) {
                "تعذر إتمام المهمة بسبب خطأ في الصفحة أو الشبكة: ${snapshot.networkErrorSummary}"
            } else {
                "Cannot proceed due to a page or network error: ${snapshot.networkErrorSummary}"
            }
            return buildDecision(
                stepNumber = stepNumber,
                phase = "fail",
                thought = msg,
                action = AgentActionType.FAIL,
                recoveryAnalysis = recoveryNote,
                finalSummary = msg,
                fallbackReason = fallbackReason
            )
        }

        // 0C. Detect site inline alert/validation errors when already attempted or hard rejection
        if (snapshot.pageErrors.isNotEmpty() || snapshot.invalidFields.isNotEmpty()) {
            val primaryError = snapshot.pageErrors.firstOrNull()
                ?: snapshot.invalidFields.firstOrNull().orEmpty()
            val alreadyActedHere = history.any { it.url == snapshot.url }
            val isHardRejection = containsAny(
                normalizeText(primaryError),
                listOf(
                    "already registered", "already exists", "invalid", "incorrect", "blocked", "denied",
                    "cannot", "unable", "مسجل مسبق", "مسجل بالفعل", "حساب مسجل", "موجود مسبق",
                    "مستخدم بالفعل", "غير صحيح", "غير صالح", "محظور", "فشل", "تعذر", "خطا:"
                )
            )
            if (alreadyActedHere || isHardRejection) {
                val msg = if (isAr) {
                    "رفض الموقع المتابعة وأظهر رسالة الخطأ: $primaryError"
                } else {
                    "The website blocked completion with error: $primaryError"
                }
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "fail",
                    thought = msg,
                    action = AgentActionType.FAIL,
                    recoveryAnalysis = if (isAr) "تم رصد خطأ مباشر في الصفحة بعد التنفيذ." else "Detected inline website error after submission.",
                    finalSummary = msg,
                    fallbackReason = fallbackReason
                )
            }
        }

        // Extract quoted phrases or search phrase from goal
        val quotedTarget = extractQuotedPhrase(goal)
        val searchQuery = quotedTarget ?: extractSearchPhrase(goal)

        val wantsSearch = containsAny(
            normGoal,
            listOf("ابحث", "بحث", "دوّر", "دور", "search", "find", "lookup", "duckduckgo", "wiki")
        )
        val wantsOpenResult = containsAny(
            normGoal,
            listOf("افتح", "نتيجه", "نتيجة", "اول", "أول", "open", "result", "click", "select")
        )
        val wantsSummarizeOrRead = containsAny(
            normGoal,
            listOf("لخص", "ملخص", "اقرا", "اقرأ", "summarize", "summary", "read")
        )
        val isConfirmationVisible = containsAny(
            normalizeText(snapshot.pageTextPreview),
            listOf("order #", "تم تاكيد", "تم تأكيد", "confirmed", "receipt", "thank you")
        )

        val authIntent = if (entities.authIntent != AuthIntentKind.NONE) {
            entities.authIntent
        } else {
            CommandIntentResolver.detectAuthIntent(goal, hasEmail = !entities.email.isNullOrBlank())
        }
        val targetRole = when (authIntent) {
            AuthIntentKind.SIGN_UP -> "signup"
            AuthIntentKind.LOG_IN -> "login"
            AuthIntentKind.NONE -> ""
        }
        val oppositeRole = when (targetRole) {
            "signup" -> "login"
            "login" -> "signup"
            else -> ""
        }
        val allowOAuth = CommandIntentResolver.explicitlyAllowsOAuth(goal)
        val allowReset = CommandIntentResolver.explicitlyAllowsPasswordReset(goal)
        val email = entities.email

        // High-priority Bilingual Auth (Sign Up & Log In) Container-Scoped & Tab-Aware Workflow
        if (authIntent != AuthIntentKind.NONE || !email.isNullOrBlank()) {
            val filteredElements = enabledElements.filter { el ->
                (allowOAuth || !isOAuthControl(el)) &&
                    (allowReset || !isForgotPasswordControl(el))
            }

            // 1. Check for tabbed auth UI where the desired tab is currently inactive (ariaSelected == false)
            if (targetRole.isNotBlank()) {
                val inactiveTargetTab = findInactiveTargetTab(
                    elements = filteredElements,
                    targetRole = targetRole,
                    alreadyClickedIds = recentClickedTargetsOnUrl
                )
                if (inactiveTargetTab != null) {
                    return buildDecision(
                        stepNumber = stepNumber,
                        phase = "act",
                        thought = if (isAr) {
                            val roleAr = if (targetRole == "signup") "التسجيل الجديد" else "تسجيل الدخول"
                            "التبويب النشط لا يطابق الهدف؛ التبديل تلقائياً إلى تبويب $roleAr (${inactiveTargetTab.text.take(40)})."
                        } else {
                            val roleEn = if (targetRole == "signup") "Sign Up" else "Log In"
                            "Active tab does not match goal; switching automatically to $roleEn tab (${inactiveTargetTab.text.take(40)})."
                        },
                        action = AgentActionType.CLICK,
                        targetAgentId = inactiveTargetTab.agentId,
                        recoveryAnalysis = recoveryNote,
                        fallbackReason = fallbackReason
                    )
                }
            }

            val authNavCandidate = if (targetRole.isNotBlank()) {
                findAuthNavigationElement(
                    elements = filteredElements,
                    targetRole = targetRole,
                    alreadyClickedIds = recentClickedTargetsOnUrl
                )
            } else {
                null
            }

            // If entire screen is clearly the opposite mode (e.g. login screen when user asked for signup) and a switch link/tab exists
            val mismatchHint = (targetRole == "signup" && snapshot.authFormHint in setOf("login", "tabbed_login_active")) ||
                (targetRole == "login" && snapshot.authFormHint in setOf("signup", "tabbed_signup_active"))
            if (mismatchHint && authNavCandidate != null) {
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        val roleAr = if (targetRole == "signup") "التسجيل الجديد" else "تسجيل الدخول"
                        "الصفحة الحالية تعرض النموذج المقابل؛ الانتقال إلى $roleAr (${authNavCandidate.text.take(40)})."
                    } else {
                        val roleEn = if (targetRole == "signup") "Sign Up" else "Log In"
                        "Current screen shows the opposite form; switching to $roleEn (${authNavCandidate.text.take(40)})."
                    },
                    action = AgentActionType.CLICK,
                    targetAgentId = authNavCandidate.agentId,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }

            // 2. Scope all interactions strictly to the matching form container (avoiding opposite form on dual-form pages)
            val preferredContainerId = if (targetRole.isNotBlank()) {
                selectPreferredAuthContainer(filteredElements, targetRole)
            } else {
                null
            }

            val scopedElements = when {
                preferredContainerId != null -> filteredElements.filter { el ->
                    el.containerId == preferredContainerId ||
                        (el.containerId.isBlank() && el.authRole == targetRole)
                }
                targetRole.isNotBlank() -> filteredElements.filter { el ->
                    el.authRole != oppositeRole
                }
                else -> filteredElements
            }

            val emailInput = findEmailInput(scopedElements, targetRole)
            val emailAlreadyInDom = !email.isNullOrBlank() && scopedElements.any { el ->
                (el.tag == "input" || el.tag == "textarea") &&
                    el.value.trim().equals(email, ignoreCase = true)
            }
            val typedIntoOppositeOrUnknownEmail = !email.isNullOrBlank() && emailInput != null &&
                emailInput.agentId in recentTypedTargets
            val emailAlreadyTypedInScoped = emailAlreadyInDom || typedIntoOppositeOrUnknownEmail || (
                !email.isNullOrBlank() && emailInput == null && history.any {
                    it.decision.action == AgentActionType.TYPE &&
                        it.decision.inputText.trim().equals(email, ignoreCase = true) &&
                        it.executionResult?.success == true
                }
                )

            // 3. Enter extracted email into the matching container's email field
            if (!email.isNullOrBlank() && emailInput != null && !emailAlreadyInDom &&
                emailInput.agentId !in recentTypedTargets
            ) {
                val containerLabel = emailInput.containerHeading.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        "المحرك المحلي: إدخال البريد الإلكتروني ($email) داخل النموذج المطابق$containerLabel (${emailInput.agentId})."
                    } else {
                        "Local engine: entering email ($email) into matching form$containerLabel (${emailInput.agentId})."
                    },
                    action = AgentActionType.TYPE,
                    targetAgentId = emailInput.agentId,
                    inputText = email,
                    submit = false,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }

            // 4. Fill name, accept consent checkbox (signup), submit, or pause cleanly for password/OTP/CAPTCHA
            if (emailAlreadyInDom || emailAlreadyTypedInScoped || email == null) {
                if (!entities.personName.isNullOrBlank()) {
                    val nameInput = scopedElements.firstOrNull { el ->
                        (el.tag == "input") &&
                            el.value.isBlank() &&
                            el.agentId !in recentTypedTargets &&
                            el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio", "password", "email") &&
                            containsAny(
                                normalizeText("${el.placeholder} ${el.labelText} ${el.ariaLabel} ${el.nameAttr} ${el.autocomplete}"),
                                listOf("name", "full name", "first name", "username", "الاسم", "اسمك")
                            )
                    }
                    if (nameInput != null) {
                        return buildDecision(
                            stepNumber = stepNumber,
                            phase = "act",
                            thought = if (isAr) {
                                "إدخال الاسم (${entities.personName}) في الحقل ${nameInput.agentId}."
                            } else {
                                "Entering name (${entities.personName}) into ${nameInput.agentId}."
                            },
                            action = AgentActionType.TYPE,
                            targetAgentId = nameInput.agentId,
                            inputText = entities.personName,
                            submit = false,
                            recoveryAnalysis = recoveryNote,
                            fallbackReason = fallbackReason
                        )
                    }
                }

                // Check unchecked consent/terms checkbox within matching container (primarily for signup)
                if (targetRole != "login") {
                    val termsCheckbox = scopedElements.firstOrNull { el ->
                        (el.inputType == "checkbox" || el.role == "checkbox") &&
                            !el.isChecked &&
                            el.agentId !in recentClickedTargetsOnUrl &&
                            containsAny(
                                normalizeText("${el.text} ${el.labelText} ${el.ariaLabel} ${el.nameAttr} ${el.selector}"),
                                listOf("agree", "terms", "privacy", "policy", "accept", "اوافق", "أوافق", "الشروط", "الخصوصية")
                            )
                    }
                    if (termsCheckbox != null) {
                        return buildDecision(
                            stepNumber = stepNumber,
                            phase = "act",
                            thought = if (isAr) {
                                "الموافقة على مربع الشروط والأحكام (${termsCheckbox.agentId})."
                            } else {
                                "Accepting terms checkbox (${termsCheckbox.agentId})."
                            },
                            action = AgentActionType.CLICK,
                            targetAgentId = termsCheckbox.agentId,
                            recoveryAnalysis = recoveryNote,
                            fallbackReason = fallbackReason
                        )
                    }
                }

                // Check Password / OTP / CAPTCHA strictly within scoped container so opposite form doesn't trigger false pause
                val emptyPasswordInput = scopedElements.firstOrNull { el ->
                    el.tag == "input" &&
                        (el.inputType == "password" || el.autocomplete.contains("password")) &&
                        el.value.isBlank()
                }
                val scopedHasPasswordField = scopedElements.any { el ->
                    el.tag == "input" && (el.inputType == "password" || el.autocomplete.contains("password"))
                }
                val pageHasClassifiedPasswordInput = filteredElements.any { el ->
                    el.tag == "input" && (el.inputType == "password" || el.autocomplete.contains("password"))
                }
                val relevantPasswordPresent = if (preferredContainerId != null || pageHasClassifiedPasswordInput) {
                    scopedHasPasswordField && emptyPasswordInput != null
                } else {
                    snapshot.hasPasswordField
                }

                if (snapshot.hasCaptcha || snapshot.hasOtpField || relevantPasswordPresent) {
                    val promptMsg = when {
                        snapshot.hasCaptcha -> if (isAr) {
                            "ظهر اختبار التحقق (CAPTCHA) في الصفحة. يرجى حله داخل المتصفح ثم الضغط على «متابعة الوكيل»."
                        } else {
                            "CAPTCHA verification appeared on the page. Solve it in the browser, then tap Resume Agent."
                        }
                        snapshot.hasOtpField -> if (isAr) {
                            "يطلب الموقع رمز التحقق (OTP) المرسل لبريدك. أدخل الرمز في الصفحة ثم اضغط «متابعة الوكيل»."
                        } else {
                            "The site asks for a verification code (OTP). Enter it on the page, then tap Resume Agent."
                        }
                        else -> if (isAr) {
                            val modeAr = if (targetRole == "login") "تسجيل الدخول" else "التسجيل الجديد"
                            "تم تعبئة البريد في نموذج $modeAr ووصلنا لحقل كلمة المرور. يرجى كتابة كلمة المرور ثم الضغط على «متابعة الوكيل»."
                        } else {
                            val modeEn = if (targetRole == "login") "Log In" else "Sign Up"
                            "Email is filled in the $modeEn form and a password field is required. Enter your password on the page, then tap Resume Agent."
                        }
                    }
                    return buildDecision(
                        stepNumber = stepNumber,
                        phase = "waiting",
                        thought = promptMsg,
                        action = AgentActionType.AWAIT_USER,
                        recoveryAnalysis = recoveryNote,
                        finalSummary = promptMsg,
                        fallbackReason = fallbackReason
                    )
                }

                // Click Continue / Next / Submit inside the matching container ONLY (never opposite role or OAuth)
                if (emailAlreadyInDom || emailAlreadyTypedInScoped) {
                    val submitBtn = findAuthSubmitButton(
                        elements = scopedElements,
                        targetRole = targetRole,
                        alreadyClickedIds = recentClickedTargetsOnUrl,
                        allowOAuth = allowOAuth,
                        allowReset = allowReset
                    )
                    if (submitBtn != null) {
                        return buildDecision(
                            stepNumber = stepNumber,
                            phase = "act",
                            thought = if (isAr) {
                                "الضغط على زر المتابعة داخل النموذج المطابق (${submitBtn.text.take(40).ifBlank { submitBtn.agentId }})."
                            } else {
                                "Clicking submit button inside matching form (${submitBtn.text.take(40).ifBlank { submitBtn.agentId }})."
                            },
                            action = AgentActionType.CLICK,
                            targetAgentId = submitBtn.agentId,
                            recoveryAnalysis = recoveryNote,
                            fallbackReason = fallbackReason
                        )
                    }
                }
            }

            // 5. No email field yet in matching container -> click target auth navigation link/button
            if (emailInput == null && authNavCandidate != null) {
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        val roleAr = if (targetRole == "login") "تسجيل الدخول" else "التسجيل الجديد"
                        "المحرك المحلي: فتح نموذج $roleAr (${authNavCandidate.text.take(40)})."
                    } else {
                        val roleEn = if (targetRole == "login") "Log In" else "Sign Up"
                        "Local engine: clicking $roleEn control (${authNavCandidate.text.take(40)})."
                    },
                    action = AgentActionType.CLICK,
                    targetAgentId = authNavCandidate.agentId,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }
        }

        // Completion heuristics
        if (isConfirmationVisible && history.isNotEmpty()) {
            return buildDecision(
                stepNumber = stepNumber,
                phase = "finish",
                thought = if (isAr) "رصد المحرك المحلي رسالة التأكيد في الصفحة الحالية." else "Local engine detected confirmation on screen.",
                action = AgentActionType.FINISH,
                recoveryAnalysis = recoveryNote,
                finalSummary = if (isAr) {
                    "تم إكمال المهمة بنجاح عبر المحرك المحلي الخفيف على: ${snapshot.title.ifBlank { snapshot.url }}"
                } else {
                    "Completed via Local Lite engine on: ${snapshot.title.ifBlank { snapshot.url }}"
                },
                fallbackReason = fallbackReason
            )
        }

        if (wantsSummarizeOrRead && (snapshot.pageTextPreview.length > 80 || snapshot.semanticOutline.isNotBlank() || snapshot.metaDescription.isNotBlank())) {
            val quickSum = summarizePageOffline(
                url = snapshot.url,
                title = snapshot.title,
                contentText = snapshot.pageTextPreview,
                language = language,
                metaDescription = snapshot.metaDescription,
                semanticOutline = snapshot.semanticOutline,
                structuredDataSummary = snapshot.structuredDataSummary
            )
            return buildDecision(
                stepNumber = stepNumber,
                phase = "finish",
                thought = if (isAr) "استخلص المحرك المحلي ملخص الصفحة من بنية HTML والمحتوى مباشرة." else "Local engine extracted a deep summary from HTML structure and content.",
                action = AgentActionType.FINISH,
                recoveryAnalysis = recoveryNote,
                finalSummary = quickSum,
                fallbackReason = fallbackReason
            )
        }

        // If goal was search & open top result, check if search already happened (URL has ?q= or history already typed)
        val onSearchResultsPage = snapshot.url.contains("?q=", ignoreCase = true) ||
            snapshot.url.contains("search", ignoreCase = true) ||
            recentTypedTargets.isNotEmpty()

        if (wantsSearch && !onSearchResultsPage && !searchQuery.isNullOrBlank()) {
            val searchBox = enabledElements.firstOrNull { el ->
                (el.tag == "input" || el.tag == "textarea") &&
                    el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio") &&
                    el.agentId !in recentTypedTargets &&
                    (el.inputType == "search" ||
                        containsAny(normalizeText("${el.placeholder} ${el.text} ${el.selector}"), listOf("search", "q", "بحث", "ابحث")))
            } ?: enabledElements.firstOrNull { el ->
                (el.tag == "input" || el.tag == "textarea") &&
                    el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio") &&
                    el.agentId !in recentTypedTargets
            }

            if (searchBox != null) {
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        "المحرك المحلي: كتابة عبارة البحث «$searchQuery» في حقل البحث (${searchBox.agentId})."
                    } else {
                        "Local engine: typing search query '$searchQuery' into ${searchBox.agentId}."
                    },
                    action = AgentActionType.TYPE,
                    targetAgentId = searchBox.agentId,
                    inputText = searchQuery,
                    submit = true,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }
        }

        // If on search results and goal asks to open top/first result
        if (onSearchResultsPage && wantsOpenResult && history.none {
                it.decision.action == AgentActionType.CLICK && it.executionResult?.success == true && !it.url.contains("?q=")
            }
        ) {
            val bestLink = enabledElements
                .filter { el ->
                    (el.tag == "a" || el.role == "link") &&
                        el.text.length >= 6 &&
                        !el.href.startsWith("#") &&
                        !containsAny(normalizeText(el.text), listOf("duckduckgo", "settings", "privacy", "about", "images", "videos", "news", "maps", "الأخبار", "الصور", "الإعدادات"))
                }
                .maxByOrNull { el ->
                    scoreElementMatch(el, expandedGoalTokens, searchQuery) + if (el.href.startsWith("http")) 2.0 else 0.0
                }

            if (bestLink != null) {
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        "المحرك المحلي: فتح أنسب نتيجة متاحة (${bestLink.text.take(40)})."
                    } else {
                        "Local engine: opening top matching result (${bestLink.text.take(40)})."
                    },
                    action = AgentActionType.CLICK,
                    targetAgentId = bestLink.agentId,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }
        }

        // Check if we already navigated to a destination after clicking a result
        if (wantsOpenResult && onSearchResultsPage && !snapshot.url.contains("duckduckgo.com/?q=", ignoreCase = true) && history.size >= 2) {
            return buildDecision(
                stepNumber = stepNumber,
                phase = "finish",
                thought = if (isAr) "تم فتح الصفحة المستهدفة عبر المحرك المحلي." else "Target page opened via Local Lite engine.",
                action = AgentActionType.FINISH,
                recoveryAnalysis = recoveryNote,
                finalSummary = if (isAr) {
                    "تم فتح الصفحة: ${snapshot.title.ifBlank { snapshot.url }}"
                } else {
                    "Opened page: ${snapshot.title.ifBlank { snapshot.url }}"
                },
                fallbackReason = fallbackReason
            )
        }

        // Score form inputs or actionable buttons against expanded bilingual goal tokens
        val candidateFallbackElements = enabledElements.filter { el ->
            (oppositeRole.isBlank() || el.authRole != oppositeRole) &&
                (allowOAuth || !isOAuthControl(el)) &&
                (allowReset || !isForgotPasswordControl(el))
        }
        val emptyRequestedInput = candidateFallbackElements.firstOrNull { el ->
            (el.tag == "input" || el.tag == "textarea") &&
                el.value.isBlank() &&
                el.agentId !in recentTypedTargets &&
                el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio", "password") &&
                scoreElementMatch(el, expandedGoalTokens, quotedTarget) > 0.0
        }
        if (emptyRequestedInput != null) {
            val valueToType = inferInputValueForField(emptyRequestedInput, goal, quotedTarget, entities)
            if (valueToType.isNotBlank()) {
                return buildDecision(
                    stepNumber = stepNumber,
                    phase = "act",
                    thought = if (isAr) {
                        "المحرك المحلي: إدخال القيمة في الحقل ${emptyRequestedInput.agentId}."
                    } else {
                        "Local engine: entering value into ${emptyRequestedInput.agentId}."
                    },
                    action = AgentActionType.TYPE,
                    targetAgentId = emptyRequestedInput.agentId,
                    inputText = valueToType,
                    submit = false,
                    recoveryAnalysis = recoveryNote,
                    fallbackReason = fallbackReason
                )
            }
        }

        val bestClickable = candidateFallbackElements
            .filter { el ->
                el.agentId !in recentClickedTargetsOnUrl &&
                    (el.tag in setOf("button", "a") || el.role in setOf("button", "link", "tab"))
            }
            .map { el ->
                val base = scoreElementMatch(el, expandedGoalTokens, quotedTarget)
                val roleBonus = if (targetRole.isNotBlank() && el.authRole == targetRole) 3.0 else 0.0
                el to (base + roleBonus)
            }
            .filter { it.second > 0.5 }
            .maxByOrNull { it.second }

        if (bestClickable != null) {
            val chosen = bestClickable.first
            return buildDecision(
                stepNumber = stepNumber,
                phase = "act",
                thought = if (isAr) {
                    "المحرك المحلي: النقر على العنصر الأنسب للهدف (${chosen.text.take(40).ifBlank { chosen.agentId }})."
                } else {
                    "Local engine: clicking highest matching element (${chosen.text.take(40).ifBlank { chosen.agentId }})."
                },
                action = AgentActionType.CLICK,
                targetAgentId = chosen.agentId,
                recoveryAnalysis = recoveryNote,
                fallbackReason = fallbackReason
            )
        }

        // Scroll if page still has room and no strong interactive match in current viewport
        if (snapshot.scrollPercent < 85 && history.count { it.decision.action == AgentActionType.SCROLL } < 2) {
            return buildDecision(
                stepNumber = stepNumber,
                phase = "act",
                thought = if (isAr) "المحرك المحلي: التمرير للأسفل لاكتشاف عناصر إضافية." else "Local engine: scrolling down to reveal more elements.",
                action = AgentActionType.SCROLL,
                scrollDirection = "down",
                recoveryAnalysis = recoveryNote,
                fallbackReason = fallbackReason
            )
        }

        // If auth intent was requested and we still couldn't find a form/button after scrolling, ask user instead of falsely claiming success
        if (authIntent != AuthIntentKind.NONE) {
            val askMsg = if (isAr) {
                val labelAr = if (authIntent == AuthIntentKind.LOG_IN) "تسجيل الدخول" else "التسجيل الجديد"
                "لم يجد الوكيل زر أو نموذج $labelAr تلقائياً في هذه الصفحة. يمكنك فتح النموذج المناسب ثم الضغط على «متابعة الوكيل»."
            } else {
                val labelEn = if (authIntent == AuthIntentKind.LOG_IN) "Log In" else "Sign Up"
                "Could not locate a visible $labelEn button or form automatically on this page. Navigate to the form, then tap Resume Agent."
            }
            return buildDecision(
                stepNumber = stepNumber,
                phase = "waiting",
                thought = askMsg,
                action = AgentActionType.AWAIT_USER,
                recoveryAnalysis = recoveryNote,
                finalSummary = askMsg,
                fallbackReason = fallbackReason
            )
        }

        // Only treat as completed when the user actually asked to read/summarize the current page
        if (wantsSummarizeOrRead) {
            return buildDecision(
                stepNumber = stepNumber,
                phase = "finish",
                thought = if (isAr) "تمت قراءة الصفحة واستخلاص المحتوى بالمحرك المحلي." else "Read page content with Local Lite engine.",
                action = AgentActionType.FINISH,
                recoveryAnalysis = recoveryNote,
                finalSummary = if (isAr) {
                    "تم تحليل الصفحة (${snapshot.title.ifBlank { snapshot.url }}) بالمحرك المحلي الخفيف."
                } else {
                    "Inspected page (${snapshot.title.ifBlank { snapshot.url }}) with Local Lite engine."
                },
                fallbackReason = fallbackReason
            )
        }

        val failSummary = if (isAr) {
            "لم يتم العثور على عنصر أو نموذج مطابق لتنفيذ الهدف في هذه الصفحة (${snapshot.title.ifBlank { snapshot.url }})."
        } else {
            "Could not locate a matching control or form to complete this goal on (${snapshot.title.ifBlank { snapshot.url }})."
        }
        return buildDecision(
            stepNumber = stepNumber,
            phase = "fail",
            thought = failSummary,
            action = AgentActionType.FAIL,
            recoveryAnalysis = recoveryNote,
            finalSummary = failSummary,
            fallbackReason = fallbackReason
        )
    }

    // 2. Offline Extractive TextRank / TF Page Summarizer (with Semantic HTML awareness)
    fun summarizePageOffline(
        url: String,
        title: String,
        contentText: String,
        language: AppLanguage,
        metaDescription: String = "",
        semanticOutline: String = "",
        structuredDataSummary: String = ""
    ): String {
        val isAr = language == AppLanguage.ARABIC
        val cleaned = contentText.replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank() && metaDescription.isBlank() && semanticOutline.isBlank()) {
            return if (isAr) "لا يوجد نص مقروء لتلخيصه محلياً." else "No readable text available to summarize locally."
        }

        val combinedSource = if (cleaned.isNotBlank()) cleaned else metaDescription
        val rawSentences = combinedSource
            .split(Regex("(?<=[.!?؟؛\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 24 }
            .distinct()

        val sentences = if (rawSentences.isNotEmpty()) rawSentences else listOf(combinedSource.take(360))

        val titleTokens = tokenize(title).filter { it.length >= 2 && it !in stopWords }.toSet()
        val freq = mutableMapOf<String, Int>()
        val allTokens = tokenize(combinedSource).filter { it.length >= 3 && it !in stopWords }
        allTokens.forEach { tok -> freq[tok] = (freq[tok] ?: 0) + 1 }

        val topKeywords = freq.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val scored = sentences.mapIndexed { idx, sent ->
            val toks = tokenize(sent).filter { it.length >= 3 && it !in stopWords }
            val tfScore = toks.sumOf { (freq[it] ?: 1).toDouble() } / (toks.size.coerceAtLeast(1))
            val titleOverlap = toks.count { it in titleTokens } * 2.5
            val posBoost = if (idx == 0) 2.2 else if (idx <= 2) 1.1 else 0.0
            val lengthBoost = ln((sent.length.coerceIn(30, 260)).toDouble())
            Triple(idx, sent, tfScore + titleOverlap + posBoost + lengthBoost)
        }

        val selected = scored
            .sortedByDescending { it.third }
            .take(4)
            .sortedBy { it.first }
            .map { it.second.take(220) }

        val outlineHeadings = semanticOutline
            .lineSequence()
            .filter { it.startsWith("<h1") || it.startsWith("<h2") || it.startsWith("<h3") }
            .map { it.replace(Regex("^<h\\d>\\s*"), "").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
            .toList()

        val wordCount = combinedSource.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        return buildString {
            if (isAr) {
                appendLine("⚡ ملخص المحرك المحلي (تحليل HTML الدلالي)")
                if (title.isNotBlank()) appendLine("• الموضوع: $title")
                if (metaDescription.isNotBlank()) appendLine("• الوصف التعريفي: ${metaDescription.take(180)}")
                if (outlineHeadings.isNotEmpty()) {
                    appendLine("• أبرز أقسام HTML: ${outlineHeadings.joinToString(" | ")}")
                }
                if (structuredDataSummary.isNotBlank()) {
                    appendLine("• بيانات مهيكلة: ${structuredDataSummary.lineSequence().firstOrNull()?.take(140).orEmpty()}")
                }
                appendLine("• أبرز النقاط:")
                selected.forEach { appendLine("  - $it") }
                if (topKeywords.isNotEmpty()) {
                    appendLine("• الكلمات المفتاحية: ${topKeywords.joinToString("، ")}")
                }
                append("• حجم المقال: $wordCount كلمة تقريباً")
            } else {
                appendLine("⚡ Local Lite Offline Summary (Semantic HTML)")
                if (title.isNotBlank()) appendLine("• Topic: $title")
                if (metaDescription.isNotBlank()) appendLine("• Meta Description: ${metaDescription.take(180)}")
                if (outlineHeadings.isNotEmpty()) {
                    appendLine("• HTML Sections: ${outlineHeadings.joinToString(" | ")}")
                }
                if (structuredDataSummary.isNotBlank()) {
                    appendLine("• Structured Data: ${structuredDataSummary.lineSequence().firstOrNull()?.take(140).orEmpty()}")
                }
                appendLine("• Key Takeaways:")
                selected.forEach { appendLine("  - $it") }
                if (topKeywords.isNotEmpty()) {
                    appendLine("• Keywords: ${topKeywords.joinToString(", ")}")
                }
                append("• Article length: ~$wordCount words")
            }
        }
    }

    // 3. Smart Goal & Quick Search Suggestions
    fun suggestPrompts(
        query: String,
        currentUrl: String,
        pageTitle: String,
        isHomeMode: Boolean,
        language: AppLanguage
    ): List<PromptSuggestion> {
        val isAr = language == AppLanguage.ARABIC
        val q = query.trim()

        if (isHomeMode) {
            if (q.isBlank()) {
                return if (isAr) {
                    listOf(
                        PromptSuggestion("h1", "أخبار الذكاء الاصطناعي", "أخبار الذكاء الاصطناعي اليوم", isSearchQuery = true),
                        PromptSuggestion("h2", "تعلم Jetpack Compose", "ابحث في DuckDuckGo عن «Jetpack Compose» وافتح أول نتيجة"),
                        PromptSuggestion("h3", "موسوعة ويكيبيديا", "https://ar.m.wikipedia.org/", isSearchQuery = true),
                        PromptSuggestion("h4", "اتجاهات GitHub", "ابحث عن «trending kotlin github» وافتح أول نتيجة")
                    )
                } else {
                    listOf(
                        PromptSuggestion("h1", "AI breakthroughs", "latest AI breakthroughs today", isSearchQuery = true),
                        PromptSuggestion("h2", "Search & open result", "Search DuckDuckGo for 'Jetpack Compose' and open top result"),
                        PromptSuggestion("h3", "Wikipedia Portal", "https://en.m.wikipedia.org/", isSearchQuery = true),
                        PromptSuggestion("h4", "Kotlin releases", "Search for 'Kotlin latest release' and open the top result")
                    )
                }
            }
            return if (isAr) {
                listOf(
                    PromptSuggestion("hq1", "بحث سريع: $q", q, isSearchQuery = true),
                    PromptSuggestion("hq2", "ابحث وافتح أول نتيجة عن «$q»", "ابحث في DuckDuckGo عن «$q» ثم افتح أول نتيجة مناسبة."),
                    PromptSuggestion("hq3", "مقالة ويكيبيديا عن $q", "$q wikipedia", isSearchQuery = true)
                )
            } else {
                listOf(
                    PromptSuggestion("hq1", "Quick Search: $q", q, isSearchQuery = true),
                    PromptSuggestion("hq2", "Search & open '$q'", "Search DuckDuckGo for '$q' and open the top result."),
                    PromptSuggestion("hq3", "Wikipedia on $q", "$q wikipedia", isSearchQuery = true)
                )
            }
        }

        // Browser Goal Suggestions based on active page + partial goal
        val list = mutableListOf<PromptSuggestion>()
        if (q.isNotBlank() && q.length < 35) {
            list += if (isAr) {
                PromptSuggestion("bg1", "ابحث عن «$q» وافتح النتيجة", "ابحث عن «$q» وافتح أول نتيجة مناسبة.")
            } else {
                PromptSuggestion("bg1", "Search & open '$q'", "Search for '$q' and open the first result.")
            }
        }

        val lowerUrl = currentUrl.lowercase()
        when {
            lowerUrl.contains("duckduckgo.com/?q=") -> {
                list += if (isAr) {
                    PromptSuggestion("ctx1", "فتح أول نتيجة", "افتح أول نتيجة بحث مناسبة في الصفحة الحالية.")
                } else {
                    PromptSuggestion("ctx1", "Open top result", "Open the first relevant search result on this page.")
                }
            }
            lowerUrl.contains("sandbox.murshed.local") -> {
                list += if (isAr) {
                    PromptSuggestion("ctx2", "إضافة لوحة مفاتيح + PRO30", "أضف «لوحة مفاتيح ميكانيكية» للسلة واستخدم كود PRO30.")
                } else {
                    PromptSuggestion("ctx2", "Keyboard + PRO30", "Add Mechanical Keyboard to cart and apply PRO30.")
                }
            }
            else -> {
                val topic = pageTitle.take(24).ifBlank { if (isAr) "هذه الصفحة" else "this page" }
                list += if (isAr) {
                    PromptSuggestion("ctx3", "تلخيص $topic", "اقرأ الصفحة الحالية ولخّص أهم النقاط.")
                } else {
                    PromptSuggestion("ctx3", "Summarize $topic", "Read this page and summarize the key takeaways.")
                }
            }
        }

        list += if (isAr) {
            PromptSuggestion("bg_scroll", "تمرير وفحص الأسفل", "مرر للأسفل واقرأ العناصر المتاحة ثم لخّص النتيجة.")
        } else {
            PromptSuggestion("bg_scroll", "Scroll & inspect", "Scroll down, read visible elements, and summarize.")
        }
        return list.distinctBy { it.label }.take(4)
    }

    private fun buildDecision(
        stepNumber: Int,
        phase: String,
        thought: String,
        action: AgentActionType,
        targetAgentId: String = "",
        inputText: String = "",
        submit: Boolean = false,
        scrollDirection: String = "down",
        recoveryAnalysis: String = "",
        finalSummary: String = "",
        fallbackReason: String = ""
    ): AgentActionDecision {
        val raw = JSONObject().apply {
            put("engine", "local-lite-v1")
            if (fallbackReason.isNotBlank()) put("fallback_reason", fallbackReason.take(90))
            put("thought", thought)
            put("phase", phase)
            put("action", action.wireName)
            put("target_agent_id", targetAgentId)
            put("input_text", inputText)
            put("submit", submit)
            put("scroll_direction", scrollDirection)
            put("recovery_analysis", recoveryAnalysis)
            put("final_summary", finalSummary)
        }.toString(2)

        return AgentActionDecision(
            stepNumber = stepNumber,
            phase = phase,
            thought = thought,
            action = action,
            targetAgentId = targetAgentId,
            inputText = inputText,
            submit = submit,
            scrollDirection = scrollDirection,
            recoveryAnalysis = recoveryAnalysis,
            finalSummary = finalSummary,
            rawJson = raw
        )
    }

    private fun htmlMeta(el: DomElement): String =
        normalizeText(
            "${el.text} ${el.placeholder} ${el.labelText} ${el.ariaLabel} " +
                "${el.elementId} ${el.nameAttr} ${el.cssClasses} ${el.autocomplete} " +
                "${el.containerSignature} ${el.htmlTagSummary} ${el.href} ${el.selector}"
        )

    private fun isOAuthControl(el: DomElement): Boolean {
        val meta = htmlMeta(el)
        return containsAny(
            meta,
            listOf("google", "apple", "facebook", "github", "microsoft", "sso", "oauth", "جوجل", "غوغل", "ابل", "أبل", "فيسبوك", "مايكروسوفت")
        )
    }

    private fun isForgotPasswordControl(el: DomElement): Boolean {
        val meta = htmlMeta(el)
        return containsAny(
            meta,
            listOf("forgot", "reset password", "recover password", "lost_password", "lost-password", "نسيت كلمة", "استعادة كلمة", "نسيت الرقم السري")
        )
    }

    private fun isSignupTextCue(meta: String): Boolean = containsAny(
        normalizeText(meta),
        listOf(
            "sign up", "signup", "register", "registration", "create account", "create an account",
            "new account", "get started", "join now", "reg_email", "reg_password", "reg_username",
            "woocommerce-form-register", "register-nonce", "register__submit", "form-register",
            "تسجيل جديد", "انشاء حساب", "حساب جديد", "عضوية جديدة", "مستخدم جديد"
        )
    )

    private fun isLoginTextCue(meta: String): Boolean = containsAny(
        normalizeText(meta),
        listOf(
            "sign in", "signin", "log in", "login", "already have an account",
            "user_login", "woocommerce-form-login", "login-nonce", "login__submit", "form-login",
            "تسجيل الدخول", "تسجيل دخول", "لديك حساب", "دخول لحسابك"
        )
    )

    private fun findInactiveTargetTab(
        elements: List<DomElement>,
        targetRole: String,
        alreadyClickedIds: Set<String>
    ): DomElement? {
        return elements
            .filter { el ->
                el.agentId !in alreadyClickedIds &&
                    el.ariaSelected == false &&
                    (el.role == "tab" || el.tag in setOf("button", "a") || el.role == "button")
            }
            .firstOrNull { el ->
                if (el.authRole == targetRole) return@firstOrNull true
                val ownMeta = "${el.text} ${el.ariaLabel} ${el.elementId} ${el.nameAttr} ${el.cssClasses} ${el.href} ${el.selector}"
                when (targetRole) {
                    "signup" -> isSignupTextCue(ownMeta) && !isLoginTextCue(ownMeta)
                    "login" -> isLoginTextCue(ownMeta) && !isSignupTextCue(ownMeta)
                    else -> false
                }
            }
    }

    private fun selectPreferredAuthContainer(
        elements: List<DomElement>,
        targetRole: String
    ): String? {
        val oppositeRole = if (targetRole == "signup") "login" else "signup"
        val grouped = elements.filter { it.containerId.isNotBlank() }.groupBy { it.containerId }
        if (grouped.isEmpty()) return null

        return grouped.entries
            .map { (cid, items) ->
                var score = 0.0
                val matchedRoleCount = items.count { it.authRole == targetRole }
                val oppositeRoleCount = items.count { it.authRole == oppositeRole }
                score += matchedRoleCount * 6.0
                score -= oppositeRoleCount * 10.0

                val containerMetaText = (
                    items.map { it.containerHeading } +
                        items.map { it.containerSignature } +
                        items.map { "${it.elementId} ${it.cssClasses} ${it.nameAttr}" }
                    ).distinct().joinToString(" ")
                if (targetRole == "signup" && isSignupTextCue(containerMetaText)) score += 8.0
                if (targetRole == "login" && isLoginTextCue(containerMetaText)) score += 8.0
                if (targetRole == "signup" && isLoginTextCue(containerMetaText)) score -= 8.0
                if (targetRole == "login" && isSignupTextCue(containerMetaText)) score -= 8.0

                val hasEmailOrUserInput = items.any { el ->
                    (el.tag == "input" || el.tag == "textarea") &&
                        el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio", "password", "search")
                }
                val hasPasswordInput = items.any { it.tag == "input" && it.inputType == "password" }
                val hasSubmitBtn = items.any {
                    it.tag == "button" || it.role == "button" || (it.tag == "input" && it.inputType in setOf("submit", "button"))
                }
                if (hasEmailOrUserInput) score += 4.0
                if (hasPasswordInput) score += 2.0
                if (hasSubmitBtn) score += 2.0
                cid to score
            }
            .filter { it.second > 0.0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun findEmailInput(elements: List<DomElement>, targetRole: String = ""): DomElement? {
        val oppositeRole = when (targetRole) {
            "signup" -> "login"
            "login" -> "signup"
            else -> ""
        }
        return elements
            .filter { el ->
                (el.tag == "input" || el.tag == "textarea") &&
                    el.inputType !in setOf("hidden", "submit", "button", "checkbox", "radio", "password", "search") &&
                    (oppositeRole.isBlank() || el.authRole != oppositeRole) &&
                    (el.inputType == "email" ||
                        el.autocomplete.equals("email", ignoreCase = true) ||
                        el.autocomplete.equals("username", ignoreCase = true) ||
                        containsAny(
                            normalizeText("${el.placeholder} ${el.labelText} ${el.ariaLabel} ${el.elementId} ${el.nameAttr} ${el.cssClasses} ${el.selector}"),
                            listOf("email", "e-mail", "mail", "reg_email", "user_login", "بريد", "ايميل", "إيميل", "بريدك")
                        ))
            }
            .maxByOrNull { el ->
                var score = 0.0
                if (targetRole.isNotBlank() && el.authRole == targetRole) score += 10.0
                if (el.inputType == "email" || el.autocomplete.equals("email", ignoreCase = true)) score += 4.0
                val attrContext = "${el.containerHeading} ${el.containerSignature} ${el.elementId} ${el.cssClasses} ${el.nameAttr}"
                if (targetRole == "signup" && isSignupTextCue(attrContext)) score += 6.0
                if (targetRole == "login" && isLoginTextCue(attrContext)) score += 6.0
                score
            }
    }

    private fun findAuthNavigationElement(
        elements: List<DomElement>,
        targetRole: String,
        alreadyClickedIds: Set<String>
    ): DomElement? {
        val signupWords = listOf(
            "sign up", "signup", "register", "create account", "create an account",
            "new account", "get started", "join now", "join", "continue with email", "use email",
            "woocommerce-form-register", "reg_email", "تسجيل جديد", "انشاء حساب", "حساب جديد", "سجل الان", "اشترك", "التسجيل"
        )
        val loginWords = listOf(
            "sign in", "signin", "log in", "login", "existing account",
            "woocommerce-form-login", "user_login", "تسجيل الدخول", "تسجيل دخول", "دخول لحسابك"
        )
        val positive = if (targetRole == "login") loginWords else signupWords
        val negative = if (targetRole == "login") signupWords else loginWords

        return elements
            .filter { el ->
                el.agentId !in alreadyClickedIds &&
                    !isOAuthControl(el) &&
                    !isForgotPasswordControl(el) &&
                    (el.tag in setOf("a", "button") || el.role in setOf("button", "link", "tab"))
            }
            .map { el ->
                val meta = htmlMeta(el)
                var score = 0.0
                if (el.authRole == targetRole) score += 4.0
                if (el.role == "tab" && el.ariaSelected == false && el.authRole == targetRole) score += 6.0
                for (p in positive) {
                    if (meta.contains(normalizeText(p))) score += 5.0
                }
                val hrefMatch = if (targetRole == "login") {
                    el.href.contains("login", ignoreCase = true) || el.href.contains("signin", ignoreCase = true)
                } else {
                    el.href.contains("signup", ignoreCase = true) || el.href.contains("register", ignoreCase = true) || el.href.contains("join", ignoreCase = true)
                }
                if (hrefMatch) score += 3.0
                for (n in negative) {
                    if (meta.contains(normalizeText(n))) score -= 6.0
                }
                el to score
            }
            .filter { it.second > 1.0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun findAuthSubmitButton(
        elements: List<DomElement>,
        targetRole: String,
        alreadyClickedIds: Set<String>,
        allowOAuth: Boolean,
        allowReset: Boolean
    ): DomElement? {
        val oppositeRole = when (targetRole) {
            "signup" -> "login"
            "login" -> "signup"
            else -> ""
        }
        val genericSubmit = listOf("continue", "next", "submit", "verify", "proceed", "متابعة", "التالي", "ارسال", "تأكيد")
        val signupLabels = listOf(
            "sign up", "signup", "register", "create account", "create",
            "woocommerce-form-register__submit", "register-nonce",
            "تسجيل", "تسجيل جديد", "انشاء الحساب", "انشاء حساب"
        )
        val loginLabels = listOf(
            "sign in", "signin", "log in", "login",
            "woocommerce-form-login__submit", "login-nonce",
            "تسجيل الدخول", "تسجيل دخول", "دخول"
        )

        val preferredLabels = when (targetRole) {
            "login" -> genericSubmit + loginLabels
            "signup" -> genericSubmit + signupLabels
            else -> genericSubmit + signupLabels + loginLabels
        }
        val oppositeLabels = when (targetRole) {
            "login" -> signupLabels
            "signup" -> loginLabels
            else -> emptyList()
        }

        return elements
            .filter { el ->
                el.agentId !in alreadyClickedIds &&
                    (oppositeRole.isBlank() || el.authRole != oppositeRole) &&
                    (allowOAuth || !isOAuthControl(el)) &&
                    (allowReset || !isForgotPasswordControl(el)) &&
                    (el.tag == "button" || el.role == "button" || (el.tag == "input" && el.inputType in setOf("submit", "button")))
            }
            .map { el ->
                val meta = normalizeText(
                    "${el.text} ${el.ariaLabel} ${el.value} ${el.elementId} ${el.nameAttr} ${el.cssClasses} ${el.containerSignature} ${el.selector}"
                )
                var score = 0.0
                if (targetRole.isNotBlank() && el.authRole == targetRole) score += 6.0
                if (el.inputType == "submit") score += 2.5
                if (el.sectionContext.startsWith("form")) score += 1.5
                for (lbl in preferredLabels) {
                    if (meta.contains(normalizeText(lbl))) score += 4.0
                }
                for (opp in oppositeLabels) {
                    if (meta.contains(normalizeText(opp))) score -= 7.0
                }
                el to score
            }
            .filter { it.second > 1.5 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun expandBilingualTokens(tokens: List<String>): List<String> {
        val out = tokens.toMutableList()
        val synonymPairs = listOf(
            setOf("تسجيل", "سجل", "جديد", "انشاء", "حساب", "اشترك") to listOf("sign", "signup", "register", "create", "account", "join", "start"),
            setOf("بريد", "ايميل", "الايميل", "البريد") to listOf("email", "mail"),
            setOf("دخول") to listOf("login", "signin"),
            setOf("التالي", "متابعه", "اكمل") to listOf("next", "continue", "proceed", "submit"),
            setOf("بحث", "ابحث") to listOf("search", "find"),
            setOf("سله", "السله") to listOf("cart", "bag", "basket"),
            setOf("خصم", "كوبون", "كود") to listOf("promo", "coupon", "discount", "code"),
            setOf("اسم", "الاسم") to listOf("name", "username")
        )
        for ((triggers, expansions) in synonymPairs) {
            if (tokens.any { tok -> triggers.any { trig -> tok.contains(trig) } }) {
                out += expansions
            }
        }
        return out.distinct()
    }

    private fun scoreElementMatch(el: DomElement, goalTokens: List<String>, quoted: String?): Double {
        val haystack = htmlMeta(el)
        var score = 0.0
        if (!quoted.isNullOrBlank() && haystack.contains(normalizeText(quoted))) {
            score += 6.0
        }
        for (tok in goalTokens) {
            if (haystack.contains(tok)) score += 2.0
        }
        if (el.tag == "button" || el.role == "button") score += 0.4
        if (el.sectionContext.startsWith("main") || el.sectionContext.startsWith("article") || el.sectionContext.startsWith("form")) score += 0.3
        return score
    }

    private fun inferInputValueForField(
        el: DomElement,
        goal: String,
        quoted: String?,
        entities: com.example.model.ExtractedCommandEntities
    ): String {
        val meta = normalizeText("${el.placeholder} ${el.labelText} ${el.ariaLabel} ${el.text} ${el.nameAttr} ${el.autocomplete} ${el.selector} ${el.inputType}")
        return when {
            el.inputType == "email" || el.autocomplete == "email" ||
                containsAny(meta, listOf("email", "e-mail", "mail", "بريد", "ايميل", "إيميل")) -> entities.email ?: ""
            containsAny(meta, listOf("phone", "tel", "mobile", "جوال", "هاتف")) -> entities.phone ?: ""
            containsAny(meta, listOf("promo", "coupon", "كود", "خصم")) -> {
                Regex("\\b[A-Z0-9]{4,12}\\b").find(goal)?.value ?: "MURSHED20"
            }
            containsAny(meta, listOf("name", "الاسم", "اسم")) -> entities.personName ?: "Murshed User"
            containsAny(meta, listOf("address", "العنوان", "مدينة")) -> "Riyadh"
            !quoted.isNullOrBlank() -> quoted
            else -> extractSearchPhrase(goal) ?: ""
        }
    }

    private fun extractQuotedPhrase(goal: String): String? {
        val patterns = listOf(
            Regex("«([^»]{2,80})»"),
            Regex("\"([^\"]{2,80})\""),
            Regex("'([^']{2,80})'")
        )
        for (p in patterns) {
            val match = p.find(goal)?.groupValues?.getOrNull(1)?.trim()
            if (!match.isNullOrBlank()) return match
        }
        return null
    }

    private fun extractSearchPhrase(goal: String): String? {
        val arMatch = Regex("(?:ابحث|بحث)(?:\\s+في\\s+\\S+)?\\s+عن\\s+(.+?)(?:\\s+(?:ثم|و|واف))|$").find(goal)
        val arGroup = arMatch?.groupValues?.getOrNull(1)?.trim()
        if (!arGroup.isNullOrBlank()) return arGroup

        val enMatch = Regex("(?:search|find)(?:\\s+(?:duckduckgo|web|google))?\\s+(?:for|about)\\s+(.+?)(?:\\s+(?:and|then))|$", RegexOption.IGNORE_CASE).find(goal)
        val enGroup = enMatch?.groupValues?.getOrNull(1)?.trim()
        if (!enGroup.isNullOrBlank()) return enGroup

        return goal.trim().take(48).ifBlank { null }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(normalizeText(it)) }

    private fun tokenize(text: String): List<String> =
        normalizeText(text)
            .split(Regex("[^\\p{L}\\p{Nd}_-]+"))
            .filter { it.isNotBlank() }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "") // Strip Arabic diacritics
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
    }
}
