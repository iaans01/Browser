package com.example.data.remote

import android.net.Uri
import com.example.model.AppLanguage
import com.example.model.CurrencyRatesSnapshot
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.UrlInspectionResult
import com.example.model.WeatherForecastDay
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class LiveToolsRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun fetchWeatherForCity(
        cityQuery: String,
        language: AppLanguage
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        val trimmed = cityQuery.trim().ifBlank { "Riyadh" }
        val langParam = if (language == AppLanguage.ARABIC) "ar" else "en"
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${Uri.encode(trimmed)}&count=1&language=$langParam&format=json"

        var geoJson = executeGetJson(geoUrl)
        var results = geoJson.optJSONArray("results")
        if ((results == null || results.length() == 0) && langParam != "en") {
            val fallbackGeoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=${Uri.encode(trimmed)}&count=1&language=en&format=json"
            geoJson = executeGetJson(fallbackGeoUrl)
            results = geoJson.optJSONArray("results")
        }

        if (results == null || results.length() == 0) {
            throw IllegalStateException("City not found: $trimmed")
        }

        val loc = results.getJSONObject(0)
        val lat = loc.getDouble("latitude")
        val lon = loc.getDouble("longitude")
        val name = loc.optString("name", trimmed)
        val country = loc.optString("country", "")

        val forecastUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=7"

        val weatherJson = executeGetJson(forecastUrl)
        val current = weatherJson.getJSONObject("current")
        val daily = weatherJson.optJSONObject("daily")

        val forecastDays = buildList {
            if (daily != null) {
                val dates = daily.optJSONArray("time")
                val codes = daily.optJSONArray("weather_code")
                val maxs = daily.optJSONArray("temperature_2m_max")
                val mins = daily.optJSONArray("temperature_2m_min")
                val count = dates?.length() ?: 0
                for (i in 0 until count) {
                    add(
                        WeatherForecastDay(
                            date = dates?.optString(i).orEmpty(),
                            weatherCode = codes?.optInt(i, 0) ?: 0,
                            maxTempC = maxs?.optDouble(i, 0.0) ?: 0.0,
                            minTempC = mins?.optDouble(i, 0.0) ?: 0.0
                        )
                    )
                }
            }
        }

        WeatherSnapshot(
            cityName = name,
            country = country,
            latitude = lat,
            longitude = lon,
            temperatureC = current.optDouble("temperature_2m", 0.0),
            apparentTempC = current.optDouble("apparent_temperature", 0.0),
            humidityPercent = current.optInt("relative_humidity_2m", 0),
            windKmh = current.optDouble("wind_speed_10m", 0.0),
            weatherCode = current.optInt("weather_code", 0),
            forecastDays = forecastDays
        )
    }

    suspend fun fetchNews(
        source: NewsSource,
        query: String
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        when (source) {
            NewsSource.HACKER_NEWS -> {
                val url = if (trimmed.isBlank()) {
                    "https://hn.algolia.com/api/v1/search?tags=front_page&hitsPerPage=20"
                } else {
                    "https://hn.algolia.com/api/v1/search?tags=story&query=${Uri.encode(trimmed)}&hitsPerPage=20"
                }
                val json = executeGetJson(url)
                val hits = json.optJSONArray("hits")
                buildList {
                    if (hits != null) {
                        for (i in 0 until hits.length()) {
                            val obj = hits.optJSONObject(i) ?: continue
                            val title = obj.optString("title").trim()
                            if (title.isBlank()) continue
                            val objectId = obj.optString("objectID")
                            val rawUrl = obj.optString("url").takeIf { it.isNotBlank() && it != "null" }
                                ?: "https://news.ycombinator.com/item?id=$objectId"
                            val host = runCatching { URI(rawUrl).host?.removePrefix("www.") }.getOrNull()
                                ?: "Hacker News"
                            add(
                                NewsArticle(
                                    id = "hn_$objectId",
                                    title = title,
                                    sourceName = host,
                                    author = obj.optString("author", ""),
                                    url = rawUrl,
                                    summary = "",
                                    publishedAt = obj.optString("created_at", "").take(10),
                                    points = obj.optInt("points", 0),
                                    commentsCount = obj.optInt("num_comments", 0)
                                )
                            )
                        }
                    }
                }
            }

            NewsSource.SPACE_TECH -> {
                val base = "https://api.spaceflightnewsapi.net/v4/articles/?limit=20"
                val url = if (trimmed.isBlank()) base else "$base&search=${Uri.encode(trimmed)}"
                val json = executeGetJson(url)
                val results = json.optJSONArray("results")
                buildList {
                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val obj = results.optJSONObject(i) ?: continue
                            val title = obj.optString("title").trim()
                            val articleUrl = obj.optString("url").trim()
                            if (title.isBlank() || articleUrl.isBlank()) continue
                            add(
                                NewsArticle(
                                    id = "sn_${obj.optInt("id", i)}",
                                    title = title,
                                    sourceName = obj.optString("news_site", "SpaceNews"),
                                    author = "",
                                    url = articleUrl,
                                    summary = obj.optString("summary", "").trim(),
                                    publishedAt = obj.optString("published_at", "").take(10)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun fetchCurrencyRates(base: String): CurrencyRatesSnapshot = withContext(Dispatchers.IO) {
        val code = base.trim().uppercase().ifBlank { "USD" }
        val json = executeGetJson("https://open.er-api.com/v6/latest/$code")
        if (json.optString("result") != "success") {
            throw IllegalStateException("Exchange rate service error.")
        }
        val ratesObj = json.getJSONObject("rates")
        val map = buildMap {
            val keys = ratesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                put(k, ratesObj.optDouble(k, 0.0))
            }
        }
        CurrencyRatesSnapshot(
            baseCode = json.optString("base_code", code),
            lastUpdatedUtc = json.optString("time_last_update_utc", ""),
            rates = map
        )
    }

    suspend fun inspectUrl(rawUrl: String): UrlInspectionResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        val normalized = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        val androidUri = Uri.parse(normalized)
        val params = androidUri.queryParameterNames.map { key ->
            key to (androidUri.getQueryParameter(key) ?: "")
        }
        val scheme = androidUri.scheme.orEmpty()
        val host = androidUri.host.orEmpty()
        val path = androidUri.path.orEmpty().ifBlank { "/" }

        val startMs = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(normalized)
                .header("User-Agent", "MurshedAgent/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startMs
                UrlInspectionResult(
                    inputUrl = normalized,
                    finalUrl = response.request.url.toString(),
                    scheme = scheme,
                    host = host,
                    path = path,
                    queryParams = params,
                    httpStatusCode = response.code,
                    latencyMs = latency,
                    contentType = response.header("Content-Type"),
                    isHttps = response.request.url.isHttps
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startMs
            UrlInspectionResult(
                inputUrl = normalized,
                finalUrl = normalized,
                scheme = scheme,
                host = host,
                path = path,
                queryParams = params,
                httpStatusCode = null,
                latencyMs = latency,
                contentType = null,
                isHttps = scheme.equals("https", ignoreCase = true),
                errorNote = e.localizedMessage ?: "Connection failed"
            )
        }
    }

    private fun executeGetJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "MurshedAgent/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            if (body.isBlank()) {
                throw IOException("Empty response body")
            }
            return JSONObject(body)
        }
    }
}
