package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.SandboxPages
import com.example.data.local.AgentRunEntity
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.HubTab
import com.example.model.ProductivityStats
import com.example.model.PromptSuggestion
import com.example.model.QuickLinkBookmark
import com.example.model.TaskPreset
import com.example.ui.theme.LocalAgentSemanticColors
import com.example.ui.theme.SpaceGroteskFontFamily
import androidx.compose.material3.AssistChip

@Composable
fun ProductivityHomeDashboard(
    language: AppLanguage,
    strings: AppStrings,
    quickSearchInput: String,
    homeSuggestions: List<PromptSuggestion>,
    stats: ProductivityStats,
    recentRuns: List<AgentRunEntity>,
    onQuickSearchInputChanged: (String) -> Unit,
    onSubmitQuickSearch: () -> Unit,
    onApplyHomeSuggestion: (PromptSuggestion) -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenQuickLink: (QuickLinkBookmark) -> Unit,
    onOpenHubTab: (HubTab) -> Unit,
    onResumeRun: (AgentRunEntity) -> Unit,
    onApplyPreset: (TaskPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val glowPrimary = semantics.observe.copy(alpha = 0.11f)
    val glowSecondary = semantics.think.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary, Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.12f),
                        radius = size.maxDimension * 0.65f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.45f),
                        radius = size.maxDimension * 0.55f
                    )
                )
            }
            .testTag("productivity_home_dashboard"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero DuckDuckGo Quick Search + Browser Switcher Card
            item(key = "hero_search") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = semantics.observe.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(99.dp),
                                border = BorderStroke(1.dp, semantics.observe.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = semantics.observe,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = strings.homeHeroBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semantics.observe,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onOpenBrowser,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_open_browser_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = strings.openBrowserBtn,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.openBrowserBtn,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.homeHeroTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.homeHeroSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = quickSearchInput,
                            onValueChange = onQuickSearchInputChanged,
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = strings.ddgBtn,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (quickSearchInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQuickSearchInputChanged("") },
                                        modifier = Modifier.testTag("home_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = strings.resetBtn
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = strings.homeQuickSearchHint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSubmitQuickSearch() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_ddg_search_input")
                        )

                        Button(
                            onClick = onSubmitQuickSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_ddg_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = strings.homeQuickSearchBtn,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(strings.homeQuickSearchBtn)
                        }

                        if (homeSuggestions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = strings.suggestionsLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .testTag("home_suggestion_chips"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    homeSuggestions.forEach { suggestion ->
                                        AssistChip(
                                            onClick = { onApplyHomeSuggestion(suggestion) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (suggestion.isSearchQuery) Icons.Filled.Search else Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (suggestion.isSearchQuery) semantics.observe else semantics.think,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = suggestion.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1
                                                )
                                            },
                                            modifier = Modifier.testTag("home_suggestion_${suggestion.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 Live Murshed Hub Shortcuts (Weather, News, Tech Tools, Redesigned Store)
            item(key = "murshed_hub_shortcuts") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.hubTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HubShortcutTile(
                            label = strings.hubTabWeather,
                            icon = Icons.Filled.WbSunny,
                            accent = semantics.observe,
                            onClick = { onOpenHubTab(HubTab.WEATHER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_hub_weather_card")
                        )
                        HubShortcutTile(
                            label = strings.hubTabNews,
                            icon = Icons.Filled.Newspaper,
                            accent = semantics.think,
                            onClick = { onOpenHubTab(HubTab.NEWS) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_hub_news_card")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HubShortcutTile(
                            label = strings.hubTabTools,
                            icon = Icons.Filled.Build,
                            accent = semantics.recover,
                            onClick = { onOpenHubTab(HubTab.TOOLS) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_hub_tools_card")
                        )
                        HubShortcutTile(
                            label = strings.hubTabStore,
                            icon = Icons.Filled.Extension,
                            accent = semantics.act,
                            onClick = { onOpenHubTab(HubTab.STORE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_hub_store_card")
                        )
                    }
                }
            }

            // 2. Productivity Metrics Overview
            item(key = "stats_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.homeStatsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = strings.statTotalRuns,
                            value = stats.totalRuns.toString(),
                            icon = Icons.Filled.History,
                            accent = semantics.observe,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statSuccessRate,
                            value = "${stats.successRatePercent}%",
                            icon = Icons.Filled.CheckCircle,
                            accent = semantics.act,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = strings.statTotalSteps,
                            value = stats.totalSteps.toString(),
                            icon = Icons.Filled.TrendingUp,
                            accent = semantics.think,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statRecoveries,
                            value = stats.totalRecoveries.toString(),
                            icon = Icons.Filled.Refresh,
                            accent = semantics.recover,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Quick Launch Websites & Sandbox
            item(key = "quick_links") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.homeQuickLinksTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = SandboxPages.quickLinks,
                            key = { it.id }
                        ) { bookmark ->
                            QuickLinkCard(
                                bookmark = bookmark,
                                language = language,
                                onClick = { onOpenQuickLink(bookmark) }
                            )
                        }
                    }
                }
            }

            // 4. Recent Sessions (1-tap Resume)
            item(key = "recent_runs_header") {
                Text(
                    text = strings.homeRecentRunsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (recentRuns.isEmpty()) {
                item(key = "recent_runs_empty") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.homeNoRecentRuns,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = recentRuns,
                    key = { "home_run_${it.id}" }
                ) { run ->
                    RecentRunCompactCard(
                        run = run,
                        strings = strings,
                        onResume = { onResumeRun(run) }
                    )
                }
            }

            // 5. Ready Task Templates
            item(key = "presets_header") {
                Text(
                    text = strings.homePresetsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(
                items = SandboxPages.presets,
                key = { "home_preset_${it.id}" }
            ) { preset ->
                PresetCard(
                    preset = preset,
                    language = language,
                    strings = strings,
                    onApply = { onApplyPreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickLinkCard(
    bookmark: QuickLinkBookmark,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val title = if (language == AppLanguage.ARABIC) bookmark.titleAr else bookmark.titleEn
    val subtitle = if (language == AppLanguage.ARABIC) bookmark.subtitleAr else bookmark.subtitleEn
    val icon = when {
        bookmark.isSandbox -> Icons.Filled.Extension
        bookmark.id == "duckduckgo" -> Icons.Filled.Search
        else -> Icons.Filled.Language
    }
    val tint = if (bookmark.isSandbox) {
        LocalAgentSemanticColors.current.act
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .widthIn(min = 148.dp, max = 180.dp)
            .clickable(onClick = onClick)
            .testTag("quick_link_${bookmark.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = tint.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentRunCompactCard(
    run: AgentRunEntity,
    strings: AppStrings,
    onResume: () -> Unit
) {
    val semantics = LocalAgentSemanticColors.current
    val (statusLabel, statusColor) = when (run.status) {
        "completed" -> strings.statusCompletedTag to semantics.act
        "stopped" -> strings.statusStoppedTag to semantics.recover
        else -> strings.statusOtherTag to MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_recent_run_${run.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = strings.stepsSummary(run.stepCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = run.goal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onResume,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("home_resume_run_${run.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.resumeRunBtn,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(strings.resumeRunBtn)
            }
        }
    }
}

@Composable
private fun HubShortcutTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

