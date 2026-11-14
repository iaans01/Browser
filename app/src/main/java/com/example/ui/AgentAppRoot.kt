package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.i18n.stringsFor
import com.example.model.AppLanguage
import com.example.model.HubTab
import com.example.model.MainDestination
import com.example.ui.components.ExtensionSideToolSheet
import com.example.ui.components.rememberManagedWebView
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MurshedHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MurshedAgentTheme
import com.example.viewmodel.AgentViewModel
import com.example.viewmodel.MurshedHubViewModel

@Composable
fun AgentAppRoot(
    viewModel: AgentViewModel
) {
    val context = LocalContext.current
    val hubViewModel: MurshedHubViewModel = viewModel(
        factory = MurshedHubViewModel.Factory(context.applicationContext as Application)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hubState by hubViewModel.uiState.collectAsStateWithLifecycle()
    val savedRuns by viewModel.savedRuns.collectAsStateWithLifecycle()
    val productivityStats by viewModel.productivityStats.collectAsStateWithLifecycle()
    val strings = stringsFor(uiState.language)
    val layoutDirection = if (uiState.language == AppLanguage.ARABIC) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    val pinnedExtensions = remember(hubState.installedExtensionIds, hubState.pinnedExtensionIds) {
        val pinned = hubViewModel.extensionCatalog.filter {
            it.id in hubState.installedExtensionIds && it.id in hubState.pinnedExtensionIds
        }
        if (pinned.isNotEmpty()) {
            pinned
        } else {
            hubViewModel.extensionCatalog.filter { it.id in hubState.installedExtensionIds }.take(4)
        }
    }

    val homeSuggestions = remember(uiState.homeSearchInput, uiState.language) {
        viewModel.getHomeSearchSuggestions(uiState.homeSearchInput, uiState.language)
    }

    val goalSuggestions = remember(uiState.goalInput, uiState.currentUrl, uiState.pageTitle, uiState.language) {
        viewModel.getGoalSuggestions(
            query = uiState.goalInput,
            currentUrl = uiState.currentUrl,
            pageTitle = uiState.pageTitle,
            language = uiState.language
        )
    }

    val webView = rememberManagedWebView(
        generation = uiState.webViewGeneration,
        initialUrl = uiState.currentUrl,
        onAttachWebView = viewModel::attachWebView,
        onDetachWebView = viewModel::detachWebView,
        onPageStarted = viewModel::onWebPageStarted,
        onPageFinished = viewModel::onWebPageFinished,
        onProgressChanged = viewModel::onWebProgressChanged,
        onRendererCrashed = viewModel::onWebViewRendererCrashed,
        onWebError = viewModel::onWebReceivedError
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MurshedAgentTheme(darkTheme = uiState.isDarkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (uiState.currentDestination) {
                    MainDestination.WORKSPACE -> {
                        WorkspaceScreen(
                            webView = webView,
                            uiState = uiState,
                            stats = productivityStats,
                            recentRuns = savedRuns.take(3),
                            strings = strings,
                            onUrlInputChanged = viewModel::onUrlInputChanged,
                            onNavigateUrl = viewModel::navigateToInputUrl,
                            onHomeDashboardClick = viewModel::openHomeDashboard,
                            onOpenBrowserClick = viewModel::openBrowserView,
                            onHomeSearchInputChanged = viewModel::onHomeSearchInputChanged,
                            onSubmitHomeQuickSearch = viewModel::submitHomeQuickSearch,
                            onOpenQuickLink = viewModel::openQuickLink,
                            onResumeRun = viewModel::loadSavedRunGoal,
                            onLoadSandbox = viewModel::loadSandbox,
                            onReloadWebView = viewModel::reloadWebView,
                            onGoBackWebView = viewModel::goBackWebView,
                            onGoalInputChanged = viewModel::onGoalInputChanged,
                            onToggleCommandIntent = viewModel::toggleCommandIntentOverride,
                            onRunAuto = viewModel::runAgentAuto,
                            onRunStep = viewModel::runSingleStep,
                            onResumeAfterUser = viewModel::resumeAfterUserInput,
                            onStopAgent = viewModel::stopAgent,
                            onResetSession = viewModel::resetSession,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleImmersiveFullscreen = viewModel::toggleImmersiveFullscreen,
                            onSetConsoleSheetOpen = viewModel::setConsoleSheetOpen,
                            onSubTabSelected = viewModel::onWorkspaceSubTabSelected,
                            onReadScreenClick = viewModel::refreshScreenDom,
                            onHighlightElement = viewModel::highlightElement,
                            onApplyPreset = viewModel::applyPreset,
                            onDismissError = viewModel::dismissError,
                            pinnedExtensions = pinnedExtensions,
                            homeSuggestions = homeSuggestions,
                            goalSuggestions = goalSuggestions,
                            onApplyHomeSuggestion = viewModel::applyHomeSuggestion,
                            onLaunchExtension = { extId ->
                                hubViewModel.openSideExtension(
                                    extensionId = extId,
                                    activeUrl = uiState.currentUrl,
                                    domBridge = viewModel.bridge,
                                    language = uiState.language
                                )
                            },
                            onOpenHub = { tab ->
                                tab?.let(hubViewModel::selectTab)
                                viewModel.onDestinationSelected(MainDestination.HUB)
                            },
                            onOpenHistory = {
                                viewModel.onDestinationSelected(MainDestination.HISTORY)
                            },
                            onOpenSettings = {
                                viewModel.onDestinationSelected(MainDestination.SETTINGS)
                            }
                        )
                    }

                    MainDestination.HUB -> {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                MurshedHubScreen(
                                    hubViewModel = hubViewModel,
                                    language = uiState.language,
                                    strings = strings,
                                    onNavigateBack = {
                                        viewModel.onDestinationSelected(MainDestination.WORKSPACE)
                                    },
                                    onOpenInAgentBrowser = { url, goal ->
                                        viewModel.openUrlInAgentBrowser(url, goal)
                                    },
                                    onLaunchSideExtension = { extId ->
                                        hubViewModel.openSideExtension(
                                            extensionId = extId,
                                            activeUrl = uiState.currentUrl,
                                            domBridge = viewModel.bridge,
                                            language = uiState.language
                                        )
                                    }
                                )
                            }
                        }
                    }

                    MainDestination.HISTORY -> {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                HistoryScreen(
                                    runs = savedRuns,
                                    strings = strings,
                                    onLoadGoal = viewModel::loadSavedRunGoal,
                                    onDeleteRun = viewModel::deleteSavedRun,
                                    onClearAll = viewModel::clearAllHistory,
                                    onNavigateBack = {
                                        viewModel.onDestinationSelected(MainDestination.WORKSPACE)
                                    }
                                )
                            }
                        }
                    }

                    MainDestination.SETTINGS -> {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                SettingsScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onLanguageSelected = viewModel::setLanguage,
                                    onThemeSelected = viewModel::setDarkTheme,
                                    onMaxStepsChanged = viewModel::setMaxSteps,
                                    onStepDelayChanged = viewModel::setStepDelayMs,
                                    onCloudGeminiToggled = viewModel::setCloudGeminiEnabled,
                                    onNavigateBack = {
                                        viewModel.onDestinationSelected(MainDestination.WORKSPACE)
                                    }
                                )
                            }
                        }
                    }
                }

                // Side Extension Tool Sheet (works seamlessly over Browser OR Extension Store)
                hubState.activeSideExtensionId?.let { activeExtId ->
                    ExtensionSideToolSheet(
                        extensionId = activeExtId,
                        uiState = hubState,
                        activeUrl = uiState.currentUrl,
                        domSnapshot = uiState.screenSnapshot,
                        domBridge = viewModel.bridge,
                        language = uiState.language,
                        strings = strings,
                        hubViewModel = hubViewModel,
                        onReadScreen = viewModel::refreshScreenDom,
                        onHighlightElement = viewModel::highlightElement,
                        onOpenInBrowser = { url, goal ->
                            viewModel.openUrlInAgentBrowser(url, goal)
                        },
                        onOpenExtensionStore = {
                            hubViewModel.selectTab(HubTab.STORE)
                            viewModel.onDestinationSelected(MainDestination.HUB)
                        },
                        onDismiss = hubViewModel::closeSideExtension
                    )
                }
            }
        }
    }
}
