package com.example.coblaxexamlock.ui.exam

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft

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
        batteryStatus.isCharging -> Color(0xFF56C271)
        batteryStatus.levelPercent <= 20 -> Color(0xFFF26A6A)
        batteryStatus.levelPercent <= 40 -> LockGoldDark
        else -> LockBlue
    }
    val serverContentDescription = when (serverStatus) {
        ExamServerFooterStatus.Online -> tr("Exam server reachable", "Server ujian bisa diakses")
        ExamServerFooterStatus.Warning -> tr("Exam server warning", "Peringatan server ujian")
        ExamServerFooterStatus.Offline -> tr("Exam server unreachable", "Server ujian tidak bisa diakses")
        ExamServerFooterStatus.Checking -> tr("Checking exam server", "Mengecek server ujian")
    }
    val shieldIndicatorColor = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> Color(0xFF56C271)
        ExamFooterShieldStatus.Warning -> LockGoldDark
        ExamFooterShieldStatus.Danger -> Color(0xFFF26A6A)
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
        NetworkReadinessVerdict.Unstable ->
            tr("Network is unstable", "Jaringan tidak stabil")
    }
    val lowRamProfile = LocalLowRamProfile.current
    val connectivityIndicatorColor = when {
        networkStatus.verdict == NetworkReadinessVerdict.Offline ||
            networkStatus.verdict == NetworkReadinessVerdict.AirplaneMode -> Color(0xFFF26A6A)
        networkStatus.verdict == NetworkReadinessVerdict.Unvalidated ||
            networkStatus.verdict == NetworkReadinessVerdict.CaptivePortal ||
            networkStatus.verdict == NetworkReadinessVerdict.Unstable -> LockGoldDark
        else -> Color(0xFF56C271)
    }
    val connectivityVisual = resolveExamFooterConnectivityVisual(networkStatus, serverStatus)
    val connectivityDescription = "$networkContentDescription. $serverContentDescription"
    val refreshContainerColor = if (isRefreshing) LockGold else LockBlue
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(layoutSpec.cornerRadiusDp.dp),
            tonalElevation = layoutSpec.tonalElevationDp.dp,
            shadowElevation = layoutSpec.shadowElevationDp.dp,
            border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.80f))
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
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )
                    ExamFooterActionCluster(
                        refreshContainerColor = refreshContainerColor,
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
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    ExamFooterActionCluster(
                        refreshContainerColor = refreshContainerColor,
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
    refreshContainerColor: Color,
    actionButtonSize: Dp,
    actionTouchTargetSize: Dp,
    iconSize: Dp,
    actionSpacing: Dp,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(actionSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExamFooterIconButton(
            onClick = onRefresh,
            icon = Icons.Rounded.Refresh,
            contentDescription = tr("Refresh exam page", "Refresh halaman ujian"),
            containerColor = refreshContainerColor,
            contentColor = LockOnDark,
            size = actionButtonSize,
            touchTargetSize = actionTouchTargetSize,
            iconSize = iconSize
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonSize)
                .border(
                    width = 1.dp,
                    color = if (visible) LockBlueDeep.copy(alpha = 0.35f) else LockOutline.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            contentColor = contentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = if (visible) {
                        tr("Hide side arrows", "Sembunyikan tombol panah")
                    } else {
                        tr("Show side arrows", "Tampilkan tombol panah")
                    },
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                Text(
                    text = "Panah",
                    color = contentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
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
    size: androidx.compose.ui.unit.Dp,
    touchTargetSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(touchTargetSize)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.32f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
