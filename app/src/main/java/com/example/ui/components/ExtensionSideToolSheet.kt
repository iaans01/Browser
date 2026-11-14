package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.WebViewDomBridge
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.MurshedHubUiState
import com.example.model.ScreenSnapshot
import com.example.viewmodel.MurshedHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSideToolSheet(
    extensionId: String,
    uiState: MurshedHubUiState,
    activeUrl: String,
    domSnapshot: ScreenSnapshot?,
    domBridge: WebViewDomBridge,
    language: AppLanguage,
    strings: AppStrings,
    hubViewModel: MurshedHubViewModel,
    onReadScreen: () -> Unit,
    onHighlightElement: (String) -> Unit,
    onOpenInBrowser: (String, String?) -> Unit,
    onOpenExtensionStore: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAr = language == AppLanguage.ARABIC
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = hubViewModel.extensionCatalog.firstOrNull { it.id == extensionId } ?: return
    val isPinned = ext.id in uiState.pinnedExtensionIds
    val accent = extensionAccentColor(ext.accentRole)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("extension_side_tool_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
        ) {
            // Side Extension Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = extensionIconFor(ext.id),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) ext.nameAr else ext.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${ext.developer} • v${ext.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { hubViewModel.togglePinExtension(ext.id) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) strings.unpinExtBtn else strings.pinExtBtn,
                        tint = if (isPinned) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = strings.dismissBtn)
                }
            }

            // Quick Switcher Bar across installed extensions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hubViewModel.extensionCatalog
                    .filter { it.id in uiState.installedExtensionIds }
                    .forEach { item ->
                        FilterChip(
                            selected = item.id == extensionId,
                            onClick = {
                                hubViewModel.openSideExtension(item.id, activeUrl, domBridge, language)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = extensionIconFor(item.id),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isAr) item.shortNameAr else item.shortNameEn,
                                    maxLines = 1
                                )
                            }
                        )
                    }

                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenExtensionStore()
                    }
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" ${strings.manageExtensionsBtn}")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (extensionId) {
                    "clean_reader" -> ExtensionCleanReaderPanel(
                        uiState = uiState,
                        language = language,
                        hubViewModel = hubViewModel,
                        onRefreshFromPage = { hubViewModel.refreshPageExtraction(domBridge) }
                    )

                    "ai_summarizer" -> ExtensionAiSummarizerPanel(
                        uiState = uiState,
                        language = language,
                        onSummarizeNow = { hubViewModel.summarizeCurrentPage(activeUrl, domBridge, language) }
                    )

                    "privacy_cleaner" -> ExtensionPrivacyCleanerPanel(
                        uiState = uiState,
                        language = language,
                        hubViewModel = hubViewModel,
                        onOpenCleanUrl = { cleanUrl ->
                            onDismiss()
                            onOpenInBrowser(cleanUrl, null)
                        }
                    )

                    "web_extractor" -> ExtensionWebExtractorPanel(
                        uiState = uiState,
                        language = language,
                        hubViewModel = hubViewModel,
                        onRefreshFromPage = { hubViewModel.refreshPageExtraction(domBridge) },
                        onOpenUrl = { url ->
                            onDismiss()
                            onOpenInBrowser(url, null)
                        }
                    )

                    "dom_inspector" -> ExtensionDomInspectorPanel(
                        uiState = uiState,
                        snapshot = domSnapshot,
                        language = language,
                        strings = strings,
                        onReadScreen = onReadScreen,
                        onHighlightElement = onHighlightElement
                    )

                    else -> ExtensionDevToolsDelegatedPanel(
                        extensionId = extensionId,
                        uiState = uiState,
                        language = language,
                        strings = strings,
                        hubViewModel = hubViewModel,
                        onOpenInBrowser = { url, goal ->
                            onDismiss()
                            onOpenInBrowser(url, goal)
                        }
                    )
                }
            }
        }
    }
}
