package com.coblax.examlock.ui.exam

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens

@Composable
internal fun ExamWebViewBottomBar(
    showArrowControls: Boolean,
    isRefreshing: Boolean,
    onToggleArrowControls: () -> Unit,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lowRamProfile = LocalLowRamProfile.current
    val tokens = ScopedUiTokens.current

    BoxWithConstraints(modifier = modifier) {
        val layoutSpec = calculateExamFooterLayoutSpec(
            maxWidthDp = maxWidth.value.toInt(),
            lowRamEnabled = lowRamProfile.enabled,
            lowRamSevere = lowRamProfile.severe
        )
        val actionSpacing = layoutSpec.actionSpacingDp.dp

        LaunchedEffect(
            layoutSpec.layoutMode,
            maxWidth.value.toInt(),
            lowRamProfile.enabled,
            lowRamProfile.severe
        ) {
            Log.i(
                "ExamRuntimeHardening",
                "${ExamRuntimeHardeningDiagnostics.FooterLayoutMode} " +
                    "mode=${layoutSpec.layoutMode.name} " +
                    "width_dp=${maxWidth.value.toInt()} " +
                    "low_ram=${lowRamProfile.enabled} " +
                    "severe=${lowRamProfile.severe}"
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutSpec.railHeightDp.dp)
                .testTag(ExamRuntimeUiTestTags.BottomActionRail),
            shape = RoundedCornerShape(
                topStart = tokens.radiusMedium,
                topEnd = tokens.radiusMedium,
                bottomStart = layoutSpec.cornerRadiusDp.dp,
                bottomEnd = layoutSpec.cornerRadiusDp.dp
            ),
            color = AppColors.current.cardBg,
            border = BorderStroke(1.dp, AppColors.current.outlineSubtle)
        ) {
            if (layoutSpec.compact) {
                CompactActionRow(
                    layoutSpec = layoutSpec,
                    showArrowControls = showArrowControls,
                    isRefreshing = isRefreshing,
                    onToggleArrowControls = onToggleArrowControls,
                    onRefresh = onRefresh,
                    onGoHome = onGoHome,
                    modifier = Modifier.padding(
                        horizontal = layoutSpec.horizontalPaddingDp.dp,
                        vertical = 2.dp
                    )
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = layoutSpec.horizontalPaddingDp.dp,
                            vertical = 2.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArrowToggleAction(
                        visible = showArrowControls,
                        fullLabel = layoutSpec.showFullActionLabels,
                        touchTarget = layoutSpec.touchTargetDp.dp,
                        iconSize = layoutSpec.iconSizeDp.dp,
                        modifier = Modifier.widthIn(min = 124.dp, max = 168.dp),
                        onClick = onToggleArrowControls
                    )
                    Spacer(Modifier.weight(1f))
                    RefreshAction(
                        isRefreshing = isRefreshing,
                        fullLabel = layoutSpec.showFullActionLabels,
                        touchTarget = layoutSpec.touchTargetDp.dp,
                        iconSize = layoutSpec.iconSizeDp.dp,
                        modifier = Modifier.widthIn(min = 124.dp, max = 168.dp),
                        onClick = onRefresh
                    )
                    ExitAction(
                        fullLabel = layoutSpec.showFullActionLabels,
                        touchTarget = layoutSpec.touchTargetDp.dp,
                        iconSize = layoutSpec.iconSizeDp.dp,
                        modifier = Modifier.widthIn(min = 124.dp, max = 168.dp),
                        onClick = onGoHome
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactActionRow(
    layoutSpec: ExamFooterLayoutSpec,
    showArrowControls: Boolean,
    isRefreshing: Boolean,
    onToggleArrowControls: () -> Unit,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layoutSpec.actionSpacingDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArrowToggleAction(
            visible = showArrowControls,
            fullLabel = false,
            touchTarget = layoutSpec.touchTargetDp.dp,
            iconSize = layoutSpec.iconSizeDp.dp,
            modifier = Modifier.weight(1f),
            onClick = onToggleArrowControls
        )
        RefreshAction(
            isRefreshing = isRefreshing,
            fullLabel = false,
            touchTarget = layoutSpec.touchTargetDp.dp,
            iconSize = layoutSpec.iconSizeDp.dp,
            modifier = Modifier.weight(1f),
            onClick = onRefresh
        )
        ExitAction(
            fullLabel = false,
            touchTarget = layoutSpec.touchTargetDp.dp,
            iconSize = layoutSpec.iconSizeDp.dp,
            modifier = Modifier.weight(1f),
            onClick = onGoHome
        )
    }
}

@Composable
private fun ArrowToggleAction(
    visible: Boolean,
    fullLabel: Boolean,
    touchTarget: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    val label = if (fullLabel) {
        if (visible) {
            tr("Hide arrows", "Sembunyikan panah")
        } else {
            tr("Show arrows", "Tampilkan panah")
        }
    } else {
        tr("Arrows", "Panah")
    }
    val description = if (visible) {
        tr("Hide side arrow controls", "Sembunyikan kontrol panah samping")
    } else {
        tr("Show side arrow controls", "Tampilkan kontrol panah samping")
    }
    val state = if (visible) {
        tr("Arrow controls shown", "Kontrol panah ditampilkan")
    } else {
        tr("Arrow controls hidden", "Kontrol panah disembunyikan")
    }

    ExamRuntimeActionButton(
        label = label,
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
        contentDescriptionText = description,
        containerColor = if (visible) colors.blueDeep else colors.surfaceSoft,
        contentColor = if (visible) {
            colors.onDark
        } else if (colors.isDark) {
            colors.blue
        } else {
            colors.blueDeep
        },
        touchTarget = touchTarget,
        iconSize = iconSize,
        onClick = onClick,
        modifier = modifier
            .testTag(ExamRuntimeUiTestTags.ArrowToggle)
            .semantics { stateDescription = state }
    )
}

@Composable
private fun RefreshAction(
    isRefreshing: Boolean,
    fullLabel: Boolean,
    touchTarget: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    val label = when {
        isRefreshing && fullLabel -> tr("Stop loading", "Hentikan loading")
        isRefreshing -> tr("Stop", "Stop")
        fullLabel -> tr("Reload page", "Muat ulang halaman")
        else -> tr("Reload", "Ulang")
    }
    val description = if (isRefreshing) {
        tr("Stop loading the exam page", "Hentikan pemuatan halaman ujian")
    } else {
        tr("Reload the exam page", "Muat ulang halaman ujian")
    }

    ExamRuntimeActionButton(
        label = label,
        icon = if (isRefreshing) Icons.Rounded.Close else Icons.Rounded.Refresh,
        contentDescriptionText = description,
        containerColor = if (isRefreshing) colors.gold else colors.blueDeep,
        contentColor = if (isRefreshing) colors.blueDeep else colors.onDark,
        touchTarget = touchTarget,
        iconSize = iconSize,
        onClick = onClick,
        modifier = modifier.testTag(ExamRuntimeUiTestTags.RefreshAction)
    )
}

@Composable
private fun ExitAction(
    fullLabel: Boolean,
    touchTarget: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    ExamRuntimeActionButton(
        label = if (fullLabel) tr("Exit exam", "Keluar ujian") else tr("Exit", "Keluar"),
        icon = Icons.AutoMirrored.Rounded.ExitToApp,
        contentDescriptionText = tr(
            "Open the exit exam confirmation",
            "Buka konfirmasi keluar dari ujian"
        ),
        containerColor = colors.surfaceSoft,
        contentColor = if (colors.isDark) colors.blue else colors.blueDeep,
        touchTarget = touchTarget,
        iconSize = iconSize,
        onClick = onClick,
        modifier = modifier.testTag(ExamRuntimeUiTestTags.ExitAction)
    )
}

@Composable
private fun ExamRuntimeActionButton(
    label: String,
    icon: ImageVector,
    contentDescriptionText: String,
    containerColor: Color,
    contentColor: Color,
    touchTarget: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(touchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = contentDescriptionText
            },
        shape = RoundedCornerShape(tokens.radiusSmall),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, AppColors.current.outline.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(
                tokens.spaceXSmall,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = label,
                color = contentColor,
                style = AppTextStyles.button,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
