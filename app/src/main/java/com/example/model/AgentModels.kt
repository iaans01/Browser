package com.example.model

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ARABIC
    }
}

enum class AgentPhase {
    IDLE,
    OBSERVING,
    THINKING,
    ACTING,
    RECOVERING,
    WAITING_FOR_USER,
    COMPLETED,
    ERROR
}

enum class AgentActionType(val wireName: String) {
    READ_SCREEN("read_screen"),
    CLICK("click"),
    TYPE("type"),
    SCROLL("scroll"),
    AWAIT_USER("await_user"),
    FINISH("finish"),
    FAIL("fail");

    companion object {
        fun fromWire(value: String?): AgentActionType =
            entries.firstOrNull { it.wireName.equals(value?.trim(), ignoreCase = true) }
                ?: READ_SCREEN
    }
}

data class DomElement(
    val agentId: String,
    val tag: String,
    val role: String,
    val text: String,
    val placeholder: String,
    val value: String,
    val inputType: String,
    val href: String,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val selector: String,
    val ariaLabel: String = "",
    val labelText: String = "",
    val sectionContext: String = "",
    val nameAttr: String = "",
    val autocomplete: String = "",
    val isRequired: Boolean = false,
    val isChecked: Boolean = false,
    val containerId: String = "",
    val containerHeading: String = "",
    val authRole: String = "",
    val ariaSelected: Boolean? = null,
    val elementId: String = "",
    val cssClasses: String = "",
    val containerSignature: String = "",
    val htmlTagSummary: String = ""
)

data class ScreenSnapshot(
    val url: String,
    val title: String,
    val scrollPercent: Int,
    val pageTextPreview: String,
    val elements: List<DomElement>,
    val rawDomJson: String,
    val pageLanguage: String = "",
    val metaDescription: String = "",
    val canonicalUrl: String = "",
    val semanticOutline: String = "",
    val structuredDataSummary: String = "",
    val htmlSnippet: String = "",
    val rawHtmlSource: String = "",
    val hasPasswordField: Boolean = false,
    val hasOtpField: Boolean = false,
    val hasCaptcha: Boolean = false,
    val authFormHint: String = "",
    val pageErrors: List<String> = emptyList(),
    val invalidFields: List<String> = emptyList(),
    val networkErrorSummary: String = ""
)

enum class FailureCategory {
    SITE_VALIDATION,
    NETWORK_HTTP,
    AUTH_OR_CAPTCHA,
    MISSING_USER_DATA,
    ELEMENT_NOT_FOUND,
    LOOP_STALLED,
    AI_SERVICE,
    UNKNOWN
}

data class FailureDiagnosis(
    val category: FailureCategory,
    val title: String,
    val causeSummary: String,
    val evidenceDetail: String = "",
    val suggestedFixes: List<String> = emptyList(),
    val canResumeAfterFix: Boolean = false,
    val canRetry: Boolean = true
)

data class AgentActionDecision(
    val stepNumber: Int,
    val phase: String,
    val thought: String,
    val action: AgentActionType,
    val targetAgentId: String,
    val inputText: String,
    val submit: Boolean,
    val scrollDirection: String,
    val recoveryAnalysis: String,
    val finalSummary: String,
    val rawJson: String
)

data class ActionExecutionResult(
    val success: Boolean,
    val message: String,
    val pageReloaded: Boolean = false
)

data class AgentStepLog(
    val stepNumber: Int,
    val phase: AgentPhase,
    val url: String,
    val decision: AgentActionDecision,
    val executionResult: ActionExecutionResult?,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TaskPreset(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val url: String,
    val goalAr: String,
    val goalEn: String,
    val isSandbox: Boolean = false
)

data class QuickLinkBookmark(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val url: String,
    val isSandbox: Boolean = false
)

data class ProductivityStats(
    val totalRuns: Int = 0,
    val completedRuns: Int = 0,
    val successRatePercent: Int = 0,
    val totalSteps: Int = 0,
    val totalRecoveries: Int = 0
)

enum class WorkspaceSubTab {
    TRACE,
    DOM,
    PRESETS
}

enum class MainDestination {
    WORKSPACE,
    HUB,
    HISTORY,
    SETTINGS
}

enum class AiEngineSource {
    CLOUD_GEMINI,
    LOCAL_LITE_FALLBACK
}

enum class CommandIntentKind {
    DIRECT_OPEN_URL,
    OPEN_URL_AND_ACT,
    SEARCH_WEB,
    GENERAL_GOAL
}

enum class CommandIntentOverride {
    AUTO,
    FORCE_OPEN_URL,
    FORCE_SEARCH
}

enum class AuthIntentKind {
    NONE,
    SIGN_UP,
    LOG_IN
}

data class ExtractedCommandEntities(
    val email: String? = null,
    val phone: String? = null,
    val personName: String? = null,
    val targetDomain: String? = null,
    val targetUrl: String? = null,
    val quotedTexts: List<String> = emptyList(),
    val authIntent: AuthIntentKind = AuthIntentKind.NONE
)

enum class SubStepKind {
    OPEN_SITE,
    SEARCH_WEB,
    OPEN_SIGNUP,
    OPEN_LOGIN,
    FILL_EMAIL,
    FILL_FORM,
    SUBMIT_FORM,
    USER_VERIFICATION,
    GENERAL_ACTION
}

enum class SubStepStatus {
    PENDING,
    IN_PROGRESS,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED
}

data class TaskSubStep(
    val index: Int,
    val kind: SubStepKind,
    val titleAr: String,
    val titleEn: String,
    val detail: String = "",
    val status: SubStepStatus = SubStepStatus.PENDING
)

data class CommandTaskPlan(
    val rawCommand: String,
    val entities: ExtractedCommandEntities,
    val steps: List<TaskSubStep>,
    val activeStepIndex: Int = 0
)

data class CommandIntentResolution(
    val kind: CommandIntentKind,
    val detectedDomain: String? = null,
    val normalizedUrl: String? = null,
    val effectiveGoal: String = "",
    val searchQuery: String? = null,
    val isAmbiguous: Boolean = false,
    val entities: ExtractedCommandEntities = ExtractedCommandEntities()
)

data class PromptSuggestion(
    val id: String,
    val label: String,
    val text: String,
    val isSearchQuery: Boolean = false
)

data class AgentUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val isDarkTheme: Boolean = true,
    val maxSteps: Int = 12,
    val stepDelayMs: Long = 900L,
    val cloudGeminiEnabled: Boolean = true,
    val currentDestination: MainDestination = MainDestination.WORKSPACE,
    val workspaceSubTab: WorkspaceSubTab = WorkspaceSubTab.TRACE,
    val isWebViewExpanded: Boolean = false,
    val isConsoleSheetOpen: Boolean = false,
    val isImmersiveFullscreen: Boolean = false,
    val isHomeDashboardVisible: Boolean = true,
    val homeSearchInput: String = "",
    val webViewGeneration: Int = 0,
    val currentUrl: String = "https://sandbox.murshed.local/",
    val urlInput: String = "",
    val pageTitle: String = "",
    val loadingProgress: Int = 100,
    val isPageLoading: Boolean = false,
    val goalInput: String = "",
    val commandIntentOverride: CommandIntentOverride = CommandIntentOverride.AUTO,
    val taskPlan: CommandTaskPlan? = null,
    val phase: AgentPhase = AgentPhase.IDLE,
    val isRunning: Boolean = false,
    val currentStep: Int = 0,
    val recoveryCount: Int = 0,
    val activeAiEngine: AiEngineSource = AiEngineSource.CLOUD_GEMINI,
    val aiFallbackCount: Int = 0,
    val lastFallbackReason: String? = null,
    val screenSnapshot: ScreenSnapshot? = null,
    val stepLogs: List<AgentStepLog> = emptyList(),
    val statusBanner: String? = null,
    val pendingUserPrompt: String? = null,
    val errorMessage: String? = null,
    val failureDiagnosis: FailureDiagnosis? = null,
    val lastWebError: String? = null,
    val finalSummary: String? = null
)
