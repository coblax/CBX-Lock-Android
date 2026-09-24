package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict

internal object ExamRuntimeUiTestTags {
    const val Header = "exam_runtime_header"
    const val Footer = "exam_runtime_footer"
    const val WarningBanner = "exam_runtime_warning_banner"
    const val KeyboardPanel = "exam_runtime_keyboard_panel"
    const val ArrowToggle = "exam_runtime_arrow_toggle"
    const val RefreshAction = "exam_runtime_refresh_action"
    const val ExitAction = "exam_runtime_exit_action"
}

@Composable
internal fun connectionSemanticLabel(
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
