package com.coblax.examlock.ui.preparation

import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkUnstableRuntimeStatus
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.ExternalDisplayInfo
import com.coblax.examlock.runtime.MultiWindowModeInfo
import com.coblax.examlock.runtime.displayFlagsLabel
import com.coblax.examlock.runtime.displayStateLabel

internal fun appendPreparationAuditDetail(
    actionDetail: String?,
    auditDetail: String?
): String? {
    return listOfNotNull(
        actionDetail?.trim()?.takeIf { it.isNotBlank() },
        auditDetail?.trim()?.takeIf { it.isNotBlank() }
    ).joinToString("\n\n").ifBlank { null }
}

internal fun buildPreparationNetworkAuditDetail(
    status: NetworkReadinessStatus,
    unstableStatus: NetworkUnstableRuntimeStatus,
    lastNetworkChangeAt: String?,
    lastNetworkChangeSource: String?,
    lastConnectedNetworkLabel: String?,
    bypassVpn: Boolean,
    vpnBypassState: VpnBypassState,
    isRefreshingNetwork: Boolean,
    uiLanguage: UiLanguage
): String {
    val diagnostics = status.diagnostics
    val probe = status.dnsProbeStatus
    val globalProbe = status.globalDnsProbeStatus
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Readiness verdict: ${status.verdict.name}",
            "User verdict: ${status.userFacingVerdict.name}",
            "Transport: ${status.transportLabel.ifBlank { "-" }}",
            "Refresh running: ${yesNo(isRefreshingNetwork)}",
            "Active network available: ${yesNo(diagnostics.activeNetworkAvailable)}",
            "Transports: ${preparationListSummary(diagnostics.transports)}",
            "Internet capability: ${yesNo(diagnostics.hasInternetCapability)}",
            "Validated: ${yesNo(diagnostics.isValidated)}",
            "Captive portal: ${yesNo(diagnostics.isCaptivePortal)}",
            "Metered: ${yesNo(diagnostics.isMetered)}",
            "VPN active: ${yesNo(diagnostics.isVpnActive)}",
            "VPN bypass active: ${yesNo(bypassVpn)}",
            "VPN bypass state: ${vpnBypassState.name}",
            "VPN bypass tampered: ${yesNo(vpnBypassState == VpnBypassState.Tampered)}",
            "Airplane mode: ${yesNo(diagnostics.isAirplaneModeEnabled)}",
            "Interface: ${diagnostics.interfaceName.ifBlank { "-" }}",
            "Global DNS probe: ${globalProbe.verdict.name}",
            "Global DNS host: ${globalProbe.host.ifBlank { "-" }}",
            "Global DNS latency: ${globalProbe.latencyMillis?.let { "$it ms" } ?: "-"}",
            "Global DNS latency bucket: ${globalProbe.latencyBucket.name}",
            "Global DNS error: ${globalProbe.error?.ifBlank { "-" } ?: "-"}",
            "Exam DNS probe: ${probe.verdict.name}",
            "Exam DNS host: ${probe.host.ifBlank { "-" }}",
            "Exam DNS latency: ${probe.latencyMillis?.let { "$it ms" } ?: "-"}",
            "Exam DNS latency bucket: ${probe.latencyBucket.name}",
            "Exam DNS error: ${probe.error?.ifBlank { "-" } ?: "-"}",
            "Unstable active: ${yesNo(unstableStatus.unstableActive)}",
            "Network changes/flaps: ${unstableStatus.flapCount}",
            "Last flap at: ${unstableStatus.lastFlapAt?.ifBlank { "-" } ?: "-"}",
            "Last network change at: ${lastNetworkChangeAt?.ifBlank { "-" } ?: "-"}",
            "Last network change source: ${lastNetworkChangeSource?.ifBlank { "-" } ?: "-"}",
            "Last connected network: ${lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-"}",
            "Quick fix reason: ${status.quickFixReason?.ifBlank { "-" } ?: "-"}",
            "User quick fix: ${status.userFacingQuickFixText?.ifBlank { "-" } ?: "-"}"
        )
    )
}

internal fun buildPreparationWebViewAuditDetail(
    status: WebViewCompatibilityStatus,
    webViewSessionResetInFlight: Boolean,
    webViewSessionResetError: String?,
    uiLanguage: UiLanguage
): String {
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Verdict: ${status.verdict.name}",
            "Severity: ${status.severity.name}",
            "Available: ${yesNo(status.available)}",
            "Provider: ${status.providerLabel}",
            "Package: ${status.packageName.ifBlank { "-" }}",
            "Version: ${status.versionLabel}",
            "Major version: ${status.majorVersion ?: "-"}",
            "Provider source: ${status.providerSource.ifBlank { "-" }}",
            "Outdated likely: ${yesNo(status.outdatedLikely)}",
            "Risk label: ${status.riskLabel}",
            "Quick fix: ${status.quickFix?.ifBlank { "-" } ?: "-"}",
            "Session reset running: ${yesNo(webViewSessionResetInFlight)}",
            "Session reset error: ${webViewSessionResetError?.ifBlank { "-" } ?: "-"}"
        )
    )
}

internal fun buildPreparationDeviceTimeAuditDetail(
    status: DeviceTimeSecurityStatus,
    uiLanguage: UiLanguage
): String {
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Final verdict: ${status.finalVerdict.name}",
            "Bypass state: ${status.bypassState.name}",
            "Bypass active: ${yesNo(status.bypassActive)}",
            "Automatic date/time: ${yesNo(status.autoTimeEnabled)}",
            "Automatic time zone: ${yesNo(status.autoTimeZoneEnabled)}",
            "Timezone: ${status.timezoneSummary.ifBlank { "-" }}",
            "Clock drift detected: ${yesNo(status.clockDriftDetected)}",
            "Clock drift: ${status.clockDriftMillis} ms",
            "Baseline wall clock: ${status.baselineWallClockMillis}",
            "Baseline elapsed realtime: ${status.baselineElapsedRealtimeMillis}",
            "Current wall clock: ${status.wallClockNowMillis}",
            "Current elapsed realtime: ${status.elapsedNowMillis}",
            "Blocking now: ${yesNo(status.blocking)}"
        )
    )
}

internal fun buildPreparationScreenRecorderAuditDetail(
    screenRecorderPackages: List<String>,
    bypassScreenRecorder: Boolean,
    uiLanguage: UiLanguage
): String {
    val packageLines = if (screenRecorderPackages.isEmpty()) {
        listOf("No visible recorder package detected: Yes")
    } else {
        screenRecorderPackages.mapIndexed { index, packageLabel ->
            "Detected package [$index]: ${packageLabel.ifBlank { "-" }}"
        }
    }
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Detection method: known package lookup + user package keyword scan",
            "Package visibility note: best-effort; Android package visibility can hide packages not visible to this app.",
            "Visible recorder package count: ${screenRecorderPackages.size}",
            "Bypass active: ${yesNo(bypassScreenRecorder)}"
        ) + packageLines
    )
}

internal fun buildPreparationDisplayMirrorAuditDetail(
    externalDisplayDetected: Boolean,
    externalDisplayCount: Int,
    externalDisplayInfoList: List<ExternalDisplayInfo>,
    bypassDisplayMirror: Boolean,
    uiLanguage: UiLanguage
): String {
    val displayLines = if (externalDisplayInfoList.isEmpty()) {
        listOf("External display list: -")
    } else {
        externalDisplayInfoList.mapIndexed { index, display ->
            "External display [$index]: id=${display.displayId} | " +
                "name=${display.name.ifBlank { "-" }} | " +
                "state=${displayStateLabel(display.state)} | " +
                "flagsRaw=${display.flags} | flags=${displayFlagsLabel(display.flags)}"
        }
    }
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Detection method: DisplayManager.getDisplays",
            "Blocking definition: external display count > 0",
            "External display detected: ${yesNo(externalDisplayDetected)}",
            "External display count: ${maxOf(externalDisplayCount, externalDisplayInfoList.size)}",
            "Display info list count: ${externalDisplayInfoList.size}",
            "Bypass active: ${yesNo(bypassDisplayMirror)}"
        ) + displayLines
    )
}

internal fun buildPreparationMultiWindowAuditDetail(
    modeInfo: MultiWindowModeInfo,
    runtimeDetected: Boolean,
    bypassMultiWindow: Boolean,
    uiLanguage: UiLanguage
): String {
    return buildAuditBlock(
        uiLanguage = uiLanguage,
        englishTitle = "Technical details",
        indonesianTitle = "Detail teknis",
        lines = listOf(
            "Detection method: Activity.isInMultiWindowMode + Activity.isInPictureInPictureMode",
            "Multi-window API >= 24 supported: ${yesNo(modeInfo.multiWindowApiSupported)}",
            "PiP API >= 26 supported: ${yesNo(modeInfo.pictureInPictureApiSupported)}",
            "isInMultiWindowMode: ${yesNo(modeInfo.inMultiWindowMode)}",
            "isInPictureInPictureMode: ${yesNo(modeInfo.inPictureInPictureMode)}",
            "isInAnySplitMode: ${yesNo(modeInfo.inAnySplitMode)}",
            "Runtime combined state: ${yesNo(runtimeDetected)}",
            "Bypass active: ${yesNo(bypassMultiWindow)}"
        )
    )
}

private fun buildAuditBlock(
    uiLanguage: UiLanguage,
    englishTitle: String,
    indonesianTitle: String,
    lines: List<String>
): String {
    return buildString {
        appendLine(localized(uiLanguage, englishTitle, indonesianTitle) + ":")
        lines.forEach { line ->
            appendLine("- $line")
        }
    }.trim()
}

private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
