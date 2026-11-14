package com.example.model

enum class HubTab {
    WEATHER,
    NEWS,
    TOOLS,
    STORE
}

enum class NewsSource {
    HACKER_NEWS,
    SPACE_TECH
}

enum class TechToolKind {
    JSON,
    BASE64_URL,
    PASSWORD,
    URL_INSPECTOR,
    CONVERTER,
    POMODORO
}

enum class UnitCategory {
    LENGTH,
    WEIGHT,
    TEMPERATURE,
    DATA_SIZE
}

enum class ExtensionCategory {
    ALL,
    INSTALLED,
    READER_PRIVACY,
    DATA_EXTRACTOR,
    DEVELOPER,
    MINI_WIDGETS
}

enum class ExtensionAccentRole {
    OBSERVE,
    THINK,
    ACT,
    RECOVER
}

data class MurshedExtension(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val shortNameAr: String,
    val shortNameEn: String,
    val descAr: String,
    val descEn: String,
    val developer: String,
    val version: String,
    val category: ExtensionCategory,
    val permissionAr: String,
    val permissionEn: String,
    val rating: Double,
    val installsLabel: String,
    val accentRole: ExtensionAccentRole
)

data class ExtractedLink(
    val text: String,
    val href: String
)

data class ExtractedImage(
    val alt: String,
    val src: String
)

data class ExtractedTable(
    val caption: String,
    val rows: List<List<String>>
)

data class PageExtractionSnapshot(
    val url: String,
    val title: String,
    val metaDescription: String,
    val cleanArticleText: String,
    val wordCount: Int,
    val links: List<ExtractedLink>,
    val images: List<ExtractedImage>,
    val headings: List<Pair<String, String>>,
    val tables: List<ExtractedTable>,
    val detectedScriptsCount: Int,
    val pageLanguage: String = "",
    val canonicalUrl: String = "",
    val semanticOutline: String = "",
    val structuredDataSummary: String = "",
    val rawHtmlSource: String = ""
)

data class WeatherForecastDay(
    val date: String,
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double
)

data class WeatherSnapshot(
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val apparentTempC: Double,
    val humidityPercent: Int,
    val windKmh: Double,
    val weatherCode: Int,
    val forecastDays: List<WeatherForecastDay>,
    val fetchedAtMs: Long = System.currentTimeMillis()
)

data class NewsArticle(
    val id: String,
    val title: String,
    val sourceName: String,
    val author: String,
    val url: String,
    val summary: String,
    val publishedAt: String,
    val points: Int? = null,
    val commentsCount: Int? = null
)

data class UrlInspectionResult(
    val inputUrl: String,
    val finalUrl: String,
    val scheme: String,
    val host: String,
    val path: String,
    val queryParams: List<Pair<String, String>>,
    val httpStatusCode: Int?,
    val latencyMs: Long?,
    val contentType: String?,
    val isHttps: Boolean,
    val errorNote: String? = null
)

data class CurrencyRatesSnapshot(
    val baseCode: String,
    val lastUpdatedUtc: String,
    val rates: Map<String, Double>
)

data class MurshedHubUiState(
    val activeTab: HubTab = HubTab.WEATHER,
    // Weather
    val weatherCityQuery: String = "Riyadh",
    val isWeatherLoading: Boolean = false,
    val weatherSnapshot: WeatherSnapshot? = null,
    val weatherError: String? = null,
    // News
    val newsSource: NewsSource = NewsSource.HACKER_NEWS,
    val newsSearchQuery: String = "",
    val isNewsLoading: Boolean = false,
    val newsArticles: List<NewsArticle> = emptyList(),
    val newsError: String? = null,
    // Tech Tools
    val activeTool: TechToolKind = TechToolKind.JSON,
    val jsonInput: String = "{\"agent\":\"Murshed\",\"loop\":[\"read_screen\",\"think\",\"act\"],\"active\":true}",
    val jsonOutput: String = "",
    val jsonError: String? = null,
    val codecInput: String = "https://duckduckgo.com/?q=Murshed+Agent",
    val codecOutput: String = "",
    val codecError: String? = null,
    val passwordLength: Int = 16,
    val includeUpper: Boolean = true,
    val includeLower: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val generatedPassword: String = "",
    val inspectUrlInput: String = "https://duckduckgo.com",
    val isInspectingUrl: Boolean = false,
    val urlInspectionResult: UrlInspectionResult? = null,
    // Currency & Unit Converter
    val converterAmount: String = "100",
    val baseCurrency: String = "USD",
    val targetCurrency: String = "SAR",
    val isRatesLoading: Boolean = false,
    val ratesSnapshot: CurrencyRatesSnapshot? = null,
    val ratesError: String? = null,
    val unitCategory: UnitCategory = UnitCategory.LENGTH,
    val unitInputValue: String = "10",
    val unitFromIndex: Int = 0,
    val unitToIndex: Int = 1,
    // Pomodoro
    val pomodoroTotalSeconds: Int = 25 * 60,
    val pomodoroRemainingSeconds: Int = 25 * 60,
    val isPomodoroRunning: Boolean = false,
    val pomodoroCompletedCount: Int = 0,
    // Murshed Extension Store & Side Tool Runner
    val storeSearchQuery: String = "",
    val storeCategory: ExtensionCategory = ExtensionCategory.ALL,
    val installedExtensionIds: Set<String> = setOf(
        "clean_reader",
        "ai_summarizer",
        "privacy_cleaner",
        "web_extractor",
        "dom_inspector",
        "weather_widget"
    ),
    val pinnedExtensionIds: Set<String> = setOf(
        "clean_reader",
        "ai_summarizer",
        "privacy_cleaner",
        "web_extractor"
    ),
    val activeSideExtensionId: String? = null,
    val isExtractingPage: Boolean = false,
    val pageExtraction: PageExtractionSnapshot? = null,
    val extractionError: String? = null,
    val readerDarkMode: Boolean = true,
    val readerFontSizeSp: Int = 16,
    val isSummarizingPage: Boolean = false,
    val pageSummaryText: String = "",
    val pageSummaryError: String? = null,
    val summaryEngineSource: AiEngineSource = AiEngineSource.CLOUD_GEMINI,
    val summaryFallbackNote: String? = null,
    val privacyUrlInput: String = "",
    val extractorFilterQuery: String = "",
    val extractorSubTabIndex: Int = 0
)
