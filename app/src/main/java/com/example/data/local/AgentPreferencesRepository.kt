package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "murshed_agent_prefs"
)

object ExtensionDefaults {
    val DEFAULT_INSTALLED: Set<String> = setOf(
        "clean_reader",
        "ai_summarizer",
        "privacy_cleaner",
        "web_extractor",
        "dom_inspector",
        "weather_widget"
    )
    val DEFAULT_PINNED: Set<String> = setOf(
        "clean_reader",
        "ai_summarizer",
        "privacy_cleaner",
        "web_extractor"
    )
}

data class AgentSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val isDarkTheme: Boolean = true,
    val maxSteps: Int = 12,
    val stepDelayMs: Long = 900L,
    val cloudGeminiEnabled: Boolean = true,
    val installedExtensions: Set<String> = ExtensionDefaults.DEFAULT_INSTALLED,
    val pinnedExtensions: Set<String> = ExtensionDefaults.DEFAULT_PINNED
)

class AgentPreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val MAX_STEPS = intPreferencesKey("max_steps")
        val STEP_DELAY_MS = longPreferencesKey("step_delay_ms")
        val CLOUD_GEMINI_ENABLED = booleanPreferencesKey("cloud_gemini_enabled")
        val INSTALLED_EXTENSIONS = stringSetPreferencesKey("installed_extension_ids")
        val PINNED_EXTENSIONS = stringSetPreferencesKey("pinned_extension_ids")
    }

    val settingsFlow: Flow<AgentSettings> = context.agentDataStore.data.map { prefs ->
        AgentSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            maxSteps = (prefs[Keys.MAX_STEPS] ?: 12).coerceIn(3, 25),
            stepDelayMs = (prefs[Keys.STEP_DELAY_MS] ?: 900L).coerceIn(300L, 2500L),
            cloudGeminiEnabled = prefs[Keys.CLOUD_GEMINI_ENABLED] ?: true,
            installedExtensions = prefs[Keys.INSTALLED_EXTENSIONS] ?: ExtensionDefaults.DEFAULT_INSTALLED,
            pinnedExtensions = prefs[Keys.PINNED_EXTENSIONS] ?: ExtensionDefaults.DEFAULT_PINNED
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.agentDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.agentDataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setMaxSteps(steps: Int) {
        context.agentDataStore.edit { it[Keys.MAX_STEPS] = steps.coerceIn(3, 25) }
    }

    suspend fun setStepDelayMs(delayMs: Long) {
        context.agentDataStore.edit { it[Keys.STEP_DELAY_MS] = delayMs.coerceIn(300L, 2500L) }
    }

    suspend fun setCloudGeminiEnabled(enabled: Boolean) {
        context.agentDataStore.edit { it[Keys.CLOUD_GEMINI_ENABLED] = enabled }
    }

    suspend fun saveExtensionsState(installed: Set<String>, pinned: Set<String>) {
        context.agentDataStore.edit {
            it[Keys.INSTALLED_EXTENSIONS] = installed
            it[Keys.PINNED_EXTENSIONS] = pinned.intersect(installed)
        }
    }
}
