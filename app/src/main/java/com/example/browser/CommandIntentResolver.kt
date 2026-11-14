package com.example.browser

import com.example.model.AgentActionType
import com.example.model.AgentStepLog
import com.example.model.AuthIntentKind
import com.example.model.CommandIntentKind
import com.example.model.CommandIntentOverride
import com.example.model.CommandIntentResolution
import com.example.model.CommandTaskPlan
import com.example.model.ExtractedCommandEntities
import com.example.model.ScreenSnapshot
import com.example.model.SubStepKind
import com.example.model.SubStepStatus
import com.example.model.TaskSubStep
import java.net.URLEncoder

object CommandIntentResolver {

    private val excludedFileExtensions = setOf(
        "json", "txt", "png", "jpg", "jpeg", "gif", "webp", "svg",
        "pdf", "xml", "csv", "kt", "java", "py", "js", "ts", "css",
        "zip", "rar", "apk", "aab", "md", "yaml", "yml"
    )

    private val emailRegex = Regex(
        """[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,15}"""
    )

    private val phoneRegex = Regex(
        """(?<!\w)(?:\+?\d[\d\s-]{7,15}\d)(?!\w)"""
    )

    private val explicitUrlRegex = Regex(
        """https?://[^\s"'«»()]+""",
        RegexOption.IGNORE_CASE
    )

    private val domainTokenRegex = Regex(
        """(?:^|[\s"'«»(])((?:localhost(?::\d{1,5})?|(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,15}(?::\d{1,5})?)(?:/[^\s"'«»)]*)?)""",
        RegexOption.IGNORE_CASE
    )

    private val navigationOnlyFillers = setOf(
        "افتح", "افتحي", "فتح", "انتقل", "انتقلي", "الى", "إلى", "على", "ادخل", "ادخلي",
        "اذهب", "تصفح", "زر", "اعرض", "عرض", "حمل", "موقع", "الموقع", "صفحة", "الصفحة",
        "رابط", "الرابط", "دومين", "عن",
        "open", "go", "to", "visit", "navigate", "browse", "load", "view", "show",
        "site", "website", "page", "url", "link", "the"
    )

    private val searchEngineMentions = listOf(
        "duckduckgo", "دوك دوك", "دك دك", "google search", "بحث جوجل",
        "ابحث في الويب", "ابحث في الانترنت", "ابحث في الإنترنت", "search web", "search the web"
    )

    private val searchAboutPatterns = listOf(
        Regex("""^(?:ابحث|بحث|دور|دوّر)\s+(?:عن|على)\s+"""),
        Regex("""^(?:معلومات|تفاصيل|تقييمات|تقييم|مراجعة|مراجعات|بدائل|ما هو|ما هي|وش هو)\s+(?:عن\s+|على\s+|لـ?\s*)?"""),
        Regex("""^(?:search|find|lookup|look up)\s+(?:for|about)\s+""", RegexOption.IGNORE_CASE),
        Regex("""^(?:reviews?|alternatives?|info|information|what is)\s+(?:of|for|to|about)?\s*""", RegexOption.IGNORE_CASE)
    )

    private val directNavAndInspectVerbs = listOf(
        "افتح", "انتقل", "ادخل", "اذهب", "تصفح", "زر", "اعرض", "اقرا", "اقرأ",
        "لخص", "ملخص", "حلل", "استخرج", "افحص", "سجل", "أنشئ", "انشئ", "اشترك",
        "open", "go to", "goto", "visit", "navigate", "browse", "load", "read",
        "summarize", "summary", "analyze", "inspect", "extract", "register", "sign up", "signup", "create account"
    )

    private val strongSignupKeywords = listOf(
        "تسجيل جديد", "حساب جديد", "انشاء حساب", "سجل حساب", "فتح حساب",
        "عضوية جديدة", "مستخدم جديد", "سجلني",
        "sign up", "signup", "register", "create account", "create an account",
        "new account", "join", "enroll"
    )

    private val strongLoginKeywords = listOf(
        "تسجيل الدخول", "تسجيل دخول", "سجل الدخول", "سجل دخول",
        "ادخل حسابي", "ادخل لحسابي", "دخول لحسابي", "دخول الحساب",
        "sign in", "signin", "log in", "login"
    )

    private val oauthKeywords = listOf(
        "google", "apple", "facebook", "github", "microsoft", "oauth", "sso",
        "جوجل", "غوغل", "ابل", "أبل", "فيسبوك", "جيت هب", "مايكروسوفت"
    )

    private val forgotPasswordKeywords = listOf(
        "forgot", "reset password", "recover password",
        "نسيت كلمة", "استعادة كلمة", "تغيير كلمة المرور", "اعادة تعيين كلمة"
    )

    fun normalizeAddressBarInput(raw: String): String {
        val trimmed = raw.trim().trim('"', '\'', '«', '»')
        if (trimmed.isBlank()) return SandboxPages.DUCKDUCKGO_URL
        if (trimmed == SandboxPages.SANDBOX_URL) return SandboxPages.SANDBOX_URL
        if (trimmed.equals("about:blank", ignoreCase = true)) return "about:blank"

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val schemeEnd = trimmed.indexOf("://")
            val scheme = trimmed.substring(0, schemeEnd).lowercase()
            return scheme + trimmed.substring(schemeEnd)
        }

        if (!trimmed.contains(" ") && isLikelyDomainOrLocalhost(trimmed)) {
            return addDefaultScheme(trimmed)
        }

        return buildDuckDuckGoSearchUrl(trimmed)
    }

    fun buildDuckDuckGoSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return "https://duckduckgo.com/?q=$encoded"
    }

    fun extractEntities(rawCommand: String): ExtractedCommandEntities {
        val trimmed = rawCommand.trim()
        if (trimmed.isBlank()) return ExtractedCommandEntities()

        val email = emailRegex.find(trimmed)?.value?.trim()
        val textWithoutEmail = if (email != null) trimmed.replace(email, " ") else trimmed
        val domainInfo = extractDomainToken(textWithoutEmail)

        val phone = phoneRegex.find(textWithoutEmail)?.value?.replace(Regex("\\s+"), "")?.trim()

        val quoted = mutableListOf<String>()
        listOf(
            Regex("«([^»]{2,80})»"),
            Regex("\"([^\"]{2,80})\""),
            Regex("'([^']{2,80})'")
        ).forEach { regex ->
            regex.findAll(trimmed).forEach { match ->
                val v = match.groupValues.getOrNull(1)?.trim().orEmpty()
                if (v.isNotBlank() && v != email) quoted.add(v)
            }
        }

        val nameMatch = Regex(
            """(?:باسم|الاسم|اسمي|name\s*[:=]?)\s*[«"']?([A-Za-z\u0600-\u06FF][A-Za-z\u0600-\u06FF\s]{1,36})[»"']?""",
            RegexOption.IGNORE_CASE
        ).find(textWithoutEmail)
        val personName = nameMatch?.groupValues?.getOrNull(1)?.trim()

        return ExtractedCommandEntities(
            email = email,
            phone = phone,
            personName = personName,
            targetDomain = domainInfo?.third,
            targetUrl = domainInfo?.second,
            quotedTexts = quoted.distinct(),
            authIntent = detectAuthIntent(trimmed, hasEmail = email != null)
        )
    }

    fun resolve(
        rawCommand: String,
        override: CommandIntentOverride = CommandIntentOverride.AUTO
    ): CommandIntentResolution {
        val trimmed = rawCommand.trim()
        if (trimmed.isBlank()) {
            return CommandIntentResolution(kind = CommandIntentKind.GENERAL_GOAL, effectiveGoal = "")
        }

        val entities = extractEntities(trimmed)
        val textWithoutEmails = emailRegex.replace(trimmed, " ")

        val detected = extractDomainToken(textWithoutEmails)
            ?: return CommandIntentResolution(
                kind = CommandIntentKind.GENERAL_GOAL,
                effectiveGoal = trimmed,
                entities = entities
            )

        val (rawToken, normalizedUrl, hostOnly) = detected
        val remainder = textWithoutEmails.replaceFirst(rawToken, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', '.', '،', '؛', '؟', ':', '-', '«', '»', '"', '\'')

        val remainderTokens = remainder
            .lowercase()
            .split(Regex("[\\s,،؛؟:.!]+"))
            .filter { it.isNotBlank() }

        val isPureOpen = entities.email == null && (
            remainderTokens.isEmpty() ||
                remainderTokens.all { it in navigationOnlyFillers }
            )

        val lowerTrimmed = normalizeArabic(textWithoutEmails.lowercase())
        val mentionsSearchEngine = searchEngineMentions.any { lowerTrimmed.contains(normalizeArabic(it)) }
        val hasNavOrInspectVerb = directNavAndInspectVerbs.any { lowerTrimmed.contains(normalizeArabic(it)) }
        val looksLikeSearchAbout = searchAboutPatterns.any { it.containsMatchIn(lowerTrimmed) }

        val resolvedKind = when (override) {
            CommandIntentOverride.FORCE_OPEN_URL -> {
                if (isPureOpen) CommandIntentKind.DIRECT_OPEN_URL else CommandIntentKind.OPEN_URL_AND_ACT
            }
            CommandIntentOverride.FORCE_SEARCH -> CommandIntentKind.SEARCH_WEB
            CommandIntentOverride.AUTO -> {
                when {
                    mentionsSearchEngine -> CommandIntentKind.SEARCH_WEB
                    looksLikeSearchAbout && !hasNavOrInspectVerb && entities.email == null -> CommandIntentKind.SEARCH_WEB
                    isPureOpen -> CommandIntentKind.DIRECT_OPEN_URL
                    else -> CommandIntentKind.OPEN_URL_AND_ACT
                }
            }
        }

        return when (resolvedKind) {
            CommandIntentKind.DIRECT_OPEN_URL -> CommandIntentResolution(
                kind = CommandIntentKind.DIRECT_OPEN_URL,
                detectedDomain = hostOnly,
                normalizedUrl = normalizedUrl,
                effectiveGoal = trimmed,
                searchQuery = trimmed,
                isAmbiguous = true,
                entities = entities
            )
            CommandIntentKind.OPEN_URL_AND_ACT -> CommandIntentResolution(
                kind = CommandIntentKind.OPEN_URL_AND_ACT,
                detectedDomain = hostOnly,
                normalizedUrl = normalizedUrl,
                effectiveGoal = trimmed,
                searchQuery = trimmed,
                isAmbiguous = true,
                entities = entities
            )
            CommandIntentKind.SEARCH_WEB -> CommandIntentResolution(
                kind = CommandIntentKind.SEARCH_WEB,
                detectedDomain = hostOnly,
                normalizedUrl = normalizedUrl,
                effectiveGoal = trimmed,
                searchQuery = trimmed,
                isAmbiguous = true,
                entities = entities
            )
            CommandIntentKind.GENERAL_GOAL -> CommandIntentResolution(
                kind = CommandIntentKind.GENERAL_GOAL,
                effectiveGoal = trimmed,
                entities = entities
            )
        }
    }

    fun detectAuthIntent(rawCommand: String, hasEmail: Boolean = emailRegex.containsMatchIn(rawCommand)): AuthIntentKind {
        val norm = normalizeArabic(rawCommand.lowercase())
        if (strongSignupKeywords.any { norm.contains(normalizeArabic(it)) }) {
            return AuthIntentKind.SIGN_UP
        }
        if (strongLoginKeywords.any { norm.contains(normalizeArabic(it)) }) {
            return AuthIntentKind.LOG_IN
        }
        if (hasEmail && (norm.contains("سجل") || norm.contains("التسجيل") || norm.contains("اشترك"))) {
            return AuthIntentKind.SIGN_UP
        }
        if (norm.contains("التسجيل") || norm.contains("اشترك") || norm.contains("سجل في")) {
            return AuthIntentKind.SIGN_UP
        }
        return AuthIntentKind.NONE
    }

    fun hasSignupIntent(rawCommand: String): Boolean =
        detectAuthIntent(rawCommand) == AuthIntentKind.SIGN_UP

    fun hasLoginIntent(rawCommand: String): Boolean =
        detectAuthIntent(rawCommand) == AuthIntentKind.LOG_IN

    fun explicitlyAllowsOAuth(rawCommand: String): Boolean {
        val norm = normalizeArabic(rawCommand.lowercase())
        return oauthKeywords.any { norm.contains(normalizeArabic(it)) }
    }

    fun explicitlyAllowsPasswordReset(rawCommand: String): Boolean {
        val norm = normalizeArabic(rawCommand.lowercase())
        return forgotPasswordKeywords.any { norm.contains(normalizeArabic(it)) }
    }

    fun decomposeCommand(
        rawCommand: String,
        override: CommandIntentOverride = CommandIntentOverride.AUTO,
        currentUrl: String = ""
    ): CommandTaskPlan {
        val trimmed = rawCommand.trim()
        val resolution = resolve(trimmed, override)
        val entities = resolution.entities
        val authIntent = entities.authIntent
        val steps = mutableListOf<TaskSubStep>()

        fun addStep(kind: SubStepKind, titleAr: String, titleEn: String, detail: String = "") {
            steps += TaskSubStep(
                index = steps.size + 1,
                kind = kind,
                titleAr = titleAr,
                titleEn = titleEn,
                detail = detail,
                status = if (steps.isEmpty()) SubStepStatus.IN_PROGRESS else SubStepStatus.PENDING
            )
        }

        when (resolution.kind) {
            CommandIntentKind.DIRECT_OPEN_URL -> {
                val host = resolution.detectedDomain.orEmpty()
                addStep(
                    SubStepKind.OPEN_SITE,
                    "الانتقال المباشر إلى $host",
                    "Navigate directly to $host",
                    resolution.normalizedUrl.orEmpty()
                )
            }
            CommandIntentKind.SEARCH_WEB -> {
                addStep(
                    SubStepKind.SEARCH_WEB,
                    "البحث في DuckDuckGo",
                    "Search on DuckDuckGo",
                    resolution.searchQuery.orEmpty()
                )
            }
            CommandIntentKind.OPEN_URL_AND_ACT -> {
                val host = resolution.detectedDomain.orEmpty()
                addStep(
                    SubStepKind.OPEN_SITE,
                    "فتح الموقع المستهدف: $host",
                    "Open target site: $host",
                    resolution.normalizedUrl.orEmpty()
                )
            }
            CommandIntentKind.GENERAL_GOAL -> Unit
        }

        if (resolution.kind != CommandIntentKind.DIRECT_OPEN_URL) {
            when (authIntent) {
                AuthIntentKind.SIGN_UP -> {
                    addStep(
                        SubStepKind.OPEN_SIGNUP,
                        "تحديد حاوية/تبويب التسجيل الجديد (Sign Up / Register)",
                        "Locate Sign Up / Registration container or tab"
                    )
                }
                AuthIntentKind.LOG_IN -> {
                    addStep(
                        SubStepKind.OPEN_LOGIN,
                        "تحديد حاوية/تبويب تسجيل الدخول (Sign In / Log In)",
                        "Locate Sign In / Login container or tab"
                    )
                }
                AuthIntentKind.NONE -> Unit
            }

            if (!entities.email.isNullOrBlank()) {
                addStep(
                    SubStepKind.FILL_EMAIL,
                    "إدخال البريد الإلكتروني في النموذج المطابق",
                    "Enter email into matching form",
                    entities.email
                )
            }

            if (authIntent != AuthIntentKind.NONE || !entities.email.isNullOrBlank()) {
                addStep(
                    SubStepKind.SUBMIT_FORM,
                    "الضغط على زر المتابعة في النموذج المطابق",
                    "Submit matching form step"
                )
                addStep(
                    SubStepKind.USER_VERIFICATION,
                    "التوقف للمستخدم عند كلمة المرور أو CAPTCHA/OTP",
                    "Pause for password, OTP, or CAPTCHA if requested"
                )
            } else {
                val clauses = splitCompoundClauses(trimmed)
                clauses.forEach { clause ->
                    if (clause.length >= 3) {
                        addStep(
                            SubStepKind.GENERAL_ACTION,
                            clause.take(70),
                            clause.take(70)
                        )
                    }
                }
            }
        }

        if (steps.isEmpty() && trimmed.isNotBlank()) {
            addStep(SubStepKind.GENERAL_ACTION, trimmed.take(70), trimmed.take(70))
        }

        // If the browser is already on the target domain before launching, advance step 1 automatically
        val initialPlan = CommandTaskPlan(
            rawCommand = trimmed,
            entities = entities,
            steps = steps,
            activeStepIndex = 0
        )
        return if (currentUrl.isNotBlank() && resolution.detectedDomain != null &&
            currentUrl.contains(resolution.detectedDomain, ignoreCase = true)
        ) {
            markKindCompleted(initialPlan, SubStepKind.OPEN_SITE)
        } else {
            initialPlan
        }
    }

    fun advanceTaskPlan(
        plan: CommandTaskPlan,
        snapshot: ScreenSnapshot?,
        stepLogs: List<AgentStepLog>,
        awaitingUser: Boolean = false
    ): CommandTaskPlan {
        var updated = plan
        val email = plan.entities.email
        val targetDomain = plan.entities.targetDomain

        // 1. Check OPEN_SITE completion
        if (targetDomain != null && snapshot != null &&
            snapshot.url.contains(targetDomain, ignoreCase = true)
        ) {
            updated = markKindCompleted(updated, SubStepKind.OPEN_SITE)
        }

        // 2. Check SEARCH_WEB completion
        if (snapshot != null && (snapshot.url.contains("?q=", ignoreCase = true) ||
                stepLogs.any { it.decision.action == AgentActionType.TYPE && it.executionResult?.success == true })
        ) {
            updated = markKindCompleted(updated, SubStepKind.SEARCH_WEB)
        }

        // 3. Check OPEN_SIGNUP / OPEN_LOGIN completion using container roles & active tabs
        val hasMatchingSignupInput = snapshot?.elements?.any { el ->
            el.isVisible && (el.tag == "input" || el.tag == "textarea") &&
                (el.authRole == "signup" || el.authRole.isBlank()) &&
                (el.inputType == "email" ||
                    el.autocomplete.equals("email", ignoreCase = true) ||
                    containsEmailCue("${el.placeholder} ${el.labelText} ${el.ariaLabel} ${el.nameAttr} ${el.selector}"))
        } == true

        val hasMatchingLoginInput = snapshot?.elements?.any { el ->
            el.isVisible && (el.tag == "input" || el.tag == "textarea") &&
                (el.authRole == "login" || el.authRole.isBlank()) &&
                (el.inputType == "email" || el.inputType == "password" ||
                    el.autocomplete.equals("email", ignoreCase = true) ||
                    containsEmailCue("${el.placeholder} ${el.labelText} ${el.ariaLabel} ${el.nameAttr} ${el.selector}"))
        } == true

        val clickedSignupRecently = stepLogs.any { log ->
            log.decision.action == AgentActionType.CLICK &&
                log.executionResult?.success == true &&
                (log.decision.thought.contains("Sign up", ignoreCase = true) ||
                    log.decision.thought.contains("التسجيل") ||
                    log.decision.thought.contains("حساب جديد"))
        }

        val clickedLoginRecently = stepLogs.any { log ->
            log.decision.action == AgentActionType.CLICK &&
                log.executionResult?.success == true &&
                (log.decision.thought.contains("Sign in", ignoreCase = true) ||
                    log.decision.thought.contains("Log in", ignoreCase = true) ||
                    log.decision.thought.contains("تسجيل الدخول"))
        }

        val inactiveSignupTabStillPresent = snapshot?.elements?.any {
            it.isVisible && it.authRole == "signup" && it.ariaSelected == false
        } == true
        val inactiveLoginTabStillPresent = snapshot?.elements?.any {
            it.isVisible && it.authRole == "login" && it.ariaSelected == false
        } == true

        if ((hasMatchingSignupInput && !inactiveSignupTabStillPresent) ||
            snapshot?.authFormHint == "signup" ||
            snapshot?.authFormHint == "tabbed_signup_active" ||
            clickedSignupRecently
        ) {
            updated = markKindCompleted(updated, SubStepKind.OPEN_SIGNUP)
        }

        if ((hasMatchingLoginInput && !inactiveLoginTabStillPresent) ||
            snapshot?.authFormHint == "login" ||
            snapshot?.authFormHint == "tabbed_login_active" ||
            clickedLoginRecently
        ) {
            updated = markKindCompleted(updated, SubStepKind.OPEN_LOGIN)
        }

        // 4. Check FILL_EMAIL completion
        if (!email.isNullOrBlank()) {
            val emailFilledInDom = snapshot?.elements?.any { el ->
                el.value.trim().equals(email, ignoreCase = true)
            } == true
            val emailTypedSuccessfully = stepLogs.any { log ->
                log.decision.action == AgentActionType.TYPE &&
                    log.decision.inputText.trim().equals(email, ignoreCase = true) &&
                    log.executionResult?.success == true
            }
            if (emailFilledInDom || emailTypedSuccessfully) {
                updated = markKindCompleted(updated, SubStepKind.FILL_EMAIL)
            }
        }

        // 5. Check SUBMIT_FORM completion (clicked continue/submit after email was filled)
        val fillEmailDone = updated.steps.any { it.kind == SubStepKind.FILL_EMAIL && it.status == SubStepStatus.COMPLETED }
        val submittedAfterEmail = fillEmailDone && stepLogs.any { log ->
            log.decision.action == AgentActionType.CLICK &&
                log.executionResult?.success == true &&
                log.stepNumber > (stepLogs.firstOrNull {
                    it.decision.action == AgentActionType.TYPE && it.decision.inputText.equals(email, ignoreCase = true)
                }?.stepNumber ?: 0)
        }
        val targetRole = when (plan.entities.authIntent) {
            AuthIntentKind.SIGN_UP -> "signup"
            AuthIntentKind.LOG_IN -> "login"
            AuthIntentKind.NONE -> ""
        }
        val scopedPasswordVisible = if (snapshot != null && targetRole.isNotBlank() &&
            snapshot.elements.any { it.tag == "input" && it.inputType == "password" }
        ) {
            snapshot.elements.any {
                it.isVisible && it.tag == "input" &&
                    (it.inputType == "password" || it.autocomplete.contains("password")) &&
                    (it.authRole == targetRole || it.authRole.isBlank())
            }
        } else {
            snapshot?.hasPasswordField == true
        }
        if (submittedAfterEmail || (scopedPasswordVisible && awaitingUser) || snapshot?.hasOtpField == true || snapshot?.hasCaptcha == true) {
            if (fillEmailDone || email.isNullOrBlank()) {
                updated = markKindCompleted(updated, SubStepKind.SUBMIT_FORM)
            }
        }

        // 6. If awaiting user (password / OTP / CAPTCHA), mark USER_VERIFICATION as WAITING_FOR_USER
        if (awaitingUser) {
            val newSteps = updated.steps.map { step ->
                if (step.kind == SubStepKind.USER_VERIFICATION ||
                    (updated.steps.none { it.kind == SubStepKind.USER_VERIFICATION } && step.index - 1 == updated.activeStepIndex)
                ) {
                    step.copy(status = SubStepStatus.WAITING_FOR_USER)
                } else {
                    step
                }
            }
            val waitIdx = newSteps.indexOfFirst { it.status == SubStepStatus.WAITING_FOR_USER }
                .takeIf { it >= 0 } ?: updated.activeStepIndex
            return updated.copy(steps = newSteps, activeStepIndex = waitIdx)
        }

        return normalizeActiveStep(updated)
    }

    fun markAllCompleted(plan: CommandTaskPlan): CommandTaskPlan {
        val doneSteps = plan.steps.map { it.copy(status = SubStepStatus.COMPLETED) }
        return plan.copy(steps = doneSteps, activeStepIndex = (doneSteps.lastIndex).coerceAtLeast(0))
    }

    private fun markKindCompleted(plan: CommandTaskPlan, kind: SubStepKind): CommandTaskPlan {
        val nextSteps = plan.steps.map { step ->
            if (step.kind == kind) step.copy(status = SubStepStatus.COMPLETED) else step
        }
        return normalizeActiveStep(plan.copy(steps = nextSteps))
    }

    private fun normalizeActiveStep(plan: CommandTaskPlan): CommandTaskPlan {
        val firstPending = plan.steps.indexOfFirst {
            it.status != SubStepStatus.COMPLETED && it.status != SubStepStatus.FAILED
        }
        if (firstPending < 0) {
            return plan.copy(activeStepIndex = (plan.steps.lastIndex).coerceAtLeast(0))
        }
        val normalized = plan.steps.mapIndexed { idx, step ->
            when {
                step.status == SubStepStatus.COMPLETED || step.status == SubStepStatus.FAILED -> step
                idx == firstPending -> step.copy(status = SubStepStatus.IN_PROGRESS)
                else -> step.copy(status = SubStepStatus.PENDING)
            }
        }
        return plan.copy(steps = normalized, activeStepIndex = firstPending)
    }

    private fun splitCompoundClauses(raw: String): List<String> {
        val cleaned = raw.trim()
        val parts = cleaned
            .split(Regex("""\s*(?:،|؛|\bthen\b|\band then\b|\bثم\b|\bوبعدها\b|\bبعد ذلك\b)\s*""", RegexOption.IGNORE_CASE))
            .flatMap { segment ->
                // Also split on Arabic " و" before common imperative verbs
                segment.split(Regex("""\s+و(?=(?:سجل|سجّل|ادخل|افتح|انشئ|أنشئ|اكتب|اضغط|ابحث|لخص|اقرأ|اشترك))"""))
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return parts.distinct().take(4)
    }

    private fun containsEmailCue(meta: String): Boolean {
        val m = normalizeArabic(meta.lowercase())
        return m.contains("email") || m.contains("e-mail") || m.contains("بريد") || m.contains("ايميل")
    }

    private fun extractDomainToken(text: String): Triple<String, String, String>? {
        val explicitMatch = explicitUrlRegex.find(text)
        if (explicitMatch != null) {
            val cleaned = cleanTrailingPunctuation(explicitMatch.value)
            val normalized = normalizeAddressBarInput(cleaned)
            val host = extractHost(normalized)
            return Triple(cleaned, normalized, host)
        }

        for (match in domainTokenRegex.findAll(text)) {
            val candidate = cleanTrailingPunctuation(match.groupValues[1])
            if (isLikelyDomainOrLocalhost(candidate)) {
                val normalized = addDefaultScheme(candidate)
                val host = extractHost(normalized)
                return Triple(candidate, normalized, host)
            }
        }
        return null
    }

    private fun cleanTrailingPunctuation(token: String): String =
        token.trimEnd('.', ',', '،', '؛', '؟', '!', ':', ';', ')', ']', '»', '"', '\'')

    private fun isLikelyDomainOrLocalhost(token: String): Boolean {
        val clean = token.trim()
        if (clean.isBlank() || clean.contains(" ") || clean.contains("@")) return false
        val hostPart = clean.substringBefore("/").substringBefore("?").substringBefore("#")
        val hostNoPort = hostPart.substringBefore(":")
        if (hostNoPort.equals("localhost", ignoreCase = true)) return true
        if (!hostNoPort.contains(".")) return false
        if (hostNoPort.startsWith(".") || hostNoPort.endsWith(".")) return false
        val parts = hostNoPort.split(".")
        if (parts.any { it.isBlank() }) return false
        val tld = parts.last().lowercase()
        if (parts.all { it.all(Char::isDigit) }) {
            return parts.size == 4 && parts.all { (it.toIntOrNull() ?: 999) in 0..255 }
        }
        val hasPathOrPort = clean.contains("/") || hostPart.contains(":")
        if (!hasPathOrPort && tld in excludedFileExtensions) return false
        return tld.length in 2..15 && tld.all { it.isLetter() }
    }

    private fun addDefaultScheme(domainOrPath: String): String {
        val hostNoPort = domainOrPath.substringBefore("/").substringBefore("?").substringBefore(":")
        val isLocal = hostNoPort.equals("localhost", ignoreCase = true) ||
            hostNoPort.startsWith("127.") ||
            hostNoPort.startsWith("192.168.") ||
            hostNoPort.startsWith("10.")
        val scheme = if (isLocal) "http://" else "https://"
        return scheme + domainOrPath
    }

    private fun extractHost(url: String): String {
        val noScheme = url.substringAfter("://", url)
        return noScheme.substringBefore("/").substringBefore("?").substringBefore("#").lowercase()
    }

    private fun normalizeArabic(text: String): String = text
        .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
}
