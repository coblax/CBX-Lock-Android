package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.DeviceCompatibilityFamily
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.WebViewHealthSeverity
import com.example.coblaxexamlock.WebViewHealthVerdict
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict

internal enum class PreExamHealthCategory {
    ScreenPinning,
    FloatingAppOverlay,
    Network,
    WebView,
    Location,
    DeviceTime,
    BatteryPower
}

internal enum class PreExamHealthVerdict {
    Stable,
    Warning,
    Blocking
}

internal data class PreExamHealthItem(
    val category: PreExamHealthCategory,
    val verdict: PreExamHealthVerdict,
    val title: String,
    val detail: String,
    val quickFix: String? = null
)

internal data class PreExamHealthSnapshot(
    val compatibilityFamily: DeviceCompatibilityFamily,
    val compatibilityLabel: String,
    val generatedAtElapsedMs: Long,
    val items: List<PreExamHealthItem>
) {
    val blockingCount: Int
        get() = items.count { it.verdict == PreExamHealthVerdict.Blocking }

    val warningCount: Int
        get() = items.count { it.verdict == PreExamHealthVerdict.Warning }

    val stableCount: Int
        get() = items.count { it.verdict == PreExamHealthVerdict.Stable }

    fun diagnosticSummary(): String {
        return "family=${compatibilityFamily.name}" +
            " | blocking=$blockingCount" +
            " | warning=$warningCount" +
            " | stable=$stableCount" +
            " | items=" + items.joinToString(",") { item ->
                "${item.category.name}:${item.verdict.name}"
            }
    }
}

internal data class PreExamHealthCheckInput(
    val compatibilityProfile: DeviceCompatibilityProfile,
    val screenPinningAvailable: Boolean,
    val screenPinningActive: Boolean,
    val screenPinningBypassed: Boolean,
    val accessibilityGuardAvailable: Boolean,
    val accessibilityGuardEnabled: Boolean,
    val overlayRiskResult: OverlayRiskResult,
    val overlayBypassed: Boolean,
    val networkReadinessStatus: NetworkReadinessStatus,
    val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    val webViewRecoveryState: String,
    val webViewSessionResetInFlight: Boolean,
    val webViewSessionResetError: String?,
    val geofenceRuntimeStatus: GeofenceRuntimeStatus,
    val geofenceBypassed: Boolean,
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    val fakeLocationBypassed: Boolean,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    val deviceTimeBypassed: Boolean,
    val batteryStatus: ExamBatteryStatus,
    val generatedAtElapsedMs: Long = 0L
)

internal fun buildPreExamHealthSnapshot(
    input: PreExamHealthCheckInput
): PreExamHealthSnapshot {
    return PreExamHealthSnapshot(
        compatibilityFamily = input.compatibilityProfile.family,
        compatibilityLabel = input.compatibilityProfile.vendorDisplayName,
        generatedAtElapsedMs = input.generatedAtElapsedMs,
        items = listOf(
            buildScreenPinningHealthItem(input),
            buildOverlayHealthItem(input),
            buildNetworkHealthItem(input.networkReadinessStatus),
            buildWebViewHealthItem(input),
            buildLocationHealthItem(input),
            buildDeviceTimeHealthItem(input),
            buildBatteryPowerHealthItem(input)
        )
    )
}

internal fun preExamHealthStartBlocker(snapshot: PreExamHealthSnapshot): PreExamHealthItem? {
    return snapshot.items.firstOrNull { it.verdict == PreExamHealthVerdict.Blocking }
}

private fun buildScreenPinningHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    return when {
        input.screenPinningBypassed -> PreExamHealthItem(
            category = PreExamHealthCategory.ScreenPinning,
            verdict = PreExamHealthVerdict.Warning,
            title = "Screen Pinning",
            detail = "Screen Pinning bypass is active.",
            quickFix = "Use bypass only for approved troubleshooting."
        )
        input.screenPinningActive -> PreExamHealthItem(
            category = PreExamHealthCategory.ScreenPinning,
            verdict = PreExamHealthVerdict.Stable,
            title = "Screen Pinning",
            detail = "Already active. CBX will skip a repeated Android pinning request."
        )
        !input.screenPinningAvailable &&
            input.accessibilityGuardAvailable &&
            input.accessibilityGuardEnabled -> PreExamHealthItem(
            category = PreExamHealthCategory.ScreenPinning,
            verdict = PreExamHealthVerdict.Warning,
            title = "Screen Pinning",
            detail = "Screen Pinning is unavailable, but CBX Accessibility Exam Guard fallback is active.",
            quickFix = "Keep the accessibility guard enabled during the exam."
        )
        !input.screenPinningAvailable -> PreExamHealthItem(
            category = PreExamHealthCategory.ScreenPinning,
            verdict = PreExamHealthVerdict.Blocking,
            title = "Screen Pinning",
            detail = "Screen Pinning is not available on this device.",
            quickFix = "Enable Screen Pinning/Lock to app or use an approved fallback device."
        )
        else -> PreExamHealthItem(
            category = PreExamHealthCategory.ScreenPinning,
            verdict = PreExamHealthVerdict.Warning,
            title = "Screen Pinning",
            detail = "Ready. Android will ask for pinning only once when Start Exam Mode is pressed.",
            quickFix = "Confirm the Android pinning dialog, then stay in CBX Exam Lock."
        )
    }
}

private fun buildOverlayHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    val compatibilityDetail = if (input.compatibilityProfile.allowPartialObscuredWebViewTouch) {
        " Partial obscured touches on this legacy device are warning-only; fully obscured touches still block."
    } else {
        ""
    }
    return when {
        input.overlayBypassed || input.overlayRiskResult.bypassed -> PreExamHealthItem(
            category = PreExamHealthCategory.FloatingAppOverlay,
            verdict = PreExamHealthVerdict.Warning,
            title = "Floating App / Overlay",
            detail = "Overlay bypass is active.$compatibilityDetail",
            quickFix = "Close floating apps and overlays before the exam."
        )
        input.overlayRiskResult.confirmedInteractionDetected -> PreExamHealthItem(
            category = PreExamHealthCategory.FloatingAppOverlay,
            verdict = PreExamHealthVerdict.Blocking,
            title = "Floating App / Overlay",
            detail = "Confirmed overlay interaction was detected.$compatibilityDetail",
            quickFix = "Close chat heads, sidebars, screen filters, and apps that appear on top."
        )
        input.overlayRiskResult.heuristicRisk ||
            input.overlayRiskResult.riskyAccessibilityPackages.isNotEmpty() -> PreExamHealthItem(
            category = PreExamHealthCategory.FloatingAppOverlay,
            verdict = PreExamHealthVerdict.Warning,
            title = "Floating App / Overlay",
            detail = "A possible floating-app risk is present.$compatibilityDetail",
            quickFix = "Review accessibility and overlay permissions before starting."
        )
        else -> PreExamHealthItem(
            category = PreExamHealthCategory.FloatingAppOverlay,
            verdict = PreExamHealthVerdict.Stable,
            title = "Floating App / Overlay",
            detail = "No overlay risk is currently detected.$compatibilityDetail"
        )
    }
}

private fun buildNetworkHealthItem(status: NetworkReadinessStatus): PreExamHealthItem {
    val verdict = when (status.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> PreExamHealthVerdict.Stable
        NetworkReadinessUserVerdict.Offline,
        NetworkReadinessUserVerdict.CaptivePortal,
        NetworkReadinessUserVerdict.Unvalidated,
        NetworkReadinessUserVerdict.DnsFailed,
        NetworkReadinessUserVerdict.Slow,
        NetworkReadinessUserVerdict.AirplaneMode,
        NetworkReadinessUserVerdict.Unstable -> PreExamHealthVerdict.Warning
    }
    return PreExamHealthItem(
        category = PreExamHealthCategory.Network,
        verdict = verdict,
        title = "Network",
        detail = "Network status is ${status.userFacingVerdict.name} on ${status.transportLabel.ifBlank { "-" }}.",
        quickFix = status.userFacingQuickFixText
    )
}

private fun buildWebViewHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    val recoveryState = input.webViewRecoveryState
    return when {
        input.webViewCompatibilityStatus.verdict == WebViewHealthVerdict.Unavailable -> PreExamHealthItem(
            category = PreExamHealthCategory.WebView,
            verdict = PreExamHealthVerdict.Blocking,
            title = "WebView Provider",
            detail = input.webViewCompatibilityStatus.studentSummary,
            quickFix = input.webViewCompatibilityStatus.quickFix
        )
        input.webViewCompatibilityStatus.severity == WebViewHealthSeverity.Warning -> PreExamHealthItem(
            category = PreExamHealthCategory.WebView,
            verdict = PreExamHealthVerdict.Warning,
            title = "WebView Provider",
            detail = input.webViewCompatibilityStatus.studentSummary,
            quickFix = input.webViewCompatibilityStatus.quickFix
        )
        input.webViewSessionResetInFlight -> PreExamHealthItem(
            category = PreExamHealthCategory.WebView,
            verdict = PreExamHealthVerdict.Warning,
            title = "WebView Provider",
            detail = "A clean WebView session reset is still running.",
            quickFix = "Wait until preparation finishes, then press Start Exam Mode."
        )
        !input.webViewSessionResetError.isNullOrBlank() ||
            recoveryState == "RendererGone" ||
            recoveryState == "ReadyToRetry" -> PreExamHealthItem(
            category = PreExamHealthCategory.WebView,
            verdict = PreExamHealthVerdict.Warning,
            title = "WebView Provider",
            detail = "The previous exam browser was closed safely. ${input.webViewCompatibilityStatus.studentSummary}",
            quickFix = "Export diagnostics if needed, check WebView provider, then press Start Exam Mode again."
        )
        else -> PreExamHealthItem(
            category = PreExamHealthCategory.WebView,
            verdict = PreExamHealthVerdict.Stable,
            title = "WebView Provider",
            detail = "${input.webViewCompatibilityStatus.studentSummary} WebView will be created only after Start Exam Mode."
        )
    }
}

private fun buildLocationHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    val geofenceStatus = input.geofenceRuntimeStatus.securityStatus
    val fakeStatus = input.fakeLocationRuntimeStatus.securityStatus
    return when {
        input.geofenceBypassed || input.fakeLocationBypassed -> PreExamHealthItem(
            category = PreExamHealthCategory.Location,
            verdict = PreExamHealthVerdict.Warning,
            title = "Location / Fake Location",
            detail = "A location-related bypass is active.",
            quickFix = "Use bypass only for approved troubleshooting."
        )
        geofenceStatus.blocking || fakeStatus.blocking -> PreExamHealthItem(
            category = PreExamHealthCategory.Location,
            verdict = PreExamHealthVerdict.Blocking,
            title = "Location / Fake Location",
            detail = "Geofence=${geofenceStatus.finalVerdict.diagnosticLabel()}, fake-location=${fakeStatus.finalVerdict.diagnosticLabel()}.",
            quickFix = "Grant precise location, enable location services, and turn off mock-location tools."
        )
        fakeStatus.warningOnly -> PreExamHealthItem(
            category = PreExamHealthCategory.Location,
            verdict = PreExamHealthVerdict.Warning,
            title = "Location / Fake Location",
            detail = "A suspicious fake-location package exists, but no strong spoof signal is active.",
            quickFix = "Remove fake GPS apps before the exam when possible."
        )
        else -> PreExamHealthItem(
            category = PreExamHealthCategory.Location,
            verdict = PreExamHealthVerdict.Stable,
            title = "Location / Fake Location",
            detail = "Location checks are ready."
        )
    }
}

private fun buildDeviceTimeHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    return when {
        input.deviceTimeBypassed -> PreExamHealthItem(
            category = PreExamHealthCategory.DeviceTime,
            verdict = PreExamHealthVerdict.Warning,
            title = "Device Time",
            detail = "Device Time bypass is active.",
            quickFix = "Keep automatic date, time, and time zone enabled."
        )
        input.deviceTimeSecurityStatus.blocking -> PreExamHealthItem(
            category = PreExamHealthCategory.DeviceTime,
            verdict = PreExamHealthVerdict.Blocking,
            title = "Device Time",
            detail = "Device time verdict is ${input.deviceTimeSecurityStatus.finalVerdict.name}.",
            quickFix = "Enable automatic date/time and automatic time zone, then refresh."
        )
        else -> PreExamHealthItem(
            category = PreExamHealthCategory.DeviceTime,
            verdict = PreExamHealthVerdict.Stable,
            title = "Device Time",
            detail = "Automatic device time checks are ready."
        )
    }
}

private fun buildBatteryPowerHealthItem(input: PreExamHealthCheckInput): PreExamHealthItem {
    val level = input.batteryStatus.levelPercent
    val lowRamDetail = if (input.compatibilityProfile.lowRamProfile.enabled) {
        " Low-RAM mode is active."
    } else {
        ""
    }
    return if (level <= 20 && !input.batteryStatus.isCharging) {
        PreExamHealthItem(
            category = PreExamHealthCategory.BatteryPower,
            verdict = PreExamHealthVerdict.Warning,
            title = "Battery / Power",
            detail = "Battery is $level% and not charging.$lowRamDetail",
            quickFix = "Charge the device and disable aggressive battery saver before the exam."
        )
    } else {
        PreExamHealthItem(
            category = PreExamHealthCategory.BatteryPower,
            verdict = PreExamHealthVerdict.Stable,
            title = "Battery / Power",
            detail = "Battery is $level%. Charging=${input.batteryStatus.isCharging}.$lowRamDetail"
        )
    }
}
