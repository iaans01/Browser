package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.SandboxPages
import com.example.i18n.AppStrings
import com.example.model.AgentPhase
import com.example.model.AgentStepLog
import com.example.model.AppLanguage
import androidx.compose.foundation.layout.heightIn
import com.example.model.CommandTaskPlan
import com.example.model.DomElement
import com.example.model.FailureCategory
import com.example.model.FailureDiagnosis
import com.example.model.SubStepStatus
import com.example.model.TaskPreset
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAgentSemanticColors

@Composable
fun PhaseStatusChip(
    phase: AgentPhase,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val accentColor by animateColorAsState(
        targetValue = when (phase) {
            AgentPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
            AgentPhase.OBSERVING -> semantics.observe
            AgentPhase.THINKING -> semantics.think
            AgentPhase.ACTING -> semantics.act
            AgentPhase.RECOVERING -> semantics.recover
            AgentPhase.WAITING_FOR_USER -> semantics.recover
            AgentPhase.COMPLETED -> semantics.act
            AgentPhase.ERROR -> MaterialTheme.colorScheme.error
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "phaseColor"
    )

    Surface(
        modifier = modifier.testTag("phase_status_chip"),
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = strings.phaseLabel(phase),
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CommandTaskPlanCard(
    plan: CommandTaskPlan,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val isAr = language == AppLanguage.ARABIC
    var expanded by rememberSaveable(plan.rawCommand) { mutableStateOf(true) }
    val doneCount = plan.steps.count { it.status == SubStepStatus.COMPLETED }
    val totalCount = plan.steps.size.coerceAtLeast(1)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, semantics.observe.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("command_task_plan_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = semantics.observe,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.planCardTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = semantics.act.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = strings.planStepsProgress(doneCount, totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = semantics.act,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_task_plan_expand")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = strings.planCardTitle,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (doneCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = semantics.act,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val hasEntities = !plan.entities.targetDomain.isNullOrBlank() ||
                        !plan.entities.email.isNullOrBlank() ||
                        plan.entities.authIntent != com.example.model.AuthIntentKind.NONE
                    if (hasEntities) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (plan.entities.authIntent != com.example.model.AuthIntentKind.NONE) {
                                val isSignupMode = plan.entities.authIntent == com.example.model.AuthIntentKind.SIGN_UP
                                val badgeColor = if (isSignupMode) semantics.act else semantics.recover
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("detected_auth_intent_chip")
                                ) {
                                    Text(
                                        text = if (isSignupMode) strings.authModeSignupBadge else strings.authModeLoginBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            plan.entities.targetDomain?.takeIf { it.isNotBlank() }?.let { host ->
                                Surface(
                                    color = semantics.observe.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = strings.entitySiteLabel(host),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semantics.observe,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            plan.entities.email?.takeIf { it.isNotBlank() }?.let { mail ->
                                Surface(
                                    color = semantics.think.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = strings.entityEmailLabel(mail),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semantics.think,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    plan.steps.forEach { step ->
                        val (icon, tint) = when (step.status) {
                            SubStepStatus.COMPLETED -> Icons.Filled.CheckCircle to semantics.act
                            SubStepStatus.WAITING_FOR_USER -> Icons.Filled.TouchApp to semantics.recover
                            SubStepStatus.IN_PROGRESS -> Icons.Filled.PlayArrow to semantics.observe
                            SubStepStatus.FAILED -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
                            SubStepStatus.PENDING -> Icons.Filled.PlayArrow to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_substep_${step.index}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${step.index}. ${if (isAr) step.titleAr else step.titleEn}" +
                                    if (step.detail.isNotBlank() && step.kind == com.example.model.SubStepKind.FILL_EMAIL) {
                                        " (${step.detail})"
                                    } else {
                                        ""
                                    },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (step.status == SubStepStatus.IN_PROGRESS || step.status == SubStepStatus.WAITING_FOR_USER) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (step.status == SubStepStatus.PENDING) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepLogCard(
    log: AgentStepLog,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current
    var jsonExpanded by rememberSaveable(log.stepNumber) { mutableStateOf(true) }
    var copiedNotice by remember { mutableStateOf(false) }

    val execSuccess = log.executionResult?.success ?: true
    val statusColor = when {
        log.phase == AgentPhase.COMPLETED -> semantics.act
        !execSuccess || log.phase == AgentPhase.RECOVERING -> semantics.recover
        log.phase == AgentPhase.ERROR -> MaterialTheme.colorScheme.error
        else -> semantics.observe
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("step_log_card_${log.stepNumber}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = strings.stepLabel(log.stepNumber),
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val actionSummary = buildString {
                            append(log.decision.action.wireName)
                            if (log.decision.targetAgentId.isNotBlank()) {
                                append(" ")
                                append(log.decision.targetAgentId)
                            }
                        }
                        Text(
                            text = actionSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (execSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = strings.executionLabel,
                    tint = if (execSuccess) semantics.act else semantics.recover,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (log.decision.thought.isNotBlank()) {
                Text(
                    text = log.decision.thought,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (log.decision.recoveryAnalysis.isNotBlank()) {
                Surface(
                    color = semantics.recover.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, semantics.recover.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.recoveryLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = semantics.recover
                        )
                        Text(
                            text = log.decision.recoveryAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (log.executionResult != null && log.executionResult.message.isNotBlank()) {
                Text(
                    text = "${strings.executionLabel}: ${log.executionResult.message}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (execSuccess) MaterialTheme.colorScheme.onSurfaceVariant else semantics.recover
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { jsonExpanded = !jsonExpanded },
                    modifier = Modifier.testTag("toggle_json_${log.stepNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = strings.rawJsonLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(strings.rawJsonLabel)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (jsonExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(log.decision.rawJson))
                        copiedNotice = true
                    },
                    modifier = Modifier.testTag("copy_json_${log.stepNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = strings.copyJson,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (copiedNotice) strings.copiedToast else strings.copyJson)
                }
            }

            AnimatedVisibility(visible = jsonExpanded) {
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
                            text = log.decision.rawJson,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = semantics.observe
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DomElementCard(
    element: DomElement,
    strings: AppStrings,
    onHighlightClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val clipboard = LocalClipboardManager.current
    var selectorCopied by remember(element.agentId, element.selector) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onHighlightClick(element.agentId) }
            .testTag("dom_node_${element.agentId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = semantics.observe.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = element.agentId,
                                style = MaterialTheme.typography.labelSmall,
                                color = semantics.observe,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "<${element.tag}>",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = semantics.think
                        )
                        if (element.authRole.isNotBlank()) {
                            val roleAccent = if (element.authRole == "signup") semantics.act else semantics.recover
                            Surface(
                                color = roleAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = strings.authRoleBadge(element.authRole),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = roleAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!element.isEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = strings.disabledBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.Highlight,
                        contentDescription = element.agentId,
                        tint = semantics.observe,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val primaryLabel = element.text.ifBlank {
                element.placeholder.ifBlank { element.labelText.ifBlank { element.ariaLabel.ifBlank { element.value.ifBlank { element.selector } } } }
            }
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val htmlBadges = buildList {
                if (element.elementId.isNotBlank()) add("#${element.elementId}")
                if (element.nameAttr.isNotBlank()) add("[name=${element.nameAttr}]")
                if (element.inputType.isNotBlank() && element.tag == "input") add("type=${element.inputType}")
                if (element.cssClasses.isNotBlank()) {
                    val classes = element.cssClasses.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(3)
                    if (classes.isNotEmpty()) {
                        add(classes.joinToString("") { ".$it" })
                    }
                }
            }
            if (htmlBadges.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        htmlBadges.forEach { badge ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            val metaParts = buildList {
                if (element.containerSignature.isNotBlank()) {
                    add(element.containerSignature)
                } else if (element.containerId.isNotBlank()) {
                    val headingSuffix = element.containerHeading.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
                    add("[${element.containerId}$headingSuffix]")
                } else if (element.sectionContext.isNotBlank()) {
                    add("<${element.sectionContext}>")
                }
                if (element.ariaSelected != null) add("selected=${element.ariaSelected}")
                if (element.labelText.isNotBlank() && element.labelText != primaryLabel) add("label=\"${element.labelText}\"")
                if (element.value.isNotBlank() && element.value != primaryLabel) add("val=\"${element.value}\"")
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = semantics.act,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (element.selector.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = element.selector,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = semantics.observe,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(element.selector))
                                    selectorCopied = true
                                },
                                modifier = Modifier.testTag("copy_selector_${element.agentId}")
                            ) {
                                Icon(
                                    imageVector = if (selectorCopied) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                    contentDescription = if (selectorCopied) strings.copiedSelectorToast else strings.copySelectorBtn,
                                    tint = if (selectorCopied) semantics.act else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
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
fun PresetCard(
    preset: TaskPreset,
    language: AppLanguage,
    strings: AppStrings,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (language == AppLanguage.ARABIC) preset.titleAr else preset.titleEn
    val goal = if (language == AppLanguage.ARABIC) preset.goalAr else preset.goalEn

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preset_card_${preset.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onApply,
                    modifier = Modifier.testTag("apply_preset_${preset.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = strings.presetUseBtn,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.presetUseBtn)
                }
            }
            Text(
                text = goal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = preset.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FailureDiagnosisCard(
    diagnosis: FailureDiagnosis,
    strings: AppStrings,
    onRetry: () -> Unit,
    onInspectDom: () -> Unit,
    onResumeAfterFix: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalAgentSemanticColors.current
    val isWarningTone = diagnosis.category == FailureCategory.AUTH_OR_CAPTCHA ||
        diagnosis.category == FailureCategory.MISSING_USER_DATA
    val accent = if (isWarningTone) semantics.recover else MaterialTheme.colorScheme.error
    val containerColor = if (isWarningTone) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
    }
    val contentColor = if (isWarningTone) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_error_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = strings.failureDiagnosisHeader,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = strings.failureCategoryLabel(diagnosis.category),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = diagnosis.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_diagnosis_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.dismissBtn,
                        tint = contentColor
                    )
                }
            }

            Text(
                text = diagnosis.causeSummary,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )

            if (diagnosis.evidenceDetail.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = diagnosis.evidenceDetail,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = contentColor.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (diagnosis.suggestedFixes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.failureFixesTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    diagnosis.suggestedFixes.take(2).forEach { fix ->
                        Text(
                            text = "• $fix",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (diagnosis.canResumeAfterFix) {
                    Button(
                        onClick = onResumeAfterFix,
                        colors = ButtonDefaults.buttonColors(containerColor = semantics.recover),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("diagnosis_resume_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = strings.resumeAgentBtn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.resumeAgentBtn, maxLines = 1)
                    }
                }
                if (diagnosis.canRetry) {
                    FilledTonalButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("diagnosis_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.retryBtn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.retryBtn, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = onInspectDom,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("diagnosis_inspect_dom_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = strings.viewDomBtn,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.viewDomBtn, maxLines = 1)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberManagedWebView(
    generation: Int,
    initialUrl: String,
    onAttachWebView: (WebView) -> Unit,
    onDetachWebView: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onRendererCrashed: () -> Unit,
    onWebError: (String) -> Unit = {}
): WebView {
    val context = LocalContext.current
    val latestOnPageStarted by rememberUpdatedState(onPageStarted)
    val latestOnPageFinished by rememberUpdatedState(onPageFinished)
    val latestOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val latestOnRendererCrashed by rememberUpdatedState(onRendererCrashed)
    val latestOnWebError by rememberUpdatedState(onWebError)

    val webView = remember(generation) {
        WebView(context).apply {
            val defaultUa = WebSettings.getDefaultUserAgent(context)
            settings.userAgentString = defaultUa
                .replace("; wv)", ")")
                .replace("; wv;", ";")
                .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            val targetWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(targetWebView, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return !(url.startsWith("http://") || url.startsWith("https://"))
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let(latestOnPageStarted)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    runCatching { CookieManager.getInstance().flush() }
                    latestOnPageFinished(url ?: initialUrl, view?.title)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString().orEmpty()
                        val code = error?.errorCode ?: 0
                        latestOnWebError("Network error ($code): $desc".trim())
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        val status = errorResponse?.statusCode ?: 0
                        if (status >= 400) {
                            val reason = errorResponse?.reasonPhrase.orEmpty()
                            latestOnWebError("HTTP $status $reason".trim())
                        }
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.w("MurshedWebView", "Render process gone (didCrash=${detail?.didCrash()}); recovering.")
                    latestOnRendererCrashed()
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    latestOnProgressChanged(newProgress)
                }
            }
            if (initialUrl == SandboxPages.SANDBOX_URL || initialUrl.isBlank()) {
                loadDataWithBaseURL(
                    SandboxPages.SANDBOX_URL,
                    SandboxPages.sandboxHtml,
                    "text/html",
                    "UTF-8",
                    SandboxPages.SANDBOX_URL
                )
            } else {
                loadUrl(initialUrl)
            }
        }
    }

    DisposableEffect(webView) {
        onAttachWebView(webView)
        onDispose {
            onDetachWebView(webView)
            (webView.parent as? ViewGroup)?.removeView(webView)
            runCatching {
                webView.stopLoading()
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }

    return webView
}

@Composable
fun LiveBrowserViewport(
    webView: WebView,
    loadingProgress: Int,
    isPageLoading: Boolean,
    modifier: Modifier = Modifier
) {
    DisposableEffect(webView) {
        webView.onResume()
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_webview_container")
    ) {
        AndroidView(
            factory = {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            },
            onRelease = { view ->
                (view.parent as? ViewGroup)?.removeView(view)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPageLoading) {
            LinearProgressIndicator(
                progress = { (loadingProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}
