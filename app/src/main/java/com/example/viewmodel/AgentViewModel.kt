package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.FailureDiagnosisAnalyzer
import com.example.ai.GeminiAgentEngine
import com.example.browser.CommandIntentResolver
import com.example.browser.SandboxPages
import com.example.browser.WebViewDomBridge
import com.example.data.local.AgentDatabase
import com.example.data.local.AgentPreferencesRepository
import com.example.data.local.AgentRunEntity
import com.example.data.local.AgentRunRepository
import com.example.i18n.stringsFor
import com.example.model.ActionExecutionResult
import com.example.model.AgentActionDecision
import com.example.model.AgentActionType
import com.example.model.AgentPhase
import com.example.model.AgentStepLog
import com.example.model.AgentUiState
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.model.CommandIntentKind
import com.example.model.CommandIntentOverride
import com.example.model.MainDestination
import com.example.model.ProductivityStats
import com.example.model.PromptSuggestion
import com.example.model.QuickLinkBookmark
import com.example.model.TaskPreset
import com.example.model.WorkspaceSubTab
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AgentViewModel"

class AgentViewModel(
    private val preferencesRepository: AgentPreferencesRepository,
    private val runRepository: AgentRunRepository,
    private val domBridge: WebViewDomBridge = WebViewDomBridge(),
    private val geminiEngine: GeminiAgentEngine = GeminiAgentEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    val savedRuns: StateFlow<List<AgentRunEntity>> = runRepository.allRuns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    val productivityStats: StateFlow<ProductivityStats> = savedRuns
        .map { runs ->
            val total = runs.size
            val completed = runs.count { it.status == "completed" }
            val rate = if (total == 0) 0 else (completed * 100) / total
            ProductivityStats(
                totalRuns = total,
                completedRuns = completed,
                successRatePercent = rate,
                totalSteps = runs.sumOf { it.stepCount },
                totalRecoveries = runs.sumOf { it.recoveryCount }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ProductivityStats()
        )

    private var activeLoopJob: Job? = null
    private var initialRunUrl: String = SandboxPages.SANDBOX_URL

    init {
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    val nextEngine = if (!settings.cloudGeminiEnabled) {
                        AiEngineSource.LOCAL_LITE_FALLBACK
                    } else if (!state.cloudGeminiEnabled && settings.cloudGeminiEnabled) {
                        AiEngineSource.CLOUD_GEMINI
                    } else {
                        state.activeAiEngine
                    }
                    state.copy(
                        language = settings.language,
                        isDarkTheme = settings.isDarkTheme,
                        maxSteps = settings.maxSteps,
                        stepDelayMs = settings.stepDelayMs,
                        cloudGeminiEnabled = settings.cloudGeminiEnabled,
                        activeAiEngine = nextEngine,
                        lastFallbackReason = if (!settings.cloudGeminiEnabled) null else state.lastFallbackReason
                    )
                }
            }
        }
    }

    fun attachWebView(webView: WebView) {
        domBridge.attach(webView)
    }

    fun detachWebView(webView: WebView) {
        domBridge.detach(webView)
    }

    val bridge: WebViewDomBridge
        get() = domBridge

    fun onWebViewRendererCrashed() {
        Log.w(TAG, "WebView renderer process crashed; regenerating WebView safely.")
        _uiState.update { state ->
            state.copy(
                webViewGeneration = state.webViewGeneration + 1,
                isPageLoading = false,
                statusBanner = if (state.language == AppLanguage.ARABIC) {
                    "تمت إعادة تشغيل عارض الصفحة تلقائياً."
                } else {
                    "Page renderer recovered automatically."
                }
            )
        }
    }

    fun onWebPageStarted(url: String) {
        _uiState.update { state ->
            val keepEmptySearchBar = state.isHomeDashboardVisible && url == SandboxPages.SANDBOX_URL
            state.copy(
                currentUrl = url,
                urlInput = if (keepEmptySearchBar) state.urlInput else url,
                isPageLoading = true,
                lastWebError = null
            )
        }
    }

    fun onWebReceivedError(errorSummary: String) {
        val clean = errorSummary.trim()
        if (clean.isBlank()) return
        _uiState.update { state ->
            state.copy(lastWebError = clean)
        }
    }

    fun onWebPageFinished(url: String, title: String?) {
        _uiState.update { state ->
            val keepEmptySearchBar = state.isHomeDashboardVisible && url == SandboxPages.SANDBOX_URL
            state.copy(
                currentUrl = url,
                urlInput = if (keepEmptySearchBar) state.urlInput else url,
                pageTitle = title.orEmpty(),
                isPageLoading = false,
                loadingProgress = 100
            )
        }
        viewModelScope.launch {
            runCatching { domBridge.readScreen() }
                .onSuccess { snapshot ->
                    _uiState.update { it.copy(screenSnapshot = snapshot) }
                }
        }
    }

    fun onWebProgressChanged(progress: Int) {
        _uiState.update {
            it.copy(
                loadingProgress = progress,
                isPageLoading = progress < 100
            )
        }
    }

    fun onDestinationSelected(destination: MainDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun onWorkspaceSubTabSelected(tab: WorkspaceSubTab) {
        _uiState.update { it.copy(workspaceSubTab = tab) }
    }

    fun toggleWebViewExpanded() {
        _uiState.update { it.copy(isWebViewExpanded = !it.isWebViewExpanded) }
    }

    fun setConsoleSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isConsoleSheetOpen = open) }
    }

    fun toggleImmersiveFullscreen() {
        _uiState.update { it.copy(isImmersiveFullscreen = !it.isImmersiveFullscreen) }
    }

    fun onUrlInputChanged(value: String) {
        _uiState.update { it.copy(urlInput = value) }
    }

    fun onGoalInputChanged(value: String) {
        _uiState.update {
            it.copy(
                goalInput = value,
                commandIntentOverride = CommandIntentOverride.AUTO,
                taskPlan = null,
                pendingUserPrompt = null,
                errorMessage = null,
                failureDiagnosis = null
            )
        }
    }

    fun toggleCommandIntentOverride() {
        _uiState.update { state ->
            val resolved = CommandIntentResolver.resolve(state.goalInput, state.commandIntentOverride)
            val nextOverride = when (resolved.kind) {
                CommandIntentKind.SEARCH_WEB -> CommandIntentOverride.FORCE_OPEN_URL
                CommandIntentKind.DIRECT_OPEN_URL,
                CommandIntentKind.OPEN_URL_AND_ACT -> CommandIntentOverride.FORCE_SEARCH
                CommandIntentKind.GENERAL_GOAL -> CommandIntentOverride.AUTO
            }
            state.copy(commandIntentOverride = nextOverride)
        }
    }

    fun openHomeDashboard() {
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = true,
                isImmersiveFullscreen = false,
                urlInput = ""
            )
        }
    }

    fun openBrowserView() {
        _uiState.update { state ->
            state.copy(
                isHomeDashboardVisible = false,
                urlInput = state.urlInput.ifBlank { state.currentUrl }
            )
        }
    }

    fun onHomeSearchInputChanged(value: String) {
        _uiState.update { it.copy(homeSearchInput = value) }
    }

    fun getHomeSearchSuggestions(query: String, language: AppLanguage): List<PromptSuggestion> =
        geminiEngine.suggestPrompts(
            query = query,
            currentUrl = _uiState.value.currentUrl,
            pageTitle = _uiState.value.pageTitle,
            isHomeMode = true,
            language = language
        )

    fun getGoalSuggestions(query: String, currentUrl: String, pageTitle: String, language: AppLanguage): List<PromptSuggestion> =
        geminiEngine.suggestPrompts(
            query = query,
            currentUrl = currentUrl,
            pageTitle = pageTitle,
            isHomeMode = false,
            language = language
        )

    fun applyHomeSuggestion(suggestion: PromptSuggestion) {
        if (suggestion.isSearchQuery) {
            _uiState.update { it.copy(homeSearchInput = suggestion.text) }
            submitHomeQuickSearch()
        } else {
            _uiState.update {
                it.copy(
                    goalInput = suggestion.text,
                    homeSearchInput = "",
                    isHomeDashboardVisible = false
                )
            }
            runAgentAuto()
        }
    }

    fun submitHomeQuickSearch() {
        val raw = _uiState.value.homeSearchInput.trim()
        val target = raw.ifBlank { SandboxPages.DUCKDUCKGO_URL }
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                urlInput = target,
                homeSearchInput = ""
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(target)
        }
    }

    fun navigateToInputUrl() {
        val raw = _uiState.value.urlInput.trim()
        val target = raw.ifBlank { SandboxPages.DUCKDUCKGO_URL }
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                urlInput = target
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(target)
        }
    }

    fun openQuickLink(bookmark: QuickLinkBookmark) {
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                urlInput = bookmark.url,
                currentUrl = bookmark.url
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(bookmark.url)
        }
    }

    fun openDuckDuckGo() {
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                urlInput = SandboxPages.DUCKDUCKGO_URL,
                currentUrl = SandboxPages.DUCKDUCKGO_URL
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(SandboxPages.DUCKDUCKGO_URL)
        }
    }

    fun loadSandbox() {
        _uiState.update {
            it.copy(
                currentDestination = MainDestination.WORKSPACE,
                isHomeDashboardVisible = false,
                urlInput = SandboxPages.SANDBOX_URL,
                currentUrl = SandboxPages.SANDBOX_URL
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(SandboxPages.SANDBOX_URL)
        }
    }

    fun openUrlInAgentBrowser(url: String, prefillGoal: String? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        _uiState.update { state ->
            state.copy(
                currentDestination = MainDestination.WORKSPACE,
                isHomeDashboardVisible = false,
                urlInput = cleanUrl,
                currentUrl = cleanUrl,
                goalInput = prefillGoal ?: state.goalInput,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(cleanUrl)
        }
    }

    fun reloadWebView() {
        if (_uiState.value.isHomeDashboardVisible) return
        viewModelScope.launch { domBridge.reload() }
    }

    fun goBackWebView() {
        if (_uiState.value.isHomeDashboardVisible) return
        viewModelScope.launch {
            val wentBack = domBridge.goBack()
            if (!wentBack) {
                openHomeDashboard()
            }
        }
    }

    fun applyPreset(preset: TaskPreset) {
        val lang = _uiState.value.language
        val goal = if (lang == AppLanguage.ARABIC) preset.goalAr else preset.goalEn
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                urlInput = preset.url,
                currentUrl = preset.url,
                goalInput = goal,
                workspaceSubTab = WorkspaceSubTab.TRACE,
                isConsoleSheetOpen = false,
                errorMessage = null,
                finalSummary = null
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(preset.url)
        }
    }

    fun loadSavedRunGoal(run: AgentRunEntity) {
        _uiState.update {
            it.copy(
                isHomeDashboardVisible = false,
                currentDestination = MainDestination.WORKSPACE,
                workspaceSubTab = WorkspaceSubTab.TRACE,
                goalInput = run.goal,
                urlInput = run.startUrl,
                currentUrl = run.startUrl,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            domBridge.loadUrl(run.startUrl)
        }
    }

    fun deleteSavedRun(runId: Long) {
        viewModelScope.launch {
            runRepository.deleteRun(runId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            runRepository.clearAll()
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.ARABIC) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.ARABIC
        }
        setLanguage(next)
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkTheme(dark)
        }
    }

    fun setMaxSteps(steps: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaxSteps(steps)
        }
    }

    fun setStepDelayMs(delayMs: Long) {
        viewModelScope.launch {
            preferencesRepository.setStepDelayMs(delayMs)
        }
    }

    fun setCloudGeminiEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                cloudGeminiEnabled = enabled,
                activeAiEngine = if (enabled) AiEngineSource.CLOUD_GEMINI else AiEngineSource.LOCAL_LITE_FALLBACK,
                lastFallbackReason = if (!enabled) null else it.lastFallbackReason,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            preferencesRepository.setCloudGeminiEnabled(enabled)
        }
    }

    fun refreshScreenDom() {
        viewModelScope.launch {
            val strings = stringsFor(_uiState.value.language)
            _uiState.update {
                it.copy(
                    phase = AgentPhase.OBSERVING,
                    statusBanner = strings.statusReadingScreen,
                    workspaceSubTab = WorkspaceSubTab.DOM,
                    isConsoleSheetOpen = true,
                    errorMessage = null
                )
            }
            try {
                val snapshot = domBridge.readScreen()
                _uiState.update {
                    it.copy(
                        screenSnapshot = snapshot,
                        phase = AgentPhase.IDLE,
                        statusBanner = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "read_screen failed", e)
                _uiState.update {
                    it.copy(
                        phase = AgentPhase.ERROR,
                        errorMessage = e.localizedMessage ?: "Failed to read screen DOM.",
                        statusBanner = null
                    )
                }
            }
        }
    }

    fun highlightElement(agentId: String) {
        _uiState.update { it.copy(isConsoleSheetOpen = false) }
        val fallbackSelector = _uiState.value.screenSnapshot?.elements
            ?.firstOrNull { it.agentId == agentId }
            ?.selector
            .orEmpty()
        viewModelScope.launch {
            domBridge.highlightElement(agentId, fallbackSelector)
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                failureDiagnosis = null,
                phase = if (it.phase == AgentPhase.ERROR) AgentPhase.IDLE else it.phase
            )
        }
    }

    fun resetSession() {
        activeLoopJob?.cancel()
        activeLoopJob = null
        _uiState.update {
            it.copy(
                isRunning = false,
                phase = AgentPhase.IDLE,
                currentStep = 0,
                recoveryCount = 0,
                stepLogs = emptyList(),
                taskPlan = null,
                pendingUserPrompt = null,
                statusBanner = null,
                errorMessage = null,
                failureDiagnosis = null,
                lastWebError = null,
                finalSummary = null
            )
        }
    }

    fun stopAgent() {
        val hadSteps = _uiState.value.stepLogs.isNotEmpty()
        activeLoopJob?.cancel()
        activeLoopJob = null
        val strings = stringsFor(_uiState.value.language)
        _uiState.update {
            it.copy(
                isRunning = false,
                phase = AgentPhase.IDLE,
                pendingUserPrompt = null,
                statusBanner = strings.statusStoppedByUser
            )
        }
        if (hadSteps) {
            persistCurrentRun("stopped", strings.statusStoppedByUser)
        }
    }

    fun runSingleStep() {
        if (_uiState.value.isRunning) return
        launchAgentLoop(singleStepOnly = true, resumeExistingPlan = _uiState.value.phase == AgentPhase.WAITING_FOR_USER)
    }

    fun runAgentAuto() {
        if (_uiState.value.isRunning) return
        launchAgentLoop(singleStepOnly = false, resumeExistingPlan = _uiState.value.phase == AgentPhase.WAITING_FOR_USER)
    }

    fun resumeAfterUserInput() {
        if (_uiState.value.isRunning) return
        launchAgentLoop(singleStepOnly = false, resumeExistingPlan = true)
    }

    private fun launchAgentLoop(
        singleStepOnly: Boolean,
        resumeExistingPlan: Boolean = false
    ) {
        val state = _uiState.value
        val strings = stringsFor(state.language)
        val goal = state.goalInput.trim()
        if (goal.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = strings.missingGoalError,
                    phase = AgentPhase.ERROR
                )
            }
            return
        }

        val intent = CommandIntentResolver.resolve(goal, state.commandIntentOverride)
        val decomposedPlan = if (resumeExistingPlan && state.taskPlan != null) {
            state.taskPlan
        } else {
            CommandIntentResolver.decomposeCommand(
                rawCommand = goal,
                override = state.commandIntentOverride,
                currentUrl = state.currentUrl
            )
        }
        val wasOnHome = state.isHomeDashboardVisible
        val pendingHomeSearch = state.homeSearchInput.trim()
        val isLocalStoreGoal = goal.contains("متجر") ||
            goal.contains("سماعة") ||
            goal.contains("لوحة مفاتيح") ||
            goal.contains("مفاتيح") ||
            goal.contains("السلة") ||
            goal.contains("المفضلة") ||
            goal.contains("فأرة") ||
            goal.contains("موزع") ||
            goal.contains("مصباح") ||
            goal.contains("شحن") ||
            goal.contains("MURSHED20", ignoreCase = true) ||
            goal.contains("PRO30", ignoreCase = true) ||
            goal.contains("Headphones", ignoreCase = true) ||
            goal.contains("Keyboard", ignoreCase = true) ||
            goal.contains("Mouse", ignoreCase = true) ||
            goal.contains("Lamp", ignoreCase = true) ||
            goal.contains("USB-C", ignoreCase = true) ||
            goal.contains("Wishlist", ignoreCase = true) ||
            goal.contains("Cart", ignoreCase = true) ||
            goal.contains("Sandbox", ignoreCase = true)

        val autoStartUrl = if (resumeExistingPlan) {
            null
        } else {
            when {
                intent.kind == CommandIntentKind.DIRECT_OPEN_URL && !intent.normalizedUrl.isNullOrBlank() ->
                    intent.normalizedUrl
                intent.kind == CommandIntentKind.OPEN_URL_AND_ACT && !intent.normalizedUrl.isNullOrBlank() && state.stepLogs.isEmpty() ->
                    intent.normalizedUrl
                intent.kind == CommandIntentKind.SEARCH_WEB ->
                    CommandIntentResolver.buildDuckDuckGoSearchUrl(intent.searchQuery ?: goal)
                wasOnHome && pendingHomeSearch.isNotBlank() -> pendingHomeSearch
                wasOnHome && state.currentUrl == SandboxPages.SANDBOX_URL && !isLocalStoreGoal -> SandboxPages.DUCKDUCKGO_URL
                else -> null
            }
        }

        if (state.stepLogs.isEmpty() && !resumeExistingPlan) {
            initialRunUrl = autoStartUrl ?: state.currentUrl
        }

        activeLoopJob?.cancel()
        activeLoopJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    isHomeDashboardVisible = false,
                    homeSearchInput = "",
                    urlInput = autoStartUrl ?: it.urlInput,
                    workspaceSubTab = WorkspaceSubTab.TRACE,
                    taskPlan = decomposedPlan,
                    pendingUserPrompt = null,
                    errorMessage = null,
                    failureDiagnosis = null,
                    finalSummary = if (singleStepOnly) it.finalSummary else null
                )
            }

            if (autoStartUrl != null) {
                domBridge.loadUrl(autoStartUrl)
                delay(350L)
                var loadTicks = 0
                while (_uiState.value.isPageLoading && loadTicks < 24) {
                    delay(250L)
                    loadTicks++
                }
                val initialSnapshot = runCatching { domBridge.readScreen() }.getOrNull()
                if (initialSnapshot != null) {
                    _uiState.update { current ->
                        current.copy(
                            screenSnapshot = initialSnapshot,
                            taskPlan = current.taskPlan?.let { p ->
                                CommandIntentResolver.advanceTaskPlan(p, initialSnapshot, current.stepLogs)
                            }
                        )
                    }
                }
            }

            // Direct navigation command (e.g., user entered a domain like "wikipedia.org" or "افتح github.com")
            if (intent.kind == CommandIntentKind.DIRECT_OPEN_URL && !intent.normalizedUrl.isNullOrBlank()) {
                val snapshot = runCatching { domBridge.readScreen() }.getOrNull()
                if (snapshot != null) {
                    _uiState.update { it.copy(screenSnapshot = snapshot) }
                }
                val isAr = _uiState.value.language == AppLanguage.ARABIC
                val loadedTitle = snapshot?.title?.ifBlank { intent.detectedDomain.orEmpty() }
                    ?: intent.detectedDomain.orEmpty()
                val loadedUrl = snapshot?.url?.ifBlank { intent.normalizedUrl } ?: intent.normalizedUrl
                val summary = if (isAr) {
                    buildString {
                        append("تم الانتقال مباشرة إلى الموقع: $loadedTitle ($loadedUrl)")
                        val desc = snapshot?.metaDescription.orEmpty()
                        if (desc.isNotBlank()) append("\n• الوصف: ${desc.take(180)}")
                        if (snapshot != null) append("\n• العناصر التفاعلية: ${snapshot.elements.size}")
                    }
                } else {
                    buildString {
                        append("Navigated directly to site: $loadedTitle ($loadedUrl)")
                        val desc = snapshot?.metaDescription.orEmpty()
                        if (desc.isNotBlank()) append("\n• Description: ${desc.take(180)}")
                        if (snapshot != null) append("\n• Interactive nodes: ${snapshot.elements.size}")
                    }
                }
                val stepNum = _uiState.value.currentStep + 1
                val navDecision = AgentActionDecision(
                    stepNumber = stepNum,
                    phase = "finish",
                    thought = if (isAr) {
                        "فهم سياق الأمر كطلب فتح مباشر للدومين ${intent.detectedDomain} دون المرور بمحرك البحث."
                    } else {
                        "Resolved command context as direct domain navigation to ${intent.detectedDomain}."
                    },
                    action = AgentActionType.FINISH,
                    targetAgentId = "",
                    inputText = intent.normalizedUrl,
                    submit = false,
                    scrollDirection = "down",
                    recoveryAnalysis = "",
                    finalSummary = summary,
                    rawJson = JSONObject().apply {
                        put("intent", "direct_open_url")
                        put("target_url", intent.normalizedUrl)
                        put("action", "finish")
                        put("final_summary", summary)
                    }.toString(2)
                )
                val stepLog = AgentStepLog(
                    stepNumber = stepNum,
                    phase = AgentPhase.COMPLETED,
                    url = loadedUrl,
                    decision = navDecision,
                    executionResult = ActionExecutionResult(true, summary)
                )
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        currentStep = stepNum,
                        phase = AgentPhase.COMPLETED,
                        stepLogs = it.stepLogs + stepLog,
                        taskPlan = it.taskPlan?.let(CommandIntentResolver::markAllCompleted),
                        finalSummary = summary,
                        statusBanner = stringsFor(it.language).statusGoalCompleted
                    )
                }
                persistCurrentRun("completed", summary)
                return@launch
            }

            try {
                val maxSteps = _uiState.value.maxSteps
                val startStep = _uiState.value.currentStep + 1
                val endStep = if (singleStepOnly) startStep else maxSteps

                var shouldContinue = true
                var step = startStep
                while (step <= endStep && shouldContinue) {
                    shouldContinue = executeOneCycle(step = step, goal = goal)
                    if (!singleStepOnly && shouldContinue && step < endStep) {
                        delay(_uiState.value.stepDelayMs)
                    }
                    step++
                }

                if (!singleStepOnly && shouldContinue) {
                    val latest = _uiState.value
                    val limitMsg = stringsFor(latest.language).statusMaxStepsReached(maxSteps)
                    val diagnosis = FailureDiagnosisAnalyzer.analyzeFailure(
                        goal = goal,
                        snapshot = latest.screenSnapshot,
                        history = latest.stepLogs,
                        language = latest.language,
                        lastWebError = latest.lastWebError,
                        rawErrorMessage = limitMsg,
                        hitMaxSteps = true,
                        taskPlan = latest.taskPlan
                    )
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            phase = AgentPhase.ERROR,
                            errorMessage = diagnosis.causeSummary,
                            failureDiagnosis = diagnosis,
                            statusBanner = null
                        )
                    }
                    persistCurrentRun("max_steps", "${diagnosis.title}: ${diagnosis.causeSummary}")
                } else if (singleStepOnly && shouldContinue) {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            phase = AgentPhase.IDLE,
                            statusBanner = null
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini decision timed out", e)
                val latest = _uiState.value
                val msg = if (latest.language == AppLanguage.ARABIC) {
                    "انتهت مهلة استجابة Gemini. تحقق من الاتصال ثم أعد المحاولة."
                } else {
                    "Gemini decision timed out. Please verify network connectivity and retry."
                }
                val diagnosis = FailureDiagnosisAnalyzer.analyzeFailure(
                    goal = goal,
                    snapshot = latest.screenSnapshot,
                    history = latest.stepLogs,
                    language = latest.language,
                    lastWebError = latest.lastWebError,
                    rawErrorMessage = msg,
                    taskPlan = latest.taskPlan
                )
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        phase = AgentPhase.ERROR,
                        errorMessage = diagnosis.causeSummary,
                        failureDiagnosis = diagnosis,
                        statusBanner = null
                    )
                }
                persistCurrentRun("error", "${diagnosis.title}: ${diagnosis.causeSummary}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent loop failed", e)
                val latest = _uiState.value
                val msg = e.localizedMessage ?: "Agent execution error."
                val diagnosis = FailureDiagnosisAnalyzer.analyzeFailure(
                    goal = goal,
                    snapshot = latest.screenSnapshot,
                    history = latest.stepLogs,
                    language = latest.language,
                    lastWebError = latest.lastWebError,
                    rawErrorMessage = msg,
                    taskPlan = latest.taskPlan
                )
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        phase = AgentPhase.ERROR,
                        errorMessage = diagnosis.causeSummary,
                        failureDiagnosis = diagnosis,
                        statusBanner = null
                    )
                }
                persistCurrentRun("error", "${diagnosis.title}: ${diagnosis.causeSummary}")
            }
        }
    }

    private suspend fun executeOneCycle(step: Int, goal: String): Boolean {
        val currentState = _uiState.value
        val currentLang = currentState.language
        val cloudEnabled = currentState.cloudGeminiEnabled
        val strings = stringsFor(currentLang)

        // Wait briefly if WebView is actively loading a page navigation
        var waitTicks = 0
        while (_uiState.value.isPageLoading && waitTicks < 16) {
            delay(250L)
            waitTicks++
        }

        // 1. OBSERVE (read_screen)
        _uiState.update {
            it.copy(
                currentStep = step,
                phase = AgentPhase.OBSERVING,
                statusBanner = strings.statusReadingScreen
            )
        }
        val snapshot = domBridge.readScreen()
        _uiState.update { current ->
            current.copy(
                screenSnapshot = snapshot,
                taskPlan = current.taskPlan?.let { p ->
                    CommandIntentResolver.advanceTaskPlan(p, snapshot, current.stepLogs)
                }
            )
        }

        // 2. THINK (Cloud Gemini if enabled -> Automatic Local Lite Fallback, or Local Lite directly)
        _uiState.update {
            it.copy(
                phase = AgentPhase.THINKING,
                statusBanner = if (cloudEnabled) strings.statusThinkingGemini else strings.statusThinkingLocal
            )
        }
        val hybridResult = geminiEngine.decideNextStep(
            goal = goal,
            stepNumber = step,
            snapshot = snapshot,
            history = _uiState.value.stepLogs,
            language = currentLang,
            allowCloudGemini = cloudEnabled,
            taskPlan = _uiState.value.taskPlan
        )
        val decision = hybridResult.decision

        _uiState.update { state ->
            val switchedFromCloudError = cloudEnabled &&
                hybridResult.engineSource == AiEngineSource.LOCAL_LITE_FALLBACK &&
                !hybridResult.fallbackReason.isNullOrBlank()
            state.copy(
                activeAiEngine = hybridResult.engineSource,
                aiFallbackCount = if (switchedFromCloudError) state.aiFallbackCount + 1 else state.aiFallbackCount,
                lastFallbackReason = if (switchedFromCloudError) hybridResult.fallbackReason else state.lastFallbackReason,
                statusBanner = if (switchedFromCloudError) {
                    strings.engineFallbackBanner(hybridResult.fallbackReason.orEmpty())
                } else {
                    state.statusBanner
                }
            )
        }

        // Handle human-in-the-loop pause (password, OTP, CAPTCHA)
        if (decision.action == AgentActionType.AWAIT_USER) {
            val promptMsg = decision.finalSummary
                .ifBlank { decision.thought }
                .ifBlank { strings.awaitUserDefaultPrompt }
            val stepLog = AgentStepLog(
                stepNumber = step,
                phase = AgentPhase.WAITING_FOR_USER,
                url = snapshot.url,
                decision = decision,
                executionResult = ActionExecutionResult(true, promptMsg)
            )
            _uiState.update { current ->
                val updatedLogs = current.stepLogs + stepLog
                current.copy(
                    isRunning = false,
                    phase = AgentPhase.WAITING_FOR_USER,
                    stepLogs = updatedLogs,
                    taskPlan = current.taskPlan?.let { p ->
                        CommandIntentResolver.advanceTaskPlan(p, snapshot, updatedLogs, awaitingUser = true)
                    },
                    pendingUserPrompt = promptMsg,
                    statusBanner = null
                )
            }
            return false
        }

        // Handle terminal actions directly
        if (decision.action == AgentActionType.FINISH) {
            val summary = decision.finalSummary.ifBlank { strings.statusGoalCompleted }
            val stepLog = AgentStepLog(
                stepNumber = step,
                phase = AgentPhase.COMPLETED,
                url = snapshot.url,
                decision = decision,
                executionResult = ActionExecutionResult(true, summary)
            )
            _uiState.update {
                it.copy(
                    isRunning = false,
                    phase = AgentPhase.COMPLETED,
                    stepLogs = it.stepLogs + stepLog,
                    taskPlan = it.taskPlan?.let(CommandIntentResolver::markAllCompleted),
                    finalSummary = summary,
                    statusBanner = strings.statusGoalCompleted
                )
            }
            persistCurrentRun("completed", summary)
            return false
        }

        if (decision.action == AgentActionType.FAIL) {
            val rawSummary = decision.finalSummary.ifBlank { decision.thought }
                .ifBlank { "Agent marked goal as unachievable." }
            val latest = _uiState.value
            val diagnosis = FailureDiagnosisAnalyzer.analyzeFailure(
                goal = goal,
                snapshot = snapshot,
                history = latest.stepLogs,
                language = currentLang,
                lastWebError = latest.lastWebError,
                rawErrorMessage = rawSummary,
                taskPlan = latest.taskPlan
            )
            val stepLog = AgentStepLog(
                stepNumber = step,
                phase = AgentPhase.ERROR,
                url = snapshot.url,
                decision = decision,
                executionResult = ActionExecutionResult(false, diagnosis.causeSummary)
            )
            _uiState.update {
                it.copy(
                    isRunning = false,
                    phase = AgentPhase.ERROR,
                    stepLogs = it.stepLogs + stepLog,
                    errorMessage = diagnosis.causeSummary,
                    failureDiagnosis = diagnosis,
                    statusBanner = null
                )
            }
            persistCurrentRun("failed", "${diagnosis.title}: ${diagnosis.causeSummary}")
            return false
        }

        // 3. ACT (click / type / scroll / read_screen)
        _uiState.update {
            it.copy(
                phase = AgentPhase.ACTING,
                statusBanner = strings.statusActingDom(
                    "${decision.action.wireName} ${decision.targetAgentId}".trim()
                )
            )
        }

        val targetSelector = snapshot.elements
            .firstOrNull { it.agentId == decision.targetAgentId }
            ?.selector
            .orEmpty()

        if (decision.targetAgentId.isNotBlank()) {
            domBridge.highlightElement(decision.targetAgentId, targetSelector)
            delay(280L)
        }

        val executionResult = when (decision.action) {
            AgentActionType.READ_SCREEN -> {
                val refreshed = domBridge.readScreen()
                _uiState.update { it.copy(screenSnapshot = refreshed) }
                ActionExecutionResult(true, "Refreshed screen DOM (${refreshed.elements.size} elements).")
            }
            AgentActionType.CLICK -> domBridge.clickElement(decision.targetAgentId, targetSelector)
            AgentActionType.TYPE -> domBridge.typeText(
                agentId = decision.targetAgentId,
                text = decision.inputText,
                submit = decision.submit,
                fallbackSelector = targetSelector
            )
            AgentActionType.SCROLL -> domBridge.scrollPage(decision.scrollDirection)
            AgentActionType.AWAIT_USER, AgentActionType.FINISH, AgentActionType.FAIL -> ActionExecutionResult(true, "")
        }

        val loggedPhase = if (executionResult.success) {
            if (decision.recoveryAnalysis.isNotBlank()) AgentPhase.RECOVERING else AgentPhase.ACTING
        } else {
            AgentPhase.RECOVERING
        }

        val stepLog = AgentStepLog(
            stepNumber = step,
            phase = loggedPhase,
            url = snapshot.url,
            decision = decision,
            executionResult = executionResult
        )

        _uiState.update { state ->
            val nextLogs = state.stepLogs + stepLog
            state.copy(
                stepLogs = nextLogs,
                taskPlan = state.taskPlan?.let { p ->
                    CommandIntentResolver.advanceTaskPlan(p, snapshot, nextLogs)
                },
                recoveryCount = if (!executionResult.success) state.recoveryCount + 1 else state.recoveryCount,
                phase = if (!executionResult.success) AgentPhase.RECOVERING else state.phase,
                statusBanner = if (!executionResult.success) {
                    strings.statusRecovering(executionResult.message)
                } else {
                    state.statusBanner
                }
            )
        }

        // Refresh DOM right after action so the DOM tab and TaskPlan stay live
        delay(220L)
        runCatching { domBridge.readScreen() }
            .onSuccess { updated ->
                _uiState.update { state ->
                    state.copy(
                        screenSnapshot = updated,
                        taskPlan = state.taskPlan?.let { p ->
                            CommandIntentResolver.advanceTaskPlan(p, updated, state.stepLogs)
                        }
                    )
                }
            }

        return true
    }

    private fun persistCurrentRun(status: String, summary: String) {
        val state = _uiState.value
        if (state.stepLogs.isEmpty()) return
        val stepsArray = JSONArray()
        state.stepLogs.forEach { log ->
            stepsArray.put(
                JSONObject().apply {
                    put("step", log.stepNumber)
                    put("phase", log.phase.name)
                    put("url", log.url)
                    put("decision", runCatching { JSONObject(log.decision.rawJson) }.getOrNull() ?: log.decision.rawJson)
                    put("execution_success", log.executionResult?.success ?: true)
                    put("execution_message", log.executionResult?.message ?: "")
                }
            )
        }
        val entity = AgentRunEntity(
            goal = state.goalInput.trim(),
            startUrl = initialRunUrl,
            finalUrl = state.currentUrl,
            status = status,
            stepCount = state.stepLogs.size,
            recoveryCount = state.recoveryCount,
            finalSummary = summary,
            stepsJson = stepsArray.toString(2)
        )
        viewModelScope.launch {
            runRepository.saveRun(entity)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AgentDatabase.getInstance(application)
            return AgentViewModel(
                preferencesRepository = AgentPreferencesRepository(application),
                runRepository = AgentRunRepository(db.agentRunDao())
            ) as T
        }
    }
}
