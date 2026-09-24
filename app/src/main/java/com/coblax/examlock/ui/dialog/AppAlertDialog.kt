package com.coblax.examlock.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.primaryActionColors
import com.coblax.examlock.ui.theme.resolveUiStatusColors

internal object AppAlertDialogTestTags {
    const val Dialog = "app_alert_dialog"
    const val PrimaryAction = "app_alert_dialog_primary"
    const val DetailsToggle = "app_alert_dialog_details_toggle"
}

internal enum class AppAlertStyle {
    /** A plain button: acknowledge, open settings, refresh. */
    Default,
    /** Leaves or ends something (exit exam, force exit). */
    Destructive
}

internal data class AppAlertAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val style: AppAlertStyle = AppAlertStyle.Default
)

/** One "label: value" line inside the collapsible technical details. */
internal data class AppAlertDetail(val label: String, val value: String)

/**
 * The shared popup for exam warnings, violations, and notices. Every popup reads the
 * same way: tone icon, title, optional count badge, a plain message, what to do next,
 * technical evidence folded away, then the buttons. It is still a real dialog window,
 * so it inherits FLAG_SECURE and the exam guard's internal-dialog handling as before.
 */
@Composable
internal fun AppAlertDialog(
    tone: UiStatusTone,
    icon: ImageVector,
    title: String,
    message: String,
    primaryAction: AppAlertAction?,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    dismissible: Boolean = false,
    badge: String? = null,
    nextStep: String? = null,
    details: List<AppAlertDetail> = emptyList(),
    secondaryActions: List<AppAlertAction> = emptyList(),
    progress: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        UpgradeUiScope {
            AppAlertDialogContent(
                tone = tone,
                icon = icon,
                title = title,
                message = message,
                primaryAction = primaryAction,
                modifier = modifier,
                badge = badge,
                nextStep = nextStep,
                details = details,
                secondaryActions = secondaryActions,
                progress = progress,
                extraContent = extraContent
            )
        }
    }
}

@Composable
internal fun AppAlertDialogContent(
    tone: UiStatusTone,
    icon: ImageVector,
    title: String,
    message: String,
    primaryAction: AppAlertAction?,
    modifier: Modifier = Modifier,
    badge: String? = null,
    nextStep: String? = null,
    details: List<AppAlertDetail> = emptyList(),
    secondaryActions: List<AppAlertAction> = emptyList(),
    progress: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val toneColors = resolveUiStatusColors(colors, tone)
    val shape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .testTag(AppAlertDialogTestTags.Dialog)
                .semantics { paneTitle = title }
                .clip(shape)
                .background(colors.cardBg)
                .border(1.dp, toneColors.borderColor.copy(alpha = 0.35f), shape)
        ) {
            // Scrollable body: long evidence never pushes the buttons off-screen.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(toneColors.containerColor)
                        .border(1.dp, toneColors.borderColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = toneColors.contentColor
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = toneColors.contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = AppTextStyles.sectionTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                if (badge != null) {
                    Text(
                        text = badge,
                        color = toneColors.contentColor,
                        style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(tokens.radiusPill))
                            .background(toneColors.containerColor)
                            .border(
                                1.dp,
                                toneColors.borderColor.copy(alpha = 0.35f),
                                RoundedCornerShape(tokens.radiusPill)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = message,
                    color = colors.textSecondary,
                    style = AppTextStyles.bodyCompact,
                    textAlign = TextAlign.Center
                )
                extraContent?.invoke()
                if (!nextStep.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.radiusMedium))
                            .background(colors.surfaceSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TipsAndUpdates,
                            contentDescription = null,
                            tint = colors.brandText,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = tr("What to do", "Yang perlu dilakukan"),
                                color = colors.textPrimary,
                                style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = nextStep,
                                color = colors.textPrimary,
                                style = AppTextStyles.bodyCompact
                            )
                        }
                    }
                }
                if (details.isNotEmpty()) {
                    AppAlertDetails(details = details)
                }
            }
            AppAlertButtons(
                primaryAction = primaryAction,
                secondaryActions = secondaryActions,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
            )
        }
    }
}

/**
 * Evidence for proctors and admins. Folded by default so a student sees the problem
 * and the fix first, not a wall of diagnostic fields.
 */
@Composable
private fun AppAlertDetails(details: List<AppAlertDetail>) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val expandedLabel = tr("Expanded", "Terbuka")
    val collapsedLabel = tr("Collapsed", "Tertutup")
    val shape = RoundedCornerShape(tokens.radiusMedium)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.outlineSubtle, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.touchTarget)
                .testTag(AppAlertDialogTestTags.DetailsToggle)
                .semantics {
                    stateDescription = if (expanded) expandedLabel else collapsedLabel
                }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Technical details", "Detail teknis"),
                color = colors.textSecondary,
                style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                details.forEach { detail ->
                    Column {
                        Text(
                            text = detail.label,
                            color = colors.textMuted,
                            style = AppTextStyles.label
                        )
                        Text(
                            text = detail.value.ifBlank { "-" },
                            color = colors.textPrimary,
                            style = AppTextStyles.diagnostic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppAlertButtons(
    primaryAction: AppAlertAction?,
    secondaryActions: List<AppAlertAction>,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    if (primaryAction == null && secondaryActions.isEmpty()) {
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        primaryAction?.let { action ->
            val fill = when (action.style) {
                AppAlertStyle.Destructive -> if (colors.isDark) {
                    colors.statusDanger to colors.background
                } else {
                    colors.dialogDangerIcon to colors.onDark
                }
                AppAlertStyle.Default -> primaryActionColors().let { it.container to it.content }
            }
            Button(
                onClick = action.onClick,
                enabled = action.enabled && !action.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.touchTarget)
                    .testTag(AppAlertDialogTestTags.PrimaryAction),
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = fill.first,
                    contentColor = fill.second,
                    disabledContainerColor = colors.surfaceSoft,
                    disabledContentColor = colors.textSecondary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AppAlertButtonContent(action)
            }
        }
        secondaryActions.forEach { action ->
            val contentColor = if (action.style == AppAlertStyle.Destructive) {
                if (colors.isDark) colors.statusDanger else colors.dialogDangerIcon
            } else {
                colors.brandText
            }
            if (primaryAction == null) {
                OutlinedButton(
                    onClick = action.onClick,
                    enabled = action.enabled && !action.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.touchTarget),
                    shape = RoundedCornerShape(tokens.radiusMedium),
                    border = BorderStroke(1.dp, colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = contentColor,
                        disabledContentColor = colors.textSecondary
                    )
                ) {
                    AppAlertButtonContent(action)
                }
            } else {
                TextButton(
                    onClick = action.onClick,
                    enabled = action.enabled && !action.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.touchTarget),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor,
                        disabledContentColor = colors.textSecondary
                    )
                ) {
                    AppAlertButtonContent(action)
                }
            }
        }
    }
}

@Composable
private fun AppAlertButtonContent(action: AppAlertAction) {
    if (action.loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
        text = action.label,
        style = AppTextStyles.button,
        textAlign = TextAlign.Center
    )
}

/** Shared "I understand" label so every acknowledgement reads the same. */
@Composable
internal fun acknowledgeLabel(): String = tr("I understand", "Saya mengerti")
