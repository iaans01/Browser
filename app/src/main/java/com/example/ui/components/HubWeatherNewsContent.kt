package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.MurshedHubUiState
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.WeatherForecastDay
import com.example.ui.theme.LocalAgentSemanticColors
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.roundToInt

private val quickCitiesAr = listOf("الرياض", "دبي", "القاهرة", "جدة", "لندن", "طوكيو")
private val quickCitiesEn = listOf("Riyadh", "Dubai", "Cairo", "Jeddah", "London", "Tokyo")

@Composable
fun WeatherTabContent(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    strings: AppStrings,
    onCityQueryChanged: (String) -> Unit,
    onSearchCity: (String?) -> Unit,
    onOpenInBrowser: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val semantics = LocalAgentSemanticColors.current
    val cities = if (isAr) quickCitiesAr else quickCitiesEn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("hub_weather_tab"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "weather_search") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.weatherCityQuery,
                        onValueChange = onCityQueryChanged,
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        placeholder = {
                            Text(if (isAr) "ابحث عن مدينة (مثل: الرياض، دبي...)" else "Search city (e.g. Riyadh, London...)")
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchCity(null) }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weather_city_input")
                    )
                    Button(
                        onClick = { onSearchCity(null) },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("weather_search_button")
                    ) {
                        Text(if (isAr) "عرض" else "Check")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cities.forEach { city ->
                        AssistChip(
                            onClick = { onSearchCity(city) },
                            label = { Text(city) },
                            modifier = Modifier.testTag("weather_chip_$city")
                        )
                    }
                }
            }
        }

        if (uiState.isWeatherLoading) {
            item(key = "weather_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (uiState.weatherError != null) {
            item(key = "weather_error") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.weatherError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = { onSearchCity(null) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.retryBtn)
                        }
                    }
                }
            }
        }

        uiState.weatherSnapshot?.let { w ->
            item(key = "weather_hero") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, semantics.observe.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weather_snapshot_card")
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = listOf(w.cityName, w.country).filter { it.isNotBlank() }.joinToString(", "),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = weatherConditionLabel(w.weatherCode, isAr),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = semantics.observe
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = weatherConditionIcon(w.weatherCode),
                                    contentDescription = weatherConditionLabel(w.weatherCode, isAr),
                                    tint = semantics.observe,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "${w.temperatureC.roundToInt()}°C",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WeatherMetricPill(
                                icon = Icons.Filled.Thermostat,
                                label = if (isAr) "المحسوسة" else "Feels",
                                value = "${w.apparentTempC.roundToInt()}°C",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetricPill(
                                icon = Icons.Filled.WaterDrop,
                                label = if (isAr) "الرطوبة" else "Humidity",
                                value = "${w.humidityPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherMetricPill(
                                icon = Icons.Filled.Air,
                                label = if (isAr) "الرياح" else "Wind",
                                value = "${w.windKmh.roundToInt()} km/h",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val q = android.net.Uri.encode("weather ${w.cityName}")
                                    val goal = if (isAr) {
                                        "ابحث عن تفاصيل طقس ${w.cityName} الأسبوعي ولخّص أبرز التوقعات."
                                    } else {
                                        "Search weekly forecast details for ${w.cityName} and summarize."
                                    }
                                    onOpenInBrowser("https://duckduckgo.com/?q=$q", goal)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weather_open_agent_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.analyzeWithAgentBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (w.forecastDays.isNotEmpty()) {
                item(key = "weather_forecast_strip") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isAr) "توقعات 7 أيام" else "7-Day Forecast",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                items = w.forecastDays,
                                key = { it.date }
                            ) { day ->
                                ForecastDayCard(day = day, isAr = isAr)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun ForecastDayCard(day: WeatherForecastDay, isAr: Boolean) {
    val semantics = LocalAgentSemanticColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.widthIn(min = 104.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = day.date.takeLast(5),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = weatherConditionIcon(day.weatherCode),
                contentDescription = weatherConditionLabel(day.weatherCode, isAr),
                tint = semantics.observe,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "${day.maxTempC.roundToInt()}° / ${day.minTempC.roundToInt()}°",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceGroteskFontFamily)
            )
        }
    }
}

@Composable
fun NewsTabContent(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    strings: AppStrings,
    onSelectSource: (NewsSource) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshNews: () -> Unit,
    onOpenInBrowser: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("hub_news_tab"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "news_controls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.newsSource == NewsSource.HACKER_NEWS,
                        onClick = { onSelectSource(NewsSource.HACKER_NEWS) },
                        label = { Text(if (isAr) "أخبار التقنية (HN)" else "Tech (HN)") },
                        modifier = Modifier.testTag("news_source_hn")
                    )
                    FilterChip(
                        selected = uiState.newsSource == NewsSource.SPACE_TECH,
                        onClick = { onSelectSource(NewsSource.SPACE_TECH) },
                        label = { Text(if (isAr) "العلوم والفضاء" else "Science & Space") },
                        modifier = Modifier.testTag("news_source_space")
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onRefreshNews,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("news_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.reloadBtn)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.newsSearchQuery,
                        onValueChange = onSearchQueryChanged,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = {
                            Text(if (isAr) "تصفية الأخبار (مثل: AI, Kotlin, NASA)..." else "Filter news (e.g. AI, Kotlin, NASA)...")
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onRefreshNews() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("news_search_input")
                    )
                    Button(
                        onClick = onRefreshNews,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("news_search_button")
                    ) {
                        Text(if (isAr) "بحث" else "Go")
                    }
                }
            }
        }

        if (uiState.isNewsLoading) {
            item(key = "news_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (uiState.newsError != null) {
            item(key = "news_error") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(uiState.newsError, color = MaterialTheme.colorScheme.onErrorContainer)
                        OutlinedButton(onClick = onRefreshNews) {
                            Text(strings.retryBtn)
                        }
                    }
                }
            }
        }

        items(
            items = uiState.newsArticles,
            key = { it.id }
        ) { article ->
            NewsArticleCard(
                article = article,
                isAr = isAr,
                strings = strings,
                onOpen = { onOpenInBrowser(article.url, null) },
                onAnalyze = {
                    val goal = if (isAr) {
                        "اقرأ هذا الخبر «${article.title}» واستخرج أهم 3 نقاط رئيسية فيه."
                    } else {
                        "Read this article '${article.title}' and summarize the top 3 takeaways."
                    }
                    onOpenInBrowser(article.url, goal)
                }
            )
        }
    }
}

@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    isAr: Boolean,
    strings: AppStrings,
    onOpen: () -> Unit,
    onAnalyze: () -> Unit
) {
    val semantics = LocalAgentSemanticColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_card_${article.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = semantics.observe.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = article.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = semantics.observe,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                val meta = buildString {
                    if (article.publishedAt.isNotBlank()) append(article.publishedAt)
                    if (article.points != null && article.points > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${article.points} pts")
                    }
                }
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (article.summary.isNotBlank()) {
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = strings.openInBrowserBtn, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.openInBrowserBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = strings.analyzeWithAgentBtn, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.analyzeWithAgentBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun weatherConditionIcon(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Filled.WbSunny
    2, 3, 45, 48 -> Icons.Filled.Cloud
    in 95..99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.WaterDrop
}

private fun weatherConditionLabel(code: Int, isAr: Boolean): String = when (code) {
    0 -> if (isAr) "سماء صافية" else "Clear sky"
    1, 2 -> if (isAr) "غائم جزئياً" else "Partly cloudy"
    3 -> if (isAr) "غائم كلياً" else "Overcast"
    45, 48 -> if (isAr) "ضباب" else "Foggy"
    in 51..67 -> if (isAr) "أمطار خفيفة إلى متوسطة" else "Rain showers"
    in 71..77 -> if (isAr) "ثلوج" else "Snow"
    in 80..82 -> if (isAr) "زخات مطر" else "Rain showers"
    in 95..99 -> if (isAr) "عواصف رعدية" else "Thunderstorm"
    else -> if (isAr) "طقس متقلب" else "Variable conditions"
}
