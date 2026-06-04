package com.example.coblaxexamlock.ui.exam

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.coblaxexamlock.AccessibilityBypassResolver
import com.example.coblaxexamlock.AccessibilityBypassState
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AdbBypassResolver
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AlarmSessionIdentity
import com.example.coblaxexamlock.AppSwitchBypassResolver
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.AppSwitchProtectionMode
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.buildAlarmSessionIdentity
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.ClipboardBypassResolver
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.ClipboardSnapshot
import com.example.coblaxexamlock.config.AlarmAcknowledgeDedupWindowMillis
import com.example.coblaxexamlock.config.AppSwitchSuppressionWindowMillis
import com.example.coblaxexamlock.config.MaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.LowMaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.UltraMaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.NetworkUnstableFlipThreshold
import com.example.coblaxexamlock.config.NetworkUnstableWindowMillis
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassResolver
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.DpcRuntimeStatus
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.ExamDeviceOwnerController
import com.example.coblaxexamlock.ExamAlarmSeverity
import com.example.coblaxexamlock.ExamParticipantCaptureBridge
import com.example.coblaxexamlock.ExamParticipantCaptureResult
import com.example.coblaxexamlock.ExamPolicyEngine
import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.ExamScheduleValidationResult
import com.example.coblaxexamlock.ExamScheduleValidator
import com.example.coblaxexamlock.FakeLocationBypassResolver
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.FatalSecuritySignal
import com.example.coblaxexamlock.format.buildIntegrityPublicSummary
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.GeofenceBypassResolver
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.GeofenceEvaluation
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.LocalDeviceCompatibilityProfile
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.NetworkDnsProbeVerdict
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.usesDefaultExamUserAgent
import com.example.coblaxexamlock.model.VirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.openAccessibilitySettings
import com.example.coblaxexamlock.openAirplaneModeSettings
import com.example.coblaxexamlock.openBluetoothSettings
import com.example.coblaxexamlock.openCellularSettings
import com.example.coblaxexamlock.openDateTimeSettings
import com.example.coblaxexamlock.openDeveloperOptionsSettings
import com.example.coblaxexamlock.openInternetConnectivitySettings
import com.example.coblaxexamlock.openKeyboardSettings
import com.example.coblaxexamlock.openLocationServicesSettings
import com.example.coblaxexamlock.openOverlaySettings
import com.example.coblaxexamlock.openScreenPinningSettings
import com.example.coblaxexamlock.openVpnSettings
import com.example.coblaxexamlock.openWebViewProviderSettings
import com.example.coblaxexamlock.openWifiSettings
import com.example.coblaxexamlock.OverlayBypassResolver
import com.example.coblaxexamlock.OverlayBypassState
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.parseExamParticipantContext
import com.example.coblaxexamlock.parseGeofenceConfig
import com.example.coblaxexamlock.PinningActivationPurpose
import com.example.coblaxexamlock.PinningActivationGraceWindowMillis
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.platform.openExternalUrl
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbStore
import com.example.coblaxexamlock.readClipboardSnapshotFull
import com.example.coblaxexamlock.readClipboardSnapshotLite
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.resolveRuntimePressureProfile
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.runtime.acquireBestEffortLocationSnapshot
import com.example.coblaxexamlock.runtime.buildRootIssueMessage
import com.example.coblaxexamlock.runtime.getExternalDisplayCount
import com.example.coblaxexamlock.runtime.getBluetoothConnectPermission
import com.example.coblaxexamlock.runtime.getCachedVirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.runtime.getCurrentInputMethodPackage
import com.example.coblaxexamlock.runtime.getVirtualEnvironmentDiagnosticsOnIo
import com.example.coblaxexamlock.runtime.hasBluetoothExamPermission
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isAllowedExamKeyboard
import com.example.coblaxexamlock.runtime.isBluetoothEnabledForExam
import com.example.coblaxexamlock.runtime.isInAnySplitMode
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readExamBatteryStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatusWithProbe
import com.example.coblaxexamlock.runtime.registerPackageInventoryInvalidationReceiver
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.runtime.resolveKeyboardAppLabel
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.sendTelegramAlarmAcknowledge
import com.example.coblaxexamlock.runtime.sendTelegramSectionReport
import com.example.coblaxexamlock.ScreenPinningBypassResolver
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.ScreenPinningPlatformBridge
import com.example.coblaxexamlock.ScreenPinningSignals
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.shouldSuppressPinningTransitionViolation
import com.example.coblaxexamlock.showKeyboardPicker
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.VpnBypassResolver
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.preparation.PreparationScreenActions
import com.example.coblaxexamlock.ui.preparation.PreparationScreenState
import com.example.coblaxexamlock.ui.preparation.preExamHealthStartBlocker
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@SuppressLint("SetJavaScriptEnabled")
@NonRestartableComposable
internal fun ExamRuntimeSessionScreenImpl(
    inputs: ExamRuntimeSessionInputs,
    callbacks: ExamRuntimeSessionCallbacks,
    modifier: Modifier = Modifier
) {
    val payload = inputs.payload
    val adminSettings = inputs.adminSettings
    val pendingDirectLinkSaveLog = inputs.pendingDirectLinkSaveLog
    val pendingRecoveryEventDetails = inputs.pendingRecoveryEventDetails
    val examSessionRecoveryNonce = inputs.examSessionRecoveryNonce
    val deviceTimeBaselineWallClockMillis = inputs.deviceTimeBaselineWallClockMillis
    val deviceTimeBaselineElapsedRealtimeMillis = inputs.deviceTimeBaselineElapsedRealtimeMillis
    val onDirectLinkSaveLogConsumed = callbacks.onDirectLinkSaveLogConsumed
    val onRecoveryEventConsumed = callbacks.onRecoveryEventConsumed
    val onExamSessionStartedStateChange = callbacks.onExamSessionStartedStateChange
    val onExit = callbacks.onExit
    val context = LocalContext.current
    val activity = context as? Activity
    val componentActivity = activity as? ComponentActivity
        ?: error("ExamWebViewScreen requires a ComponentActivity host")
    val mainActivity = activity as? MainActivity
    val lockTaskBridge = remember(mainActivity) { ActivityLockTaskBridge { mainActivity } }
    val lowRamProfile = LocalLowRamProfile.current
    val deviceCompatibilityProfile = LocalDeviceCompatibilityProfile.current
    var webViewCompatibilityRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    val webViewCompatibilityStatus = remember(context, webViewCompatibilityRefreshKey) {
        SecurityDetectorCache.readWebViewCompatibilityStatus(
            context = context.applicationContext,
            forceRefresh = webViewCompatibilityRefreshKey > 0
        )
    }
    val deviceQuirkProfile = remember(deviceCompatibilityProfile) {
        deviceCompatibilityProfile.toExamRuntimeDeviceQuirkProfile()
    }
    val uiLanguage = LocalUiLanguage.current
    val isIndonesian = uiLanguage == UiLanguage.Indonesian
    val deviceTimeBaseline = remember(
        deviceTimeBaselineWallClockMillis,
        deviceTimeBaselineElapsedRealtimeMillis
    ) {
        DeviceTimeBaseline(
            wallClockMillis = deviceTimeBaselineWallClockMillis,
            elapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        )
    }
    val screenPinningBypassState = remember(
        adminSettings.bypassScreenPinning,
        adminSettings.screenPinningBypassTampered
    ) {
        ScreenPinningBypassResolver.stateOf(
            enabled = adminSettings.bypassScreenPinning,
            tampered = adminSettings.screenPinningBypassTampered
        )
    }
    val screenPinningMode = remember(screenPinningBypassState) {
        ScreenPinningBypassResolver.modeOf(screenPinningBypassState)
    }
    val overlayBypassState = remember(
        adminSettings.bypassOverlay,
        adminSettings.overlayBypassTampered
    ) {
        OverlayBypassResolver.stateOf(
            enabled = adminSettings.bypassOverlay,
            tampered = adminSettings.overlayBypassTampered
        )
    }
    val appSwitchBypassState = remember(
        adminSettings.bypassAppSwitch,
        adminSettings.appSwitchBypassTampered
    ) {
        AppSwitchBypassResolver.stateOf(
            enabled = adminSettings.bypassAppSwitch,
            tampered = adminSettings.appSwitchBypassTampered
        )
    }
    val accessibilityBypassState = remember(
        adminSettings.bypassAccessibility,
        adminSettings.accessibilityBypassTampered
    ) {
        AccessibilityBypassResolver.stateOf(
            enabled = adminSettings.bypassAccessibility,
            tampered = adminSettings.accessibilityBypassTampered
        )
    }
    val clipboardBypassState = remember(
        adminSettings.bypassClipboard,
        adminSettings.clipboardBypassTampered
    ) {
        ClipboardBypassResolver.stateOf(
            enabled = adminSettings.bypassClipboard,
            tampered = adminSettings.clipboardBypassTampered
        )
    }
    val adbBypassState = remember(
        adminSettings.bypassAdb,
        adminSettings.adbBypassTampered
    ) {
        AdbBypassResolver.stateOf(
            enabled = adminSettings.bypassAdb,
            tampered = adminSettings.adbBypassTampered
        )
    }
    val rootBypassState = remember(
        adminSettings.bypassRoot,
        adminSettings.rootBypassTampered
    ) {
        RootBypassResolver.stateOf(
            enabled = adminSettings.bypassRoot,
            tampered = adminSettings.rootBypassTampered
        )
    }
    val geofenceBypassState = remember(
        adminSettings.bypassGeofence,
        adminSettings.geofenceBypassTampered
    ) {
        GeofenceBypassResolver.stateOf(
            enabled = adminSettings.bypassGeofence,
            tampered = adminSettings.geofenceBypassTampered
        )
    }
    val fakeLocationBypassState = remember(
        adminSettings.bypassFakeLocation,
        adminSettings.fakeLocationBypassTampered
    ) {
        FakeLocationBypassResolver.stateOf(
            enabled = adminSettings.bypassFakeLocation,
            tampered = adminSettings.fakeLocationBypassTampered
        )
    }
    val deviceTimeBypassState = remember(
        adminSettings.bypassDeviceTime,
        adminSettings.deviceTimeBypassTampered
    ) {
        DeviceTimeBypassResolver.stateOf(
            enabled = adminSettings.bypassDeviceTime,
            tampered = adminSettings.deviceTimeBypassTampered
        )
    }
    val vpnBypassState = remember(
        adminSettings.bypassVpn,
        adminSettings.vpnBypassTampered
    ) {
        VpnBypassResolver.stateOf(
            enabled = adminSettings.bypassVpn,
            tampered = adminSettings.vpnBypassTampered
        )
    }
    val locationBypassState = geofenceBypassState
    val bypassScreenPinning = adminSettings.bypassScreenPinning
    val bypassBluetooth = adminSettings.bypassBluetooth
    val bypassAccessibility = accessibilityBypassState == AccessibilityBypassState.Active
    val bypassAdb = adbBypassState == AdbBypassState.Active
    val bypassRoot = rootBypassState == RootBypassState.Active
    val bypassReverseEngineering = adminSettings.bypassReverseEngineering
    val bypassApkIntegrity = adminSettings.bypassApkIntegrity
    val bypassVirtualEnvironment = adminSettings.bypassVirtualEnvironment
    val bypassKeyboardPolicy = adminSettings.bypassKeyboardPolicy
    val bypassClipboard = adminSettings.bypassClipboard
    val bypassOverlay = adminSettings.bypassOverlay
    val bypassGeofence = geofenceBypassState == GeofenceBypassState.Active
    val bypassFakeLocation = fakeLocationBypassState == FakeLocationBypassState.Active
    val bypassDeviceTime = deviceTimeBypassState == DeviceTimeBypassState.Active
    val bypassVpn = vpnBypassState == VpnBypassState.Active
    val bypassLocation = bypassGeofence
    val bypassAppSwitch = adminSettings.bypassAppSwitch
    val bypassScreenRecorder = adminSettings.bypassScreenRecorder
    val bypassDisplayMirror = adminSettings.bypassDisplayMirror
    val bypassMultiWindow = adminSettings.bypassMultiWindow
    val adminOverridesSummary = adminSettings.overrideSummary()
    val effectiveLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
    val effectiveLocationPolicySource = if (bypassGeofence) {
        LocationPolicySource.Bypassed
    } else if (payload.locationPolicy != null) {
        payload.locationPolicySource
    } else {
        LocationPolicySource.DisabledNoPolicy
    }
    val geofenceConfigParseResult = remember(
        effectiveLocationPolicy.geofenceEnabled,
        effectiveLocationPolicy.centerLat,
        effectiveLocationPolicy.centerLng,
        effectiveLocationPolicy.radiusMeters,
        effectiveLocationPolicy.shapeType,
        effectiveLocationPolicy.vertices,
        effectiveLocationPolicy.effectiveCircleCenters
    ) {
        parseGeofenceConfig(
            enabled = effectiveLocationPolicy.geofenceEnabled,
            centerLatRaw = effectiveLocationPolicy.centerLat,
            centerLngRaw = effectiveLocationPolicy.centerLng,
            radiusMetersRaw = effectiveLocationPolicy.radiusMeters,
            shapeType = effectiveLocationPolicy.shapeType,
            polygonVertices = effectiveLocationPolicy.vertices,
            circleCenters = effectiveLocationPolicy.effectiveCircleCenters
        )
    }
    val warmLocationPolicySignature = remember(
        geofenceConfigParseResult,
        geofenceBypassState,
        fakeLocationBypassState
    ) {
        buildWarmLocationValidationPolicySignature(
            geofenceConfigParseResult = geofenceConfigParseResult,
            geofenceBypassState = geofenceBypassState,
            fakeLocationBypassState = fakeLocationBypassState
        )
    }
    val geofenceEnabled = geofenceConfigParseResult.enabled
    val officialApkUrl = adminSettings.officialApkUrl.trim()
    val webViewUiState = rememberExamRuntimeWebViewUiState(context)
    var loadingProgress by webViewUiState.loadingProgress
    var webViewStopRequested by webViewUiState.stopRequested
    var webViewInstance by webViewUiState.instance
    var webViewGeneration by webViewUiState.generation
    var destroyedWebViewGeneration by webViewUiState.destroyedGeneration
    val flowUiState = rememberExamRuntimeFlowUiState(
        context = context,
        bypassKeyboardPolicy = bypassKeyboardPolicy
    )
    val locationWarmupUiState = rememberExamRuntimeLocationWarmupUiState()
    var examSessionStarted by flowUiState.examSessionStarted
    var lockTaskRequestPending by flowUiState.lockTaskRequestPending
    var pinningActivationPurpose by flowUiState.pinningActivationPurpose
    var pinningActivationState by flowUiState.pinningActivationState
    var pinningActivationStartedAtElapsedMs by flowUiState.pinningActivationStartedAtElapsedMs
    var pinningSuppressedTransitionCount by flowUiState.pinningSuppressedTransitionCount
    var screenPinningMessage by flowUiState.screenPinningMessage
    var showExitExamDialog by flowUiState.showExitExamDialog
    var webViewErrorMessage by flowUiState.webViewErrorMessage
    val examServerStatusState = rememberSaveable(payload.examUrl) {
        mutableStateOf(ExamServerFooterStatus.Checking)
    }
    var examServerStatus by examServerStatusState
    LaunchedEffect(examSessionRecoveryNonce, examSessionStarted) {
        onExamSessionStartedStateChange(examSessionStarted)
    }
    val baseNetworkReadinessState = remember { mutableStateOf(readNetworkReadinessStatus(context)) }
    var baseNetworkReadiness by baseNetworkReadinessState
    val networkUiState = rememberExamRuntimeNetworkUiState(baseNetworkReadiness)
    val networkTimeline = remember { mutableStateListOf<NetworkTimelineEntry>() }
    val networkFlapElapsedMs = remember { mutableStateListOf<Long>() }
    var networkUnstableEpisodeStartedAt by networkUiState.networkUnstableEpisodeStartedAt
    var networkUnstableEpisodeStartedElapsedMs by networkUiState.networkUnstableEpisodeStartedElapsedMs
    var networkUnstableLastFlapAt by networkUiState.networkUnstableLastFlapAt
    var networkUnstableLastFlapElapsedMs by networkUiState.networkUnstableLastFlapElapsedMs
    var networkUnstableWarningShown by networkUiState.networkUnstableWarningShown
    var lastNetworkUnstableWarningAt by networkUiState.lastNetworkUnstableWarningAt
    var showNetworkUnstableDialog by networkUiState.showNetworkUnstableDialog
    var networkUnstableFlapCount by networkUiState.networkUnstableFlapCount
    var networkUnstableLastTransportLabel by networkUiState.networkUnstableLastTransportLabel
    var lastNetworkChangeAt by networkUiState.lastNetworkChangeAt
    var lastNetworkChangeSource by networkUiState.lastNetworkChangeSource
    var networkManualRefreshInFlight by networkUiState.networkManualRefreshInFlight
    var lastConnectedNetworkLabel by networkUiState.lastConnectedNetworkLabel
    var offlineStartedAtElapsedMs by networkUiState.offlineStartedAtElapsedMs
    var offlineStartedAtTimestamp by networkUiState.offlineStartedAtTimestamp
    var offlineWarningShown by networkUiState.offlineWarningShown
    var lastOfflineWarningAt by networkUiState.lastOfflineWarningAt
    var lastOfflineWarningElapsedMs by networkUiState.lastOfflineWarningElapsedMs
    var lastOfflineDurationMs by networkUiState.lastOfflineDurationMs
    var offlineWarningDurationMs by networkUiState.offlineWarningDurationMs
    var showOfflineWarningDialog by networkUiState.showOfflineWarningDialog
    val batteryStatusState = remember { mutableStateOf(readExamBatteryStatus(context)) }
    var batteryStatus by batteryStatusState
    val dpcRuntimeStatusState = remember {
        mutableStateOf(ExamDeviceOwnerController.readStatus(context))
    }
    var dpcRuntimeStatus by dpcRuntimeStatusState
    var dpcCreateWindowsRestrictionAppliedBySession by rememberSaveable {
        mutableStateOf(false)
    }
    var dpcExamPolicyAppliedForSession by rememberSaveable {
        mutableStateOf(false)
    }
    val networkMainHandler = remember { Handler(Looper.getMainLooper()) }
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }
    var fullScreenCustomView by webViewUiState.fullScreenCustomView
    var fullScreenCustomViewCallback by webViewUiState.fullScreenCustomViewCallback
    val fullScreenContainer = webViewUiState.fullScreenContainer
    fullScreenContainer.setBackgroundColor(LockBackground.toArgb())
    var useBuiltInExamKeyboard by flowUiState.useBuiltInExamKeyboard
    var exitSessionClearInFlight by flowUiState.exitSessionClearInFlight
    val exitSessionClearRequestedState = rememberSaveable { mutableStateOf(false) }
    var exitSessionClearRequested by exitSessionClearRequestedState
    val exitSessionClearDeferredState = remember {
        mutableStateOf<CompletableDeferred<Result<Unit>>?>(null)
    }
    var exitSessionClearDeferred by exitSessionClearDeferredState
    var examRuntimeRecoveryState by webViewUiState.recoveryState
    val lastTrustedRuntimeChromeActionElapsedMsState = rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var lastTrustedRuntimeChromeActionElapsedMs by lastTrustedRuntimeChromeActionElapsedMsState
    val lastTrustedRuntimeChromeActionReasonState = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var lastTrustedRuntimeChromeActionReason by lastTrustedRuntimeChromeActionReasonState
    val runtimeCacheState = rememberExamRuntimeRuntimeCacheState()
    var lastRuntimeMemoryActionSummary by runtimeCacheState.lastRuntimeMemoryActionSummary
    var showBuiltInExamKeyboard by flowUiState.showBuiltInExamKeyboard
    var sideArrowControlsVisible by flowUiState.sideArrowControlsVisible
    var hasEditableFocus by flowUiState.hasEditableFocus
    var builtInKeyboardShiftEnabled by flowUiState.builtInKeyboardShiftEnabled
    var geofencePermissionRequestInFlight by flowUiState.geofencePermissionRequestInFlight
    var geofenceStartValidationInFlight by flowUiState.geofenceStartValidationInFlight
    var webViewSessionResetInFlight by flowUiState.webViewSessionResetInFlight
    var webViewSessionResetError by flowUiState.webViewSessionResetError
    var geofenceManualRefreshInFlight by flowUiState.geofenceManualRefreshInFlight
    var pendingStartExamAfterLocationPermission by flowUiState.pendingStartExamAfterLocationPermission
    var retryStartExamAfterLocationPermissionGrant by flowUiState.retryStartExamAfterLocationPermissionGrant
    var geofenceViolationCount by flowUiState.geofenceViolationCount
    var showGeofenceViolationDialog by flowUiState.showGeofenceViolationDialog
    var showGeofenceMapViewer by flowUiState.showGeofenceMapViewer
    var lastGeofenceTrigger by flowUiState.lastGeofenceTrigger
    var lastGeofenceAt by flowUiState.lastGeofenceAt
    var lastGeofenceContext by flowUiState.lastGeofenceContext
    var lastGeofenceRefreshAt by flowUiState.lastGeofenceRefreshAt
    var geofenceRuntimeEpisodeKey by flowUiState.geofenceRuntimeEpisodeKey
    var fakeLocationViolationCount by flowUiState.fakeLocationViolationCount
    var showFakeLocationViolationDialog by flowUiState.showFakeLocationViolationDialog
    var lastFakeLocationTrigger by flowUiState.lastFakeLocationTrigger
    var lastFakeLocationAt by flowUiState.lastFakeLocationAt
    var lastFakeLocationContext by flowUiState.lastFakeLocationContext
    var fakeLocationRuntimeEpisodeKey by flowUiState.fakeLocationRuntimeEpisodeKey
    var lastFakeLocationWarningKey by flowUiState.lastFakeLocationWarningKey
    var currentKeyboardPackage by flowUiState.currentKeyboardPackage
    var currentKeyboardLabel by flowUiState.currentKeyboardLabel
    var lastKeyboardAllowed by flowUiState.lastKeyboardAllowed
    var locationWarmupInFlight by locationWarmupUiState.locationWarmupInFlight
    var reusableWarmLocationValidation by locationWarmupUiState.reusableWarmLocationValidation
    val securityUiState = rememberExamRuntimeSecurityUiState(
        context = context,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState
    )
    var forcedExitViolationCount by securityUiState.forcedExitViolationCount
    var pendingForcedExitViolation by securityUiState.pendingForcedExitViolation
    var showForcedExitAlarm by securityUiState.showForcedExitAlarm
    var keyboardViolationCount by securityUiState.keyboardViolationCount
    var showKeyboardViolationDialog by securityUiState.showKeyboardViolationDialog
    var overlayViolationCount by securityUiState.overlayViolationCount
    var showOverlayViolationDialog by securityUiState.showOverlayViolationDialog
    var overlayShieldRequested by securityUiState.overlayShieldRequested
    var overlayShieldLastApplySucceeded by securityUiState.overlayShieldLastApplySucceeded
    var overlayShieldLastAppliedAt by securityUiState.overlayShieldLastAppliedAt
    var lastOverlayTrigger by securityUiState.lastOverlayTrigger
    var lastOverlayAt by securityUiState.lastOverlayAt
    var lastOverlayContext by securityUiState.lastOverlayContext
    var lastExamRefreshDecision by securityUiState.lastExamRefreshDecision
    var overlayWindowHasFocus by securityUiState.overlayWindowHasFocus
    var overlayWindowFocusLossPending by securityUiState.overlayWindowFocusLossPending
    var overlayFocusLossConfirmRunnable by securityUiState.overlayFocusLossConfirmRunnable
    var bluetoothPermissionGranted by securityUiState.bluetoothPermissionGranted
    var bluetoothEnabled by securityUiState.bluetoothEnabled
    var accessibilityInspection by securityUiState.accessibilityInspection
    var accessibilityServiceEnabled by securityUiState.accessibilityServiceEnabled
    var adbInspection by securityUiState.adbInspection
    var developerOptionsEnabled by securityUiState.developerOptionsEnabled
    var adbEnabled by securityUiState.adbEnabled
    var rootSecurityStatus by securityUiState.rootSecurityStatus
    var rootDetected by securityUiState.rootDetected
    var selinuxPermissiveWarning by securityUiState.selinuxPermissiveWarning
    var signatureMismatchDetected by securityUiState.signatureMismatchDetected
    var virtualEnvironmentDetected by securityUiState.virtualEnvironmentDetected
    var packageInventoryChangeNonce by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(context) {
        virtualEnvironmentDetected = getVirtualEnvironmentDiagnosticsOnIo(context).detected
    }
    DisposableEffect(context) {
        val unregisterPackageInventoryInvalidation =
            registerPackageInventoryInvalidationReceiver(context) {
                SecurityDetectorCache.invalidateStaticSecurity()
                packageInventoryChangeNonce += 1
            }
        onDispose { unregisterPackageInventoryInvalidation() }
    }
    var tamperDetected by securityUiState.tamperDetected
    var tamperSummary by securityUiState.tamperSummary
    var tamperLastLoggedSummary by securityUiState.tamperLastLoggedSummary
    var integrityTamperDetected by securityUiState.integrityTamperDetected
    var integritySummary by securityUiState.integritySummary
    var integrityPublicSummary by securityUiState.integrityPublicSummary
    var integrityLastLoggedSummary by securityUiState.integrityLastLoggedSummary
    var integrityBaselineFingerprint by securityUiState.integrityBaselineFingerprint
    var bluetoothViolationCount by securityUiState.bluetoothViolationCount
    var showBluetoothViolationDialog by securityUiState.showBluetoothViolationDialog
    var geofenceEvaluation by securityUiState.geofenceEvaluation
    var geofenceSecurityStatus by securityUiState.geofenceSecurityStatus
    var fakeLocationSecurityStatus by securityUiState.fakeLocationSecurityStatus
    LaunchedEffect(context, fakeLocationBypassState) {
        val snapshot = readInitialStaticSecuritySnapshotOnIo(
            context = context,
            forceRefresh = false
        )
        applyInitialStaticSecuritySnapshot(
            snapshot = snapshot,
            securityUiState = securityUiState,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            fixQualityStatus = geofenceSecurityStatus.fixQualityStatus,
            developerOptionsEnabled = developerOptionsEnabled,
            fakeLocationBypassState = fakeLocationBypassState
        )
    }
    val deviceTimeSecurityStatusState = remember(
        deviceTimeBaseline,
        deviceTimeBypassState
    ) {
        mutableStateOf(
            inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = deviceTimeBypassState
            )
        )
    }
    var deviceTimeSecurityStatus by deviceTimeSecurityStatusState
    val lastDeviceTimeDiagnosticKeyState = rememberSaveable { mutableStateOf<String?>(null) }
    var lastDeviceTimeDiagnosticKey by lastDeviceTimeDiagnosticKeyState
    val clipboardUiState = rememberExamRuntimeClipboardUiState(context)
    var clipboardSignature by clipboardUiState.clipboardSignature
    var clipboardDecisionFingerprint by clipboardUiState.clipboardDecisionFingerprint
    var clipboardDecisionSemanticSignature by clipboardUiState.clipboardDecisionSemanticSignature
    var clipboardViolationCount by clipboardUiState.clipboardViolationCount
    var lastClipboardChangeEvent by clipboardUiState.lastClipboardChangeEvent
    var lastClipboardObservedAt by clipboardUiState.lastClipboardObservedAt
    var lastClipboardConfirmedAt by clipboardUiState.lastClipboardConfirmedAt
    var lastClipboardObservedSignature by clipboardUiState.lastClipboardObservedSignature
    var lastClipboardBaselineSemanticSignature by clipboardUiState.lastClipboardBaselineSemanticSignature
    var lastClipboardDetectedSemanticSignature by clipboardUiState.lastClipboardDetectedSemanticSignature
    var lastClipboardDecision by clipboardUiState.lastClipboardDecision
    var clipboardPreBackgroundFingerprint by clipboardUiState.clipboardPreBackgroundFingerprint
    var clipboardPreBackgroundSignature by clipboardUiState.clipboardPreBackgroundSignature
    var clipboardPreBackgroundSemanticSignature by clipboardUiState.clipboardPreBackgroundSemanticSignature
    var clipboardConfirmRunnable by clipboardUiState.clipboardConfirmRunnable
    var clipboardResumeCheckRunnable by clipboardUiState.clipboardResumeCheckRunnable
    var clipboardResumeCheckPending by clipboardUiState.clipboardResumeCheckPending
    var showClipboardViolationDialog by clipboardUiState.showClipboardViolationDialog
    val adminUiState = rememberExamRuntimeAdminUiState(
        context = context,
        payload = payload
    )
    var securityIssueDialogTitle by adminUiState.securityIssueDialogTitle
    var securityIssueDialogMessage by adminUiState.securityIssueDialogMessage
    var exitOnSecurityIssueDialogDismiss by adminUiState.exitOnSecurityIssueDialogDismiss
    var screenPinningBypassTamperLogged by adminUiState.screenPinningBypassTamperLogged
    var accessibilityBypassTamperLogged by adminUiState.accessibilityBypassTamperLogged
    var adbBypassTamperLogged by adminUiState.adbBypassTamperLogged
    var clipboardBypassTamperLogged by adminUiState.clipboardBypassTamperLogged
    var overlayBypassTamperLogged by adminUiState.overlayBypassTamperLogged
    var geofenceBypassTamperLogged by adminUiState.geofenceBypassTamperLogged
    var fakeLocationBypassTamperLogged by adminUiState.fakeLocationBypassTamperLogged
    var deviceTimeBypassTamperLogged by adminUiState.deviceTimeBypassTamperLogged
    var vpnBypassTamperLogged by adminUiState.vpnBypassTamperLogged
    var appSwitchBypassTamperLogged by adminUiState.appSwitchBypassTamperLogged
    var rootBypassTamperLogged by adminUiState.rootBypassTamperLogged
    var lastAppSwitchTrigger by adminUiState.lastAppSwitchTrigger
    var lastAppSwitchAt by adminUiState.lastAppSwitchAt
    var lastAppSwitchContext by adminUiState.lastAppSwitchContext
    var appSwitchSuppressionReason by adminUiState.appSwitchSuppressionReason
    var appSwitchSuppressedUntilElapsedMs by adminUiState.appSwitchSuppressedUntilElapsedMs
    var appSwitchLifecycleResumePending by adminUiState.appSwitchLifecycleResumePending
    var appSwitchFallbackArmedLogged by adminUiState.appSwitchFallbackArmedLogged
    val accessibilityGuardEnabledState = remember {
        mutableStateOf(isExamGuardAccessibilityEnabled(context))
    }
    var accessibilityGuardEnabled by accessibilityGuardEnabledState
    val accessibilityGuardFallbackActiveState = remember { mutableStateOf(false) }
    var accessibilityGuardFallbackActive by accessibilityGuardFallbackActiveState
    val accessibilityGuardLastReasonState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastReason by accessibilityGuardLastReasonState
    val accessibilityGuardLastForeignPackageState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastForeignPackage by accessibilityGuardLastForeignPackageState
    val accessibilityGuardLastEventTypeState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastEventType by accessibilityGuardLastEventTypeState
    val accessibilityGuardLastDetectedAtState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastDetectedAt by accessibilityGuardLastDetectedAtState
    val accessibilityGuardAlarmSeverityState = remember {
        mutableStateOf(ExamAlarmSeverity.Warning.name)
    }
    var accessibilityGuardAlarmSeverity by accessibilityGuardAlarmSeverityState
    var screenPinningAvailable by adminUiState.screenPinningAvailable
    var screenPinningEnabledInSystem by adminUiState.screenPinningEnabledInSystem
    var lockTaskStateBeforePinningRequest by adminUiState.lockTaskStateBeforePinningRequest
    var lockTaskStateAfterPinningRequest by adminUiState.lockTaskStateAfterPinningRequest
    var screenPinningRequestOutcome by adminUiState.screenPinningRequestOutcome
    var screenPinningDialogLikelyShown by adminUiState.screenPinningDialogLikelyShown
    var screenPinningUserActionInference by adminUiState.screenPinningUserActionInference
    var screenPinningActivationDurationMs by adminUiState.screenPinningActivationDurationMs
    var examSessionCancelledByPinningFailure by adminUiState.examSessionCancelledByPinningFailure
    var sendingSection by adminUiState.sendingSection
    var pendingSection by adminUiState.pendingSection
    var bugReportFeedbackTitle by adminUiState.bugReportFeedbackTitle
    var bugReportFeedbackMessage by adminUiState.bugReportFeedbackMessage
    val appStartedAtElapsedMs = adminUiState.appStartedAtElapsedMs
    var examRuntimeMonitoringArmed by adminUiState.examRuntimeMonitoringArmed
    var examSessionStartedAtElapsedMs by adminUiState.examSessionStartedAtElapsedMs
    var lastParticipantCaptureLogKey by adminUiState.lastParticipantCaptureLogKey
    var participantContext by adminUiState.participantContext
    val examDisplayName = payload.examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val alarmSessionIdentity = remember(payload.examName, payload.examUrl, participantContext) {
        buildAlarmSessionIdentity(
            payload = payload,
            participantContext = participantContext
        )
    }
    val appVersionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }
    val overlayRiskResult = OverlayRiskAnalyzer.inspect(
        bypassed = overlayBypassState == OverlayBypassState.Active,
        accessibilityEnabled = accessibilityInspection.blockingServiceActive,
        riskyAccessibilityPackages = accessibilityInspection.riskyPackages,
        violationCount = overlayViolationCount,
        shieldStatus = OverlayShieldStatus(
            supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            requested = overlayShieldRequested,
            lastApplySucceeded = overlayShieldLastApplySucceeded,
            lastApplyAt = overlayShieldLastAppliedAt
        ),
        lastTrigger = lastOverlayTrigger,
        lastDetectedAt = lastOverlayAt,
        lastContext = lastOverlayContext
    )
    val effectiveExamUserAgent = adminSettings.effectiveExamUserAgent()
    val examUserAgentSourceLabel = if (adminSettings.usesDefaultExamUserAgent()) {
        tr("Default", "Default")
    } else {
        tr("Custom", "Custom")
    }
    var diagnosticEvents by adminUiState.diagnosticEvents
    val examAlarmController = remember(context) {
        ExamAlarmController(context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    var reverseEngineeringRefreshCache by runtimeCacheState.reverseEngineeringRefreshCache
    var integrityRefreshCache by runtimeCacheState.integrityRefreshCache
    var lastAlarmAcknowledgeDedupKey by adminUiState.lastAlarmAcknowledgeDedupKey
    var lastAlarmAcknowledgeAtElapsedMs by adminUiState.lastAlarmAcknowledgeAtElapsedMs
    val isKeyboardAllowed = bypassKeyboardPolicy || isAllowedExamKeyboard(context, currentKeyboardPackage)
    val reverseEngineeringTamperBlocking = tamperDetected && !bypassReverseEngineering
    val apkIntegrityTamperBlocking =
        (integrityTamperDetected || signatureMismatchDetected) && !bypassApkIntegrity
    val securityTamperDetected = reverseEngineeringTamperBlocking || apkIntegrityTamperBlocking
    val examGuardArmed = examRuntimeMonitoringArmed || lockTaskRequestPending || examSessionStarted
    val nativeExamFullscreenActive = examGuardArmed || fullScreenCustomView != null
    val networkReadinessStatus =
        if (
            networkUnstableEpisodeStartedElapsedMs != null &&
            baseNetworkReadiness.verdict == NetworkReadinessVerdict.ConnectedStable &&
            baseNetworkReadiness.examStatus.isConnected
        ) {
            baseNetworkReadiness.copy(
                verdict = NetworkReadinessVerdict.Unstable,
                quickFixReason = "unstable"
            )
        } else {
            baseNetworkReadiness
        }
    val networkStatus = networkReadinessStatus.examStatus
    val runtimeDiagnosticsOps = ExamRuntimeDiagnosticsOps(
        context = context,
        coroutineScope = coroutineScope,
        lockTaskBridge = lockTaskBridge,
        mainActivity = mainActivity,
        lowRamProfile = lowRamProfile,
        screenPinningMode = screenPinningMode,
        appSwitchBypassState = appSwitchBypassState,
        effectiveLocationPolicySource = effectiveLocationPolicySource,
        deviceTimeBaseline = deviceTimeBaseline,
        deviceTimeBypassState = deviceTimeBypassState,
        examUrl = payload.examUrl,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState,
        bypassVpn = bypassVpn,
        bypassGeofence = bypassGeofence,
        bypassFakeLocation = bypassFakeLocation,
        warmLocationPolicySignature = warmLocationPolicySignature,
        networkReadinessStatus = networkReadinessStatus,
        baseNetworkReadinessState = baseNetworkReadinessState,
        networkUiState = networkUiState,
        networkTimeline = networkTimeline,
        networkFlapElapsedMs = networkFlapElapsedMs,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        webViewUiState = webViewUiState,
        locationWarmupUiState = locationWarmupUiState,
        deviceTimeSecurityStatusState = deviceTimeSecurityStatusState,
        lastDeviceTimeDiagnosticKeyState = lastDeviceTimeDiagnosticKeyState,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        accessibilityGuardLastReasonState = accessibilityGuardLastReasonState,
        accessibilityGuardLastForeignPackageState = accessibilityGuardLastForeignPackageState,
        accessibilityGuardLastEventTypeState = accessibilityGuardLastEventTypeState,
        accessibilityGuardLastDetectedAtState = accessibilityGuardLastDetectedAtState,
        accessibilityGuardAlarmSeverityState = accessibilityGuardAlarmSeverityState,
        examAlarmController = examAlarmController
    )
    val currentOfflineDurationMs = runtimeDiagnosticsOps.currentOfflineDurationMs
    val offlineRuntimeStatus = runtimeDiagnosticsOps.offlineRuntimeStatus
    val networkTimelinePreview = runtimeDiagnosticsOps.networkTimelinePreview
    val networkUnstableRuntimeStatus = runtimeDiagnosticsOps.networkUnstableRuntimeStatus
    val geofenceRuntimeStatus = runtimeDiagnosticsOps.geofenceRuntimeStatus
    val fakeLocationRuntimeStatus = runtimeDiagnosticsOps.fakeLocationRuntimeStatus
    val overlayShieldStatus = runtimeDiagnosticsOps.overlayShieldStatus
    val clipboardRuntimeStatus = runtimeDiagnosticsOps.clipboardRuntimeStatus
    val appSwitchLockTaskActive = runtimeDiagnosticsOps.appSwitchLockTaskActive
    val appSwitchProtectionMode = runtimeDiagnosticsOps.appSwitchProtectionMode
    val appSwitchStatus = runtimeDiagnosticsOps.appSwitchStatus
    fun currentNetworkPollingIntervalMillis(): Long = runtimeDiagnosticsOps.currentNetworkPollingIntervalMillis()
    fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long = runtimeDiagnosticsOps.currentScreenPinningMonitorIntervalMillis(nowElapsedMs)
    fun currentDiagnosticScreen(): String = runtimeDiagnosticsOps.currentDiagnosticScreen()
    fun clearAppSwitchSuppression() = runtimeDiagnosticsOps.clearAppSwitchSuppression()
    fun setAppSwitchSuppression(
        reason: AppSwitchSuppressionReason,
        durationMs: Long = AppSwitchSuppressionWindowMillis
    ) = runtimeDiagnosticsOps.setAppSwitchSuppression(reason, durationMs)
    fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? = runtimeDiagnosticsOps.currentAppSwitchSuppressionReason()
    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String = runtimeDiagnosticsOps.currentAppSwitchEventDetails(signal, suppressionReason)
    fun currentOverlayEventDetails(
        signal: OverlaySignal,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentOverlayEventDetails(signal, extraContext)
    fun currentInternalDialogReason(): String? = runtimeDiagnosticsOps.currentInternalDialogReason()
    fun recordOverlayEvent(
        code: String,
        signal: OverlaySignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        extraContext: String? = null
    ) = runtimeDiagnosticsOps.recordOverlayEvent(code, signal, level, extraContext)
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = runtimeDiagnosticsOps.recordAction(code, details, level)
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
        if (dpcExamPolicyAppliedForSession) {
            if (startLockTask && dpcRuntimeStatus.deviceOwner && !lockTaskBridge.active()) {
                lockTaskBridge.engage(allowLockTask = true)
            }
            refreshDpcRuntimeStatus()
            return lockTaskBridge.active()
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
        if (startLockTask && result.after.deviceOwner && !lockTaskBridge.active()) {
            lockTaskBridge.engage(allowLockTask = true)
        }
        refreshDpcRuntimeStatus()
        return lockTaskBridge.active()
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
    fun writePreviousSessionBreadcrumb(
        code: String,
        details: String = "-"
    ) = runtimeDiagnosticsOps.writePreviousSessionBreadcrumb(code, details)

    LaunchedEffect(deviceCompatibilityProfile.family, deviceCompatibilityProfile.model) {
        if (deviceCompatibilityProfile.samsungLegacyTablet) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.SamsungLegacyProfileActive,
                details = deviceCompatibilityProfile.diagnosticSummary(),
                level = DiagnosticEventLevel.INFO
            )
        }
    }
    LaunchedEffect(dpcRuntimeStatus.diagnosticSummary()) {
        recordDpcRuntimeStatus()
    }

    fun markTrustedRuntimeChromeAction(reason: String) {
        lastTrustedRuntimeChromeActionElapsedMs = SystemClock.elapsedRealtime()
        lastTrustedRuntimeChromeActionReason = reason
    }
    suspend fun runExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) {
        val host = safeExamServerHost(payload.examUrl)
        if (markChecking) {
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
        examServerStatus = result.status
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
        coroutineScope.launch {
            runExamServerProbe(trigger = trigger, markChecking = markChecking)
        }
    }
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
    fun appendNetworkTimelineEntry(entry: NetworkTimelineEntry) = runtimeDiagnosticsOps.appendNetworkTimelineEntry(entry)
    fun updateNetworkReadiness(source: String) = runtimeDiagnosticsOps.updateNetworkReadiness(source)
    fun applyNetworkReadinessStatus(
        source: String,
        refreshedStatus: NetworkReadinessStatus
    ) = runtimeDiagnosticsOps.applyNetworkReadinessStatus(source, refreshedStatus)
    fun launchNetworkManualRefresh(trigger: String) = runtimeDiagnosticsOps.launchNetworkManualRefresh(trigger)
    suspend fun evaluateLocationSecurityNow(preferFresh: Boolean): SplitLocationSecurityStatus = runtimeDiagnosticsOps.evaluateLocationSecurityNow(preferFresh)
    fun applyGeofenceRuntimeEvaluation(
        geofenceStatus: GeofenceSecurityStatus,
        trigger: String
    ) = runtimeDiagnosticsOps.applyGeofenceRuntimeEvaluation(geofenceStatus, trigger)
    fun applyFakeLocationRuntimeEvaluation(
        fakeLocationStatus: LocationSpoofSecurityStatus,
        trigger: String
    ) = runtimeDiagnosticsOps.applyFakeLocationRuntimeEvaluation(fakeLocationStatus, trigger)
    suspend fun refreshGeofenceStatus(
        preferFresh: Boolean,
        trigger: String,
        allowRuntimeViolation: Boolean
    ): SplitLocationSecurityStatus = runtimeDiagnosticsOps.refreshGeofenceStatus(preferFresh, trigger, allowRuntimeViolation)
    fun buildCurrentWarmLocationValidationKey(): String = runtimeDiagnosticsOps.buildCurrentWarmLocationValidationKey()
    fun invalidateWarmLocationValidationCache() = runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
    suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus = runtimeDiagnosticsOps.resolveStartExamLocationValidation()
    fun launchLocationSecurityManualRefresh(trigger: String) = runtimeDiagnosticsOps.launchLocationSecurityManualRefresh(trigger)
    LaunchedEffect(warmLocationPolicySignature) {
        invalidateWarmLocationValidationCache()
    }
    LaunchedEffect(examSessionStarted, payload.examUrl) {
        if (!examSessionStarted) {
            examServerStatus = ExamServerFooterStatus.Checking
            return@LaunchedEffect
        }
        var firstProbe = true
        while (true) {
            runExamServerProbe(
                trigger = if (firstProbe) "exam_start" else "periodic",
                markChecking = examServerStatus == ExamServerFooterStatus.Checking
            )
            firstProbe = false
            delay(examServerProbeIntervalMillis(lowRamProfile))
        }
    }

    // Auto-reload WebView when network recovers from offline while an error page is displayed.
    // This eliminates the need for students to manually press "Muat Ulang" after a brief
    // network hiccup — the exam page recovers automatically once connectivity is restored.
    LaunchedEffect(examSessionStarted, networkStatus.isConnected, webViewErrorMessage) {
        if (!examSessionStarted || !networkStatus.isConnected || webViewErrorMessage == null) {
            return@LaunchedEffect
        }
        // Wait a short stabilization period to ensure the connection is truly back
        delay(1_500L)
        // Double-check: still connected and still in error state
        if (networkStatus.isConnected && webViewErrorMessage != null) {
            recordAction(
                "WEBVIEW_AUTO_RELOAD_ON_RECOVERY",
                "error=${webViewErrorMessage?.take(80)} | transport=${networkReadinessStatus.transportLabel}",
                DiagnosticEventLevel.INFO
            )
            webViewErrorMessage = null
            loadingProgress = 0.05f
            launchExamServerProbe("network_recovery", true)
            webViewInstance?.let { webView ->
                webView.loadExamUrlSafely(payload.examUrl)
                webView.requestedExamUrl = payload.examUrl
            }
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            geofencePermissionRequestInFlight = false
            invalidateWarmLocationValidationCache()
            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasFineLocationPermission(context)
            val anyGranted = result.values.any { it } || hasLocationPermissionForWifi(context)
            val preciseRequiredForStart = geofenceEnabled && !bypassGeofence
            val permissionReadyForStart = if (preciseRequiredForStart) fineGranted else anyGranted
            if (permissionReadyForStart && pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                retryStartExamAfterLocationPermissionGrant = true
            } else if (pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                val blockedByGeofencePrecision = preciseRequiredForStart && anyGranted
                recordAction(
                    code = when {
                        blockedByGeofencePrecision -> "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED"
                        preciseRequiredForStart -> "START_EXAM_BLOCKED_GEOFENCE_PERMISSION"
                        else -> "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION"
                    },
                    details = when {
                        blockedByGeofencePrecision -> "reason=approximate_only"
                        anyGranted -> "reason=permission_not_ready"
                        else -> "reason=permission_denied"
                    },
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    if (blockedByGeofencePrecision) "Precise Location Required" else "Location Permission Required",
                    if (blockedByGeofencePrecision) "Lokasi Presisi Diperlukan" else "Izin Lokasi Diperlukan"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    when {
                        blockedByGeofencePrecision ->
                            "Precise location must be granted before the exam can start."
                        preciseRequiredForStart ->
                            "Location permission must be granted before the exam can start."
                        else ->
                            "Location access is required so anti-fake-location can validate the exam before it starts."
                    },
                    when {
                        blockedByGeofencePrecision ->
                            "Lokasi presisi harus diberikan sebelum ujian bisa dimulai."
                        preciseRequiredForStart ->
                            "Izin lokasi harus diberikan sebelum ujian bisa dimulai."
                        else ->
                            "Akses lokasi wajib tersedia agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                    }
                )
            } else {
                launchLocationSecurityManualRefresh(trigger = "location_permission_quick_fix")
            }
        }
    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            bluetoothPermissionGranted = granted || !requiresBluetoothExamPermission()
            bluetoothEnabled = if (bluetoothPermissionGranted) {
                isBluetoothEnabledForExam(context)
            } else {
                false
            }
        }

    val runtimeMonitoringOps = ExamRuntimeMonitoringOps(
        context = context,
        componentActivity = componentActivity,
        coroutineScope = coroutineScope,
        lockTaskBridge = lockTaskBridge,
        lowRamProfile = lowRamProfile,
        screenPinningMode = screenPinningMode,
        uiLanguage = uiLanguage,
        mainActivity = mainActivity,
        examAlarmController = examAlarmController,
        webViewUiState = webViewUiState,
        runtimeCacheState = runtimeCacheState,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        clipboardBypassState = clipboardBypassState,
        bypassClipboard = bypassClipboard,
        clipboardMainHandler = clipboardMainHandler,
        overlayMainHandler = overlayMainHandler,
        networkFlapElapsedMs = networkFlapElapsedMs,
        networkTimeline = networkTimeline,
        locationWarmupUiState = locationWarmupUiState,
        exitCleanupState = ExamRuntimeExitCleanupStateAccess(
            requested = exitSessionClearRequestedState,
            deferred = exitSessionClearDeferredState
        ),
        callbacks = ExamRuntimeMonitoringCallbacks(
            currentAppSwitchEventDetails = { signal -> currentAppSwitchEventDetails(signal) },
            clearAppSwitchSuppression = ::clearAppSwitchSuppression,
            clearDpcExamPoliciesForSession = ::clearDpcExamPoliciesForSession,
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            }
        )
    )
    fun armExamRuntimeMonitoring(reason: String) = runtimeMonitoringOps.armExamRuntimeMonitoring(reason)
    fun disarmExamRuntimeMonitoring() = runtimeMonitoringOps.disarmExamRuntimeMonitoring()
    fun recordAppSwitchEvent(
        code: String,
        signal: AppSwitchSignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        updateLastDetectedAt: Boolean = true
    ) = runtimeMonitoringOps.recordAppSwitchEvent(code, signal, level, updateLastDetectedAt)
    fun acknowledgeRuntimeAlarm(
        type: AlarmAcknowledgeType,
        violationCount: Int,
        buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
        onUiAcknowledge: () -> Unit
    ) = runtimeMonitoringOps.acknowledgeRuntimeAlarm(type, violationCount, buildPayload, onUiAcknowledge)
    fun confirmClipboardViolation(
        snapshot: ClipboardSnapshot,
        decision: ClipboardChangeDecision,
        eventSuffix: String,
        updateObservedSnapshot: Boolean,
        baselineSemanticSignatureOverride: String? = null
    ) = runtimeMonitoringOps.confirmClipboardViolation(snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride)
    fun armClipboardResumeCheck(reason: String) = runtimeMonitoringOps.armClipboardResumeCheck(reason)
    fun applyFatalSecuritySignal(signal: FatalSecuritySignal) = runtimeMonitoringOps.applyFatalSecuritySignal(signal)
    fun refreshReverseEngineeringStatus() = runtimeMonitoringOps.refreshReverseEngineeringStatus()
    fun refreshIntegrityGuard() = runtimeMonitoringOps.refreshIntegrityGuard()
    suspend fun refreshReverseEngineeringStatusOnDetector() =
        runtimeMonitoringOps.refreshReverseEngineeringStatusOnDetector()
    suspend fun refreshIntegrityGuardOnDetector() =
        runtimeMonitoringOps.refreshIntegrityGuardOnDetector()
    fun hideSystemKeyboard() = runtimeMonitoringOps.hideSystemKeyboard()
    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback?) = runtimeMonitoringOps.showCustomView(view, callback)
    fun hideCustomView() = runtimeMonitoringOps.hideCustomView()
    fun cleanupActiveExamWebViewInstance() = runtimeMonitoringOps.cleanupActiveExamWebViewInstance()
    suspend fun clearExamSessionOnExit(reason: String, waitForResult: Boolean): Result<Unit> =
        runtimeMonitoringOps.clearExamSessionOnExit(reason, waitForResult)
    fun launchExitSessionClearBestEffort(reason: String) =
        runtimeMonitoringOps.launchExitSessionClearBestEffort(reason)
    fun handleWebViewRendererGone(
        view: SecureExamWebView?,
        didCrash: Boolean,
        rendererPriorityAtExit: Int?
    ): Boolean = runtimeMonitoringOps.handleWebViewRendererGone(view, didCrash, rendererPriorityAtExit)
    fun shouldIgnoreStaleWebViewCallback(callbackName: String, view: WebView?): Boolean =
        runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback(callbackName, view)
    fun handleRuntimeTrimMemory(level: Int) = runtimeMonitoringOps.handleRuntimeTrimMemory(level)
    RuntimeRecoveryAndMemoryEffects(
        pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
        pendingRecoveryEventDetails = pendingRecoveryEventDetails,
        examSessionRecoveryNonce = examSessionRecoveryNonce,
        recordInfoAction = { code, details -> recordAction(code = code, details = details) },
        onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
        onRecoveryEventConsumed = onRecoveryEventConsumed,
        refreshReverseEngineeringStatus = ::refreshReverseEngineeringStatus,
        refreshIntegrityGuard = ::refreshIntegrityGuard,
        onSimulateRendererGone = {
            handleWebViewRendererGone(
                view = webViewInstance,
                didCrash = false,
                rendererPriorityAtExit = null
            )
        },
        onTrimMemory = ::handleRuntimeTrimMemory
    )

    val keyboardBridge: ExamKeyboardBridge = remember {
        ExamKeyboardBridge(
            onEditableFocusChangedCallback = { focused ->
                if (hasEditableFocus != focused) {
                    hasEditableFocus = focused
                }
                val shouldShowBuiltInKeyboard = useBuiltInExamKeyboard && focused
                if (showBuiltInExamKeyboard != shouldShowBuiltInKeyboard) {
                    showBuiltInExamKeyboard = shouldShowBuiltInKeyboard
                }
                if (shouldShowBuiltInKeyboard) {
                    hideSystemKeyboard()
                }
            }
        )
    }
    val participantCaptureBridge = ExamParticipantCaptureBridge { rawPayload, sourceKey ->
        val result = parseExamParticipantContext(rawPayload, sourceKey)
        when (result) {
            is ExamParticipantCaptureResult.Captured -> {
                val capturedContext = result.context
                if (participantContext != capturedContext) {
                    participantContext = capturedContext
                }
                val details = capturedContext.diagnosticSummary()
                if (lastParticipantCaptureLogKey != "captured|$details") {
                    lastParticipantCaptureLogKey = "captured|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Ignored -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "ignored|$details") {
                    lastParticipantCaptureLogKey = "ignored|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_IGNORED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Failed -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "failed|$details") {
                    lastParticipantCaptureLogKey = "failed|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURE_FAILED",
                        details = details,
                        level = DiagnosticEventLevel.ERROR
                    )
                }
            }
        }
    }
    val nativeFullscreenBridge = remember(mainActivity) {
        ExamNativeFullscreenBridge {
            val hostActivity = mainActivity ?: return@ExamNativeFullscreenBridge false
            hostActivity.setExamLockMode(enabled = true, allowLockTask = false)
            true
        }
    }

    val runtimeSecurityOps = ExamRuntimeSecurityOps(
        context = context,
        coroutineScope = coroutineScope,
        lockTaskBridge = lockTaskBridge,
        uiLanguage = uiLanguage,
        mainActivity = mainActivity,
        adminSettings = adminSettings,
        payload = payload,
        participantContext = participantContext,
        lowRamProfile = lowRamProfile,
        screenPinningMode = screenPinningMode,
        accessibilityBypassState = accessibilityBypassState,
        overlayBypassState = overlayBypassState,
        appSwitchBypassState = appSwitchBypassState,
        adbBypassState = adbBypassState,
        rootBypassState = rootBypassState,
        deviceTimeBypassState = deviceTimeBypassState,
        vpnBypassState = vpnBypassState,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        networkUiState = networkUiState,
        dpcRuntimeStatusProvider = { dpcRuntimeStatus },
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        accessibilityGuardLastReasonState = accessibilityGuardLastReasonState,
        accessibilityGuardLastForeignPackageState = accessibilityGuardLastForeignPackageState,
        accessibilityGuardLastEventTypeState = accessibilityGuardLastEventTypeState,
        accessibilityGuardLastDetectedAtState = accessibilityGuardLastDetectedAtState,
        accessibilityGuardAlarmSeverityState = accessibilityGuardAlarmSeverityState,
        examAlarmController = examAlarmController,
        refreshIntegrityGuard = ::refreshIntegrityGuard
    )
    fun refreshKeyboardSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshKeyboardSecurity(triggerViolation)
    fun refreshBluetoothSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshBluetoothSecurity(triggerViolation)
    fun refreshScreenPinningDiagnostics() = runtimeSecurityOps.refreshScreenPinningDiagnostics()
    fun checkSignatureIntegrity(triggerViolation: Boolean): SignatureIntegrityResult = runtimeSecurityOps.checkSignatureIntegrity(triggerViolation)
    fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshDeviceIntegritySecurity(triggerViolation)
    fun launchTelegramSectionReport(section: DiagnosticSection) = runtimeSecurityOps.launchTelegramSectionReport(section)

    class StartExamController {
        private fun applyStartExamBlockMessage(
            message: StartExamBlockMessage,
            level: DiagnosticEventLevel = DiagnosticEventLevel.WARNING
        ) {
            applyExamRuntimeStartBlockMessage(
                message = message,
                level = level,
                callbacks = ExamRuntimeStartBlockCallbacks(
                    recordAction = { code, details, eventLevel ->
                        recordAction(code = code, details = details, level = eventLevel)
                    },
                    setSecurityIssueDialogTitle = { securityIssueDialogTitle = it },
                    setSecurityIssueDialogMessage = { securityIssueDialogMessage = it }
                )
            )
        }

        fun resetPreparationSecurityEpisodes() {
            resetStartExamPreparationSecurityEpisodes(flowUiState)
        }

        fun finalizeExamSessionStart(lockTaskAlreadyActive: Boolean) {
            applyDpcExamPoliciesForStart(startLockTask = false)
            finalizeStartExamSession(
                context = context,
                lockTaskBridge = lockTaskBridge,
                flowUiState = flowUiState,
                adminUiState = adminUiState,
                clipboardUiState = clipboardUiState,
                lockTaskAlreadyActive = lockTaskAlreadyActive,
                hideSystemKeyboard = ::hideSystemKeyboard
            )
        }

        suspend fun prepareCleanExamWebViewSessionForStart(): Boolean {
            return prepareCleanExamWebViewSessionForStart(
                context = context,
                existingWebView = webViewInstance,
                lowRamProfile = lowRamProfile,
                flowUiState = flowUiState,
                adminUiState = adminUiState,
                uiLanguage = uiLanguage,
                recordAction = { code, details, level ->
                    recordAction(code = code, details = details, level = level)
                },
                onRecoveryStateIdle = { examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle }
            )
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
                    resetPreparationSecurityEpisodes = this::resetPreparationSecurityEpisodes,
                    prepareCleanExamWebViewSessionForStart = this::prepareCleanExamWebViewSessionForStart,
                    armExamRuntimeMonitoring = ::armExamRuntimeMonitoring,
                    finalizeExamSessionStart = this::finalizeExamSessionStart,
                    ensureDeviceOwnerLockTaskActive = {
                        applyDpcExamPoliciesForStart(startLockTask = true)
                    },
                    clearAppSwitchSuppression = ::clearAppSwitchSuppression,
                    setAppSwitchSuppression = { reason -> setAppSwitchSuppression(reason) },
                    applyStartExamBlockMessage = this::applyStartExamBlockMessage,
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
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle
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
                    refreshReverseEngineeringStatus = ::refreshReverseEngineeringStatusOnDetector,
                    refreshIntegrityGuard = ::refreshIntegrityGuardOnDetector,
                    refreshScreenPinningDiagnostics = ::refreshScreenPinningDiagnostics,
                    refreshKeyboardSecurity = ::refreshKeyboardSecurity,
                    refreshBluetoothSecurity = ::refreshBluetoothSecurity,
                    refreshDeviceIntegritySecurity = ::refreshDeviceIntegritySecurity,
                    applyStartExamBlockMessage = this::applyStartExamBlockMessage,
                    refreshDeviceTimeSecurity = { trigger, emitDiagnosticEvent ->
                        refreshDeviceTimeSecurity(
                            trigger = trigger,
                            emitDiagnosticEvent = emitDiagnosticEvent
                        )
                    },
                    applyNetworkReadinessStatus = ::applyNetworkReadinessStatus,
                    checkSignatureIntegrity = ::checkSignatureIntegrity,
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
                    refreshDpcRuntimeStatus = ::refreshDpcRuntimeStatus,
                    requestBluetoothPermission = {
                        bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
                    },
                    requestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
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
                            callbacks = ExamRuntimeStartLocationValidationCallbacks(
                                isGeofenceStartValidationInFlight = { geofenceStartValidationInFlight },
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
                                applyStartExamBlockMessage = this::applyStartExamBlockMessage,
                                refreshDeviceTimeSecurity = { trigger, emitDiagnosticEvent ->
                                    refreshDeviceTimeSecurity(
                                        trigger = trigger,
                                        emitDiagnosticEvent = emitDiagnosticEvent
                                    )
                                },
                                completeStartExamSessionAfterPrechecks = this::completeStartExamSessionAfterPrechecks,
                                debugLogExamStart = ::debugLogExamStart
                            )
                        )
                    },
                    debugLogExamStart = ::debugLogExamStart
                )
            )
        }

    }
    val startExamController = StartExamController()

    LaunchedEffect(retryStartExamAfterLocationPermissionGrant) {
        if (retryStartExamAfterLocationPermissionGrant) {
            retryStartExamAfterLocationPermissionGrant = false
            startExamController.startExamSession()
        }
    }

    fun sendBuiltInKeyboardText(rawText: String) {
        sendBuiltInExamKeyboardText(
            webView = webViewInstance,
            rawText = rawText,
            shiftEnabled = builtInKeyboardShiftEnabled,
            updateShiftEnabled = { builtInKeyboardShiftEnabled = it },
            hideSystemKeyboard = ::hideSystemKeyboard
        )
    }

    fun sendBuiltInKeyboardBackspace() {
        sendBuiltInExamKeyboardBackspace(webViewInstance, ::hideSystemKeyboard)
    }

    fun sendKeyboardArrowLeft() {
        sendExamKeyboardArrowLeft(webViewInstance)
    }

    fun sendKeyboardArrowRight() {
        sendExamKeyboardArrowRight(webViewInstance)
    }

    fun sendBuiltInKeyboardEnter() {
        sendBuiltInExamKeyboardEnter(webViewInstance, ::hideSystemKeyboard)
    }

    fun handleScreenPinningTransitionInterrupted() {
        handleExamRuntimeScreenPinningTransitionInterrupted(
            lockTaskRequestPending = lockTaskRequestPending,
            examSessionStarted = examSessionStarted,
            lockTaskBridge = lockTaskBridge,
            pinningActivationStartedAtElapsedMs = pinningActivationStartedAtElapsedMs,
            pinningSuppressedTransitionCount = pinningSuppressedTransitionCount,
            isIndonesian = isIndonesian,
            pinningActivationPurpose = pinningActivationPurpose,
            setPinningActivationState = { pinningActivationState = it },
            setLockTaskStateAfterPinningRequest = { lockTaskStateAfterPinningRequest = it },
            setScreenPinningDialogLikelyShown = { screenPinningDialogLikelyShown = it },
            setPinningSuppressedTransitionCount = { pinningSuppressedTransitionCount = it },
            setScreenPinningMessage = { screenPinningMessage = it },
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            }
        )
    }

    RuntimeSetupEffects(
        context = context,
        mainActivity = mainActivity,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        examSessionStarted = examSessionStarted,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        webViewInstance = webViewInstance,
        nativeFullscreenBridge = nativeFullscreenBridge,
        refreshScreenPinningDiagnostics = ::refreshScreenPinningDiagnostics,
        refreshKeyboardSecurity = ::refreshKeyboardSecurity,
        refreshBluetoothSecurity = ::refreshBluetoothSecurity,
        refreshDeviceIntegritySecurity = ::refreshDeviceIntegritySecurity,
        updateBluetoothPermissionGranted = { bluetoothPermissionGranted = it },
        updateUseBuiltInExamKeyboard = { useBuiltInExamKeyboard = it },
        updateShowBuiltInExamKeyboard = { showBuiltInExamKeyboard = it },
        cleanupActiveExamWebViewInstance = ::cleanupActiveExamWebViewInstance
    )

    PreparationLocationWarmupEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        geofenceEnabled = geofenceEnabled,
        warmLocationPolicySignature = warmLocationPolicySignature,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState,
        geofencePermissionRequestInFlight = geofencePermissionRequestInFlight,
        geofenceStartValidationInFlight = geofenceStartValidationInFlight,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        webViewSessionResetInFlight = webViewSessionResetInFlight,
        locationWarmupInFlight = locationWarmupInFlight,
        warmupIntervalMillis = PreparationLocationWarmupIntervalMillis *
            lowRamProfile.slowPollingMultiplier,
        updateLocationWarmupInFlight = { locationWarmupInFlight = it },
        updateReusableWarmLocationValidation = { reusableWarmLocationValidation = it },
        updateLastGeofenceRefreshAt = { lastGeofenceRefreshAt = it },
        refreshGeofenceStatus = ::refreshGeofenceStatus
    )

    BypassTamperLoggingEffects(
        adminSettings = adminSettings,
        screenPinningBypassTamperLogged = screenPinningBypassTamperLogged,
        updateScreenPinningBypassTamperLogged = { screenPinningBypassTamperLogged = it },
        accessibilityBypassTamperLogged = accessibilityBypassTamperLogged,
        updateAccessibilityBypassTamperLogged = { accessibilityBypassTamperLogged = it },
        adbBypassTamperLogged = adbBypassTamperLogged,
        updateAdbBypassTamperLogged = { adbBypassTamperLogged = it },
        clipboardBypassTamperLogged = clipboardBypassTamperLogged,
        updateClipboardBypassTamperLogged = { clipboardBypassTamperLogged = it },
        overlayBypassTamperLogged = overlayBypassTamperLogged,
        updateOverlayBypassTamperLogged = { overlayBypassTamperLogged = it },
        geofenceBypassTamperLogged = geofenceBypassTamperLogged,
        updateGeofenceBypassTamperLogged = { geofenceBypassTamperLogged = it },
        fakeLocationBypassTamperLogged = fakeLocationBypassTamperLogged,
        updateFakeLocationBypassTamperLogged = { fakeLocationBypassTamperLogged = it },
        deviceTimeBypassTamperLogged = deviceTimeBypassTamperLogged,
        updateDeviceTimeBypassTamperLogged = { deviceTimeBypassTamperLogged = it },
        vpnBypassTamperLogged = vpnBypassTamperLogged,
        updateVpnBypassTamperLogged = { vpnBypassTamperLogged = it },
        appSwitchBypassTamperLogged = appSwitchBypassTamperLogged,
        updateAppSwitchBypassTamperLogged = { appSwitchBypassTamperLogged = it },
        rootBypassTamperLogged = rootBypassTamperLogged,
        updateRootBypassTamperLogged = { rootBypassTamperLogged = it },
        recordAction = ::recordAction
    )

    DisposableEffect(mainActivity) {
        onDispose {
            mainActivity?.setExamLockMode(enabled = false, allowLockTask = false)
        }
    }

    RuntimeAppSwitchFallbackLoggingEffect(
        examGuardArmed = examGuardArmed,
        appSwitchStatus = appSwitchStatus,
        screenPinningMode = screenPinningMode,
        appSwitchFallbackArmedLogged = appSwitchFallbackArmedLogged,
        updateAppSwitchFallbackArmedLogged = { appSwitchFallbackArmedLogged = it },
        recordAction = ::recordAction
    )

    AccessibilityExamGuardViolationEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        onViolation = ::handleAccessibilityGuardViolation
    )

    AccessibilityExamGuardLivenessEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        recordAction = ::recordAction
    )

    RuntimeDisposeCleanupEffect(
        examSessionStarted = examSessionStarted,
        lockTaskRequestPending = lockTaskRequestPending,
        lockTaskBridge = lockTaskBridge,
        cleanupActiveExamWebViewInstance = ::cleanupActiveExamWebViewInstance,
        launchExitSessionClearBestEffort = {
            launchExitSessionClearBestEffort("runtime_dispose")
        },
        clearDpcExamPoliciesForSession = ::clearDpcExamPoliciesForSession,
        disarmAccessibilityGuard = { AccessibilityExamGuardStore.disarm(context) },
        stopAlarm = examAlarmController::stop
    )

    RuntimeScreenPinningActivationEffect(
        mainActivity = mainActivity,
        lockTaskBridge = lockTaskBridge,
        isIndonesian = isIndonesian,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        coroutineScope = coroutineScope,
        recordAction = ::recordAction,
        clearAppSwitchSuppression = ::clearAppSwitchSuppression,
        disarmExamRuntimeMonitoring = ::disarmExamRuntimeMonitoring,
        resetPreparationSecurityEpisodes = startExamController::resetPreparationSecurityEpisodes,
        prepareCleanExamWebViewSessionForStart = startExamController::prepareCleanExamWebViewSessionForStart,
        finalizeExamSessionStart = startExamController::finalizeExamSessionStart
    )

    RuntimeScreenPinningMonitorEffect(
        mainActivity = mainActivity,
        screenPinningMode = screenPinningMode,
        examSessionStarted = examSessionStarted,
        examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
        lockTaskRequestPending = lockTaskRequestPending,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        exitOnSecurityIssueDialogDismiss = exitOnSecurityIssueDialogDismiss,
        lockTaskBridge = lockTaskBridge,
        isIndonesian = isIndonesian,
        deviceQuirkProfile = deviceQuirkProfile,
        currentScreenPinningMonitorIntervalMillis = ::currentScreenPinningMonitorIntervalMillis,
        recordAction = ::recordAction,
        applyFatalSecuritySignal = ::applyFatalSecuritySignal
    )

    RuntimePrimaryGuardEffects(
        mainActivity = mainActivity,
        examGuardArmed = examGuardArmed,
        overlayBypassState = overlayBypassState,
        clipboardBypassState = clipboardBypassState,
        bypassClipboard = bypassClipboard,
        appSwitchRuntimeMonitoringActive = appSwitchStatus.runtimeMonitoringActive,
        appSwitchProtectionMode = appSwitchStatus.protectionMode,
        appSwitchLockTaskActive = appSwitchStatus.lockTaskActive,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        fullScreenCustomView = fullScreenCustomView,
        showOfflineWarningDialog = showOfflineWarningDialog,
        showExitExamDialog = showExitExamDialog,
        pendingSection = pendingSection,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        deviceQuirkProfile = deviceQuirkProfile,
        currentLastTrustedRuntimeChromeActionElapsedMs = {
            lastTrustedRuntimeChromeActionElapsedMsState.value
        },
        currentLastTrustedRuntimeChromeActionReason = {
            lastTrustedRuntimeChromeActionReasonState.value
        },
        currentAppSwitchSuppressionReason = ::currentAppSwitchSuppressionReason,
        currentAppSwitchEventDetails = { signal, suppressionReason ->
            currentAppSwitchEventDetails(
                signal = signal,
                suppressionReason = suppressionReason
            )
        },
        currentOverlayEventDetails = { signal, extraContext ->
            currentOverlayEventDetails(
                signal = signal,
                extraContext = extraContext
            )
        },
        currentInternalDialogReason = ::currentInternalDialogReason,
        recordAction = ::recordAction,
        recordAppSwitchEvent = ::recordAppSwitchEvent,
        onScreenPinningTransitionInterrupted = ::handleScreenPinningTransitionInterrupted,
        armClipboardResumeCheck = ::armClipboardResumeCheck,
        startAlarm = examAlarmController::start
    )

    RuntimeStaticSecurityEffects(
        context = context,
        mainActivity = mainActivity,
        examSessionStarted = examSessionStarted,
        bypassScreenRecorder = bypassScreenRecorder,
        bypassDisplayMirror = bypassDisplayMirror,
        bypassMultiWindow = bypassMultiWindow,
        bypassOverlay = bypassOverlay,
        packageInventoryChangeNonce = packageInventoryChangeNonce,
        securityUiState = securityUiState,
        recordAction = ::recordAction,
        startAlarm = examAlarmController::start
    )

    RuntimeHostActivityLifecycleEffect(
        context = context,
        componentActivity = componentActivity,
        coroutineScope = coroutineScope,
        examAlarmController = examAlarmController,
        examGuardArmed = examGuardArmed,
        geofenceEnabled = geofenceEnabled,
        clipboardBypassState = clipboardBypassState,
        bypassClipboard = bypassClipboard,
        appSwitchRuntimeMonitoringActive = appSwitchStatus.runtimeMonitoringActive,
        appSwitchSuppressionReason = appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs = appSwitchSuppressedUntilElapsedMs,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        accessibilityGuardLastReasonState = accessibilityGuardLastReasonState,
        accessibilityGuardLastForeignPackageState = accessibilityGuardLastForeignPackageState,
        accessibilityGuardLastEventTypeState = accessibilityGuardLastEventTypeState,
        accessibilityGuardLastDetectedAtState = accessibilityGuardLastDetectedAtState,
        accessibilityGuardAlarmSeverityState = accessibilityGuardAlarmSeverityState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        currentAppSwitchSuppressionReason = ::currentAppSwitchSuppressionReason,
        currentAppSwitchEventDetails = ::currentAppSwitchEventDetails,
        recordAction = ::recordAction,
        recordAppSwitchEvent = ::recordAppSwitchEvent,
        armClipboardResumeCheck = ::armClipboardResumeCheck,
        refreshReverseEngineeringStatus = ::refreshReverseEngineeringStatus,
        refreshKeyboardSecurity = ::refreshKeyboardSecurity,
        refreshBluetoothSecurity = ::refreshBluetoothSecurity,
        refreshDeviceIntegritySecurity = ::refreshDeviceIntegritySecurity,
        refreshDeviceTimeSecurity = { trigger ->
            refreshDeviceTimeSecurity(trigger = trigger)
        },
        refreshGeofenceStatus = { preferFresh, trigger, allowRuntimeViolation ->
            refreshGeofenceStatus(
                preferFresh = preferFresh,
                trigger = trigger,
                allowRuntimeViolation = allowRuntimeViolation
            )
            Unit
        },
        confirmClipboardViolation = { snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride ->
            confirmClipboardViolation(
                snapshot = snapshot,
                decision = decision,
                eventSuffix = eventSuffix,
                updateObservedSnapshot = updateObservedSnapshot,
                baselineSemanticSignatureOverride = baselineSemanticSignatureOverride
            )
        },
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    RuntimeLocationAndClipboardEffects(
        context = context,
        deviceTimeBaseline = deviceTimeBaseline,
        deviceTimeBypassState = deviceTimeBypassState,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceEnabled = geofenceEnabled,
        bypassGeofence = bypassGeofence,
        bypassFakeLocation = bypassFakeLocation,
        examGuardArmed = examGuardArmed,
        bypassClipboard = bypassClipboard,
        clipboardBypassState = clipboardBypassState,
        bypassBluetooth = bypassBluetooth,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        clipboardMainHandler = clipboardMainHandler,
        refreshDeviceTimeSecurity = ::refreshDeviceTimeSecurity,
        refreshGeofenceStatus = { preferFresh, trigger, allowRuntimeViolation ->
            refreshGeofenceStatus(
                preferFresh = preferFresh,
                trigger = trigger,
                allowRuntimeViolation = allowRuntimeViolation
            )
            Unit
        },
        confirmClipboardViolation = { snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride ->
            confirmClipboardViolation(
                snapshot = snapshot,
                decision = decision,
                eventSuffix = eventSuffix,
                updateObservedSnapshot = updateObservedSnapshot,
                baselineSemanticSignatureOverride = baselineSemanticSignatureOverride
            )
        },
        examAlarmController = examAlarmController,
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    RuntimeConnectivityEffects(
        context = context,
        examSessionStarted = examSessionStarted,
        networkReadinessStatus = networkReadinessStatus,
        baseNetworkReadiness = baseNetworkReadiness,
        networkUiState = networkUiState,
        batteryStatusState = batteryStatusState,
        networkMainHandler = networkMainHandler,
        updateNetworkReadiness = ::updateNetworkReadiness,
        currentNetworkPollingIntervalMillis = ::currentNetworkPollingIntervalMillis,
        recordAction = ::recordAction,
        currentNetworkEventDetails = ::currentNetworkEventDetails,
        clearNetworkFlapHistory = { networkFlapElapsedMs.clear() },
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    BackHandler {
        if (fullScreenCustomView != null) {
            hideCustomView()
            return@BackHandler
        }
        val webView = webViewInstance
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            showExitExamDialog = true
        }
    }

    val preparationActionOps = ExamRuntimePreparationActionOps(
        context = context,
        activity = activity,
        uiLanguage = uiLanguage,
        isIndonesian = isIndonesian,
        adminSettings = adminSettings,
        officialApkUrl = officialApkUrl,
        lockTaskBridge = lockTaskBridge,
        screenPinningMode = screenPinningMode,
        vpnBypassState = vpnBypassState,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        adminUiState = adminUiState,
        networkUiState = networkUiState,
        webViewUiState = webViewUiState,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        runtimeSecurityOps = runtimeSecurityOps,
        runtimeMonitoringOps = runtimeMonitoringOps,
        examAlarmController = examAlarmController,
        launchBluetoothPermission = {
            bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
        },
        launchLocationPermission = { permissions ->
            locationPermissionLauncher.launch(permissions)
        },
        incrementWebViewCompatibilityRefreshKey = { webViewCompatibilityRefreshKey += 1 },
        debugLogExamStart = ::debugLogExamStart
    )

    fun handleChooseKeyboard() = preparationActionOps.handleChooseKeyboard()
    fun handleOpenKeyboardSettings() = preparationActionOps.handleOpenKeyboardSettings()
    fun handleGrantBluetoothPermission() = preparationActionOps.handleGrantBluetoothPermission()
    fun handleOpenBluetoothSettings() = preparationActionOps.handleOpenBluetoothSettings()
    fun handleOpenAccessibilitySettings() = preparationActionOps.handleOpenAccessibilitySettings()
    fun handleOpenOverlayAccessibilitySettings() = preparationActionOps.handleOpenOverlayAccessibilitySettings()
    fun handleOpenDeveloperOptionsSettings() = preparationActionOps.handleOpenDeveloperOptionsSettings()
    fun handleRequestLocationPermission() = preparationActionOps.handleRequestLocationPermission()
    fun handleOpenLocationServicesSettings() = preparationActionOps.handleOpenLocationServicesSettings()
    fun handleRefreshLocationSecurity() = preparationActionOps.handleRefreshLocationSecurity()
    fun handleOpenGeofenceMapViewer() = preparationActionOps.handleOpenGeofenceMapViewer()
    fun handleOpenInternetSettings() = preparationActionOps.handleOpenInternetSettings()
    fun handleOpenVpnSettings() = preparationActionOps.handleOpenVpnSettings()
    fun handleOpenDateTimeSettings() = preparationActionOps.handleOpenDateTimeSettings()
    fun handleOpenWifiSettings() = preparationActionOps.handleOpenWifiSettings()
    fun handleOpenCellularSettings() = preparationActionOps.handleOpenCellularSettings()
    fun handleOpenAirplaneModeSettings() = preparationActionOps.handleOpenAirplaneModeSettings()
    fun handleRefreshNetworkStatus() = preparationActionOps.handleRefreshNetworkStatus()
    fun handleOpenFakeLocationDeveloperOptionsSettings() =
        preparationActionOps.handleOpenFakeLocationDeveloperOptionsSettings()
    fun handleOpenScreenPinningSettings() = preparationActionOps.handleOpenScreenPinningSettings()
    fun handleStartScreenPinning() = preparationActionOps.handleStartScreenPinning()
    fun handleOpenOverlaySettings() = preparationActionOps.handleOpenOverlaySettings()
    fun handleOpenAppSettings() = preparationActionOps.handleOpenAppSettings()
    fun handleOpenCastSettings() = preparationActionOps.handleOpenCastSettings()
    fun handleOpenWebViewProviderSettings() = preparationActionOps.handleOpenWebViewProviderSettings()
    fun handleReinstallOfficialApk() = preparationActionOps.handleReinstallOfficialApk()
    fun refreshPreparationStatusChecks() = preparationActionOps.refreshPreparationStatusChecks()
    fun handleRefreshPreparationStatus() = preparationActionOps.handleRefreshPreparationStatus()
    fun handleRefreshAllSecurityChecks() = preparationActionOps.handleRefreshAllSecurityChecks()
    fun handleRefreshPreExamHealthCheck() =
        preparationActionOps.handleRefreshPreExamHealthCheck(deviceCompatibilityProfile)
    fun handleRequestSectionReport(section: DiagnosticSection) =
        preparationActionOps.handleRequestSectionReport(section)
    fun buildCurrentPreExamHealthSnapshot() = buildExamRuntimePreExamHealthSnapshot(
        context = context,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        lockTaskBridge = lockTaskBridge,
        adminSettings = adminSettings,
        vpnBypassState = vpnBypassState,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState,
        deviceTimeBypassState = deviceTimeBypassState,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        overlayRiskResult = overlayRiskResult,
        networkReadinessStatus = networkReadinessStatus,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        examRuntimeRecoveryState = examRuntimeRecoveryState,
        flowUiState = flowUiState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        batteryStatus = batteryStatus,
        dpcRuntimeStatus = dpcRuntimeStatus
    )

    val preExamHealthCheckSnapshot = buildCurrentPreExamHealthSnapshot()
    val deviceSurvivalPolicy = resolveExamRuntimeDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        preExamHealthSnapshot = preExamHealthCheckSnapshot
    )
    val diagnosticExportOps = ExamRuntimeDiagnosticExportOps(
        context = context,
        uiLanguage = uiLanguage,
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        payload = payload,
        adminSettings = adminSettings,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        webViewUiState = webViewUiState,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        securityUiState = securityUiState,
        runtimeCacheState = runtimeCacheState,
        preExamHealthSnapshotProvider = ::buildCurrentPreExamHealthSnapshot
    )

    fun handleExportExamDiagnostics(source: String) = diagnosticExportOps.export(source)

    fun handleStartExam() {
        writePreviousSessionBreadcrumb(
            code = PreviousExamSessionBreadcrumbCodes.StartPressed,
            details = "score=${deviceSurvivalPolicy.score.name} | health_blocking=${deviceSurvivalPolicy.healthBlockingCount}"
        )
        coroutineScope.launch {
            startExamController.startExamSession()
        }
    }
    val footerShieldStatus = resolveExamFooterShieldStatus(
        examGuardArmed = examGuardArmed,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        isKeyboardAllowed = isKeyboardAllowed,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        bypassBluetooth = bypassBluetooth,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        bypassAdb = bypassAdb,
        adbInspection = adbInspection,
        bypassRoot = bypassRoot,
        bypassReverseEngineering = bypassReverseEngineering,
        bypassApkIntegrity = bypassApkIntegrity,
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassVpn = bypassVpn,
        networkReadinessStatus = networkReadinessStatus,
        bypassGeofence = bypassGeofence,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        bypassClipboard = bypassClipboard,
        signatureMismatchDetected = signatureMismatchDetected && !bypassApkIntegrity,
        securityTamperDetected = securityTamperDetected,
        forcedExitViolationCount = forcedExitViolationCount,
        keyboardViolationCount = keyboardViolationCount,
        overlayViolationCount = overlayViolationCount,
        geofenceViolationCount = geofenceViolationCount,
        fakeLocationViolationCount = fakeLocationViolationCount,
        bluetoothViolationCount = bluetoothViolationCount,
        clipboardViolationCount = clipboardViolationCount,
        showForcedExitAlarm = showForcedExitAlarm,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        showOverlayViolationDialog = showOverlayViolationDialog,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        showClipboardViolationDialog = showClipboardViolationDialog
    )
    val runtimeChromeState = buildExamRuntimeChromeState(
        examSessionStarted = examSessionStarted,
        examDisplayName = examDisplayName,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = fullScreenCustomView != null,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        showBuiltInExamKeyboard = showBuiltInExamKeyboard,
        showSideArrowControls = examSessionStarted && sideArrowControlsVisible,
        hasEditableFocus = hasEditableFocus,
        builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
        networkStatus = networkReadinessStatus,
        serverStatus = examServerStatus,
        batteryStatus = batteryStatus,
        shieldStatus = footerShieldStatus
    )
    val runtimeChromeActions = buildExamRuntimeChromeActionsForSession(
        examSessionStarted = examSessionStarted,
        screenPinningMode = screenPinningMode,
        screenPinningAvailable = screenPinningAvailable,
        lockTaskRequestPending = lockTaskRequestPending,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        isIndonesian = isIndonesian,
        isCurrentlyLoading = { loadingProgress in 0f..0.98f },
        lockTaskAlreadyActive = { lockTaskBridge.active() },
        markTrustedRuntimeChromeAction = ::markTrustedRuntimeChromeAction,
        clearWebViewError = { webViewErrorMessage = null },
        loadExamUrl = {
            webViewInstance?.let { webView ->
                webView.loadExamUrlSafely(payload.examUrl)
                webView.requestedExamUrl = payload.examUrl
            }
        },
        stopWebViewLoading = { webViewInstance?.stopLoading() },
        setLoadingProgress = { loadingProgress = it },
        setWebViewStopRequested = { webViewStopRequested = it },
        setLastExamRefreshDecision = { lastExamRefreshDecision = it },
        setScreenPinningMessage = { screenPinningMessage = it },
        setShowExitExamDialog = { showExitExamDialog = it },
        launchExamServerProbe = { trigger, markChecking ->
            launchExamServerProbe(trigger = trigger, markChecking = markChecking)
        },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        sendBuiltInKeyboardText = ::sendBuiltInKeyboardText,
        sendBuiltInKeyboardBackspace = ::sendBuiltInKeyboardBackspace,
        sendKeyboardArrowLeft = ::sendKeyboardArrowLeft,
        sendKeyboardArrowRight = ::sendKeyboardArrowRight,
        toggleSideArrowControls = {
            sideArrowControlsVisible = !sideArrowControlsVisible
            sideArrowControlsVisible
        },
        sendBuiltInKeyboardEnter = ::sendBuiltInKeyboardEnter,
        toggleBuiltInKeyboardShift = {
            builtInKeyboardShiftEnabled = !builtInKeyboardShiftEnabled
        }
    )
    val runtimeDialogsState = buildExamRuntimeDialogsState(
        showForcedExitAlarm = showForcedExitAlarm,
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayViolationCount = overlayViolationCount,
        overlayTrigger = overlayRiskResult.lastTrigger,
        showOfflineWarningDialog = showOfflineWarningDialog,
        offlineDurationMs = offlineWarningDurationMs,
        currentOfflineDurationMs = currentOfflineDurationMs,
        uiLanguage = uiLanguage,
        showVpnDetectedDialog = examSessionStarted && networkReadinessStatus.diagnostics.isVpnActive && !bypassVpn,
        vpnBypassActive = bypassVpn,
        vpnBypassTampered = vpnBypassState == VpnBypassState.Tampered,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothViolationCount = bluetoothViolationCount,
        showClipboardViolationDialog = showClipboardViolationDialog,
        clipboardViolationCount = clipboardViolationCount,
        clipboardLastConfirmedAt = lastClipboardConfirmedAt,
        clipboardLastDecision = lastClipboardDecision,
        showExitExamDialog = showExitExamDialog,
        exitSessionClearInFlight = exitSessionClearInFlight
    )
    val runtimeDialogsActions = buildRuntimeDialogsActionsForSession(
        context = context,
        componentActivity = componentActivity,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        networkUiState = networkUiState,
        appSwitchStatus = appSwitchStatus,
        overlayRiskResult = overlayRiskResult,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        currentOfflineDurationMs = currentOfflineDurationMs,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        adminOverridesSummary = adminOverridesSummary,
        examSessionStarted = examSessionStarted,
        examGuardArmed = examGuardArmed,
        acknowledgeRuntimeAlarm = ::acknowledgeRuntimeAlarm,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        currentNetworkEventDetails = ::currentNetworkEventDetails,
        openVpnSettings = ::handleOpenVpnSettings,
        refreshVpnStatus = { trigger -> launchNetworkManualRefresh(trigger) },
        requestSectionReport = ::handleRequestSectionReport,
        refreshBluetoothSecurity = ::refreshBluetoothSecurity,
        clearExamSessionOnExit = { reason, waitForResult ->
            clearExamSessionOnExit(reason = reason, waitForResult = waitForResult)
        },
        writePreviousSessionBreadcrumb = { code, details ->
            writePreviousSessionBreadcrumb(code = code, details = details)
        },
        onExit = onExit,
        examAlarmController = examAlarmController
    )

    val screenPinningFixNeeded = !bypassScreenPinning &&
        screenPinningAvailable &&
        screenPinningEnabledInSystem.equals("Nonaktif", ignoreCase = true)
    val reinstallApkFixNeeded = signatureMismatchDetected && officialApkUrl.isNotBlank()
    val previousExamSessionBreadcrumb = remember {
        PreviousExamSessionBreadcrumbStore.read(context)
    }
    ExamRuntimeResolvedDiagnosticsEffects(
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        examName = payload.examName,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        writePreviousSessionBreadcrumb = { code, details ->
            writePreviousSessionBreadcrumb(code = code, details = details)
        }
    )
    val preparationState = buildPreparationStateForSession(
        payload = payload,
        adminSettings = adminSettings,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        networkUiState = networkUiState,
        locationWarmupUiState = locationWarmupUiState,
        keyboardAllowed = isKeyboardAllowed,
        sendingSection = sendingSection,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        networkTimelinePreview = networkTimelinePreview,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningActive = lockTaskBridge.active(),
        screenPinningFixNeeded = screenPinningFixNeeded,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        clipboardBypassState = clipboardBypassState,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        deviceTimeBypassState = deviceTimeBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        overlayRiskResult = overlayRiskResult,
        appSwitchStatus = appSwitchStatus,
        reinstallApkFixNeeded = reinstallApkFixNeeded,
        bypassScreenPinning = bypassScreenPinning,
        bypassBluetooth = bypassBluetooth,
        bypassAccessibility = bypassAccessibility,
        bypassAdb = bypassAdb,
        adbBypassState = adbBypassState,
        bypassRoot = bypassRoot,
        rootBypassState = rootBypassState,
        bypassReverseEngineering = bypassReverseEngineering,
        bypassApkIntegrity = bypassApkIntegrity,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        bypassVpn = bypassVpn,
        vpnBypassState = vpnBypassState,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        bypassClipboard = bypassClipboard,
        bypassOverlay = bypassOverlay,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        bypassDeviceTime = bypassDeviceTime,
        bypassAppSwitch = bypassAppSwitch,
        bypassScreenRecorder = bypassScreenRecorder,
        bypassDisplayMirror = bypassDisplayMirror,
        externalDisplayInfoList = securityUiState.externalDisplayInfoList.value,
        bypassMultiWindow = bypassMultiWindow,
        multiWindowModeInfo = securityUiState.multiWindowModeInfo.value,
        preExamHealthCheckSnapshot = preExamHealthCheckSnapshot,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        previousExamSessionBreadcrumb = previousExamSessionBreadcrumb,
        dpcRuntimeStatus = dpcRuntimeStatus
    )
    val preparationActions = buildPreparationScreenActions(
        onChooseKeyboard = ::handleChooseKeyboard,
        onOpenKeyboardSettings = ::handleOpenKeyboardSettings,
        onGrantBluetoothPermission = ::handleGrantBluetoothPermission,
        onOpenBluetoothSettings = ::handleOpenBluetoothSettings,
        onOpenAccessibilitySettings = ::handleOpenAccessibilitySettings,
        onOpenOverlayAccessibilitySettings = ::handleOpenOverlayAccessibilitySettings,
        onOpenDeveloperOptionsSettings = ::handleOpenDeveloperOptionsSettings,
        onRequestLocationPermission = ::handleRequestLocationPermission,
        onOpenLocationServicesSettings = ::handleOpenLocationServicesSettings,
        onRefreshGeofenceLocation = ::handleRefreshLocationSecurity,
        onOpenGeofenceMapViewer = ::handleOpenGeofenceMapViewer,
        onOpenInternetSettings = ::handleOpenInternetSettings,
        onOpenVpnSettings = ::handleOpenVpnSettings,
        onOpenWifiSettings = ::handleOpenWifiSettings,
        onOpenCellularSettings = ::handleOpenCellularSettings,
        onOpenAirplaneModeSettings = ::handleOpenAirplaneModeSettings,
        onRefreshNetworkStatus = ::handleRefreshNetworkStatus,
        onOpenDateTimeSettings = ::handleOpenDateTimeSettings,
        onOpenFakeLocationDeveloperOptionsSettings = ::handleOpenFakeLocationDeveloperOptionsSettings,
        onOpenScreenPinningSettings = ::handleOpenScreenPinningSettings,
        onStartScreenPinning = ::handleStartScreenPinning,
        onOpenOverlaySettings = ::handleOpenOverlaySettings,
        onOpenAppSettings = ::handleOpenAppSettings,
        onOpenCastSettings = ::handleOpenCastSettings,
        onOpenWebViewProviderSettings = ::handleOpenWebViewProviderSettings,
        onReinstallOfficialApk = ::handleReinstallOfficialApk,
        onRefreshStatus = ::handleRefreshPreparationStatus,
        onRefreshAllSecurityChecks = ::handleRefreshAllSecurityChecks,
        onRefreshHealthCheck = ::handleRefreshPreExamHealthCheck,
        onRequestSectionReport = ::handleRequestSectionReport,
        onExportDiagnostics = { handleExportExamDiagnostics("preparation_recovery") },
        onAutoFixShown = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreparationAutoFixShown,
                details = details
            )
        },
        onPreviousSessionRecoveryHintShown = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreviousSessionRecoveryHintShown,
                details = details
            )
        },
        onAutoFixActionOpened = { actionCode ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreparationAutoFixActionOpened,
                details = "action=$actionCode"
            )
        },
        onScreenPinningDeferred = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.ScreenPinningDeferredUntilBlockersClear,
                details = details
            )
        },
        onStartExam = ::handleStartExam,
        onBackHome = onExit
    )
    val renderedUiCallbacks = ExamRuntimeRenderedUiCallbacks(
        componentActivity = componentActivity,
        lockTaskBridge = lockTaskBridge,
        deviceQuirkProfile = deviceQuirkProfile,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        runtimeMonitoringOps = runtimeMonitoringOps,
        webViewUiState = webViewUiState,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        adminUiState = adminUiState,
        examServerStatusState = examServerStatusState,
        lastTrustedRuntimeChromeActionElapsedMsState = lastTrustedRuntimeChromeActionElapsedMsState,
        lastTrustedRuntimeChromeActionReasonState = lastTrustedRuntimeChromeActionReasonState,
        examAlarmController = examAlarmController,
        hideSystemKeyboard = ::hideSystemKeyboard,
        launchTelegramSectionReport = ::launchTelegramSectionReport,
        onExit = onExit
    )

    ExamRuntimeSessionRenderedUiSection(
        examSessionStarted = examSessionStarted,
        showGeofenceMapViewer = showGeofenceMapViewer,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        preparationState = preparationState,
        preparationActions = preparationActions,
        runtimeChromeState = runtimeChromeState,
        runtimeChromeActions = runtimeChromeActions,
        payload = payload,
        bypassOverlay = bypassOverlay,
        examAlarmController = examAlarmController,
        participantCaptureBridge = participantCaptureBridge,
        nativeFullscreenBridge = nativeFullscreenBridge,
        keyboardBridge = keyboardBridge,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        effectiveExamUserAgent = effectiveExamUserAgent,
        fullScreenContainer = fullScreenContainer,
        fullScreenCustomView = fullScreenCustomView,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        runtimeDialogsState = runtimeDialogsState,
        runtimeDialogsActions = runtimeDialogsActions,
        pendingSection = pendingSection,
        uiLanguage = uiLanguage,
        screenPinningMessage = if (examSessionStarted) screenPinningMessage else null,
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        securityUiState = securityUiState,
        renderedUiCallbacks = renderedUiCallbacks,
        onHideSystemKeyboard = ::hideSystemKeyboard,
        onHideCustomView = ::hideCustomView,
        onOpenStaticSecurityAppSettings = ::handleOpenAppSettings,
        onOpenStaticSecurityCastSettings = ::handleOpenCastSettings,
        onRefreshStaticSecurityStatus = ::handleRefreshPreparationStatus,
        onSendStaticSecurityReport = ::launchTelegramSectionReport,
        modifier = modifier
    )

}

@Composable
private fun ExamRuntimeSessionRenderedUiSection(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: com.example.coblaxexamlock.ExamQrPayload,
    bypassOverlay: Boolean,
    examAlarmController: ExamAlarmController,
    participantCaptureBridge: ExamParticipantCaptureBridge,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    keyboardBridge: ExamKeyboardBridge,
    useBuiltInExamKeyboard: Boolean,
    effectiveExamUserAgent: String,
    fullScreenContainer: FrameLayout,
    fullScreenCustomView: View?,
    nativeExamFullscreenActive: Boolean,
    runtimeDialogsState: com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsState,
    runtimeDialogsActions: com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions,
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    bugReportFeedbackTitle: String?,
    bugReportFeedbackMessage: String?,
    securityUiState: ExamRuntimeSecurityUiState,
    renderedUiCallbacks: ExamRuntimeRenderedUiCallbacks,
    onHideSystemKeyboard: () -> Unit,
    onHideCustomView: () -> Unit,
    onOpenStaticSecurityAppSettings: () -> Unit,
    onOpenStaticSecurityCastSettings: () -> Unit,
    onRefreshStaticSecurityStatus: () -> Unit,
    onSendStaticSecurityReport: (DiagnosticSection) -> Unit,
    modifier: Modifier
) {
    ExamRuntimeSessionRenderedUi(
        examSessionStarted = examSessionStarted,
        showGeofenceMapViewer = showGeofenceMapViewer,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        preparationState = preparationState,
        preparationActions = preparationActions,
        runtimeChromeState = runtimeChromeState,
        runtimeChromeActions = runtimeChromeActions,
        payload = payload,
        bypassOverlay = bypassOverlay,
        examAlarmController = examAlarmController,
        participantCaptureBridge = participantCaptureBridge,
        nativeFullscreenBridge = nativeFullscreenBridge,
        keyboardBridge = keyboardBridge,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        effectiveExamUserAgent = effectiveExamUserAgent,
        fullScreenContainer = fullScreenContainer,
        fullScreenCustomView = fullScreenCustomView,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        runtimeDialogsState = runtimeDialogsState,
        runtimeDialogsActions = runtimeDialogsActions,
        pendingSection = pendingSection,
        uiLanguage = uiLanguage,
        screenPinningMessage = screenPinningMessage,
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        securityUiState = securityUiState,
        onDismissGeofenceMapViewer = renderedUiCallbacks::onDismissGeofenceMapViewer,
        onRefreshGeofenceMapViewer = renderedUiCallbacks::onRefreshGeofenceMapViewer,
        onRefreshMapViewerActionLogged = renderedUiCallbacks::onRefreshMapViewerActionLogged,
        onOverlayObscuredTouch = renderedUiCallbacks::onOverlayObscuredTouch,
        onShowBuiltInExamKeyboardChange = renderedUiCallbacks::onShowBuiltInExamKeyboardChange,
        onWebViewInstanceChange = renderedUiCallbacks::onWebViewInstanceChange,
        onHideSystemKeyboard = onHideSystemKeyboard,
        onWebViewLoadStart = renderedUiCallbacks::onWebViewLoadStart,
        onWebViewLoadFinish = renderedUiCallbacks::onWebViewLoadFinish,
        onWebViewLoadError = renderedUiCallbacks::onWebViewLoadError,
        onWebViewHttpError = renderedUiCallbacks::onWebViewHttpError,
        onWebViewRenderProcessGone = renderedUiCallbacks::onWebViewRenderProcessGone,
        onLoadingProgressChange = renderedUiCallbacks::onLoadingProgressChange,
        onWebViewErrorMessageChange = renderedUiCallbacks::onWebViewErrorMessageChange,
        onShowCustomView = renderedUiCallbacks::onShowCustomView,
        onHideCustomView = onHideCustomView,
        onDismissPendingSection = renderedUiCallbacks::onDismissPendingSection,
        onConfirmPendingSection = renderedUiCallbacks::onConfirmPendingSection,
        onOpenStaticSecurityAppSettings = onOpenStaticSecurityAppSettings,
        onOpenStaticSecurityCastSettings = onOpenStaticSecurityCastSettings,
        onRefreshStaticSecurityStatus = onRefreshStaticSecurityStatus,
        onSendStaticSecurityReport = onSendStaticSecurityReport,
        onDismissScreenPinningMessage = renderedUiCallbacks::onDismissScreenPinningMessage,
        onDismissSecurityIssueDialog = renderedUiCallbacks::onDismissSecurityIssueDialog,
        onDismissBugReportFeedback = renderedUiCallbacks::onDismissBugReportFeedback,
        modifier = modifier
    )
}

private fun handleExamRuntimeScreenPinningTransitionInterrupted(
    lockTaskRequestPending: Boolean,
    examSessionStarted: Boolean,
    lockTaskBridge: ActivityLockTaskBridge,
    pinningActivationStartedAtElapsedMs: Long?,
    pinningSuppressedTransitionCount: Int,
    isIndonesian: Boolean,
    pinningActivationPurpose: PinningActivationPurpose,
    setPinningActivationState: (PinningActivationState) -> Unit,
    setLockTaskStateAfterPinningRequest: (String) -> Unit,
    setScreenPinningDialogLikelyShown: (Boolean) -> Unit,
    setPinningSuppressedTransitionCount: (Int) -> Unit,
    setScreenPinningMessage: (String?) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    if (!lockTaskRequestPending || examSessionStarted) {
        return
    }
    if (lockTaskBridge.active()) {
        setPinningActivationState(PinningActivationState.ActiveConfirmed)
        recordAction(
            ExamRuntimeHardeningDiagnostics.ScreenPinningAlreadyActive,
            "transition_interrupt_ignored | state=${lockTaskBridge.stateLabel()}",
            DiagnosticEventLevel.INFO
        )
        return
    }

    val stateAfterInterrupt = lockTaskBridge.stateLabel()
    setLockTaskStateAfterPinningRequest(stateAfterInterrupt)
    setScreenPinningDialogLikelyShown(true)
    val nowElapsedMs = SystemClock.elapsedRealtime()
    val elapsedMs = pinningActivationStartedAtElapsedMs?.let { (nowElapsedMs - it).coerceAtLeast(0L) }
    val withinGrace = shouldSuppressPinningTransitionViolation(
        lockTaskRequestPending = lockTaskRequestPending,
        examSessionStarted = examSessionStarted,
        startedAtElapsedMs = pinningActivationStartedAtElapsedMs,
        nowElapsedMs = nowElapsedMs
    )
    val newSuppressedTransitionCount = pinningSuppressedTransitionCount + 1
    setPinningSuppressedTransitionCount(newSuppressedTransitionCount)
    setPinningActivationState(PinningActivationState.WaitingForLockTaskActive)
    setScreenPinningMessage(
        ScreenPinningEnforcer.activatingMessage(
            isIndonesian = isIndonesian,
            purpose = pinningActivationPurpose
        )
    )
    recordAction(
        ExamRuntimeHardeningDiagnostics.PinningTransitionViolationSuppressed,
        "source=user_leave_hint | state=$stateAfterInterrupt | elapsed_ms=${elapsedMs ?: -1} | within_grace=$withinGrace | suppressed_count=$newSuppressedTransitionCount | wait_until_timeout=true",
        DiagnosticEventLevel.WARNING
    )
    if (!withinGrace) {
        recordAction(
            ExamRuntimeHardeningDiagnostics.ScreenPinningTransitionInterrupted,
            "source=user_leave_hint | state=$stateAfterInterrupt | elapsed_ms=${elapsedMs ?: -1} | suppressed_until_timeout=true",
            DiagnosticEventLevel.WARNING
        )
    }
}
