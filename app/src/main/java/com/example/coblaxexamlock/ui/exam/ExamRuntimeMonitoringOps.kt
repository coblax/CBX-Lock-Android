package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.lifecycle.lifecycleScope
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.ClipboardSnapshot
import com.example.coblaxexamlock.FatalSecuritySignal
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.config.AlarmAcknowledgeDedupWindowMillis
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.buildIntegrityPublicSummary
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.readClipboardSnapshotFull
import com.example.coblaxexamlock.readClipboardSnapshotLite
import com.example.coblaxexamlock.runtime.sendTelegramAlarmAcknowledge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ExamRuntimeExitCleanupStateAccess(
    val requested: MutableState<Boolean>,
    val deferred: MutableState<CompletableDeferred<Result<Unit>>?>
)

internal class ExamRuntimeMonitoringCallbacks(
    val currentAppSwitchEventDetails: (AppSwitchSignal) -> String,
    val clearAppSwitchSuppression: () -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal class ExamRuntimeMonitoringOps(
    private val context: Context,
    private val componentActivity: ComponentActivity,
    private val coroutineScope: CoroutineScope,
    private val lockTaskBridge: ActivityLockTaskBridge,
    private val lowRamProfile: LowRamProfile,
    private val screenPinningMode: ScreenPinningMode,
    private val uiLanguage: com.example.coblaxexamlock.model.UiLanguage,
    private val mainActivity: MainActivity?,
    private val examAlarmController: ExamAlarmController,
    private val webViewUiState: ExamRuntimeWebViewUiState,
    private val runtimeCacheState: ExamRuntimeRuntimeCacheState,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val clipboardUiState: ExamRuntimeClipboardUiState,
    private val accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    private val clipboardBypassState: ClipboardBypassState,
    private val bypassClipboard: Boolean,
    private val clipboardMainHandler: Handler,
    private val overlayMainHandler: Handler,
    private val networkFlapElapsedMs: MutableList<Long>,
    private val networkTimeline: MutableList<NetworkTimelineEntry>,
    private val locationWarmupUiState: ExamRuntimeLocationWarmupUiState,
    private val exitCleanupState: ExamRuntimeExitCleanupStateAccess,
    private val callbacks: ExamRuntimeMonitoringCallbacks
) {
    fun armExamRuntimeMonitoring(reason: String) {
        adminUiState.examRuntimeMonitoringArmed.value = true
        callbacks.recordAction(
            "EXAM_RUNTIME_GUARDS_ARMED",
            "reason=$reason | screen_pinning_mode=${screenPinningMode.name.lowercase()}",
            DiagnosticEventLevel.INFO
        )
    }

    fun disarmExamRuntimeMonitoring() {
        adminUiState.examRuntimeMonitoringArmed.value = false
        if (accessibilityGuardFallbackActiveState.value || AccessibilityExamGuardStore.snapshot(context).armed) {
            AccessibilityExamGuardStore.disarm(context)
            callbacks.recordAction(
                "ACCESSIBILITY_GUARD_DISARMED",
                "reason=runtime_monitoring_disarmed",
                DiagnosticEventLevel.INFO
            )
        }
        accessibilityGuardFallbackActiveState.value = false
        adminUiState.appSwitchLifecycleResumePending.value = false
        clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
        clipboardUiState.clipboardResumeCheckRunnable.value = null
        securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
        securityUiState.overlayFocusLossConfirmRunnable.value = null
        securityUiState.overlayWindowFocusLossPending.value = false
        clipboardUiState.clipboardResumeCheckPending.value = false
        clipboardUiState.clipboardPreBackgroundFingerprint.value = null
        clipboardUiState.clipboardPreBackgroundSignature.value = null
        clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
        adminUiState.participantContext.value = null
    }

    fun recordAppSwitchEvent(
        code: String,
        signal: AppSwitchSignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        updateLastDetectedAt: Boolean = true
    ) {
        val details = callbacks.currentAppSwitchEventDetails(signal)
        adminUiState.lastAppSwitchTrigger.value = signal.diagnosticLabel()
        if (updateLastDetectedAt) {
            adminUiState.lastAppSwitchAt.value = diagnosticTimestamp()
        }
        adminUiState.lastAppSwitchContext.value = details
        callbacks.recordAction(code, details, level)
    }

    fun acknowledgeRuntimeAlarm(
        type: AlarmAcknowledgeType,
        violationCount: Int,
        buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
        onUiAcknowledge: () -> Unit
    ) {
        val detailRef = latestAlarmDetailRef(
            diagnosticEvents = adminUiState.diagnosticEvents.value,
            type = type
        )
        val alarmPayload = buildPayload(detailRef)
        onUiAcknowledge()

        if (!examGuardArmed() && !flowUiState.examSessionStarted.value) {
            return
        }

        val dedupeKey = listOf(
            alarmPayload.alarmType.wireName,
            violationCount.toString(),
            alarmPayload.examName,
            alarmPayload.examUrlHost,
            alarmPayload.examUrlHashShort
        ).joinToString("|")
        val nowElapsedMs = SystemClock.elapsedRealtime()
        if (
            adminUiState.lastAlarmAcknowledgeDedupKey.value == dedupeKey &&
            nowElapsedMs - adminUiState.lastAlarmAcknowledgeAtElapsedMs.longValue <=
            AlarmAcknowledgeDedupWindowMillis
        ) {
            return
        }

        adminUiState.lastAlarmAcknowledgeDedupKey.value = dedupeKey
        adminUiState.lastAlarmAcknowledgeAtElapsedMs.longValue = nowElapsedMs
        callbacks.recordAction(
            "ALARM_ACKNOWLEDGED",
            buildAlarmAckEventDetails(payload = alarmPayload, result = "queued"),
            DiagnosticEventLevel.INFO
        )

        coroutineScope.launch {
            sendTelegramAlarmAcknowledge(alarmPayload)
                .onSuccess {
                    callbacks.recordAction(
                        "ALARM_ACK_TG_SENT",
                        buildAlarmAckEventDetails(payload = alarmPayload, result = "sent"),
                        DiagnosticEventLevel.INFO
                    )
                }
                .onFailure { error ->
                    val errorSummary = error.message?.take(160)
                        ?: error.javaClass.simpleName.take(160)
                    callbacks.recordAction(
                        "ALARM_ACK_TG_FAILED",
                        buildAlarmAckEventDetails(
                            payload = alarmPayload,
                            result = "failed",
                            extra = "error=$errorSummary"
                        ),
                        DiagnosticEventLevel.ERROR
                    )
                }
        }
    }

    fun confirmClipboardViolation(
        snapshot: ClipboardSnapshot,
        decision: ClipboardChangeDecision,
        eventSuffix: String,
        updateObservedSnapshot: Boolean,
        baselineSemanticSignatureOverride: String? = null
    ) {
        val eventTimestamp = diagnosticTimestamp()
        val baselineSemanticSignature =
            baselineSemanticSignatureOverride ?: clipboardUiState.clipboardDecisionSemanticSignature.value
        val diagnosticSnapshot =
            if (snapshot.rawSignature.isBlank()) readClipboardSnapshotFull(context) else snapshot
        clipboardUiState.clipboardSignature.value = diagnosticSnapshot.rawSignature
        clipboardUiState.clipboardDecisionFingerprint.value = diagnosticSnapshot.decisionFingerprint
        clipboardUiState.clipboardDecisionSemanticSignature.value = diagnosticSnapshot.semanticSignature
        if (updateObservedSnapshot) {
            clipboardUiState.lastClipboardObservedAt.value = eventTimestamp
            clipboardUiState.lastClipboardObservedSignature.value = diagnosticSnapshot.rawSignature.ifBlank { null }
        }
        clipboardUiState.lastClipboardBaselineSemanticSignature.value = baselineSemanticSignature.ifBlank { null }
        clipboardUiState.lastClipboardDetectedSemanticSignature.value =
            diagnosticSnapshot.semanticSignature.ifBlank { null }
        clipboardUiState.lastClipboardConfirmedAt.value = eventTimestamp
        clipboardUiState.lastClipboardDecision.value = decision.diagnosticLabel()
        callbacks.recordAction(
            "CLIPBOARD_CHANGED",
            "decision=${decision.diagnosticLabel()};source=$eventSuffix",
            DiagnosticEventLevel.SECURITY
        )
        clipboardUiState.lastClipboardChangeEvent.value =
            "$eventTimestamp - Clipboard berubah saat sesi ujian ($eventSuffix)"
        clipboardUiState.clipboardViolationCount.intValue += 1
        clipboardUiState.showClipboardViolationDialog.value = true
        examAlarmController.start()
    }

    fun armClipboardResumeCheck(reason: String) {
        if (clipboardBypassState == ClipboardBypassState.Active || bypassClipboard) {
            clipboardUiState.clipboardResumeCheckPending.value = false
            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
            clipboardUiState.clipboardPreBackgroundSignature.value = null
            clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
            return
        }
        val beforeBackgroundSnapshot = readClipboardSnapshotLite(context)
        clipboardUiState.clipboardPreBackgroundFingerprint.value = beforeBackgroundSnapshot.decisionFingerprint
        clipboardUiState.clipboardPreBackgroundSignature.value = null
        clipboardUiState.clipboardPreBackgroundSemanticSignature.value =
            beforeBackgroundSnapshot.semanticSignature.ifBlank { null }
        clipboardUiState.clipboardResumeCheckPending.value = true
        clipboardUiState.lastClipboardDecision.value = "resume_check_armed:$reason"
    }

    fun applyFatalSecuritySignal(signal: FatalSecuritySignal) {
        callbacks.recordAction(signal.eventCode, signal.details, DiagnosticEventLevel.SECURITY)
        adminUiState.examSessionCancelledByPinningFailure.value = true
        flowUiState.lockTaskRequestPending.value = false
        callbacks.clearAppSwitchSuppression()
        disarmExamRuntimeMonitoring()
        securityUiState.pendingForcedExitViolation.value = false
        securityUiState.showForcedExitAlarm.value = false
        flowUiState.screenPinningMessage.value = null
        flowUiState.showBuiltInExamKeyboard.value = false
        flowUiState.hasEditableFocus.value = false
        adminUiState.securityIssueDialogTitle.value = signal.title
        adminUiState.securityIssueDialogMessage.value = signal.message
        adminUiState.exitOnSecurityIssueDialogDismiss.value = true
        flowUiState.examSessionStarted.value = false
        adminUiState.examSessionStartedAtElapsedMs.value = null
        flowUiState.webViewErrorMessage.value = null
        lockTaskBridge.disengage()
        examAlarmController.start()
    }

    fun refreshReverseEngineeringStatus() {
        val cachedResult = runtimeCacheState.reverseEngineeringRefreshCache.value
        val result =
            if (cachedResult != null && cachedResult.isFresh()) {
                cachedResult.result
            } else {
                ReverseEngineeringGuard.inspect(context).also { refreshed ->
                    runtimeCacheState.reverseEngineeringRefreshCache.value =
                        RuntimeReverseEngineeringRefreshCache(
                            result = refreshed,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                }
            }
        securityUiState.tamperDetected.value = result.tamperDetected
        securityUiState.tamperSummary.value = result.summary()
        if (
            result.tamperDetected &&
            securityUiState.tamperSummary.value != securityUiState.tamperLastLoggedSummary.value
        ) {
            callbacks.recordAction(
                "TAMPER_DETECTED",
                securityUiState.tamperSummary.value,
                DiagnosticEventLevel.SECURITY
            )
            securityUiState.tamperLastLoggedSummary.value = securityUiState.tamperSummary.value
        }
        if (!result.tamperDetected && securityUiState.tamperLastLoggedSummary.value != null) {
            securityUiState.tamperLastLoggedSummary.value = null
        }
    }

    fun refreshIntegrityGuard() {
        val cachedResult = runtimeCacheState.integrityRefreshCache.value
        val result =
            if (
                cachedResult != null &&
                cachedResult.isFreshFor(securityUiState.integrityBaselineFingerprint.value)
            ) {
                cachedResult.result
            } else {
                IntegrityGuard.check(
                    context,
                    securityUiState.integrityBaselineFingerprint.value
                ).also { refreshed ->
                    runtimeCacheState.integrityRefreshCache.value =
                        RuntimeIntegrityRefreshCache(
                            result = refreshed,
                            baselineFingerprint = securityUiState.integrityBaselineFingerprint.value,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                }
            }
        if (
            securityUiState.integrityBaselineFingerprint.value.isNullOrBlank() &&
            result.currentFingerprint.isNotBlank() &&
            result.currentFingerprint != "-"
        ) {
            securityUiState.integrityBaselineFingerprint.value = result.currentFingerprint
        }
        securityUiState.integrityTamperDetected.value = !result.ok
        securityUiState.integritySummary.value = result.details
        securityUiState.integrityPublicSummary.value = buildIntegrityPublicSummary(result.issues)
        val issueSignature = securityUiState.integritySummary.value.ifBlank { "-" }
        if (!result.ok && issueSignature != securityUiState.integrityLastLoggedSummary.value) {
            val issueSet = result.issues.toSet()
            if ("dex_hash_mismatch" in issueSet) {
                callbacks.recordAction(
                    "TAMPER_APK_HASH",
                    securityUiState.integritySummary.value,
                    DiagnosticEventLevel.SECURITY
                )
            }
            if ("signature_changed" in issueSet) {
                callbacks.recordAction(
                    "TAMPER_SIGNATURE_CHANGED",
                    securityUiState.integritySummary.value,
                    DiagnosticEventLevel.SECURITY
                )
            }
            if (issueSet.any { it.startsWith("sysprop_") } || "test_keys" in issueSet) {
                callbacks.recordAction(
                    "TAMPER_SYSTEM_PROP",
                    securityUiState.integritySummary.value,
                    DiagnosticEventLevel.SECURITY
                )
            }
            if ("hook_class" in issueSet) {
                callbacks.recordAction(
                    "TAMPER_HOOK_CLASS",
                    securityUiState.integritySummary.value,
                    DiagnosticEventLevel.SECURITY
                )
            }
            securityUiState.integrityLastLoggedSummary.value = issueSignature
        }
        if (result.ok && securityUiState.integrityLastLoggedSummary.value != null) {
            securityUiState.integrityLastLoggedSummary.value = null
        }
    }

    fun hideSystemKeyboard() {
        val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        webViewUiState.instance.value?.windowToken?.let { windowToken ->
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }
        componentActivity.currentFocus?.windowToken?.let { windowToken ->
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    fun showSystemKeyboard() {
        if (flowUiState.useBuiltInExamKeyboard.value) {
            return
        }
        val inputMethodManager = context.getSystemService(InputMethodManager::class.java) ?: return
        webViewUiState.instance.value?.post {
            webViewUiState.instance.value?.isFocusable = true
            webViewUiState.instance.value?.isFocusableInTouchMode = true
            webViewUiState.instance.value?.requestFocus(View.FOCUS_DOWN)
            webViewUiState.instance.value?.requestFocus()
            webViewUiState.instance.value?.let {
                inputMethodManager.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback?) {
        showExamRuntimeCustomView(
            webViewUiState = webViewUiState,
            lockTaskBridge = lockTaskBridge,
            view = view,
            callback = callback
        )
    }

    fun hideCustomView() {
        hideExamRuntimeCustomView(
            webViewUiState = webViewUiState,
            lockTaskBridge = lockTaskBridge
        )
    }

    fun shouldIgnoreStaleWebViewCallback(callbackName: String, view: WebView?): Boolean =
        shouldIgnoreStaleExamWebViewCallback(
            webViewUiState = webViewUiState,
            callbackName = callbackName,
            view = view,
            recordAction = callbacks.recordAction
        )

    fun cleanupActiveExamWebViewInstance() {
        cleanupActiveExamWebViewInstance(
            webViewUiState = webViewUiState,
            clearEditableFocus = { flowUiState.hasEditableFocus.value = false }
        )
    }

    suspend fun clearExamSessionOnExit(
        reason: String,
        waitForResult: Boolean
    ): Result<Unit> =
        clearExamRuntimeSessionOnExit(
            context = context,
            lowRamProfile = lowRamProfile,
            webViewUiState = webViewUiState,
            reason = reason,
            waitForResult = waitForResult,
            exitSessionClearRequested = exitCleanupState.requested.value,
            exitSessionClearInFlight = flowUiState.exitSessionClearInFlight.value,
            exitSessionClearDeferred = exitCleanupState.deferred.value,
            setExitSessionClearRequested = { exitCleanupState.requested.value = it },
            setExitSessionClearInFlight = { flowUiState.exitSessionClearInFlight.value = it },
            setExitSessionClearDeferred = { exitCleanupState.deferred.value = it },
            setRecoveryState = { webViewUiState.recoveryState.value = it },
            cleanupActiveWebViewInstance = ::cleanupActiveExamWebViewInstance,
            recordAction = callbacks.recordAction
        )

    fun launchExitSessionClearBestEffort(reason: String) {
        componentActivity.lifecycleScope.launch {
            clearExamSessionOnExit(
                reason = reason,
                waitForResult = false
            )
        }
    }

    fun handleWebViewRendererGone(
        view: SecureExamWebView?,
        didCrash: Boolean,
        rendererPriorityAtExit: Int?
    ): Boolean =
        handleExamRuntimeWebViewRendererGone(
            webViewUiState = webViewUiState,
            lowRamProfile = lowRamProfile,
            uiLanguage = uiLanguage,
            mainActivity = mainActivity,
            lockTaskBridge = lockTaskBridge,
            examAlarmController = examAlarmController,
            callbacks = ExamRuntimeRendererGoneCallbacks(
                cleanupActiveWebViewInstance = ::cleanupActiveExamWebViewInstance,
                disarmExamRuntimeMonitoring = ::disarmExamRuntimeMonitoring,
                clearAppSwitchSuppression = callbacks.clearAppSwitchSuppression,
                setLockTaskRequestPending = { flowUiState.lockTaskRequestPending.value = it },
                setExamSessionStarted = { flowUiState.examSessionStarted.value = it },
                setExamSessionStartedAtElapsedMs = { adminUiState.examSessionStartedAtElapsedMs.value = it },
                setShowBuiltInExamKeyboard = { flowUiState.showBuiltInExamKeyboard.value = it },
                setHasEditableFocus = { flowUiState.hasEditableFocus.value = it },
                setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
                setWebViewSessionResetInFlight = { flowUiState.webViewSessionResetInFlight.value = it },
                setWebViewSessionResetError = { flowUiState.webViewSessionResetError.value = it },
                recordAction = callbacks.recordAction
            ),
            view = view,
            didCrash = didCrash,
            rendererPriorityAtExit = rendererPriorityAtExit
        )

    fun handleRuntimeTrimMemory(level: Int) {
        handleExamRuntimeTrimMemory(
            level = level,
            lowRamProfile = lowRamProfile,
            examSessionStarted = flowUiState.examSessionStarted.value,
            webViewUiState = webViewUiState,
            networkFlapElapsedMs = networkFlapElapsedMs,
            networkTimeline = networkTimeline,
            callbacks = ExamRuntimeMemoryTrimCallbacks(
                clearWarmLocation = { locationWarmupUiState.reusableWarmLocationValidation.value = null },
                clearReverseEngineeringCache = { runtimeCacheState.reverseEngineeringRefreshCache.value = null },
                clearIntegrityCache = { runtimeCacheState.integrityRefreshCache.value = null },
                cleanupActiveWebViewInstance = ::cleanupActiveExamWebViewInstance,
                getDiagnosticEvents = { adminUiState.diagnosticEvents.value },
                setDiagnosticEvents = { adminUiState.diagnosticEvents.value = it },
                setLastRuntimeMemoryActionSummary = {
                    runtimeCacheState.lastRuntimeMemoryActionSummary.value = it
                },
                recordAction = callbacks.recordAction
            )
        )
    }

    private fun examGuardArmed(): Boolean =
        adminUiState.examRuntimeMonitoringArmed.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.examSessionStarted.value
}
