package com.example.coblaxexamlock.ui.admin

import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.WebViewHealthSeverity
import com.example.coblaxexamlock.WebViewHealthVerdict
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict

internal enum class FieldReadinessVerdict {
    Ready,
    Warning,
    Blocked
}

internal enum class FieldReadinessCategory {
    ScreenPinning,
    FloatingAppOverlay,
    WebView,
    Network,
    Battery,
    Location,
    DeviceTime,
    SamsungLegacy
}

internal data class FieldReadinessItem(
    val category: FieldReadinessCategory,
    val verdict: FieldReadinessVerdict,
    val title: String,
    val detail: String,
    val quickFix: String? = null
)

internal data class FieldReadinessReport(
    val generatedAt: String,
    val compatibilityFamily: String,
    val compatibilityLabel: String,
    val items: List<FieldReadinessItem>
) {
    val blockedCount: Int
        get() = items.count { it.verdict == FieldReadinessVerdict.Blocked }

    val warningCount: Int
        get() = items.count { it.verdict == FieldReadinessVerdict.Warning }

    val readyCount: Int
        get() = items.count { it.verdict == FieldReadinessVerdict.Ready }

    val finalVerdict: FieldReadinessVerdict
        get() = when {
            blockedCount > 0 -> FieldReadinessVerdict.Blocked
            warningCount > 0 -> FieldReadinessVerdict.Warning
            else -> FieldReadinessVerdict.Ready
        }

    fun diagnosticSummary(): String {
        return "verdict=${finalVerdict.name}" +
            " | family=$compatibilityFamily" +
            " | blocked=$blockedCount" +
            " | warning=$warningCount" +
            " | ready=$readyCount" +
            " | items=" + items.joinToString(",") { item ->
                "${item.category.name}:${item.verdict.name}"
            }
    }
}

internal enum class AdminReadinessVerdict {
    NotRun,
    Ready,
    NeedsSetup,
    Blocked
}

internal data class AdminReadinessSummary(
    val verdict: AdminReadinessVerdict,
    val title: String,
    val detail: String,
    val blockedCount: Int,
    val warningCount: Int,
    val webViewLabel: String,
    val vendorLabel: String,
    val nextActionLabel: String
)

internal fun buildAdminReadinessSummary(
    report: FieldReadinessReport?,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    vendorChecklist: DeviceVendorChecklist
): AdminReadinessSummary {
    val reportWebViewVerdict = report?.items
        ?.firstOrNull { it.category == FieldReadinessCategory.WebView }
        ?.verdict
    val extraWebViewBlocked =
        reportWebViewVerdict == null &&
            webViewCompatibilityStatus.severity == WebViewHealthSeverity.Blocking
    val extraWebViewWarning =
        reportWebViewVerdict == null &&
            webViewCompatibilityStatus.severity == WebViewHealthSeverity.Warning
    val blockedCount = (report?.blockedCount ?: 0) + if (extraWebViewBlocked) 1 else 0
    val warningCount = (report?.warningCount ?: 0) + if (extraWebViewWarning) 1 else 0
    val verdict = when {
        blockedCount > 0 -> AdminReadinessVerdict.Blocked
        warningCount > 0 -> AdminReadinessVerdict.NeedsSetup
        report == null -> AdminReadinessVerdict.NotRun
        else -> AdminReadinessVerdict.Ready
    }
    return AdminReadinessSummary(
        verdict = verdict,
        title = when (verdict) {
            AdminReadinessVerdict.NotRun -> "Belum dicek"
            AdminReadinessVerdict.Ready -> "Ready"
            AdminReadinessVerdict.NeedsSetup -> "Perlu Setup"
            AdminReadinessVerdict.Blocked -> "Blocked"
        },
        detail = when (verdict) {
            AdminReadinessVerdict.NotRun ->
                "Jalankan pemeriksaan di perangkat asli sebelum ujian."
            AdminReadinessVerdict.Ready ->
                "Perangkat siap untuk uji lapangan."
            AdminReadinessVerdict.NeedsSetup ->
                "Ada hal yang perlu dicek, tetapi hanya blocker keamanan yang menghentikan Start Exam."
            AdminReadinessVerdict.Blocked ->
                "Ada blocker keamanan yang harus dibereskan sebelum ujian."
        },
        blockedCount = blockedCount,
        warningCount = warningCount,
        webViewLabel = when (webViewCompatibilityStatus.verdict) {
            WebViewHealthVerdict.Ready -> "Ready"
            WebViewHealthVerdict.NeedsUpdate -> "Need Update"
            WebViewHealthVerdict.Unavailable -> "Unavailable"
            WebViewHealthVerdict.Unknown -> "Unknown"
        },
        vendorLabel = vendorChecklist.displayName,
        nextActionLabel = when (verdict) {
            AdminReadinessVerdict.NotRun -> "Run Check"
            AdminReadinessVerdict.Ready -> "Details"
            AdminReadinessVerdict.NeedsSetup,
            AdminReadinessVerdict.Blocked -> "Fix First"
        }
    )
}

internal data class FieldReadinessInput(
    val generatedAt: String,
    val compatibilityProfile: DeviceCompatibilityProfile,
    val screenPinningAvailable: Boolean,
    val screenPinningSystemSetting: String,
    val lockTaskState: String,
    val accessibilityGuardAvailable: Boolean,
    val accessibilityGuardEnabled: Boolean,
    val overlayRiskResult: OverlayRiskResult,
    val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    val networkReadinessStatus: NetworkReadinessStatus,
    val batteryStatus: ExamBatteryStatus,
    val locationPermissionGranted: Boolean,
    val preciseLocationGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val geofencePolicyEnabled: Boolean,
    val fakeLocationMonitoringEnabled: Boolean,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus
)

internal fun buildFieldReadinessReport(input: FieldReadinessInput): FieldReadinessReport {
    val items = buildList {
        add(buildFieldScreenPinningItem(input))
        add(buildFieldOverlayItem(input))
        add(buildFieldWebViewItem(input.webViewCompatibilityStatus))
        add(buildFieldNetworkItem(input.networkReadinessStatus))
        add(buildFieldBatteryItem(input.batteryStatus, input.compatibilityProfile.lowRamProfile.enabled))
        add(buildFieldLocationItem(input))
        add(buildFieldDeviceTimeItem(input.deviceTimeSecurityStatus))
        if (input.compatibilityProfile.samsungLegacyTablet) {
            add(buildSamsungLegacyItem(input.compatibilityProfile))
        }
    }
    return FieldReadinessReport(
        generatedAt = input.generatedAt,
        compatibilityFamily = input.compatibilityProfile.family.name,
        compatibilityLabel = input.compatibilityProfile.vendorDisplayName,
        items = items
    )
}

private fun buildFieldScreenPinningItem(input: FieldReadinessInput): FieldReadinessItem {
    val lockTaskActive = input.lockTaskState.equals("PINNED", ignoreCase = true) ||
        input.lockTaskState.equals("LOCKED", ignoreCase = true)
    return when {
        lockTaskActive -> FieldReadinessItem(
            category = FieldReadinessCategory.ScreenPinning,
            verdict = FieldReadinessVerdict.Ready,
            title = "Screen Pinning",
            detail = "Lock task is already active. CBX will skip repeated pinning requests."
        )
        input.screenPinningAvailable -> FieldReadinessItem(
            category = FieldReadinessCategory.ScreenPinning,
            verdict = FieldReadinessVerdict.Ready,
            title = "Screen Pinning",
            detail = "Available. Android should ask for pinning once when the exam starts. System setting=${input.screenPinningSystemSetting}."
        )
        input.accessibilityGuardAvailable && input.accessibilityGuardEnabled -> FieldReadinessItem(
            category = FieldReadinessCategory.ScreenPinning,
            verdict = FieldReadinessVerdict.Warning,
            title = "Screen Pinning",
            detail = "Screen Pinning is unavailable, but CBX Accessibility Exam Guard fallback is active.",
            quickFix = "Keep the accessibility guard enabled through the exam."
        )
        input.accessibilityGuardAvailable -> FieldReadinessItem(
            category = FieldReadinessCategory.ScreenPinning,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Screen Pinning",
            detail = "Screen Pinning is unavailable and the CBX Accessibility Exam Guard fallback is disabled.",
            quickFix = "Enable CBX Lock Exam Guard in Android Accessibility settings."
        )
        else -> FieldReadinessItem(
            category = FieldReadinessCategory.ScreenPinning,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Screen Pinning",
            detail = "Screen Pinning is unavailable on this device.",
            quickFix = "Use an approved fallback device or admin-approved Screen Pinning bypass."
        )
    }
}

private fun buildFieldOverlayItem(input: FieldReadinessInput): FieldReadinessItem {
    val legacySuffix = if (input.compatibilityProfile.allowPartialObscuredWebViewTouch) {
        " Legacy Samsung partial overlay signals are warning-only; full obscured touches still block."
    } else {
        ""
    }
    return when {
        input.overlayRiskResult.bypassed -> FieldReadinessItem(
            category = FieldReadinessCategory.FloatingAppOverlay,
            verdict = FieldReadinessVerdict.Warning,
            title = "Floating App / Overlay",
            detail = "Overlay bypass is active.$legacySuffix",
            quickFix = "Use bypass only for approved troubleshooting."
        )
        input.overlayRiskResult.confirmedInteractionDetected -> FieldReadinessItem(
            category = FieldReadinessCategory.FloatingAppOverlay,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Floating App / Overlay",
            detail = "A confirmed overlay interaction was detected.$legacySuffix",
            quickFix = "Close floating windows, chat heads, sidebars, screen filters, and apps that appear on top."
        )
        input.overlayRiskResult.heuristicRisk ||
            input.overlayRiskResult.riskyAccessibilityPackages.isNotEmpty() -> FieldReadinessItem(
            category = FieldReadinessCategory.FloatingAppOverlay,
            verdict = FieldReadinessVerdict.Warning,
            title = "Floating App / Overlay",
            detail = "Possible overlay/accessibility risk is present.$legacySuffix",
            quickFix = "Review overlay and accessibility permissions before the exam."
        )
        else -> FieldReadinessItem(
            category = FieldReadinessCategory.FloatingAppOverlay,
            verdict = FieldReadinessVerdict.Ready,
            title = "Floating App / Overlay",
            detail = "No floating-app risk is currently detected.$legacySuffix"
        )
    }
}

private fun buildFieldWebViewItem(status: WebViewCompatibilityStatus): FieldReadinessItem {
    return when {
        !status.available -> FieldReadinessItem(
            category = FieldReadinessCategory.WebView,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Exam WebView",
            detail = "Android WebView provider is unavailable.",
            quickFix = status.quickFix
        )
        status.outdatedLikely -> FieldReadinessItem(
            category = FieldReadinessCategory.WebView,
            verdict = FieldReadinessVerdict.Warning,
            title = "Exam WebView",
            detail = "Provider looks old: ${status.displayLabel}.",
            quickFix = status.quickFix
        )
        else -> FieldReadinessItem(
            category = FieldReadinessCategory.WebView,
            verdict = FieldReadinessVerdict.Ready,
            title = "Exam WebView",
            detail = "Provider ${status.displayLabel} is available."
        )
    }
}

private fun buildFieldNetworkItem(status: NetworkReadinessStatus): FieldReadinessItem {
    val verdict = when (status.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> FieldReadinessVerdict.Ready
        NetworkReadinessUserVerdict.Offline,
        NetworkReadinessUserVerdict.CaptivePortal,
        NetworkReadinessUserVerdict.Unvalidated,
        NetworkReadinessUserVerdict.DnsFailed,
        NetworkReadinessUserVerdict.Slow,
        NetworkReadinessUserVerdict.AirplaneMode,
        NetworkReadinessUserVerdict.Unstable -> FieldReadinessVerdict.Warning
    }
    return FieldReadinessItem(
        category = FieldReadinessCategory.Network,
        verdict = verdict,
        title = "Network",
        detail = "Network status is ${status.userFacingVerdict.name} on ${status.transportLabel.ifBlank { "-" }}.",
        quickFix = status.userFacingQuickFixText
    )
}

private fun buildFieldBatteryItem(
    status: ExamBatteryStatus,
    lowRamEnabled: Boolean
): FieldReadinessItem {
    val lowRamSuffix = if (lowRamEnabled) " Low-RAM mode is active." else ""
    return if (status.levelPercent <= 20 && !status.isCharging) {
        FieldReadinessItem(
            category = FieldReadinessCategory.Battery,
            verdict = FieldReadinessVerdict.Warning,
            title = "Battery / Power",
            detail = "Battery is ${status.levelPercent}% and not charging.$lowRamSuffix",
            quickFix = "Charge the device and disable aggressive battery saver before exam time."
        )
    } else {
        FieldReadinessItem(
            category = FieldReadinessCategory.Battery,
            verdict = FieldReadinessVerdict.Ready,
            title = "Battery / Power",
            detail = "Battery is ${status.levelPercent}%. Charging=${status.isCharging}.$lowRamSuffix"
        )
    }
}

private fun buildFieldLocationItem(input: FieldReadinessInput): FieldReadinessItem {
    val locationRequired = input.geofencePolicyEnabled || input.fakeLocationMonitoringEnabled
    return when {
        !locationRequired -> FieldReadinessItem(
            category = FieldReadinessCategory.Location,
            verdict = FieldReadinessVerdict.Ready,
            title = "Location / Fake Location",
            detail = "No active field geofence requirement is configured for Direct Link."
        )
        !input.locationPermissionGranted -> FieldReadinessItem(
            category = FieldReadinessCategory.Location,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Location / Fake Location",
            detail = "Location permission is not granted.",
            quickFix = "Grant location permission before the field trial."
        )
        input.geofencePolicyEnabled && !input.preciseLocationGranted -> FieldReadinessItem(
            category = FieldReadinessCategory.Location,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Location / Fake Location",
            detail = "Precise location is required for geofence validation.",
            quickFix = "Grant precise location, then rerun the field readiness test."
        )
        !input.locationServicesEnabled -> FieldReadinessItem(
            category = FieldReadinessCategory.Location,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Location / Fake Location",
            detail = "Location services are turned off.",
            quickFix = "Turn on Android location services."
        )
        else -> FieldReadinessItem(
            category = FieldReadinessCategory.Location,
            verdict = FieldReadinessVerdict.Ready,
            title = "Location / Fake Location",
            detail = "Location permissions and services are ready."
        )
    }
}

private fun buildFieldDeviceTimeItem(status: DeviceTimeSecurityStatus): FieldReadinessItem {
    return if (status.blocking) {
        FieldReadinessItem(
            category = FieldReadinessCategory.DeviceTime,
            verdict = FieldReadinessVerdict.Blocked,
            title = "Device Time",
            detail = "Device time verdict is ${status.finalVerdict.name}.",
            quickFix = "Enable automatic date/time and automatic time zone."
        )
    } else {
        FieldReadinessItem(
            category = FieldReadinessCategory.DeviceTime,
            verdict = FieldReadinessVerdict.Ready,
            title = "Device Time",
            detail = "Automatic device time checks are ready."
        )
    }
}

private fun buildSamsungLegacyItem(profile: DeviceCompatibilityProfile): FieldReadinessItem =
    FieldReadinessItem(
        category = FieldReadinessCategory.SamsungLegacy,
        verdict = FieldReadinessVerdict.Warning,
        title = "Samsung Legacy Mode",
        detail = "Legacy Samsung tablet policy is active for ${profile.model}. Refresh/back/toolbar actions are treated as trusted CBX chrome, partial overlays are warning-only, and full overlays still block.",
        quickFix = "On SM-T295, confirm Screen Pinning once at Start Exam and avoid floating panels during the test."
    )
