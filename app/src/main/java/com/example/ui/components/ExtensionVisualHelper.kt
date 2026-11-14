package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.ExtensionAccentRole
import com.example.ui.theme.LocalAgentSemanticColors

fun extensionIconFor(extensionId: String): ImageVector = when (extensionId) {
    "clean_reader" -> Icons.Filled.DarkMode
    "ai_summarizer" -> Icons.Filled.AutoAwesome
    "privacy_cleaner" -> Icons.Filled.Security
    "web_extractor" -> Icons.Filled.TableChart
    "dom_inspector" -> Icons.Filled.Code
    "json_formatter" -> Icons.Filled.DataObject
    "url_ping_codec" -> Icons.Filled.Link
    "password_gen" -> Icons.Filled.Key
    "weather_widget" -> Icons.Filled.WbSunny
    "news_widget" -> Icons.Filled.Newspaper
    "currency_widget" -> Icons.Filled.CurrencyExchange
    "pomodoro_widget" -> Icons.Filled.Timer
    else -> Icons.Filled.Extension
}

@Composable
fun extensionAccentColor(role: ExtensionAccentRole): Color {
    val semantics = LocalAgentSemanticColors.current
    return when (role) {
        ExtensionAccentRole.OBSERVE -> semantics.observe
        ExtensionAccentRole.THINK -> semantics.think
        ExtensionAccentRole.ACT -> semantics.act
        ExtensionAccentRole.RECOVER -> semantics.recover
    }
}
