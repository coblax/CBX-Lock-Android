package com.example.coblaxexamlock.ui.exam

import android.os.SystemClock
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import java.util.Locale

internal fun handleExamRuntimeOverlayObscuredTouch(
    touchSignal: ExamOverlayTouchSignal,
    lockTaskRequestPending: Boolean,
    examSessionStarted: Boolean,
    lockTaskStateLabel: String,
    deviceQuirkProfile: ExamRuntimeDeviceQuirkProfile,
    lastTrustedRuntimeChromeActionElapsedMs: Long?,
    lastTrustedRuntimeChromeActionReason: String?,
    currentOverlayEventDetails: (OverlaySignal, String?) -> String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    recordOverlayEvent: (String, OverlaySignal, DiagnosticEventLevel, String?) -> Unit,
    onBlockedOverlayTouch: () -> Unit
): Boolean {
    val nowElapsedMs = SystemClock.elapsedRealtime()
    if (lockTaskRequestPending && !examSessionStarted) {
        val pendingContext = buildString {
            append("source=secure_exam_webview_touch_filter")
            append(" | pinning_pending=true")
            append(" | fully_obscured=")
            append(if (touchSignal.fullyObscured) "yes" else "no")
            append(" | partially_obscured=")
            append(if (touchSignal.partiallyObscured) "yes" else "no")
            append(" | action=")
            append(touchSignal.actionMasked)
            append(" | state=")
            append(lockTaskStateLabel)
        }
        recordAction(
            ExamRuntimeHardeningDiagnostics.PinningTransitionViolationSuppressed,
            "$pendingContext | overlay_touch_pending=true",
            DiagnosticEventLevel.WARNING
        )
        recordAction(
            ExamRuntimeHardeningDiagnostics.OverlayTouchSuppressed,
            currentOverlayEventDetails(OverlaySignal.ObscuredTouch, pendingContext),
            DiagnosticEventLevel.INFO
        )
        return false
    }

    val elapsedSinceTrustedChromeActionMs =
        lastTrustedRuntimeChromeActionElapsedMs?.let { lastActionAt ->
            (nowElapsedMs - lastActionAt).coerceAtLeast(0L)
        }
    val decision = decideExamOverlayTouch(
        signal = touchSignal,
        profile = deviceQuirkProfile,
        source = ExamOverlayTouchSource.WebViewContent,
        elapsedSinceTrustedChromeActionMs = elapsedSinceTrustedChromeActionMs
    )
    val touchContext = buildString {
        append("source=secure_exam_webview_touch_filter")
        append(" | fully_obscured=")
        append(if (touchSignal.fullyObscured) "yes" else "no")
        append(" | partially_obscured=")
        append(if (touchSignal.partiallyObscured) "yes" else "no")
        append(" | action=")
        append(touchSignal.actionMasked)
        append(" | decision=")
        append(decision.name.lowercase(Locale.US))
        append(" | model=")
        append(deviceQuirkProfile.model)
        append(" | samsung_legacy_tablet=")
        append(deviceQuirkProfile.samsungLegacyTablet)
        lastTrustedRuntimeChromeActionReason?.let { reason ->
            append(" | trusted_chrome_reason=")
            append(reason)
        }
        elapsedSinceTrustedChromeActionMs?.let { elapsedMs ->
            append(" | trusted_chrome_age_ms=")
            append(elapsedMs)
        }
    }
    return when (decision) {
        ExamOverlayTouchDecision.Allow -> false
        ExamOverlayTouchDecision.SuppressAndAllow -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.OverlayTouchSuppressed,
                currentOverlayEventDetails(OverlaySignal.ObscuredTouch, touchContext),
                DiagnosticEventLevel.INFO
            )
            false
        }
        ExamOverlayTouchDecision.WarnAndAllow -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.OverlayTouchWarning,
                currentOverlayEventDetails(OverlaySignal.ObscuredTouch, touchContext),
                DiagnosticEventLevel.WARNING
            )
            if (deviceQuirkProfile.samsungLegacyTablet && touchSignal.partiallyObscured) {
                recordAction(
                    ExamRuntimeHardeningDiagnostics.OverlayPartialLegacyWarning,
                    currentOverlayEventDetails(OverlaySignal.ObscuredTouch, touchContext),
                    DiagnosticEventLevel.WARNING
                )
            }
            false
        }
        ExamOverlayTouchDecision.BlockAndReport -> {
            recordOverlayEvent(
                "OVERLAY_TOUCH_DETECTED",
                OverlaySignal.ObscuredTouch,
                DiagnosticEventLevel.SECURITY,
                touchContext
            )
            onBlockedOverlayTouch()
            true
        }
    }
}
