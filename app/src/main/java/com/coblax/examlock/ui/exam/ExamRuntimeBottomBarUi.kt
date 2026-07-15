package com.coblax.examlock.ui.exam

import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockFooterBg
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineSubtle

@Composable
internal fun ExamWebViewBottomBar(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus,
    showArrowControls: Boolean,
    isRefreshing: Boolean,
    onToggleArrowControls: () -> Unit,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val batteryIndicatorColor = when {
        batteryStatus.isCharging -> Color(0xFF2E9E52)
        batteryStatus.levelPercent <= 20 -> Color(0xFFD93025)
        batteryStatus.levelPercent <= 40 -> LockGoldDark
        else -> LockBlue
    }
    val serverContentDescription = when (serverStatus) {
        ExamServerFooterStatus.Online -> tr("Exam server reachable", "Server ujian bisa diakses")
        ExamServerFooterStatus.Warning -> tr("Exam server warning", "Peringatan server ujian")
        ExamServerFooterStatus.Offline -> tr("Exam server unreachable", "Server ujian tidak bisa diakses")
        ExamServerFooterStatus.Checking -> tr("Checking exam server", "Mengecek server ujian")
        ExamServerFooterStatus.Unstable -> tr("Exam server unstable", "Server ujian tidak stabil")
    }
    val shieldIndicatorColor = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> Color(0xFF2E9E52)
        ExamFooterShieldStatus.Warning -> LockGoldDark
        ExamFooterShieldStatus.Danger -> Color(0xFFD93025)
    }
    val shieldContentDescription = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> tr("Security protected", "Keamanan terlindungi")
        ExamFooterShieldStatus.Warning -> tr("Security warning", "Peringatan keamanan")
        ExamFooterShieldStatus.Danger -> tr("Security issue detected", "Masalah keamanan terdeteksi")
    }
    val shieldLabel = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> "Aman"
        ExamFooterShieldStatus.Warning -> "Cek"
        ExamFooterShieldStatus.Danger -> "Blok"
    }
    val transportLabel = networkStatus.transportLabel
    val networkContentDescription = when (networkStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable ->
            tr(
                "Network connected: ${transportLabel.ifBlank { "unknown transport" }}",
                "Jaringan terhubung: ${transportLabel.ifBlank { "transport tidak diketahui" }}"
            )
        NetworkReadinessVerdict.Offline ->
            tr("Network offline", "Jaringan offline")
        NetworkReadinessVerdict.AirplaneMode ->
            tr("Airplane mode is active", "Mode pesawat aktif")
        NetworkReadinessVerdict.Unvalidated ->
            tr("Network is limited", "Jaringan terbatas")
        NetworkReadinessVerdict.CaptivePortal ->
            tr("Network requires captive portal login", "Jaringan membutuhkan login captive portal")
        NetworkReadinessVerdict.VpnActive ->
            tr("VPN is active", "VPN aktif")
        NetworkReadinessVerdict.Unstable ->
            tr("Network is unstable", "Jaringan tidak stabil")
    }
    val lowRamProfile = LocalLowRamProfile.current
    val connectivityIndicatorColor = when {
        networkStatus.verdict == NetworkReadinessVerdict.Offline ||
            networkStatus.verdict == NetworkReadinessVerdict.VpnActive ||
            networkStatus.verdict == NetworkReadinessVerdict.AirplaneMode -> Color(0xFFD93025)
        networkStatus.verdict == NetworkReadinessVerdict.Unvalidated ||
            networkStatus.verdict == NetworkReadinessVerdict.CaptivePortal ||
            networkStatus.verdict == NetworkReadinessVerdict.Unstable -> LockGoldDark
        else -> Color(0xFF2E9E52)
    }
    val connectivityVisual = resolveExamFooterConnectivityVisual(networkStatus, serverStatus)
    val connectivityDescription = "$networkContentDescription. $serverContentDescription"
    val refreshContainerColor = if (isRefreshing) LockGold else LockBlue

    // Refresh spin animation Ã¢â‚¬â€ continuous rotation while loading
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val refreshRotation by if (isRefreshing && !lowRamProfile.enabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "refresh_rotation"
        )
    } else {
        animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "refresh_rotation"
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val layoutSpec = calculateExamFooterLayoutSpec(
            maxWidthDp = maxWidth.value.toInt(),
            lowRamEnabled = lowRamProfile.enabled,
            lowRamSevere = lowRamProfile.severe
        )
        val footerHorizontalPadding = layoutSpec.horizontalPaddingDp.dp
        val footerVerticalPadding = layoutSpec.verticalPaddingDp.dp
        val itemSpacing = layoutSpec.itemSpacingDp.dp
        val actionSpacing = layoutSpec.actionSpacingDp.dp
        val actionButtonSize = layoutSpec.buttonSizeDp.dp
        val arrowPillWidth = layoutSpec.arrowPillWidthDp.dp
        val connectivityPillWidth = layoutSpec.connectivityPillWidthDp.dp
        val shieldPillWidth = layoutSpec.shieldPillWidthDp.dp
        val actionTouchTargetSize = layoutSpec.touchTargetDp.dp
        val iconSize = layoutSpec.iconSizeDp.dp
        val rowSpacing = layoutSpec.rowSpacingDp.dp
        val batteryPillWidth = when {
            !layoutSpec.showBatteryPercent -> actionButtonSize
            layoutSpec.compact -> 52.dp
            batteryStatus.levelPercent.coerceIn(0, 100) >= 100 -> 62.dp
            else -> 58.dp
        }
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

        val footerShape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = layoutSpec.cornerRadiusDp.dp,
                bottomEnd = layoutSpec.cornerRadiusDp.dp
            )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(footerShape)
                .background(LockFooterBg)
                .border(1.dp, LockOutlineSubtle, footerShape)
        ) {
            if (layoutSpec.layoutMode == ExamFooterLayoutMode.TwoRowCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = layoutSpec.minHeightDp.dp,
                            max = layoutSpec.maxHeightDp.dp
                        )
                        .padding(
                            horizontal = footerHorizontalPadding,
                            vertical = footerVerticalPadding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(rowSpacing, Alignment.CenterVertically)
                ) {
                    ArrowVisibilityTogglePill(
                        visible = showArrowControls,
                        buttonWidth = arrowPillWidth,
                        buttonSize = actionButtonSize,
                        touchTargetSize = actionTouchTargetSize,
                        iconSize = layoutSpec.arrowIconSizeDp.dp,
                        onClick = onToggleArrowControls
                    )
                    ExamFooterStatusCluster(
                        connectivityVisual = connectivityVisual,
                        connectivityIndicatorColor = connectivityIndicatorColor,
                        connectivityDescription = connectivityDescription,
                        connectivityPillWidth = connectivityPillWidth,
                        batteryStatus = batteryStatus,
                        batteryIndicatorColor = batteryIndicatorColor,
                        batteryPillWidth = batteryPillWidth,
                        shieldIndicatorColor = shieldIndicatorColor,
                        shieldLabel = shieldLabel,
                        shieldContentDescription = shieldContentDescription,
                        shieldPillWidth = shieldPillWidth,
                        shieldStatus = shieldStatus,
                        serverStatus = serverStatus,
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )
                    ExamFooterActionCluster(
                        isRefreshing = isRefreshing,
                        refreshContainerColor = refreshContainerColor,
                        refreshRotation = refreshRotation,
                        actionButtonSize = actionButtonSize,
                        actionTouchTargetSize = actionTouchTargetSize,
                        iconSize = iconSize,
                        actionSpacing = actionSpacing,
                        onRefresh = onRefresh,
                        onGoHome = onGoHome
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = layoutSpec.minHeightDp.dp,
                            max = layoutSpec.maxHeightDp.dp
                        )
                        .padding(horizontal = footerHorizontalPadding, vertical = footerVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArrowVisibilityTogglePill(
                        visible = showArrowControls,
                        buttonWidth = arrowPillWidth,
                        buttonSize = actionButtonSize,
                        touchTargetSize = actionTouchTargetSize,
                        iconSize = layoutSpec.arrowIconSizeDp.dp,
                        onClick = onToggleArrowControls
                    )
                    Spacer(modifier = Modifier.width(itemSpacing))
                    ExamFooterStatusCluster(
                        connectivityVisual = connectivityVisual,
                        connectivityIndicatorColor = connectivityIndicatorColor,
                        connectivityDescription = connectivityDescription,
                        connectivityPillWidth = connectivityPillWidth,
                        batteryStatus = batteryStatus,
                        batteryIndicatorColor = batteryIndicatorColor,
                        batteryPillWidth = batteryPillWidth,
                        shieldIndicatorColor = shieldIndicatorColor,
                        shieldLabel = shieldLabel,
                        shieldContentDescription = shieldContentDescription,
                        shieldPillWidth = shieldPillWidth,
                        shieldStatus = shieldStatus,
                        serverStatus = serverStatus,
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Visual separator between status cluster and action cluster
                    VerticalDivider(
                        modifier = Modifier
                            .height(actionButtonSize * 0.7f)
                            .padding(end = actionSpacing),
                        thickness = 0.5.dp,
                        color = LockOutlineSubtle
                    )

                    ExamFooterActionCluster(
                        isRefreshing = isRefreshing,
                        refreshContainerColor = refreshContainerColor,
                        refreshRotation = refreshRotation,
                        actionButtonSize = actionButtonSize,
                        actionTouchTargetSize = actionTouchTargetSize,
                        iconSize = iconSize,
                        actionSpacing = actionSpacing,
                        onRefresh = onRefresh,
                        onGoHome = onGoHome
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamFooterActionCluster(
    isRefreshing: Boolean,
    refreshContainerColor: Color,
    refreshRotation: Float,
    actionButtonSize: Dp,
    actionTouchTargetSize: Dp,
    iconSize: Dp,
    actionSpacing: Dp,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val refreshIcon = if (isRefreshing) Icons.Rounded.Close else Icons.Rounded.Refresh
    val refreshContentDescription = if (isRefreshing) {
        tr("Stop loading exam page", "Batalkan loading halaman ujian")
    } else {
        tr("Refresh exam page", "Refresh halaman ujian")
    }
    val refreshIconRotation = if (isRefreshing) 0f else refreshRotation

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(actionSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExamFooterIconButton(
            onClick = onRefresh,
            icon = refreshIcon,
            contentDescription = refreshContentDescription,
            containerColor = refreshContainerColor,
            contentColor = LockOnDark,
            size = actionButtonSize,
            touchTargetSize = actionTouchTargetSize,
            iconSize = iconSize,
            iconRotation = refreshIconRotation
        )

        ExamFooterIconButton(
            onClick = onGoHome,
            icon = Icons.Rounded.Home,
            contentDescription = tr("Back to the main menu", "Kembali ke menu utama"),
            containerColor = LockSurfaceSoft,
            contentColor = LockBlueDeep,
            size = actionButtonSize,
            touchTargetSize = actionTouchTargetSize,
            iconSize = iconSize
        )
    }
}

@Composable
private fun ArrowVisibilityTogglePill(
    visible: Boolean,
    buttonWidth: Dp,
    buttonSize: Dp,
    touchTargetSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val containerColor = if (visible) LockBlue else LockSurfaceSoft
    val contentColor = if (visible) LockOnDark else LockBlueDeep
    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(touchTargetSize)
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonSize)
                .clip(RoundedCornerShape(UiTokens.RadiusSm))
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (visible) LockBlue.copy(alpha = 0.50f) else LockOutlineSubtle,
                    shape = RoundedCornerShape(UiTokens.RadiusSm)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = tr("Arrow controls", "Kontrol panah"),
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (visible) {
                        tr("Hide side arrows", "Sembunyikan tombol panah")
                    } else {
                        tr("Show side arrows", "Tampilkan tombol panah")
                    },
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun ExamFooterIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    size: Dp,
    touchTargetSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    Box(
        modifier = Modifier
            .size(touchTargetSize)
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(UiTokens.RadiusSm))
                .background(containerColor)
                .border(1.dp, LockOutline.copy(alpha = 0.28f), RoundedCornerShape(UiTokens.RadiusSm)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer { rotationZ = iconRotation }
            )
        }
    }
}
