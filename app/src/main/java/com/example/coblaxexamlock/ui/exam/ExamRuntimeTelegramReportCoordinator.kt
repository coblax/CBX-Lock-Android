package com.example.coblaxexamlock.ui.exam

import android.content.Context
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AppSwitchBypassState
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DpcRuntimeStatus
import com.example.coblaxexamlock.ExamParticipantContext
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.OverlayBypassState
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.usesDefaultExamUserAgent
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.sendTelegramSectionReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ExamRuntimeTelegramReportCallbacks(
    val isSendingSection: () -> Boolean,
    val setSendingSection: (DiagnosticSection?) -> Unit,
    val refreshScreenPinningDiagnostics: () -> Unit,
    val refreshKeyboardSecurity: () -> Unit,
    val refreshBluetoothSecurity: () -> Unit,
    val refreshDeviceIntegritySecurity: () -> Unit,
    val refreshIntegrityGuard: () -> Unit,
    val refreshRuntimeStaticSecurity: () -> Unit,
    val refreshDeviceTimeSecurity: () -> com.example.coblaxexamlock.DeviceTimeSecurityStatus,
    val refreshGeofenceStatus: suspend () -> com.example.coblaxexamlock.SplitLocationSecurityStatus,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    val showFeedback: (String, String) -> Unit
)

internal fun launchExamRuntimeTelegramSectionReport(
    scope: CoroutineScope,
    context: Context,
    section: DiagnosticSection,
    uiLanguage: UiLanguage,
    mainActivity: MainActivity?,
    adminSettings: AdminSettings,
    payload: ExamQrPayload,
    effectiveExamUserAgent: String,
    participantContext: ExamParticipantContext?,
    lowRamProfile: LowRamProfile,
    lockTaskBridge: ActivityLockTaskBridge,
    screenPinningMode: ScreenPinningMode,
    securityUiState: ExamRuntimeSecurityUiState,
    overlayBypassState: OverlayBypassState,
    overlayViolationCount: Int,
    overlayShieldStatus: OverlayShieldStatus,
    lastOverlayTrigger: String?,
    lastOverlayAt: String?,
    lastOverlayContext: String?,
    appSwitchBypassState: AppSwitchBypassState,
    forcedExitViolationCount: Int,
    pendingForcedExitViolation: Boolean,
    lastAppSwitchTrigger: String?,
    lastAppSwitchAt: String?,
    lastAppSwitchContext: String?,
    accessibilityGuardEnabled: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    accessibilityGuardLastReason: String?,
    accessibilityGuardLastForeignPackage: String?,
    accessibilityGuardLastEventType: String?,
    accessibilityGuardLastDetectedAt: String?,
    accessibilityGuardAlarmSeverity: String,
    examSessionStarted: Boolean,
    examGuardArmed: Boolean,
    adminOverridesSummary: String,
    currentKeyboardPackage: String,
    isKeyboardAllowed: Boolean,
    useBuiltInExamKeyboard: Boolean,
    bluetoothPermissionGranted: Boolean,
    bluetoothEnabled: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAccessibility: Boolean,
    adbBypassState: AdbBypassState,
    rootBypassState: RootBypassState,
    clipboardSignature: String,
    clipboardViolationCount: Int,
    lastClipboardChangeEvent: String,
    networkStatus: ExamNetworkStatus,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    offlineRuntimeStatus: ExamOfflineRuntimeStatus,
    effectiveLocationPolicySource: LocationPolicySource,
    geofenceViolationCount: Int,
    lastGeofenceTrigger: String?,
    lastGeofenceAt: String?,
    lastGeofenceContext: String?,
    fakeLocationViolationCount: Int,
    lastFakeLocationTrigger: String?,
    lastFakeLocationAt: String?,
    lastFakeLocationContext: String?,
    screenPinningAvailable: Boolean,
    screenPinningEnabledInSystem: String,
    lockTaskStateBeforePinningRequest: String,
    lockTaskStateAfterPinningRequest: String,
    screenPinningRequestOutcome: String,
    screenPinningDialogLikelyShown: Boolean,
    screenPinningUserActionInference: String,
    screenPinningActivationDurationMs: Long?,
    examSessionCancelledByPinningFailure: Boolean,
    bypassScreenPinning: Boolean,
    bypassOverlay: Boolean,
    bypassAppSwitch: Boolean,
    bypassDeviceTime: Boolean,
    bypassVpn: Boolean,
    integrityPublicSummary: String,
    diagnosticEvents: List<DiagnosticEvent>,
    webViewCompatibilityStatus: com.example.coblaxexamlock.WebViewCompatibilityStatus,
    lastExamRefreshDecision: String?,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    networkTimelinePreview: List<NetworkTimelineEntry>,
    lastNetworkChangeAt: String?,
    lastNetworkChangeSource: String?,
    lastConnectedNetworkLabel: String?,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    dpcRuntimeStatus: DpcRuntimeStatus,
    callbacks: ExamRuntimeTelegramReportCallbacks
) {
    if (callbacks.isSendingSection()) {
        return
    }
    val sectionLabel = diagnosticSectionLabel(section, uiLanguage)
    val latestAccessibilityInspection = inspectAccessibility(context)
    val latestOverlayRiskResult = OverlayRiskAnalyzer.inspect(
        bypassed = overlayBypassState == OverlayBypassState.Active,
        accessibilityEnabled = latestAccessibilityInspection.blockingServiceActive,
        riskyAccessibilityPackages = latestAccessibilityInspection.riskyPackages,
        violationCount = overlayViolationCount,
        shieldStatus = overlayShieldStatus,
        lastTrigger = lastOverlayTrigger,
        lastDetectedAt = lastOverlayAt,
        lastContext = lastOverlayContext
    )
    val latestAdbInspection = inspectAdb(context)
    val latestRootSecurityStatus = buildRootSecurityStatus(
        SecurityDetectorCache.readRootDetectionDetails(
            context = context,
            forceRefresh = section == DiagnosticSection.Root
        )
    )
    val latestAppSwitchStatus = AppSwitchMonitor.statusOf(
        bypassState = appSwitchBypassState,
        runtimeMonitoringActive = AppSwitchMonitor.shouldMonitor(
            hostAvailable = mainActivity != null,
            guardArmed = examGuardArmed,
            bypassState = appSwitchBypassState
        ),
        protectionMode = AppSwitchMonitor.protectionModeOf(
            bypassState = appSwitchBypassState,
            screenPinningMode = screenPinningMode,
            guardArmed = examGuardArmed,
            lockTaskActive = lockTaskBridge.active()
        ),
        lockTaskActive = lockTaskBridge.active(),
        violationCount = forcedExitViolationCount,
        pendingViolation = pendingForcedExitViolation,
        lastTrigger = lastAppSwitchTrigger,
        lastDetectedAt = lastAppSwitchAt,
        lastContext = lastAppSwitchContext,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        accessibilityFallbackActive = accessibilityGuardFallbackActive,
        accessibilityViolationCount = AccessibilityExamGuardStore.snapshot(context).violationCount,
        accessibilityLastReason = accessibilityGuardLastReason,
        accessibilityLastForeignPackage = accessibilityGuardLastForeignPackage,
        accessibilityLastEventType = accessibilityGuardLastEventType,
        accessibilityLastDetectedAt = accessibilityGuardLastDetectedAt,
        accessibilityAlarmSeverity = accessibilityGuardAlarmSeverity
    )
    callbacks.recordAction("DIAGNOSTIC_SECTION_REQUESTED", section.name, DiagnosticEventLevel.INFO)
    callbacks.refreshScreenPinningDiagnostics()
    callbacks.refreshKeyboardSecurity()
    callbacks.refreshBluetoothSecurity()
    callbacks.refreshDeviceIntegritySecurity()
    callbacks.refreshIntegrityGuard()
    callbacks.refreshRuntimeStaticSecurity()
    val latestDeviceTimeStatus = callbacks.refreshDeviceTimeSecurity()
    callbacks.setSendingSection(section)

    scope.launch {
        try {
            val latestLocationStatus = callbacks.refreshGeofenceStatus()
            val latestGeofenceRuntimeStatus = GeofenceRuntimeStatus(
                evaluation = latestLocationStatus.geofenceStatus.geofenceEvaluation,
                securityStatus = latestLocationStatus.geofenceStatus,
                policySource = effectiveLocationPolicySource,
                violationCount = geofenceViolationCount,
                lastTrigger = lastGeofenceTrigger,
                lastDetectedAt = lastGeofenceAt,
                lastContext = lastGeofenceContext
            )
            val latestFakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
                securityStatus = latestLocationStatus.fakeLocationStatus,
                violationCount = fakeLocationViolationCount,
                lastTrigger = lastFakeLocationTrigger,
                lastDetectedAt = lastFakeLocationAt,
                lastContext = lastFakeLocationContext
            )
            sendTelegramSectionReport(
                context = context,
                section = section,
                examName = payload.examName,
                examUserAgent = effectiveExamUserAgent,
                examUserAgentSource = if (adminSettings.usesDefaultExamUserAgent()) "default" else "custom",
                participantContext = participantContext,
                examSessionStarted = examSessionStarted,
                examRuntimeGuardsArmed = examGuardArmed,
                adminOverridesSummary = adminOverridesSummary,
                keyboardPackage = currentKeyboardPackage,
                keyboardAllowed = isKeyboardAllowed,
                usingBuiltInExamKeyboard = useBuiltInExamKeyboard,
                bluetoothPermissionGranted = bluetoothPermissionGranted,
                bluetoothEnabled = bluetoothEnabled,
                accessibilityServiceEnabled = accessibilityServiceEnabled,
                bypassAccessibility = bypassAccessibility,
                accessibilityBypassTampered = adminSettings.accessibilityBypassTampered,
                adbInspection = latestAdbInspection,
                adbBypassState = adbBypassState,
                rootSecurityStatus = latestRootSecurityStatus,
                rootBypassState = rootBypassState,
                clipboardSignature = clipboardSignature,
                clipboardViolationCount = clipboardViolationCount,
                lastClipboardChangeEvent = lastClipboardChangeEvent,
                networkStatus = networkStatus,
                clipboardRuntimeStatus = clipboardRuntimeStatus,
                offlineRuntimeStatus = offlineRuntimeStatus,
                geofenceRuntimeStatus = latestGeofenceRuntimeStatus,
                fakeLocationRuntimeStatus = latestFakeLocationRuntimeStatus,
                overlayViolationCount = overlayViolationCount,
                overlayRiskResult = latestOverlayRiskResult,
                overlayBypassTampered = adminSettings.overlayBypassTampered,
                appSwitchStatus = latestAppSwitchStatus,
                appSwitchBypassTampered = adminSettings.appSwitchBypassTampered,
                screenPinningAvailable = screenPinningAvailable,
                screenPinningEnabledInSystem = screenPinningEnabledInSystem,
                lockTaskStateBeforePinningRequest = lockTaskStateBeforePinningRequest,
                lockTaskStateAfterPinningRequest = lockTaskStateAfterPinningRequest,
                screenPinningRequestOutcome = screenPinningRequestOutcome,
                screenPinningDialogLikelyShown = screenPinningDialogLikelyShown,
                screenPinningUserActionInference = screenPinningUserActionInference,
                screenPinningActivationDurationMs = screenPinningActivationDurationMs,
                examSessionCancelledByPinningFailure = examSessionCancelledByPinningFailure,
                isScreenPinningActive = lockTaskBridge.active(),
                bypassScreenPinning = bypassScreenPinning,
                bypassOverlay = bypassOverlay,
                bypassAppSwitch = bypassAppSwitch,
                deviceTimeSecurityStatus = latestDeviceTimeStatus,
                bypassDeviceTime = bypassDeviceTime,
                bypassVpn = bypassVpn,
                vpnBypassTampered = adminSettings.vpnBypassTampered,
                integritySummary = integrityPublicSummary,
                reverseEngineeringDetected = securityUiState.tamperDetected.value,
                reverseEngineeringBypass = adminSettings.bypassReverseEngineering,
                reverseEngineeringBypassTampered = adminSettings.reverseEngineeringBypassTampered,
                reverseEngineeringSignals = securityUiState.tamperSummary.value,
                apkIntegrityDetected = securityUiState.integrityTamperDetected.value,
                apkIntegrityBypass = adminSettings.bypassApkIntegrity,
                apkIntegrityBypassTampered = adminSettings.apkIntegrityBypassTampered,
                integrityIssues = securityUiState.integrityPublicSummary.value
                    .ifBlank { securityUiState.integritySummary.value },
                diagnosticEvents = diagnosticEvents,
                uiLanguage = uiLanguage,
                webViewCompatibilityStatus = webViewCompatibilityStatus,
                lastExamRefreshDecision = lastExamRefreshDecision,
                networkReadinessStatus = networkReadinessStatus,
                networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
                networkTimelinePreview = networkTimelinePreview,
                lastNetworkChangeAt = lastNetworkChangeAt,
                lastNetworkChangeSource = lastNetworkChangeSource,
                lastConnectedNetworkLabel = lastConnectedNetworkLabel,
                screenRecorderPackages = securityUiState.screenRecorderPackages.value,
                bypassScreenRecorder = bypassScreenRecorder,
                screenRecorderBypassTampered = adminSettings.screenRecorderBypassTampered,
                screenRecorderViolationCount = securityUiState.screenRecorderViolationCount.intValue,
                screenRecorderDialogActive = securityUiState.showScreenRecorderViolationDialog.value,
                externalDisplayDetected = securityUiState.externalDisplayDetected.value,
                externalDisplayCount = securityUiState.externalDisplayCount.intValue,
                bypassDisplayMirror = bypassDisplayMirror,
                displayMirrorBypassTampered = adminSettings.displayMirrorBypassTampered,
                displayMirrorViolationCount = securityUiState.displayMirrorViolationCount.intValue,
                displayMirrorDialogActive = securityUiState.showDisplayMirrorViolationDialog.value,
                multiWindowDetected = securityUiState.multiWindowDetected.value,
                bypassMultiWindow = bypassMultiWindow,
                multiWindowBypassTampered = adminSettings.multiWindowBypassTampered,
                multiWindowViolationCount = securityUiState.multiWindowViolationCount.intValue,
                multiWindowDialogActive = securityUiState.showMultiWindowViolationDialog.value,
                dpcRuntimeStatus = dpcRuntimeStatus,
                compactReport = lowRamProfile.telegramCompactReport
            ).onSuccess {
                callbacks.recordAction("DIAGNOSTIC_SECTION_SENT", section.name, DiagnosticEventLevel.INFO)
                callbacks.showFeedback(
                    localized(uiLanguage, "Diagnostics sent", "Diagnostik terkirim"),
                    localized(
                        uiLanguage,
                        "$sectionLabel diagnostics have been sent to Telegram.",
                        "Diagnostik $sectionLabel sudah dikirim ke Telegram."
                    )
                )
            }.onFailure { throwable ->
                callbacks.recordAction(
                    "DIAGNOSTIC_SECTION_FAILED",
                    throwable.message ?: "-",
                    DiagnosticEventLevel.ERROR
                )
                callbacks.showFeedback(
                    localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal"),
                    throwable.message ?: localized(
                        uiLanguage,
                        "Diagnostics could not be sent to Telegram.",
                        "Data diagnostik belum berhasil dikirim ke Telegram."
                    )
                )
            }
        } finally {
            callbacks.setSendingSection(null)
        }
    }
}
