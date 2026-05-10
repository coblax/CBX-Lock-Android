package com.example.coblaxexamlock.ui.exam

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.ui.theme.LockOutline
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
        ConnectivitySignalPill(
            visual = connectivityVisual,
            statusColor = connectivityIndicatorColor,
            contentDescription = connectivityDescription,
            width = connectivityPillWidth,
            height = actionButtonSize
        )
        BatteryStatusIconPill(
            batteryStatus = batteryStatus,
            statusColor = batteryIndicatorColor,
            height = actionButtonSize,
            width = batteryPillWidth,
            iconSize = iconSize,
            showPercent = showBatteryPercent
        )
        SecurityShieldStatusIconPill(
            statusColor = shieldIndicatorColor,
            label = shieldLabel,
            contentDescription = shieldContentDescription,
            width = shieldPillWidth,
            height = actionButtonSize,
            iconSize = iconSize
        )
    }
}

@Composable
private fun ConnectivitySignalPill(
    visual: ExamFooterConnectivityVisual,
    statusColor: Color,
    contentDescription: String,
    width: Dp,
    height: Dp
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
            visual.badgeText?.let { badge ->
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                    maxLines = 1
                )
            }
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
        listOf(7.dp, 10.dp, 13.dp, 16.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index < activeBars) statusColor else inactiveColor)
            )
        }
    }
}

@Composable
private fun SecurityShieldStatusIconPill(
    statusColor: Color,
    label: String,
    contentDescription: String,
    width: Dp,
    height: Dp,
    iconSize: Dp
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
}

@Composable
private fun BatteryStatusIconPill(
    batteryStatus: ExamBatteryStatus,
    statusColor: Color,
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    showPercent: Boolean
) {
    val percent = batteryStatus.levelPercent.coerceIn(0, 100)
    val contentDescription = if (batteryStatus.isCharging) {
        tr("Battery $percent percent, charging", "Baterai $percent persen, sedang diisi")
    } else {
        tr("Battery $percent percent", "Baterai $percent persen")
    }

    Surface(
        modifier = Modifier
            .height(height)
            .width(width),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = batteryStatusIcon(batteryStatus),
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size(iconSize)
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
}

private fun batteryStatusIcon(status: ExamBatteryStatus): ImageVector {
    return when {
        status.isCharging -> Icons.Rounded.BatteryChargingFull
        status.levelPercent <= 20 -> Icons.Rounded.BatteryAlert
        else -> Icons.Rounded.BatteryFull
    }
}
