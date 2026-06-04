package com.example.coblaxexamlock.format
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.UiLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
internal fun buildExamNativeFullscreenStateSyncScript(active: Boolean): String {
    return """
        (function() {
            var normalized = ${if (active) "true" else "false"};
            window.__CBT_NATIVE_FULLSCREEN_ACTIVE__ = normalized;
            if (typeof window.__CBT_SET_NATIVE_FULLSCREEN_ACTIVE__ === 'function') {
                window.__CBT_SET_NATIVE_FULLSCREEN_ACTIVE__(normalized);
                return;
            }
            var detail = { active: normalized };
            try {
                window.dispatchEvent(new CustomEvent('cbt-native-fullscreen-change', { detail: detail }));
            } catch (e) {}
            try {
                window.dispatchEvent(new CustomEvent('cbt:native-fullscreen-change', { detail: detail }));
            } catch (e) {}
        })();
    """.trimIndent()
}

internal fun diagnosticTimestamp(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

internal fun formatElapsedDuration(durationMs: Long?, language: UiLanguage): String {
    if (durationMs == null || durationMs < 0L) {
        return "-"
    }
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes <= 0L) {
        localized(language, "$seconds seconds", "$seconds detik")
    } else {
        localized(language, "$minutes min $seconds sec", "$minutes menit $seconds detik")
    }
}

internal fun formatGeofenceDistance(distanceMeters: Double?): String {
    if (distanceMeters == null || distanceMeters.isNaN()) {
        return "-"
    }
    return String.format(Locale.US, "%.1f m", distanceMeters)
}

internal fun formatLocationFixAge(ageMs: Long?): String {
    if (ageMs == null || ageMs < 0L) {
        return "-"
    }
    return if (ageMs < 1_000L) {
        "${ageMs} ms"
    } else {
        String.format(Locale.US, "%.1f s", ageMs / 1000.0)
    }
}
internal fun buildIntegrityPublicSummary(issues: List<String>): String {
    if (issues.isEmpty()) return "OK"
    val categories = linkedSetOf<String>()
    var hasOther = false
    issues.forEach { issue ->
        when {
            issue == "dex_hash_mismatch" -> categories.add("dex_hash")
            issue == "signature_changed" -> categories.add("signature_changed")
            issue.startsWith("sysprop_") || issue == "test_keys" -> categories.add("system_props")
            issue == "hook_class" -> categories.add("hook_class")
            else -> hasOther = true
        }
    }
    if (hasOther) {
        categories.add("other")
    }
    return "issues: ${categories.joinToString()}"
}

internal fun diagnosticSectionEventCodes(section: DiagnosticSection): Set<String> {
    return when (section) {
        DiagnosticSection.Keyboard -> setOf(
            "KEYBOARD_POLICY_VIOLATION",
            "KEYBOARD_PICKER_OPENED",
            "KEYBOARD_SETTINGS_OPENED"
        )
        DiagnosticSection.Bluetooth -> setOf(
            "BLUETOOTH_ENABLED_DURING_EXAM",
            "BLUETOOTH_PERMISSION_REQUESTED",
            "BLUETOOTH_SETTINGS_OPENED"
        )
        DiagnosticSection.Network -> setOf(
            "NETWORK_OFFLINE_STARTED",
            "NETWORK_OFFLINE_RECOVERED",
            "NETWORK_OFFLINE_TOO_LONG_WARNING",
            "NETWORK_OFFLINE_WARNING_ACKNOWLEDGED",
            "NETWORK_UNSTABLE_EPISODE_STARTED",
            "NETWORK_UNSTABLE_EPISODE_RECOVERED",
            "NETWORK_UNSTABLE_WARNING_SHOWN",
            "NETWORK_UNSTABLE_WARNING_ACKNOWLEDGED",
            "NETWORK_QUICK_FIX_REFRESH_REQUESTED",
            "NETWORK_VPN_DETECTED",
            "NETWORK_VPN_CLEARED",
            "VPN_SETTINGS_OPENED",
            "VPN_BYPASS_TAMPER_DETECTED",
            "START_EXAM_BLOCKED_VPN",
            "START_EXAM_BLOCKED_NETWORK_REACHABILITY",
            "EXAM_SERVER_PROBE_ONLINE",
            "EXAM_SERVER_PROBE_WARNING",
            "EXAM_SERVER_PROBE_OFFLINE",
            "INTERNET_SETTINGS_OPENED",
            "WIFI_SETTINGS_OPENED",
            "CELLULAR_SETTINGS_OPENED",
            "AIRPLANE_MODE_SETTINGS_OPENED"
        )
        DiagnosticSection.Accessibility -> setOf(
            "ACCESSIBILITY_ENABLED_DURING_EXAM",
            "START_EXAM_BLOCKED_ACCESSIBILITY",
            "ACCESSIBILITY_SETTINGS_OPENED",
            "ACCESSIBILITY_BYPASS_TAMPER_DETECTED"
        )
        DiagnosticSection.Overlay -> setOf(
            "OVERLAY_TOUCH_DETECTED",
            "OVERLAY_TOUCH_SUPPRESSED",
            "OVERLAY_TOUCH_WARNING",
            "OVERLAY_WINDOW_FOCUS_LOSS",
            "OVERLAY_MONITOR_SUPPRESSED",
            "OVERLAY_SHIELD_APPLIED",
            "OVERLAY_SHIELD_DISABLED",
            "OVERLAY_SHIELD_APPLY_FAILED",
            "OVERLAY_SHIELD_UNSUPPORTED",
            "OVERLAY_SETTINGS_OPENED",
            "OVERLAY_ACCESSIBILITY_SETTINGS_OPENED",
            "OVERLAY_BYPASS_TAMPER_DETECTED",
            "DPC_STATUS_RESOLVED",
            "DPC_CREATE_WINDOWS_RESTRICTION_APPLIED",
            "DPC_CREATE_WINDOWS_RESTRICTION_UNSUPPORTED",
            "DPC_CREATE_WINDOWS_RESTRICTION_CLEARED"
        )
        DiagnosticSection.Geofence -> setOf(
            "LOCATION_PERMISSION_REQUESTED",
            "GEOFENCE_BYPASS_TAMPER_DETECTED",
            "START_EXAM_BLOCKED_GEOFENCE_CONFIG",
            "START_EXAM_BLOCKED_GEOFENCE_PERMISSION",
            "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED",
            "START_EXAM_BLOCKED_GEOFENCE_LOCATION_DISABLED",
            "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
            "START_EXAM_BLOCKED_GEOFENCE_OUTSIDE",
            "GEOFENCE_RUNTIME_OUTSIDE",
            "GEOFENCE_RUNTIME_PRECISE_REQUIRED",
            "GEOFENCE_RUNTIME_LOCATION_UNAVAILABLE",
            "GEOFENCE_RUNTIME_RECOVERED"
        )
        DiagnosticSection.FakeLocation -> setOf(
            "FAKE_LOCATION_BYPASS_TAMPER_DETECTED",
            "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION",
            "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_DISABLED",
            "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_UNAVAILABLE",
            "START_EXAM_BLOCKED_FAKE_LOCATION_SPOOF",
            "FAKE_LOCATION_PACKAGE_WARNING",
            "FAKE_LOCATION_RUNTIME_PERMISSION_REQUIRED",
            "FAKE_LOCATION_RUNTIME_LOCATION_SERVICES_REQUIRED",
            "FAKE_LOCATION_RUNTIME_LOCATION_UNAVAILABLE",
            "FAKE_LOCATION_RUNTIME_SPOOF_DETECTED",
            "FAKE_LOCATION_RUNTIME_RECOVERED"
        )
        DiagnosticSection.AppSwitch -> setOf(
            "APP_SWITCH_DETECTED",
            "APP_SWITCH_RESUME_AFTER_LEAVE",
            "APP_SWITCH_MONITOR_SUPPRESSED",
            "APP_SWITCH_FALLBACK_ARMED",
            "APP_SWITCH_BYPASS_TAMPER_DETECTED",
            "ACCESSIBILITY_GUARD_ENABLED_REQUIRED",
            "ACCESSIBILITY_GUARD_MISSING_BLOCKED",
            "ACCESSIBILITY_GUARD_ARMED",
            "ACCESSIBILITY_GUARD_DISARMED",
            "ACCESSIBILITY_GUARD_APP_SWITCH_DETECTED",
            "ACCESSIBILITY_GUARD_SERVICE_DISABLED",
            "ACCESSIBILITY_GUARD_NOTIFICATION_SHADE_DETECTED",
            "ACCESSIBILITY_GUARD_SYSTEM_PANEL_DETECTED",
            "ACCESSIBILITY_GUARD_LIVENESS_MONITOR_STARTED",
            "ACCESSIBILITY_GUARD_LIVENESS_MONITOR_STOPPED",
            "ACCESSIBILITY_GUARD_RETURN_TO_EXAM_REQUESTED"
        )
        DiagnosticSection.DeveloperAdb -> setOf(
            "ADB_ENABLED_DURING_EXAM",
            "START_EXAM_BLOCKED_ADB",
            "START_EXAM_BLOCKED_DEVELOPER_OPTIONS",
            "DEVELOPER_OPTIONS_ENABLED_DURING_EXAM",
            "DEVELOPER_OPTIONS_OPENED",
            "ADB_BYPASS_TAMPER_DETECTED"
        )
        DiagnosticSection.Root -> setOf(
            "ROOT_INDICATOR_DETECTED",
            "START_EXAM_BLOCKED_ROOT",
            "ROOT_BYPASS_TAMPER_DETECTED"
        )
        DiagnosticSection.Signature -> setOf(
            "SIGNATURE_MISMATCH_DETECTED",
            "START_EXAM_BLOCKED_SIGNATURE",
            "OFFICIAL_APK_REINSTALL_OPENED"
        )
        DiagnosticSection.VirtualEnvironment -> setOf(
            "VIRTUAL_ENVIRONMENT_DETECTED",
            "START_EXAM_BLOCKED_VIRTUAL_ENV"
        )
        DiagnosticSection.Clipboard -> setOf(
            "CLIPBOARD_CHANGED",
            "CLIPBOARD_BYPASS_TAMPER_DETECTED"
        )
        DiagnosticSection.ScreenPinning -> setOf(
            "SCREEN_PINNING_REQUESTED",
            "SCREEN_PINNING_PENDING",
            "SCREEN_PINNING_ACTIVE",
            "SCREEN_PINNING_FAILED",
            "SCREEN_PINNING_REQUEST_FAILED",
            "SCREEN_PINNING_BYPASSED",
            "SCREEN_PINNING_BYPASS_USED",
            "SCREEN_PINNING_BYPASS_TAMPER_DETECTED",
            "SCREEN_PINNING_TRANSIENT_LOSS_RECHECK",
            "SCREEN_PINNING_LOST_DURING_EXAM",
            "START_EXAM_BLOCKED_SCREEN_PINNING_UNAVAILABLE",
            "DPC_STATUS_RESOLVED",
            "DPC_LOCK_TASK_ALLOWLIST_APPLIED"
        )
        DiagnosticSection.DeviceTime -> setOf(
            "DEVICE_TIME_AUTO_DISABLED",
            "DEVICE_TIME_AUTO_TIME_ZONE_DISABLED",
            "DEVICE_TIME_DRIFT_DETECTED",
            "DEVICE_TIME_BYPASS_TAMPER_DETECTED",
            "DEVICE_TIME_SETTINGS_OPENED",
            "QR_BLOCKED_DEVICE_TIME",
            "START_EXAM_BLOCKED_DEVICE_TIME"
        )
        DiagnosticSection.SecurityHealth -> setOf(
            "DEVICE_SURVIVAL_POLICY_RESOLVED",
            "COMPATIBILITY_SCORE_UPDATED",
            "WEBVIEW_PROVIDER_HEALTH_RESOLVED",
            "WEBVIEW_PROVIDER_HEALTH_WARNING",
            "WEBVIEW_PROVIDER_HEALTH_FIX_OPENED",
            "PRE_EXAM_HEALTH_CHECK_STARTED",
            "PRE_EXAM_HEALTH_CHECK_COMPLETED",
            "START_EXAM_BLOCKED_HEALTH_CHECK"
        )
        DiagnosticSection.ScreenRecorder -> setOf(
            "SCREEN_RECORDER_DETECTED",
            "SCREEN_RECORDER_CLEARED",
            "START_EXAM_BLOCKED_SCREEN_RECORDER",
            "APP_SETTINGS_OPENED"
        )
        DiagnosticSection.DisplayMirror -> setOf(
            "DISPLAY_MIRROR_DETECTED",
            "DISPLAY_MIRROR_CLEARED",
            "START_EXAM_BLOCKED_DISPLAY_MIRROR",
            "CAST_SETTINGS_OPENED"
        )
        DiagnosticSection.MultiWindow -> setOf(
            "MULTI_WINDOW_DETECTED",
            "MULTI_WINDOW_CLEARED",
            "MULTI_WINDOW_MODE_CHANGED",
            "START_EXAM_BLOCKED_MULTI_WINDOW"
        )
    }
}
