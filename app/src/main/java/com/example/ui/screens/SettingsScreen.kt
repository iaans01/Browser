package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import com.example.i18n.AppStrings
import com.example.model.AgentUiState
import com.example.model.AiEngineSource
import com.example.model.AppLanguage
import com.example.ui.theme.LocalAgentSemanticColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: AgentUiState,
    strings: AppStrings,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (Boolean) -> Unit,
    onMaxStepsChanged: (Int) -> Unit,
    onStepDelayChanged: (Long) -> Unit,
    onCloudGeminiToggled: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backWebBtn
                    )
                }
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Language Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.languageSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_language_selector")
                    ) {
                        SegmentedButton(
                            selected = uiState.language == AppLanguage.ARABIC,
                            onClick = { onLanguageSelected(AppLanguage.ARABIC) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("select_lang_ar")
                        ) {
                            Text(strings.arabicLabel)
                        }
                        SegmentedButton(
                            selected = uiState.language == AppLanguage.ENGLISH,
                            onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("select_lang_en")
                        ) {
                            Text(strings.englishLabel)
                        }
                    }
                }
            }

            // Theme Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.appearanceSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_theme_selector")
                    ) {
                        SegmentedButton(
                            selected = uiState.isDarkTheme,
                            onClick = { onThemeSelected(true) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("select_theme_dark")
                        ) {
                            Text(strings.darkTelemetryLabel)
                        }
                        SegmentedButton(
                            selected = !uiState.isDarkTheme,
                            onClick = { onThemeSelected(false) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("select_theme_light")
                        ) {
                            Text(strings.lightStudioLabel)
                        }
                    }
                }
            }

            // Loop Tuning Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.loopConfigSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = strings.maxStepsLabel(uiState.maxSteps),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.maxSteps.toFloat(),
                        onValueChange = { onMaxStepsChanged(it.roundToInt()) },
                        valueRange = 3f..25f,
                        steps = 21,
                        modifier = Modifier.testTag("slider_max_steps")
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = strings.stepDelayLabel(uiState.stepDelayMs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.stepDelayMs.toFloat(),
                        onValueChange = {
                            val snapped = ((it / 100f).roundToInt() * 100L).coerceIn(300L, 2500L)
                            onStepDelayChanged(snapped)
                        },
                        valueRange = 300f..2500f,
                        modifier = Modifier.testTag("slider_step_delay")
                    )
                }
            }

            // Protocol Reference Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.agentProtocolTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.agentProtocolBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Hybrid AI Engine & Cloud Gemini Toggle Card
            val semantics = LocalAgentSemanticColors.current
            val isLocalActive = !uiState.cloudGeminiEnabled || uiState.activeAiEngine == AiEngineSource.LOCAL_LITE_FALLBACK
            val badgeAccent = if (isLocalActive) semantics.recover else semantics.observe
            val badgeLabel = when {
                !uiState.cloudGeminiEnabled -> strings.engineLocalManualLabel
                uiState.activeAiEngine == AiEngineSource.LOCAL_LITE_FALLBACK -> strings.engineLocalLabel
                else -> strings.engineCloudLabel
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_ai_engine_card")
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
                        Text(
                            text = strings.aiEngineSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = badgeAccent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeAccent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.cloudGeminiToggleTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.cloudGeminiEnabled) {
                                    strings.cloudGeminiEnabledDesc
                                } else {
                                    strings.cloudGeminiDisabledDesc
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.cloudGeminiEnabled) semantics.observe else semantics.recover
                            )
                        }
                        Switch(
                            checked = uiState.cloudGeminiEnabled,
                            onCheckedChange = onCloudGeminiToggled,
                            modifier = Modifier.testTag("switch_cloud_gemini")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text(
                        text = strings.aiEnginePolicyDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val isAr = uiState.language == AppLanguage.ARABIC
                    Text(
                        text = if (isAr) {
                            "عمليات التحويل التلقائي في هذه الجلسة: ${uiState.aiFallbackCount}" +
                                (uiState.lastFallbackReason?.let { " • آخر سبب: $it" } ?: "")
                        } else {
                            "Auto-failovers this session: ${uiState.aiFallbackCount}" +
                                (uiState.lastFallbackReason?.let { " • Last reason: $it" } ?: "")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeAccent
                    )
                }
            }
        }
    }
}
