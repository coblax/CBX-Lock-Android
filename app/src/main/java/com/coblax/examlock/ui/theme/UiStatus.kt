package com.coblax.examlock.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal enum class UiStatusTone {
    Neutral,
    Info,
    Success,
    Warning,
    Danger
}

internal enum class UiStatusIcon {
    Check,
    Info,
    Warning,
    Blocked,
    Loading
}

@Immutable
internal data class UiStatusVisual(
    val tone: UiStatusTone,
    val icon: UiStatusIcon,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

@Immutable
internal data class UiStatusColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

internal fun defaultUiStatusIcon(tone: UiStatusTone): UiStatusIcon =
    when (tone) {
        UiStatusTone.Neutral -> UiStatusIcon.Info
        UiStatusTone.Info -> UiStatusIcon.Info
        UiStatusTone.Success -> UiStatusIcon.Check
        UiStatusTone.Warning -> UiStatusIcon.Warning
        UiStatusTone.Danger -> UiStatusIcon.Blocked
    }

internal fun uiStatusSeverityRank(tone: UiStatusTone): Int =
    when (tone) {
        UiStatusTone.Neutral -> 0
        UiStatusTone.Info -> 1
        UiStatusTone.Success -> 1
        UiStatusTone.Warning -> 2
        UiStatusTone.Danger -> 3
    }

/**
 * Resolves opaque status surfaces with readable foreground colors.
 *
 * Some palette accents are intentionally suitable for icons or borders but
 * are too light for small status text. Keep those accents as the border while
 * selecting a stronger foreground on light surfaces. The informational fill
 * is composited here so its contrast does not depend on the parent surface.
 */
internal fun resolveUiStatusColors(
    colors: ExamLockColorPalette,
    tone: UiStatusTone
): UiStatusColors =
    when (tone) {
        UiStatusTone.Neutral -> UiStatusColors(
            containerColor = colors.surfaceSoft,
            contentColor = colors.textSecondary,
            borderColor = colors.outline
        )
        UiStatusTone.Info -> UiStatusColors(
            containerColor = colors.blueTint.compositeOver(colors.cardBg),
            contentColor = if (colors.isDark) colors.blue else colors.blueDeep,
            borderColor = colors.blue
        )
        UiStatusTone.Success -> UiStatusColors(
            containerColor = colors.statusSafeFill,
            contentColor = if (colors.isDark) colors.statusSafe else colors.safeStrong,
            borderColor = colors.statusSafe
        )
        UiStatusTone.Warning -> UiStatusColors(
            containerColor = colors.statusWarnFill,
            contentColor = if (colors.isDark) colors.statusWarn else colors.textPrimary,
            borderColor = colors.statusWarn
        )
        UiStatusTone.Danger -> UiStatusColors(
            containerColor = colors.statusDangerFill,
            contentColor = if (colors.isDark) colors.statusDanger else colors.dialogDangerIcon,
            borderColor = colors.statusDanger
        )
    }

@Composable
internal fun resolveUiStatusVisual(
    tone: UiStatusTone,
    label: String,
    icon: UiStatusIcon = defaultUiStatusIcon(tone)
): UiStatusVisual {
    val colors = AppColors.current
    return remember(colors, tone, icon, label) {
        val semanticColors = resolveUiStatusColors(colors, tone)
        UiStatusVisual(
            tone = tone,
            icon = icon,
            label = label,
            containerColor = semanticColors.containerColor,
            contentColor = semanticColors.contentColor,
            borderColor = semanticColors.borderColor
        )
    }
}

private fun uiStatusImageVector(icon: UiStatusIcon): ImageVector =
    when (icon) {
        UiStatusIcon.Check -> Icons.Rounded.CheckCircle
        UiStatusIcon.Info -> Icons.Rounded.Info
        UiStatusIcon.Warning -> Icons.Rounded.Warning
        UiStatusIcon.Blocked -> Icons.Rounded.Block
        UiStatusIcon.Loading -> Icons.Rounded.Sync
    }

@Composable
internal fun StatusBadge(
    label: String,
    tone: UiStatusTone,
    modifier: Modifier = Modifier,
    icon: UiStatusIcon = defaultUiStatusIcon(tone),
    semanticLabel: String = label
) {
    val visual = resolveUiStatusVisual(tone = tone, label = label, icon = icon)
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(tokens.radiusPill)

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                stateDescription = semanticLabel
            }
            .clip(shape)
            .background(visual.containerColor)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = visual.borderColor.copy(alpha = 0.42f)
                ),
                shape
            )
            .heightIn(min = 32.dp)
            .padding(horizontal = tokens.spaceSmall, vertical = tokens.spaceXSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = uiStatusImageVector(visual.icon),
            contentDescription = null,
            tint = visual.contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = visual.label,
            color = visual.contentColor,
            style = AppTextStyles.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: UiStatusTone = UiStatusTone.Info,
    title: String? = null,
    icon: UiStatusIcon = defaultUiStatusIcon(tone),
    semanticLabel: String = title ?: message,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val visual = resolveUiStatusVisual(
        tone = tone,
        label = title ?: semanticLabel,
        icon = icon
    )
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(tokens.radiusMedium)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = semanticLabel
            }
            .clip(shape)
            .background(visual.containerColor)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = visual.borderColor.copy(alpha = 0.45f)
                ),
                shape
            )
            .padding(
                horizontal = tokens.spaceMedium,
                vertical = tokens.spaceSmall
            ),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(tokens.radiusSmall))
                .background(visual.contentColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = uiStatusImageVector(visual.icon),
                contentDescription = null,
                tint = visual.contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(tokens.spaceSmall))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(tokens.spaceXSmall)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = visual.contentColor,
                    style = AppTextStyles.cardTitle
                )
            }
            Text(
                text = message,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.bodyCompact
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = tokens.touchTarget),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = visual.contentColor
                    )
                ) {
                    Text(text = actionLabel, style = AppTextStyles.button)
                }
            }
        }
    }
}

@Composable
internal fun StatusSummaryCard(
    title: String,
    message: String,
    tone: UiStatusTone,
    modifier: Modifier = Modifier,
    statusLabel: String = title,
    supportingText: String? = null,
    icon: UiStatusIcon = defaultUiStatusIcon(tone),
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tokens = ScopedUiTokens.current
    val visual = resolveUiStatusVisual(
        tone = tone,
        label = statusLabel,
        icon = icon
    )
    val shape = RoundedCornerShape(tokens.radiusLarge)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = statusLabel
            }
            .clip(shape)
            .background(AppColors.current.cardBg)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = visual.borderColor.copy(alpha = 0.42f)
                ),
                shape
            )
            .padding(tokens.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(tokens.spaceSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = AppColors.current.textPrimary,
                style = AppTextStyles.cardTitle
            )
            Spacer(Modifier.width(tokens.spaceSmall))
            StatusBadge(
                label = statusLabel,
                tone = tone,
                icon = icon
            )
        }
        Text(
            text = message,
            color = AppColors.current.textPrimary,
            style = AppTextStyles.body
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = AppColors.current.textSecondary,
                style = AppTextStyles.bodyCompact
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = tokens.touchTarget),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = visual.contentColor
                )
            ) {
                Text(text = actionLabel, style = AppTextStyles.button)
            }
        }
    }
}
