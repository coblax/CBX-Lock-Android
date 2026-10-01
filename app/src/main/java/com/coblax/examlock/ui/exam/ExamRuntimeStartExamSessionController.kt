package com.coblax.examlock.ui.exam

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.coblax.examlock.ExamWebViewSessionResetStep
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.isGeofenceEnforced
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.NetworkReadinessStatus
import kotlinx.coroutines.CancellationException

/**
 * Runs Start Exam: the preflight dialog, the prechecks, the clean browser session and
 * the hand-over to screen pinning. It was a local class of the session composable that
 * captured some eighty of its locals; it now reads them from the session core.
 */
internal class ExamRuntimeStartExamController(
    private val core: ExamRuntimeSessionCore,
    private val runtimeMonitoringOps: ExamRuntimeMonitoringOps,
    private val runtimeSecurityOps: ExamRuntimeSecurityOps,
    private val permissionLaunchers: ExamRuntimePermissionLaunchers
) {
    private val context = core.context
    private val lockTaskBridge = core.lockTaskBridge
    private val coroutineScope = core.coroutineScope
    private val payload = core.payload
    private val uiLanguage = core.uiLanguage
    private val isIndonesian = core.isIndonesian
    private val lowRamProfile = core.lowRamProfile
    private val deviceCompatibilityProfile = core.deviceCompatibilityProfile
    private val webViewCompatibilityStatus = core.webViewCompatibilityStatus
    private val overlayRiskResult = core.overlayRiskResult
    private val examGuardArmed = core.examGuardArmed
    private val flowUiState = core.flowUiState
    private val securityUiState = core.securityUiState
    private val clipboardUiState = core.clipboardUiState
    private val adminUiState = core.adminUiState
    private val accessibilityGuardEnabledState = core.accessibilityGuardEnabledState
    private val screenPinningMode = core.bypass.screenPinningMode
    private val geofenceConfigParseResult = core.bypass.geofenceConfigParseResult
    private val effectiveLocationPolicySource = core.bypass.effectiveLocationPolicySource
    private val geofenceBypassState = core.bypass.geofenceBypassState
    private val fakeLocationBypassState = core.bypass.fakeLocationBypassState
    private val bypassScreenPinning = core.bypass.bypassScreenPinning
    private val bypassOverlay = core.bypass.bypassOverlay
    private val bypassVpn = core.bypass.bypassVpn
    private val bypassDeviceTime = core.bypass.bypassDeviceTime
    private val bypassKeyboardPolicy = core.bypass.bypassKeyboardPolicy
    private val bypassBluetooth = core.bypass.bypassBluetooth
    private val bypassAccessibility = core.bypass.bypassAccessibility
    private val bypassAdb = core.bypass.bypassAdb
    private val bypassVirtualEnvironment = core.bypass.bypassVirtualEnvironment
    private val bypassRoot = core.bypass.bypassRoot
    private val bypassReverseEngineering = core.bypass.bypassReverseEngineering
    private val bypassApkIntegrity = core.bypass.bypassApkIntegrity
    private val bypassScreenRecorder = core.bypass.bypassScreenRecorder
    private val bypassDisplayMirror = core.bypass.bypassDisplayMirror
    private val bypassMultiWindow = core.bypass.bypassMultiWindow
    private val bypassGeofence = core.bypass.bypassGeofence
    private val bypassFakeLocation = core.bypass.bypassFakeLocation

    private val webViewInstance get() = core.webViewUiState.instance.value
    private val batteryStatus get() = core.batteryStatusState.value
    private val screenPinningAvailable get() = adminUiState.screenPinningAvailable.value
    private val accessibilityGuardEnabled get() = accessibilityGuardEnabledState.value
    private var examRuntimeRecoveryState by core.webViewUiState.recoveryState
    private var lockTaskRequestPending by flowUiState.lockTaskRequestPending
    private var geofenceStartValidationInFlight by flowUiState.geofenceStartValidationInFlight
    private var webViewSessionResetInFlight by flowUiState.webViewSessionResetInFlight
    private var pinningActivationState by flowUiState.pinningActivationState
    private var pinningActivationStartedAtElapsedMs by flowUiState.pinningActivationStartedAtElapsedMs
    private var pinningSuppressedTransitionCount by flowUiState.pinningSuppressedTransitionCount
    private var screenPinningMessage by flowUiState.screenPinningMessage
    private var webViewErrorMessage by flowUiState.webViewErrorMessage
    private var securityIssueDialogTitle by adminUiState.securityIssueDialogTitle
    private var securityIssueDialogMessage by adminUiState.securityIssueDialogMessage
    private var securityIssueDialogCode by adminUiState.securityIssueDialogCode
    private var exitOnSecurityIssueDialogDismiss by adminUiState.exitOnSecurityIssueDialogDismiss
    private var lockTaskStateBeforePinningRequest by adminUiState.lockTaskStateBeforePinningRequest
    private var lockTaskStateAfterPinningRequest by adminUiState.lockTaskStateAfterPinningRequest
    private var screenPinningRequestOutcome by adminUiState.screenPinningRequestOutcome
    private var screenPinningDialogLikelyShown by adminUiState.screenPinningDialogLikelyShown
    private var screenPinningUserActionInference by adminUiState.screenPinningUserActionInference
    private var screenPinningActivationDurationMs by adminUiState.screenPinningActivationDurationMs
    private var examSessionCancelledByPinningFailure by adminUiState.examSessionCancelledByPinningFailure
    private var forcedExitViolationCount by securityUiState.forcedExitViolationCount
    private var pendingForcedExitViolation by securityUiState.pendingForcedExitViolation
    private var showForcedExitAlarm by securityUiState.showForcedExitAlarm
    private var accessibilityGuardFallbackActive by core.accessibilityGuardFallbackActiveState
    private var accessibilityGuardLastReason by core.accessibilityGuardLastReasonState
    private var accessibilityGuardLastForeignPackage by core.accessibilityGuardLastForeignPackageState
    private var accessibilityGuardLastEventType by core.accessibilityGuardLastEventTypeState
    private var accessibilityGuardLastDetectedAt by core.accessibilityGuardLastDetectedAtState
    private var accessibilityGuardAlarmSeverity by core.accessibilityGuardAlarmSeverityState

    private fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = core.recordAction(code, details, level)
    private fun applyDpcExamPoliciesForStart(startLockTask: Boolean): Boolean =
        core.applyDpcExamPoliciesForStart(startLockTask)
    private fun refreshDpcRuntimeStatus() = core.refreshDpcRuntimeStatus()
    private fun clearAppSwitchSuppression() = core.clearAppSwitchSuppression()
    private fun setAppSwitchSuppression(reason: com.coblax.examlock.AppSwitchSuppressionReason) =
        core.setAppSwitchSuppression(reason)
    private fun refreshDeviceTimeSecurity(trigger: String, emitDiagnosticEvent: Boolean = true) =
        core.refreshDeviceTimeSecurity(trigger, emitDiagnosticEvent)
    private fun applyNetworkReadinessStatus(source: String, refreshedStatus: NetworkReadinessStatus) =
        core.applyNetworkReadinessStatus(source, refreshedStatus)
    private fun currentGeofenceEventDetails(
        trigger: String,
        geofenceStatus: com.coblax.examlock.GeofenceSecurityStatus
    ) = core.currentGeofenceEventDetails(trigger, geofenceStatus)
    private fun currentFakeLocationEventDetails(
        trigger: String,
        fakeLocationStatus: com.coblax.examlock.LocationSpoofSecurityStatus
    ) = core.currentFakeLocationEventDetails(trigger, fakeLocationStatus)
    private suspend fun resolveStartExamLocationValidation() = core.resolveStartExamLocationValidation()
    private fun hideSystemKeyboard() = runtimeMonitoringOps.hideSystemKeyboard()
    private fun armExamRuntimeMonitoring(reason: String) = runtimeMonitoringOps.armExamRuntimeMonitoring(reason)
    private suspend fun refreshReverseEngineeringStatusOnDetector() =
        runtimeMonitoringOps.refreshReverseEngineeringStatusOnDetector()
    private suspend fun refreshIntegrityGuardOnDetector() = runtimeMonitoringOps.refreshIntegrityGuardOnDetector()
    private fun refreshScreenPinningDiagnostics() = runtimeSecurityOps.refreshScreenPinningDiagnostics()
    private fun refreshKeyboardSecurity(triggerViolation: Boolean) =
        runtimeSecurityOps.refreshKeyboardSecurity(triggerViolation)
    private fun refreshBluetoothSecurity(triggerViolation: Boolean) =
        runtimeSecurityOps.refreshBluetoothSecurity(triggerViolation)
    private fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) =
        runtimeSecurityOps.refreshDeviceIntegritySecurity(triggerViolation)
    private fun checkSignatureIntegrity(triggerViolation: Boolean) =
        runtimeSecurityOps.checkSignatureIntegrity(triggerViolation)

    fun showStartExamPreflight(
        step: StartExamPreflightStep = StartExamPreflightStep.Starting,
        detail: String? = null
    ) {
        showStartExamPreflight(
            state = flowUiState.startExamPreflight,
            step = step,
            detail = detail,
            startedAtElapsedMs = SystemClock.elapsedRealtime()
        )
    }

    fun updateStartExamPreflight(
        step: StartExamPreflightStep,
        detail: String? = null
    ) {
        updateStartExamPreflightStep(
            state = flowUiState.startExamPreflight,
            step = step,
            detail = detail
        )
    }

    fun hideStartExamPreflight() {
        hideStartExamPreflight(flowUiState.startExamPreflight)
    }

    private fun applyStartExamBlockMessage(
        message: StartExamBlockMessage,
        level: DiagnosticEventLevel = DiagnosticEventLevel.WARNING
    ) {
        hideStartExamPreflight()
        applyExamRuntimeStartBlockMessage(
            message = message,
            level = level,
            callbacks = ExamRuntimeStartBlockCallbacks(
                recordAction = { code, details, eventLevel ->
                    recordAction(code = code, details = details, level = eventLevel)
                },
                setSecurityIssueDialogTitle = { securityIssueDialogTitle = it },
                setSecurityIssueDialogMessage = { securityIssueDialogMessage = it },
                setSecurityIssueDialogCode = { securityIssueDialogCode = it }
            )
        )
    }

    fun resetPreparationSecurityEpisodes() {
        resetStartExamPreparationSecurityEpisodes(flowUiState)
    }

    fun finalizeExamSessionStart(lockTaskAlreadyActive: Boolean) {
        updateStartExamPreflight(StartExamPreflightStep.Complete)
        hideStartExamPreflight()
        applyDpcExamPoliciesForStart(startLockTask = false)
        finalizeStartExamSession(
            context = context,
            lockTaskBridge = lockTaskBridge,
            flowUiState = flowUiState,
            adminUiState = adminUiState,
            clipboardUiState = clipboardUiState,
            securityUiState = securityUiState,
            lockTaskAlreadyActive = lockTaskAlreadyActive,
            hideSystemKeyboard = { hideSystemKeyboard() },
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            }
        )
    }

    suspend fun prepareCleanExamWebViewSessionForStart(): Boolean {
        // Cancel on the dialog must stop the exam from starting, before and after the reset.
        if (flowUiState.startExamPreflight.cancelRequested.value) {
            return false
        }
        updateStartExamPreflight(StartExamPreflightStep.PreparingWebView)
        val prepared = prepareCleanExamWebViewSessionForStart(
            context = context,
            existingWebView = webViewInstance,
            lowRamProfile = lowRamProfile,
            flowUiState = flowUiState,
            adminUiState = adminUiState,
            uiLanguage = uiLanguage,
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            },
            onRecoveryStateIdle = { examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle },
            onResetProgress = { resetStep ->
                val detail = when (resetStep) {
                    ExamWebViewSessionResetStep.ClearCookies -> localized(
                        uiLanguage,
                        "Clearing previous exam cookies.",
                        "Membersihkan cookie ujian sebelumnya."
                    )
                    ExamWebViewSessionResetStep.ClearStorage -> localized(
                        uiLanguage,
                        "Clearing exam browser storage.",
                        "Membersihkan storage browser ujian."
                    )
                    ExamWebViewSessionResetStep.ClearDatabase -> localized(
                        uiLanguage,
                        "Clearing saved browser form and auth data.",
                        "Membersihkan data form dan autentikasi browser."
                    )
                    ExamWebViewSessionResetStep.PrepareWebView -> localized(
                        uiLanguage,
                        "Preparing the exam browser instance.",
                        "Menyiapkan instance browser ujian."
                    )
                    ExamWebViewSessionResetStep.Complete -> localized(
                        uiLanguage,
                        "Loading the exam page.",
                        "Memuat halaman ujian."
                    )
                }
                updateStartExamPreflight(
                    StartExamPreflightStep.PreparingWebView,
                    detail = detail
                )
            }
        )
        if (!prepared) {
            hideStartExamPreflight()
        }
        if (prepared && flowUiState.startExamPreflight.cancelRequested.value) {
            recordAction(
                code = "START_EXAM_CANCELLED_BY_STUDENT",
                details = "stage=preparing_webview",
                level = DiagnosticEventLevel.INFO
            )
            return false
        }
        return prepared
    }

    fun completeStartExamSessionAfterPrechecks() {
        completeExamRuntimeStartAfterPrechecks(
            context = context,
            lockTaskBridge = lockTaskBridge,
            coroutineScope = coroutineScope,
            uiLanguage = uiLanguage,
            isIndonesian = isIndonesian,
            screenPinningMode = screenPinningMode,
            screenPinningAvailable = screenPinningAvailable,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            lockTaskRequestPending = lockTaskRequestPending,
            geofenceStartValidationInFlight = geofenceStartValidationInFlight,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            examGuardArmed = examGuardArmed,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            callbacks = ExamRuntimeCompleteStartCallbacks(
                setAccessibilityGuardFallbackActive = { accessibilityGuardFallbackActive = it },
                setAccessibilityGuardLastReason = { accessibilityGuardLastReason = it },
                setAccessibilityGuardLastForeignPackage = { accessibilityGuardLastForeignPackage = it },
                setAccessibilityGuardLastEventType = { accessibilityGuardLastEventType = it },
                setAccessibilityGuardLastDetectedAt = { accessibilityGuardLastDetectedAt = it },
                setAccessibilityGuardAlarmSeverity = { accessibilityGuardAlarmSeverity = it },
                setForcedExitViolationCount = { forcedExitViolationCount = it },
                setPendingForcedExitViolation = { pendingForcedExitViolation = it },
                setShowForcedExitAlarm = { showForcedExitAlarm = it },
                setLockTaskStateBeforePinningRequest = { lockTaskStateBeforePinningRequest = it },
                setLockTaskStateAfterPinningRequest = { lockTaskStateAfterPinningRequest = it },
                setScreenPinningRequestOutcome = { screenPinningRequestOutcome = it },
                setScreenPinningDialogLikelyShown = { screenPinningDialogLikelyShown = it },
                setScreenPinningUserActionInference = { screenPinningUserActionInference = it },
                setScreenPinningActivationDurationMs = { screenPinningActivationDurationMs = it },
                setExamSessionCancelledByPinningFailure = { examSessionCancelledByPinningFailure = it },
                setLockTaskRequestPending = { lockTaskRequestPending = it },
                setPinningActivationState = { pinningActivationState = it },
                setPinningActivationStartedAtElapsedMs = { pinningActivationStartedAtElapsedMs = it },
                setPinningSuppressedTransitionCount = { pinningSuppressedTransitionCount = it },
                setScreenPinningMessage = { screenPinningMessage = it },
                setWebViewErrorMessage = { webViewErrorMessage = it },
                setExitOnSecurityIssueDialogDismiss = { exitOnSecurityIssueDialogDismiss = it },
                resetPreparationSecurityEpisodes = { this.resetPreparationSecurityEpisodes() },
                prepareCleanExamWebViewSessionForStart = { this.prepareCleanExamWebViewSessionForStart() },
                armExamRuntimeMonitoring = { reason -> armExamRuntimeMonitoring(reason) },
                finalizeExamSessionStart = { lockTaskAlreadyActive -> this.finalizeExamSessionStart(lockTaskAlreadyActive) },
                ensureDeviceOwnerLockTaskActive = {
                    applyDpcExamPoliciesForStart(startLockTask = true)
                },
                refreshDpcRuntimeStatus = { refreshDpcRuntimeStatus() },
                clearAppSwitchSuppression = { clearAppSwitchSuppression() },
                setAppSwitchSuppression = { reason -> setAppSwitchSuppression(reason) },
                hideStartExamPreflight = { this.hideStartExamPreflight() },
                applyStartExamBlockMessage = { message -> this.applyStartExamBlockMessage(message) },
                recordAction = { code, details, level ->
                    recordAction(code = code, details = details, level = level)
                }
            )
        )
    }

    suspend fun startExamSession() {
        if (webViewSessionResetInFlight) {
            return
        }
        showStartExamPreflight()
        examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle
        try {
            runExamRuntimeStartPrechecks(
                context = context,
                uiLanguage = uiLanguage,
                payload = payload,
                lockTaskBridge = lockTaskBridge,
                screenPinningMode = screenPinningMode,
                screenPinningAvailable = screenPinningAvailable,
                deviceCompatibilityProfile = deviceCompatibilityProfile,
                overlayRiskResult = overlayRiskResult,
                webViewCompatibilityStatus = webViewCompatibilityStatus,
                webViewRecoveryStateName = examRuntimeRecoveryState.name,
                batteryStatus = batteryStatus,
                geofenceConfigParseResult = geofenceConfigParseResult,
                effectiveLocationPolicySource = effectiveLocationPolicySource,
                geofenceBypassState = geofenceBypassState,
                fakeLocationBypassState = fakeLocationBypassState,
                flowUiState = flowUiState,
                securityUiState = securityUiState,
                adminUiState = adminUiState,
                accessibilityGuardEnabledState = accessibilityGuardEnabledState,
                bypassScreenPinning = bypassScreenPinning,
                bypassOverlay = bypassOverlay,
                bypassVpn = bypassVpn,
                bypassDeviceTime = bypassDeviceTime,
                bypassKeyboardPolicy = bypassKeyboardPolicy,
                bypassBluetooth = bypassBluetooth,
                bypassAccessibility = bypassAccessibility,
                bypassAdb = bypassAdb,
                bypassVirtualEnvironment = bypassVirtualEnvironment,
                bypassRoot = bypassRoot,
                bypassReverseEngineering = bypassReverseEngineering,
                bypassApkIntegrity = bypassApkIntegrity,
                bypassScreenRecorder = bypassScreenRecorder,
                bypassDisplayMirror = bypassDisplayMirror,
                bypassMultiWindow = bypassMultiWindow,
                bypassGeofence = bypassGeofence,
                bypassFakeLocation = bypassFakeLocation,
                callbacks = ExamRuntimeStartPrecheckCallbacks(
                    recordAction = { code, details, level ->
                        recordAction(code = code, details = details, level = level)
                    },
                    applyVirtualEnvironmentDiagnostics = { diagnostics, triggerViolation ->
                        runtimeSecurityOps.applyVirtualEnvironmentDiagnostics(
                            diagnostics = diagnostics,
                            triggerViolation = triggerViolation
                        )
                    },
                    refreshReverseEngineeringStatus = { refreshReverseEngineeringStatusOnDetector() },
                    refreshIntegrityGuard = { refreshIntegrityGuardOnDetector() },
                    refreshScreenPinningDiagnostics = { refreshScreenPinningDiagnostics() },
                    refreshKeyboardSecurity = { triggerViolation -> refreshKeyboardSecurity(triggerViolation) },
                    refreshBluetoothSecurity = { triggerViolation -> refreshBluetoothSecurity(triggerViolation) },
                    refreshDeviceIntegritySecurity = { triggerViolation -> refreshDeviceIntegritySecurity(triggerViolation) },
                    updateStartExamPreflight = { step, detail -> this.updateStartExamPreflight(step, detail) },
                    hideStartExamPreflight = { this.hideStartExamPreflight() },
                    applyStartExamBlockMessage = { message -> this.applyStartExamBlockMessage(message) },
                    refreshDeviceTimeSecurity = { trigger, emitDiagnosticEvent ->
                        refreshDeviceTimeSecurity(
                            trigger = trigger,
                            emitDiagnosticEvent = emitDiagnosticEvent
                        )
                    },
                    applyNetworkReadinessStatus = { source, refreshedStatus -> applyNetworkReadinessStatus(source, refreshedStatus) },
                    checkSignatureIntegrity = { triggerViolation -> checkSignatureIntegrity(triggerViolation) },
                    currentGeofenceEventDetails = { trigger, geofenceStatus ->
                        currentGeofenceEventDetails(
                            trigger = trigger,
                            geofenceStatus = geofenceStatus
                        )
                    },
                    currentFakeLocationEventDetails = { trigger, fakeLocationStatus ->
                        currentFakeLocationEventDetails(
                            trigger = trigger,
                            fakeLocationStatus = fakeLocationStatus
                        )
                    },
                    ensureDeviceOwnerLockTaskActive = {
                        applyDpcExamPoliciesForStart(startLockTask = true)
                    },
                    refreshDpcRuntimeStatus = { refreshDpcRuntimeStatus() },
                    requestBluetoothPermission = {
                        permissionLaunchers.launchBluetooth()
                    },
                    requestLocationPermission = {
                        permissionLaunchers.launchLocation()
                    },
                    launchFinalLocationValidation = { startExamPressedAt ->
                        launchExamRuntimeStartLocationValidation(
                            context = context,
                            coroutineScope = coroutineScope,
                            uiLanguage = uiLanguage,
                            payload = payload,
                            bypassGeofence = bypassGeofence,
                            bypassFakeLocation = bypassFakeLocation,
                            startExamPressedAt = startExamPressedAt,
                            locationCheckRequired =
                                isGeofenceEnforced(geofenceConfigParseResult, geofenceBypassState),
                            callbacks = ExamRuntimeStartLocationValidationCallbacks(
                                isGeofenceStartValidationInFlight = { geofenceStartValidationInFlight },
                                isStartCancelledByStudent = {
                                    flowUiState.startExamPreflight.cancelRequested.value
                                },
                                setGeofenceStartValidationInFlight = { geofenceStartValidationInFlight = it },
                                resolveStartExamLocationValidation = { resolveStartExamLocationValidation() },
                                currentGeofenceEventDetails = { trigger, geofenceStatus ->
                                    currentGeofenceEventDetails(
                                        trigger = trigger,
                                        geofenceStatus = geofenceStatus
                                    )
                                },
                                currentFakeLocationEventDetails = { trigger, fakeLocationStatus ->
                                    currentFakeLocationEventDetails(
                                        trigger = trigger,
                                        fakeLocationStatus = fakeLocationStatus
                                    )
                                },
                                updateStartExamPreflight = { step, detail -> this.updateStartExamPreflight(step, detail) },
                                hideStartExamPreflight = { this.hideStartExamPreflight() },
                                applyStartExamBlockMessage = { message -> this.applyStartExamBlockMessage(message) },
                                refreshDeviceTimeSecurity = { trigger, emitDiagnosticEvent ->
                                    refreshDeviceTimeSecurity(
                                        trigger = trigger,
                                        emitDiagnosticEvent = emitDiagnosticEvent
                                    )
                                },
                                completeStartExamSessionAfterPrechecks = { this.completeStartExamSessionAfterPrechecks() },
                                debugLogExamStart = { message -> debugLogExamStart(message) }
                            )
                        )
                    },
                    debugLogExamStart = { message -> debugLogExamStart(message) }
                )
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            geofenceStartValidationInFlight = false
            webViewSessionResetInFlight = false
            applyStartExamBlockMessage(
                resolveStartExamUnexpectedFailureBlockMessage(
                    uiLanguage = uiLanguage,
                    phase = "start_prechecks",
                    throwable = throwable
                ),
                level = DiagnosticEventLevel.ERROR
            )
        }
    }
}
