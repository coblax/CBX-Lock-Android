package com.example.coblaxexamlock.ui.exam

import android.os.Build
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.resolveDeviceCompatibilityProfile

internal data class ExamRuntimeDeviceQuirkProfile(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val sdkInt: Int,
    val samsungLegacyTablet: Boolean,
    val overlayFocusLossConfirmWindowMillis: Long,
    val overlayChromeActionSuppressionMillis: Long,
    val screenPinningLostConfirmWindowMillis: Long,
    val allowPartialObscuredWebViewTouch: Boolean
)

internal data class ExamOverlayTouchSignal(
    val fullyObscured: Boolean,
    val partiallyObscured: Boolean,
    val actionMasked: Int
) {
    val obscured: Boolean
        get() = fullyObscured || partiallyObscured
}

internal enum class ExamOverlayTouchSource {
    WebViewContent,
    RuntimeChrome
}

internal enum class ExamOverlayTouchDecision {
    Allow,
    WarnAndAllow,
    SuppressAndAllow,
    BlockAndReport
}

internal data class ExamTrustedChromeActionSuppression(
    val reason: String,
    val ageMs: Long
)

internal fun currentExamRuntimeDeviceQuirkProfile(): ExamRuntimeDeviceQuirkProfile {
    return resolveExamRuntimeDeviceQuirkProfile(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        sdkInt = Build.VERSION.SDK_INT
    )
}

internal fun resolveExamRuntimeDeviceQuirkProfile(
    manufacturer: String?,
    brand: String?,
    model: String?,
    sdkInt: Int
): ExamRuntimeDeviceQuirkProfile {
    return resolveDeviceCompatibilityProfile(
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        sdkInt = sdkInt
    ).toExamRuntimeDeviceQuirkProfile()
}

internal fun DeviceCompatibilityProfile.toExamRuntimeDeviceQuirkProfile(): ExamRuntimeDeviceQuirkProfile {
    return ExamRuntimeDeviceQuirkProfile(
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        sdkInt = sdkInt,
        samsungLegacyTablet = samsungLegacyTablet,
        overlayFocusLossConfirmWindowMillis = overlayFocusLossConfirmWindowMillis,
        overlayChromeActionSuppressionMillis = overlayChromeActionSuppressionMillis,
        screenPinningLostConfirmWindowMillis = screenPinningLostConfirmWindowMillis,
        allowPartialObscuredWebViewTouch = allowPartialObscuredWebViewTouch
    )
}

internal fun decideExamOverlayTouch(
    signal: ExamOverlayTouchSignal,
    profile: ExamRuntimeDeviceQuirkProfile,
    source: ExamOverlayTouchSource,
    elapsedSinceTrustedChromeActionMs: Long?
): ExamOverlayTouchDecision {
    if (!signal.obscured) {
        return ExamOverlayTouchDecision.Allow
    }

    val withinTrustedChromeWindow =
        elapsedSinceTrustedChromeActionMs != null &&
            elapsedSinceTrustedChromeActionMs >= 0L &&
            elapsedSinceTrustedChromeActionMs <= profile.overlayChromeActionSuppressionMillis

    if (
        source == ExamOverlayTouchSource.RuntimeChrome &&
        withinTrustedChromeWindow
    ) {
        return ExamOverlayTouchDecision.SuppressAndAllow
    }

    if (
        withinTrustedChromeWindow &&
        signal.partiallyObscured &&
        !signal.fullyObscured
    ) {
        return ExamOverlayTouchDecision.SuppressAndAllow
    }

    if (
        profile.allowPartialObscuredWebViewTouch &&
        signal.partiallyObscured &&
        !signal.fullyObscured
    ) {
        return ExamOverlayTouchDecision.WarnAndAllow
    }

    return ExamOverlayTouchDecision.BlockAndReport
}

internal fun resolveExamTrustedChromeActionSuppression(
    profile: ExamRuntimeDeviceQuirkProfile,
    nowElapsedMs: Long,
    lastTrustedActionElapsedMs: Long?,
    lastTrustedActionReason: String?
): ExamTrustedChromeActionSuppression? {
    val lastActionAt = lastTrustedActionElapsedMs ?: return null
    val ageMs = (nowElapsedMs - lastActionAt).coerceAtLeast(0L)
    if (ageMs > profile.overlayChromeActionSuppressionMillis) {
        return null
    }
    return ExamTrustedChromeActionSuppression(
        reason = lastTrustedActionReason?.ifBlank { null } ?: "runtime_chrome",
        ageMs = ageMs
    )
}
