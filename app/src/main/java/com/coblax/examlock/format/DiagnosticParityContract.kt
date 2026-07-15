package com.coblax.examlock.format

import com.coblax.examlock.model.DiagnosticSection

internal data class DiagnosticParityContract(
    val section: DiagnosticSection,
    val preparationTokens: List<String>,
    val telegramTokens: List<String>,
    val runtimeDialogTokens: List<String> = emptyList(),
    val eventCodes: List<String> = emptyList(),
    val primaryActions: List<String> = emptyList()
)

internal fun diagnosticParityContracts(): List<DiagnosticParityContract> = listOf(
    DiagnosticParityContract(
        section = DiagnosticSection.Network,
        preparationTokens = listOf(
            "Readiness verdict",
            "VPN active",
            "VPN bypass active",
            "VPN bypass tampered",
            "Transports",
            "Interface",
            "Validated",
            "Captive portal",
            "DNS probe"
        ),
        telegramTokens = listOf(
            "Readiness verdict",
            "VPN active",
            "VPN bypass active",
            "VPN bypass tampered",
            "Transports",
            "Interface",
            "Validated",
            "Captive portal"
        ),
        runtimeDialogTokens = listOf(
            "Transport",
            "Interface",
            "VPN bypass active",
            "VPN bypass tampered"
        ),
        eventCodes = listOf(
            "NETWORK_VPN_DETECTED",
            "NETWORK_VPN_CLEARED",
            "START_EXAM_BLOCKED_VPN",
            "START_EXAM_BLOCKED_NETWORK_REACHABILITY",
            "EXAM_SERVER_PROBE_ONLINE",
            "EXAM_SERVER_PROBE_WARNING",
            "EXAM_SERVER_PROBE_OFFLINE",
            "VPN_BYPASS_TAMPER_DETECTED",
            "VPN_SETTINGS_OPENED"
        ),
        primaryActions = listOf("Open VPN Settings", "Refresh Status", "Send Network Report")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.SecurityHealth,
        preparationTokens = listOf("Verdict", "Severity", "Provider label", "Package", "Version", "Quick fix"),
        telegramTokens = listOf("WEBVIEW_PROVIDER_HEALTH_RESOLVED", "WEBVIEW_PROVIDER_HEALTH_WARNING"),
        eventCodes = listOf(
            "WEBVIEW_PROVIDER_HEALTH_RESOLVED",
            "WEBVIEW_PROVIDER_HEALTH_WARNING",
            "WEBVIEW_PROVIDER_HEALTH_FIX_OPENED"
        ),
        primaryActions = listOf("Open WebView Settings", "Refresh Status")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.DeviceTime,
        preparationTokens = listOf(
            "Final verdict",
            "Bypass state",
            "Automatic date/time",
            "Automatic time zone",
            "Clock drift",
            "Blocking now"
        ),
        telegramTokens = listOf(
            "Device time verdict",
            "Device time bypass active",
            "Device time bypass state",
            "Auto date & time",
            "Automatic time zone",
            "Blocking now"
        ),
        eventCodes = listOf("DEVICE_TIME_SETTINGS_OPENED", "START_EXAM_BLOCKED_DEVICE_TIME"),
        primaryActions = listOf("Open Date & Time Settings", "Refresh Status")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.ScreenPinning,
        preparationTokens = listOf("Screen pinning", "lock task", "bypass", "DPC"),
        telegramTokens = listOf("Screen pinning", "lock task", "bypass", "DPC"),
        eventCodes = listOf(
            "SCREEN_PINNING_REQUESTED",
            "SCREEN_PINNING_ACTIVE",
            "SCREEN_PINNING_FAILED",
            "SCREEN_PINNING_BYPASS_TAMPER_DETECTED",
            "DPC_STATUS_RESOLVED",
            "DPC_LOCK_TASK_ALLOWLIST_APPLIED"
        ),
        primaryActions = listOf("Start Screen Pinning", "Open Screen Pinning Settings")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.ScreenRecorder,
        preparationTokens = listOf(
            "Detection method",
            "Visible recorder package count",
            "No visible recorder package detected",
            "Bypass active"
        ),
        telegramTokens = listOf(
            "Detection method",
            "Screen recorder packages detected",
            "Package visibility note",
            "Bypass active",
            "Bypass tampered",
            "Runtime violation count"
        ),
        runtimeDialogTokens = listOf(
            "Detection method",
            "Detected package count",
            "Detected packages",
            "Runtime violation count"
        ),
        eventCodes = listOf(
            "SCREEN_RECORDER_DETECTED",
            "SCREEN_RECORDER_CLEARED",
            "START_EXAM_BLOCKED_SCREEN_RECORDER"
        ),
        primaryActions = listOf("Open App Settings", "Refresh Status", "Send Report")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.DisplayMirror,
        preparationTokens = listOf(
            "Detection method",
            "External display detected",
            "External display count",
            "Bypass active"
        ),
        telegramTokens = listOf(
            "Detection method",
            "External display detected",
            "External display count",
            "Bypass active",
            "Bypass tampered",
            "Runtime violation count"
        ),
        runtimeDialogTokens = listOf(
            "Detection method",
            "External display count",
            "External displays",
            "Runtime violation count"
        ),
        eventCodes = listOf(
            "DISPLAY_MIRROR_DETECTED",
            "DISPLAY_MIRROR_CLEARED",
            "START_EXAM_BLOCKED_DISPLAY_MIRROR"
        ),
        primaryActions = listOf("Open Cast Settings", "Refresh Status", "Send Report")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.MultiWindow,
        preparationTokens = listOf(
            "Detection method",
            "isInMultiWindowMode",
            "isInPictureInPictureMode",
            "isInAnySplitMode",
            "Bypass active"
        ),
        telegramTokens = listOf(
            "Detection method",
            "isInMultiWindowMode",
            "isInPictureInPictureMode",
            "isInAnySplitMode",
            "Bypass active",
            "Bypass tampered",
            "Runtime violation count"
        ),
        runtimeDialogTokens = listOf(
            "Detection method",
            "isInMultiWindowMode",
            "isInPictureInPictureMode",
            "isInAnySplitMode",
            "Runtime violation count"
        ),
        eventCodes = listOf(
            "MULTI_WINDOW_MODE_CHANGED",
            "MULTI_WINDOW_DETECTED",
            "MULTI_WINDOW_CLEARED",
            "START_EXAM_BLOCKED_MULTI_WINDOW"
        ),
        primaryActions = listOf("Refresh Status", "Send Report")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.AppSwitch,
        preparationTokens = listOf("Bypass", "fallback", "active"),
        telegramTokens = listOf("App switch bypass", "Violation count", "last trigger"),
        runtimeDialogTokens = listOf("violation", "fallback"),
        eventCodes = listOf("APP_SWITCH_DETECTED", "APP_SWITCH_BYPASS_TAMPER_DETECTED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Overlay,
        preparationTokens = listOf("Overlay", "Bypass", "shield", "DPC"),
        telegramTokens = listOf("Overlay bypass", "Overlay violation count", "Overlay signals", "DPC"),
        runtimeDialogTokens = listOf("Overlay", "violation"),
        eventCodes = listOf(
            "OVERLAY_TOUCH_DETECTED",
            "OVERLAY_BYPASS_TAMPER_DETECTED",
            "DPC_CREATE_WINDOWS_RESTRICTION_APPLIED",
            "DPC_CREATE_WINDOWS_RESTRICTION_UNSUPPORTED",
            "DPC_CREATE_WINDOWS_RESTRICTION_CLEARED"
        )
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Geofence,
        preparationTokens = listOf("Geofence", "permission", "bypass"),
        telegramTokens = listOf("Geofence bypass state", "Location permission granted", "Current coordinates"),
        runtimeDialogTokens = listOf("Location", "violation"),
        eventCodes = listOf("GEOFENCE_RUNTIME_OUTSIDE", "START_EXAM_BLOCKED_GEOFENCE_OUTSIDE")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.FakeLocation,
        preparationTokens = listOf("Fake", "location", "bypass"),
        telegramTokens = listOf("Fake location", "bypass", "suspicious"),
        runtimeDialogTokens = listOf("Fake Location", "violation"),
        eventCodes = listOf("FAKE_LOCATION_RUNTIME_SPOOF_DETECTED", "START_EXAM_BLOCKED_FAKE_LOCATION_SPOOF")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Clipboard,
        preparationTokens = listOf("Clipboard", "bypass"),
        telegramTokens = listOf("Clipboard", "violation count", "signature"),
        runtimeDialogTokens = listOf("Clipboard", "violation"),
        eventCodes = listOf("CLIPBOARD_CHANGED", "CLIPBOARD_BYPASS_TAMPER_DETECTED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Bluetooth,
        preparationTokens = listOf("Bluetooth", "permission", "enabled"),
        telegramTokens = listOf("Bluetooth permission", "Bluetooth aktif", "Bluetooth adapter state"),
        runtimeDialogTokens = listOf("Bluetooth", "violation"),
        eventCodes = listOf("BLUETOOTH_ENABLED_DURING_EXAM", "BLUETOOTH_SETTINGS_OPENED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Accessibility,
        preparationTokens = listOf("Accessibility", "bypass", "services"),
        telegramTokens = listOf("Accessibility bypass", "Accessibility services count", "Risky accessibility packages"),
        runtimeDialogTokens = listOf("Accessibility", "violation"),
        eventCodes = listOf("ACCESSIBILITY_ENABLED_DURING_EXAM", "ACCESSIBILITY_BYPASS_TAMPER_DETECTED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.DeveloperAdb,
        preparationTokens = listOf("Developer", "ADB", "bypass"),
        telegramTokens = listOf("ADB", "Developer", "USB"),
        eventCodes = listOf("ADB_ENABLED_DURING_EXAM", "START_EXAM_BLOCKED_ADB", "ADB_BYPASS_TAMPER_DETECTED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Root,
        preparationTokens = listOf("Root", "bypass", "detected"),
        telegramTokens = listOf("Root", "bypass", "detected"),
        eventCodes = listOf("ROOT_INDICATOR_DETECTED", "START_EXAM_BLOCKED_ROOT", "ROOT_BYPASS_TAMPER_DETECTED")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.Signature,
        preparationTokens = listOf("Signature", "official", "APK"),
        telegramTokens = listOf("Signature", "fingerprint", "install source"),
        eventCodes = listOf("SIGNATURE_MISMATCH_DETECTED", "START_EXAM_BLOCKED_SIGNATURE")
    ),
    DiagnosticParityContract(
        section = DiagnosticSection.VirtualEnvironment,
        preparationTokens = listOf("Virtual", "environment", "detected"),
        telegramTokens = listOf("Virtual environment", "detected", "signals"),
        eventCodes = listOf("VIRTUAL_ENVIRONMENT_DETECTED", "START_EXAM_BLOCKED_VIRTUAL_ENV")
    )
)
