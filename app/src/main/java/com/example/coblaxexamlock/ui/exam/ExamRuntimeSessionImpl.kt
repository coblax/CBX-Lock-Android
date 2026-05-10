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
import android.view.inputmethod.InputMethodManager
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
import com.example.coblaxexamlock.buildDeviceSurvivalPolicy
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.ClipboardBypassResolver
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.ClipboardSnapshot
import com.example.coblaxexamlock.config.AlarmAcknowledgeDedupWindowMillis
import com.example.coblaxexamlock.config.AppSwitchSuppressionWindowMillis
import com.example.coblaxexamlock.config.MaxDiagnosticActionLogEntries
import com.example.coblaxexamlock.config.MaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.NetworkUnstableFlipThreshold
import com.example.coblaxexamlock.config.NetworkUnstableWindowMillis
import com.example.coblaxexamlock.detachExamKeyboardBridge
import com.example.coblaxexamlock.detachExamNativeFullscreenBridge
import com.example.coblaxexamlock.detachExamParticipantCaptureBridge
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassResolver
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
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
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
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
import com.example.coblaxexamlock.PinningActivationGraceWindowMillis
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.platform.openExternalUrl
import com.example.coblaxexamlock.prepareForFreshExamSession
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbStore
import com.example.coblaxexamlock.readClipboardSnapshotFull
import com.example.coblaxexamlock.readClipboardSnapshotLite
import com.example.coblaxexamlock.readWebViewCompatibilityStatus
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.runtime.acquireBestEffortLocationSnapshot
import com.example.coblaxexamlock.runtime.buildRootIssueMessage
import com.example.coblaxexamlock.runtime.detectSuspiciousFakeLocationPackages
import com.example.coblaxexamlock.runtime.getBluetoothConnectPermission
import com.example.coblaxexamlock.runtime.getCachedVirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.runtime.getCurrentInputMethodPackage
import com.example.coblaxexamlock.runtime.getRootDetectionDetails
import com.example.coblaxexamlock.runtime.getVirtualEnvironmentDiagnosticsOnIo
import com.example.coblaxexamlock.runtime.hasBluetoothExamPermission
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isAllowedExamKeyboard
import com.example.coblaxexamlock.runtime.isBluetoothEnabledForExam
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readExamBatteryStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatusWithProbe
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.runtime.resolveKeyboardAppLabel
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
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.VpnBypassResolver
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.preparation.buildPreExamHealthSnapshot
import com.example.coblaxexamlock.ui.preparation.PreExamHealthCheckInput
import com.example.coblaxexamlock.ui.preparation.preExamHealthStartBlocker
import com.example.coblaxexamlock.ui.preparation.PreparationScreenState
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.viewmodel.ExamRuntimeUiAction
import com.example.coblaxexamlock.viewmodel.rememberBoundExamRuntimeViewModel
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
        readWebViewCompatibilityStatus(context.applicationContext)
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
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var webViewInstance by remember { mutableStateOf<SecureExamWebView?>(null) }
    val flowUiState = rememberExamRuntimeFlowUiState(
        context = context,
        bypassKeyboardPolicy = bypassKeyboardPolicy
    )
    val locationWarmupUiState = rememberExamRuntimeLocationWarmupUiState()
    var examSessionStarted by flowUiState.examSessionStarted
    var lockTaskRequestPending by flowUiState.lockTaskRequestPending
    var pinningActivationState by flowUiState.pinningActivationState
    var pinningActivationStartedAtElapsedMs by flowUiState.pinningActivationStartedAtElapsedMs
    var pinningSuppressedTransitionCount by flowUiState.pinningSuppressedTransitionCount
    var screenPinningMessage by flowUiState.screenPinningMessage
    var showExitExamDialog by flowUiState.showExitExamDialog
    var webViewErrorMessage by flowUiState.webViewErrorMessage
    var examServerStatus by rememberSaveable(payload.examUrl) {
        mutableStateOf(ExamServerFooterStatus.Checking)
    }
    LaunchedEffect(examSessionRecoveryNonce, examSessionStarted) {
        onExamSessionStartedStateChange(examSessionStarted)
    }
    var baseNetworkReadiness by remember { mutableStateOf(readNetworkReadinessStatus(context)) }
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
    val networkMainHandler = remember { Handler(Looper.getMainLooper()) }
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }
    var fullScreenCustomView by remember { mutableStateOf<View?>(null) }
    var fullScreenCustomViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val fullScreenContainer = remember {
        FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(LockBackground.toArgb())
        }
    }
    var useBuiltInExamKeyboard by flowUiState.useBuiltInExamKeyboard
    var exitSessionClearInFlight by flowUiState.exitSessionClearInFlight
    var exitSessionClearRequested by rememberSaveable { mutableStateOf(false) }
    var exitSessionClearDeferred by remember {
        mutableStateOf<CompletableDeferred<Result<Unit>>?>(null)
    }
    var examRuntimeRecoveryState by rememberSaveable {
        mutableStateOf(ExamRuntimeRecoveryState.Idle)
    }
    val lastTrustedRuntimeChromeActionElapsedMsState = rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var lastTrustedRuntimeChromeActionElapsedMs by lastTrustedRuntimeChromeActionElapsedMsState
    val lastTrustedRuntimeChromeActionReasonState = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var lastTrustedRuntimeChromeActionReason by lastTrustedRuntimeChromeActionReasonState
    var lastRuntimeMemoryActionSummary by rememberSaveable {
        mutableStateOf<String?>(null)
    }
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
    LaunchedEffect(context) {
        virtualEnvironmentDetected = getVirtualEnvironmentDiagnosticsOnIo(context).detected
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
    var deviceTimeSecurityStatus by remember(
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
    var lastDeviceTimeDiagnosticKey by rememberSaveable { mutableStateOf<String?>(null) }
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
    var reverseEngineeringRefreshCache by remember {
        mutableStateOf<RuntimeReverseEngineeringRefreshCache?>(null)
    }
    var integrityRefreshCache by remember {
        mutableStateOf<RuntimeIntegrityRefreshCache?>(null)
    }
    var lastAlarmAcknowledgeDedupKey by adminUiState.lastAlarmAcknowledgeDedupKey
    var lastAlarmAcknowledgeAtElapsedMs by adminUiState.lastAlarmAcknowledgeAtElapsedMs
    val isKeyboardAllowed = bypassKeyboardPolicy || isAllowedExamKeyboard(context, currentKeyboardPackage)
    val securityTamperDetected = tamperDetected || integrityTamperDetected
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
    class RuntimeDiagnosticsOps {
        fun currentNetworkPollingIntervalMillis(): Long {
            val baseInterval = if (
                !networkStatus.isConnected ||
                networkUnstableEpisodeStartedElapsedMs != null
            ) {
                NetworkReadinessPollingUnstableIntervalMillis
            } else {
                NetworkReadinessPollingStableIntervalMillis
            }
            return baseInterval * lowRamProfile.slowPollingMultiplier
        }

        fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long {
            val sessionStartedAt = examSessionStartedAtElapsedMs
            val withinWarmupWindow =
                lockTaskRequestPending ||
                    (
                        sessionStartedAt != null &&
                            (nowElapsedMs - sessionStartedAt).coerceAtLeast(0L) <=
                            ScreenPinningMonitorWarmupWindowMillis
                        )
            return if (withinWarmupWindow) {
                ScreenPinningMonitorWarmupIntervalMillis
            } else {
                ScreenPinningMonitorSteadyIntervalMillis
            }
        }

        val currentOfflineDurationMs = if (
            examSessionStarted &&
                !networkStatus.isConnected &&
                offlineStartedAtElapsedMs != null
        ) {
            (SystemClock.elapsedRealtime() - offlineStartedAtElapsedMs!!).coerceAtLeast(0L)
        } else {
            null
        }
        val offlineRuntimeStatus = ExamOfflineRuntimeStatus(
            offlineActive = examSessionStarted && !networkStatus.isConnected && offlineStartedAtElapsedMs != null,
            offlineStartedAt = offlineStartedAtTimestamp,
            currentOfflineDurationMs = currentOfflineDurationMs,
            offlineWarningShown = offlineWarningShown,
            lastOfflineWarningAt = lastOfflineWarningAt,
            lastOfflineDurationMs = lastOfflineDurationMs
        )
        val networkTimelinePreview = networkTimeline.takeLast(5).asReversed()
        val networkUnstableRuntimeStatus = NetworkUnstableRuntimeStatus(
            unstableActive = networkUnstableEpisodeStartedElapsedMs != null,
            episodeStartedAt = networkUnstableEpisodeStartedAt,
            flapCount = networkUnstableFlapCount,
            lastFlapAt = networkUnstableLastFlapAt,
            warningShown = networkUnstableWarningShown,
            lastWarningAt = lastNetworkUnstableWarningAt,
            lastTransportLabel = networkUnstableLastTransportLabel
        )
        val geofenceRuntimeStatus = GeofenceRuntimeStatus(
            evaluation = geofenceEvaluation,
            securityStatus = geofenceSecurityStatus,
            policySource = effectiveLocationPolicySource,
            violationCount = geofenceViolationCount,
            lastTrigger = lastGeofenceTrigger,
            lastDetectedAt = lastGeofenceAt,
            lastContext = lastGeofenceContext
        )
        val fakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
            securityStatus = fakeLocationSecurityStatus,
            violationCount = fakeLocationViolationCount,
            lastTrigger = lastFakeLocationTrigger,
            lastDetectedAt = lastFakeLocationAt,
            lastContext = lastFakeLocationContext
        )
        val overlayShieldStatus = OverlayShieldStatus(
            supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            requested = overlayShieldRequested,
            lastApplySucceeded = overlayShieldLastApplySucceeded,
            lastApplyAt = overlayShieldLastAppliedAt
        )
        val clipboardRuntimeStatus = ClipboardRuntimeStatus(
            lastObservedAt = lastClipboardObservedAt,
            lastConfirmedAt = lastClipboardConfirmedAt,
            lastObservedSignature = lastClipboardObservedSignature,
            lastDecision = lastClipboardDecision,
            baselineSemanticSignature = lastClipboardBaselineSemanticSignature,
            detectedSemanticSignature = lastClipboardDetectedSemanticSignature,
            currentSemanticSignature = clipboardDecisionSemanticSignature
        )
        val appSwitchLockTaskActive = lockTaskBridge.active()
        val appSwitchProtectionMode = AppSwitchMonitor.protectionModeOf(
            bypassState = appSwitchBypassState,
            screenPinningMode = screenPinningMode,
            guardArmed = examGuardArmed,
            lockTaskActive = appSwitchLockTaskActive
        )
        val appSwitchStatus = AppSwitchMonitor.statusOf(
            bypassState = appSwitchBypassState,
            runtimeMonitoringActive = AppSwitchMonitor.shouldMonitor(
                hostAvailable = mainActivity != null,
                guardArmed = examGuardArmed,
                bypassState = appSwitchBypassState
            ),
            protectionMode = appSwitchProtectionMode,
            lockTaskActive = appSwitchLockTaskActive,
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

        fun currentDiagnosticScreen(): String {
            return examRuntimeDiagnosticScreen(
                lockTaskRequestPending = lockTaskRequestPending,
                examSessionStarted = examSessionStarted,
                examRuntimeMonitoringArmed = examRuntimeMonitoringArmed
            )
        }

        fun writePreviousSessionBreadcrumb(
            code: String,
            details: String = "-"
        ) {
            runCatching {
                PreviousExamSessionBreadcrumbStore.append(
                    context = context,
                    code = code,
                    details = details
                )
            }
            if (ExamRuntimeHardeningDiagnostics.shouldLogForQa(
                    ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten
                )
            ) {
                Log.i(
                    ExamRuntimeHardeningLogTag,
                    "code=${ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten} " +
                        "level=INFO details=event=$code | ${details.ifBlank { "-" }}"
                )
            }
            diagnosticEvents = prependDiagnosticEvent(
                existingEvents = diagnosticEvents,
                code = ExamRuntimeHardeningDiagnostics.PreviousSessionBreadcrumbWritten,
                details = "event=$code | ${details.ifBlank { "-" }}",
                level = DiagnosticEventLevel.INFO,
                screen = currentDiagnosticScreen(),
                appStartedAtElapsedMs = appStartedAtElapsedMs,
                examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
                maxEntries = MaxDiagnosticActionLogEntries
            )
        }

        fun maybeWritePreviousSessionBreadcrumb(
            code: String,
            details: String
        ) {
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
            appSwitchSuppressionReason = null
            appSwitchSuppressedUntilElapsedMs = null
        }

        fun setAppSwitchSuppression(
            reason: AppSwitchSuppressionReason,
            durationMs: Long = AppSwitchSuppressionWindowMillis
        ) {
            appSwitchSuppressionReason = reason
            appSwitchSuppressedUntilElapsedMs = SystemClock.elapsedRealtime() + durationMs
        }

        fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? {
            return resolveAppSwitchSuppressionReason(
                reason = appSwitchSuppressionReason,
                expiresAtElapsedMs = appSwitchSuppressedUntilElapsedMs
            )
        }

        fun currentAppSwitchEventDetails(
            signal: AppSwitchSignal,
            suppressionReason: AppSwitchSuppressionReason? = null
        ): String {
            return buildAppSwitchEventDetails(
                signal = signal,
                appSwitchStatus = appSwitchStatus,
                screenPinningMode = screenPinningMode,
                lockTaskActive = lockTaskBridge.active(),
                suppressionReason = suppressionReason
            )
        }

        fun currentOverlayEventDetails(
            signal: OverlaySignal,
            extraContext: String? = null
        ): String {
            return buildOverlayEventDetails(
                signal = signal,
                overlayShieldStatus = overlayShieldStatus,
                appSwitchStatus = appSwitchStatus,
                pendingForcedExitViolation = pendingForcedExitViolation,
                appSwitchLifecycleResumePending = appSwitchLifecycleResumePending,
                overlayWindowHasFocus = overlayWindowHasFocus,
                suppressionReason = currentAppSwitchSuppressionReason(),
                hasFullScreenCustomView = fullScreenCustomView != null,
                extraContext = extraContext
            )
        }

        fun currentInternalDialogReason(): String? {
            return resolveInternalDialogReason(
                showOfflineWarningDialog = showOfflineWarningDialog,
                showNetworkUnstableDialog = showNetworkUnstableDialog,
                showForcedExitAlarm = showForcedExitAlarm,
                showKeyboardViolationDialog = showKeyboardViolationDialog,
                showOverlayViolationDialog = showOverlayViolationDialog,
                showGeofenceViolationDialog = showGeofenceViolationDialog,
                showFakeLocationViolationDialog = showFakeLocationViolationDialog,
                showBluetoothViolationDialog = showBluetoothViolationDialog,
                showClipboardViolationDialog = showClipboardViolationDialog,
                showExitExamDialog = showExitExamDialog,
                pendingSectionPresent = pendingSection != null,
                securityIssueDialogMessagePresent = securityIssueDialogMessage != null,
                bugReportFeedbackMessagePresent = bugReportFeedbackMessage != null
            )
        }

        fun recordOverlayEvent(
            code: String,
            signal: OverlaySignal,
            level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
            extraContext: String? = null
        ) {
            val details = currentOverlayEventDetails(signal, extraContext)
            lastOverlayTrigger = signal.diagnosticLabel()
            lastOverlayAt = diagnosticTimestamp()
            lastOverlayContext = details
            diagnosticEvents = prependDiagnosticEvent(
                existingEvents = diagnosticEvents,
                code = code,
                details = details,
                level = level,
                screen = currentDiagnosticScreen(),
                appStartedAtElapsedMs = appStartedAtElapsedMs,
                examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
                maxEntries = MaxDiagnosticActionLogEntries
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
            diagnosticEvents = prependDiagnosticEvent(
                existingEvents = diagnosticEvents,
                code = code,
                details = details,
                level = level,
                screen = currentDiagnosticScreen(),
                appStartedAtElapsedMs = appStartedAtElapsedMs,
                examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
                maxEntries = MaxDiagnosticActionLogEntries
            )
        }

        fun currentGeofenceEventDetails(
            trigger: String,
            geofenceStatus: GeofenceSecurityStatus,
            extraContext: String? = null
        ): String {
            return buildGeofenceEventDetails(
                trigger = trigger,
                geofenceStatus = geofenceStatus,
                policySource = effectiveLocationPolicySource,
                extraContext = extraContext
            )
        }

        fun currentFakeLocationEventDetails(
            trigger: String,
            fakeLocationStatus: LocationSpoofSecurityStatus,
            extraContext: String? = null
        ): String {
            return buildFakeLocationEventDetails(
                trigger = trigger,
                fakeLocationStatus = fakeLocationStatus,
                extraContext = extraContext
            )
        }

        fun currentNetworkEventDetails(
            trigger: String,
            status: NetworkReadinessStatus,
            extraContext: String? = null
        ): String {
            return buildNetworkEventDetails(
                trigger = trigger,
                status = status,
                extraContext = extraContext
            )
        }

        fun refreshDeviceTimeSecurity(
            trigger: String,
            emitDiagnosticEvent: Boolean = true
        ): DeviceTimeSecurityStatus {
            val refreshedStatus = inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = deviceTimeBypassState
            )
            deviceTimeSecurityStatus = refreshedStatus
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
                if (eventCode != null && eventKey != lastDeviceTimeDiagnosticKey) {
                    recordAction(
                        code = eventCode,
                        details = buildDeviceTimeEventDetails(trigger, refreshedStatus),
                        level = DiagnosticEventLevel.WARNING
                    )
                    lastDeviceTimeDiagnosticKey = eventKey
                } else if (eventCode == null) {
                    lastDeviceTimeDiagnosticKey = null
                }
            }
            return refreshedStatus
        }

        fun appendNetworkTimelineEntry(entry: NetworkTimelineEntry) {
            networkTimeline.add(entry)
            while (networkTimeline.size > MaxNetworkTimelineEntries) {
                networkTimeline.removeAt(0)
            }
        }

        fun applyNetworkReadinessStatus(
            source: String,
            refreshedStatus: NetworkReadinessStatus
        ) {
            val previousStatus = baseNetworkReadiness
            val coreStateChanged =
                previousStatus.examStatus.isConnected != refreshedStatus.examStatus.isConnected ||
                    previousStatus.transportLabel != refreshedStatus.transportLabel ||
                    previousStatus.diagnostics.isValidated != refreshedStatus.diagnostics.isValidated ||
                    previousStatus.diagnostics.isCaptivePortal != refreshedStatus.diagnostics.isCaptivePortal ||
                    previousStatus.diagnostics.isVpnActive != refreshedStatus.diagnostics.isVpnActive ||
                    previousStatus.diagnostics.isAirplaneModeEnabled != refreshedStatus.diagnostics.isAirplaneModeEnabled ||
                    previousStatus.verdict != refreshedStatus.verdict ||
                    previousStatus.userFacingVerdict != refreshedStatus.userFacingVerdict ||
                    previousStatus.dnsProbeStatus.verdict != refreshedStatus.dnsProbeStatus.verdict
            baseNetworkReadiness = refreshedStatus

            if (refreshedStatus.examStatus.isConnected) {
                lastConnectedNetworkLabel = refreshedStatus.transportLabel
            }

            if (refreshedStatus.diagnostics.isCaptivePortal) {
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.NetworkCaptivePortalDetected,
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus
                    ),
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
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus
                    ),
                    level = DiagnosticEventLevel.INFO
                )
            }
            if (
                refreshedStatus.dnsProbeStatus.verdict == NetworkDnsProbeVerdict.Failed ||
                refreshedStatus.dnsProbeStatus.verdict == NetworkDnsProbeVerdict.Timeout
            ) {
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.NetworkDnsProbeFailed,
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus,
                        extraContext = "dns=${refreshedStatus.dnsProbeStatus.verdict.name.lowercase(Locale.US)}"
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
            }

            if (coreStateChanged) {
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
                lastNetworkChangeAt = timelineTimestamp
                lastNetworkChangeSource = source
            }

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
                networkUnstableLastFlapAt = flapTimestamp
                networkUnstableLastFlapElapsedMs = nowElapsed
                networkUnstableLastTransportLabel = refreshedStatus.transportLabel
                networkUnstableFlapCount = networkFlapElapsedMs.size

                if (
                    networkFlapElapsedMs.size >= NetworkUnstableFlipThreshold &&
                    networkUnstableEpisodeStartedElapsedMs == null
                ) {
                    networkUnstableEpisodeStartedElapsedMs = nowElapsed
                    networkUnstableEpisodeStartedAt = flapTimestamp
                    networkUnstableWarningShown = false
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
                examSessionStarted &&
                networkUnstableEpisodeStartedElapsedMs != null &&
                !networkUnstableWarningShown
            ) {
                networkUnstableWarningShown = true
                lastNetworkUnstableWarningAt = diagnosticTimestamp()
                showNetworkUnstableDialog = true
                recordAction(
                    code = "NETWORK_UNSTABLE_WARNING_SHOWN",
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus,
                        extraContext = "flap_count=${networkUnstableFlapCount}"
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
            }
        }

        fun updateNetworkReadiness(source: String) {
            applyNetworkReadinessStatus(source, readNetworkReadinessStatus(context))
        }

        fun launchNetworkManualRefresh(trigger: String) {
            if (networkManualRefreshInFlight) {
                return
            }
            coroutineScope.launch {
                networkManualRefreshInFlight = true
                applyNetworkReadinessStatus(
                    trigger,
                    readNetworkReadinessStatusWithProbe(context)
                )
                delay(250L)
                networkManualRefreshInFlight = false
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
                if (
                    geofenceSnapshotRequired ||
                    fakeLocationSnapshotRequired
                ) {
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
                suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
                bypassState = fakeLocationBypassState
            )
            geofenceEvaluation = latestGeofenceStatus.geofenceEvaluation
            geofenceSecurityStatus = latestGeofenceStatus
            fakeLocationSecurityStatus = latestFakeLocationStatus
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
                geofenceRuntimeEpisodeKey = null
                return
            }

            if (!geofenceStatus.blocking) {
                val previousEpisode = geofenceRuntimeEpisodeKey
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
                geofenceRuntimeEpisodeKey = null
                return
            }

            val nextEpisodeKey = geofenceStatus.finalVerdict.diagnosticLabel()
            if (geofenceRuntimeEpisodeKey == nextEpisodeKey) {
                return
            }

            geofenceRuntimeEpisodeKey = nextEpisodeKey
            val eventCode = when (geofenceStatus.finalVerdict) {
                GeofenceSecurityVerdict.Outside -> "GEOFENCE_RUNTIME_OUTSIDE"
                GeofenceSecurityVerdict.PreciseRequired -> "GEOFENCE_RUNTIME_PRECISE_REQUIRED"
                else -> "GEOFENCE_RUNTIME_LOCATION_UNAVAILABLE"
            }
            val details = currentGeofenceEventDetails(trigger = trigger, geofenceStatus = geofenceStatus)
            lastGeofenceTrigger = trigger
            lastGeofenceAt = diagnosticTimestamp()
            lastGeofenceContext = details
            geofenceViolationCount += 1
            showGeofenceViolationDialog = true
            recordAction(
                code = eventCode,
                details = details,
                level = DiagnosticEventLevel.SECURITY
            )
            examAlarmController.start()
        }

        fun applyFakeLocationRuntimeEvaluation(
            fakeLocationStatus: LocationSpoofSecurityStatus,
            trigger: String
        ) {
            if (!fakeLocationStatus.monitoringEnabled) {
                fakeLocationRuntimeEpisodeKey = null
                return
            }

            if (!fakeLocationStatus.blocking) {
                val previousEpisode = fakeLocationRuntimeEpisodeKey
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
                fakeLocationRuntimeEpisodeKey = null
                return
            }

            val nextEpisodeKey = buildString {
                append(fakeLocationStatus.finalVerdict.diagnosticLabel())
                append(':')
                append(fakeLocationStatus.confidenceTier.diagnosticLabel())
            }
            if (fakeLocationRuntimeEpisodeKey == nextEpisodeKey) {
                return
            }

            fakeLocationRuntimeEpisodeKey = nextEpisodeKey
            val eventCode = when (fakeLocationStatus.finalVerdict) {
                LocationSpoofSecurityVerdict.PermissionRequired -> "FAKE_LOCATION_RUNTIME_PERMISSION_REQUIRED"
                LocationSpoofSecurityVerdict.LocationServicesDisabled -> "FAKE_LOCATION_RUNTIME_LOCATION_SERVICES_REQUIRED"
                LocationSpoofSecurityVerdict.LocationUnavailable -> "FAKE_LOCATION_RUNTIME_LOCATION_UNAVAILABLE"
                else -> "FAKE_LOCATION_RUNTIME_SPOOF_DETECTED"
            }
            val details = currentFakeLocationEventDetails(
                trigger = trigger,
                fakeLocationStatus = fakeLocationStatus
            )
            lastFakeLocationTrigger = trigger
            lastFakeLocationAt = diagnosticTimestamp()
            lastFakeLocationContext = details
            fakeLocationViolationCount += 1
            showFakeLocationViolationDialog = true
            recordAction(
                code = eventCode,
                details = details,
                level = DiagnosticEventLevel.SECURITY
            )
            examAlarmController.start()
        }

        suspend fun refreshGeofenceStatus(
            preferFresh: Boolean,
            trigger: String,
            allowRuntimeViolation: Boolean
        ): SplitLocationSecurityStatus {
            val latestLocationStatus = evaluateLocationSecurityNow(preferFresh = preferFresh)
            if (examSessionStarted && geofenceEnabled && allowRuntimeViolation && !bypassGeofence) {
                applyGeofenceRuntimeEvaluation(
                    geofenceStatus = latestLocationStatus.geofenceStatus,
                    trigger = trigger
                )
            } else if (!latestLocationStatus.geofenceStatus.geofenceEvaluation.enabled || !examSessionStarted) {
                geofenceRuntimeEpisodeKey = null
            }
            if (
                examSessionStarted &&
                allowRuntimeViolation &&
                latestLocationStatus.fakeLocationStatus.monitoringEnabled &&
                !bypassFakeLocation
            ) {
                applyFakeLocationRuntimeEvaluation(
                    fakeLocationStatus = latestLocationStatus.fakeLocationStatus,
                    trigger = trigger
                )
            } else if (!latestLocationStatus.fakeLocationStatus.monitoringEnabled || !examSessionStarted) {
                fakeLocationRuntimeEpisodeKey = null
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
                if (warningKey != lastFakeLocationWarningKey) {
                    recordAction(
                        code = "FAKE_LOCATION_PACKAGE_WARNING",
                        details = currentFakeLocationEventDetails(
                            trigger = trigger,
                            fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                        ),
                        level = DiagnosticEventLevel.WARNING
                    )
                    lastFakeLocationWarningKey = warningKey
                }
            } else {
                lastFakeLocationWarningKey = null
            }
            return latestLocationStatus
        }

        fun buildCurrentWarmLocationValidationKey(): String {
            return buildWarmLocationValidationKey(
                permissionGranted = hasLocationPermissionForWifi(context),
                locationServicesEnabled = isLocationServicesEnabled(context),
                policySignature = warmLocationPolicySignature
            )
        }

        fun invalidateWarmLocationValidationCache() {
            reusableWarmLocationValidation = null
        }

        suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus {
            val currentValidationKey = buildCurrentWarmLocationValidationKey()
            val reusableWarmLocationForStart = reusableWarmLocationValidation?.takeIf {
                it.isReusableForStart(currentValidationKey = currentValidationKey)
            }
            if (reusableWarmLocationForStart != null) {
                val warmAgeMs = (
                    SystemClock.elapsedRealtime() - reusableWarmLocationForStart.completedAtElapsedMs
                ).coerceAtLeast(0L)
                debugLogExamStart(
                    "startExamSession reused warm location validation prepared ${warmAgeMs} ms ago"
                )
                geofenceEvaluation = reusableWarmLocationForStart.result.geofenceStatus.geofenceEvaluation
                geofenceSecurityStatus = reusableWarmLocationForStart.result.geofenceStatus
                fakeLocationSecurityStatus = reusableWarmLocationForStart.result.fakeLocationStatus
                return reusableWarmLocationForStart.result
            }

            val forcedRefreshReason = reusableWarmLocationValidation
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
            if (geofenceManualRefreshInFlight) {
                return
            }
            invalidateWarmLocationValidationCache()
            geofenceManualRefreshInFlight = true
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
                    lastGeofenceRefreshAt = refreshedAt
                    reusableWarmLocationValidation = WarmLocationValidationCache(
                        result = refreshedStatus,
                        validationKey = validationKey,
                        completedAtElapsedMs = SystemClock.elapsedRealtime(),
                        completedAtTimestamp = refreshedAt
                    ).takeIf { it.isReusableForStart(currentValidationKey = validationKey) }
                } finally {
                    geofenceManualRefreshInFlight = false
                }
            }
        }

    }
    val runtimeDiagnosticsOps = RuntimeDiagnosticsOps()
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
            delay(ExamServerProbeIntervalMillis)
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

    class RuntimeMonitoringOps {
        fun armExamRuntimeMonitoring(reason: String) {
            examRuntimeMonitoringArmed = true
            recordAction(
                code = "EXAM_RUNTIME_GUARDS_ARMED",
                details = "reason=$reason | screen_pinning_mode=${screenPinningMode.name.lowercase()}",
                level = DiagnosticEventLevel.INFO
            )
        }

        fun disarmExamRuntimeMonitoring() {
            examRuntimeMonitoringArmed = false
            if (accessibilityGuardFallbackActive || AccessibilityExamGuardStore.snapshot(context).armed) {
                AccessibilityExamGuardStore.disarm(context)
                recordAction(
                    code = "ACCESSIBILITY_GUARD_DISARMED",
                    details = "reason=runtime_monitoring_disarmed",
                    level = DiagnosticEventLevel.INFO
                )
            }
            accessibilityGuardFallbackActive = false
            appSwitchLifecycleResumePending = false
            clipboardResumeCheckRunnable?.let(clipboardMainHandler::removeCallbacks)
            clipboardResumeCheckRunnable = null
            overlayFocusLossConfirmRunnable?.let(overlayMainHandler::removeCallbacks)
            overlayFocusLossConfirmRunnable = null
            overlayWindowFocusLossPending = false
            clipboardResumeCheckPending = false
            clipboardPreBackgroundFingerprint = null
            clipboardPreBackgroundSignature = null
            clipboardPreBackgroundSemanticSignature = null
            participantContext = null
        }

        fun recordAppSwitchEvent(
            code: String,
            signal: AppSwitchSignal,
            level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
            updateLastDetectedAt: Boolean = true
        ) {
            val details = currentAppSwitchEventDetails(signal)
            lastAppSwitchTrigger = signal.diagnosticLabel()
            if (updateLastDetectedAt) {
                lastAppSwitchAt = diagnosticTimestamp()
            }
            lastAppSwitchContext = details
            recordAction(
                code = code,
                details = details,
                level = level
            )
        }

        fun acknowledgeRuntimeAlarm(
            type: AlarmAcknowledgeType,
            violationCount: Int,
            buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
            onUiAcknowledge: () -> Unit
        ) {
            val detailRef = latestAlarmDetailRef(
                diagnosticEvents = diagnosticEvents,
                type = type
            )
            val alarmPayload = buildPayload(detailRef)
            onUiAcknowledge()

            if (!examGuardArmed && !examSessionStarted) {
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
                lastAlarmAcknowledgeDedupKey == dedupeKey &&
                nowElapsedMs - lastAlarmAcknowledgeAtElapsedMs <= AlarmAcknowledgeDedupWindowMillis
            ) {
                return
            }

            lastAlarmAcknowledgeDedupKey = dedupeKey
            lastAlarmAcknowledgeAtElapsedMs = nowElapsedMs
            recordAction(
                code = "ALARM_ACKNOWLEDGED",
                details = buildAlarmAckEventDetails(
                    payload = alarmPayload,
                    result = "queued"
                ),
                level = DiagnosticEventLevel.INFO
            )

            coroutineScope.launch {
                sendTelegramAlarmAcknowledge(alarmPayload)
                    .onSuccess {
                        recordAction(
                            code = "ALARM_ACK_TG_SENT",
                            details = buildAlarmAckEventDetails(
                                payload = alarmPayload,
                                result = "sent"
                            ),
                            level = DiagnosticEventLevel.INFO
                        )
                    }
                    .onFailure { error ->
                        val errorSummary = error.message?.take(160)
                            ?: error.javaClass.simpleName.take(160)
                        recordAction(
                            code = "ALARM_ACK_TG_FAILED",
                            details = buildAlarmAckEventDetails(
                                payload = alarmPayload,
                                result = "failed",
                                extra = "error=$errorSummary"
                            ),
                            level = DiagnosticEventLevel.ERROR
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
                baselineSemanticSignatureOverride ?: clipboardDecisionSemanticSignature
            val diagnosticSnapshot =
                if (snapshot.rawSignature.isBlank()) readClipboardSnapshotFull(context) else snapshot
            clipboardSignature = diagnosticSnapshot.rawSignature
            clipboardDecisionFingerprint = diagnosticSnapshot.decisionFingerprint
            clipboardDecisionSemanticSignature = diagnosticSnapshot.semanticSignature
            if (updateObservedSnapshot) {
                lastClipboardObservedAt = eventTimestamp
                lastClipboardObservedSignature = diagnosticSnapshot.rawSignature.ifBlank { null }
            }
            lastClipboardBaselineSemanticSignature = baselineSemanticSignature.ifBlank { null }
            lastClipboardDetectedSemanticSignature = diagnosticSnapshot.semanticSignature.ifBlank { null }
            lastClipboardConfirmedAt = eventTimestamp
            lastClipboardDecision = decision.diagnosticLabel()
            recordAction(
                code = "CLIPBOARD_CHANGED",
                details = "decision=${decision.diagnosticLabel()};source=$eventSuffix",
                level = DiagnosticEventLevel.SECURITY
            )
            lastClipboardChangeEvent = "$eventTimestamp - Clipboard berubah saat sesi ujian ($eventSuffix)"
            clipboardViolationCount += 1
            showClipboardViolationDialog = true
            examAlarmController.start()
        }

        fun armClipboardResumeCheck(reason: String) {
            if (clipboardBypassState == ClipboardBypassState.Active || bypassClipboard) {
                clipboardResumeCheckPending = false
                clipboardPreBackgroundFingerprint = null
                clipboardPreBackgroundSignature = null
                clipboardPreBackgroundSemanticSignature = null
                return
            }
            val beforeBackgroundSnapshot = readClipboardSnapshotLite(context)
            clipboardPreBackgroundFingerprint = beforeBackgroundSnapshot.decisionFingerprint
            clipboardPreBackgroundSignature = null
            clipboardPreBackgroundSemanticSignature = beforeBackgroundSnapshot.semanticSignature.ifBlank { null }
            clipboardResumeCheckPending = true
            lastClipboardDecision = "resume_check_armed:$reason"
        }

        fun applyFatalSecuritySignal(signal: FatalSecuritySignal) {
            recordAction(
                code = signal.eventCode,
                details = signal.details,
                level = DiagnosticEventLevel.SECURITY
            )
            examSessionCancelledByPinningFailure = true
            lockTaskRequestPending = false
            clearAppSwitchSuppression()
            disarmExamRuntimeMonitoring()
            pendingForcedExitViolation = false
            showForcedExitAlarm = false
            screenPinningMessage = null
            showBuiltInExamKeyboard = false
            hasEditableFocus = false
            securityIssueDialogTitle = signal.title
            securityIssueDialogMessage = signal.message
            exitOnSecurityIssueDialogDismiss = true
            examSessionStarted = false
            examSessionStartedAtElapsedMs = null
            webViewErrorMessage = null
            lockTaskBridge.disengage()
            examAlarmController.start()
        }

        fun refreshReverseEngineeringStatus() {
            val cachedResult = reverseEngineeringRefreshCache
            val result =
                if (cachedResult != null && cachedResult.isFresh()) {
                    cachedResult.result
                } else {
                    ReverseEngineeringGuard.inspect(context).also { refreshed ->
                        reverseEngineeringRefreshCache = RuntimeReverseEngineeringRefreshCache(
                            result = refreshed,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                    }
                }
            tamperDetected = result.tamperDetected
            tamperSummary = result.summary()
            if (result.tamperDetected && tamperSummary != tamperLastLoggedSummary) {
                recordAction(
                    code = "TAMPER_DETECTED",
                    details = tamperSummary,
                    level = DiagnosticEventLevel.SECURITY
                )
                tamperLastLoggedSummary = tamperSummary
            }
            if (!result.tamperDetected && tamperLastLoggedSummary != null) {
                tamperLastLoggedSummary = null
            }
        }

        fun refreshIntegrityGuard() {
            val cachedResult = integrityRefreshCache
            val result =
                if (cachedResult != null && cachedResult.isFreshFor(integrityBaselineFingerprint)) {
                    cachedResult.result
                } else {
                    IntegrityGuard.check(context, integrityBaselineFingerprint).also { refreshed ->
                        integrityRefreshCache = RuntimeIntegrityRefreshCache(
                            result = refreshed,
                            baselineFingerprint = integrityBaselineFingerprint,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                    }
                }
            if (integrityBaselineFingerprint.isNullOrBlank() &&
                result.currentFingerprint.isNotBlank() &&
                result.currentFingerprint != "-"
            ) {
                integrityBaselineFingerprint = result.currentFingerprint
            }
            integrityTamperDetected = !result.ok
            integritySummary = result.details
            integrityPublicSummary = buildIntegrityPublicSummary(result.issues)
            val issueSignature = integritySummary.ifBlank { "-" }
            if (!result.ok && issueSignature != integrityLastLoggedSummary) {
                val issueSet = result.issues.toSet()
                if ("dex_hash_mismatch" in issueSet) {
                    recordAction(
                        code = "TAMPER_APK_HASH",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if ("signature_changed" in issueSet) {
                    recordAction(
                        code = "TAMPER_SIGNATURE_CHANGED",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if (issueSet.any { it.startsWith("sysprop_") } || "test_keys" in issueSet) {
                    recordAction(
                        code = "TAMPER_SYSTEM_PROP",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if ("hook_class" in issueSet) {
                    recordAction(
                        code = "TAMPER_HOOK_CLASS",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                integrityLastLoggedSummary = issueSignature
            }
            if (result.ok && integrityLastLoggedSummary != null) {
                integrityLastLoggedSummary = null
            }
        }

        fun hideSystemKeyboard() {
            val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
            webViewInstance?.windowToken?.let { windowToken ->
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            }
            componentActivity.currentFocus?.windowToken?.let { windowToken ->
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            }
        }

        fun showSystemKeyboard() {
            if (useBuiltInExamKeyboard) {
                return
            }
            val inputMethodManager = context.getSystemService(InputMethodManager::class.java) ?: return
            webViewInstance?.post {
                webViewInstance?.isFocusable = true
                webViewInstance?.isFocusableInTouchMode = true
                webViewInstance?.requestFocus(View.FOCUS_DOWN)
                webViewInstance?.requestFocus()
                webViewInstance?.let { inputMethodManager.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
            }
        }

        fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback?) {
            if (fullScreenCustomView != null) {
                callback?.onCustomViewHidden()
                return
            }
            fullScreenCustomView = view
            fullScreenCustomViewCallback = callback
            webViewInstance?.visibility = View.GONE
            (view.parent as? ViewGroup)?.removeView(view)
            fullScreenContainer.removeAllViews()
            fullScreenContainer.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            lockTaskBridge.engage(allowLockTask = false)
        }

        fun hideCustomView() {
            val view = fullScreenCustomView ?: return
            fullScreenContainer.removeView(view)
            fullScreenCustomViewCallback?.onCustomViewHidden()
            fullScreenCustomViewCallback = null
            fullScreenCustomView = null
            webViewInstance?.visibility = View.VISIBLE
            lockTaskBridge.engage(allowLockTask = false)
        }

        fun cleanupActiveExamWebViewInstance() {
            fullScreenCustomView?.let { view ->
                runCatching { fullScreenContainer.removeView(view) }
                runCatching { fullScreenCustomViewCallback?.onCustomViewHidden() }
            }
            fullScreenCustomViewCallback = null
            fullScreenCustomView = null
            hasEditableFocus = false
            webViewInstance?.apply {
                runCatching { stopLoading() }
                runCatching { detachExamKeyboardBridge() }
                runCatching { detachExamParticipantCaptureBridge() }
                runCatching { detachExamNativeFullscreenBridge() }
                runCatching { prepareForFreshExamSession() }
                runCatching { removeAllViews() }
                runCatching { destroy() }
            }
            webViewInstance = null
        }

        suspend fun clearExamSessionOnExit(
            reason: String,
            waitForResult: Boolean
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
                        code = ExamRuntimeHardeningDiagnostics.WebViewExitCleanupJoined,
                        details = requestDetails
                    )
                    return if (waitForResult) {
                        exitSessionClearDeferred?.await() ?: Result.success(Unit)
                    } else {
                        Result.success(Unit)
                    }
                }

                ExamRuntimeExitCleanupDecision.AlreadyCompleted -> {
                    recordAction(
                        code = ExamRuntimeHardeningDiagnostics.WebViewExitCleanupSkipped,
                        details = requestDetails
                    )
                    return Result.success(Unit)
                }

                ExamRuntimeExitCleanupDecision.StartCleanup -> Unit
            }

            val cleanupCompletion = CompletableDeferred<Result<Unit>>()
            exitSessionClearDeferred = cleanupCompletion
            exitSessionClearRequested = true
            exitSessionClearInFlight = true
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.CleanupInFlight

            val existingWebView = if (waitForResult) webViewInstance else null
            val details = buildString {
                append(requestDetails)
                append(" | webview=")
                append(if (existingWebView != null) "present" else "none")
            }
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.WebViewExitCleanupStarted,
                details = details
            )

            val clearResult = try {
                clearExamWebViewSessionData(
                    context = context.applicationContext,
                    existingWebView = existingWebView
                )
            } catch (throwable: Throwable) {
                Result.failure(throwable)
            }

            if (waitForResult) {
                cleanupActiveExamWebViewInstance()
            }
            exitSessionClearInFlight = false
            exitSessionClearDeferred = null
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle

            if (clearResult.isSuccess) {
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.WebViewExitCleanupSucceeded,
                    details = details
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
                    code = failureCode,
                    details = "$details | error=$errorSummary",
                    level = DiagnosticEventLevel.ERROR
                )
            }
            cleanupCompletion.complete(clearResult)

            return clearResult
        }

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
        ): Boolean {
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.RendererGone
            val details = buildString {
                append("did_crash=")
                append(if (didCrash) "yes" else "no")
                append(" | priority_at_exit=")
                append(rendererPriorityAtExit ?: "-")
                append(" | low_ram=")
                append(if (lowRamProfile.enabled) "yes" else "no")
                append(" | severe=")
                append(if (lowRamProfile.severe) "yes" else "no")
                append(" | recovery=manual_safe")
            }
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.WebViewRendererGone,
                details = details,
                level = DiagnosticEventLevel.ERROR
            )
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.CleanupInFlight
            if (view != null && view !== webViewInstance) {
                runCatching {
                    view.stopLoading()
                    view.detachExamKeyboardBridge()
                    view.detachExamParticipantCaptureBridge()
                    view.detachExamNativeFullscreenBridge()
                    view.removeAllViews()
                    view.destroy()
                }
            } else {
                cleanupActiveExamWebViewInstance()
            }
            mainActivity?.setExamLockMode(enabled = false, allowLockTask = false)
            lockTaskBridge.disengage()
            disarmExamRuntimeMonitoring()
            clearAppSwitchSuppression()
            examAlarmController.stop()
            lockTaskRequestPending = false
            examSessionStarted = false
            examSessionStartedAtElapsedMs = null
            showBuiltInExamKeyboard = false
            hasEditableFocus = false
            loadingProgress = 0f
            webViewErrorMessage = null
            webViewSessionResetInFlight = false
            webViewSessionResetError = localized(
                uiLanguage,
                "The exam page stopped and was closed safely. Press Start Exam Mode again to reopen a clean session.",
                "Halaman ujian berhenti dan sudah ditutup dengan aman. Tekan Mulai Ujian lagi untuk membuka sesi bersih."
            )
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.ReadyToRetry
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.WebViewRecoveryReady,
                details = details,
                level = DiagnosticEventLevel.WARNING
            )
            return true
        }

        fun handleRuntimeTrimMemory(level: Int) {
            val memoryAction = resolveExamRuntimeMemoryAction(
                shouldRespondToPressure = MemoryPressureCoordinator.shouldRespondToPressure(level),
                examSessionStarted = examSessionStarted,
                hasFullscreenCustomView = fullScreenCustomView != null
            )
            if (!memoryAction.respond) {
                return
            }
            if (memoryAction.clearWarmLocation) {
                reusableWarmLocationValidation = null
            }
            if (memoryAction.clearReverseEngineeringCache) {
                reverseEngineeringRefreshCache = null
            }
            if (memoryAction.clearIntegrityCache) {
                integrityRefreshCache = null
            }
            if (memoryAction.clearUnusedFullscreenContainer) {
                runCatching { fullScreenContainer.removeAllViews() }
            }
            if (memoryAction.cleanupInactiveWebView) {
                cleanupActiveExamWebViewInstance()
            }
            val actions = memoryAction.diagnosticActions().joinToString(",")
            val details = "trim_level=$level | exam_started=$examSessionStarted | " +
                "low_ram=${lowRamProfile.enabled} | severe=${lowRamProfile.severe} | actions=$actions"
            lastRuntimeMemoryActionSummary = details.take(240)
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.MemoryTrimHandled,
                details = details
            )
            Log.i(
                RuntimeMemoryPerfTag,
                details
            )
        }
    }
    val runtimeMonitoringOps = RuntimeMonitoringOps()
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
    fun hideSystemKeyboard() = runtimeMonitoringOps.hideSystemKeyboard()
    fun showSystemKeyboard() = runtimeMonitoringOps.showSystemKeyboard()
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
                hasEditableFocus = focused
                showBuiltInExamKeyboard = useBuiltInExamKeyboard && focused
                if (useBuiltInExamKeyboard && focused) {
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

    class RuntimeSecurityOps {
        fun refreshKeyboardSecurity(triggerViolation: Boolean) {
            val latestPackage = getCurrentInputMethodPackage(context).orEmpty()
            val latestLabel = resolveKeyboardAppLabel(context, latestPackage)
            val allowedNow = if (bypassKeyboardPolicy) true else isAllowedExamKeyboard(context, latestPackage)

            if (bypassKeyboardPolicy) {
                currentKeyboardPackage = latestPackage
                currentKeyboardLabel = latestLabel
                lastKeyboardAllowed = true
                if (!examSessionStarted) {
                    useBuiltInExamKeyboard = false
                    showBuiltInExamKeyboard = false
                    hasEditableFocus = false
                }
                return
            }

            if (!examSessionStarted && allowedNow) {
                useBuiltInExamKeyboard = false
            }

            if (triggerViolation && !useBuiltInExamKeyboard && lastKeyboardAllowed && !allowedNow) {
                recordAction(
                    code = "KEYBOARD_POLICY_VIOLATION",
                    details = latestPackage,
                    level = DiagnosticEventLevel.SECURITY
                )
                keyboardViolationCount += 1
                showKeyboardViolationDialog = true
                examAlarmController.start()
            }

            currentKeyboardPackage = latestPackage
            currentKeyboardLabel = latestLabel
            lastKeyboardAllowed = allowedNow
        }

        fun refreshBluetoothSecurity(triggerViolation: Boolean) {
            bluetoothPermissionGranted = hasBluetoothExamPermission(context)
            val enabledNow = if (bluetoothPermissionGranted) {
                isBluetoothEnabledForExam(context)
            } else {
                false
            }

            if (!bypassBluetooth && triggerViolation && examSessionStarted && enabledNow) {
                recordAction(
                    code = "BLUETOOTH_ENABLED_DURING_EXAM",
                    level = DiagnosticEventLevel.SECURITY
                )
                bluetoothViolationCount += 1
                showBluetoothViolationDialog = true
                examAlarmController.start()
            }

            bluetoothEnabled = enabledNow
        }

        fun refreshScreenPinningDiagnostics() {
            screenPinningAvailable = ScreenPinningPlatformBridge.isAvailable()
            screenPinningEnabledInSystem = ScreenPinningPlatformBridge.readSystemSetting(context)
            val currentLockTaskState = lockTaskBridge.stateLabel()
            lockTaskStateAfterPinningRequest = currentLockTaskState
            if (screenPinningRequestOutcome == "Belum diminta") {
                lockTaskStateBeforePinningRequest = currentLockTaskState
            }
        }

        fun checkSignatureIntegrity(triggerViolation: Boolean): SignatureIntegrityResult {
            val expectedFingerprints = resolveExpectedSigningFingerprints(
                isDebugBuild = BuildConfig.DEBUG,
                releaseFingerprint = SecureStrings.signingFingerprintRelease,
                debugFingerprint = SecureStrings.signingFingerprintDebug
            )
            val result = SignatureIntegrity.check(context, expectedFingerprints)
            signatureMismatchDetected = !result.isMatch
            if (!result.isMatch && triggerViolation) {
                recordAction(
                    code = "SIGNATURE_MISMATCH_DETECTED",
                    details = result.reason,
                    level = DiagnosticEventLevel.SECURITY
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "App Integrity Warning",
                    "Integritas Aplikasi Bermasalah"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "The app signature does not match the official release. Reinstall the official APK.",
                    "Signature aplikasi tidak cocok dengan APK resmi. Instal ulang APK resmi."
                )
            }
            return result
        }

        fun applyVirtualEnvironmentDiagnostics(
            diagnostics: VirtualEnvironmentDiagnostics,
            triggerViolation: Boolean
        ) {
            val latestVirtualEnvironmentDetected = diagnostics.detected
            if (
                triggerViolation &&
                examSessionStarted &&
                !bypassVirtualEnvironment &&
                !virtualEnvironmentDetected &&
                latestVirtualEnvironmentDetected
            ) {
                recordAction(
                    code = "VIRTUAL_ENVIRONMENT_DETECTED",
                    details = diagnostics.indicators.joinToString().ifBlank { "-" },
                    level = DiagnosticEventLevel.SECURITY
                )
                securityIssueDialogTitle = "Virtual Environment Terdeteksi"
                securityIssueDialogMessage =
                    "Perangkat ini terdeteksi berjalan di emulator/VM. Gunakan perangkat fisik untuk melanjutkan ujian."
                examAlarmController.start()
            }
            virtualEnvironmentDetected = latestVirtualEnvironmentDetected
        }

        fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) {
            val latestAccessibilityInspection = inspectAccessibility(context)
            val latestAccessibilityServiceEnabled = latestAccessibilityInspection.blockingServiceActive
            val latestAdbInspection = inspectAdb(context)
            val rootDetectionDetails = getRootDetectionDetails(context)
            val latestRootSecurityStatus = buildRootSecurityStatus(rootDetectionDetails)
            val cachedVirtualEnvironmentDiagnostics = getCachedVirtualEnvironmentDiagnostics()
            checkSignatureIntegrity(triggerViolation)

            if (triggerViolation && examSessionStarted) {
                if (!bypassAccessibility && !accessibilityServiceEnabled && latestAccessibilityServiceEnabled) {
                    recordAction(
                        code = "ACCESSIBILITY_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Accessibility Service Terdeteksi"
                    securityIssueDialogMessage =
                        "Aksesibilitas aktif saat ujian berjalan. Nonaktifkan accessibility service agar ujian tetap aman."
                    examAlarmController.start()
                }

                if (!bypassAdb && !developerOptionsEnabled && latestAdbInspection.developerOptionsEnabled) {
                    recordAction(
                        code = "DEVELOPER_OPTIONS_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Developer Mode Aktif"
                    securityIssueDialogMessage =
                        "Developer Mode terdeteksi aktif saat ujian berjalan. Nonaktifkan sebelum melanjutkan."
                    examAlarmController.start()
                }

                if (!bypassAdb && !adbEnabled && latestAdbInspection.adbEnabled) {
                    recordAction(
                        code = "ADB_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "USB Debugging (ADB) Aktif"
                    securityIssueDialogMessage =
                        "USB debugging terdeteksi aktif saat ujian berjalan. Nonaktifkan ADB sebelum melanjutkan."
                    examAlarmController.start()
                }

                if (!bypassRoot && !rootDetected && latestRootSecurityStatus.detected) {
                    recordAction(
                        code = "ROOT_INDICATOR_DETECTED",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Root Device Terdeteksi"
                    securityIssueDialogMessage = buildRootIssueMessage(latestRootSecurityStatus.details)
                    examAlarmController.start()
                }
            }

            accessibilityInspection = latestAccessibilityInspection
            accessibilityServiceEnabled = latestAccessibilityServiceEnabled
            accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
            adbInspection = latestAdbInspection
            developerOptionsEnabled = latestAdbInspection.developerOptionsEnabled
            adbEnabled = latestAdbInspection.adbEnabled
            rootSecurityStatus = latestRootSecurityStatus
            rootDetected = latestRootSecurityStatus.detected
            selinuxPermissiveWarning = latestRootSecurityStatus.selinuxPermissive
            if (cachedVirtualEnvironmentDiagnostics != null) {
                applyVirtualEnvironmentDiagnostics(cachedVirtualEnvironmentDiagnostics, triggerViolation)
            } else {
                coroutineScope.launch {
                    val diagnostics = getVirtualEnvironmentDiagnosticsOnIo(context)
                    applyVirtualEnvironmentDiagnostics(diagnostics, triggerViolation)
                }
            }
        }

        fun launchTelegramSectionReport(section: DiagnosticSection) {
            if (sendingSection != null) {
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
            val latestRootSecurityStatus = buildRootSecurityStatus(getRootDetectionDetails(context))
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
            recordAction(code = "DIAGNOSTIC_SECTION_REQUESTED", details = section.name)
            refreshScreenPinningDiagnostics()
            refreshKeyboardSecurity(triggerViolation = false)
            refreshBluetoothSecurity(triggerViolation = false)
            refreshDeviceIntegritySecurity(triggerViolation = false)
            refreshIntegrityGuard()
            val latestDeviceTimeStatus = refreshDeviceTimeSecurity(
                trigger = "diagnostic_request",
                emitDiagnosticEvent = false
            )
            sendingSection = section

            coroutineScope.launch {
                val latestLocationStatus = refreshGeofenceStatus(
                    preferFresh = false,
                    trigger = "diagnostic_request",
                    allowRuntimeViolation = false
                )
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
                    diagnosticEvents = diagnosticEvents,
                    uiLanguage = uiLanguage,
                    webViewCompatibilityStatus = webViewCompatibilityStatus,
                    lastExamRefreshDecision = lastExamRefreshDecision,
                    networkReadinessStatus = networkReadinessStatus,
                    networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
                    networkTimelinePreview = networkTimelinePreview,
                    lastNetworkChangeAt = lastNetworkChangeAt,
                    lastNetworkChangeSource = lastNetworkChangeSource,
                    lastConnectedNetworkLabel = lastConnectedNetworkLabel
                ).onSuccess {
                    recordAction(code = "DIAGNOSTIC_SECTION_SENT", details = section.name)
                    bugReportFeedbackTitle = localized(uiLanguage, "Diagnostics sent", "Diagnostik terkirim")
                    bugReportFeedbackMessage = localized(
                        uiLanguage,
                        "$sectionLabel diagnostics have been sent to Telegram.",
                        "Diagnostik $sectionLabel sudah dikirim ke Telegram."
                    )
                }.onFailure { throwable ->
                    recordAction(
                        code = "DIAGNOSTIC_SECTION_FAILED",
                        details = throwable.message ?: "-",
                        level = DiagnosticEventLevel.ERROR
                    )
                    bugReportFeedbackTitle = localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                    bugReportFeedbackMessage =
                        throwable.message ?: localized(
                            uiLanguage,
                            "Diagnostics could not be sent to Telegram.",
                            "Data diagnostik belum berhasil dikirim ke Telegram."
                        )
                }

                sendingSection = null
            }
        }

    }
    val runtimeSecurityOps = RuntimeSecurityOps()
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
            recordAction(
                code = message.code,
                details = message.details,
                level = level
            )
            securityIssueDialogTitle = message.title
            securityIssueDialogMessage = message.message
        }

        fun resetPreparationSecurityEpisodes() {
            resetStartExamPreparationSecurityEpisodes(flowUiState)
        }

        fun finalizeExamSessionStart(lockTaskAlreadyActive: Boolean) {
            finalizeStartExamSession(
                context = context,
                lockTaskBridge = lockTaskBridge,
                flowUiState = flowUiState,
                adminUiState = adminUiState,
                clipboardUiState = clipboardUiState,
                lockTaskAlreadyActive = lockTaskAlreadyActive,
                hideSystemKeyboard = ::hideSystemKeyboard,
                showSystemKeyboard = ::showSystemKeyboard
            )
        }

        suspend fun prepareCleanExamWebViewSessionForStart(): Boolean {
            return prepareCleanExamWebViewSessionForStart(
                context = context,
                existingWebView = webViewInstance,
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
            if (lockTaskRequestPending || geofenceStartValidationInFlight || webViewSessionResetInFlight) {
                return
            }

            if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                accessibilityGuardEnabled
            ) {
                launchAccessibilityGuardFallbackExamStart(
                    context = context,
                    lockTaskBridge = lockTaskBridge,
                    coroutineScope = coroutineScope,
                    examGuardArmed = examGuardArmed,
                    updateFallbackUiState = { beforeState ->
                        accessibilityGuardFallbackActive = true
                        accessibilityGuardLastReason = null
                        accessibilityGuardLastForeignPackage = null
                        accessibilityGuardLastEventType = null
                        accessibilityGuardLastDetectedAt = null
                        accessibilityGuardAlarmSeverity = ExamAlarmSeverity.Warning.name
                        forcedExitViolationCount = 0
                        pendingForcedExitViolation = false
                        showForcedExitAlarm = false
                        lockTaskStateBeforePinningRequest = beforeState
                        lockTaskStateAfterPinningRequest = beforeState
                        screenPinningRequestOutcome = "Accessibility guard fallback"
                        screenPinningDialogLikelyShown = false
                        screenPinningUserActionInference = "Tidak diminta; Accessibility Exam Guard aktif"
                        screenPinningActivationDurationMs = 0L
                        examSessionCancelledByPinningFailure = false
                        lockTaskRequestPending = false
                        pinningActivationState = PinningActivationState.ActiveConfirmed
                        pinningActivationStartedAtElapsedMs = null
                        pinningSuppressedTransitionCount = 0
                        screenPinningMessage = null
                        webViewErrorMessage = null
                        exitOnSecurityIssueDialogDismiss = false
                    },
                    recordAction = { code, details, level -> recordAction(code, details, level) },
                    clearAppSwitchSuppression = ::clearAppSwitchSuppression,
                    resetPreparationSecurityEpisodes = this::resetPreparationSecurityEpisodes,
                    prepareCleanExamWebViewSessionForStart = this::prepareCleanExamWebViewSessionForStart,
                    armExamRuntimeMonitoring = ::armExamRuntimeMonitoring,
                    finalizeExamSessionStart = this::finalizeExamSessionStart,
                    onCleanSessionFailed = { accessibilityGuardFallbackActive = false }
                )
                return
            }

            AccessibilityExamGuardStore.disarm(context)
            accessibilityGuardFallbackActive = false

            if (screenPinningMode == ScreenPinningMode.Bypassed) {
                val bypassState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
                recordAction(code = bypassState.eventCode, details = bypassState.eventDetails)
                lockTaskStateBeforePinningRequest = bypassState.beforeState
                lockTaskStateAfterPinningRequest = bypassState.afterState
                screenPinningRequestOutcome = bypassState.outcome
                screenPinningDialogLikelyShown = bypassState.dialogLikelyShown
                screenPinningUserActionInference = bypassState.userActionInference
                screenPinningActivationDurationMs = bypassState.activationDurationMs
                examSessionCancelledByPinningFailure = false
                lockTaskRequestPending = false
                pinningActivationState = PinningActivationState.Idle
                pinningActivationStartedAtElapsedMs = null
                pinningSuppressedTransitionCount = 0
                clearAppSwitchSuppression()
                screenPinningMessage = null
                webViewErrorMessage = null
                exitOnSecurityIssueDialogDismiss = false
                resetPreparationSecurityEpisodes()
                coroutineScope.launch {
                    if (!prepareCleanExamWebViewSessionForStart()) {
                        return@launch
                    }
                    if (!examGuardArmed) {
                        armExamRuntimeMonitoring(reason = "start_exam_pressed")
                    }
                    finalizeExamSessionStart(lockTaskAlreadyActive = false)
                }
                return
            }

            if (lockTaskBridge.active()) {
                val activeState = lockTaskBridge.stateLabel()
                lockTaskStateBeforePinningRequest = activeState
                lockTaskStateAfterPinningRequest = activeState
                screenPinningRequestOutcome = ScreenPinningSignals.successOutcome()
                screenPinningDialogLikelyShown = false
                screenPinningUserActionInference = "Sudah aktif; request pinning dilewati"
                screenPinningActivationDurationMs = 0L
                examSessionCancelledByPinningFailure = false
                lockTaskRequestPending = false
                pinningActivationState = PinningActivationState.ActiveConfirmed
                pinningActivationStartedAtElapsedMs = null
                pinningSuppressedTransitionCount = 0
                clearAppSwitchSuppression()
                screenPinningMessage = null
                webViewErrorMessage = null
                exitOnSecurityIssueDialogDismiss = false
                recordAction(
                    code = ScreenPinningSignals.eventActive(),
                    details = "already_active_before_request | state=$activeState",
                    level = DiagnosticEventLevel.INFO
                )
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.ScreenPinningAlreadyActive,
                    details = "state=$activeState | request_pending=false",
                    level = DiagnosticEventLevel.INFO
                )
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.ScreenPinningRequestSkippedAlreadyActive,
                    details = "state=$activeState | policy_skip_if_active=${deviceCompatibilityProfile.skipScreenPinningRequestWhenAlreadyActive}",
                    level = DiagnosticEventLevel.INFO
                )
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.PinningActiveConfirmed,
                    details = "already_active_before_request=true | state=$activeState | duration_ms=0",
                    level = DiagnosticEventLevel.INFO
                )
                resetPreparationSecurityEpisodes()
                if (!examGuardArmed) {
                    armExamRuntimeMonitoring(reason = "start_exam_pressed_pinning_already_active")
                }
                lockTaskBridge.engage(allowLockTask = false)
                coroutineScope.launch {
                    if (!prepareCleanExamWebViewSessionForStart()) {
                        return@launch
                    }
                    finalizeExamSessionStart(lockTaskAlreadyActive = true)
                }
                return
            }

            if (!examGuardArmed) {
                armExamRuntimeMonitoring(reason = "start_exam_pressed")
            }

            val requestState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
            lockTaskStateBeforePinningRequest = requestState.beforeState
            lockTaskStateAfterPinningRequest = requestState.afterState
            screenPinningRequestOutcome = requestState.outcome
            screenPinningDialogLikelyShown = requestState.dialogLikelyShown
            screenPinningUserActionInference = requestState.userActionInference
            screenPinningActivationDurationMs = requestState.activationDurationMs
            examSessionCancelledByPinningFailure = false
            lockTaskRequestPending = true
            pinningActivationState = PinningActivationState.Requested
            pinningActivationStartedAtElapsedMs = SystemClock.elapsedRealtime()
            pinningSuppressedTransitionCount = 0
            recordAction(code = requestState.eventCode, details = requestState.eventDetails)
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PinningStartRequested,
                details = "before=${requestState.beforeState} | state=${requestState.afterState} | grace_ms=$PinningActivationGraceWindowMillis",
                level = DiagnosticEventLevel.INFO
            )
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PinningDialogExpected,
                details = "screen_pinning_dialog_expected=true | keep_app_foreground=true",
                level = DiagnosticEventLevel.INFO
            )
            setAppSwitchSuppression(AppSwitchSuppressionReason.ScreenPinningRequest)
            screenPinningMessage = ScreenPinningEnforcer.activatingMessage(isIndonesian)
            webViewErrorMessage = null
            exitOnSecurityIssueDialogDismiss = false
        }

        suspend fun startExamSession() {
            if (webViewSessionResetInFlight) {
                return
            }
            val startExamPressedAt = SystemClock.elapsedRealtime()
            examRuntimeRecoveryState = ExamRuntimeRecoveryState.Idle
            webViewSessionResetError = null
            recordAction(code = "START_EXAM_PRESSED")
            val startVirtualEnvironmentDiagnostics = getVirtualEnvironmentDiagnosticsOnIo(context)
            runtimeSecurityOps.applyVirtualEnvironmentDiagnostics(
                diagnostics = startVirtualEnvironmentDiagnostics,
                triggerViolation = false
            )
            debugMeasureExamStartWork("startExamSession:tampers") {
                refreshReverseEngineeringStatus()
                refreshIntegrityGuard()
            }
            val securityTamperDetectedNow = tamperDetected || integrityTamperDetected
            if (securityTamperDetectedNow) {
                applyStartExamBlockMessage(resolveStartExamTamperBlockMessage(uiLanguage))
                return
            }
            refreshScreenPinningDiagnostics()
            val latestAccessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context)
            val latestAccessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
            accessibilityGuardEnabled = latestAccessibilityGuardEnabled
            val screenPinningBlock = resolveStartExamScreenPinningBlockMessage(
                uiLanguage = uiLanguage,
                screenPinningMode = screenPinningMode,
                screenPinningAvailable = screenPinningAvailable,
                accessibilityGuardAvailable = latestAccessibilityGuardAvailable,
                accessibilityGuardEnabled = latestAccessibilityGuardEnabled
            )
            if (screenPinningBlock != null) {
                applyStartExamBlockMessage(screenPinningBlock)
                return
            } else if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                latestAccessibilityGuardEnabled
            ) {
                recordAction(
                    code = "ACCESSIBILITY_GUARD_ENABLED_REQUIRED",
                    details = "screen_pinning_available=false | accessibility_guard_enabled=true",
                    level = DiagnosticEventLevel.INFO
                )
            }
            debugMeasureExamStartWork("startExamSession:device_prechecks") {
                refreshScreenPinningDiagnostics()
                accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
                refreshKeyboardSecurity(triggerViolation = false)
                refreshBluetoothSecurity(triggerViolation = false)
                refreshDeviceIntegritySecurity(triggerViolation = false)
            }
            val devicePrecheckScreenPinningBlock = resolveStartExamScreenPinningBlockMessage(
                uiLanguage = uiLanguage,
                screenPinningMode = screenPinningMode,
                screenPinningAvailable = screenPinningAvailable,
                accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
                accessibilityGuardEnabled = accessibilityGuardEnabled,
                phaseSuffix = "phase=device_prechecks"
            )
            if (devicePrecheckScreenPinningBlock != null) {
                applyStartExamBlockMessage(devicePrecheckScreenPinningBlock)
                return
            }
            val startDeviceTimeStatus = refreshDeviceTimeSecurity(trigger = "start_exam_precheck")
            val startDeviceTimeBlock = resolveStartExamDeviceTimeBlockMessage(
                uiLanguage = uiLanguage,
                trigger = "start_exam_precheck",
                status = startDeviceTimeStatus
            )
            if (startDeviceTimeBlock != null) {
                applyStartExamBlockMessage(startDeviceTimeBlock)
                return
            }
            val startNetworkStatus = readNetworkReadinessStatus(context)
            applyNetworkReadinessStatus("start_exam_precheck", startNetworkStatus)
            if (!bypassVpn) {
                val startVpnBlock = resolveStartExamVpnBlockMessage(
                    uiLanguage = uiLanguage,
                    status = startNetworkStatus
                )
                if (startVpnBlock != null) {
                    applyStartExamBlockMessage(startVpnBlock)
                    return
                }
            }
            val startHealthSnapshot = buildPreExamHealthSnapshot(
                PreExamHealthCheckInput(
                    compatibilityProfile = deviceCompatibilityProfile,
                    screenPinningAvailable = screenPinningAvailable,
                    screenPinningActive = lockTaskBridge.active(),
                    screenPinningBypassed = bypassScreenPinning,
                    accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
                    accessibilityGuardEnabled = accessibilityGuardEnabled,
                    overlayRiskResult = overlayRiskResult,
                    overlayBypassed = bypassOverlay,
                    networkReadinessStatus = startNetworkStatus,
                    vpnBypassed = bypassVpn,
                    webViewCompatibilityStatus = webViewCompatibilityStatus,
                    webViewRecoveryState = examRuntimeRecoveryState.name,
                    webViewSessionResetInFlight = webViewSessionResetInFlight,
                    webViewSessionResetError = webViewSessionResetError,
                    geofenceRuntimeStatus = geofenceRuntimeStatus,
                    geofenceBypassed = bypassGeofence,
                    fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
                    fakeLocationBypassed = bypassFakeLocation,
                    deviceTimeSecurityStatus = startDeviceTimeStatus,
                    deviceTimeBypassed = bypassDeviceTime,
                    batteryStatus = batteryStatus,
                    generatedAtElapsedMs = SystemClock.elapsedRealtime()
                )
            )
            val healthBlocker = preExamHealthStartBlocker(startHealthSnapshot)
            if (healthBlocker != null) {
                recordAction(
                    code = ExamRuntimeHardeningDiagnostics.StartExamBlockedHealthCheck,
                    details = "category=${healthBlocker.category.name} | verdict=${healthBlocker.verdict.name} | detail=${healthBlocker.detail}",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Pre-Exam Health Check",
                    "Health Check Sebelum Ujian"
                )
                securityIssueDialogMessage = buildString {
                    append(healthBlocker.title)
                    append("\n\n")
                    append(healthBlocker.detail)
                    if (!healthBlocker.quickFix.isNullOrBlank()) {
                        append("\n\n")
                        append(healthBlocker.quickFix)
                    }
                }
                return
            }
            val signatureResult = debugMeasureExamStartWork("startExamSession:signature_check") {
                checkSignatureIntegrity(triggerViolation = true)
            }
            if (ExamPolicyEngine.shouldBlock(signatureResult)) {
                recordAction(
                    code = "START_EXAM_BLOCKED_SIGNATURE",
                    level = DiagnosticEventLevel.WARNING
                )
                return
            }
            val keyboardAllowedNow = lastKeyboardAllowed
            val builtInKeyboardNeeded = !bypassKeyboardPolicy && !keyboardAllowedNow
            val bluetoothPermissionReady =
                bluetoothPermissionGranted || !requiresBluetoothExamPermission()

            useBuiltInExamKeyboard = builtInKeyboardNeeded
            showBuiltInExamKeyboard = builtInKeyboardNeeded
            builtInKeyboardShiftEnabled = false

            if (!bypassBluetooth) {
                if (!bluetoothPermissionReady) {
                    bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
                    return
                }

                if (bluetoothEnabled) {
                    showBluetoothViolationDialog = true
                    return
                }
            }

            val staticSecurityBlock = resolveStartExamStaticSecurityBlockMessage(
                bypassAccessibility = bypassAccessibility,
                accessibilityServiceEnabled = accessibilityServiceEnabled,
                bypassAdb = bypassAdb,
                developerOptionsEnabled = developerOptionsEnabled,
                bypassVirtualEnvironment = bypassVirtualEnvironment,
                virtualEnvironmentDetected = virtualEnvironmentDetected,
                adbEnabled = adbEnabled,
                adbInsecureSystemProperty = adbInspection.insecureSystemProperty,
                bypassRoot = bypassRoot,
                rootSecurityStatus = rootSecurityStatus
            )
            if (staticSecurityBlock != null) {
                applyStartExamBlockMessage(staticSecurityBlock)
                return
            }

            if (geofenceEnabled && !bypassGeofence && geofenceConfigParseResult.config == null) {
                geofenceSecurityStatus = evaluateGeofenceSecurity(
                    configResult = geofenceConfigParseResult,
                    permissionGranted = hasLocationPermissionForWifi(context),
                    preciseLocationGranted = hasFineLocationPermission(context),
                    locationServicesEnabled = isLocationServicesEnabled(context),
                    locationSnapshot = null,
                    bypassState = geofenceBypassState
                )
                geofenceEvaluation = geofenceSecurityStatus.geofenceEvaluation
                applyStartExamBlockMessage(
                    resolveStartExamGeofenceConfigBlockMessage(
                        uiLanguage = uiLanguage,
                        details = currentGeofenceEventDetails(
                            trigger = "start_exam",
                            geofenceStatus = geofenceSecurityStatus
                        )
                    )
                )
                return
            }

            val coarseOrFineGranted = hasLocationPermissionForWifi(context)
            val preciseLocationGranted = hasFineLocationPermission(context)
            val preciseLocationRequiredForStart = geofenceEnabled && !bypassGeofence
            if (
                (preciseLocationRequiredForStart && !preciseLocationGranted) ||
                (!preciseLocationRequiredForStart && !bypassFakeLocation && !coarseOrFineGranted)
            ) {
                pendingStartExamAfterLocationPermission = true
                geofencePermissionRequestInFlight = true
                recordAction(
                    code = "LOCATION_PERMISSION_REQUESTED",
                    details = "trigger=start_exam",
                    level = DiagnosticEventLevel.WARNING
                )
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                debugLogExamStart(
                    "startExamSession waiting for location permission after ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
                )
                return
            }

            if (!bypassGeofence && geofenceEnabled && !isLocationServicesEnabled(context)) {
                geofenceSecurityStatus = evaluateGeofenceSecurity(
                    configResult = geofenceConfigParseResult,
                    permissionGranted = true,
                    preciseLocationGranted = true,
                    locationServicesEnabled = false,
                    locationSnapshot = null,
                    bypassState = geofenceBypassState
                )
                geofenceEvaluation = geofenceSecurityStatus.geofenceEvaluation
                applyStartExamBlockMessage(
                    resolveStartExamGeofenceLocationDisabledBlockMessage(
                        uiLanguage = uiLanguage,
                        details = currentGeofenceEventDetails(
                            trigger = "start_exam",
                            geofenceStatus = geofenceSecurityStatus
                        )
                    )
                )
                return
            }

            if (!bypassFakeLocation && !isLocationServicesEnabled(context)) {
                fakeLocationSecurityStatus = evaluateFakeLocationSecurity(
                    monitoringEnabled = true,
                    permissionGranted = hasLocationPermissionForWifi(context),
                    locationServicesEnabled = false,
                    locationSnapshot = null,
                    fixQualityStatus = evaluateLocationFixQuality(null),
                    developerOptionsEnabled = developerOptionsEnabled,
                    suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
                    bypassState = fakeLocationBypassState
                )
                applyStartExamBlockMessage(
                    resolveStartExamFakeLocationServicesDisabledBlockMessage(
                        uiLanguage = uiLanguage,
                        details = currentFakeLocationEventDetails(
                            trigger = "start_exam",
                            fakeLocationStatus = fakeLocationSecurityStatus
                        )
                    )
                )
                return
            }

            if (geofenceStartValidationInFlight) {
                return
            }

            geofenceStartValidationInFlight = true
            coroutineScope.launch {
                val latestLocationStatus = debugMeasureExamStartSuspendWork("startExamSession:location_validation") {
                    resolveStartExamLocationValidation()
                }
                geofenceStartValidationInFlight = false
                val locationBlockMessage = resolveStartExamLocationBlockMessage(
                    uiLanguage = uiLanguage,
                    latestLocationStatus = latestLocationStatus,
                    bypassGeofence = bypassGeofence,
                    bypassFakeLocation = bypassFakeLocation,
                    geofenceDetails = { geofenceStatus ->
                        currentGeofenceEventDetails(
                            trigger = "start_exam",
                            geofenceStatus = geofenceStatus
                        )
                    },
                    fakeLocationDetails = { fakeLocationStatus ->
                        currentFakeLocationEventDetails(
                            trigger = "start_exam",
                            fakeLocationStatus = fakeLocationStatus
                        )
                    }
                )
                if (locationBlockMessage != null) {
                    applyStartExamBlockMessage(locationBlockMessage)
                    return@launch
                }

                val finalDeviceTimeStatus = refreshDeviceTimeSecurity(
                    trigger = "start_exam_final",
                    emitDiagnosticEvent = false
                )
                val finalDeviceTimeBlock = resolveStartExamDeviceTimeBlockMessage(
                    uiLanguage = uiLanguage,
                    trigger = "start_exam_final",
                    status = finalDeviceTimeStatus
                )
                if (finalDeviceTimeBlock != null) {
                    applyStartExamBlockMessage(finalDeviceTimeBlock)
                    return@launch
                }
                val networkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                val scheduleValidationResult = ExamScheduleValidator.validateAfterDeviceTimeCheck(
                    payload = payload,
                    deviceTimeStatus = finalDeviceTimeStatus,
                    networkNowMillis = networkNowMillis
                )
                val scheduleBlock = resolveStartExamScheduleBlockMessage(
                    uiLanguage = uiLanguage,
                    payload = payload,
                    validationResult = scheduleValidationResult,
                    networkNowMillis = networkNowMillis,
                    deviceTimeStatus = finalDeviceTimeStatus
                )
                if (scheduleBlock != null) {
                    applyStartExamBlockMessage(scheduleBlock)
                    return@launch
                }
                debugLogExamStart(
                    "startExamSession passed all prechecks in ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
                )
                completeStartExamSessionAfterPrechecks()
            }
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
        if (!lockTaskRequestPending || examSessionStarted) {
            return
        }
        if (lockTaskBridge.active()) {
            pinningActivationState = PinningActivationState.ActiveConfirmed
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.ScreenPinningAlreadyActive,
                details = "transition_interrupt_ignored | state=${lockTaskBridge.stateLabel()}",
                level = DiagnosticEventLevel.INFO
            )
            return
        }

        val stateAfterInterrupt = lockTaskBridge.stateLabel()
        lockTaskStateAfterPinningRequest = stateAfterInterrupt
        screenPinningDialogLikelyShown = true
        val nowElapsedMs = SystemClock.elapsedRealtime()
        val startedAt = pinningActivationStartedAtElapsedMs
        val elapsedMs = startedAt?.let { (nowElapsedMs - it).coerceAtLeast(0L) }
        val withinGrace = shouldSuppressPinningTransitionViolation(
            lockTaskRequestPending = lockTaskRequestPending,
            examSessionStarted = examSessionStarted,
            startedAtElapsedMs = startedAt,
            nowElapsedMs = nowElapsedMs
        )
        pinningSuppressedTransitionCount += 1
        pinningActivationState = PinningActivationState.WaitingForLockTaskActive
        screenPinningMessage = ScreenPinningEnforcer.activatingMessage(isIndonesian)
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.PinningTransitionViolationSuppressed,
            details = "source=user_leave_hint | state=$stateAfterInterrupt | elapsed_ms=${elapsedMs ?: -1} | within_grace=$withinGrace | suppressed_count=$pinningSuppressedTransitionCount | wait_until_timeout=true",
            level = DiagnosticEventLevel.WARNING
        )
        if (!withinGrace) {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.ScreenPinningTransitionInterrupted,
                details = "source=user_leave_hint | state=$stateAfterInterrupt | elapsed_ms=${elapsedMs ?: -1} | suppressed_until_timeout=true",
                level = DiagnosticEventLevel.WARNING
            )
        }
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
        examAlarmController = examAlarmController,
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
        recordOverlayEvent = ::recordOverlayEvent,
        onScreenPinningTransitionInterrupted = ::handleScreenPinningTransitionInterrupted,
        armClipboardResumeCheck = ::armClipboardResumeCheck
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

    fun handleChooseKeyboard() {
        recordAction(code = "KEYBOARD_PICKER_OPENED")
        if (!showKeyboardPicker(activity)) {
            openKeyboardSettings(context)
        }
    }

    fun handleOpenKeyboardSettings() {
        recordAction(code = "KEYBOARD_SETTINGS_OPENED")
        openKeyboardSettings(context)
    }

    fun handleGrantBluetoothPermission() {
        recordAction(code = "BLUETOOTH_PERMISSION_REQUESTED")
        bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
    }

    fun handleOpenBluetoothSettings() {
        recordAction(code = "BLUETOOTH_SETTINGS_OPENED")
        openBluetoothSettings(context)
    }

    fun handleOpenAccessibilitySettings() {
        recordAction(code = "ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenOverlayAccessibilitySettings() {
        recordAction(code = "OVERLAY_ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenDeveloperOptionsSettings() {
        recordAction(code = "DEVELOPER_OPTIONS_OPENED")
        openDeveloperOptionsSettings(context)
    }

    fun handleRequestLocationPermission() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "LOCATION_PERMISSION_REQUESTED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        geofencePermissionRequestInFlight = true
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun handleOpenLocationServicesSettings() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "LOCATION_SERVICES_SETTINGS_OPENED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        openLocationServicesSettings(context)
    }

    fun handleRefreshLocationSecurity() {
        launchLocationSecurityManualRefresh(trigger = "location_quick_fix")
        recordAction(
            code = "LOCATION_QUICK_FIX_REFRESH_REQUESTED",
            details = "trigger=checklist",
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenGeofenceMapViewer() {
        showGeofenceMapViewer = true
        recordAction(
            code = "GEOFENCE_MAP_VIEW_OPENED",
            details = currentGeofenceEventDetails(
                trigger = "checklist_map_view",
                geofenceStatus = geofenceSecurityStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenInternetSettings() {
        recordAction(
            code = "INTERNET_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openInternetConnectivitySettings(context)
    }

    fun handleOpenVpnSettings() {
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.VpnSettingsOpened,
            details = currentNetworkEventDetails(
                trigger = "network_vpn_quick_fix",
                status = networkReadinessStatus,
                extraContext = "bypass=${if (bypassVpn) "yes" else "no"}"
            ),
            level = DiagnosticEventLevel.INFO
        )
        openVpnSettings(context)
    }

    fun handleOpenDateTimeSettings() {
        recordAction(
            code = "DEVICE_TIME_SETTINGS_OPENED",
            details = buildDeviceTimeEventDetails(
                trigger = "device_time_quick_fix",
                status = refreshDeviceTimeSecurity(
                    trigger = "device_time_quick_fix",
                    emitDiagnosticEvent = false
                )
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDateTimeSettings(context)
    }

    fun handleOpenWifiSettings() {
        recordAction(
            code = "WIFI_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_wifi_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openWifiSettings(context)
    }

    fun handleOpenCellularSettings() {
        recordAction(
            code = "CELLULAR_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_cellular_quick_fix",
                status = networkReadinessStatus,
                extraContext = "last_connected=" + (lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-")
            ),
            level = DiagnosticEventLevel.INFO
        )
        openCellularSettings(context)
    }

    fun handleOpenAirplaneModeSettings() {
        recordAction(
            code = "AIRPLANE_MODE_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_airplane_mode_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openAirplaneModeSettings(context)
    }

    fun handleRefreshNetworkStatus() {
        launchNetworkManualRefresh(trigger = "network_quick_fix")
        recordAction(
            code = "NETWORK_QUICK_FIX_REFRESH_REQUESTED",
            details = currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenFakeLocationDeveloperOptionsSettings() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "FAKE_LOCATION_DEVELOPER_OPTIONS_OPENED",
            details = currentFakeLocationEventDetails(
                trigger = "fake_location_developer_options_quick_fix",
                fakeLocationStatus = fakeLocationRuntimeStatus.securityStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDeveloperOptionsSettings(context)
    }

    fun handleOpenScreenPinningSettings() {
        recordAction(code = "SCREEN_PINNING_SETTINGS_OPENED")
        openScreenPinningSettings(context)
    }

    fun handleOpenOverlaySettings() {
        recordAction(code = "OVERLAY_SETTINGS_OPENED")
        openOverlaySettings(context)
    }

    fun handleOpenAppSettings() {
        recordAction(code = "APP_SETTINGS_OPENED", details = "quick_fix=screen_recorder")
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun handleOpenCastSettings() {
        recordAction(code = "CAST_SETTINGS_OPENED", details = "quick_fix=display_mirror")
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun handleOpenWebViewProviderSettings() {
        recordAction(
            code = "WEBVIEW_PROVIDER_SETTINGS_OPENED",
            details = webViewCompatibilityStatus.adminDetail
        )
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.WebViewProviderHealthFixOpened,
            details = webViewCompatibilityStatus.adminDetail
        )
        openWebViewProviderSettings(
            context = context,
            providerPackageName = webViewCompatibilityStatus.packageName
        )
    }

    fun handleReinstallOfficialApk() {
        recordAction(code = "OFFICIAL_APK_REINSTALL_OPENED")
        openExternalUrl(context, officialApkUrl)
    }

    fun refreshPreparationStatusChecks() {
        val startedAt = SystemClock.elapsedRealtime()
        if (!networkManualRefreshInFlight) {
            launchNetworkManualRefresh(trigger = "checklist_refresh")
        } else {
            updateNetworkReadiness("checklist_refresh")
        }
        if (!geofenceManualRefreshInFlight) {
            launchLocationSecurityManualRefresh(trigger = "checklist_refresh")
        }
        refreshReverseEngineeringStatus()
        refreshIntegrityGuard()
        refreshScreenPinningDiagnostics()
        webViewCompatibilityRefreshKey += 1
        accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
        refreshKeyboardSecurity(triggerViolation = false)
        refreshBluetoothSecurity(triggerViolation = false)
        refreshDeviceIntegritySecurity(triggerViolation = false)
        refreshDeviceTimeSecurity(trigger = "checklist_refresh")
        debugLogExamStart(
            "refreshPreparationStatusChecks scheduled in ${SystemClock.elapsedRealtime() - startedAt} ms"
        )
    }

    fun handleRefreshPreparationStatus() {
        recordAction(code = "SECURITY_STATUS_REFRESHED")
        refreshPreparationStatusChecks()
    }

    fun handleRefreshAllSecurityChecks() {
        recordAction(
            code = "ALL_SECURITY_CHECKS_REFRESH_REQUESTED",
            details = "source=quick_fixes",
            level = DiagnosticEventLevel.INFO
        )
        refreshPreparationStatusChecks()
    }

    fun handleRefreshPreExamHealthCheck() {
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.PreExamHealthCheckStarted,
            details = "source=pre_exam_health | ${deviceCompatibilityProfile.diagnosticSummary()}"
        )
        refreshPreparationStatusChecks()
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.PreExamHealthCheckCompleted,
            details = "source=pre_exam_health | family=${deviceCompatibilityProfile.family.name}"
        )
    }

    fun handleRequestSectionReport(section: DiagnosticSection) {
        pendingSection = section
    }

    fun buildCurrentPreExamHealthSnapshot() = buildPreExamHealthSnapshot(
        PreExamHealthCheckInput(
            compatibilityProfile = deviceCompatibilityProfile,
            screenPinningAvailable = screenPinningAvailable,
            screenPinningActive = lockTaskBridge.active(),
            screenPinningBypassed = bypassScreenPinning,
            accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            overlayRiskResult = overlayRiskResult,
            overlayBypassed = bypassOverlay,
            networkReadinessStatus = networkReadinessStatus,
            vpnBypassed = bypassVpn,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewRecoveryState = examRuntimeRecoveryState.name,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            webViewSessionResetError = webViewSessionResetError,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            geofenceBypassed = bypassGeofence,
            fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
            fakeLocationBypassed = bypassFakeLocation,
            deviceTimeSecurityStatus = deviceTimeSecurityStatus,
            deviceTimeBypassed = bypassDeviceTime,
            batteryStatus = batteryStatus,
            generatedAtElapsedMs = SystemClock.elapsedRealtime()
        )
    )

    val preExamHealthCheckSnapshot = buildCurrentPreExamHealthSnapshot()
    val deviceSurvivalPolicy = buildDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        preExamHealthSnapshot = preExamHealthCheckSnapshot
    )

    fun buildCurrentExamDiagnosticSnapshot(source: String): ExamDiagnosticSnapshot {
        return buildExamDiagnosticSnapshot(
            ExamDiagnosticSnapshotInput(
                source = source,
                lowRamProfile = lowRamProfile,
                deviceCompatibilityProfile = deviceCompatibilityProfile,
                deviceSurvivalPolicy = deviceSurvivalPolicy,
                previousExamSessionBreadcrumb = PreviousExamSessionBreadcrumbStore.read(context),
                payload = payload,
                examSessionStarted = examSessionStarted,
                examGuardArmed = examGuardArmed,
                webViewPresent = webViewInstance != null,
                webViewCompatibilityStatus = webViewCompatibilityStatus,
                webViewError = webViewErrorMessage ?: webViewSessionResetError,
                rendererGone = examRuntimeRecoveryState == ExamRuntimeRecoveryState.RendererGone ||
                    examRuntimeRecoveryState == ExamRuntimeRecoveryState.ReadyToRetry,
                recoveryState = examRuntimeRecoveryState,
                lastTrimMemoryAction = lastRuntimeMemoryActionSummary,
                networkReadinessStatus = networkReadinessStatus,
                geofenceRuntimeStatus = geofenceRuntimeStatus,
                fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
                deviceTimeSecurityStatus = deviceTimeSecurityStatus,
                preExamHealthSnapshot = buildCurrentPreExamHealthSnapshot(),
                lastPinningDecision = screenPinningUserActionInference,
                lastOverlayDecision = lastOverlayContext,
                lastRefreshDecision = lastExamRefreshDecision,
                diagnosticEvents = diagnosticEvents
            )
        )
    }

    fun handleExportExamDiagnostics(source: String) {
        recordAction(
            code = ExamRuntimeHardeningDiagnostics.DiagnosticExportRequested,
            details = "source=$source"
        )
        val snapshot = buildCurrentExamDiagnosticSnapshot(source)
        runCatching {
            ExamDiagnosticExportHelper.share(context, snapshot)
        }.onSuccess {
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.DiagnosticExportSucceeded,
                details = "source=$source | events=${snapshot.events.size}"
            )
        }.onFailure { throwable ->
            val errorSummary = throwable.message?.take(160)
                ?: throwable.javaClass.simpleName.take(160)
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.DiagnosticExportFailed,
                details = "source=$source | error=$errorSummary",
                level = DiagnosticEventLevel.ERROR
            )
            bugReportFeedbackTitle = localized(
                uiLanguage,
                "Diagnostics export failed",
                "Export diagnostik gagal"
            )
            bugReportFeedbackMessage = errorSummary
        }
    }

    fun handleStartExam() {
        writePreviousSessionBreadcrumb(
            code = PreviousExamSessionBreadcrumbCodes.StartPressed,
            details = "score=${deviceSurvivalPolicy.score.name} | health_blocking=${deviceSurvivalPolicy.healthBlockingCount}"
        )
        coroutineScope.launch {
            startExamController.startExamSession()
        }
    }
    val examRuntimeViewModel = rememberBoundExamRuntimeViewModel(
        activity = componentActivity,
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
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassVpn = bypassVpn,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeBypassState = deviceTimeBypassState,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        signatureMismatchDetected = signatureMismatchDetected,
        securityTamperDetected = securityTamperDetected,
        networkReadinessStatus = networkReadinessStatus,
        examSessionStarted = examSessionStarted,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = fullScreenCustomView != null,
        builtInKeyboardVisible = useBuiltInExamKeyboard && showBuiltInExamKeyboard,
        hasEditableFocus = hasEditableFocus,
        pendingSection = pendingSection,
        showForcedExitAlarm = showForcedExitAlarm,
        showOfflineWarningDialog = showOfflineWarningDialog,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        showOverlayViolationDialog = showOverlayViolationDialog,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        showClipboardViolationDialog = showClipboardViolationDialog,
        showExitExamDialog = showExitExamDialog,
        onRefreshAllSecurityChecks = ::handleRefreshAllSecurityChecks,
        onRequestSectionReport = ::handleRequestSectionReport,
        onRequestLocationPermission = ::handleRequestLocationPermission,
        onOpenInternetSettings = ::handleOpenInternetSettings
    )
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
        signatureMismatchDetected = signatureMismatchDetected,
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
        lockTaskAlreadyActive = { lockTaskBridge.active() },
        markTrustedRuntimeChromeAction = ::markTrustedRuntimeChromeAction,
        clearWebViewError = { webViewErrorMessage = null },
        reloadWebView = { webViewInstance?.reload() },
        setLoadingProgress = { loadingProgress = it },
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
    val runtimeDialogsActions = buildExamRuntimeDialogsActions(
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        overlayViolationCount = overlayViolationCount,
        overlayRiskResult = overlayRiskResult,
        lastConnectedNetworkLabel = lastConnectedNetworkLabel,
        offlineWarningDurationMs = offlineWarningDurationMs,
        currentOfflineDurationMs = currentOfflineDurationMs,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        geofenceViolationCount = geofenceViolationCount,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationViolationCount = fakeLocationViolationCount,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bluetoothViolationCount = bluetoothViolationCount,
        bluetoothEnabled = bluetoothEnabled,
        clipboardViolationCount = clipboardViolationCount,
        lastClipboardConfirmedAt = lastClipboardConfirmedAt,
        lastClipboardDecision = lastClipboardDecision,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        adminOverridesSummary = adminOverridesSummary,
        examSessionStarted = examSessionStarted,
        examGuardArmed = examGuardArmed,
        acknowledgeRuntimeAlarm = ::acknowledgeRuntimeAlarm,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        currentNetworkEventDetails = ::currentNetworkEventDetails,
        dismissForcedExitAlarm = {
            showForcedExitAlarm = false
            pendingForcedExitViolation = false
            examAlarmController.stop()
        },
        dismissKeyboardViolationDialog = {
            showKeyboardViolationDialog = false
            examAlarmController.stop()
        },
        dismissOverlayViolationDialog = {
            showOverlayViolationDialog = false
            examAlarmController.stop()
        },
        dismissOfflineWarningDialog = { showOfflineWarningDialog = false },
        openVpnSettings = ::handleOpenVpnSettings,
        refreshVpnStatus = { launchNetworkManualRefresh("vpn_runtime_dialog") },
        sendVpnReport = { handleRequestSectionReport(DiagnosticSection.Network) },
        dismissNetworkUnstableDialog = { showNetworkUnstableDialog = false },
        dismissGeofenceViolationDialog = {
            showGeofenceViolationDialog = false
            examAlarmController.stop()
        },
        dismissFakeLocationViolationDialog = {
            showFakeLocationViolationDialog = false
            examAlarmController.stop()
        },
        openBluetoothSettings = { openBluetoothSettings(context) },
        dismissBluetoothViolationDialog = {
            showBluetoothViolationDialog = false
            examAlarmController.stop()
        },
        refreshBluetoothSecurity = { refreshBluetoothSecurity(triggerViolation = false) },
        dismissClipboardViolationDialog = {
            showClipboardViolationDialog = false
            examAlarmController.stop()
        },
        dismissExitExamDialog = {
            if (!exitSessionClearInFlight) {
                showExitExamDialog = false
            }
        },
        confirmExitExam = {
            if (!exitSessionClearInFlight) {
                componentActivity.lifecycleScope.launch {
                    clearExamSessionOnExit(
                        reason = "footer_home_confirm",
                        waitForResult = true
                    )
                    writePreviousSessionBreadcrumb(
                        code = PreviousExamSessionBreadcrumbCodes.ExitCompleted,
                        details = "reason=footer_home_confirm"
                    )
                    showExitExamDialog = false
                    onExit()
                }
            }
        }
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
    val preparationState = PreparationScreenState(
        examName = payload.examName,
        keyboardPackage = currentKeyboardPackage,
        keyboardAllowed = isKeyboardAllowed,
        usingBuiltInExamKeyboard = useBuiltInExamKeyboard || !isKeyboardAllowed,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bluetoothEnabled = bluetoothEnabled,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        adbInspection = adbInspection,
        adbBypassState = adbBypassState,
        rootSecurityStatus = rootSecurityStatus,
        rootBypassState = rootBypassState,
        signatureMismatchDetected = signatureMismatchDetected,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        tamperDetected = securityTamperDetected,
        sendingSection = sendingSection,
        isStartingExam = lockTaskRequestPending || geofenceStartValidationInFlight,
        pinningActivationState = pinningActivationState,
        screenPinningMessage = screenPinningMessage,
        webViewSessionResetInFlight = webViewSessionResetInFlight,
        webViewSessionResetError = webViewSessionResetError,
        isRefreshingGeofence = geofenceManualRefreshInFlight,
        isWarmingLocation = locationWarmupInFlight,
        isRefreshingNetwork = networkManualRefreshInFlight,
        lastGeofenceRefreshAt = lastGeofenceRefreshAt,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        networkTimelinePreview = networkTimelinePreview,
        lastNetworkChangeAt = lastNetworkChangeAt,
        lastNetworkChangeSource = lastNetworkChangeSource,
        lastConnectedNetworkLabel = lastConnectedNetworkLabel,
        screenPinningAvailable = screenPinningAvailable,
        isScreenPinningActive = lockTaskBridge.active(),
        screenPinningFixNeeded = screenPinningFixNeeded,
        clipboardViolationCount = clipboardViolationCount,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        clipboardBypassState = clipboardBypassState,
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
        bypassRoot = bypassRoot,
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
        screenRecorderPackages = com.example.coblaxexamlock.runtime.detectScreenRecorderPackages(context),
        bypassScreenRecorder = adminSettings.bypassScreenRecorder,
        externalDisplayDetected = com.example.coblaxexamlock.runtime.hasExternalDisplay(context),
        bypassDisplayMirror = adminSettings.bypassDisplayMirror,
        multiWindowDetected = com.example.coblaxexamlock.runtime.isInAnySplitMode(context),
        bypassMultiWindow = adminSettings.bypassMultiWindow,
        preExamHealthCheckSnapshot = preExamHealthCheckSnapshot,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        previousExamSessionBreadcrumb = previousExamSessionBreadcrumb,
        showChecklistDetails = adminSettings.showChecklistDetails
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
        onOpenOverlaySettings = ::handleOpenOverlaySettings,
        onOpenAppSettings = ::handleOpenAppSettings,
        onOpenCastSettings = ::handleOpenCastSettings,
        onOpenWebViewProviderSettings = ::handleOpenWebViewProviderSettings,
        onReinstallOfficialApk = ::handleReinstallOfficialApk,
        onRefreshStatus = ::handleRefreshPreparationStatus,
        onRefreshAllSecurityChecks = {
            examRuntimeViewModel.dispatch(ExamRuntimeUiAction.RefreshRequested)
        },
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
        onStartExam = ::handleStartExam,
        onBackHome = onExit
    )

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
        screenPinningMessage = if (examSessionStarted) screenPinningMessage else null,
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        onDismissGeofenceMapViewer = { showGeofenceMapViewer = false },
        onRefreshGeofenceMapViewer = {
            launchLocationSecurityManualRefresh(trigger = "geofence_map_viewer_refresh")
        },
        onRefreshMapViewerActionLogged = {
            recordAction(
                code = "GEOFENCE_QUICK_FIX_REFRESH_REQUESTED",
                details = "trigger=map_viewer",
                level = DiagnosticEventLevel.INFO
            )
        },
        onOverlayObscuredTouch = { touchSignal ->
            handleExamRuntimeOverlayObscuredTouch(
                touchSignal = touchSignal,
                lockTaskRequestPending = lockTaskRequestPending,
                examSessionStarted = examSessionStarted,
                lockTaskStateLabel = lockTaskBridge.stateLabel(),
                deviceQuirkProfile = deviceQuirkProfile,
                lastTrustedRuntimeChromeActionElapsedMs = lastTrustedRuntimeChromeActionElapsedMsState.value,
                lastTrustedRuntimeChromeActionReason = lastTrustedRuntimeChromeActionReasonState.value,
                currentOverlayEventDetails = ::currentOverlayEventDetails,
                recordAction = { code, details, level -> recordAction(code, details, level) },
                recordOverlayEvent = { code, signal, level, extraContext ->
                    recordOverlayEvent(code, signal, level, extraContext)
                },
                onBlockedOverlayTouch = {
                    overlayViolationCount += 1
                    showOverlayViolationDialog = true
                    examAlarmController.start()
                }
            )
        },
        onShowBuiltInExamKeyboardChange = { showBuiltInExamKeyboard = it },
        onWebViewInstanceChange = { nextWebView ->
            val wasMissing = webViewInstance == null
            webViewInstance = nextWebView
            if (wasMissing && nextWebView != null) {
                writePreviousSessionBreadcrumb(
                    code = PreviousExamSessionBreadcrumbCodes.WebViewCreated,
                    details = "provider=${webViewCompatibilityStatus.packageName} | score=${deviceSurvivalPolicy.score.name}"
                )
            }
        },
        onHideSystemKeyboard = ::hideSystemKeyboard,
        onWebViewLoadStart = { url ->
            handleExamRuntimeWebViewLoadStart(
                url = url,
                useBuiltInExamKeyboard = useBuiltInExamKeyboard,
                recordAction = { code, details, level -> recordAction(code, details, level) },
                setHasEditableFocus = { hasEditableFocus = it },
                setWebViewErrorMessage = { webViewErrorMessage = it },
                setLoadingProgress = { loadingProgress = it },
                setExamServerStatus = { examServerStatus = it },
                setShowBuiltInExamKeyboard = { showBuiltInExamKeyboard = it }
            )
        },
        onWebViewLoadFinish = { view, url ->
            handleExamRuntimeWebViewLoadFinish(
                view = view,
                url = url,
                sideArrowControlsVisible = sideArrowControlsVisible,
                useBuiltInExamKeyboard = useBuiltInExamKeyboard,
                nativeExamFullscreenActive = nativeExamFullscreenActive,
                recordAction = { code, details, level -> recordAction(code, details, level) },
                setWebViewErrorMessage = { webViewErrorMessage = it },
                setExamServerStatus = { examServerStatus = it },
                hideSystemKeyboard = ::hideSystemKeyboard
            )
        },
        onWebViewLoadError = { description ->
            handleExamRuntimeWebViewLoadError(
                description = description,
                recordAction = { code, details, level -> recordAction(code, details, level) },
                setWebViewErrorMessage = { webViewErrorMessage = it },
                setExamServerStatus = { examServerStatus = it }
            )
        },
        onWebViewHttpError = { statusCode ->
            handleExamRuntimeWebViewHttpError(
                statusCode = statusCode,
                recordAction = { code, details, level -> recordAction(code, details, level) },
                setWebViewErrorMessage = { webViewErrorMessage = it },
                setExamServerStatus = { examServerStatus = it }
            )
        },
        onWebViewRenderProcessGone = { view, didCrash, rendererPriorityAtExit ->
            handleWebViewRendererGone(
                view = view,
                didCrash = didCrash,
                rendererPriorityAtExit = rendererPriorityAtExit
            )
        },
        onLoadingProgressChange = { loadingProgress = it },
        onWebViewErrorMessageChange = { webViewErrorMessage = it },
        onShowCustomView = { view, callback ->
            if (view != null) {
                showCustomView(view, callback)
            }
        },
        onHideCustomView = ::hideCustomView,
        onDismissPendingSection = { pendingSection = null },
        onConfirmPendingSection = { section ->
            pendingSection = null
            launchTelegramSectionReport(section)
        },
        onDismissScreenPinningMessage = { screenPinningMessage = null },
        onDismissSecurityIssueDialog = {
            val shouldExit = exitOnSecurityIssueDialogDismiss
            securityIssueDialogTitle = null
            securityIssueDialogMessage = null
            exitOnSecurityIssueDialogDismiss = false
            examAlarmController.stop()
            if (shouldExit) {
                componentActivity.lifecycleScope.launch {
                    clearExamSessionOnExit(
                        reason = "fatal_security_dialog_dismiss",
                        waitForResult = true
                    )
                    writePreviousSessionBreadcrumb(
                        code = PreviousExamSessionBreadcrumbCodes.ExitCompleted,
                        details = "reason=fatal_security_dialog_dismiss"
                    )
                    onExit()
                }
            }
        },
        onDismissBugReportFeedback = {
            bugReportFeedbackTitle = null
            bugReportFeedbackMessage = null
        },
        modifier = modifier
    )

}
