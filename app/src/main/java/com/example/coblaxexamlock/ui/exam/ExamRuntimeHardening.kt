package com.example.coblaxexamlock.ui.exam

import android.annotation.SuppressLint
import android.webkit.WebView

internal const val ExamRuntimeHardeningLogTag = "ExamRuntimeHardening"

internal enum class ExamRuntimeRecoveryState {
    Idle,
    RendererGone,
    CleanupInFlight,
    ReadyToRetry
}

internal data class ExamRuntimeMemoryAction(
    val respond: Boolean,
    val clearWarmLocation: Boolean = false,
    val clearReverseEngineeringCache: Boolean = false,
    val clearIntegrityCache: Boolean = false,
    val clearUnusedFullscreenContainer: Boolean = false,
    val cleanupInactiveWebView: Boolean = false,
    val keepActiveWebView: Boolean = false,
    val clearActiveWebViewCache: Boolean = false
) {
    fun diagnosticActions(): List<String> {
        if (!respond) {
            return listOf("ignore_trim_level")
        }
        return buildList {
            if (clearWarmLocation) add("clear_warm_location")
            if (clearReverseEngineeringCache) add("clear_reverse_engineering_cache")
            if (clearIntegrityCache) add("clear_integrity_cache")
            if (clearUnusedFullscreenContainer) add("clear_unused_fullscreen_container")
            if (cleanupInactiveWebView) add("cleanup_inactive_webview")
            if (clearActiveWebViewCache) add("clear_active_webview_cache")
            if (keepActiveWebView) add("keep_active_webview")
        }
    }
}

internal data class ExamWebViewRendererPriorityPolicy(
    val rendererPriority: Int,
    val waivedWhenNotVisible: Boolean
)

@SuppressLint("InlinedApi")
internal fun resolveExamWebViewRendererPriorityPolicy(): ExamWebViewRendererPriorityPolicy =
    ExamWebViewRendererPriorityPolicy(
        rendererPriority = WebView.RENDERER_PRIORITY_IMPORTANT,
        waivedWhenNotVisible = false
    )

internal fun nextExamWebViewGeneration(currentGeneration: Long): Long =
    currentGeneration + 1L

internal fun isCurrentExamWebViewGeneration(
    callbackGeneration: Long,
    activeGeneration: Long
): Boolean = callbackGeneration == activeGeneration

internal fun shouldRunExamWebViewCleanup(
    activeGeneration: Long,
    destroyedGeneration: Long?
): Boolean = destroyedGeneration != activeGeneration

internal fun resolveExamRuntimeMemoryAction(
    shouldRespondToPressure: Boolean,
    examSessionStarted: Boolean,
    hasFullscreenCustomView: Boolean
): ExamRuntimeMemoryAction {
    if (!shouldRespondToPressure) {
        return ExamRuntimeMemoryAction(respond = false)
    }
    return ExamRuntimeMemoryAction(
        respond = true,
        clearWarmLocation = true,
        clearReverseEngineeringCache = true,
        clearIntegrityCache = true,
        clearUnusedFullscreenContainer = !hasFullscreenCustomView,
        cleanupInactiveWebView = !examSessionStarted,
        keepActiveWebView = examSessionStarted,
        clearActiveWebViewCache = false
    )
}

internal data class ExamRuntimeExitCleanupSnapshot(
    val requested: Boolean,
    val inFlight: Boolean
)

internal enum class ExamRuntimeExitCleanupDecision {
    StartCleanup,
    JoinInFlight,
    AlreadyCompleted
}

internal fun resolveExamRuntimeExitCleanupDecision(
    snapshot: ExamRuntimeExitCleanupSnapshot
): ExamRuntimeExitCleanupDecision {
    return when {
        snapshot.inFlight -> ExamRuntimeExitCleanupDecision.JoinInFlight
        snapshot.requested -> ExamRuntimeExitCleanupDecision.AlreadyCompleted
        else -> ExamRuntimeExitCleanupDecision.StartCleanup
    }
}

internal enum class ExamRefreshSafetyOutcome {
    SafeReloadOnly,
    PreparationOnly,
    BlockedPinningPending,
    BlockedPinningInactive
}

internal data class ExamRefreshSafetyDecision(
    val outcome: ExamRefreshSafetyOutcome,
    val allowWebViewReload: Boolean,
    val shouldRequestLockTask: Boolean,
    val eventCode: String,
    val diagnosticDetails: String
)

internal fun resolveExamRefreshSafetyDecision(
    examSessionStarted: Boolean,
    screenPinningEnforced: Boolean,
    lockTaskAlreadyActive: Boolean,
    lockTaskRequestPending: Boolean
): ExamRefreshSafetyDecision {
    return when {
        !examSessionStarted -> ExamRefreshSafetyDecision(
            outcome = ExamRefreshSafetyOutcome.PreparationOnly,
            allowWebViewReload = false,
            shouldRequestLockTask = false,
            eventCode = ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted,
            diagnosticDetails = "exam_started=false | reload=false | request_lock_task=false"
        )
        lockTaskRequestPending -> ExamRefreshSafetyDecision(
            outcome = ExamRefreshSafetyOutcome.BlockedPinningPending,
            allowWebViewReload = false,
            shouldRequestLockTask = false,
            eventCode = ExamRuntimeHardeningDiagnostics.ExamRefreshPinningPendingBlocked,
            diagnosticDetails = "exam_started=true | pinning_pending=true | reload=false | request_lock_task=false"
        )
        screenPinningEnforced && !lockTaskAlreadyActive -> ExamRefreshSafetyDecision(
            outcome = ExamRefreshSafetyOutcome.BlockedPinningInactive,
            allowWebViewReload = false,
            shouldRequestLockTask = false,
            eventCode = ExamRuntimeHardeningDiagnostics.ExamRefreshPinningInactiveBlocked,
            diagnosticDetails = "exam_started=true | lock_task_active=false | reload=false | request_lock_task=false"
        )
        else -> ExamRefreshSafetyDecision(
            outcome = ExamRefreshSafetyOutcome.SafeReloadOnly,
            allowWebViewReload = true,
            shouldRequestLockTask = false,
            eventCode = if (screenPinningEnforced && lockTaskAlreadyActive) {
                ExamRuntimeHardeningDiagnostics.ExamRefreshSafeLockTaskSkipped
            } else {
                ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted
            },
            diagnosticDetails = "exam_started=true | lock_task_active=$lockTaskAlreadyActive | " +
                "screen_pinning_enforced=$screenPinningEnforced | reload=true | request_lock_task=false"
        )
    }
}

internal object ExamRuntimeHardeningDiagnostics {
    const val WebViewRendererGone = "WEBVIEW_RENDERER_GONE"
    const val WebViewRendererGoneStale = "WEBVIEW_RENDERER_GONE_STALE"
    const val WebViewStaleCallbackIgnored = "WEBVIEW_STALE_CALLBACK_IGNORED"
    const val WebViewRecoveryReady = "WEBVIEW_RECOVERY_READY"
    const val WebViewExitCleanupStarted = "WEBVIEW_EXIT_CLEANUP_STARTED"
    const val WebViewExitCleanupSucceeded = "WEBVIEW_EXIT_CLEANUP_SUCCEEDED"
    const val WebViewExitCleanupTimeout = "WEBVIEW_EXIT_CLEANUP_TIMEOUT"
    const val WebViewExitCleanupFailed = "WEBVIEW_EXIT_CLEANUP_FAILED"
    const val WebViewExitCleanupJoined = "WEBVIEW_EXIT_CLEANUP_JOINED"
    const val WebViewExitCleanupSkipped = "WEBVIEW_EXIT_CLEANUP_SKIPPED"
    const val MemoryTrimHandled = "MEMORY_TRIM_HANDLED"
    const val DiagnosticExportRequested = "DIAGNOSTIC_EXPORT_REQUESTED"
    const val DiagnosticExportSucceeded = "DIAGNOSTIC_EXPORT_SUCCEEDED"
    const val DiagnosticExportFailed = "DIAGNOSTIC_EXPORT_FAILED"
    const val NetworkDnsProbeFailed = "NETWORK_DNS_PROBE_FAILED"
    const val NetworkCaptivePortalDetected = "NETWORK_CAPTIVE_PORTAL_DETECTED"
    const val NetworkVpnDetected = "NETWORK_VPN_DETECTED"
    const val NetworkVpnCleared = "NETWORK_VPN_CLEARED"
    const val VpnBypassTamperDetected = "VPN_BYPASS_TAMPER_DETECTED"
    const val VpnSettingsOpened = "VPN_SETTINGS_OPENED"
    const val StartExamBlockedVpn = "START_EXAM_BLOCKED_VPN"
    const val VendorChecklistOpened = "VENDOR_CHECKLIST_OPENED"
    const val OverlayTouchSuppressed = "OVERLAY_TOUCH_SUPPRESSED"
    const val OverlayTouchWarning = "OVERLAY_TOUCH_WARNING"
    const val ScreenPinningTransientLossRecheck = "SCREEN_PINNING_TRANSIENT_LOSS_RECHECK"
    const val ScreenPinningTransitionInterrupted = "SCREEN_PINNING_TRANSITION_INTERRUPTED"
    const val DeviceCompatProfileResolved = "DEVICE_COMPAT_PROFILE_RESOLVED"
    const val PreExamHealthCheckStarted = "PRE_EXAM_HEALTH_CHECK_STARTED"
    const val PreExamHealthCheckCompleted = "PRE_EXAM_HEALTH_CHECK_COMPLETED"
    const val ScreenPinningAlreadyActive = "SCREEN_PINNING_ALREADY_ACTIVE"
    const val ScreenPinningRequestSkippedAlreadyActive = "SCREEN_PINNING_REQUEST_SKIPPED_ALREADY_ACTIVE"
    const val SamsungLegacyProfileActive = "SAMSUNG_LEGACY_PROFILE_ACTIVE"
    const val PinningRefreshSafeSuppressed = "PINNING_REFRESH_SAFE_SUPPRESSED"
    const val OverlayPartialLegacyWarning = "OVERLAY_PARTIAL_LEGACY_WARNING"
    const val StartExamBlockedHealthCheck = "START_EXAM_BLOCKED_HEALTH_CHECK"
    const val StartExamBlockedScreenPinningInactive = "START_EXAM_BLOCKED_SCREEN_PINNING_INACTIVE"
    const val ScreenRecorderDetected = "SCREEN_RECORDER_DETECTED"
    const val ScreenRecorderCleared = "SCREEN_RECORDER_CLEARED"
    const val DisplayMirrorDetected = "DISPLAY_MIRROR_DETECTED"
    const val DisplayMirrorCleared = "DISPLAY_MIRROR_CLEARED"
    const val MultiWindowDetected = "MULTI_WINDOW_DETECTED"
    const val MultiWindowCleared = "MULTI_WINDOW_CLEARED"
    const val MultiWindowModeChanged = "MULTI_WINDOW_MODE_CHANGED"
    const val FieldReadinessTestStarted = "FIELD_READINESS_TEST_STARTED"
    const val FieldReadinessTestCompleted = "FIELD_READINESS_TEST_COMPLETED"
    const val DeviceSurvivalPolicyResolved = "DEVICE_SURVIVAL_POLICY_RESOLVED"
    const val CompatibilityScoreUpdated = "COMPATIBILITY_SCORE_UPDATED"
    const val PreparationAutoFixShown = "PREPARATION_AUTOFIX_SHOWN"
    const val PreparationAutoFixActionOpened = "PREPARATION_AUTOFIX_ACTION_OPENED"
    const val ScreenPinningDeferredUntilBlockersClear = "SCREEN_PINNING_DEFERRED_UNTIL_BLOCKERS_CLEAR"
    const val PreviousSessionBreadcrumbWritten = "PREVIOUS_SESSION_BREADCRUMB_WRITTEN"
    const val PreviousSessionRecoveryHintShown = "PREVIOUS_SESSION_RECOVERY_HINT_SHOWN"
    const val WebViewProviderHealthResolved = "WEBVIEW_PROVIDER_HEALTH_RESOLVED"
    const val WebViewProviderHealthFixOpened = "WEBVIEW_PROVIDER_HEALTH_FIX_OPENED"
    const val WebViewProviderHealthWarning = "WEBVIEW_PROVIDER_HEALTH_WARNING"
    const val FooterLayoutMode = "EXAM_FOOTER_LAYOUT_MODE"
    const val ExamRefreshRequested = "EXAM_REFRESH_REQUESTED"
    const val ExamRefreshSafeLockTaskSkipped = "EXAM_REFRESH_SAFE_LOCKTASK_SKIPPED"
    const val ExamRefreshPinningPendingBlocked = "EXAM_REFRESH_PINNING_PENDING_BLOCKED"
    const val ExamRefreshPinningInactiveBlocked = "EXAM_REFRESH_PINNING_INACTIVE_BLOCKED"
    const val ExamRefreshCompleted = "EXAM_REFRESH_COMPLETED"
    const val ExamRefreshStoppedByUser = "EXAM_REFRESH_STOPPED_BY_USER"
    const val PinningStartRequested = "PINNING_START_REQUESTED"
    const val PinningDialogExpected = "PINNING_DIALOG_EXPECTED"
    const val PinningWaitStarted = "PINNING_WAIT_STARTED"
    const val PinningActiveConfirmed = "PINNING_ACTIVE_CONFIRMED"
    const val PinningWaitTimeout = "PINNING_WAIT_TIMEOUT"
    const val PinningTransitionViolationSuppressed = "PINNING_TRANSITION_VIOLATION_SUPPRESSED"
    const val PinningRetryReady = "PINNING_RETRY_READY"
    const val DpcStatusResolved = "DPC_STATUS_RESOLVED"
    const val DpcLockTaskAllowlistApplied = "DPC_LOCK_TASK_ALLOWLIST_APPLIED"
    const val DpcCreateWindowsRestrictionApplied = "DPC_CREATE_WINDOWS_RESTRICTION_APPLIED"
    const val DpcCreateWindowsRestrictionUnsupported = "DPC_CREATE_WINDOWS_RESTRICTION_UNSUPPORTED"
    const val DpcCreateWindowsRestrictionCleared = "DPC_CREATE_WINDOWS_RESTRICTION_CLEARED"
    const val OverlayAppPermissionDetected = "OVERLAY_APP_PERMISSION_DETECTED"
    const val OverlayAppPermissionCleared = "OVERLAY_APP_PERMISSION_CLEARED"

    private val qaLogCodes = setOf(
        WebViewRendererGone,
        WebViewRendererGoneStale,
        WebViewStaleCallbackIgnored,
        WebViewRecoveryReady,
        WebViewExitCleanupStarted,
        WebViewExitCleanupSucceeded,
        WebViewExitCleanupTimeout,
        WebViewExitCleanupFailed,
        WebViewExitCleanupJoined,
        WebViewExitCleanupSkipped,
        MemoryTrimHandled,
        DiagnosticExportRequested,
        DiagnosticExportSucceeded,
        DiagnosticExportFailed,
        NetworkDnsProbeFailed,
        NetworkCaptivePortalDetected,
        NetworkVpnDetected,
        NetworkVpnCleared,
        VpnBypassTamperDetected,
        VpnSettingsOpened,
        StartExamBlockedVpn,
        VendorChecklistOpened,
        OverlayTouchSuppressed,
        OverlayTouchWarning,
        ScreenPinningTransientLossRecheck,
        ScreenPinningTransitionInterrupted,
        DeviceCompatProfileResolved,
        PreExamHealthCheckStarted,
        PreExamHealthCheckCompleted,
        ScreenPinningAlreadyActive,
        ScreenPinningRequestSkippedAlreadyActive,
        SamsungLegacyProfileActive,
        PinningRefreshSafeSuppressed,
        OverlayPartialLegacyWarning,
        StartExamBlockedHealthCheck,
        StartExamBlockedScreenPinningInactive,
        ScreenRecorderDetected,
        ScreenRecorderCleared,
        DisplayMirrorDetected,
        DisplayMirrorCleared,
        MultiWindowDetected,
        MultiWindowCleared,
        MultiWindowModeChanged,
        FieldReadinessTestStarted,
        FieldReadinessTestCompleted,
        DeviceSurvivalPolicyResolved,
        CompatibilityScoreUpdated,
        PreparationAutoFixShown,
        PreparationAutoFixActionOpened,
        ScreenPinningDeferredUntilBlockersClear,
        PreviousSessionBreadcrumbWritten,
        PreviousSessionRecoveryHintShown,
        WebViewProviderHealthResolved,
        WebViewProviderHealthFixOpened,
        WebViewProviderHealthWarning,
        FooterLayoutMode,
        ExamRefreshRequested,
        ExamRefreshSafeLockTaskSkipped,
        ExamRefreshPinningPendingBlocked,
        ExamRefreshPinningInactiveBlocked,
        ExamRefreshCompleted,
        ExamRefreshStoppedByUser,
        PinningStartRequested,
        PinningDialogExpected,
        PinningWaitStarted,
        PinningActiveConfirmed,
        PinningWaitTimeout,
        PinningTransitionViolationSuppressed,
        PinningRetryReady,
        DpcStatusResolved,
        DpcLockTaskAllowlistApplied,
        DpcCreateWindowsRestrictionApplied,
        DpcCreateWindowsRestrictionUnsupported,
        DpcCreateWindowsRestrictionCleared,
        OverlayAppPermissionDetected,
        OverlayAppPermissionCleared
    )

    fun shouldLogForQa(code: String): Boolean = code in qaLogCodes
}
