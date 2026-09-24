package com.coblax.examlock.ui.exam

import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessVerdict

internal enum class ExamFooterLayoutMode {
    Compact,
    Regular,
    TabletWide
}

internal data class ExamChromeLayoutSpec(
    val layoutMode: ExamFooterLayoutMode,
    val showExamName: Boolean,
    val showStatusLabels: Boolean,
    val showActionLabels: Boolean,
    val stackActionLabels: Boolean,
    val headerHeightDp: Int,
    val footerHeightDp: Int,
    val touchTargetDp: Int
)

internal enum class ExamFooterConnectivityTransport {
    Wifi,
    Cellular,
    Unknown
}

/**
 * Connectivity is intentionally never rendered as Danger. A disconnected or
 * unstable transport is disruptive but does not, by itself, prove a blocking
 * security violation. Blocking states are represented by the security shield
 * and the existing runtime dialogs.
 */
internal enum class ExamFooterConnectivitySeverity {
    Stable,
    Info,
    Warning
}

internal enum class ExamRuntimeConnectivityState {
    Online,
    Checking,
    Limited,
    Offline
}

internal data class ExamFooterConnectivityVisual(
    val transport: ExamFooterConnectivityTransport,
    val severity: ExamFooterConnectivitySeverity,
    val state: ExamRuntimeConnectivityState,
    val signalLevel: Int,
    val cellularLabel: String?
)

internal enum class ExamRuntimeConnectionNoticeKind {
    Offline,
    AirplaneMode,
    VpnActive,
    CaptivePortal,
    LimitedNetwork,
    UnstableNetwork,
    ServerOffline,
    ServerUnstable,
    ServerWarning
}

/**
 * Sizing for the exam header (information only) and footer (actions). Widths are
 * divided by the (capped) font scale so an enlarged system font drops words before it squeezes
 * anything: status words go first, action labels move under their icons (and only
 * vanish on extreme settings), and the exam name goes last. Icons, battery digits, and
 * 48dp touch targets always stay.
 */
/** Text in the two bars follows the system font up to this scale; beyond it only the page grows. */
internal const val ExamChromeMaxFontScale = 1.3f

internal fun calculateExamChromeLayoutSpec(
    maxWidthDp: Int,
    fontScale: Float,
    lowRamSevere: Boolean
): ExamChromeLayoutSpec {
    val effectiveWidth = maxWidthDp / fontScale.coerceIn(1f, ExamChromeMaxFontScale)
    val layoutMode = when {
        maxWidthDp >= 600 && !lowRamSevere -> ExamFooterLayoutMode.TabletWide
        maxWidthDp < 360 || lowRamSevere -> ExamFooterLayoutMode.Compact
        else -> ExamFooterLayoutMode.Regular
    }
    return ExamChromeLayoutSpec(
        layoutMode = layoutMode,
        showExamName = effectiveWidth >= 240,
        showStatusLabels = effectiveWidth >= 340,
        showActionLabels = effectiveWidth >= 170,
        stackActionLabels = effectiveWidth < 290,
        headerHeightDp = 32,
        footerHeightDp = 48,
        touchTargetDp = 48
    )
}

internal fun resolveExamFooterConnectivityVisual(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus
): ExamFooterConnectivityVisual {
    val transportSummary =
        (networkStatus.diagnostics.transports + networkStatus.transportLabel)
            .joinToString(" ")
            .lowercase()
    val transport = when {
        networkStatus.diagnostics.wifi != null || transportSummary.contains("wifi") ->
            ExamFooterConnectivityTransport.Wifi
        networkStatus.diagnostics.cellular != null ||
            transportSummary.contains("cellular") ||
            transportSummary.contains("mobile") ->
            ExamFooterConnectivityTransport.Cellular
        else -> ExamFooterConnectivityTransport.Unknown
    }

    val state = when (networkStatus.verdict) {
        NetworkReadinessVerdict.Offline,
        NetworkReadinessVerdict.AirplaneMode -> ExamRuntimeConnectivityState.Offline
        NetworkReadinessVerdict.Unvalidated,
        NetworkReadinessVerdict.CaptivePortal,
        NetworkReadinessVerdict.VpnActive,
        NetworkReadinessVerdict.Unstable -> ExamRuntimeConnectivityState.Limited
        NetworkReadinessVerdict.ConnectedStable -> when (serverStatus) {
            ExamServerFooterStatus.Online -> ExamRuntimeConnectivityState.Online
            ExamServerFooterStatus.Checking -> ExamRuntimeConnectivityState.Checking
            ExamServerFooterStatus.Warning,
            ExamServerFooterStatus.Offline,
            ExamServerFooterStatus.Unstable -> ExamRuntimeConnectivityState.Limited
        }
    }
    val severity = when (state) {
        ExamRuntimeConnectivityState.Online -> ExamFooterConnectivitySeverity.Stable
        ExamRuntimeConnectivityState.Checking -> ExamFooterConnectivitySeverity.Info
        ExamRuntimeConnectivityState.Limited,
        ExamRuntimeConnectivityState.Offline -> ExamFooterConnectivitySeverity.Warning
    }
    val rawSignalLevel = when (transport) {
        ExamFooterConnectivityTransport.Wifi -> networkStatus.diagnostics.wifi?.signalLevel
        ExamFooterConnectivityTransport.Cellular -> networkStatus.diagnostics.cellular?.signalLevel
        ExamFooterConnectivityTransport.Unknown -> null
    }
    val signalLevel = when (state) {
        ExamRuntimeConnectivityState.Offline -> 0
        ExamRuntimeConnectivityState.Online ->
            rawSignalLevel?.coerceIn(0, 4) ?: 3
        ExamRuntimeConnectivityState.Checking,
        ExamRuntimeConnectivityState.Limited ->
            rawSignalLevel?.coerceIn(0, 4) ?: 2
    }
    val cellularLabel = networkStatus.diagnostics.cellular
        ?.networkType
        ?.uppercase()
        ?.takeIf { transport == ExamFooterConnectivityTransport.Cellular }
        ?.let { type ->
            when {
                "5G" in type || "NR" in type -> "5G"
                "4G" in type || "LTE" in type -> "4G"
                "3G" in type || "UMTS" in type || "HSPA" in type -> "3G"
                else -> null
            }
        }

    return ExamFooterConnectivityVisual(
        transport = transport,
        severity = severity,
        state = state,
        signalLevel = signalLevel,
        cellularLabel = cellularLabel
    )
}

internal fun resolveExamRuntimeConnectionNotice(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus
): ExamRuntimeConnectionNoticeKind? {
    return when (networkStatus.verdict) {
        NetworkReadinessVerdict.Offline -> ExamRuntimeConnectionNoticeKind.Offline
        NetworkReadinessVerdict.AirplaneMode -> ExamRuntimeConnectionNoticeKind.AirplaneMode
        NetworkReadinessVerdict.VpnActive -> ExamRuntimeConnectionNoticeKind.VpnActive
        NetworkReadinessVerdict.CaptivePortal -> ExamRuntimeConnectionNoticeKind.CaptivePortal
        NetworkReadinessVerdict.Unvalidated -> ExamRuntimeConnectionNoticeKind.LimitedNetwork
        NetworkReadinessVerdict.Unstable -> ExamRuntimeConnectionNoticeKind.UnstableNetwork
        NetworkReadinessVerdict.ConnectedStable -> when (serverStatus) {
            ExamServerFooterStatus.Offline -> ExamRuntimeConnectionNoticeKind.ServerOffline
            ExamServerFooterStatus.Unstable -> ExamRuntimeConnectionNoticeKind.ServerUnstable
            ExamServerFooterStatus.Warning -> ExamRuntimeConnectionNoticeKind.ServerWarning
            ExamServerFooterStatus.Checking,
            ExamServerFooterStatus.Online -> null
        }
    }
}
