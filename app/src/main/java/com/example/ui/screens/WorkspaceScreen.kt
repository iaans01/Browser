package com.example.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.browser.CommandIntentResolver
import com.example.browser.SandboxPages
import com.example.data.local.AgentRunEntity
import com.example.i18n.AppStrings
import com.example.model.AgentPhase
import com.example.model.AgentUiState
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.model.CommandIntentKind
import com.example.model.FailureCategory
import com.example.model.FailureDiagnosis
import com.example.model.HubTab
import com.example.model.MurshedExtension
import com.example.model.ProductivityStats
import com.example.model.PromptSuggestion
import com.example.model.QuickLinkBookmark
import com.example.model.TaskPreset
import com.example.model.WorkspaceSubTab
import com.example.ui.components.CommandTaskPlanCard
import com.example.ui.components.DomElementCard
import com.example.ui.components.FailureDiagnosisCard
import com.example.ui.components.LiveBrowserViewport
import com.example.ui.components.PhaseStatusChip
import com.example.ui.components.PresetCard
import com.example.ui.components.ProductivityHomeDashboard
import com.example.ui.components.StepLogCard
import com.example.ui.components.extensionAccentColor
import com.example.ui.components.extensionIconFor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAgentSemanticColors

private enum class DomSheetMode {
    NODES,
    HTML,
    JSON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    webView: WebView,
    uiState: AgentUiState,
    stats: ProductivityStats,
    recentRuns: List<AgentRunEntity>,
    strings: AppStrings,
    onUrlInputChanged: (String) -> Unit,
    onNavigateUrl: () -> Unit,
    onHomeDashboardClick: () -> Unit,
    onOpenBrowserClick: () -> Unit,
    onHomeSearchInputChanged: (String) -> Unit,
    onSubmitHomeQuickSearch: () -> Unit,
    onOpenQuickLink: (QuickLinkBookmark) -> Unit,
    onResumeRun: (AgentRunEntity) -> Unit,
    onLoadSandbox: () -> Unit,
    onReloadWebView: () -> Unit,
    onGoBackWebView: () -> Unit,
    onGoalInputChanged: (String) -> Unit,
    onToggleCommandIntent: () -> Unit,
    onRunAuto: () -> Unit,
    onRunStep: () -> Unit,
    onResumeAfterUser: () -> Unit,
    onStopAgent: () -> Unit,
    onResetSession: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleImmersiveFullscreen: () -> Unit,
    onSetConsoleSheetOpen: (Boolean) -> Unit,
    onSubTabSelected: (WorkspaceSubTab) -> Unit,
    onReadScreenClick: () -> Unit,
    onHighlightElement: (String) -> Unit,
    onApplyPreset: (TaskPreset) -> Unit,
    onDismissError: () -> Unit,
    pinnedExtensions: List<MurshedExtension>,
    homeSuggestions: List<PromptSuggestion>,
    goalSuggestions: List<PromptSuggestion>,
    onApplyHomeSuggestion: (PromptSuggestion) -> Unit,
    onLaunchExtension: (String) -> Unit,
    onOpenHub: (HubTab?) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current

    BackHandler(
        enabled = uiState.isConsoleSheetOpen || uiState.isImmersiveFullscreen || !uiState.isHomeDashboardVisible
    ) {
        when {
            uiState.isConsoleSheetOpen -> onSetConsoleSheetOpen(false)
            uiState.isImmersiveFullscreen -> onToggleImmersiveFullscreen()
            !uiState.isHomeDashboardVisible -> onGoBackWebView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact Top Browser Toolbar (hidden in 100% immersive fullscreen mode)
            if (!uiState.isImmersiveFullscreen) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row 1: Browser navigation + Productive Home + DuckDuckGo Quick Search / URL Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onGoBackWebView,
                                enabled = !uiState.isHomeDashboardVisible,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("web_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.backWebBtn,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onHomeDashboardClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_dashboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = strings.homeBtn,
                                    tint = if (uiState.isHomeDashboardVisible) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            val trimmedInput = uiState.urlInput.trim()
                            val looksLikeUrl = trimmedInput.isNotBlank() &&
                                (trimmedInput == SandboxPages.SANDBOX_URL ||
                                    !CommandIntentResolver.normalizeAddressBarInput(trimmedInput)
                                        .startsWith("https://duckduckgo.com/?q="))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 10.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = strings.ddgBtn,
                                            tint = semantics.observe,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (uiState.urlInput.isBlank()) {
                                                Text(
                                                    text = strings.urlHint,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            BasicTextField(
                                                value = uiState.urlInput,
                                                onValueChange = onUrlInputChanged,
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(onSearch = { onNavigateUrl() }),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("url_input_field")
                                            )
                                        }
                                        if (uiState.urlInput.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onUrlInputChanged("") },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("url_clear_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = strings.resetBtn,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = onNavigateUrl,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("url_go_button")
                                        ) {
                                            Icon(
                                                imageVector = if (looksLikeUrl) Icons.Filled.PlayArrow else Icons.Filled.Search,
                                                contentDescription = strings.goBtn,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = onReloadWebView,
                                enabled = !uiState.isHomeDashboardVisible,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("web_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = strings.reloadBtn,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (uiState.isHomeDashboardVisible) {
                                        onOpenBrowserClick()
                                    } else {
                                        onToggleImmersiveFullscreen()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_webview_expand_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInFull,
                                    contentDescription = strings.expandWebView,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Row 2: Slim Agent Phase + Quick Nav (read_screen, Sandbox, History, Settings, AR/EN)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PhaseStatusChip(phase = uiState.phase, strings = strings)

                            val isLocalEngine = !uiState.cloudGeminiEnabled || uiState.activeAiEngine == AiEngineSource.LOCAL_LITE_FALLBACK
                            val engineColor = if (isLocalEngine) semantics.recover else semantics.observe
                            val engineLabel = when {
                                !uiState.cloudGeminiEnabled -> strings.engineLocalManualLabel
                                uiState.activeAiEngine == AiEngineSource.LOCAL_LITE_FALLBACK -> strings.engineLocalLabel
                                else -> strings.engineCloudLabel
                            }
                            Surface(
                                color = engineColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, engineColor.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(99.dp),
                                modifier = Modifier.testTag("active_ai_engine_chip")
                            ) {
                                Text(
                                    text = engineLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = engineColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                AssistChip(
                                    onClick = onReadScreenClick,
                                    label = {
                                        Text(
                                            text = strings.readScreenBtn,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = strings.readScreenBtn,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("read_screen_chip")
                                )

                                IconButton(
                                    onClick = { onOpenHub(HubTab.STORE) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("sandbox_home_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Extension,
                                        contentDescription = strings.hubTabStore,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onOpenHistory,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("nav_history")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = strings.navHistory,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("nav_settings")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = strings.navSettings,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                TextButton(
                                    onClick = onToggleLanguage,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("language_quick_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = strings.languageSectionTitle,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = if (uiState.language == AppLanguage.ARABIC) "EN" else "عربي",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        // Row 3 (When Browsing): In-Browser Quick Extensions Bar
                        AnimatedVisibility(visible = !uiState.isHomeDashboardVisible) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("browser_quick_extension_bar"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${strings.extensionsBarLabel}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                pinnedExtensions.forEach { ext ->
                                    val isAr = uiState.language == AppLanguage.ARABIC
                                    val accent = extensionAccentColor(ext.accentRole)
                                    AssistChip(
                                        onClick = { onLaunchExtension(ext.id) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = extensionIconFor(ext.id),
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (isAr) ext.shortNameAr else ext.shortNameEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag("quick_ext_chip_${ext.id}")
                                    )
                                }
                                AssistChip(
                                    onClick = { onOpenHub(HubTab.STORE) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Extension,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = strings.manageExtensionsBtn,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_bar_manage_extensions")
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            }

            // Main Viewport: Productive Start Dashboard OR Full-Screen Live Browser
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(
                    targetState = uiState.isHomeDashboardVisible,
                    label = "homeBrowserCrossfade"
                ) { showHome ->
                    if (showHome) {
                        ProductivityHomeDashboard(
                            language = uiState.language,
                            strings = strings,
                            quickSearchInput = uiState.homeSearchInput,
                            homeSuggestions = homeSuggestions,
                            stats = stats,
                            recentRuns = recentRuns,
                            onQuickSearchInputChanged = onHomeSearchInputChanged,
                            onSubmitQuickSearch = onSubmitHomeQuickSearch,
                            onApplyHomeSuggestion = onApplyHomeSuggestion,
                            onOpenBrowser = onOpenBrowserClick,
                            onOpenQuickLink = { bookmark ->
                                if (bookmark.isSandbox) {
                                    onOpenHub(HubTab.STORE)
                                } else {
                                    onOpenQuickLink(bookmark)
                                }
                            },
                            onOpenHubTab = onOpenHub,
                            onResumeRun = onResumeRun,
                            onApplyPreset = onApplyPreset,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LiveBrowserViewport(
                            webView = webView,
                            loadingProgress = uiState.loadingProgress,
                            isPageLoading = uiState.isPageLoading,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Immersive Fullscreen Floating Restore Pill
                if (!uiState.isHomeDashboardVisible && uiState.isImmersiveFullscreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(99.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            PhaseStatusChip(phase = uiState.phase, strings = strings)
                            IconButton(
                                onClick = { onSetConsoleSheetOpen(true) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_open_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = strings.tabTrace
                                )
                            }
                            IconButton(
                                onClick = onToggleImmersiveFullscreen,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("exit_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloseFullscreen,
                                    contentDescription = strings.collapseWebView
                                )
                            }
                        }
                    }
                }

                // Floating Task Plan / Waiting-for-User / Status / Error / Completion Overlay Toast right above bottom dock
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedVisibility(visible = uiState.taskPlan != null && !uiState.isHomeDashboardVisible) {
                        uiState.taskPlan?.let { activePlan ->
                            CommandTaskPlanCard(
                                plan = activePlan,
                                language = uiState.language,
                                strings = strings
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.phase == AgentPhase.WAITING_FOR_USER && !uiState.isRunning) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            border = BorderStroke(1.5.dp, semantics.recover.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("waiting_for_user_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TouchApp,
                                        contentDescription = strings.awaitUserTitle,
                                        tint = semantics.recover
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.awaitUserTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = semantics.recover
                                        )
                                        Text(
                                            text = uiState.pendingUserPrompt ?: strings.awaitUserDefaultPrompt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onResumeAfterUser,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = semantics.recover
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("resume_after_user_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = strings.resumeAgentBtn,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.resumeAgentBtn, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = onStopAgent,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("pause_stop_agent_button")
                                    ) {
                                        Text(strings.stopBtn, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.isRunning && uiState.statusBanner != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.statusBanner.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onSetConsoleSheetOpen(true) }) {
                                        Text(strings.tabTrace)
                                    }
                                }
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.failureDiagnosis != null || uiState.errorMessage != null) {
                        val activeDiagnosis = uiState.failureDiagnosis ?: FailureDiagnosis(
                            category = FailureCategory.UNKNOWN,
                            title = strings.errorTitle,
                            causeSummary = uiState.errorMessage.orEmpty()
                        )
                        FailureDiagnosisCard(
                            diagnosis = activeDiagnosis,
                            strings = strings,
                            onRetry = {
                                onDismissError()
                                onRunAuto()
                            },
                            onInspectDom = {
                                onSubTabSelected(WorkspaceSubTab.DOM)
                                onSetConsoleSheetOpen(true)
                            },
                            onResumeAfterFix = {
                                onDismissError()
                                onRunAuto()
                            },
                            onDismiss = onDismissError
                        )
                    }

                    AnimatedVisibility(visible = uiState.finalSummary != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, semantics.act.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_completed_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = strings.finalResultTitle,
                                    tint = semantics.act
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.finalResultTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = semantics.act
                                    )
                                    Text(
                                        text = uiState.finalSummary.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = { onSetConsoleSheetOpen(true) }) {
                                    Text("JSON")
                                }
                            }
                        }
                    }
                }
            }

            // Compact Bottom Agent Goal & Controls Dock (hidden in 100% immersive mode)
            if (!uiState.isImmersiveFullscreen) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.goalInput,
                            onValueChange = onGoalInputChanged,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = strings.goalHint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agent_goal_input")
                        )

                        val resolvedIntent = remember(uiState.goalInput, uiState.commandIntentOverride) {
                            CommandIntentResolver.resolve(uiState.goalInput, uiState.commandIntentOverride)
                        }
                        val detectedEmail = resolvedIntent.entities.email
                        AnimatedVisibility(
                            visible = !uiState.isRunning && (resolvedIntent.detectedDomain != null || !detectedEmail.isNullOrBlank())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("command_intent_chip"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (resolvedIntent.detectedDomain != null) {
                                    val domain = resolvedIntent.detectedDomain
                                    val isSearchMode = resolvedIntent.kind == CommandIntentKind.SEARCH_WEB
                                    val intentLabel = when (resolvedIntent.kind) {
                                        CommandIntentKind.DIRECT_OPEN_URL -> strings.intentDirectOpen(domain)
                                        CommandIntentKind.OPEN_URL_AND_ACT -> strings.intentOpenAndAct(domain)
                                        CommandIntentKind.SEARCH_WEB -> strings.intentSearchWeb
                                        CommandIntentKind.GENERAL_GOAL -> ""
                                    }
                                    val intentAccent = if (isSearchMode) semantics.recover else semantics.act
                                    Surface(
                                        color = intentAccent.copy(alpha = 0.13f),
                                        border = BorderStroke(1.dp, intentAccent.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(99.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSearchMode) Icons.Filled.Search else Icons.Filled.OpenInBrowser,
                                                contentDescription = null,
                                                tint = intentAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = intentLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = intentAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = onToggleCommandIntent,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("toggle_command_intent_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isSearchMode) strings.switchToOpenBtn else strings.switchToSearchBtn,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!detectedEmail.isNullOrBlank()) {
                                    Surface(
                                        color = semantics.think.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, semantics.think.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(99.dp),
                                        modifier = Modifier.testTag("detected_email_chip")
                                    ) {
                                        Text(
                                            text = strings.entityEmailLabel(detectedEmail),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semantics.think,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = !uiState.isRunning && !uiState.isHomeDashboardVisible && goalSuggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("goal_suggestion_chips"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                goalSuggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { onGoalInputChanged(suggestion.text) },
                                        label = {
                                            Text(
                                                text = suggestion.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag("goal_suggestion_${suggestion.id}")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isRunning) {
                                Button(
                                    onClick = onStopAgent,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stop_agent_button")
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = strings.stopBtn)
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.stopBtn, maxLines = 1)
                                }
                            } else {
                                Button(
                                    onClick = onRunAuto,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("run_agent_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = strings.runAgentBtn)
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.runAgentBtn, maxLines = 1)
                                }

                                FilledTonalButton(
                                    onClick = onRunStep,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.testTag("step_agent_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = strings.stepOnceBtn,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(strings.stepOnceBtn, maxLines = 1)
                                }
                            }

                            FilledTonalButton(
                                onClick = { onSetConsoleSheetOpen(true) },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("open_telemetry_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = strings.tabTrace,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "JSON (${uiState.stepLogs.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }

                            if (!uiState.isRunning && uiState.stepLogs.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onResetSession,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.testTag("reset_session_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteSweep,
                                        contentDescription = strings.resetBtn,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Telemetry ModalBottomSheet for JSON Loop, DOM Tree (read_screen), and Presets
    if (uiState.isConsoleSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onSetConsoleSheetOpen(false) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.testTag("telemetry_bottom_sheet")
        ) {
            AgentConsoleSheetContent(
                uiState = uiState,
                strings = strings,
                onSubTabSelected = onSubTabSelected,
                onReadScreenClick = onReadScreenClick,
                onHighlightElement = onHighlightElement,
                onApplyPreset = onApplyPreset
            )
        }
    }
}

@Composable
private fun AgentConsoleSheetContent(
    uiState: AgentUiState,
    strings: AppStrings,
    onSubTabSelected: (WorkspaceSubTab) -> Unit,
    onReadScreenClick: () -> Unit,
    onHighlightElement: (String) -> Unit,
    onApplyPreset: (TaskPreset) -> Unit
) {
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current
    var domFilter by remember { mutableStateOf("") }
    var htmlSearchQuery by remember { mutableStateOf("") }
    var domSheetMode by remember { mutableStateOf(DomSheetMode.NODES) }
    var copiedHtmlNotice by remember { mutableStateOf(false) }
    val traceListState = rememberLazyListState()

    LaunchedEffect(uiState.stepLogs.size) {
        if (uiState.stepLogs.isNotEmpty()) {
            traceListState.animateScrollToItem(uiState.stepLogs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workspace_subtabs")
        ) {
            WorkspaceSubTab.entries.forEachIndexed { index, tab ->
                val label = when (tab) {
                    WorkspaceSubTab.TRACE -> "${strings.tabTrace} (${uiState.stepLogs.size})"
                    WorkspaceSubTab.DOM -> "${strings.tabDom} (${uiState.screenSnapshot?.elements?.size ?: 0})"
                    WorkspaceSubTab.PRESETS -> strings.tabPresets
                }
                SegmentedButton(
                    selected = uiState.workspaceSubTab == tab,
                    onClick = { onSubTabSelected(tab) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = WorkspaceSubTab.entries.size
                    ),
                    modifier = Modifier.testTag("subtab_${tab.name.lowercase()}")
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (uiState.workspaceSubTab) {
                WorkspaceSubTab.TRACE -> {
                    if (uiState.stepLogs.isEmpty()) {
                        EmptyTelemetryState(
                            title = strings.emptyTraceTitle,
                            body = strings.emptyTraceBody
                        )
                    } else {
                        LazyColumn(
                            state = traceListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.stepLogs,
                                key = { it.stepNumber }
                            ) { log ->
                                StepLogCard(log = log, strings = strings)
                            }
                        }
                    }
                }

                WorkspaceSubTab.DOM -> {
                    val snapshot = uiState.screenSnapshot
                    val elements = snapshot?.elements.orEmpty()
                    val filtered = remember(elements, domFilter) {
                        if (domFilter.isBlank()) {
                            elements
                        } else {
                            val q = domFilter.trim().lowercase()
                            elements.filter {
                                it.agentId.lowercase().contains(q) ||
                                    it.tag.lowercase().contains(q) ||
                                    it.text.lowercase().contains(q) ||
                                    it.placeholder.lowercase().contains(q) ||
                                    it.labelText.lowercase().contains(q) ||
                                    it.sectionContext.lowercase().contains(q) ||
                                    it.elementId.lowercase().contains(q) ||
                                    it.cssClasses.lowercase().contains(q) ||
                                    it.nameAttr.lowercase().contains(q) ||
                                    it.selector.lowercase().contains(q) ||
                                    it.containerSignature.lowercase().contains(q) ||
                                    it.htmlTagSummary.lowercase().contains(q)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onReadScreenClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sheet_refresh_dom_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = strings.readScreenBtn,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(strings.readScreenBtn)
                            }

                            if (snapshot != null) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = domSheetMode == DomSheetMode.NODES,
                                        onClick = { domSheetMode = DomSheetMode.NODES },
                                        label = { Text(strings.tabDom, maxLines = 1) },
                                        modifier = Modifier.testTag("dom_mode_nodes_chip")
                                    )
                                    FilterChip(
                                        selected = domSheetMode == DomSheetMode.HTML,
                                        onClick = { domSheetMode = DomSheetMode.HTML },
                                        label = { Text(strings.htmlTabLabel, maxLines = 1) },
                                        modifier = Modifier.testTag("dom_mode_html_chip")
                                    )
                                    FilterChip(
                                        selected = domSheetMode == DomSheetMode.JSON,
                                        onClick = { domSheetMode = DomSheetMode.JSON },
                                        label = { Text("JSON", maxLines = 1) },
                                        modifier = Modifier.testTag("toggle_raw_dom_json_button")
                                    )
                                }
                            }
                        }

                        if (snapshot == null) {
                            EmptyTelemetryState(
                                title = strings.emptyDomTitle,
                                body = strings.emptyDomBody
                            )
                        } else when (domSheetMode) {
                            DomSheetMode.JSON -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(snapshot.rawDomJson))
                                        },
                                        modifier = Modifier.testTag("copy_raw_dom_json_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = strings.copyJson,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.copyJson)
                                    }
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(semantics.codeBackground)
                                            .border(1.dp, semantics.codeBorder, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        item {
                                            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                                Text(
                                                    text = snapshot.rawDomJson,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = JetBrainsMonoFontFamily
                                                    ),
                                                    color = semantics.observe
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            DomSheetMode.HTML -> {
                                val displayedHtml = remember(snapshot.rawHtmlSource, htmlSearchQuery) {
                                    val source = snapshot.rawHtmlSource
                                    if (htmlSearchQuery.isBlank() || source.isBlank()) {
                                        source
                                    } else {
                                        val q = htmlSearchQuery.trim()
                                        source.lineSequence()
                                            .filter { it.contains(q, ignoreCase = true) }
                                            .joinToString("\n")
                                            .ifBlank {
                                                // Fallback when HTML is single-line minified: show matching snippets
                                                val idx = source.indexOf(q, ignoreCase = true)
                                                if (idx >= 0) {
                                                    val start = (idx - 160).coerceAtLeast(0)
                                                    val end = (idx + 640).coerceAtMost(source.length)
                                                    "..." + source.substring(start, end) + "..."
                                                } else {
                                                    ""
                                                }
                                            }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = htmlSearchQuery,
                                        onValueChange = { htmlSearchQuery = it },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                        placeholder = { Text(strings.htmlSearchHint) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("html_search_input")
                                    )
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(snapshot.rawHtmlSource))
                                            copiedHtmlNotice = true
                                        },
                                        enabled = snapshot.rawHtmlSource.isNotBlank(),
                                        modifier = Modifier.testTag("copy_html_source_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = strings.copyHtmlBtn,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (copiedHtmlNotice) strings.copiedHtmlToast else strings.copyHtmlBtn)
                                    }
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("html_viewer_list")
                                ) {
                                    if (snapshot.metaDescription.isNotBlank() ||
                                        snapshot.pageLanguage.isNotBlank() ||
                                        snapshot.canonicalUrl.isNotBlank() ||
                                        snapshot.structuredDataSummary.isNotBlank()
                                    ) {
                                        item(key = "html_meta_card") {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, semantics.observe.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = strings.htmlMetadataTitle,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = semantics.observe
                                                    )
                                                    if (snapshot.pageLanguage.isNotBlank()) {
                                                        Text("lang: ${snapshot.pageLanguage}", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    if (snapshot.canonicalUrl.isNotBlank()) {
                                                        Text(
                                                            text = "canonical: ${snapshot.canonicalUrl}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (snapshot.metaDescription.isNotBlank()) {
                                                        Text(snapshot.metaDescription, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (snapshot.structuredDataSummary.isNotBlank()) {
                                                        Text(
                                                            text = snapshot.structuredDataSummary,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                            color = semantics.act
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (snapshot.semanticOutline.isNotBlank()) {
                                        item(key = "html_outline_card") {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, semantics.think.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = strings.htmlOutlineTitle,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = semantics.think
                                                    )
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                                            Text(
                                                                text = snapshot.semanticOutline,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item(key = "html_source_block") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${strings.htmlSourceTitle} (${snapshot.rawHtmlSource.length} chars)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (displayedHtml.isBlank()) {
                                                Text(
                                                    text = strings.emptyHtmlBody,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(semantics.codeBackground)
                                                            .border(1.dp, semantics.codeBorder, RoundedCornerShape(10.dp))
                                                            .horizontalScroll(rememberScrollState())
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(
                                                            text = displayedHtml.take(24000),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = JetBrainsMonoFontFamily
                                                            ),
                                                            color = semantics.observe
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DomSheetMode.NODES -> {
                                if (elements.isEmpty()) {
                                    EmptyTelemetryState(
                                        title = strings.emptyDomTitle,
                                        body = strings.emptyDomBody
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = domFilter,
                                        onValueChange = { domFilter = it },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Filled.Search, contentDescription = null)
                                        },
                                        placeholder = { Text(strings.domSearchHint) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dom_filter_input")
                                    )
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        items(
                                            items = filtered,
                                            key = { it.agentId }
                                        ) { node ->
                                            DomElementCard(
                                                element = node,
                                                strings = strings,
                                                onHighlightClick = onHighlightElement
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                WorkspaceSubTab.PRESETS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = SandboxPages.presets,
                            key = { it.id }
                        ) { preset ->
                            PresetCard(
                                preset = preset,
                                language = uiState.language,
                                strings = strings,
                                onApply = { onApplyPreset(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTelemetryState(
    title: String,
    body: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
