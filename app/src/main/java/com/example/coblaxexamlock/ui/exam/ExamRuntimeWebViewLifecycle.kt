package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.detachExamKeyboardBridge
import com.example.coblaxexamlock.detachExamNativeFullscreenBridge
import com.example.coblaxexamlock.detachExamParticipantCaptureBridge
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.prepareForFreshExamSession
import com.example.coblaxexamlock.resolveRuntimePressureProfile
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import kotlinx.coroutines.CompletableDeferred

internal fun destroyExamWebViewInstance(view: SecureExamWebView) {
    runCatching { view.stopLoading() }
    runCatching { view.detachExamKeyboardBridge() }
    runCatching { view.detachExamParticipantCaptureBridge() }
    runCatching { view.detachExamNativeFullscreenBridge() }
    runCatching { view.prepareForFreshExamSession(clearHttpCache = false) }
    runCatching { view.webChromeClient = null }
    runCatching { view.webViewClient = WebViewClient() }
    runCatching { (view.parent as? ViewGroup)?.removeView(view) }
    runCatching { view.removeAllViews() }
    runCatching { view.destroy() }
}

internal fun recordExamWebViewStaleCallbackIgnored(
    callbackName: String,
    callbackView: WebView?,
    activeWebView: SecureExamWebView?,
    activeGeneration: Long,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    recordAction(
        ExamRuntimeHardeningDiagnostics.WebViewStaleCallbackIgnored,
        "callback=$callbackName | active_generation=$activeGeneration | " +
            "has_active=${if (activeWebView != null) "yes" else "no"} | " +
            "has_callback_view=${if (callbackView != null) "yes" else "no"}",
        DiagnosticEventLevel.WARNING
    )
}

internal fun showExamRuntimeCustomView(
    webViewUiState: ExamRuntimeWebViewUiState,
    lockTaskBridge: ActivityLockTaskBridge,
    view: android.view.View,
    callback: WebChromeClient.CustomViewCallback?
) {
    if (webViewUiState.fullScreenCustomView.value != null) {
        callback?.onCustomViewHidden()
        return
    }
    webViewUiState.fullScreenCustomView.value = view
    webViewUiState.fullScreenCustomViewCallback.value = callback
    webViewUiState.instance.value?.visibility = android.view.View.GONE
    (view.parent as? ViewGroup)?.removeView(view)
    webViewUiState.fullScreenContainer.removeAllViews()
    webViewUiState.fullScreenContainer.addView(
        view,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    lockTaskBridge.engage(allowLockTask = false)
}

internal fun hideExamRuntimeCustomView(
    webViewUiState: ExamRuntimeWebViewUiState,
    lockTaskBridge: ActivityLockTaskBridge
) {
    val view = webViewUiState.fullScreenCustomView.value ?: return
    webViewUiState.fullScreenContainer.removeView(view)
    webViewUiState.fullScreenCustomViewCallback.value?.onCustomViewHidden()
    webViewUiState.fullScreenCustomViewCallback.value = null
    webViewUiState.fullScreenCustomView.value = null
    webViewUiState.instance.value?.visibility = android.view.View.VISIBLE
    lockTaskBridge.engage(allowLockTask = false)
}

internal fun shouldIgnoreStaleExamWebViewCallback(
    webViewUiState: ExamRuntimeWebViewUiState,
    callbackName: String,
    view: WebView?,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
): Boolean {
    val activeWebView = webViewUiState.instance.value
    if (view == null || view === activeWebView) {
        return false
    }
    recordExamWebViewStaleCallbackIgnored(
        callbackName = callbackName,
        callbackView = view,
        activeWebView = activeWebView,
        activeGeneration = webViewUiState.generation.value,
        recordAction = recordAction
    )
    return true
}

internal fun cleanupActiveExamWebViewInstance(
    webViewUiState: ExamRuntimeWebViewUiState,
    clearEditableFocus: () -> Unit
) {
    webViewUiState.fullScreenCustomView.value?.let { view ->
        runCatching { webViewUiState.fullScreenContainer.removeView(view) }
        runCatching { webViewUiState.fullScreenCustomViewCallback.value?.onCustomViewHidden() }
    }
    webViewUiState.fullScreenCustomViewCallback.value = null
    webViewUiState.fullScreenCustomView.value = null
    clearEditableFocus()
    val activeWebView = webViewUiState.instance.value ?: return
    if (!shouldRunExamWebViewCleanup(
            activeGeneration = webViewUiState.generation.value,
            destroyedGeneration = webViewUiState.destroyedGeneration.value
        )
    ) {
        webViewUiState.instance.value = null
        return
    }
    webViewUiState.destroyedGeneration.value = webViewUiState.generation.value
    webViewUiState.instance.value = null
    destroyExamWebViewInstance(activeWebView)
}

internal suspend fun clearExamRuntimeSessionOnExit(
    context: Context,
    lowRamProfile: LowRamProfile,
    webViewUiState: ExamRuntimeWebViewUiState,
    reason: String,
    waitForResult: Boolean,
    exitSessionClearRequested: Boolean,
    exitSessionClearInFlight: Boolean,
    exitSessionClearDeferred: CompletableDeferred<Result<Unit>>?,
    setExitSessionClearRequested: (Boolean) -> Unit,
    setExitSessionClearInFlight: (Boolean) -> Unit,
    setExitSessionClearDeferred: (CompletableDeferred<Result<Unit>>?) -> Unit,
    setRecoveryState: (ExamRuntimeRecoveryState) -> Unit,
    cleanupActiveWebViewInstance: () -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
): Result<Unit> {
    val requestDetails = "reason=$reason | wait=${if (waitForResult) "yes" else "no"}"
    when (
        resolveExamRuntimeExitCleanupDecision(
            ExamRuntimeExitCleanupSnapshot(
                requested = exitSessionClearRequested,
                inFlight = exitSessionClearInFlight
            )
        )
    ) {
        ExamRuntimeExitCleanupDecision.JoinInFlight -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.WebViewExitCleanupJoined,
                requestDetails,
                DiagnosticEventLevel.INFO
            )
            return if (waitForResult) {
                exitSessionClearDeferred?.await() ?: Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        }

        ExamRuntimeExitCleanupDecision.AlreadyCompleted -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.WebViewExitCleanupSkipped,
                requestDetails,
                DiagnosticEventLevel.INFO
            )
            return Result.success(Unit)
        }

        ExamRuntimeExitCleanupDecision.StartCleanup -> Unit
    }

    val cleanupCompletion = CompletableDeferred<Result<Unit>>()
    setExitSessionClearDeferred(cleanupCompletion)
    setExitSessionClearRequested(true)
    setExitSessionClearInFlight(true)
    setRecoveryState(ExamRuntimeRecoveryState.CleanupInFlight)

    val existingWebView = if (waitForResult) webViewUiState.instance.value else null
    val details = buildString {
        append(requestDetails)
        append(" | webview=")
        append(if (existingWebView != null) "present" else "none")
    }
    recordAction(
        ExamRuntimeHardeningDiagnostics.WebViewExitCleanupStarted,
        details,
        DiagnosticEventLevel.INFO
    )

    val clearResult = try {
        clearExamWebViewSessionData(
            context = context.applicationContext,
            existingWebView = existingWebView,
            lowRamProfile = lowRamProfile
        )
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

    if (waitForResult) {
        cleanupActiveWebViewInstance()
    }
    setExitSessionClearInFlight(false)
    setExitSessionClearDeferred(null)
    setRecoveryState(ExamRuntimeRecoveryState.Idle)

    if (clearResult.isSuccess) {
        recordAction(
            ExamRuntimeHardeningDiagnostics.WebViewExitCleanupSucceeded,
            details,
            DiagnosticEventLevel.INFO
        )
    } else {
        val error = clearResult.exceptionOrNull()
        val errorSummary = error?.message?.take(160)
            ?: error?.javaClass?.simpleName?.take(160)
            ?: "unknown"
        val failureCode =
            if (errorSummary.contains("Timed out", ignoreCase = true)) {
                ExamRuntimeHardeningDiagnostics.WebViewExitCleanupTimeout
            } else {
                ExamRuntimeHardeningDiagnostics.WebViewExitCleanupFailed
            }
        recordAction(
            failureCode,
            "$details | error=$errorSummary",
            DiagnosticEventLevel.ERROR
        )
    }
    cleanupCompletion.complete(clearResult)

    return clearResult
}

internal class ExamRuntimeRendererGoneCallbacks(
    val cleanupActiveWebViewInstance: () -> Unit,
    val disarmExamRuntimeMonitoring: () -> Unit,
    val clearAppSwitchSuppression: () -> Unit,
    val setLockTaskRequestPending: (Boolean) -> Unit,
    val setExamSessionStarted: (Boolean) -> Unit,
    val setExamSessionStartedAtElapsedMs: (Long?) -> Unit,
    val setShowBuiltInExamKeyboard: (Boolean) -> Unit,
    val setHasEditableFocus: (Boolean) -> Unit,
    val setWebViewErrorMessage: (String?) -> Unit,
    val setWebViewSessionResetInFlight: (Boolean) -> Unit,
    val setWebViewSessionResetError: (String?) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun handleExamRuntimeWebViewRendererGone(
    webViewUiState: ExamRuntimeWebViewUiState,
    lowRamProfile: LowRamProfile,
    uiLanguage: UiLanguage,
    mainActivity: MainActivity?,
    lockTaskBridge: ActivityLockTaskBridge,
    examAlarmController: ExamAlarmController,
    callbacks: ExamRuntimeRendererGoneCallbacks,
    view: SecureExamWebView?,
    didCrash: Boolean,
    rendererPriorityAtExit: Int?
): Boolean {
    val details = buildString {
        append("did_crash=")
        append(if (didCrash) "yes" else "no")
        append(" | priority_at_exit=")
        append(rendererPriorityAtExit ?: "-")
        append(" | active_generation=")
        append(webViewUiState.generation.value)
        append(" | low_ram=")
        append(if (lowRamProfile.enabled) "yes" else "no")
        append(" | severe=")
        append(if (lowRamProfile.severe) "yes" else "no")
        append(" | ultra=")
        append(if (lowRamProfile.ultra) "yes" else "no")
        append(" | recovery=manual_safe")
    }
    if (view != null && view !== webViewUiState.instance.value) {
        callbacks.recordAction(
            ExamRuntimeHardeningDiagnostics.WebViewRendererGoneStale,
            details,
            DiagnosticEventLevel.WARNING
        )
        destroyExamWebViewInstance(view)
        return true
    }
    webViewUiState.recoveryState.value = ExamRuntimeRecoveryState.RendererGone
    callbacks.recordAction(
        ExamRuntimeHardeningDiagnostics.WebViewRendererGone,
        details,
        DiagnosticEventLevel.ERROR
    )
    webViewUiState.recoveryState.value = ExamRuntimeRecoveryState.CleanupInFlight
    callbacks.cleanupActiveWebViewInstance()
    mainActivity?.setExamLockMode(enabled = false, allowLockTask = false)
    lockTaskBridge.disengage()
    callbacks.disarmExamRuntimeMonitoring()
    callbacks.clearAppSwitchSuppression()
    examAlarmController.stop()
    callbacks.setLockTaskRequestPending(false)
    callbacks.setExamSessionStarted(false)
    callbacks.setExamSessionStartedAtElapsedMs(null)
    callbacks.setShowBuiltInExamKeyboard(false)
    callbacks.setHasEditableFocus(false)
    webViewUiState.loadingProgress.floatValue = 0f
    callbacks.setWebViewErrorMessage(null)
    callbacks.setWebViewSessionResetInFlight(false)
    callbacks.setWebViewSessionResetError(
        localized(
            uiLanguage,
            "The exam page stopped and was closed safely. Press Start Exam Mode again to reopen a clean session.",
            "Halaman ujian berhenti dan sudah ditutup dengan aman. Tekan Mulai Ujian lagi untuk membuka sesi bersih."
        )
    )
    webViewUiState.recoveryState.value = ExamRuntimeRecoveryState.ReadyToRetry
    callbacks.recordAction(
        ExamRuntimeHardeningDiagnostics.WebViewRecoveryReady,
        details,
        DiagnosticEventLevel.WARNING
    )
    return true
}

internal class ExamRuntimeMemoryTrimCallbacks(
    val clearWarmLocation: () -> Unit,
    val clearReverseEngineeringCache: () -> Unit,
    val clearIntegrityCache: () -> Unit,
    val cleanupActiveWebViewInstance: () -> Unit,
    val getDiagnosticEvents: () -> List<DiagnosticEvent>,
    val setDiagnosticEvents: (List<DiagnosticEvent>) -> Unit,
    val setLastRuntimeMemoryActionSummary: (String) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun handleExamRuntimeTrimMemory(
    level: Int,
    lowRamProfile: LowRamProfile,
    examSessionStarted: Boolean,
    webViewUiState: ExamRuntimeWebViewUiState,
    networkFlapElapsedMs: MutableList<Long>,
    networkTimeline: MutableList<NetworkTimelineEntry>,
    callbacks: ExamRuntimeMemoryTrimCallbacks
) {
    val memoryAction = resolveExamRuntimeMemoryAction(
        shouldRespondToPressure = MemoryPressureCoordinator.shouldRespondToPressure(level),
        examSessionStarted = examSessionStarted,
        hasFullscreenCustomView = webViewUiState.fullScreenCustomView.value != null,
        clearActiveWebViewCacheAllowed = MemoryPressureCoordinator.shouldClearActiveWebViewCache(level)
    )
    if (!memoryAction.respond) {
        return
    }
    val escalatedProfile = resolveRuntimePressureProfile(
        baseProfile = lowRamProfile,
        trimLevel = level
    )
    if (escalatedProfile.ultra && !lowRamProfile.ultra) {
        SecurityDetectorCache.invalidateStaticSecurity()
        callbacks.recordAction(
            "LOW_RAM_RUNTIME_ESCALATED",
            "trim_level=$level | runtime_profile=Ultra | " +
                "screen_pinning_poll_ms=${escalatedProfile.screenPinningSteadyPollMillis} | " +
                "accessibility_poll_ms=${escalatedProfile.accessibilityLivenessPollMillis} | " +
                "server_probe_ms=${escalatedProfile.examServerProbeIntervalMillis} | " +
                "detector_cache_max=${escalatedProfile.detectorMetadataCacheMaxEntries}",
            DiagnosticEventLevel.WARNING
        )
    }

    if (memoryAction.clearWarmLocation) {
        callbacks.clearWarmLocation()
    }
    if (memoryAction.clearReverseEngineeringCache) {
        callbacks.clearReverseEngineeringCache()
    }
    if (memoryAction.clearIntegrityCache) {
        callbacks.clearIntegrityCache()
    }
    if (memoryAction.clearUnusedFullscreenContainer) {
        runCatching { webViewUiState.fullScreenContainer.removeAllViews() }
    }
    if (memoryAction.cleanupInactiveWebView) {
        callbacks.cleanupActiveWebViewInstance()
    }
    if (memoryAction.clearActiveWebViewCache) {
        runCatching { webViewUiState.instance.value?.clearCache(false) }
    }

    val isCritical = MemoryPressureCoordinator.shouldClearActiveWebViewCache(level)
    if (isCritical || lowRamProfile.ultra) {
        networkFlapElapsedMs.clear()
    }
    if (isCritical) {
        SecurityDetectorCache.invalidateAll()
        val maxEvents = lowRamProfile.diagnosticLogMaxEntries
        val truncateTarget = maxEvents / 2
        val diagnosticEvents = callbacks.getDiagnosticEvents()
        if (diagnosticEvents.size > truncateTarget) {
            callbacks.setDiagnosticEvents(diagnosticEvents.take(truncateTarget))
        }
        while (networkTimeline.size > 3) {
            networkTimeline.removeAt(0)
        }
    } else {
        SecurityDetectorCache.invalidateStaticSecurity()
    }

    val actions = memoryAction.diagnosticActions().joinToString(",")
    val escalation = if (isCritical) "critical" else "standard"
    val details = "trim_level=$level | escalation=$escalation | exam_started=$examSessionStarted | " +
        "low_ram=${lowRamProfile.enabled} | severe=${lowRamProfile.severe} | " +
        "ultra=${lowRamProfile.ultra} | actions=$actions"
    callbacks.setLastRuntimeMemoryActionSummary(details.take(240))
    callbacks.recordAction(
        ExamRuntimeHardeningDiagnostics.MemoryTrimHandled,
        details,
        DiagnosticEventLevel.INFO
    )
    Log.i(RuntimeMemoryPerfTag, details)
}
