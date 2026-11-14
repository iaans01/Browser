package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAgentEngine
import com.example.browser.WebViewDomBridge
import com.example.data.local.AgentPreferencesRepository
import com.example.data.remote.LiveToolsRepository
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.model.ExtensionAccentRole
import com.example.model.ExtensionCategory
import com.example.model.HubTab
import com.example.model.MurshedExtension
import com.example.model.MurshedHubUiState
import com.example.model.NewsSource
import com.example.model.TechToolKind
import com.example.model.UnitCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

private const val HUB_TAG = "MurshedHubViewModel"

class MurshedHubViewModel(
    private val repository: LiveToolsRepository = LiveToolsRepository(),
    private val preferencesRepository: AgentPreferencesRepository? = null,
    private val geminiEngine: GeminiAgentEngine = GeminiAgentEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MurshedHubUiState())
    val uiState: StateFlow<MurshedHubUiState> = _uiState.asStateFlow()

    private var hasBootstrapped = false
    private var pomodoroJob: Job? = null
    private var summaryJob: Job? = null
    private val secureRandom = SecureRandom()

    val extensionCatalog: List<MurshedExtension> = listOf(
        // 1. Reader & Privacy Extensions
        MurshedExtension(
            id = "clean_reader",
            nameAr = "القارئ النظيف والوضع الليلي",
            nameEn = "Clean Dark Reader",
            shortNameAr = "قارئ ليلي",
            shortNameEn = "Reader",
            descAr = "يحول مقالات الويب إلى نص صافٍ مريح للعين مع وضع ليلي وتكبير الخط.",
            descEn = "Turns web pages into distraction-free reading mode with dark theme and font controls.",
            developer = "Murshed Labs",
            version = "2.4.0",
            category = ExtensionCategory.READER_PRIVACY,
            permissionAr = "قراءة نص الصفحة",
            permissionEn = "Read page text",
            rating = 4.9,
            installsLabel = "18K+",
            accentRole = ExtensionAccentRole.OBSERVE
        ),
        MurshedExtension(
            id = "ai_summarizer",
            nameAr = "ملخّص الصفحة الذكي (Gemini)",
            nameEn = "AI Page Summarizer",
            shortNameAr = "ملخّص AI",
            shortNameEn = "Summary",
            descAr = "يلخص محتوى الصفحة المفتوحة فورياً في نقاط مركزة عبر Firebase AI Logic.",
            descEn = "Streams concise bullet summaries of the current webpage using Firebase AI Logic.",
            developer = "Murshed AI",
            version = "3.1.0",
            category = ExtensionCategory.READER_PRIVACY,
            permissionAr = "تلخيص النص بالذكاء",
            permissionEn = "AI text summary",
            rating = 4.9,
            installsLabel = "24K+",
            accentRole = ExtensionAccentRole.THINK
        ),
        MurshedExtension(
            id = "privacy_cleaner",
            nameAr = "حظر التتبع ومنظف الروابط",
            nameEn = "Tracker Blocker & URL Cleaner",
            shortNameAr = "منظف تتبع",
            shortNameEn = "Privacy",
            descAr = "يكشف معلمات التتبع مثل utm و fbclid و gclid ويزيلها مع فحص HTTPS.",
            descEn = "Strips utm, fbclid, gclid tracking parameters and checks HTTPS security.",
            developer = "Murshed Shield",
            version = "1.9.2",
            category = ExtensionCategory.READER_PRIVACY,
            permissionAr = "فحص وتنظيف الرابط",
            permissionEn = "Inspect & clean URL",
            rating = 4.8,
            installsLabel = "15K+",
            accentRole = ExtensionAccentRole.ACT
        ),
        // 2. Data Extractor Extensions
        MurshedExtension(
            id = "web_extractor",
            nameAr = "مستخرج الروابط والصور والجداول",
            nameEn = "Links, Images & Tables Extractor",
            shortNameAr = "مستخرج ويب",
            shortNameEn = "Extractor",
            descAr = "يستخلص الروابط والصور والعناوين والجداول المنظمة من أي صفحة بضغطة واحدة.",
            descEn = "Extracts links, images, headings, and HTML tables from the active page.",
            developer = "Murshed Data",
            version = "2.2.1",
            category = ExtensionCategory.DATA_EXTRACTOR,
            permissionAr = "استخراج عناصر DOM",
            permissionEn = "Extract DOM data",
            rating = 4.9,
            installsLabel = "21K+",
            accentRole = ExtensionAccentRole.OBSERVE
        ),
        // 3. Developer Extensions
        MurshedExtension(
            id = "dom_inspector",
            nameAr = "فاحص الصفحة وشجرة DOM",
            nameEn = "Page & DOM Inspector",
            shortNameAr = "فاحص DOM",
            shortNameEn = "DOM",
            descAr = "يفحص العناصر التفاعلية وبيانات التعريف والسكربتات مع تمييز العنصر على الشاشة.",
            descEn = "Inspects interactive elements, meta tags, scripts count, and highlights nodes.",
            developer = "Murshed DevTools",
            version = "2.0.5",
            category = ExtensionCategory.DEVELOPER,
            permissionAr = "تحليل شجرة الصفحة",
            permissionEn = "Inspect DOM tree",
            rating = 4.8,
            installsLabel = "14K+",
            accentRole = ExtensionAccentRole.RECOVER
        ),
        MurshedExtension(
            id = "json_formatter",
            nameAr = "منسق ومُدقق JSON المباشر",
            nameEn = "Live JSON Formatter",
            shortNameAr = "JSON",
            shortNameEn = "JSON",
            descAr = "أداة جانبية لتنسيق وتصغير واكتشاف أخطاء صياغة JSON بسرعة أثناء التصفح.",
            descEn = "Side-panel JSON beautifier, minifier, and syntax validator.",
            developer = "Murshed DevTools",
            version = "1.7.0",
            category = ExtensionCategory.DEVELOPER,
            permissionAr = "أداة محلية سريعة",
            permissionEn = "Local dev tool",
            rating = 4.7,
            installsLabel = "11K+",
            accentRole = ExtensionAccentRole.OBSERVE
        ),
        MurshedExtension(
            id = "url_ping_codec",
            nameAr = "فاحص استجابة الروابط و Base64",
            nameEn = "URL Ping & Base64 Codec",
            shortNameAr = "Ping/Base64",
            shortNameEn = "Codec",
            descAr = "يفحص رمز الحالة وزمن الاستجابة الفعلي مع تشفير وفك تشفير Base64 والروابط.",
            descEn = "Live HTTP latency/status ping plus Base64 and URL encoder/decoder.",
            developer = "Murshed Net",
            version = "1.6.4",
            category = ExtensionCategory.DEVELOPER,
            permissionAr = "اتصال شبكي مباشر",
            permissionEn = "Network HTTP ping",
            rating = 4.8,
            installsLabel = "9.4K+",
            accentRole = ExtensionAccentRole.ACT
        ),
        MurshedExtension(
            id = "password_gen",
            nameAr = "مولّد كلمات المرور الآمنة",
            nameEn = "Secure Password Side Gen",
            shortNameAr = "كلمات مرور",
            shortNameEn = "PassGen",
            descAr = "يولد كلمات مرور عشوائية بـ SecureRandom بجانب أي نموذج تسجيل دخول.",
            descEn = "Cryptographic SecureRandom password generator right next to login forms.",
            developer = "Murshed Shield",
            version = "1.4.0",
            category = ExtensionCategory.DEVELOPER,
            permissionAr = "توليد محلي آمن",
            permissionEn = "On-device crypto",
            rating = 4.9,
            installsLabel = "12K+",
            accentRole = ExtensionAccentRole.THINK
        ),
        // 4. Live Mini-Widgets Extensions
        MurshedExtension(
            id = "weather_widget",
            nameAr = "مصغر الطقس المباشر",
            nameEn = "Live Weather Mini-Widget",
            shortNameAr = "الطقس",
            shortNameEn = "Weather",
            descAr = "يعرض درجة الحرارة والتوقعات الحية لأي مدينة كبطاقة جانبية سريعة.",
            descEn = "Compact Open-Meteo current weather and forecast card inside your browser.",
            developer = "Murshed Live",
            version = "1.8.0",
            category = ExtensionCategory.MINI_WIDGETS,
            permissionAr = "بيانات Open-Meteo حية",
            permissionEn = "Live weather feed",
            rating = 4.8,
            installsLabel = "19K+",
            accentRole = ExtensionAccentRole.OBSERVE
        ),
        MurshedExtension(
            id = "news_widget",
            nameAr = "مصغر آخر الأخبار التقنية",
            nameEn = "Live Tech News Ticker",
            shortNameAr = "الأخبار",
            shortNameEn = "News",
            descAr = "يعرض عناوين الأخبار التقنية الحية مع فتح أي خبر أو تحليله بالوكيل فوراً.",
            descEn = "Live Hacker News & Spaceflight headlines ready to open or analyze.",
            developer = "Murshed Live",
            version = "1.5.2",
            category = ExtensionCategory.MINI_WIDGETS,
            permissionAr = "خلاصات أخبار حية",
            permissionEn = "Live news feeds",
            rating = 4.7,
            installsLabel = "13K+",
            accentRole = ExtensionAccentRole.THINK
        ),
        MurshedExtension(
            id = "currency_widget",
            nameAr = "مصغر محول العملات والوحدات",
            nameEn = "Live Currency & Units Widget",
            shortNameAr = "عملات",
            shortNameEn = "FX/Units",
            descAr = "تحويل فوري للعملات بأسعار صرف حية (SAR, AED, EGP, USD) والوحدات الهندسية.",
            descEn = "Live FX exchange rates and length/weight/data unit conversions.",
            developer = "Murshed Live",
            version = "1.6.0",
            category = ExtensionCategory.MINI_WIDGETS,
            permissionAr = "أسعار صرف حية",
            permissionEn = "Live FX feed",
            rating = 4.8,
            installsLabel = "16K+",
            accentRole = ExtensionAccentRole.ACT
        ),
        MurshedExtension(
            id = "pomodoro_widget",
            nameAr = "مؤقت التركيز (بومودورو)",
            nameEn = "Focus Pomodoro Side Timer",
            shortNameAr = "تركيز",
            shortNameEn = "Focus",
            descAr = "مؤقت جلسات عمل 25 دقيقة يعمل في الخلفية بجانب التصفح.",
            descEn = "25/15/5 minute background focus timer for productive browsing sessions.",
            developer = "Murshed Productivity",
            version = "1.3.1",
            category = ExtensionCategory.MINI_WIDGETS,
            permissionAr = "مؤقت محلي",
            permissionEn = "Local timer",
            rating = 4.7,
            installsLabel = "10K+",
            accentRole = ExtensionAccentRole.RECOVER
        )
    )

    init {
        generatePassword()
        formatJson()
        if (preferencesRepository != null) {
            viewModelScope.launch {
                val settings = preferencesRepository.settingsFlow.first()
                _uiState.update { state ->
                    state.copy(
                        installedExtensionIds = settings.installedExtensions,
                        pinnedExtensionIds = settings.pinnedExtensions.intersect(settings.installedExtensions)
                    )
                }
            }
        }
    }

    fun ensureInitialLoaded(language: AppLanguage) {
        if (hasBootstrapped) return
        hasBootstrapped = true
        searchWeatherCity("Riyadh", language)
        refreshNews()
        refreshCurrencyRates("USD")
    }

    fun selectTab(tab: HubTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // --- Chrome-Style Extension Store & Side Tool Runner ---
    fun onStoreSearchChanged(query: String) {
        _uiState.update { it.copy(storeSearchQuery = query) }
    }

    fun selectStoreCategory(category: ExtensionCategory) {
        _uiState.update { it.copy(storeCategory = category) }
    }

    fun toggleInstallExtension(extensionId: String) {
        val prev = _uiState.value
        val wasInstalled = extensionId in prev.installedExtensionIds
        val nextInstalled = if (wasInstalled) {
            prev.installedExtensionIds - extensionId
        } else {
            prev.installedExtensionIds + extensionId
        }
        val nextPinned = if (wasInstalled) {
            prev.pinnedExtensionIds - extensionId
        } else {
            prev.pinnedExtensionIds + extensionId
        }
        val nextActive = if (wasInstalled && prev.activeSideExtensionId == extensionId) null else prev.activeSideExtensionId
        _uiState.update {
            it.copy(
                installedExtensionIds = nextInstalled,
                pinnedExtensionIds = nextPinned,
                activeSideExtensionId = nextActive
            )
        }
        persistExtensions(nextInstalled, nextPinned)
    }

    fun togglePinExtension(extensionId: String) {
        val prev = _uiState.value
        if (extensionId !in prev.installedExtensionIds) return
        val nextPinned = if (extensionId in prev.pinnedExtensionIds) {
            prev.pinnedExtensionIds - extensionId
        } else {
            prev.pinnedExtensionIds + extensionId
        }
        _uiState.update { it.copy(pinnedExtensionIds = nextPinned) }
        persistExtensions(prev.installedExtensionIds, nextPinned)
    }

    private fun persistExtensions(installed: Set<String>, pinned: Set<String>) {
        val repo = preferencesRepository ?: return
        viewModelScope.launch {
            repo.saveExtensionsState(installed, pinned)
        }
    }

    fun openSideExtension(
        extensionId: String,
        activeUrl: String,
        domBridge: WebViewDomBridge?,
        language: AppLanguage
    ) {
        // Auto-install if launched directly from Store preview
        val prev = _uiState.value
        val nextInstalled = prev.installedExtensionIds + extensionId
        _uiState.update {
            it.copy(
                installedExtensionIds = nextInstalled,
                activeSideExtensionId = extensionId,
                privacyUrlInput = it.privacyUrlInput.ifBlank { activeUrl },
                extractionError = null,
                pageSummaryError = null
            )
        }
        if (extensionId !in prev.installedExtensionIds) {
            persistExtensions(nextInstalled, prev.pinnedExtensionIds)
        }

        when (extensionId) {
            "clean_reader", "ai_summarizer", "web_extractor", "dom_inspector", "privacy_cleaner" -> {
                _uiState.update { it.copy(privacyUrlInput = activeUrl) }
                if (domBridge != null) {
                    refreshPageExtraction(domBridge)
                }
            }
            "weather_widget" -> {
                if (_uiState.value.weatherSnapshot == null) searchWeatherCity(null, language)
            }
            "news_widget" -> {
                if (_uiState.value.newsArticles.isEmpty()) refreshNews()
            }
            "currency_widget" -> {
                if (_uiState.value.ratesSnapshot == null) refreshCurrencyRates()
            }
        }
    }

    fun closeSideExtension() {
        _uiState.update { it.copy(activeSideExtensionId = null) }
    }

    fun refreshPageExtraction(domBridge: WebViewDomBridge) {
        _uiState.update { it.copy(isExtractingPage = true, extractionError = null) }
        viewModelScope.launch {
            runCatching { domBridge.extractPageResources() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isExtractingPage = false,
                            pageExtraction = snapshot,
                            privacyUrlInput = snapshot.url.ifBlank { it.privacyUrlInput },
                            extractionError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isExtractingPage = false,
                            extractionError = err.localizedMessage ?: "Failed to read current browser page."
                        )
                    }
                }
        }
    }

    fun toggleReaderDarkMode() {
        _uiState.update { it.copy(readerDarkMode = !it.readerDarkMode) }
    }

    fun adjustReaderFontSize(delta: Int) {
        _uiState.update { it.copy(readerFontSizeSp = (it.readerFontSizeSp + delta).coerceIn(13, 24)) }
    }

    fun onExtractorFilterChanged(query: String) {
        _uiState.update { it.copy(extractorFilterQuery = query) }
    }

    fun selectExtractorSubTab(index: Int) {
        _uiState.update { it.copy(extractorSubTabIndex = index.coerceIn(0, 3)) }
    }

    fun onPrivacyUrlInputChanged(url: String) {
        _uiState.update { it.copy(privacyUrlInput = url) }
    }

    fun summarizeCurrentPage(
        activeUrl: String,
        domBridge: WebViewDomBridge?,
        language: AppLanguage
    ) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _uiState.update { it.copy(isSummarizingPage = true, pageSummaryText = "", pageSummaryError = null) }
            try {
                var extraction = _uiState.value.pageExtraction
                if ((extraction == null || extraction.cleanArticleText.isBlank()) && domBridge != null) {
                    extraction = runCatching { domBridge.extractPageResources() }.getOrNull()
                    if (extraction != null) {
                        _uiState.update { it.copy(pageExtraction = extraction) }
                    }
                }
                val content = extraction?.cleanArticleText.orEmpty()
                val hasDeepContent = content.isNotBlank() ||
                    !extraction?.metaDescription.isNullOrBlank() ||
                    !extraction?.semanticOutline.isNullOrBlank()
                if (!hasDeepContent) {
                    _uiState.update {
                        it.copy(
                            isSummarizingPage = false,
                            pageSummaryError = if (language == AppLanguage.ARABIC) {
                                "لا يوجد نص كافٍ في الصفحة الحالية لتلخيصه. افتح مقالة أو موقعاً ثم حاول مجدداً."
                            } else {
                                "No readable text found on the current page. Open an article or website first."
                            }
                        )
                    }
                    return@launch
                }

                val allowCloudGemini = preferencesRepository?.settingsFlow?.first()?.cloudGeminiEnabled ?: true

                geminiEngine.streamPageSummary(
                    url = extraction?.url?.ifBlank { activeUrl } ?: activeUrl,
                    title = extraction?.title.orEmpty(),
                    contentText = content,
                    language = language,
                    allowCloudGemini = allowCloudGemini,
                    metaDescription = extraction?.metaDescription.orEmpty(),
                    semanticOutline = extraction?.semanticOutline.orEmpty(),
                    structuredDataSummary = extraction?.structuredDataSummary.orEmpty()
                ).collect { chunk ->
                    _uiState.update {
                        it.copy(
                            pageSummaryText = chunk.text,
                            summaryEngineSource = chunk.engineSource,
                            summaryFallbackNote = chunk.fallbackReason
                        )
                    }
                }

                _uiState.update { it.copy(isSummarizingPage = false) }
            } catch (e: TimeoutCancellationException) {
                Log.e(HUB_TAG, "Page summary timed out", e)
                _uiState.update {
                    it.copy(
                        isSummarizingPage = false,
                        pageSummaryError = if (language == AppLanguage.ARABIC) {
                            "انتهت مهلة التلخيص. حاول مجدداً."
                        } else {
                            "Summary timed out. Please try again."
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(HUB_TAG, "Page summary failed", e)
                _uiState.update {
                    it.copy(
                        isSummarizingPage = false,
                        pageSummaryError = e.localizedMessage ?: "AI summary failed."
                    )
                }
            }
        }
    }

    fun cleanTrackingParams(rawUrl: String): Pair<String, List<String>> {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return "" to emptyList()
        return try {
            val uri = Uri.parse(trimmed)
            if (uri.scheme == null || uri.host == null) return trimmed to emptyList()
            val trackerKeys = setOf(
                "fbclid", "gclid", "dclid", "msclkid", "yclid", "igshid",
                "mc_cid", "mc_eid", "_hsenc", "_hsmi", "oly_enc_id", "oly_anon_id",
                "si", "spm", "vero_id", "ref_src", "ref_url"
            )
            val stripped = mutableListOf<String>()
            val builder = uri.buildUpon().clearQuery()
            for (name in uri.queryParameterNames) {
                val lower = name.lowercase()
                if (lower.startsWith("utm_") || lower in trackerKeys) {
                    stripped.add(name)
                } else {
                    uri.getQueryParameters(name).forEach { value ->
                        builder.appendQueryParameter(name, value)
                    }
                }
            }
            builder.build().toString() to stripped
        } catch (_: Exception) {
            trimmed to emptyList()
        }
    }

    // --- Weather ---
    fun onWeatherCityQueryChanged(query: String) {
        _uiState.update { it.copy(weatherCityQuery = query) }
    }

    fun searchWeatherCity(cityOverride: String? = null, language: AppLanguage = AppLanguage.ARABIC) {
        val target = (cityOverride ?: _uiState.value.weatherCityQuery).trim().ifBlank { "Riyadh" }
        _uiState.update {
            it.copy(
                weatherCityQuery = target,
                isWeatherLoading = true,
                weatherError = null
            )
        }
        viewModelScope.launch {
            runCatching { repository.fetchWeatherForCity(target, language) }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherSnapshot = snapshot,
                            weatherError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isWeatherLoading = false,
                            weatherError = err.localizedMessage ?: "Failed to load weather."
                        )
                    }
                }
        }
    }

    // --- News ---
    fun selectNewsSource(source: NewsSource) {
        _uiState.update { it.copy(newsSource = source) }
        refreshNews()
    }

    fun onNewsSearchQueryChanged(query: String) {
        _uiState.update { it.copy(newsSearchQuery = query) }
    }

    fun refreshNews() {
        val state = _uiState.value
        _uiState.update { it.copy(isNewsLoading = true, newsError = null) }
        viewModelScope.launch {
            runCatching { repository.fetchNews(state.newsSource, state.newsSearchQuery) }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isNewsLoading = false,
                            newsArticles = list,
                            newsError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isNewsLoading = false,
                            newsError = err.localizedMessage ?: "Failed to load news."
                        )
                    }
                }
        }
    }

    // --- Tech Tools ---
    fun selectTool(tool: TechToolKind) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun onJsonInputChanged(value: String) {
        _uiState.update { it.copy(jsonInput = value, jsonError = null) }
    }

    fun formatJson() {
        val raw = _uiState.value.jsonInput.trim()
        if (raw.isBlank()) {
            _uiState.update { it.copy(jsonOutput = "", jsonError = null) }
            return
        }
        runCatching {
            when (val parsed = JSONTokener(raw).nextValue()) {
                is JSONObject -> parsed.toString(2)
                is JSONArray -> parsed.toString(2)
                else -> throw IllegalArgumentException("Input is not a JSON object or array.")
            }
        }.onSuccess { pretty ->
            _uiState.update { it.copy(jsonOutput = pretty, jsonError = null) }
        }.onFailure { err ->
            _uiState.update { it.copy(jsonError = err.localizedMessage ?: "Invalid JSON syntax") }
        }
    }

    fun minifyJson() {
        val raw = _uiState.value.jsonInput.trim()
        if (raw.isBlank()) return
        runCatching {
            when (val parsed = JSONTokener(raw).nextValue()) {
                is JSONObject -> parsed.toString()
                is JSONArray -> parsed.toString()
                else -> throw IllegalArgumentException("Input is not a JSON object or array.")
            }
        }.onSuccess { compact ->
            _uiState.update { it.copy(jsonOutput = compact, jsonError = null) }
        }.onFailure { err ->
            _uiState.update { it.copy(jsonError = err.localizedMessage ?: "Invalid JSON syntax") }
        }
    }

    fun onCodecInputChanged(value: String) {
        _uiState.update { it.copy(codecInput = value, codecError = null) }
    }

    fun encodeBase64() {
        val raw = _uiState.value.codecInput
        val out = Base64.encodeToString(raw.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        _uiState.update { it.copy(codecOutput = out, codecError = null) }
    }

    fun decodeBase64() {
        val raw = _uiState.value.codecInput.trim()
        runCatching {
            String(Base64.decode(raw, Base64.DEFAULT), StandardCharsets.UTF_8)
        }.onSuccess { out ->
            _uiState.update { it.copy(codecOutput = out, codecError = null) }
        }.onFailure { err ->
            _uiState.update { it.copy(codecError = err.localizedMessage ?: "Invalid Base64 input") }
        }
    }

    fun encodeUrl() {
        _uiState.update { it.copy(codecOutput = Uri.encode(it.codecInput), codecError = null) }
    }

    fun decodeUrl() {
        runCatching { Uri.decode(_uiState.value.codecInput) }
            .onSuccess { out -> _uiState.update { it.copy(codecOutput = out, codecError = null) } }
            .onFailure { err -> _uiState.update { it.copy(codecError = err.localizedMessage ?: "Decode error") } }
    }

    fun setPasswordLength(len: Int) {
        _uiState.update { it.copy(passwordLength = len.coerceIn(8, 32)) }
        generatePassword()
    }

    fun togglePasswordOption(upper: Boolean? = null, lower: Boolean? = null, digits: Boolean? = null, symbols: Boolean? = null) {
        _uiState.update { state ->
            val nextUpper = upper ?: state.includeUpper
            val nextLower = lower ?: state.includeLower
            val nextDigits = digits ?: state.includeDigits
            val nextSymbols = symbols ?: state.includeSymbols
            if (!nextUpper && !nextLower && !nextDigits && !nextSymbols) {
                state
            } else {
                state.copy(
                    includeUpper = nextUpper,
                    includeLower = nextLower,
                    includeDigits = nextDigits,
                    includeSymbols = nextSymbols
                )
            }
        }
        generatePassword()
    }

    fun generatePassword() {
        val s = _uiState.value
        val pool = buildString {
            if (s.includeUpper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (s.includeLower) append("abcdefghijklmnopqrstuvwxyz")
            if (s.includeDigits) append("0123456789")
            if (s.includeSymbols) append("!@#\$%^&*()-_=+[]?")
        }.ifEmpty { "abcdefghijklmnopqrstuvwxyz0123456789" }

        val pw = buildString {
            repeat(s.passwordLength) {
                append(pool[secureRandom.nextInt(pool.length)])
            }
        }
        _uiState.update { it.copy(generatedPassword = pw) }
    }

    fun onInspectUrlChanged(value: String) {
        _uiState.update { it.copy(inspectUrlInput = value) }
    }

    fun runUrlInspection() {
        val target = _uiState.value.inspectUrlInput.trim()
        if (target.isBlank()) return
        _uiState.update { it.copy(isInspectingUrl = true) }
        viewModelScope.launch {
            val result = repository.inspectUrl(target)
            _uiState.update {
                it.copy(
                    isInspectingUrl = false,
                    urlInspectionResult = result
                )
            }
        }
    }

    // --- Currency & Unit Converter ---
    fun onCurrencyAmountChanged(value: String) {
        _uiState.update { it.copy(converterAmount = value) }
    }

    fun selectBaseCurrency(code: String) {
        _uiState.update { it.copy(baseCurrency = code) }
        refreshCurrencyRates(code)
    }

    fun selectTargetCurrency(code: String) {
        _uiState.update { it.copy(targetCurrency = code) }
    }

    fun swapCurrencies() {
        val current = _uiState.value
        val newBase = current.targetCurrency
        val newTarget = current.baseCurrency
        _uiState.update { it.copy(baseCurrency = newBase, targetCurrency = newTarget) }
        refreshCurrencyRates(newBase)
    }

    fun refreshCurrencyRates(baseOverride: String? = null) {
        val base = (baseOverride ?: _uiState.value.baseCurrency).uppercase()
        _uiState.update { it.copy(isRatesLoading = true, ratesError = null) }
        viewModelScope.launch {
            runCatching { repository.fetchCurrencyRates(base) }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isRatesLoading = false,
                            ratesSnapshot = snapshot,
                            ratesError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isRatesLoading = false,
                            ratesError = err.localizedMessage ?: "Failed to load exchange rates."
                        )
                    }
                }
        }
    }

    fun selectUnitCategory(cat: UnitCategory) {
        _uiState.update { it.copy(unitCategory = cat, unitFromIndex = 0, unitToIndex = 1) }
    }

    fun onUnitInputChanged(value: String) {
        _uiState.update { it.copy(unitInputValue = value) }
    }

    fun setUnitIndices(from: Int? = null, to: Int? = null) {
        _uiState.update {
            it.copy(
                unitFromIndex = from ?: it.unitFromIndex,
                unitToIndex = to ?: it.unitToIndex
            )
        }
    }

    // --- Pomodoro Timer ---
    fun setPomodoroPreset(minutes: Int) {
        pomodoroJob?.cancel()
        val secs = minutes * 60
        _uiState.update {
            it.copy(
                pomodoroTotalSeconds = secs,
                pomodoroRemainingSeconds = secs,
                isPomodoroRunning = false
            )
        }
    }

    fun togglePomodoroRunning() {
        if (_uiState.value.isPomodoroRunning) {
            pomodoroJob?.cancel()
            _uiState.update { it.copy(isPomodoroRunning = false) }
            return
        }
        if (_uiState.value.pomodoroRemainingSeconds <= 0) {
            _uiState.update { it.copy(pomodoroRemainingSeconds = it.pomodoroTotalSeconds) }
        }
        _uiState.update { it.copy(isPomodoroRunning = true) }
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (_uiState.value.pomodoroRemainingSeconds > 0 && _uiState.value.isPomodoroRunning) {
                delay(1_000L)
                _uiState.update { state ->
                    val next = (state.pomodoroRemainingSeconds - 1).coerceAtLeast(0)
                    if (next == 0) {
                        state.copy(
                            pomodoroRemainingSeconds = 0,
                            isPomodoroRunning = false,
                            pomodoroCompletedCount = state.pomodoroCompletedCount + 1
                        )
                    } else {
                        state.copy(pomodoroRemainingSeconds = next)
                    }
                }
            }
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        _uiState.update {
            it.copy(
                pomodoroRemainingSeconds = it.pomodoroTotalSeconds,
                isPomodoroRunning = false
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MurshedHubViewModel(
                repository = LiveToolsRepository(),
                preferencesRepository = AgentPreferencesRepository(application),
                geminiEngine = GeminiAgentEngine()
            ) as T
        }
    }
}
