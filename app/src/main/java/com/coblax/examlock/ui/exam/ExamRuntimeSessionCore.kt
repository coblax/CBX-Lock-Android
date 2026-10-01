package com.coblax.examlock.ui.exam

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.coblax.examlock.ActivityLockTaskBridge
import com.coblax.examlock.AppSwitchSignal
import com.coblax.examlock.AppSwitchSuppressionReason
import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DpcRuntimeStatus
import com.coblax.examlock.ExamDeviceOwnerController
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.GeofenceSecurityStatus
import com.coblax.examlock.LocationSpoofSecurityStatus
import com.coblax.examlock.LockTaskSecurityRequirement
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.MainActivity
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.OverlaySignal
import com.coblax.examlock.SplitLocationSecurityStatus
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.config.AppSwitchSuppressionWindowMillis
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkTimelineEntry
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.resolveLockTaskSecurityRequirement
import com.coblax.examlock.shouldRestartLockTaskForRequirement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The exam session's shared state and the helpers that used to be local functions of
 * the session composable. Sections of the session receive this one object instead of
 * capturing dozens of locals, which is what had grown that composable past the size the
 * Android runtime compiles cheaply. It is rebuilt on every composition, like the other
 * runtime ops classes, so the plain values it carries are that composition's snapshot.
 */
internal class ExamRuntimeSessionCore(
    val context: Context,
    val activity: Activity?,
    val componentActivity: ComponentActivity,
    val mainActivity: MainActivity?,
    val lockTaskBridge: ActivityLockTaskBridge,
    val coroutineScope: CoroutineScope,
    val examExceptionHandler: CoroutineExceptionHandler,
    val examAlarmController: ExamAlarmController,
    val payload: ExamQrPayload,
    val adminSettings: AdminSettings,
    val bypass: ExamRuntimeBypassContext,
    val uiLanguage: UiLanguage,
    val lowRamProfile: LowRamProfile,
    val deviceCompatibilityProfile: DeviceCompatibilityProfile,
    val deviceQuirkProfile: ExamRuntimeDeviceQuirkProfile,
    val deviceTimeBaseline: DeviceTimeBaseline,
    val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    val webViewCompatibilityRefreshKeyState: MutableIntState,
    val webViewUiState: ExamRuntimeWebViewUiState,
    val flowUiState: ExamRuntimeFlowUiState,
    val locationWarmupUiState: ExamRuntimeLocationWarmupUiState,
    val networkUiState: ExamRuntimeNetworkUiState,
    val securityUiState: ExamRuntimeSecurityUiState,
    val clipboardUiState: ExamRuntimeClipboardUiState,
    val adminUiState: ExamRuntimeAdminUiState,
    val runtimeCacheState: ExamRuntimeRuntimeCacheState,
    val examServerStatusState: MutableState<ExamServerFooterStatus>,
    val latestServerProbeIdState: MutableState<Long>,
    val baseNetworkReadinessState: MutableState<NetworkReadinessStatus>,
    val networkTimeline: SnapshotStateList<NetworkTimelineEntry>,
    val networkFlapElapsedMs: SnapshotStateList<Long>,
    val batteryStatusState: MutableState<ExamBatteryStatus>,
    val dpcRuntimeStatusState: MutableState<DpcRuntimeStatus>,
    val dpcCreateWindowsRestrictionAppliedBySessionState: MutableState<Boolean>,
    val dpcExamPolicyAppliedForSessionState: MutableState<Boolean>,
    val networkMainHandler: Handler,
    val clipboardMainHandler: Handler,
    val overlayMainHandler: Handler,
    val exitSessionClearRequestedState: MutableState<Boolean>,
    val exitSessionClearDeferredState: MutableState<CompletableDeferred<Result<Unit>>?>,
    val lastTrustedRuntimeChromeActionElapsedMsState: MutableState<Long?>,
    val lastTrustedRuntimeChromeActionReasonState: MutableState<String?>,
    val deviceTimeSecurityStatusState: MutableState<DeviceTimeSecurityStatus>,
    val lastDeviceTimeDiagnosticKeyState: MutableState<String?>,
    val accessibilityGuardEnabledState: MutableState<Boolean>,
    val accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    val accessibilityGuardLastReasonState: MutableState<String?>,
    val accessibilityGuardLastForeignPackageState: MutableState<String?>,
    val accessibilityGuardLastEventTypeState: MutableState<String?>,
    val accessibilityGuardLastDetectedAtState: MutableState<String?>,
    val accessibilityGuardAlarmSeverityState: MutableState<String>,
    val packageInventoryChangeNonceState: MutableIntState,
    val runtimeDiagnosticsOps: ExamRuntimeDiagnosticsOps,
    val networkReadinessStatus: NetworkReadinessStatus,
    val overlayRiskResult: OverlayRiskResult,
    val examGuardArmed: Boolean,
    val nativeExamFullscreenActive: Boolean,
    val isKeyboardAllowed: Boolean,
    val securityTamperDetected: Boolean,
    val alarmSessionIdentity: com.coblax.examlock.AlarmSessionIdentity,
    val appVersionName: String,
    val onExit: () -> Unit
) {
    val isIndonesian: Boolean get() = uiLanguage == UiLanguage.Indonesian

    private var dpcRuntimeStatus by dpcRuntimeStatusState
    private var dpcExamPolicyAppliedForSession by dpcExamPolicyAppliedForSessionState
    private var dpcCreateWindowsRestrictionAppliedBySession by dpcCreateWindowsRestrictionAppliedBySessionState
    private var lastTrustedRuntimeChromeActionElapsedMs by lastTrustedRuntimeChromeActionElapsedMsState
    private var lastTrustedRuntimeChromeActionReason by lastTrustedRuntimeChromeActionReasonState
    private var latestServerProbeId by latestServerProbeIdState
    private var examServerStatus by examServerStatusState
    private var forcedExitViolationCount by securityUiState.forcedExitViolationCount
    private var pendingForcedExitViolation by securityUiState.pendingForcedExitViolation
    private var showForcedExitAlarm by securityUiState.showForcedExitAlarm
    private var accessibilityGuardLastReason by accessibilityGuardLastReasonState
    private var accessibilityGuardLastForeignPackage by accessibilityGuardLastForeignPackageState
    private var accessibilityGuardLastEventType by accessibilityGuardLastEventTypeState
    private var accessibilityGuardLastDetectedAt by accessibilityGuardLastDetectedAtState
    private var accessibilityGuardAlarmSeverity by accessibilityGuardAlarmSeverityState
    private var lastAppSwitchTrigger by adminUiState.lastAppSwitchTrigger
    private var lastAppSwitchAt by adminUiState.lastAppSwitchAt
    private var lastAppSwitchContext by adminUiState.lastAppSwitchContext
    private var securityIssueDialogTitle by adminUiState.securityIssueDialogTitle
    private var securityIssueDialogMessage by adminUiState.securityIssueDialogMessage
    private var securityIssueDialogCode by adminUiState.securityIssueDialogCode

    // --- Diagnostics (delegates) -------------------------------------------------------

    fun currentNetworkPollingIntervalMillis(): Long = runtimeDiagnosticsOps.currentNetworkPollingIntervalMillis()
    fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long =
        runtimeDiagnosticsOps.currentScreenPinningMonitorIntervalMillis(nowElapsedMs)
    fun clearAppSwitchSuppression() = runtimeDiagnosticsOps.clearAppSwitchSuppression()
    fun setAppSwitchSuppression(
        reason: AppSwitchSuppressionReason,
        durationMs: Long = AppSwitchSuppressionWindowMillis
    ) = runtimeDiagnosticsOps.setAppSwitchSuppression(reason, durationMs)
    fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? =
        runtimeDiagnosticsOps.currentAppSwitchSuppressionReason()
    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String = runtimeDiagnosticsOps.currentAppSwitchEventDetails(signal, suppressionReason)
    fun currentOverlayEventDetails(
        signal: OverlaySignal,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentOverlayEventDetails(signal, extraContext)
    fun currentInternalDialogReason(): String? = runtimeDiagnosticsOps.currentInternalDialogReason()
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = runtimeDiagnosticsOps.recordAction(code, details, level)
    fun writePreviousSessionBreadcrumb(
        code: String,
        details: String = "-"
    ) = runtimeDiagnosticsOps.writePreviousSessionBreadcrumb(code, details)
    fun currentGeofenceEventDetails(
        trigger: String,
        geofenceStatus: GeofenceSecurityStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentGeofenceEventDetails(trigger, geofenceStatus, extraContext)
    fun currentFakeLocationEventDetails(
        trigger: String,
        fakeLocationStatus: LocationSpoofSecurityStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentFakeLocationEventDetails(trigger, fakeLocationStatus, extraContext)
    fun currentNetworkEventDetails(
        trigger: String,
        status: NetworkReadinessStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentNetworkEventDetails(trigger, status, extraContext)
    fun refreshDeviceTimeSecurity(
        trigger: String,
        emitDiagnosticEvent: Boolean = true
    ): DeviceTimeSecurityStatus = runtimeDiagnosticsOps.refreshDeviceTimeSecurity(trigger, emitDiagnosticEvent)
    fun updateNetworkReadiness(source: String) = runtimeDiagnosticsOps.updateNetworkReadiness(source)
    fun applyNetworkReadinessStatus(
        source: String,
        refreshedStatus: NetworkReadinessStatus
    ) = runtimeDiagnosticsOps.applyNetworkReadinessStatus(source, refreshedStatus)
    fun launchNetworkManualRefresh(trigger: String) = runtimeDiagnosticsOps.launchNetworkManualRefresh(trigger)
    suspend fun refreshGeofenceStatus(
        preferFresh: Boolean,
        trigger: String,
        allowRuntimeViolation: Boolean
    ): SplitLocationSecurityStatus =
        runtimeDiagnosticsOps.refreshGeofenceStatus(preferFresh, trigger, allowRuntimeViolation)
    fun invalidateWarmLocationValidationCache() = runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
    suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus =
        runtimeDiagnosticsOps.resolveStartExamLocationValidation()
    fun launchLocationSecurityManualRefresh(trigger: String) =
        runtimeDiagnosticsOps.launchLocationSecurityManualRefresh(trigger)

    // --- Device owner ------------------------------------------------------------------

    fun refreshDpcRuntimeStatus(): DpcRuntimeStatus {
        dpcRuntimeStatus = ExamDeviceOwnerController.readStatus(context)
        return dpcRuntimeStatus
    }

    fun recordDpcRuntimeStatus(
        status: DpcRuntimeStatus = dpcRuntimeStatus,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        extraContext: String? = null
    ) {
        val details = buildString {
            append(status.diagnosticSummary())
            extraContext?.takeIf { it.isNotBlank() }?.let { extra ->
                append(" | ")
                append(extra)
            }
            if (!status.deviceOwner) {
                append(" | enroll=")
                append(ExamDeviceOwnerController.enrollmentCommand(context))
            }
        }
        recordAction(
            ExamRuntimeHardeningDiagnostics.DpcStatusResolved,
            details,
            level
        )
    }

    fun applyDpcExamPoliciesForStart(startLockTask: Boolean): Boolean {
        fun ensureManagedLockTask(status: DpcRuntimeStatus): Boolean {
            val managedLockTaskRequired = dpcExamPolicyAppliedForSession || status.deviceOwner
            val requirement = resolveLockTaskSecurityRequirement(managedLockTaskRequired)
            if (!startLockTask || !managedLockTaskRequired) {
                return lockTaskBridge.satisfies(requirement)
            }

            val currentState = lockTaskBridge.state()
            if (shouldRestartLockTaskForRequirement(currentState, requirement)) {
                recordAction(
                    ExamRuntimeHardeningDiagnostics.DpcLockTaskUpgradeRequested,
                    "from=${currentState.diagnosticLabel} | required=LOCKED | action=restart_lock_task",
                    DiagnosticEventLevel.SECURITY
                )
                lockTaskBridge.disengage()
            }
            if (!lockTaskBridge.satisfies(requirement)) {
                lockTaskBridge.engage(allowLockTask = true)
            }
            return lockTaskBridge.satisfies(requirement)
        }

        if (dpcExamPolicyAppliedForSession) {
            val requirementSatisfied = ensureManagedLockTask(dpcRuntimeStatus)
            refreshDpcRuntimeStatus()
            return requirementSatisfied && lockTaskBridge.satisfies(LockTaskSecurityRequirement.Locked)
        }
        val result = ExamDeviceOwnerController.applyExamPolicies(context)
        dpcRuntimeStatus = result.after
        dpcExamPolicyAppliedForSession = result.after.deviceOwner
        recordDpcRuntimeStatus(
            status = result.after,
            level = if (result.error == null) DiagnosticEventLevel.INFO else DiagnosticEventLevel.WARNING,
            extraContext = result.error?.let { "error=$it" }
        )
        if (result.lockTaskAllowlistApplied) {
            recordAction(
                ExamRuntimeHardeningDiagnostics.DpcLockTaskAllowlistApplied,
                result.after.diagnosticSummary(),
                DiagnosticEventLevel.INFO
            )
        }
        if (result.createWindowsRestrictionApplied) {
            dpcCreateWindowsRestrictionAppliedBySession = true
            recordAction(
                ExamRuntimeHardeningDiagnostics.DpcCreateWindowsRestrictionApplied,
                result.after.diagnosticSummary(),
                DiagnosticEventLevel.INFO
            )
        }
        if (result.createWindowsRestrictionUnsupported) {
            recordAction(
                ExamRuntimeHardeningDiagnostics.DpcCreateWindowsRestrictionUnsupported,
                result.after.diagnosticSummary(),
                DiagnosticEventLevel.WARNING
            )
        }
        val requirementSatisfied = ensureManagedLockTask(result.after)
        refreshDpcRuntimeStatus()
        val finalRequirement = resolveLockTaskSecurityRequirement(dpcExamPolicyAppliedForSession)
        return requirementSatisfied && lockTaskBridge.satisfies(finalRequirement)
    }

    fun clearDpcExamPoliciesForSession(reason: String) {
        val result = ExamDeviceOwnerController.clearCreateWindowsRestrictionIfSessionApplied(
            context = context,
            sessionAppliedRestriction = dpcCreateWindowsRestrictionAppliedBySession
        )
        dpcRuntimeStatus = result.after
        if (result.createWindowsRestrictionCleared) {
            dpcCreateWindowsRestrictionAppliedBySession = false
            dpcExamPolicyAppliedForSession = false
            recordAction(
                ExamRuntimeHardeningDiagnostics.DpcCreateWindowsRestrictionCleared,
                "reason=$reason | ${result.after.diagnosticSummary()}",
                DiagnosticEventLevel.INFO
            )
        } else if (!result.skipped && result.error != null) {
            recordDpcRuntimeStatus(
                status = result.after,
                level = DiagnosticEventLevel.WARNING,
                extraContext = "clear_reason=$reason | error=${result.error}"
            )
        }
    }

    fun runtimeLockTaskRequirement(): LockTaskSecurityRequirement =
        resolveLockTaskSecurityRequirement(dpcExamPolicyAppliedForSession || dpcRuntimeStatus.deviceOwner)

    // --- Runtime chrome and server probe -----------------------------------------------

    fun markTrustedRuntimeChromeAction(reason: String) {
        lastTrustedRuntimeChromeActionElapsedMs = SystemClock.elapsedRealtime()
        lastTrustedRuntimeChromeActionReason = reason
    }

    suspend fun runExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) {
        val probeId = ++latestServerProbeId
        val probeWebView = webViewUiState.instance.value
        val probeNavigation = probeWebView?.navigationState
        val probeRevision = probeNavigation?.revision
        fun canUpdateStatus(): Boolean =
            probeId == latestServerProbeId && probeWebView === webViewUiState.instance.value &&
                flowUiState.webViewErrorMessage.value == null &&
                (probeNavigation == null || probeNavigation.canApplyServerProbe(probeRevision!!))
        val host = safeExamServerHost(payload.examUrl)
        if (markChecking && canUpdateStatus()) {
            examServerStatus = ExamServerFooterStatus.Checking
        }
        recordAction(
            code = "EXAM_SERVER_PROBE_STARTED",
            details = buildExamServerProbeDetails(
                trigger = trigger,
                host = host,
                reason = "started"
            )
        )
        val result = probeExamServerFooterStatus(payload.examUrl)
        if (canUpdateStatus()) {
            examServerStatus = if (networkUiState.networkUnstableEpisodeStartedElapsedMs.value != null) {
                ExamServerFooterStatus.Unstable
            } else {
                result.status
            }
        }
        recordAction(
            code = result.eventCode,
            details = buildExamServerProbeDetails(
                trigger = trigger,
                host = result.host,
                method = result.method,
                code = result.code,
                latencyMs = result.latencyMs,
                reason = result.reason
            ),
            level = result.eventLevel
        )
    }

    fun launchExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) {
        coroutineScope.launch(examExceptionHandler) {
            runExamServerProbe(trigger = trigger, markChecking = markChecking)
        }
    }

    // --- Guard reactions ---------------------------------------------------------------

    fun handleAccessibilityGuardViolation(violation: AccessibilityGuardRuntimeViolation) {
        val currentViolationCount = forcedExitViolationCount
        forcedExitViolationCount = maxOf(
            currentViolationCount,
            violation.violationCount.coerceAtLeast(1)
        )
        pendingForcedExitViolation = true
        showForcedExitAlarm = true
        accessibilityGuardLastReason = violation.reason
        accessibilityGuardLastForeignPackage = violation.foreignPackage
        accessibilityGuardLastEventType = violation.eventType
        accessibilityGuardLastDetectedAt = violation.detectedAt
        accessibilityGuardAlarmSeverity = violation.severity.name
        lastAppSwitchTrigger = AppSwitchSignal.AccessibilityGuard.diagnosticLabel()
        lastAppSwitchAt = violation.detectedAt ?: diagnosticTimestamp()
        val details = buildAccessibilityGuardViolationDetails(
            currentAppSwitchEventDetails(AppSwitchSignal.AccessibilityGuard),
            violation
        )
        lastAppSwitchContext = details
        recordAction(
            accessibilityGuardEventCodeForReason(violation.reason),
            details,
            DiagnosticEventLevel.SECURITY
        )
        recordAction(
            "ACCESSIBILITY_GUARD_RETURN_TO_EXAM_REQUESTED",
            "reason=${violation.reason?.ifBlank { "-" } ?: "-"} | " +
                "foreign_package=${violation.foreignPackage?.ifBlank { "-" } ?: "-"}",
            DiagnosticEventLevel.INFO
        )
        examAlarmController.start(violation.severity)
    }

    fun showBlockedPermissionDialog(kind: BlockedPermissionKind) {
        val message = resolveBlockedPermissionMessage(uiLanguage, kind)
        recordAction(
            code = "PERMISSION_PROMPT_BLOCKED",
            details = message.details,
            level = DiagnosticEventLevel.WARNING
        )
        securityIssueDialogTitle = message.title
        securityIssueDialogMessage = message.message
        securityIssueDialogCode = message.code
    }

    fun handleScreenPinningTransitionInterrupted() {
        handleExamRuntimeScreenPinningTransitionInterrupted(
            lockTaskRequestPending = flowUiState.lockTaskRequestPending.value,
            examSessionStarted = flowUiState.examSessionStarted.value,
            lockTaskBridge = lockTaskBridge,
            pinningActivationStartedAtElapsedMs = flowUiState.pinningActivationStartedAtElapsedMs.value,
            pinningSuppressedTransitionCount = flowUiState.pinningSuppressedTransitionCount.intValue,
            isIndonesian = isIndonesian,
            pinningActivationPurpose = flowUiState.pinningActivationPurpose.value,
            setPinningActivationState = { flowUiState.pinningActivationState.value = it },
            setLockTaskStateAfterPinningRequest = { adminUiState.lockTaskStateAfterPinningRequest.value = it },
            setScreenPinningDialogLikelyShown = { adminUiState.screenPinningDialogLikelyShown.value = it },
            setPinningSuppressedTransitionCount = { flowUiState.pinningSuppressedTransitionCount.intValue = it },
            setScreenPinningMessage = { flowUiState.screenPinningMessage.value = it },
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            }
        )
    }
}
