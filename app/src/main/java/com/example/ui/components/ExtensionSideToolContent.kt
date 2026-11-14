package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppStrings
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.model.MurshedHubUiState
import com.example.model.ScreenSnapshot
import com.example.model.TechToolKind
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAgentSemanticColors
import com.example.viewmodel.MurshedHubViewModel

@Composable
fun ExtensionCleanReaderPanel(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    hubViewModel: MurshedHubViewModel,
    onRefreshFromPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val clipboard = LocalClipboardManager.current
    val extraction = uiState.pageExtraction

    val readerBg = if (uiState.readerDarkMode) Color(0xFF0E141F) else Color(0xFFFDF8EE)
    val readerFg = if (uiState.readerDarkMode) Color(0xFFE8EEF8) else Color(0xFF1F242D)
    val readerMuted = if (uiState.readerDarkMode) Color(0xFF9DB0CC) else Color(0xFF5A6577)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ext_panel_clean_reader"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "reader_controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = hubViewModel::toggleReaderDarkMode) {
                    Icon(
                        imageVector = if (uiState.readerDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (uiState.readerDarkMode) (if (isAr) "فاتح" else "Light")
                        else (if (isAr) "ليلي" else "Dark")
                    )
                }
                OutlinedButton(onClick = { hubViewModel.adjustReaderFontSize(-1) }) {
                    Text("A-")
                }
                OutlinedButton(onClick = { hubViewModel.adjustReaderFontSize(1) }) {
                    Text("A+")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefreshFromPage, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = if (isAr) "تحديث" else "Refresh")
                }
            }
        }

        if (uiState.isExtractingPage) {
            item(key = "reader_loading") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
        } else if (uiState.extractionError != null) {
            item(key = "reader_error") {
                Text(uiState.extractionError, color = MaterialTheme.colorScheme.error)
            }
        } else {
            item(key = "reader_body") {
                Surface(
                    color = readerBg,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = extraction?.title?.ifBlank { extraction.url }
                                ?: (if (isAr) "لم تُقرأ أي صفحة بعد" else "No page extracted yet"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = readerFg
                        )
                        if (extraction != null) {
                            Text(
                                text = if (isAr) {
                                    "${extraction.wordCount} كلمة • قراءة نظيفة خالية من التشتيت"
                                } else {
                                    "${extraction.wordCount} words • Distraction-free view"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = readerMuted
                            )
                            HorizontalDivider(color = readerMuted.copy(alpha = 0.3f))
                            Text(
                                text = extraction.cleanArticleText.ifBlank {
                                    if (isAr) "لا يوجد نص مقروء في هذه الصفحة." else "No readable article text found."
                                },
                                fontSize = uiState.readerFontSizeSp.sp,
                                lineHeight = (uiState.readerFontSizeSp + 9).sp,
                                color = readerFg
                            )
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(extraction.cleanArticleText)) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isAr) "نسخ النص" else "Copy Text")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionAiSummarizerPanel(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    onSummarizeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ext_panel_ai_summarizer"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "sum_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, semantics.think.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiState.pageExtraction?.title?.ifBlank { uiState.pageExtraction.url }
                            ?: (if (isAr) "الصفحة الحالية في المتصفح" else "Current Browser Page"),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = onSummarizeNow,
                        enabled = !uiState.isSummarizingPage,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (uiState.isSummarizingPage) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isAr) "جارٍ التلخيص عبر Gemini..." else "Summarizing with Gemini...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAr) "لخّص الصفحة الحالية" else "Summarize Current Page")
                        }
                    }
                }
            }
        }

        uiState.pageSummaryError?.let { err ->
            item(key = "sum_err") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
        }

        if (uiState.pageSummaryText.isNotBlank()) {
            item(key = "sum_result") {
                val isLocal = uiState.summaryEngineSource == AiEngineSource.LOCAL_LITE_FALLBACK
                val badgeColor = if (isLocal) semantics.recover else semantics.observe
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            val badgeText = if (isLocal) {
                                val note = uiState.summaryFallbackNote
                                if (isAr) {
                                    "محلي خفيف" + (if (!note.isNullOrBlank()) " • $note" else "")
                                } else {
                                    "Local Lite" + (if (!note.isNullOrBlank()) " • $note" else "")
                                }
                            } else {
                                if (isAr) "Gemini سحابي" else "Gemini Cloud"
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = uiState.pageSummaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(uiState.pageSummaryText)) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAr) "نسخ الملخص" else "Copy Summary")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionPrivacyCleanerPanel(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    hubViewModel: MurshedHubViewModel,
    onOpenCleanUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current
    val (cleanedUrl, strippedParams) = remember(uiState.privacyUrlInput) {
        hubViewModel.cleanTrackingParams(uiState.privacyUrlInput)
    }
    val isHttps = cleanedUrl.startsWith("https://", ignoreCase = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ext_panel_privacy_cleaner"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "privacy_input") {
            OutlinedTextField(
                value = uiState.privacyUrlInput,
                onValueChange = hubViewModel::onPrivacyUrlInputChanged,
                label = { Text(if (isAr) "الرابط المراد فحصه وتنظيفه" else "URL to inspect & strip trackers") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "privacy_stats") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, semantics.act.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = if (isHttps) semantics.act else semantics.recover
                        )
                        Text(
                            text = if (isHttps) {
                                if (isAr) "اتصال آمن HTTPS" else "Secure HTTPS Connection"
                            } else {
                                if (isAr) "رابط غير مشفر أو محلي" else "Non-HTTPS / Local URL"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Text(
                        text = if (strippedParams.isEmpty()) {
                            if (isAr) "لم يتم رصد معلمات تتبع معروفة (utm / fbclid / gclid)." else "No known tracking parameters detected."
                        } else {
                            (if (isAr) "تمت إزالة ${strippedParams.size} معلمة تتبع: " else "Stripped ${strippedParams.size} trackers: ") +
                                strippedParams.joinToString(", ")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (strippedParams.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else semantics.act
                    )

                    uiState.pageExtraction?.let { ext ->
                        Text(
                            text = if (isAr) "سكربتات خارجية مرصودة في الصفحة: ${ext.detectedScriptsCount}" else "External scripts on page: ${ext.detectedScriptsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (cleanedUrl.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cleanedUrl,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(cleanedUrl)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "نسخ النظيف" else "Copy Clean")
                            }
                            Button(
                                onClick = { onOpenCleanUrl(cleanedUrl) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "فتح النظيف" else "Open Clean")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionWebExtractorPanel(
    uiState: MurshedHubUiState,
    language: AppLanguage,
    hubViewModel: MurshedHubViewModel,
    onRefreshFromPage: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val clipboard = LocalClipboardManager.current
    val extraction = uiState.pageExtraction
    val query = uiState.extractorFilterQuery.trim().lowercase()

    val subTabs = if (isAr) {
        listOf(
            "الروابط (${extraction?.links?.size ?: 0})",
            "الصور (${extraction?.images?.size ?: 0})",
            "الجداول (${extraction?.tables?.size ?: 0})",
            "العناوين (${extraction?.headings?.size ?: 0})"
        )
    } else {
        listOf(
            "Links (${extraction?.links?.size ?: 0})",
            "Images (${extraction?.images?.size ?: 0})",
            "Tables (${extraction?.tables?.size ?: 0})",
            "Headings (${extraction?.headings?.size ?: 0})"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ext_panel_web_extractor"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "extractor_controls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.extractorFilterQuery,
                        onValueChange = hubViewModel::onExtractorFilterChanged,
                        singleLine = true,
                        placeholder = { Text(if (isAr) "تصفية النتائج..." else "Filter extracted items...") },
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = onRefreshFromPage, modifier = Modifier.height(52.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subTabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = uiState.extractorSubTabIndex == idx,
                            onClick = { hubViewModel.selectExtractorSubTab(idx) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        if (uiState.isExtractingPage) {
            item(key = "ext_loading") {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (extraction == null) {
            item(key = "ext_empty") {
                Text(
                    text = uiState.extractionError ?: (if (isAr) "اضغط تحديث لاستخراج بيانات الصفحة." else "Tap refresh to extract page data."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            when (uiState.extractorSubTabIndex) {
                0 -> {
                    val links = extraction.links.filter {
                        query.isBlank() || it.text.lowercase().contains(query) || it.href.lowercase().contains(query)
                    }
                    items(items = links, key = { it.href + it.text.hashCode() }) { link ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(link.text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        link.href,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(link.href)) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = { onOpenUrl(link.href) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val imgs = extraction.images.filter {
                        query.isBlank() || it.alt.lowercase().contains(query) || it.src.lowercase().contains(query)
                    }
                    items(items = imgs, key = { it.src }) { img ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        img.alt.ifBlank { if (isAr) "صورة بدون نص بديل" else "Image (no alt)" },
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        img.src,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(img.src)) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (extraction.tables.isEmpty()) {
                        item(key = "no_tables") {
                            Text(if (isAr) "لا توجد جداول HTML في الصفحة الحالية." else "No HTML tables found on current page.")
                        }
                    } else {
                        items(items = extraction.tables, key = { it.caption }) { table ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(table.caption, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    table.rows.take(8).forEach { row ->
                                        Text(
                                            text = row.joinToString(" | "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val heads = extraction.headings.filter {
                        query.isBlank() || it.second.lowercase().contains(query)
                    }
                    items(items = heads, key = { it.first + it.second.hashCode() }) { (tag, txt) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text(tag.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text(txt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionDomInspectorPanel(
    uiState: MurshedHubUiState,
    snapshot: ScreenSnapshot?,
    language: AppLanguage,
    strings: AppStrings,
    onReadScreen: () -> Unit,
    onHighlightElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current
    val elements = snapshot?.elements.orEmpty()
    val htmlSource = snapshot?.rawHtmlSource?.ifBlank { uiState.pageExtraction?.rawHtmlSource.orEmpty() }
        ?: uiState.pageExtraction?.rawHtmlSource.orEmpty()
    val outline = snapshot?.semanticOutline?.ifBlank { uiState.pageExtraction?.semanticOutline.orEmpty() }
        ?: uiState.pageExtraction?.semanticOutline.orEmpty()
    val metaDesc = snapshot?.metaDescription?.ifBlank { uiState.pageExtraction?.metaDescription.orEmpty() }
        ?: uiState.pageExtraction?.metaDescription.orEmpty()

    var showHtmlView by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ext_panel_dom_inspector"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "dom_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "العناصر التفاعلية: ${elements.size}" else "Interactive Elements: ${elements.size}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FilledTonalButton(onClick = onReadScreen) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.readScreenBtn)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showHtmlView,
                        onClick = { showHtmlView = false },
                        label = { Text(if (isAr) "العناصر (${elements.size})" else "Nodes (${elements.size})") }
                    )
                    FilterChip(
                        selected = showHtmlView,
                        onClick = { showHtmlView = true },
                        label = { Text(if (isAr) "بنية ومصدر HTML" else "HTML & Structure") }
                    )
                }
            }
        }
        if (!showHtmlView) {
            items(items = elements.take(40), key = { it.agentId }) { el ->
                DomElementCard(
                    element = el,
                    strings = strings,
                    onHighlightClick = { onHighlightElement(el.agentId) }
                )
            }
        } else {
            if (metaDesc.isNotBlank() || outline.isNotBlank()) {
                item(key = "ext_html_structure") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (metaDesc.isNotBlank()) {
                                Text(strings.htmlMetadataTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(metaDesc, style = MaterialTheme.typography.bodySmall)
                            }
                            if (outline.isNotBlank()) {
                                Text(strings.htmlOutlineTitle, style = MaterialTheme.typography.labelLarge, color = semantics.think)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = outline.take(1800),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "ext_html_source") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.htmlSourceTitle, style = MaterialTheme.typography.labelLarge)
                        if (htmlSource.isNotBlank()) {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(htmlSource)) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = strings.copyHtmlBtn, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.copyHtmlBtn)
                            }
                        }
                    }
                    if (htmlSource.isBlank()) {
                        Text(strings.emptyHtmlBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    text = htmlSource.take(18000),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = semantics.observe
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionDevToolsDelegatedPanel(
    extensionId: String,
    uiState: MurshedHubUiState,
    language: AppLanguage,
    strings: AppStrings,
    hubViewModel: MurshedHubViewModel,
    onOpenInBrowser: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (extensionId) {
        "json_formatter" -> {
            hubViewModel.selectTool(TechToolKind.JSON)
            TechToolsTabContent(uiState, language, strings, hubViewModel, onOpenInBrowser, modifier)
        }
        "url_ping_codec" -> {
            hubViewModel.selectTool(TechToolKind.URL_INSPECTOR)
            TechToolsTabContent(uiState, language, strings, hubViewModel, onOpenInBrowser, modifier)
        }
        "password_gen" -> {
            hubViewModel.selectTool(TechToolKind.PASSWORD)
            TechToolsTabContent(uiState, language, strings, hubViewModel, onOpenInBrowser, modifier)
        }
        "currency_widget" -> {
            hubViewModel.selectTool(TechToolKind.CONVERTER)
            TechToolsTabContent(uiState, language, strings, hubViewModel, onOpenInBrowser, modifier)
        }
        "pomodoro_widget" -> {
            hubViewModel.selectTool(TechToolKind.POMODORO)
            TechToolsTabContent(uiState, language, strings, hubViewModel, onOpenInBrowser, modifier)
        }
        "weather_widget" -> {
            WeatherTabContent(
                uiState = uiState,
                language = language,
                strings = strings,
                onCityQueryChanged = hubViewModel::onWeatherCityQueryChanged,
                onSearchCity = { hubViewModel.searchWeatherCity(it, language) },
                onOpenInBrowser = onOpenInBrowser,
                modifier = modifier
            )
        }
        "news_widget" -> {
            NewsTabContent(
                uiState = uiState,
                language = language,
                strings = strings,
                onSelectSource = hubViewModel::selectNewsSource,
                onSearchQueryChanged = hubViewModel::onNewsSearchQueryChanged,
                onRefreshNews = hubViewModel::refreshNews,
                onOpenInBrowser = onOpenInBrowser,
                modifier = modifier
            )
        }
    }
}
