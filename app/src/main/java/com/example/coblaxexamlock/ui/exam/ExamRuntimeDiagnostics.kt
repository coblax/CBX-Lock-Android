package com.example.coblaxexamlock.ui.exam

import android.os.SystemClock
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import java.util.Locale

internal fun examRuntimeDiagnosticScreen(
    lockTaskRequestPending: Boolean,
    examSessionStarted: Boolean,
    examRuntimeMonitoringArmed: Boolean
): String {
    return when {
        lockTaskRequestPending -> "pinning_wait"
        examSessionStarted -> "exam_webview"
        examRuntimeMonitoringArmed -> "exam_arming"
        else -> "preparation"
    }
}

internal fun resolveAppSwitchSuppressionReason(
    reason: AppSwitchSuppressionReason?,
    expiresAtElapsedMs: Long?,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): AppSwitchSuppressionReason? {
    val currentReason = reason ?: return null
    val expiresAt = expiresAtElapsedMs ?: return currentReason
    return if (nowElapsedMs <= expiresAt) currentReason else null
}

internal fun buildAppSwitchEventDetails(
    signal: AppSwitchSignal,
    appSwitchStatus: AppSwitchStatus,
    screenPinningMode: ScreenPinningMode,
    lockTaskActive: Boolean,
    suppressionReason: AppSwitchSuppressionReason? = null
): String {
    return buildString {
        append(
            AppSwitchMonitor.eventDetails(
                signal = signal,
                protectionMode = appSwitchStatus.protectionMode,
                screenPinningMode = screenPinningMode,
                lockTaskActive = lockTaskActive,
                suppressionReason = suppressionReason
            )
        )
        append(" | accessibility_guard_enabled=")
        append(if (appSwitchStatus.accessibilityGuardEnabled) "yes" else "no")
        append(" | accessibility_fallback_active=")
        append(if (appSwitchStatus.accessibilityFallbackActive) "yes" else "no")
        append(" | accessibility_violations=")
        append(appSwitchStatus.accessibilityViolationCount)
        append(" | accessibility_last_reason=")
        append(appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-")
        append(" | accessibility_last_package=")
        append(appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-")
        append(" | accessibility_last_event=")
        append(appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-")
        append(" | accessibility_alarm_severity=")
        append(appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-")
    }
}

internal fun buildOverlayEventDetails(
    signal: OverlaySignal,
    overlayShieldStatus: OverlayShieldStatus,
    appSwitchStatus: AppSwitchStatus,
    pendingForcedExitViolation: Boolean,
    appSwitchLifecycleResumePending: Boolean,
    overlayWindowHasFocus: Boolean,
    suppressionReason: AppSwitchSuppressionReason?,
    hasFullScreenCustomView: Boolean,
    extraContext: String? = null
): String {
    return buildString {
        append("trigger=")
        append(signal.diagnosticLabel())
        append(" | shield_supported=")
        append(if (overlayShieldStatus.supported) "yes" else "no")
        append(" | shield_requested=")
        append(if (overlayShieldStatus.requested) "yes" else "no")
        append(" | shield_active=")
        append(if (overlayShieldStatus.active) "yes" else "no")
        append(" | app_switch_monitoring=")
        append(if (appSwitchStatus.runtimeMonitoringActive) "yes" else "no")
        append(" | app_switch_pending=")
        append(if (pendingForcedExitViolation || appSwitchLifecycleResumePending) "yes" else "no")
        append(" | window_focus=")
        append(if (overlayWindowHasFocus) "focused" else "not_focused")
        suppressionReason?.let {
            append(" | app_switch_suppression=")
            append(it.diagnosticLabel())
        }
        if (hasFullScreenCustomView) {
            append(" | fullscreen_custom_view=yes")
        }
        extraContext?.takeIf { it.isNotBlank() }?.let {
            append(" | ")
            append(it)
        }
    }
}

internal fun resolveInternalDialogReason(
    showOfflineWarningDialog: Boolean,
    showNetworkUnstableDialog: Boolean,
    showForcedExitAlarm: Boolean,
    showKeyboardViolationDialog: Boolean,
    showOverlayViolationDialog: Boolean,
    showGeofenceViolationDialog: Boolean,
    showFakeLocationViolationDialog: Boolean,
    showBluetoothViolationDialog: Boolean,
    showClipboardViolationDialog: Boolean,
    showExitExamDialog: Boolean,
    pendingSectionPresent: Boolean,
    securityIssueDialogMessagePresent: Boolean,
    bugReportFeedbackMessagePresent: Boolean
): String? {
    return when {
        showOfflineWarningDialog -> "offline_warning_dialog"
        showNetworkUnstableDialog -> "network_unstable_dialog"
        showForcedExitAlarm -> "app_switch_alarm_dialog"
        showKeyboardViolationDialog -> "keyboard_alarm_dialog"
        showOverlayViolationDialog -> "overlay_alarm_dialog"
        showGeofenceViolationDialog -> "geofence_alarm_dialog"
        showFakeLocationViolationDialog -> "fake_location_alarm_dialog"
        showBluetoothViolationDialog -> "bluetooth_alarm_dialog"
        showClipboardViolationDialog -> "clipboard_alarm_dialog"
        showExitExamDialog -> "exit_exam_dialog"
        pendingSectionPresent -> "telegram_confirm_dialog"
        securityIssueDialogMessagePresent -> "security_issue_dialog"
        bugReportFeedbackMessagePresent -> "feedback_dialog"
        else -> null
    }
}

internal fun prependDiagnosticEvent(
    existingEvents: List<DiagnosticEvent>,
    code: String,
    details: String,
    level: DiagnosticEventLevel,
    screen: String,
    appStartedAtElapsedMs: Long,
    examSessionStartedAtElapsedMs: Long?,
    maxEntries: Int,
    nowElapsedMs: Long = SystemClock.elapsedRealtime(),
    timestamp: String = diagnosticTimestamp()
): List<DiagnosticEvent> {
    return (listOf(
        DiagnosticEvent(
            timestamp = timestamp,
            level = level.name,
            code = code,
            screen = screen,
            appElapsedMs = nowElapsedMs - appStartedAtElapsedMs,
            sessionElapsedMs = examSessionStartedAtElapsedMs?.let { nowElapsedMs - it },
            details = details.ifBlank { "-" }
        )
    ) + existingEvents).take(maxEntries)
}

internal fun buildGeofenceEventDetails(
    trigger: String,
    geofenceStatus: GeofenceSecurityStatus,
    policySource: LocationPolicySource,
    extraContext: String? = null
): String {
    val evaluation = geofenceStatus.geofenceEvaluation
    val config = evaluation.config
    val snapshot = evaluation.locationSnapshot
    return buildString {
        append("trigger=").append(trigger)
        append(" | policy_source=").append(policySource.diagnosticLabel())
        append(" | geofence_verdict=").append(evaluation.verdict.diagnosticLabel())
        append(" | final_verdict=").append(geofenceStatus.finalVerdict.diagnosticLabel())
        append(" | enabled=").append(if (evaluation.enabled) "yes" else "no")
        append(" | shape=").append(config?.shapeType?.name?.lowercase(Locale.US) ?: "-")
        append(" | polygon_points=").append(config?.vertices?.size ?: 0)
        append(" | permission=").append(if (evaluation.permissionGranted) "granted" else "missing")
        append(" | precise=").append(if (geofenceStatus.preciseLocationGranted) "granted" else "required")
        append(" | services=").append(if (evaluation.locationServicesEnabled) "enabled" else "disabled")
        append(" | center=").append(config?.let { formatCoordinates(it.centerLat, it.centerLng) } ?: "-")
        append(" | radius_m=").append(
            config?.radiusMeters?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
        )
        append(" | current=").append(snapshot?.let { formatCoordinates(it.latitude, it.longitude) } ?: "-")
        append(" | fix_quality=").append(geofenceStatus.fixQualityStatus.verdict.diagnosticLabel())
        append(" | fix_age=").append(formatLocationFixAge(geofenceStatus.fixQualityStatus.ageMs))
        append(" | snapshot_used=").append(if (geofenceStatus.fixQualityStatus.usableForGeofence) "yes" else "no")
        append(" | distance_m=").append(formatGeofenceDistance(evaluation.distanceMeters))
        append(" | provider=").append(snapshot?.provider?.ifBlank { "-" } ?: "-")
        append(" | accuracy_m=").append(
            snapshot?.accuracyMeters?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
        )
        evaluation.configError?.takeIf { it.isNotBlank() }?.let {
            append(" | config_error=").append(it)
        }
        extraContext?.takeIf { it.isNotBlank() }?.let {
            append(" | ").append(it)
        }
    }
}

internal fun buildFakeLocationEventDetails(
    trigger: String,
    fakeLocationStatus: LocationSpoofSecurityStatus,
    extraContext: String? = null
): String {
    return buildString {
        append("trigger=").append(trigger)
        append(" | monitoring_enabled=").append(if (fakeLocationStatus.monitoringEnabled) "yes" else "no")
        append(" | permission_granted=").append(if (fakeLocationStatus.permissionGranted) "yes" else "no")
        append(" | location_services_enabled=").append(if (fakeLocationStatus.locationServicesEnabled) "yes" else "no")
        append(" | snapshot_available=").append(if (fakeLocationStatus.snapshotAvailable) "yes" else "no")
        append(" | final_verdict=").append(fakeLocationStatus.finalVerdict.diagnosticLabel())
        append(" | confidence_tier=").append(fakeLocationStatus.confidenceTier.diagnosticLabel())
        append(" | mock=").append(if (fakeLocationStatus.mockLocationDetected) "yes" else "no")
        append(" | developer_options=").append(if (fakeLocationStatus.developerOptionsEnabled) "enabled" else "disabled")
        append(" | fix_quality=").append(fakeLocationStatus.fixQualityStatus.verdict.diagnosticLabel())
        append(" | fix_quality_eligible=").append(if (fakeLocationStatus.fixQualityEligible) "yes" else "no")
        append(" | suspicious_packages=").append(
            fakeLocationStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }
        )
        append(" | supporting_signals=").append(
            fakeLocationStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }
        )
        extraContext?.takeIf { it.isNotBlank() }?.let {
            append(" | ").append(it)
        }
    }
}

internal fun buildNetworkEventDetails(
    trigger: String,
    status: NetworkReadinessStatus,
    extraContext: String? = null
): String {
    return buildString {
        append("trigger=").append(trigger)
        append(" | verdict=").append(status.verdict.name.lowercase(Locale.US))
        append(" | transport=").append(status.transportLabel.ifBlank { "-" })
        append(" | connected=").append(if (status.examStatus.isConnected) "yes" else "no")
        append(" | validated=").append(if (status.diagnostics.isValidated) "yes" else "no")
        append(" | captive_portal=").append(if (status.diagnostics.isCaptivePortal) "yes" else "no")
        append(" | metered=").append(if (status.diagnostics.isMetered) "yes" else "no")
        append(" | vpn=").append(if (status.diagnostics.isVpnActive) "yes" else "no")
        append(" | airplane_mode=").append(if (status.diagnostics.isAirplaneModeEnabled) "yes" else "no")
        append(" | interface=").append(status.diagnostics.interfaceName.ifBlank { "-" })
        append(" | user_verdict=").append(status.userFacingVerdict.name.lowercase(Locale.US))
        append(" | dns_probe=").append(status.dnsProbeStatus.verdict.name.lowercase(Locale.US))
        append(" | dns_latency=").append(status.dnsProbeStatus.latencyBucket.name.lowercase(Locale.US))
        append(" | detail=").append(status.examStatus.detail.ifBlank { "-" })
        extraContext?.takeIf { it.isNotBlank() }?.let {
            append(" | ").append(it)
        }
    }
}
