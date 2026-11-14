package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.MurshedHubUiState
import com.example.model.TechToolKind
import com.example.model.UnitCategory
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAgentSemanticColors
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.MurshedHubViewModel
import java.util.Locale

private val popularCurrencies = listOf("USD", "SAR", "AED", "EGP", "EUR", "GBP", "KWD", "TRY")

@Composable
fun TechToolsTabContent(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    strings: AppStrings,
    hubViewModel: MurshedHubViewModel,
    onOpenInBrowser: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("hub_tools_tab"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "tool_selector") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechToolKind.entries.forEach { tool ->
                    val label = when (tool) {
                        TechToolKind.JSON -> "JSON"
                        TechToolKind.BASE64_URL -> "Base64 / URL"
                        TechToolKind.PASSWORD -> if (isAr) "كلمات مرور" else "Password"
                        TechToolKind.URL_INSPECTOR -> if (isAr) "فاحص الروابط" else "URL Ping"
                        TechToolKind.CONVERTER -> if (isAr) "العملات والوحدات" else "FX & Units"
                        TechToolKind.POMODORO -> if (isAr) "مؤقت التركيز" else "Pomodoro"
                    }
                    FilterChip(
                        selected = uiState.activeTool == tool,
                        onClick = { hubViewModel.selectTool(tool) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("tool_chip_${tool.name.lowercase()}")
                    )
                }
            }
        }

        item(key = "active_tool_body") {
            when (uiState.activeTool) {
                TechToolKind.JSON -> JsonToolCard(uiState, isAr, hubViewModel)
                TechToolKind.BASE64_URL -> CodecToolCard(uiState, isAr, hubViewModel)
                TechToolKind.PASSWORD -> PasswordToolCard(uiState, isAr, hubViewModel)
                TechToolKind.URL_INSPECTOR -> UrlInspectorCard(uiState, isAr, strings, hubViewModel, onOpenInBrowser)
                TechToolKind.CONVERTER -> ConverterToolCard(uiState, isAr, hubViewModel)
                TechToolKind.POMODORO -> PomodoroToolCard(uiState, isAr, hubViewModel)
            }
        }
    }
}

@Composable
private fun JsonToolCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val clipboard = LocalClipboardManager.current
    val semantics = LocalAgentSemanticColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = semantics.observe)
                Text(
                    text = if (isAr) "منسق وفاحص JSON" else "JSON Formatter & Validator",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = uiState.jsonInput,
                    onValueChange = vm::onJsonInputChanged,
                    minLines = 4,
                    maxLines = 8,
                    textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("json_tool_input")
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::formatJson, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "تنسيق" else "Format")
                }
                OutlinedButton(onClick = vm::minifyJson, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "ضغط" else "Minify")
                }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(uiState.jsonOutput.ifBlank { uiState.jsonInput })) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
            }
            if (uiState.jsonError != null) {
                Text(uiState.jsonError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (uiState.jsonOutput.isNotBlank()) {
                MonoResultBox(text = uiState.jsonOutput)
            }
        }
    }
}

@Composable
private fun CodecToolCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val clipboard = LocalClipboardManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isAr) "تشفير وفك ترميز Base64 و URL" else "Base64 & URL Encoder / Decoder",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = uiState.codecInput,
                onValueChange = vm::onCodecInputChanged,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = vm::encodeBase64, modifier = Modifier.weight(1f)) { Text("Base64 +") }
                OutlinedButton(onClick = vm::decodeBase64, modifier = Modifier.weight(1f)) { Text("Base64 -") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = vm::encodeUrl, modifier = Modifier.weight(1f)) { Text("URL +") }
                OutlinedButton(onClick = vm::decodeUrl, modifier = Modifier.weight(1f)) { Text("URL -") }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(uiState.codecOutput)) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
            }
            if (uiState.codecError != null) {
                Text(uiState.codecError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (uiState.codecOutput.isNotBlank()) {
                MonoResultBox(text = uiState.codecOutput)
            }
        }
    }
}

@Composable
private fun PasswordToolCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val clipboard = LocalClipboardManager.current
    val semantics = LocalAgentSemanticColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = semantics.act)
                Text(
                    text = if (isAr) "مولد كلمات مرور مشفر" else "Secure Password Generator",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            MonoResultBox(text = uiState.generatedPassword)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::generatePassword, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isAr) "توليد جديد" else "Generate")
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(uiState.generatedPassword)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isAr) "نسخ" else "Copy")
                }
            }
            Text(
                text = if (isAr) "الطول: ${uiState.passwordLength} حرف" else "Length: ${uiState.passwordLength} chars",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = uiState.passwordLength.toFloat(),
                onValueChange = { vm.setPasswordLength(it.toInt()) },
                valueRange = 8f..32f,
                steps = 23
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.includeUpper, onCheckedChange = { vm.togglePasswordOption(upper = it) })
                    Text("A-Z")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.includeLower, onCheckedChange = { vm.togglePasswordOption(lower = it) })
                    Text("a-z")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.includeDigits, onCheckedChange = { vm.togglePasswordOption(digits = it) })
                    Text("0-9")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.includeSymbols, onCheckedChange = { vm.togglePasswordOption(symbols = it) })
                    Text("!@#")
                }
            }
        }
    }
}

@Composable
private fun UrlInspectorCard(
    uiState: MurshedHubUiState,
    isAr: Boolean,
    strings: AppStrings,
    vm: MurshedHubViewModel,
    onOpenInBrowser: (String, String?) -> Unit
) {
    val semantics = LocalAgentSemanticColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = semantics.observe)
                Text(
                    text = if (isAr) "فاحص الروابط والاستجابة الحي" else "Live URL Ping & Inspector",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = uiState.inspectUrlInput,
                    onValueChange = vm::onInspectUrlChanged,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.runUrlInspection() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = vm::runUrlInspection,
                enabled = !uiState.isInspectingUrl,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isInspectingUrl) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isAr) "فحص الرابط والاستجابة" else "Inspect & Ping URL")
            }

            uiState.urlInspectionResult?.let { res ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val statusText = res.httpStatusCode?.let { "HTTP $it" } ?: (res.errorNote ?: "Error")
                        Text(
                            text = "$statusText • ${res.latencyMs ?: 0} ms • ${if (res.isHttps) "HTTPS" else "HTTP"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if ((res.httpStatusCode ?: 500) < 400) semantics.act else MaterialTheme.colorScheme.error
                        )
                        Text("Host: ${res.host}", style = MaterialTheme.typography.labelSmall)
                        Text("Path: ${res.path}", style = MaterialTheme.typography.labelSmall)
                        res.contentType?.let { Text("Type: $it", style = MaterialTheme.typography.labelSmall) }
                        if (res.queryParams.isNotEmpty()) {
                            Text(
                                text = "Params: " + res.queryParams.joinToString(", ") { "${it.first}=${it.second}" },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenInBrowser(res.finalUrl, null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.openInBrowserBtn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConverterToolCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val semantics = LocalAgentSemanticColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Currency Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CurrencyExchange, contentDescription = null, tint = semantics.act)
                        Text(
                            text = if (isAr) "محول العملات الحي" else "Live Currency Converter",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(onClick = { vm.refreshCurrencyRates() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }

                OutlinedTextField(
                    value = uiState.converterAmount,
                    onValueChange = vm::onCurrencyAmountChanged,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(if (isAr) "المبلغ" else "Amount") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (isAr) "من:" else "From:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    popularCurrencies.forEach { code ->
                        FilterChip(
                            selected = uiState.baseCurrency == code,
                            onClick = { vm.selectBaseCurrency(code) },
                            label = { Text(code) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isAr) "إلى:" else "To:", style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(onClick = vm::swapCurrencies) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isAr) "تبديل" else "Swap")
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    popularCurrencies.forEach { code ->
                        FilterChip(
                            selected = uiState.targetCurrency == code,
                            onClick = { vm.selectTargetCurrency(code) },
                            label = { Text(code) }
                        )
                    }
                }

                if (uiState.isRatesLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                } else if (uiState.ratesError != null) {
                    Text(uiState.ratesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                } else {
                    val amount = uiState.converterAmount.toDoubleOrNull() ?: 0.0
                    val rate = uiState.ratesSnapshot?.rates?.get(uiState.targetCurrency)
                    if (rate != null) {
                        val converted = amount * rate
                        Surface(
                            color = semantics.act.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, semantics.act.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = String.format(Locale.US, "%.2f %s = %.2f %s", amount, uiState.baseCurrency, converted, uiState.targetCurrency),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold),
                                    color = semantics.act
                                )
                                Text(
                                    text = "1 ${uiState.baseCurrency} = ${String.format(Locale.US, "%.4f", rate)} ${uiState.targetCurrency}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Unit Converter Card
        UnitConverterCard(uiState = uiState, isAr = isAr, vm = vm)
    }
}

@Composable
private fun UnitConverterCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val units = when (uiState.unitCategory) {
        UnitCategory.LENGTH -> listOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mile" to 1609.34)
        UnitCategory.WEIGHT -> listOf("kg" to 1.0, "g" to 0.001, "lb" to 0.453592)
        UnitCategory.TEMPERATURE -> listOf("°C" to 1.0, "°F" to 1.0, "K" to 1.0)
        UnitCategory.DATA_SIZE -> listOf("MB" to 1.0, "GB" to 1024.0, "KB" to (1.0 / 1024.0))
    }
    val fromIdx = uiState.unitFromIndex.coerceIn(0, units.lastIndex)
    val toIdx = uiState.unitToIndex.coerceIn(0, units.lastIndex)
    val inputVal = uiState.unitInputValue.toDoubleOrNull() ?: 0.0
    val outputVal = if (uiState.unitCategory == UnitCategory.TEMPERATURE) {
        convertTemp(inputVal, units[fromIdx].first, units[toIdx].first)
    } else {
        inputVal * units[fromIdx].second / units[toIdx].second
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isAr) "محول الوحدات التقنية" else "Unit Converter", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                UnitCategory.entries.forEach { cat ->
                    val name = when (cat) {
                        UnitCategory.LENGTH -> if (isAr) "طول" else "Length"
                        UnitCategory.WEIGHT -> if (isAr) "وزن" else "Weight"
                        UnitCategory.TEMPERATURE -> if (isAr) "حرارة" else "Temp"
                        UnitCategory.DATA_SIZE -> if (isAr) "بيانات" else "Data"
                    }
                    FilterChip(
                        selected = uiState.unitCategory == cat,
                        onClick = { vm.selectUnitCategory(cat) },
                        label = { Text(name) }
                    )
                }
            }
            OutlinedTextField(
                value = uiState.unitInputValue,
                onValueChange = vm::onUnitInputChanged,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                units.forEachIndexed { idx, pair ->
                    FilterChip(
                        selected = fromIdx == idx,
                        onClick = { vm.setUnitIndices(from = idx) },
                        label = { Text(pair.first) }
                    )
                }
            }
            Text(
                text = "➔ ${String.format(Locale.US, "%.4f", outputVal)} ${units[toIdx].first}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                units.forEachIndexed { idx, pair ->
                    FilterChip(
                        selected = toIdx == idx,
                        onClick = { vm.setUnitIndices(to = idx) },
                        label = { Text(pair.first) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PomodoroToolCard(uiState: MurshedHubUiState, isAr: Boolean, vm: MurshedHubViewModel) {
    val semantics = LocalAgentSemanticColors.current
    val mins = uiState.pomodoroRemainingSeconds / 60
    val secs = uiState.pomodoroRemainingSeconds % 60
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = semantics.think)
                Text(
                    text = if (isAr) "مؤقت التركيز (Pomodoro)" else "Pomodoro Focus Timer",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (uiState.isPomodoroRunning) semantics.observe else MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 15, 5).forEach { m ->
                    FilterChip(
                        selected = uiState.pomodoroTotalSeconds == m * 60,
                        onClick = { vm.setPomodoroPreset(m) },
                        label = { Text("${m}m") }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::togglePomodoroRunning, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (uiState.isPomodoroRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (uiState.isPomodoroRunning) (if (isAr) "إيقاف مؤقت" else "Pause")
                        else (if (isAr) "بدء" else "Start")
                    )
                }
                OutlinedButton(onClick = vm::resetPomodoro, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isAr) "إعادة" else "Reset")
                }
            }
            Text(
                text = if (isAr) "الجلسات المكتملة اليوم: ${uiState.pomodoroCompletedCount}"
                else "Completed focus sessions: ${uiState.pomodoroCompletedCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonoResultBox(text: String) {
    val semantics = LocalAgentSemanticColors.current
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
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = semantics.observe
            )
        }
    }
}

private fun convertTemp(v: Double, from: String, to: String): Double {
    val c = when (from) {
        "°F" -> (v - 32.0) * 5.0 / 9.0
        "K" -> v - 273.15
        else -> v
    }
    return when (to) {
        "°F" -> c * 9.0 / 5.0 + 32.0
        "K" -> c + 273.15
        else -> c
    }
}
