package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.AppSwitchBypassState
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbStore
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.config.AppSwitchSuppressionWindowMillis
import com.example.coblaxexamlock.config.LowMaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.MaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.NetworkUnstableFlipThreshold
import com.example.coblaxexamlock.config.NetworkUnstableWindowMillis
import com.example.coblaxexamlock.config.UltraMaxNetworkTimelineEntries
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkDnsProbeVerdict
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.runtime.LowRamDispatchers
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.acquireBestEffortLocationSnapshot
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatusWithExamHostProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal fun examRuntimeNetworkPollingIntervalMillis(
    networkConnected: Boolean,
    networkUnstableEpisodeStartedElapsedMs: Long?,
    lowRamProfile: LowRamProfile
): Long {
    val baseInterval = if (!networkConnected || networkUnstableEpisodeStartedElapsedMs != null) {
        NetworkReadinessPollingUnstableIntervalMillis
    } else {
        NetworkReadinessPollingStableIntervalMillis
    }
    return baseInterval * lowRamProfile.slowPollingMultiplier
}

internal fun examRuntimeScreenPinningMonitorIntervalMillis(
    lockTaskRequestPending: Boolean,
    examSessionStartedAtElapsedMs: Long?,
    lowRamProfile: LowRamProfile,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Long {
    val withinWarmupWindow =
        lockTaskRequestPending ||
            (
                examSessionStartedAtElapsedMs != null &&
                    (nowElapsedMs - examSessionStartedAtElapsedMs).coerceAtLeast(0L) <=
                    ScreenPinningMonitorWarmupWindowMillis
            )
    return if (withinWarmupWindow) {
        ScreenPinningMonitorWarmupIntervalMillis
    } else {
        screenPinningMonitorSteadyIntervalMillis(lowRamProfile)
    }
}

internal fun buildExamRuntimeOfflineStatus(
    examSessionStarted: Boolean,
    networkConnected: Boolean,
    offlineStartedAtElapsedMs: Long?,
    offlineStartedAtTimestamp: String?,
    offlineWarningShown: Boolean,
    lastOfflineWarningAt: String?,
    lastOfflineDurationMs: Long?
): Pair<Long?, ExamOfflineRuntimeStatus> {
    val currentOfflineDurationMs = if (
        examSessionStarted &&
        !networkConnected &&
        offlineStartedAtElapsedMs != null
    ) {
        (SystemClock.elapsedRealtime() - offlineStartedAtElapsedMs).coerceAtLeast(0L)
    } else {
        null
    }
    return currentOfflineDurationMs to ExamOfflineRuntimeStatus(
        offlineActive = examSessionStarted && !networkConnected && offlineStartedAtElapsedMs != null,
        offlineStartedAt = offlineStartedAtTimestamp,
        currentOfflineDurationMs = currentOfflineDurationMs,
        offlineWarningShown = offlineWarningShown,
        lastOfflineWarningAt = lastOfflineWarningAt,
        lastOfflineDurationMs = lastOfflineDurationMs
    )
}

internal fun buildExamRuntimeNetworkUnstableStatus(
    networkUnstableEpisodeStartedElapsedMs: Long?,
    networkUnstableEpisodeStartedAt: String?,
    networkUnstableFlapCount: Int,
    networkUnstableLastFlapAt: String?,
    networkUnstableWarningShown: Boolean,
    lastNetworkUnstableWarningAt: String?,
    networkUnstableLastTransportLabel: String?
): NetworkUnstableRuntimeStatus =
    NetworkUnstableRuntimeStatus(
        unstableActive = networkUnstableEpisodeStartedElapsedMs != null,
        episodeStartedAt = networkUnstableEpisodeStartedAt,
        flapCount = networkUnstableFlapCount,
        lastFlapAt = networkUnstableLastFlapAt,
        warningShown = networkUnstableWarningShown,
        lastWarningAt = lastNetworkUnstableWarningAt,
        lastTransportLabel = networkUnstableLastTransportLabel
    )

internal fun buildExamRuntimeGeofenceStatus(
    geofenceStatus: GeofenceSecurityStatus,
    policySource: LocationPolicySource,
    violationCount: Int,
    lastTrigger: String?,
    lastDetectedAt: String?,
    lastContext: String?
): GeofenceRuntimeStatus =
    GeofenceRuntimeStatus(
        evaluation = geofenceStatus.geofenceEvaluation,
        securityStatus = geofenceStatus,
        policySource = policySource,
        violationCount = violationCount,
        lastTrigger = lastTrigger,
        lastDetectedAt = lastDetectedAt,
        lastContext = lastContext
    )

internal fun buildExamRuntimeFakeLocationStatus(
    fakeLocationStatus: LocationSpoofSecurityStatus,
    violationCount: Int,
    lastTrigger: String?,
    lastDetectedAt: String?,
    lastContext: String?
): FakeLocationRuntimeStatus =
    FakeLocationRuntimeStatus(
        securityStatus = fakeLocationStatus,
        violationCount = violationCount,
        lastTrigger = lastTrigger,
        lastDetectedAt = lastDetectedAt,
        lastContext = lastContext
    )

internal fun buildExamRuntimeOverlayShieldStatus(
    requested: Boolean,
    lastApplySucceeded: Boolean?,
    lastApplyAt: String?
): OverlayShieldStatus =
    OverlayShieldStatus(
        supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        requested = requested,
        lastApplySucceeded = lastApplySucceeded,
        lastApplyAt = lastApplyAt
    )

internal fun buildExamRuntimeClipboardStatus(
    lastClipboardObservedAt: String?,
    lastClipboardConfirmedAt: String?,
    lastClipboardObservedSignature: String?,
    lastClipboardDecision: String,
    lastClipboardBaselineSemanticSignature: String?,
    lastClipboardDetectedSemanticSignature: String?,
    clipboardDecisionSemanticSignature: String
): ClipboardRuntimeStatus =
    ClipboardRuntimeStatus(
        lastObservedAt = lastClipboardObservedAt,
        lastConfirmedAt = lastClipboardConfirmedAt,
        lastObservedSignature = lastClipboardObservedSignature,
        lastDecision = lastClipboardDecision,
        baselineSemanticSignature = lastClipboardBaselineSemanticSignature,
        detectedSemanticSignature = lastClipboardDetectedSemanticSignature,
        currentSemanticSignature = clipboardDecisionSemanticSignature
    )

internal fun latestNetworkTimelinePreview(networkTimeline: List<NetworkTimelineEntry>): List<NetworkTimelineEntry> =
    networkTimeline.takeLast(5).asReversed()

internal class ExamRuntimeDiagnosticsOps(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val lockTaskBridge: ActivityLockTaskBridge,
    private val mainActivity: MainActivity?,
    private val lowRamProfile: LowRamProfile,
    private val screenPinningMode: ScreenPinningMode,
    private val appSwitchBypassState: AppSwitchBypassState,
    val effectiveLocationPolicySource: LocationPolicySource,
    private val deviceTimeBaseline: DeviceTimeBaseline,
    private val deviceTimeBypassState: DeviceTimeBypassState,
    private val examUrl: String,
    private val geofenceConfigParseResult: GeofenceConfigParseResult,
    private val geofenceBypassState: GeofenceBypassState,
    private val fakeLocationBypassState: FakeLocationBypassState,
    private val bypassVpn: Boolean,
    private val bypassGeofence: Boolean,
    private val bypassFakeLocation: Boolean,
    private val warmLocationPolicySignature: String,
    val networkReadinessStatus: NetworkReadinessStatus,
    private val baseNetworkReadinessState: MutableState<NetworkReadinessStatus>,
    private val networkUiState: ExamRuntimeNetworkUiState,
    private val networkTimeline: MutableList<NetworkTimelineEntry>,
    private val networkFlapElapsedMs: MutableList<Long>,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val clipboardUiState: ExamRuntimeClipboardUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val webViewUiState: ExamRuntimeWebViewUiState,
    private val locationWarmupUiState: ExamRuntimeLocationWarmupUiState,
    private val deviceTimeSecurityStatusState: MutableState<DeviceTimeSecurityStatus>,
    private val lastDeviceTimeDiagnosticKeyState: MutableState<String?>,
    private val accessibilityGuardEnabledState: MutableState<Boolean>,
    private val accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    private val accessibilityGuardLastReasonState: MutableState<String?>,
    private val accessibilityGuardLastForeignPackageState: MutableState<String?>,
    private val accessibilityGuardLastEventTypeState: MutableState<String?>,
    private val accessibilityGuardLastDetectedAtState: MutableState<String?>,
    private val accessibilityGuardAlarmSeverityState: MutableState<String>,
    private val examAlarmController: ExamAlarmController
) {
    private val networkStatus = networkReadinessStatus.examStatus
    private val examGuardArmed: Boolean
        get() = adminUiState.examRuntimeMonitoringArmed.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.examSessionStarted.value

    fun currentNetworkPollingIntervalMillis(): Long =
        examRuntimeNetworkPollingIntervalMillis(
            networkConnected = networkStatus.isConnected,
            networkUnstableEpisodeStartedElapsedMs =
                networkUiState.networkUnstableEpisodeStartedElapsedMs.value,
            lowRamProfile = lowRamProfile
        )

    fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long =
        examRuntimeScreenPinningMonitorIntervalMillis(
            lockTaskRequestPending = flowUiState.lockTaskRequestPending.value,
            examSessionStartedAtElapsedMs = adminUiState.examSessionStartedAtElapsedMs.value,
            lowRamProfile = lowRamProfile,
            nowElapsedMs = nowElapsedMs
        )

    private val offlineRuntimeStatusSnapshot = buildExamRuntimeOfflineStatus(
        examSessionStarted = flowUiState.examSessionStarted.value,
        networkConnected = networkStatus.isConnected,
        offlineStartedAtElapsedMs = networkUiState.offlineStartedAtElapsedMs.value,
        offlineStartedAtTimestamp = networkUiState.offlineStartedAtTimestamp.value,
        offlineWarningShown = networkUiState.offlineWarningShown.value,
        lastOfflineWarningAt = networkUiState.lastOfflineWarningAt.value,
        lastOfflineDurationMs = networkUiState.lastOfflineDurationMs.value
    )
    val currentOfflineDurationMs: Long? = offlineRuntimeStatusSnapshot.first
    val offlineRuntimeStatus: ExamOfflineRuntimeStatus = offlineRuntimeStatusSnapshot.second
    val networkTimelinePreview: List<NetworkTimelineEntry> = latestNetworkTimelinePreview(networkTimeline)
    val networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus =
        buildExamRuntimeNetworkUnstableStatus(
            networkUnstableEpisodeStartedElapsedMs =
                networkUiState.networkUnstableEpisodeStartedElapsedMs.value,
            networkUnstableEpisodeStartedAt = networkUiState.networkUnstableEpisodeStartedAt.value,
            networkUnstableFlapCount = networkUiState.networkUnstableFlapCount.intValue,
            networkUnstableLastFlapAt = networkUiState.networkUnstableLastFlapAt.value,
            networkUnstableWarningShown = networkUiState.networkUnstableWarningShown.value,
            lastNetworkUnstableWarningAt = networkUiState.lastNetworkUnstableWarningAt.value,
            networkUnstableLastTransportLabel = networkUiState.networkUnstableLastTransportLabel.value
        )
    val geofenceRuntimeStatus: GeofenceRuntimeStatus =
        buildExamRuntimeGeofenceStatus(
            geofenceStatus = securityUiState.geofenceSecurityStatus.value,
            policySource = effectiveLocationPolicySource,
            violationCount = flowUiState.geofenceViolationCount.intValue,
            lastTrigger = flowUiState.lastGeofenceTrigger.value,
            lastDetectedAt = flowUiState.lastGeofenceAt.value,
            lastContext = flowUiState.lastGeofenceContext.value
        )
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus =
        buildExamRuntimeFakeLocationStatus(
            fakeLocationStatus = securityUiState.fakeLocationSecurityStatus.value,
            violationCount = flowUiState.fakeLocationViolationCount.intValue,
            lastTrigger = flowUiState.lastFakeLocationTrigger.value,
            lastDetectedAt = flowUiState.lastFakeLocationAt.value,
            lastContext = flowUiState.lastFakeLocationContext.value
        )
    val overlayShieldStatus: OverlayShieldStatus =
        buildExamRuntimeOverlayShieldStatus(
            requested = securityUiState.overlayShieldRequested.value,
            lastApplySucceeded = securityUiState.overlayShieldLastApplySucceeded.value,
            lastApplyAt = securityUiState.overlayShieldLastAppliedAt.value
        )
    val clipboardRuntimeStatus: ClipboardRuntimeStatus =
        buildExamRuntimeClipboardStatus(
            lastClipboardObservedAt = clipboardUiState.lastClipboardObservedAt.value,
            lastClipboardConfirmedAt = clipboardUiState.lastClipboardConfirmedAt.value,
            lastClipboardObservedSignature = clipboardUiState.lastClipboardObservedSignature.value,
            lastClipboardDecision = clipboardUiState.lastClipboardDecision.value,
            lastClipboardBaselineSemanticSignature =
                clipboardUiState.lastClipboardBaselineSemanticSignature.value,
            lastClipboardDetectedSemanticSignature =
                clipboardUiState.lastClipboardDetectedSemanticSignature.value,
            clipboardDecisionSemanticSignature = clipboardUiState.clipboardDecisionSemanticSignature.value
        )
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus
        get() = deviceTimeSecurityStatusState.value
    val appSwitchLockTaskActive: Boolean = lockTaskBridge.active()
    val appSwitchProtectionMode =
        AppSwitchMonitor.protectionModeOf(
            bypassState = appSwitchBypassState,
            screenPinningMode = screenPinningMode,
            guardArmed = examGuardArmed,
            lockTaskActive = appSwitchLockTaskActive
        )
    val appSwitchStatus =
        AppSwitchMonitor.statusOf(
            bypassState = appSwitchBypassState,
            runtimeMonitoringActive = AppSwitchMonitor.shouldMonitor(
                hostAvailable = mainActivity != null,
                guardArmed = examGuardArmed,
                bypassState = appSwitchBypassState
            ),
            protectionMode = appSwitchProtectionMode,
            lockTaskActive = appSwitchLockTaskActive,
            violationCount = securityUiState.forcedExitViolationCount.intValue,
            pendingViolation = securityUiState.pendingForcedExitViolation.value,
            lastTrigger = adminUiState.lastAppSwitchTrigger.value,
            lastDetectedAt = adminUiState.lastAppSwitchAt.value,
            lastContext = adminUiState.lastAppSwitchContext.value,
            accessibilityGuardEnabled = accessibilityGuardEnabledState.value,
            accessibilityFallbackActive = accessibilityGuardFallbackActiveState.value,
            accessibilityViolationCount = AccessibilityExamGuardStore.snapshot(context).violationCount,
            accessibilityLastReason = accessibilityGuardLastReasonState.value,
            accessibilityLastForeignPackage = accessibilityGuardLastForeignPackageState.value,
            accessibilityLastEventType = accessibilityGuardLastEventTypeState.value,
            accessibilityLastDetectedAt = accessibilityGuardLastDetectedAtState.value,
            accessibilityAlarmSeverity = accessibilityGuardAlarmSeverityState.value
        )

    fun currentDiagnosticScreen(): String =
        examRuntimeDiagnosticScreen(
            lockTaskRequestPending = flowUiState.lockTaskRequestPending.value,
            examSessionStarted = flowUiState.examSessionStarted.value,
            examRuntimeMonitoringArmed = adminUiState.examRuntimeMonitoringArmed.value
        )

    fun writePreviousSessionBreadcrumb(code: String, details: String = "-") {
        runCatching {
            PreviousExamSessionBreadcrumbStore.append(
                context = context,
                code = code,
                details = details
            )
        }
        if (
            ExamRuntimeHardeningDiagnostics.shouldLogForQa(
                ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten
            )
        ) {
            Log.i(
                ExamRuntimeHardeningLogTag,
                "code=${ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten} " +
                    "level=INFO details=event=$code | ${details.ifBlank { "-" }}"
            )
        }
        adminUiState.diagnosticEvents.value = prependDiagnosticEvent(
            existingEvents = adminUiState.diagnosticEvents.value,
            code = ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten,
            details = "event=$code | ${details.ifBlank { "-" }}",
            level = DiagnosticEventLevel.INFO,
            screen = currentDiagnosticScreen(),
            appStartedAtElapsedMs = adminUiState.appStartedAtElapsedMs,
            examSessionStartedAtElapsedMs = adminUiState.examSessionStartedAtElapsedMs.value,
            maxEntries = lowRamProfile.diagnosticLogMaxEntries
        )
    }

    fun maybeWritePreviousSessionBreadcrumb(code: String, details: String) {
        val breadcrumbCode = when (code) {
            ExamRuntimeHardeningDiagnostics.ScreenPinningAlreadyActive ->
                PreviousExamSessionBreadcrumbCodes.ScreenPinningActive
            ExamRuntimeHardeningDiagnostics.ScreenPinningRequestSkippedAlreadyActive ->
                PreviousExamSessionBreadcrumbCodes.ScreenPinningSkipped
            ExamRuntimeHardeningDiagnostics.WebViewRendererGone ->
                PreviousExamSessionBreadcrumbCodes.RendererGone
            ExamRuntimeHardeningDiagnostics.WebViewExitCleanupStarted ->
                PreviousExamSessionBreadcrumbCodes.CleanupStarted
            ExamRuntimeHardeningDiagnostics.WebViewExitCleanupSucceeded ->
                PreviousExamSessionBreadcrumbCodes.CleanupSucceeded
            ExamRuntimeHardeningDiagnostics.WebViewExitCleanupTimeout ->
                PreviousExamSessionBreadcrumbCodes.CleanupTimeout
            else -> null
        }
        if (breadcrumbCode != null) {
            writePreviousSessionBreadcrumb(breadcrumbCode, details)
        }
    }

    fun clearAppSwitchSuppression() {
        adminUiState.appSwitchSuppressionReason.value = null
        adminUiState.appSwitchSuppressedUntilElapsedMs.value = null
    }

    fun setAppSwitchSuppression(
        reason: AppSwitchSuppressionReason,
        durationMs: Long = AppSwitchSuppressionWindowMillis
    ) {
        adminUiState.appSwitchSuppressionReason.value = reason
        adminUiState.appSwitchSuppressedUntilElapsedMs.value =
            SystemClock.elapsedRealtime() + durationMs
    }

    fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? =
        resolveAppSwitchSuppressionReason(
            reason = adminUiState.appSwitchSuppressionReason.value,
            expiresAtElapsedMs = adminUiState.appSwitchSuppressedUntilElapsedMs.value
        )

    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String =
        buildAppSwitchEventDetails(
            signal = signal,
            appSwitchStatus = appSwitchStatus,
            screenPinningMode = screenPinningMode,
            lockTaskActive = lockTaskBridge.active(),
            suppressionReason = suppressionReason
        )

    fun currentOverlayEventDetails(
        signal: OverlaySignal,
        extraContext: String? = null
    ): String =
        buildOverlayEventDetails(
            signal = signal,
            overlayShieldStatus = overlayShieldStatus,
            appSwitchStatus = appSwitchStatus,
            pendingForcedExitViolation = securityUiState.pendingForcedExitViolation.value,
            appSwitchLifecycleResumePending = adminUiState.appSwitchLifecycleResumePending.value,
            overlayWindowHasFocus = securityUiState.overlayWindowHasFocus.value,
            suppressionReason = currentAppSwitchSuppressionReason(),
            hasFullScreenCustomView = webViewUiState.fullScreenCustomView.value != null,
            extraContext = extraContext
        )

    fun currentInternalDialogReason(): String? =
        resolveInternalDialogReason(
            showOfflineWarningDialog = networkUiState.showOfflineWarningDialog.value,
            showNetworkUnstableDialog = networkUiState.showNetworkUnstableDialog.value,
            showForcedExitAlarm = securityUiState.showForcedExitAlarm.value,
            showKeyboardViolationDialog = securityUiState.showKeyboardViolationDialog.value,
            showOverlayViolationDialog = securityUiState.showOverlayViolationDialog.value,
            showGeofenceViolationDialog = flowUiState.showGeofenceViolationDialog.value,
            showFakeLocationViolationDialog = flowUiState.showFakeLocationViolationDialog.value,
            showBluetoothViolationDialog = securityUiState.showBluetoothViolationDialog.value,
            showScreenRecorderViolationDialog = securityUiState.showScreenRecorderViolationDialog.value,
            showDisplayMirrorViolationDialog = securityUiState.showDisplayMirrorViolationDialog.value,
            showMultiWindowViolationDialog = securityUiState.showMultiWindowViolationDialog.value,
            showClipboardViolationDialog = clipboardUiState.showClipboardViolationDialog.value,
            showExitExamDialog = flowUiState.showExitExamDialog.value,
            pendingSectionPresent = adminUiState.pendingSection.value != null,
            securityIssueDialogMessagePresent = adminUiState.securityIssueDialogMessage.value != null,
            bugReportFeedbackMessagePresent = adminUiState.bugReportFeedbackMessage.value != null
        )

    fun recordOverlayEvent(
        code: String,
        signal: OverlaySignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        extraContext: String? = null
    ) {
        val details = currentOverlayEventDetails(signal, extraContext)
        securityUiState.lastOverlayTrigger.value = signal.diagnosticLabel()
        securityUiState.lastOverlayAt.value = diagnosticTimestamp()
        securityUiState.lastOverlayContext.value = details
        adminUiState.diagnosticEvents.value = prependDiagnosticEvent(
            existingEvents = adminUiState.diagnosticEvents.value,
            code = code,
            details = details,
            level = level,
            screen = currentDiagnosticScreen(),
            appStartedAtElapsedMs = adminUiState.appStartedAtElapsedMs,
            examSessionStartedAtElapsedMs = adminUiState.examSessionStartedAtElapsedMs.value,
            maxEntries = lowRamProfile.diagnosticLogMaxEntries
        )
    }

    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) {
        maybeWritePreviousSessionBreadcrumb(code, details)
        if (ExamRuntimeHardeningDiagnostics.shouldLogForQa(code)) {
            Log.i(
                ExamRuntimeHardeningLogTag,
                "code=$code level=${level.name} details=${details.ifBlank { "-" }}"
            )
        }
        adminUiState.diagnosticEvents.value = prependDiagnosticEvent(
            existingEvents = adminUiState.diagnosticEvents.value,
            code = code,
            details = details,
            level = level,
            screen = currentDiagnosticScreen(),
            appStartedAtElapsedMs = adminUiState.appStartedAtElapsedMs,
            examSessionStartedAtElapsedMs = adminUiState.examSessionStartedAtElapsedMs.value,
            maxEntries = lowRamProfile.diagnosticLogMaxEntries
        )
    }

    fun currentGeofenceEventDetails(
        trigger: String,
        geofenceStatus: GeofenceSecurityStatus,
        extraContext: String? = null
    ): String =
        buildGeofenceEventDetails(
            trigger = trigger,
            geofenceStatus = geofenceStatus,
            policySource = effectiveLocationPolicySource,
            extraContext = extraContext
        )

    fun currentFakeLocationEventDetails(
        trigger: String,
        fakeLocationStatus: LocationSpoofSecurityStatus,
        extraContext: String? = null
    ): String =
        buildFakeLocationEventDetails(
            trigger = trigger,
            fakeLocationStatus = fakeLocationStatus,
            extraContext = extraContext
        )

    fun currentNetworkEventDetails(
        trigger: String,
        status: NetworkReadinessStatus,
        extraContext: String? = null
    ): String =
        buildNetworkEventDetails(
            trigger = trigger,
            status = status,
            extraContext = extraContext
        )

    fun refreshDeviceTimeSecurity(
        trigger: String,
        emitDiagnosticEvent: Boolean = true
    ): DeviceTimeSecurityStatus {
        val refreshedStatus = inspectDeviceTimeSecurity(
            context = context,
            baseline = deviceTimeBaseline,
            bypassState = deviceTimeBypassState
        )
        deviceTimeSecurityStatusState.value = refreshedStatus
        if (emitDiagnosticEvent) {
            val eventCode = when {
                refreshedStatus.bypassState == DeviceTimeBypassState.Tampered ->
                    "DEVICE_TIME_BYPASS_TAMPER_DETECTED"
                refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled ->
                    "DEVICE_TIME_AUTO_DISABLED"
                refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled ->
                    "DEVICE_TIME_AUTO_TIME_ZONE_DISABLED"
                refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected ->
                    "DEVICE_TIME_DRIFT_DETECTED"
                else -> null
            }
            val eventKey = eventCode?.plus("|")?.plus(refreshedStatus.finalVerdict.name)
            if (eventCode != null && eventKey != lastDeviceTimeDiagnosticKeyState.value) {
                recordAction(
                    code = eventCode,
                    details = buildDeviceTimeEventDetails(trigger, refreshedStatus),
                    level = DiagnosticEventLevel.WARNING
                )
                lastDeviceTimeDiagnosticKeyState.value = eventKey
            } else if (eventCode == null) {
                lastDeviceTimeDiagnosticKeyState.value = null
            }
        }
        return refreshedStatus
    }

    fun appendNetworkTimelineEntry(entry: NetworkTimelineEntry) {
        networkTimeline.add(entry)
        val effectiveMax = when {
            lowRamProfile.ultra -> UltraMaxNetworkTimelineEntries
            lowRamProfile.enabled -> LowMaxNetworkTimelineEntries
            else -> MaxNetworkTimelineEntries
        }
        while (networkTimeline.size > effectiveMax) {
            networkTimeline.removeAt(0)
        }
    }

    fun applyNetworkReadinessStatus(
        source: String,
        refreshedStatus: NetworkReadinessStatus
    ) {
        val previousStatus = baseNetworkReadinessState.value
        val coreStateChanged =
            previousStatus.examStatus.isConnected != refreshedStatus.examStatus.isConnected ||
                previousStatus.transportLabel != refreshedStatus.transportLabel ||
                previousStatus.diagnostics.isValidated != refreshedStatus.diagnostics.isValidated ||
                previousStatus.diagnostics.isCaptivePortal != refreshedStatus.diagnostics.isCaptivePortal ||
                previousStatus.diagnostics.isVpnActive != refreshedStatus.diagnostics.isVpnActive ||
                previousStatus.diagnostics.isAirplaneModeEnabled !=
                refreshedStatus.diagnostics.isAirplaneModeEnabled ||
                previousStatus.verdict != refreshedStatus.verdict ||
                previousStatus.userFacingVerdict != refreshedStatus.userFacingVerdict ||
                previousStatus.globalDnsProbeStatus.verdict != refreshedStatus.globalDnsProbeStatus.verdict ||
                previousStatus.dnsProbeStatus.verdict != refreshedStatus.dnsProbeStatus.verdict
        if (!coreStateChanged) {
            return
        }
        baseNetworkReadinessState.value = refreshedStatus

        if (refreshedStatus.examStatus.isConnected) {
            networkUiState.lastConnectedNetworkLabel.value = refreshedStatus.transportLabel
        }
        if (refreshedStatus.diagnostics.isCaptivePortal) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.NetworkCaptivePortalDetected,
                details = currentNetworkEventDetails(source, refreshedStatus),
                level = DiagnosticEventLevel.WARNING
            )
        }
        if (!previousStatus.diagnostics.isVpnActive && refreshedStatus.diagnostics.isVpnActive) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.NetworkVpnDetected,
                details = currentNetworkEventDetails(
                    trigger = source,
                    status = refreshedStatus,
                    extraContext = "bypass=${if (bypassVpn) "yes" else "no"}"
                ),
                level = if (bypassVpn) DiagnosticEventLevel.INFO else DiagnosticEventLevel.WARNING
            )
        } else if (previousStatus.diagnostics.isVpnActive && !refreshedStatus.diagnostics.isVpnActive) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.NetworkVpnCleared,
                details = currentNetworkEventDetails(source, refreshedStatus),
                level = DiagnosticEventLevel.INFO
            )
        }
        if (
            refreshedStatus.dnsProbeStatus.verdict == NetworkDnsProbeVerdict.Failed ||
            refreshedStatus.dnsProbeStatus.verdict == NetworkDnsProbeVerdict.Timeout ||
            refreshedStatus.globalDnsProbeStatus.verdict == NetworkDnsProbeVerdict.Failed ||
            refreshedStatus.globalDnsProbeStatus.verdict == NetworkDnsProbeVerdict.Timeout
        ) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.NetworkDnsProbeFailed,
                details = currentNetworkEventDetails(
                    trigger = source,
                    status = refreshedStatus,
                    extraContext = "global_dns=${refreshedStatus.globalDnsProbeStatus.verdict.name.lowercase(Locale.US)} | " +
                        "exam_dns=${refreshedStatus.dnsProbeStatus.verdict.name.lowercase(Locale.US)}"
                ),
                level = DiagnosticEventLevel.WARNING
            )
        }

        val timelineTimestamp = diagnosticTimestamp()
        appendNetworkTimelineEntry(
            NetworkTimelineEntry(
                timestamp = timelineTimestamp,
                source = source,
                transportLabel = refreshedStatus.transportLabel,
                connected = refreshedStatus.examStatus.isConnected,
                validated = refreshedStatus.diagnostics.isValidated,
                captivePortal = refreshedStatus.diagnostics.isCaptivePortal,
                summary = buildString {
                    append(refreshedStatus.verdict.name)
                    append(" | ")
                    append(refreshedStatus.examStatus.detail.ifBlank { "-" })
                }
            )
        )
        networkUiState.lastNetworkChangeAt.value = timelineTimestamp
        networkUiState.lastNetworkChangeSource.value = source

        val flapRelevantChanged =
            previousStatus.examStatus.isConnected != refreshedStatus.examStatus.isConnected ||
                previousStatus.transportLabel != refreshedStatus.transportLabel ||
                previousStatus.diagnostics.isValidated != refreshedStatus.diagnostics.isValidated ||
                previousStatus.diagnostics.isVpnActive != refreshedStatus.diagnostics.isVpnActive
        if (flapRelevantChanged) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val flapTimestamp = diagnosticTimestamp()
            networkFlapElapsedMs.add(nowElapsed)
            while (
                networkFlapElapsedMs.isNotEmpty() &&
                nowElapsed - networkFlapElapsedMs.first() > NetworkUnstableWindowMillis
            ) {
                networkFlapElapsedMs.removeAt(0)
            }
            networkUiState.networkUnstableLastFlapAt.value = flapTimestamp
            networkUiState.networkUnstableLastFlapElapsedMs.value = nowElapsed
            networkUiState.networkUnstableLastTransportLabel.value = refreshedStatus.transportLabel
            networkUiState.networkUnstableFlapCount.intValue = networkFlapElapsedMs.size
            if (
                networkFlapElapsedMs.size >= NetworkUnstableFlipThreshold &&
                networkUiState.networkUnstableEpisodeStartedElapsedMs.value == null
            ) {
                networkUiState.networkUnstableEpisodeStartedElapsedMs.value = nowElapsed
                networkUiState.networkUnstableEpisodeStartedAt.value = flapTimestamp
                networkUiState.networkUnstableWarningShown.value = false
                recordAction(
                    code = "NETWORK_UNSTABLE_EPISODE_STARTED",
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus,
                        extraContext = "flap_count=${networkFlapElapsedMs.size}"
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
            }
        }
        if (
            flowUiState.examSessionStarted.value &&
            networkUiState.networkUnstableEpisodeStartedElapsedMs.value != null &&
            !networkUiState.networkUnstableWarningShown.value
        ) {
            networkUiState.networkUnstableWarningShown.value = true
            networkUiState.lastNetworkUnstableWarningAt.value = diagnosticTimestamp()
            networkUiState.showNetworkUnstableDialog.value = true
            recordAction(
                code = "NETWORK_UNSTABLE_WARNING_SHOWN",
                details = currentNetworkEventDetails(
                    trigger = source,
                    status = refreshedStatus,
                    extraContext = "flap_count=${networkUiState.networkUnstableFlapCount.intValue}"
                ),
                level = DiagnosticEventLevel.WARNING
            )
        }
    }

    fun updateNetworkReadiness(source: String) {
        applyNetworkReadinessStatus(source, readNetworkReadinessStatus(context))
    }

    fun launchNetworkManualRefresh(trigger: String) {
        if (networkUiState.networkManualRefreshInFlight.value) {
            networkUiState.lastNetworkChangeSource.value = "refresh_already_running:$trigger"
            recordAction(
                code = "NETWORK_MANUAL_REFRESH_ALREADY_RUNNING",
                details = "trigger=$trigger",
                level = DiagnosticEventLevel.INFO
            )
            return
        }
        coroutineScope.launch {
            networkUiState.networkManualRefreshInFlight.value = true
            applyNetworkReadinessStatus(
                trigger,
                readNetworkReadinessStatusWithExamHostProbe(context, examUrl)
            )
            delay(250L)
            networkUiState.networkManualRefreshInFlight.value = false
        }
    }

    suspend fun evaluateLocationSecurityNow(preferFresh: Boolean): SplitLocationSecurityStatus {
        val permissionGranted = hasLocationPermissionForWifi(context)
        val preciseGranted = hasFineLocationPermission(context)
        val servicesEnabled = isLocationServicesEnabled(context)
        val developerOptionsForLocation = inspectAdb(context).developerOptionsEnabled
        val geofenceSnapshotRequired =
            geofenceConfigParseResult.enabled &&
                geofenceConfigParseResult.config != null &&
                geofenceBypassState != GeofenceBypassState.Active &&
                permissionGranted &&
                servicesEnabled
        val fakeLocationSnapshotRequired =
            fakeLocationBypassState != FakeLocationBypassState.Active &&
                permissionGranted &&
                servicesEnabled
        val locationSnapshot =
            if (geofenceSnapshotRequired || fakeLocationSnapshotRequired) {
                acquireBestEffortLocationSnapshot(
                    context = context,
                    preferFresh = preferFresh,
                    geofenceConfig = geofenceConfigParseResult.config.takeIf { geofenceSnapshotRequired }
                )
            } else {
                null
            }
        val latestGeofenceStatus = evaluateGeofenceSecurity(
            configResult = geofenceConfigParseResult,
            permissionGranted = permissionGranted,
            preciseLocationGranted = preciseGranted,
            locationServicesEnabled = servicesEnabled,
            locationSnapshot = locationSnapshot,
            bypassState = geofenceBypassState
        )
        val latestFakeLocationStatus = evaluateFakeLocationSecurity(
            monitoringEnabled = true,
            permissionGranted = permissionGranted,
            locationServicesEnabled = servicesEnabled,
            locationSnapshot = locationSnapshot,
            fixQualityStatus = latestGeofenceStatus.fixQualityStatus,
            developerOptionsEnabled = developerOptionsForLocation,
            suspiciousFakeLocationPackages = withContext(LowRamDispatchers.detectorIo) {
                SecurityDetectorCache.readSuspiciousFakeLocationPackages(context)
            },
            bypassState = fakeLocationBypassState
        )
        securityUiState.geofenceEvaluation.value = latestGeofenceStatus.geofenceEvaluation
        securityUiState.geofenceSecurityStatus.value = latestGeofenceStatus
        securityUiState.fakeLocationSecurityStatus.value = latestFakeLocationStatus
        return SplitLocationSecurityStatus(
            geofenceStatus = latestGeofenceStatus,
            fakeLocationStatus = latestFakeLocationStatus
        )
    }

    fun applyGeofenceRuntimeEvaluation(
        geofenceStatus: GeofenceSecurityStatus,
        trigger: String
    ) {
        val evaluation = geofenceStatus.geofenceEvaluation
        if (!evaluation.enabled) {
            flowUiState.geofenceRuntimeEpisodeKey.value = null
            return
        }
        if (!geofenceStatus.blocking) {
            val previousEpisode = flowUiState.geofenceRuntimeEpisodeKey.value
            if (previousEpisode != null) {
                recordAction(
                    code = "GEOFENCE_RUNTIME_RECOVERED",
                    details = currentGeofenceEventDetails(
                        trigger = trigger,
                        geofenceStatus = geofenceStatus,
                        extraContext = "previous_verdict=$previousEpisode"
                    ),
                    level = DiagnosticEventLevel.INFO
                )
            }
            flowUiState.geofenceRuntimeEpisodeKey.value = null
            return
        }
        val nextEpisodeKey = geofenceStatus.finalVerdict.diagnosticLabel()
        if (flowUiState.geofenceRuntimeEpisodeKey.value == nextEpisodeKey) {
            return
        }
        flowUiState.geofenceRuntimeEpisodeKey.value = nextEpisodeKey
        val eventCode = when (geofenceStatus.finalVerdict) {
            GeofenceSecurityVerdict.Outside -> "GEOFENCE_RUNTIME_OUTSIDE"
            GeofenceSecurityVerdict.PreciseRequired -> "GEOFENCE_RUNTIME_PRECISE_REQUIRED"
            else -> "GEOFENCE_RUNTIME_LOCATION_UNAVAILABLE"
        }
        val details = currentGeofenceEventDetails(trigger = trigger, geofenceStatus = geofenceStatus)
        flowUiState.lastGeofenceTrigger.value = trigger
        flowUiState.lastGeofenceAt.value = diagnosticTimestamp()
        flowUiState.lastGeofenceContext.value = details
        flowUiState.geofenceViolationCount.intValue += 1
        flowUiState.showGeofenceViolationDialog.value = true
        recordAction(code = eventCode, details = details, level = DiagnosticEventLevel.SECURITY)
        examAlarmController.start()
    }

    fun applyFakeLocationRuntimeEvaluation(
        fakeLocationStatus: LocationSpoofSecurityStatus,
        trigger: String
    ) {
        if (!fakeLocationStatus.monitoringEnabled) {
            flowUiState.fakeLocationRuntimeEpisodeKey.value = null
            return
        }
        if (!fakeLocationStatus.blocking) {
            val previousEpisode = flowUiState.fakeLocationRuntimeEpisodeKey.value
            if (previousEpisode != null) {
                recordAction(
                    code = "FAKE_LOCATION_RUNTIME_RECOVERED",
                    details = currentFakeLocationEventDetails(
                        trigger = trigger,
                        fakeLocationStatus = fakeLocationStatus,
                        extraContext = "previous_verdict=$previousEpisode"
                    ),
                    level = DiagnosticEventLevel.INFO
                )
            }
            flowUiState.fakeLocationRuntimeEpisodeKey.value = null
            return
        }
        val nextEpisodeKey = buildString {
            append(fakeLocationStatus.finalVerdict.diagnosticLabel())
            append(':')
            append(fakeLocationStatus.confidenceTier.diagnosticLabel())
        }
        if (flowUiState.fakeLocationRuntimeEpisodeKey.value == nextEpisodeKey) {
            return
        }
        flowUiState.fakeLocationRuntimeEpisodeKey.value = nextEpisodeKey
        val eventCode = when (fakeLocationStatus.finalVerdict) {
            LocationSpoofSecurityVerdict.PermissionRequired -> "FAKE_LOCATION_RUNTIME_PERMISSION_REQUIRED"
            LocationSpoofSecurityVerdict.LocationServicesDisabled ->
                "FAKE_LOCATION_RUNTIME_LOCATION_SERVICES_REQUIRED"
            LocationSpoofSecurityVerdict.LocationUnavailable -> "FAKE_LOCATION_RUNTIME_LOCATION_UNAVAILABLE"
            else -> "FAKE_LOCATION_RUNTIME_SPOOF_DETECTED"
        }
        val details = currentFakeLocationEventDetails(trigger, fakeLocationStatus)
        flowUiState.lastFakeLocationTrigger.value = trigger
        flowUiState.lastFakeLocationAt.value = diagnosticTimestamp()
        flowUiState.lastFakeLocationContext.value = details
        flowUiState.fakeLocationViolationCount.intValue += 1
        flowUiState.showFakeLocationViolationDialog.value = true
        recordAction(code = eventCode, details = details, level = DiagnosticEventLevel.SECURITY)
        examAlarmController.start()
    }

    suspend fun refreshGeofenceStatus(
        preferFresh: Boolean,
        trigger: String,
        allowRuntimeViolation: Boolean
    ): SplitLocationSecurityStatus {
        val latestLocationStatus = evaluateLocationSecurityNow(preferFresh = preferFresh)
        if (
            flowUiState.examSessionStarted.value &&
            geofenceConfigParseResult.enabled &&
            allowRuntimeViolation &&
            !bypassGeofence
        ) {
            applyGeofenceRuntimeEvaluation(latestLocationStatus.geofenceStatus, trigger)
        } else if (!latestLocationStatus.geofenceStatus.geofenceEvaluation.enabled ||
            !flowUiState.examSessionStarted.value
        ) {
            flowUiState.geofenceRuntimeEpisodeKey.value = null
        }
        if (
            flowUiState.examSessionStarted.value &&
            allowRuntimeViolation &&
            latestLocationStatus.fakeLocationStatus.monitoringEnabled &&
            !bypassFakeLocation
        ) {
            applyFakeLocationRuntimeEvaluation(latestLocationStatus.fakeLocationStatus, trigger)
        } else if (!latestLocationStatus.fakeLocationStatus.monitoringEnabled ||
            !flowUiState.examSessionStarted.value
        ) {
            flowUiState.fakeLocationRuntimeEpisodeKey.value = null
        }
        if (
            latestLocationStatus.fakeLocationStatus.monitoringEnabled &&
            latestLocationStatus.fakeLocationStatus.bypassState != FakeLocationBypassState.Active &&
            latestLocationStatus.fakeLocationStatus.warningOnly &&
            latestLocationStatus.fakeLocationStatus.suspiciousFakeLocationPackages.isNotEmpty()
        ) {
            val warningKey = buildString {
                append(latestLocationStatus.fakeLocationStatus.confidenceTier.diagnosticLabel())
                append(':')
                append(latestLocationStatus.fakeLocationStatus.suspiciousFakeLocationPackages.joinToString())
            }
            if (warningKey != flowUiState.lastFakeLocationWarningKey.value) {
                recordAction(
                    code = "FAKE_LOCATION_PACKAGE_WARNING",
                    details = currentFakeLocationEventDetails(
                        trigger = trigger,
                        fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
                flowUiState.lastFakeLocationWarningKey.value = warningKey
            }
        } else {
            flowUiState.lastFakeLocationWarningKey.value = null
        }
        return latestLocationStatus
    }

    fun buildCurrentWarmLocationValidationKey(): String =
        buildWarmLocationValidationKey(
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            policySignature = warmLocationPolicySignature
        )

    fun invalidateWarmLocationValidationCache() {
        locationWarmupUiState.reusableWarmLocationValidation.value = null
    }

    suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus {
        val currentValidationKey = buildCurrentWarmLocationValidationKey()
        val reusableWarmLocationForStart =
            locationWarmupUiState.reusableWarmLocationValidation.value?.takeIf {
                it.isReusableForStart(currentValidationKey = currentValidationKey)
            }
        if (reusableWarmLocationForStart != null) {
            val warmAgeMs = (
                SystemClock.elapsedRealtime() - reusableWarmLocationForStart.completedAtElapsedMs
            ).coerceAtLeast(0L)
            debugLogExamStart(
                "startExamSession reused warm location validation prepared ${warmAgeMs} ms ago"
            )
            securityUiState.geofenceEvaluation.value =
                reusableWarmLocationForStart.result.geofenceStatus.geofenceEvaluation
            securityUiState.geofenceSecurityStatus.value =
                reusableWarmLocationForStart.result.geofenceStatus
            securityUiState.fakeLocationSecurityStatus.value =
                reusableWarmLocationForStart.result.fakeLocationStatus
            return reusableWarmLocationForStart.result
        }
        val forcedRefreshReason = locationWarmupUiState.reusableWarmLocationValidation.value
            ?.reuseFailureReason(currentValidationKey = currentValidationKey)
            ?: "no_warm_validation"
        debugLogExamStart(
            "startExamSession forcing full location validation (reason=$forcedRefreshReason)"
        )
        return refreshGeofenceStatus(
            preferFresh = true,
            trigger = "start_exam_validation",
            allowRuntimeViolation = false
        )
    }

    fun launchLocationSecurityManualRefresh(trigger: String) {
        if (flowUiState.geofenceManualRefreshInFlight.value) {
            recordAction(
                code = "LOCATION_MANUAL_REFRESH_ALREADY_RUNNING",
                details = "trigger=$trigger",
                level = DiagnosticEventLevel.INFO
            )
            return
        }
        invalidateWarmLocationValidationCache()
        flowUiState.geofenceManualRefreshInFlight.value = true
        coroutineScope.launch {
            try {
                val refreshedStatus = debugMeasureExamStartSuspendWork("locationRefresh:$trigger") {
                    refreshGeofenceStatus(
                        preferFresh = true,
                        trigger = trigger,
                        allowRuntimeViolation = false
                    )
                }
                val refreshedAt = diagnosticTimestamp()
                val validationKey = buildCurrentWarmLocationValidationKey()
                flowUiState.lastGeofenceRefreshAt.value = refreshedAt
                locationWarmupUiState.reusableWarmLocationValidation.value =
                    WarmLocationValidationCache(
                        result = refreshedStatus,
                        validationKey = validationKey,
                        completedAtElapsedMs = SystemClock.elapsedRealtime(),
                        completedAtTimestamp = refreshedAt
                    ).takeIf { it.isReusableForStart(currentValidationKey = validationKey) }
            } finally {
                flowUiState.geofenceManualRefreshInFlight.value = false
            }
        }
    }
}
