package com.example.coblaxexamlock.ui.exam

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
    val keepActiveWebView: Boolean = false
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
            if (keepActiveWebView) add("keep_active_webview")
        }
    }
}

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
        keepActiveWebView = examSessionStarted
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

internal object ExamRuntimeHardeningDiagnostics {
    const val WebViewRendererGone = "WEBVIEW_RENDERER_GONE"
    const val WebViewRecoveryReady = "WEBVIEW_RECOVERY_READY"
    const val WebViewExitCleanupStarted = "WEBVIEW_EXIT_CLEANUP_STARTED"
    const val WebViewExitCleanupSucceeded = "WEBVIEW_EXIT_CLEANUP_SUCCEEDED"
    const val WebViewExitCleanupTimeout = "WEBVIEW_EXIT_CLEANUP_TIMEOUT"
    const val WebViewExitCleanupFailed = "WEBVIEW_EXIT_CLEANUP_FAILED"
    const val WebViewExitCleanupJoined = "WEBVIEW_EXIT_CLEANUP_JOINED"
    const val WebViewExitCleanupSkipped = "WEBVIEW_EXIT_CLEANUP_SKIPPED"
    const val MemoryTrimHandled = "MEMORY_TRIM_HANDLED"

    private val qaLogCodes = setOf(
        WebViewRendererGone,
        WebViewRecoveryReady,
        WebViewExitCleanupStarted,
        WebViewExitCleanupSucceeded,
        WebViewExitCleanupTimeout,
        WebViewExitCleanupFailed,
        WebViewExitCleanupJoined,
        WebViewExitCleanupSkipped,
        MemoryTrimHandled
    )

    fun shouldLogForQa(code: String): Boolean = code in qaLogCodes
}
