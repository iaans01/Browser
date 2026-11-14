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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.AppLanguage
import com.example.model.ExtensionCategory
import com.example.model.MurshedExtension
import com.example.model.MurshedHubUiState
import com.example.ui.theme.LocalAgentSemanticColors
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.MurshedHubViewModel

@Composable
fun StoreTabContent(
    uiState: MurshedHubUiState,
    catalog: List<MurshedExtension>,
    language: AppLanguage,
    strings: AppStrings,
    hubViewModel: MurshedHubViewModel,
    onLaunchSideExtension: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val semantics = LocalAgentSemanticColors.current

    val filteredExtensions = remember(
        catalog,
        uiState.storeSearchQuery,
        uiState.storeCategory,
        uiState.installedExtensionIds
    ) {
        val q = uiState.storeSearchQuery.trim().lowercase()
        catalog.filter { ext ->
            val catOk = when (uiState.storeCategory) {
                ExtensionCategory.ALL -> true
                ExtensionCategory.INSTALLED -> ext.id in uiState.installedExtensionIds
                else -> ext.category == uiState.storeCategory
            }
            val textOk = q.isBlank() ||
                ext.nameAr.lowercase().contains(q) ||
                ext.nameEn.lowercase().contains(q) ||
                ext.descAr.lowercase().contains(q) ||
                ext.descEn.lowercase().contains(q) ||
                ext.developer.lowercase().contains(q)
            catOk && textOk
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("hub_extension_store_tab"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chrome Web Store Style Hero Banner
        item(key = "extension_store_hero") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Extension,
                                        contentDescription = strings.hubTabStore,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isAr) "متجر إضافات مرشد" else "Murshed Extension Store",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAr) {
                                        "إضافات متصفح وأدوات جانبية تُثبّت داخل التطبيق"
                                    } else {
                                        "Chrome-style side extensions installed inside the app"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = semantics.act.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, semantics.act.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = if (isAr) {
                                    "${uiState.installedExtensionIds.size} مثبّت"
                                } else {
                                    "${uiState.installedExtensionIds.size} Installed"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = semantics.act,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.PushPin, contentDescription = null, tint = semantics.observe, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isAr) {
                                        "مثبّت في الشريط السريع: ${uiState.pinnedExtensionIds.size}"
                                    } else {
                                        "Pinned to Quick Bar: ${uiState.pinnedExtensionIds.size}"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { hubViewModel.selectStoreCategory(ExtensionCategory.INSTALLED) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("filter_installed_extensions_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAr) "المثبّتة" else "Installed", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Search + Category Filter Chips
        item(key = "extension_store_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.storeSearchQuery,
                    onValueChange = hubViewModel::onStoreSearchChanged,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = {
                        Text(if (isAr) "ابحث في إضافات مرشد..." else "Search extensions...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extension_store_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExtensionCategory.entries.forEach { cat ->
                        val label = when (cat) {
                            ExtensionCategory.ALL -> if (isAr) "الكل (${catalog.size})" else "All (${catalog.size})"
                            ExtensionCategory.INSTALLED -> if (isAr) "المثبّتة (${uiState.installedExtensionIds.size})" else "Installed (${uiState.installedExtensionIds.size})"
                            ExtensionCategory.READER_PRIVACY -> if (isAr) "قراءة وخصوصية" else "Reader & Privacy"
                            ExtensionCategory.DATA_EXTRACTOR -> if (isAr) "استخراج البيانات" else "Data Extractor"
                            ExtensionCategory.DEVELOPER -> if (isAr) "المطورين" else "Developer"
                            ExtensionCategory.MINI_WIDGETS -> if (isAr) "مصغرات حية" else "Mini-Widgets"
                        }
                        FilterChip(
                            selected = uiState.storeCategory == cat,
                            onClick = { hubViewModel.selectStoreCategory(cat) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("ext_category_${cat.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Extension Cards
        items(
            items = filteredExtensions,
            key = { it.id }
        ) { ext ->
            val isInstalled = ext.id in uiState.installedExtensionIds
            val isPinned = ext.id in uiState.pinnedExtensionIds
            val accent = extensionAccentColor(ext.accentRole)
            val name = if (isAr) ext.nameAr else ext.nameEn
            val desc = if (isAr) ext.descAr else ext.descEn
            val permission = if (isAr) ext.permissionAr else ext.permissionEn

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isInstalled) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extension_card_${ext.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = extensionIconFor(ext.id),
                                    contentDescription = name,
                                    tint = accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isInstalled) {
                                    Surface(
                                        color = semantics.act.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(99.dp)
                                    ) {
                                        Text(
                                            text = strings.installedBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semantics.act,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${ext.developer} • v${ext.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = semantics.recover, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${ext.rating} (${ext.installsLabel})",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpaceGroteskFontFamily)
                            )
                        }
                    }

                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Capability / Permission Badge (Chrome Web Store style)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = (if (isAr) "الصلاحية: " else "Access: ") + permission,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isInstalled) {
                            Button(
                                onClick = { onLaunchSideExtension(ext.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ext_open_${ext.id}")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.openSideToolBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            IconButton(
                                onClick = { hubViewModel.togglePinExtension(ext.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("ext_pin_${ext.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) strings.unpinExtBtn else strings.pinExtBtn,
                                    tint = if (isPinned) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { hubViewModel.toggleInstallExtension(ext.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("ext_remove_${ext.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = strings.uninstallExtBtn,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { hubViewModel.toggleInstallExtension(ext.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ext_install_${ext.id}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.installExtBtn, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { onLaunchSideExtension(ext.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("ext_preview_${ext.id}")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = strings.openSideToolBtn, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "معاينة" else "Try", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
