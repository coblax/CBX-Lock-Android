package com.coblax.examlock.ui.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.StatusBanner
import com.coblax.examlock.ui.theme.UiStatusIcon
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.resolveUiStatusVisual

internal object ExamRuntimeUiTestTags {
    const val StatusStrip = "exam_runtime_status_strip"
    const val WarningBanner = "exam_runtime_warning_banner"
    const val KeyboardPanel = "exam_runtime_keyboard_panel"
    const val BottomActionRail = "exam_runtime_bottom_action_rail"
    const val ArrowToggle = "exam_runtime_arrow_toggle"
    const val RefreshAction = "exam_runtime_refresh_action"
    const val ExitAction = "exam_runtime_exit_action"
}

@Composable
internal fun ExamRuntimeStatusStrip(
    examDisplayName: String,
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val connectivity = resolveExamFooterConnectivityVisual(networkStatus, serverStatus)
    val connectionLabel = connectivityStateLabel(connectivity.state)
    val connectionTone = when (connectivity.severity) {
        ExamFooterConnectivitySeverity.Stable -> UiStatusTone.Success
        ExamFooterConnectivitySeverity.Info -> UiStatusTone.Info
        ExamFooterConnectivitySeverity.Warning -> UiStatusTone.Warning
    }
    val connectionIcon = when (connectivity.state) {
        ExamRuntimeConnectivityState.Online -> UiStatusIcon.Check
        ExamRuntimeConnectivityState.Checking -> UiStatusIcon.Loading
        ExamRuntimeConnectivityState.Limited,
        ExamRuntimeConnectivityState.Offline -> UiStatusIcon.Warning
    }
    val shieldLabel = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> tr("Protected", "Aman")
        ExamFooterShieldStatus.Warning -> tr("Check", "Periksa")
        ExamFooterShieldStatus.Danger -> tr("Blocked", "Diblokir")
    }
    val shieldTone = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> UiStatusTone.Success
        ExamFooterShieldStatus.Warning -> UiStatusTone.Warning
        ExamFooterShieldStatus.Danger -> UiStatusTone.Danger
    }
    val shieldDescription = when (shieldStatus) {
        ExamFooterShieldStatus.Safe ->
            tr("Exam security is protected", "Keamanan ujian terlindungi")
        ExamFooterShieldStatus.Warning ->
            tr("Exam security needs attention", "Keamanan ujian perlu diperiksa")
        ExamFooterShieldStatus.Danger ->
            tr("A blocking security issue was detected", "Masalah keamanan yang memblokir terdeteksi")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.statusStripHeight)
            .testTag(ExamRuntimeUiTestTags.StatusStrip)
            .background(AppColors.current.footerBg)
            .border(
                BorderStroke(1.dp, AppColors.current.outlineSubtle),
                RoundedCornerShape(0.dp)
            )
            .padding(horizontal = tokens.spaceSmall, vertical = tokens.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = examDisplayName.ifBlank { tr("Exam", "Ujian") },
            modifier = Modifier.weight(1f),
            color = AppColors.current.textPrimary,
            style = AppTextStyles.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(tokens.spaceXSmall))
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spaceXSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                label = connectionLabel,
                tone = connectionTone,
                icon = connectionIcon,
                semanticLabel = connectionSemanticLabel(networkStatus, serverStatus)
            )
            RuntimeBatteryBadge(batteryStatus = batteryStatus)
            StatusBadge(
                label = shieldLabel,
                tone = shieldTone,
                icon = when (shieldTone) {
                    UiStatusTone.Success -> UiStatusIcon.Check
                    UiStatusTone.Warning -> UiStatusIcon.Warning
                    UiStatusTone.Danger -> UiStatusIcon.Blocked
                    else -> UiStatusIcon.Info
                },
                semanticLabel = shieldDescription
            )
        }
    }
}

@Composable
internal fun RuntimeConnectionWarningBanner(
    noticeKind: ExamRuntimeConnectionNoticeKind,
    modifier: Modifier = Modifier
) {
    val message = when (noticeKind) {
        ExamRuntimeConnectionNoticeKind.Offline ->
            tr(
                "The connection is offline. The exam stays open while reconnection is attempted.",
                "Koneksi terputus. Ujian tetap terbuka saat koneksi dipulihkan."
            )
        ExamRuntimeConnectionNoticeKind.AirplaneMode ->
            tr(
                "Airplane mode is active. Turn it off to reconnect to the exam.",
                "Mode pesawat aktif. Nonaktifkan untuk menyambungkan kembali ujian."
            )
        ExamRuntimeConnectionNoticeKind.VpnActive ->
            tr(
                "A VPN connection was detected. Check the required exam network.",
                "Koneksi VPN terdeteksi. Periksa jaringan yang diwajibkan untuk ujian."
            )
        ExamRuntimeConnectionNoticeKind.CaptivePortal ->
            tr(
                "This network needs sign-in before it can reach the exam server.",
                "Jaringan ini memerlukan login sebelum dapat menjangkau server ujian."
            )
        ExamRuntimeConnectionNoticeKind.LimitedNetwork ->
            tr(
                "Internet access is limited. The app will keep checking the connection.",
                "Akses internet terbatas. Aplikasi akan terus memeriksa koneksi."
            )
        ExamRuntimeConnectionNoticeKind.UnstableNetwork ->
            tr(
                "The connection is unstable. Avoid leaving the exam while it recovers.",
                "Koneksi tidak stabil. Jangan keluar dari ujian selama koneksi dipulihkan."
            )
        ExamRuntimeConnectionNoticeKind.ServerOffline ->
            tr(
                "The exam server cannot be reached yet. The app will retry automatically.",
                "Server ujian belum dapat dijangkau. Aplikasi akan mencoba kembali otomatis."
            )
        ExamRuntimeConnectionNoticeKind.ServerUnstable ->
            tr(
                "The exam server response is unstable. The current page remains open.",
                "Respons server ujian tidak stabil. Halaman saat ini tetap terbuka."
            )
        ExamRuntimeConnectionNoticeKind.ServerWarning ->
            tr(
                "The exam server needs attention. Continue on the current page.",
                "Server ujian perlu diperiksa. Tetap lanjutkan pada halaman saat ini."
            )
    }

    StatusBanner(
        message = message,
        modifier = modifier.testTag(ExamRuntimeUiTestTags.WarningBanner),
        tone = UiStatusTone.Warning,
        title = tr("Connection warning", "Peringatan koneksi"),
        icon = UiStatusIcon.Warning
    )
}

@Composable
private fun RuntimeBatteryBadge(
    batteryStatus: ExamBatteryStatus,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val percent = batteryStatus.levelPercent.coerceIn(0, 100)
    val tone = when {
        batteryStatus.isCharging -> UiStatusTone.Success
        percent <= 40 -> UiStatusTone.Warning
        else -> UiStatusTone.Neutral
    }
    val visual = resolveUiStatusVisual(
        tone = tone,
        label = "$percent%",
        icon = if (percent <= 20) UiStatusIcon.Warning else UiStatusIcon.Info
    )
    val description = if (batteryStatus.isCharging) {
        tr("Battery $percent percent, charging", "Baterai $percent persen, sedang diisi")
    } else {
        tr("Battery $percent percent", "Baterai $percent persen")
    }
    val shape = RoundedCornerShape(tokens.radiusPill)

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                stateDescription = description
            }
            .clip(shape)
            .background(visual.containerColor)
            .border(1.dp, visual.borderColor.copy(alpha = 0.42f), shape)
            .heightIn(min = 32.dp)
            .padding(horizontal = tokens.spaceSmall, vertical = tokens.spaceXSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                batteryStatus.isCharging -> Icons.Rounded.BatteryChargingFull
                percent <= 20 -> Icons.Rounded.BatteryAlert
                else -> Icons.Rounded.BatteryFull
            },
            contentDescription = null,
            tint = visual.contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$percent%",
            color = visual.contentColor,
            style = AppTextStyles.label,
            maxLines = 1
        )
    }
}

@Composable
private fun connectivityStateLabel(state: ExamRuntimeConnectivityState): String =
    when (state) {
        ExamRuntimeConnectivityState.Online -> tr("Online", "Online")
        ExamRuntimeConnectivityState.Checking -> tr("Checking", "Memeriksa")
        ExamRuntimeConnectivityState.Limited -> tr("Limited", "Terbatas")
        ExamRuntimeConnectivityState.Offline -> tr("Offline", "Offline")
    }

@Composable
private fun connectionSemanticLabel(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus
): String {
    val networkDescription = when (networkStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable ->
            tr("Network connected", "Jaringan terhubung")
        NetworkReadinessVerdict.Offline ->
            tr("Network offline", "Jaringan offline")
        NetworkReadinessVerdict.AirplaneMode ->
            tr("Airplane mode active", "Mode pesawat aktif")
        NetworkReadinessVerdict.Unvalidated ->
            tr("Network is limited", "Jaringan terbatas")
        NetworkReadinessVerdict.CaptivePortal ->
            tr("Network sign-in required", "Login jaringan diperlukan")
        NetworkReadinessVerdict.VpnActive ->
            tr("VPN active", "VPN aktif")
        NetworkReadinessVerdict.Unstable ->
            tr("Network unstable", "Jaringan tidak stabil")
    }
    val serverDescription = when (serverStatus) {
        ExamServerFooterStatus.Checking -> tr("checking exam server", "memeriksa server ujian")
        ExamServerFooterStatus.Online -> tr("exam server online", "server ujian online")
        ExamServerFooterStatus.Warning -> tr("exam server warning", "peringatan server ujian")
        ExamServerFooterStatus.Offline -> tr("exam server offline", "server ujian offline")
        ExamServerFooterStatus.Unstable -> tr("exam server unstable", "server ujian tidak stabil")
    }
    return "$networkDescription, $serverDescription"
}
