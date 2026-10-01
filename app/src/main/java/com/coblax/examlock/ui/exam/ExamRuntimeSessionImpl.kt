package com.coblax.examlock.ui.exam

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import com.coblax.examlock.ActivityLockTaskBridge
import com.coblax.examlock.AppSwitchSignal
import com.coblax.examlock.AppSwitchSuppressionReason
import com.coblax.examlock.buildAlarmSessionIdentity
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.ExamAlarmSeverity
import com.coblax.examlock.ExamDeviceOwnerController
import com.coblax.examlock.ExamParticipantCaptureBridge
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.inspectDeviceTimeSecurity
import com.coblax.examlock.isExamGuardAccessibilityEnabled
import com.coblax.examlock.LocalDeviceCompatibilityProfile
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.MainActivity
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.NetworkTimelineEntry
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.OverlayBypassState
import com.coblax.examlock.OverlayRiskAnalyzer
import com.coblax.examlock.OverlayShieldStatus
import com.coblax.examlock.PinningActivationPurpose
import com.coblax.examlock.PinningActivationState
import com.coblax.examlock.runtime.isAllowedExamKeyboard
import com.coblax.examlock.runtime.readExamBatteryStatus
import com.coblax.examlock.runtime.readNetworkReadinessStatus
import com.coblax.examlock.runtime.SecurityDetectorCache
import com.coblax.examlock.ScreenPinningEnforcer
import com.coblax.examlock.shouldSuppressPinningTransitionViolation
import com.coblax.examlock.ui.preparation.PreparationScreenActions
import com.coblax.examlock.ui.preparation.PreparationScreenState
import com.coblax.examlock.ui.theme.AppColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
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
    val webViewCompatibilityRefreshKeyState = rememberSaveable { mutableIntStateOf(0) }
    var webViewCompatibilityRefreshKey by webViewCompatibilityRefreshKeyState
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
    val deviceTimeBaseline = remember(
        deviceTimeBaselineWallClockMillis,
        deviceTimeBaselineElapsedRealtimeMillis
    ) {
        DeviceTimeBaseline(
            wallClockMillis = deviceTimeBaselineWallClockMillis,
            elapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        )
    }
    val bypassContext = rememberExamRuntimeBypassContext(adminSettings, payload)
    val screenPinningMode = bypassContext.screenPinningMode
    val overlayBypassState = bypassContext.overlayBypassState
    val appSwitchBypassState = bypassContext.appSwitchBypassState
    val accessibilityBypassState = bypassContext.accessibilityBypassState
    val clipboardBypassState = bypassContext.clipboardBypassState
    val adbBypassState = bypassContext.adbBypassState
    val rootBypassState = bypassContext.rootBypassState
    val geofenceBypassState = bypassContext.geofenceBypassState
    val fakeLocationBypassState = bypassContext.fakeLocationBypassState
    val deviceTimeBypassState = bypassContext.deviceTimeBypassState
    val vpnBypassState = bypassContext.vpnBypassState
    val bypassReverseEngineering = bypassContext.bypassReverseEngineering
    val bypassApkIntegrity = bypassContext.bypassApkIntegrity
    val bypassKeyboardPolicy = bypassContext.bypassKeyboardPolicy
    val bypassClipboard = bypassContext.bypassClipboard
    val bypassGeofence = bypassContext.bypassGeofence
    val bypassFakeLocation = bypassContext.bypassFakeLocation
    val bypassVpn = bypassContext.bypassVpn
    val effectiveLocationPolicySource = bypassContext.effectiveLocationPolicySource
    val geofenceConfigParseResult = bypassContext.geofenceConfigParseResult
    val warmLocationPolicySignature = bypassContext.warmLocationPolicySignature
    val webViewUiState = rememberExamRuntimeWebViewUiState(context)
    val flowUiState = rememberExamRuntimeFlowUiState(
        context = context,
        bypassKeyboardPolicy = bypassKeyboardPolicy
    )
    val locationWarmupUiState = rememberExamRuntimeLocationWarmupUiState()
    var examSessionStarted by flowUiState.examSessionStarted
    var lockTaskRequestPending by flowUiState.lockTaskRequestPending
    val examServerStatusState = rememberSaveable(payload.examUrl) {
        mutableStateOf(ExamServerFooterStatus.Checking)
    }
    LaunchedEffect(examSessionRecoveryNonce, examSessionStarted) {
        onExamSessionStartedStateChange(examSessionStarted)
    }
    val baseNetworkReadinessState = remember { mutableStateOf(readNetworkReadinessStatus(context)) }
    var baseNetworkReadiness by baseNetworkReadinessState
    val networkUiState = rememberExamRuntimeNetworkUiState(baseNetworkReadiness)
    val networkTimeline = remember { mutableStateListOf<NetworkTimelineEntry>() }
    val networkFlapElapsedMs = remember { mutableStateListOf<Long>() }
    var networkUnstableEpisodeStartedElapsedMs by networkUiState.networkUnstableEpisodeStartedElapsedMs
    val batteryStatusState = remember { mutableStateOf(readExamBatteryStatus(context)) }
    val dpcRuntimeStatusState = remember {
        mutableStateOf(ExamDeviceOwnerController.readStatus(context))
    }
    var dpcRuntimeStatus by dpcRuntimeStatusState
    val dpcCreateWindowsRestrictionAppliedBySessionState = rememberSaveable { mutableStateOf(false) }
    val dpcExamPolicyAppliedForSessionState = rememberSaveable { mutableStateOf(false) }
    val networkMainHandler = remember { Handler(Looper.getMainLooper()) }
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }
    var fullScreenCustomView by webViewUiState.fullScreenCustomView
    val fullScreenContainer = webViewUiState.fullScreenContainer
    fullScreenContainer.setBackgroundColor(AppColors.current.background.toArgb())
    val exitSessionClearRequestedState = rememberSaveable { mutableStateOf(false) }
    val exitSessionClearDeferredState = remember {
        mutableStateOf<CompletableDeferred<Result<Unit>>?>(null)
    }
    val lastTrustedRuntimeChromeActionElapsedMsState = rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    val lastTrustedRuntimeChromeActionReasonState = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val runtimeCacheState = rememberExamRuntimeRuntimeCacheState()
    var currentKeyboardPackage by flowUiState.currentKeyboardPackage
    val securityUiState = rememberExamRuntimeSecurityUiState(
        context = context,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState
    )
    var overlayViolationCount by securityUiState.overlayViolationCount
    var overlayShieldRequested by securityUiState.overlayShieldRequested
    var overlayShieldLastApplySucceeded by securityUiState.overlayShieldLastApplySucceeded
    var overlayShieldLastAppliedAt by securityUiState.overlayShieldLastAppliedAt
    var lastOverlayTrigger by securityUiState.lastOverlayTrigger
    var lastOverlayAt by securityUiState.lastOverlayAt
    var lastOverlayContext by securityUiState.lastOverlayContext
    var accessibilityInspection by securityUiState.accessibilityInspection
    var signatureMismatchDetected by securityUiState.signatureMismatchDetected
    val packageInventoryChangeNonceState = rememberSaveable { mutableIntStateOf(0) }
    var tamperDetected by securityUiState.tamperDetected
    var integrityTamperDetected by securityUiState.integrityTamperDetected
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
    val lastDeviceTimeDiagnosticKeyState = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardUiState = rememberExamRuntimeClipboardUiState(context)
    val adminUiState = rememberExamRuntimeAdminUiState(
        context = context,
        payload = payload
    )
    val accessibilityGuardEnabledState = remember {
        mutableStateOf(isExamGuardAccessibilityEnabled(context))
    }
    val accessibilityGuardFallbackActiveState = remember { mutableStateOf(false) }
    val accessibilityGuardLastReasonState = remember { mutableStateOf<String?>(null) }
    val accessibilityGuardLastForeignPackageState = remember { mutableStateOf<String?>(null) }
    val accessibilityGuardLastEventTypeState = remember { mutableStateOf<String?>(null) }
    val accessibilityGuardLastDetectedAtState = remember { mutableStateOf<String?>(null) }
    val accessibilityGuardAlarmSeverityState = remember {
        mutableStateOf(ExamAlarmSeverity.Warning.name)
    }
    var examRuntimeMonitoringArmed by adminUiState.examRuntimeMonitoringArmed
    var participantContext by adminUiState.participantContext
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
        // Only violations newer than the ones handled in preparation count as confirmed:
        // an old touch (or a failed shield on a start that never began) no longer blocks
        // every later attempt. The full count stays in reports and the footer.
        violationCount = (overlayViolationCount - securityUiState.overlayViolationAcknowledgedCount.intValue)
            .coerceAtLeast(0),
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
    val examAlarmController = remember(context) {
        ExamAlarmController(context.applicationContext)
    }
    // Ensure alarm stops and volume is restored when the composable exits.
    // Without this, a playing alarm ringtone could leak into the background
    // with boosted volume if the user navigates away from the exam screen.
    DisposableEffect(examAlarmController) {
        onDispose { examAlarmController.stop() }
    }
    val coroutineScope = rememberCoroutineScope()
    // Global exception handler for all fire-and-forget coroutineScope.launch calls.
    // This prevents silent crashes where a background coroutine fails without anyone
    // noticing, leaving the exam session in a stuck/broken state.
    val examExceptionHandler = remember {
        CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e(
                ExamRuntimeHardeningLogTag,
                "Uncaught coroutine exception: ${throwable.javaClass.simpleName}",
                throwable
            )
        }
    }
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
        examAlarmController = examAlarmController,
        batteryStatusState = batteryStatusState
    )
    fun clearAppSwitchSuppression() = runtimeDiagnosticsOps.clearAppSwitchSuppression()
    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String = runtimeDiagnosticsOps.currentAppSwitchEventDetails(signal, suppressionReason)
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = runtimeDiagnosticsOps.recordAction(code, details, level)
    val latestServerProbeIdState = remember { mutableStateOf(0L) }
    val sessionCore = ExamRuntimeSessionCore(
        context = context,
        activity = activity,
        componentActivity = componentActivity,
        mainActivity = mainActivity,
        lockTaskBridge = lockTaskBridge,
        coroutineScope = coroutineScope,
        examExceptionHandler = examExceptionHandler,
        examAlarmController = examAlarmController,
        payload = payload,
        adminSettings = adminSettings,
        bypass = bypassContext,
        uiLanguage = uiLanguage,
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        deviceQuirkProfile = deviceQuirkProfile,
        deviceTimeBaseline = deviceTimeBaseline,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        webViewCompatibilityRefreshKeyState = webViewCompatibilityRefreshKeyState,
        webViewUiState = webViewUiState,
        flowUiState = flowUiState,
        locationWarmupUiState = locationWarmupUiState,
        networkUiState = networkUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        runtimeCacheState = runtimeCacheState,
        examServerStatusState = examServerStatusState,
        latestServerProbeIdState = latestServerProbeIdState,
        baseNetworkReadinessState = baseNetworkReadinessState,
        networkTimeline = networkTimeline,
        networkFlapElapsedMs = networkFlapElapsedMs,
        batteryStatusState = batteryStatusState,
        dpcRuntimeStatusState = dpcRuntimeStatusState,
        dpcCreateWindowsRestrictionAppliedBySessionState = dpcCreateWindowsRestrictionAppliedBySessionState,
        dpcExamPolicyAppliedForSessionState = dpcExamPolicyAppliedForSessionState,
        networkMainHandler = networkMainHandler,
        clipboardMainHandler = clipboardMainHandler,
        overlayMainHandler = overlayMainHandler,
        exitSessionClearRequestedState = exitSessionClearRequestedState,
        exitSessionClearDeferredState = exitSessionClearDeferredState,
        lastTrustedRuntimeChromeActionElapsedMsState = lastTrustedRuntimeChromeActionElapsedMsState,
        lastTrustedRuntimeChromeActionReasonState = lastTrustedRuntimeChromeActionReasonState,
        deviceTimeSecurityStatusState = deviceTimeSecurityStatusState,
        lastDeviceTimeDiagnosticKeyState = lastDeviceTimeDiagnosticKeyState,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        accessibilityGuardLastReasonState = accessibilityGuardLastReasonState,
        accessibilityGuardLastForeignPackageState = accessibilityGuardLastForeignPackageState,
        accessibilityGuardLastEventTypeState = accessibilityGuardLastEventTypeState,
        accessibilityGuardLastDetectedAtState = accessibilityGuardLastDetectedAtState,
        accessibilityGuardAlarmSeverityState = accessibilityGuardAlarmSeverityState,
        packageInventoryChangeNonceState = packageInventoryChangeNonceState,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        networkReadinessStatus = networkReadinessStatus,
        overlayRiskResult = overlayRiskResult,
        examGuardArmed = examGuardArmed,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        isKeyboardAllowed = isKeyboardAllowed,
        securityTamperDetected = securityTamperDetected,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        onExit = onExit
    )
    fun clearDpcExamPoliciesForSession(reason: String) = sessionCore.clearDpcExamPoliciesForSession(reason)

    val permissionLaunchers = rememberExamRuntimePermissionLaunchers(sessionCore)

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
            clearAppSwitchSuppression = { clearAppSwitchSuppression() },
            clearDpcExamPoliciesForSession = { reason -> clearDpcExamPoliciesForSession(reason) },
            recordAction = { code, details, level ->
                recordAction(code = code, details = details, level = level)
            }
        )
    )
    fun refreshIntegrityGuard() = runtimeMonitoringOps.refreshIntegrityGuard()
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
        refreshIntegrityGuard = { refreshIntegrityGuard() }
    )
    val startExamController = ExamRuntimeStartExamController(
        core = sessionCore,
        runtimeMonitoringOps = runtimeMonitoringOps,
        runtimeSecurityOps = runtimeSecurityOps,
        permissionLaunchers = permissionLaunchers
    )

    ExamRuntimeSessionEffects(
        core = sessionCore,
        runtimeMonitoringOps = runtimeMonitoringOps,
        runtimeSecurityOps = runtimeSecurityOps,
        startExamController = startExamController,
        nativeFullscreenBridge = nativeFullscreenBridge,
        pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
        pendingRecoveryEventDetails = pendingRecoveryEventDetails,
        examSessionRecoveryNonce = examSessionRecoveryNonce,
        onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
        onRecoveryEventConsumed = onRecoveryEventConsumed
    )

    ExamRuntimeSessionContent(
        core = sessionCore,
        runtimeMonitoringOps = runtimeMonitoringOps,
        runtimeSecurityOps = runtimeSecurityOps,
        startExamController = startExamController,
        permissionLaunchers = permissionLaunchers,
        nativeFullscreenBridge = nativeFullscreenBridge,
        modifier = modifier
    )
}

@Composable
internal fun ExamRuntimeSessionRenderedUiSection(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: com.coblax.examlock.ExamQrPayload,
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
    runtimeDialogsState: com.coblax.examlock.ui.dialog.ExamRuntimeDialogsState,
    runtimeDialogsActions: com.coblax.examlock.ui.dialog.ExamRuntimeDialogsActions,
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    securityIssueDialogCode: String?,
    startExamPreflightState: StartExamPreflightUiState,
    lockTaskRequestPending: Boolean,
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
    onRefreshNetworkStatus: () -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
    onCancelPinningActivation: () -> Unit,
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
        securityIssueDialogCode = securityIssueDialogCode,
        startExamPreflightState = startExamPreflightState,
        lockTaskRequestPending = lockTaskRequestPending,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        securityUiState = securityUiState,
        onDismissGeofenceMapViewer = { renderedUiCallbacks.onDismissGeofenceMapViewer() },
        onRefreshGeofenceMapViewer = { renderedUiCallbacks.onRefreshGeofenceMapViewer() },
        onRefreshMapViewerActionLogged = { renderedUiCallbacks.onRefreshMapViewerActionLogged() },
        onOverlayObscuredTouch = { touchSignal -> renderedUiCallbacks.onOverlayObscuredTouch(touchSignal) },
        onShowBuiltInExamKeyboardChange = { show -> renderedUiCallbacks.onShowBuiltInExamKeyboardChange(show) },
        onWebViewInstanceChange = { nextWebView -> renderedUiCallbacks.onWebViewInstanceChange(nextWebView) },
        onHideSystemKeyboard = onHideSystemKeyboard,
        onWebViewLoadStart = { view, url -> renderedUiCallbacks.onWebViewLoadStart(view, url) },
        onWebViewLoadFinish = { view, url -> renderedUiCallbacks.onWebViewLoadFinish(view, url) },
        onWebViewLoadError = { view, description -> renderedUiCallbacks.onWebViewLoadError(view, description) },
        onWebViewHttpError = { view, statusCode -> renderedUiCallbacks.onWebViewHttpError(view, statusCode) },
        onWebViewRenderProcessGone = { view, didCrash, rendererPriorityAtExit -> renderedUiCallbacks.onWebViewRenderProcessGone(view, didCrash, rendererPriorityAtExit) },
        onLoadingProgressChange = { view, progress -> renderedUiCallbacks.onLoadingProgressChange(view, progress) },
        onWebViewErrorMessageChange = { message -> renderedUiCallbacks.onWebViewErrorMessageChange(message) },
        onShowCustomView = { view, callback -> renderedUiCallbacks.onShowCustomView(view, callback) },
        onHideCustomView = onHideCustomView,
        onDismissPendingSection = { renderedUiCallbacks.onDismissPendingSection() },
        onConfirmPendingSection = { section -> renderedUiCallbacks.onConfirmPendingSection(section) },
        onOpenStaticSecurityAppSettings = onOpenStaticSecurityAppSettings,
        onOpenStaticSecurityCastSettings = onOpenStaticSecurityCastSettings,
        onRefreshStaticSecurityStatus = onRefreshStaticSecurityStatus,
        onSendStaticSecurityReport = onSendStaticSecurityReport,
        onDismissScreenPinningMessage = { renderedUiCallbacks.onDismissScreenPinningMessage() },
        onDismissSecurityIssueDialog = { renderedUiCallbacks.onDismissSecurityIssueDialog() },
        onRefreshNetworkStatus = onRefreshNetworkStatus,
        onOpenAppPermissionSettings = onOpenAppPermissionSettings,
        onDismissBugReportFeedback = { renderedUiCallbacks.onDismissBugReportFeedback() },
        onCancelPinningActivation = onCancelPinningActivation,
        modifier = modifier
    )
}

internal fun handleExamRuntimeScreenPinningTransitionInterrupted(
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
