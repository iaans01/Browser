package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.HubTab
import com.example.ui.components.StoreTabContent
import com.example.ui.components.TechToolsTabContent
import com.example.ui.components.WeatherTabContent
import com.example.ui.components.NewsTabContent
import com.example.viewmodel.MurshedHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MurshedHubScreen(
    hubViewModel: MurshedHubViewModel,
    language: AppLanguage,
    strings: AppStrings,
    onNavigateBack: () -> Unit,
    onOpenInAgentBrowser: (String, String?) -> Unit,
    onLaunchSideExtension: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by hubViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        hubViewModel.ensureInitialLoaded(language)
    }

    BackHandler {
        onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("murshed_hub_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    text = strings.hubTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("hub_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backWebBtn
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        val tabs = listOf(
            Triple(HubTab.WEATHER, strings.hubTabWeather, Icons.Filled.WbSunny),
            Triple(HubTab.NEWS, strings.hubTabNews, Icons.Filled.Newspaper),
            Triple(HubTab.TOOLS, strings.hubTabTools, Icons.Filled.Build),
            Triple(HubTab.STORE, strings.hubTabStore, Icons.Filled.Extension)
        )

        PrimaryTabRow(
            selectedTabIndex = uiState.activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { (tab, label, icon) ->
                Tab(
                    selected = uiState.activeTab == tab,
                    onClick = { hubViewModel.selectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("hub_tab_${tab.name.lowercase()}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

        when (uiState.activeTab) {
            HubTab.WEATHER -> WeatherTabContent(
                uiState = uiState,
                language = language,
                strings = strings,
                onCityQueryChanged = hubViewModel::onWeatherCityQueryChanged,
                onSearchCity = { city -> hubViewModel.searchWeatherCity(city, language) },
                onOpenInBrowser = onOpenInAgentBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            HubTab.NEWS -> NewsTabContent(
                uiState = uiState,
                language = language,
                strings = strings,
                onSelectSource = hubViewModel::selectNewsSource,
                onSearchQueryChanged = hubViewModel::onNewsSearchQueryChanged,
                onRefreshNews = hubViewModel::refreshNews,
                onOpenInBrowser = onOpenInAgentBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            HubTab.TOOLS -> TechToolsTabContent(
                uiState = uiState,
                language = language,
                strings = strings,
                hubViewModel = hubViewModel,
                onOpenInBrowser = onOpenInAgentBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            HubTab.STORE -> StoreTabContent(
                uiState = uiState,
                catalog = hubViewModel.extensionCatalog,
                language = language,
                strings = strings,
                hubViewModel = hubViewModel,
                onLaunchSideExtension = onLaunchSideExtension,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
