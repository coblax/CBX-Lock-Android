package com.example.coblaxexamlock.ui.exam

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockStatusDanger
import com.example.coblaxexamlock.ui.theme.LockStatusDangerFill
import com.example.coblaxexamlock.ui.theme.LockStatusSafe
import com.example.coblaxexamlock.ui.theme.LockStatusSafeFill
import com.example.coblaxexamlock.ui.theme.LockStatusWarnFill
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextPrimary

@Composable
internal fun ExamFooterStatusCluster(
    connectivityVisual: ExamFooterConnectivityVisual,
    connectivityIndicatorColor: Color,
    connectivityDescription: String,
    connectivityPillWidth: Dp,
    batteryStatus: ExamBatteryStatus,
    batteryIndicatorColor: Color,
    batteryPillWidth: Dp,
    shieldIndicatorColor: Color,
    shieldLabel: String,
    shieldContentDescription: String,
    shieldPillWidth: Dp,
    shieldStatus: ExamFooterShieldStatus,
    serverStatus: ExamServerFooterStatus,
    actionButtonSize: Dp,
    iconSize: Dp,
    itemSpacing: Dp,
    showBatteryPercent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectivityInfoPill(
            visual = connectivityVisual,
            statusColor = connectivityIndicatorColor,
            serverStatus = serverStatus,
            contentDescription = connectivityDescription,
            width = connectivityPillWidth,
            height = actionButtonSize
        )
        BatteryInfoPill(
            batteryStatus = batteryStatus,
            statusColor = batteryIndicatorColor,
            height = actionButtonSize,
            width = batteryPillWidth,
            iconSize = iconSize,
            showPercent = showBatteryPercent
        )
        SecurityShieldPill(
            shieldStatus = shieldStatus,
            statusColor = shieldIndicatorColor,
            label = shieldLabel,
            contentDescription = shieldContentDescription,
            width = shieldPillWidth,
            height = actionButtonSize,
            iconSize = iconSize
        )
    }
}

// ---------------------------------------------------------------------------
// Connectivity pill
// ---------------------------------------------------------------------------

@Composable
private fun ConnectivityInfoPill(
    visual: ExamFooterConnectivityVisual,
    statusColor: Color,
    serverStatus: ExamServerFooterStatus,
    contentDescription: String,
    width: Dp,
    height: Dp
) {
    val pillFill = when (visual.severity) {
        ExamFooterConnectivitySeverity.Stable  -> LockStatusSafeFill
        ExamFooterConnectivitySeverity.Warning -> LockStatusWarnFill
        ExamFooterConnectivitySeverity.Danger  -> LockStatusDangerFill
    }
    val pillBorder = statusColor.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(pillFill)
                .border(1.dp, pillBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
            ) {
                SignalBars(
                    level = visual.signalLevel,
                    statusColor = statusColor,
                    danger = visual.severity == ExamFooterConnectivitySeverity.Danger
                )
                visual.cellularLabel?.let { label ->
                    Text(
                        text = label,
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
        }

        // Server-status badge dot (replaces the old "!" badge text)
        val serverDotColor = when (serverStatus) {
            ExamServerFooterStatus.Online   -> LockStatusSafe
            ExamServerFooterStatus.Checking -> LockGoldDark
            ExamServerFooterStatus.Warning  -> LockGoldDark
            ExamServerFooterStatus.Offline  -> LockStatusDanger
        }
        // Hide dot when everything is stable & server is online to reduce clutter
        val showServerDot = serverStatus != ExamServerFooterStatus.Online ||
            visual.severity != ExamFooterConnectivitySeverity.Stable
        if (showServerDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(serverDotColor)
                    .border(0.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            )
        }
    }
}

@Composable
private fun SignalBars(
    level: Int,
    statusColor: Color,
    danger: Boolean
) {
    val activeBars = if (danger) 1 else level.coerceIn(0, 4)
    val inactiveColor = LockOutline.copy(alpha = 0.42f)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(7.dp, 10.dp, 13.dp, 16.dp).forEachIndexed { index, barHeight ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index < activeBars) statusColor else inactiveColor)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Security shield pill
// ---------------------------------------------------------------------------

@Composable
private fun SecurityShieldPill(
    shieldStatus: ExamFooterShieldStatus,
    statusColor: Color,
    label: String,
    contentDescription: String,
    width: Dp,
    height: Dp,
    iconSize: Dp
) {
    val lowRam = LocalLowRamProfile.current
    val pillFill = when (shieldStatus) {
        ExamFooterShieldStatus.Safe    -> LockStatusSafeFill
        ExamFooterShieldStatus.Warning -> LockStatusWarnFill
        ExamFooterShieldStatus.Danger  -> LockStatusDangerFill
    }
    val pillBorder = statusColor.copy(alpha = 0.45f)

    // Pulse animation for Danger state (disabled on low-RAM)
    val animatePulse = shieldStatus == ExamFooterShieldStatus.Danger && !lowRam.enabled
    val pulseScale by if (animatePulse) {
        rememberInfiniteTransition(label = "shield_pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shield_pulse_scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Row(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(pulseScale)
            .clip(RoundedCornerShape(12.dp))
            .background(pillFill)
            .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = contentDescription,
            tint = statusColor,
            modifier = Modifier.size((iconSize.value - 3).coerceAtLeast(12f).dp)
        )
        Text(
            text = label,
            color = statusColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

// ---------------------------------------------------------------------------
// Battery pill
// ---------------------------------------------------------------------------

@Composable
private fun BatteryInfoPill(
    batteryStatus: ExamBatteryStatus,
    statusColor: Color,
    height: Dp,
    width: Dp,
    iconSize: Dp,
    showPercent: Boolean
) {
    val lowRam = LocalLowRamProfile.current
    val percent = batteryStatus.levelPercent.coerceIn(0, 100)

    val pillFill = when {
        batteryStatus.isCharging    -> LockStatusSafeFill
        percent <= 20               -> LockStatusDangerFill
        percent <= 40               -> LockStatusWarnFill
        else                        -> LockSurfaceSoft
    }
    val pillBorder = statusColor.copy(alpha = 0.40f)

    val contentDescription = if (batteryStatus.isCharging) {
        tr("Battery $percent percent, charging", "Baterai $percent persen, sedang diisi")
    } else {
        tr("Battery $percent percent", "Baterai $percent persen")
    }

    // Charging blink animation (disabled on low-RAM)
    val animateCharging = batteryStatus.isCharging && !lowRam.enabled
    val chargingAlpha by if (animateCharging) {
        rememberInfiniteTransition(label = "battery_charge").animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "battery_blink"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Row(
        modifier = Modifier
            .height(height)
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .background(pillFill)
            .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = batteryStatusIcon(batteryStatus),
            contentDescription = contentDescription,
            tint = statusColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { alpha = chargingAlpha }
        )
        if (showPercent) {
            Text(
                text = "$percent%",
                color = LockTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private fun batteryStatusIcon(status: ExamBatteryStatus): ImageVector {
    return when {
        status.isCharging          -> Icons.Rounded.BatteryChargingFull
        status.levelPercent <= 20  -> Icons.Rounded.BatteryAlert
        else                       -> Icons.Rounded.BatteryFull
    }
}
