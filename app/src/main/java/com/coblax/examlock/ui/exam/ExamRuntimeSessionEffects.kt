package com.coblax.examlock.ui.exam

import android.os.SystemClock
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coblax.examlock.AccessibilityExamGuardStore
import com.coblax.examlock.AppSwitchSignal
import com.coblax.examlock.AppSwitchSuppressionReason
import com.coblax.examlock.ClipboardChangeDecision
import com.coblax.examlock.ClipboardSnapshot
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.FatalSecuritySignal
import com.coblax.examlock.MemoryPressureCoordinator
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.isGeofenceEnforced
import com.coblax.examlock.OverlaySignal
import com.coblax.examlock.runtime.getVirtualEnvironmentDiagnosticsOnIo
import com.coblax.examlock.runtime.hasLocationPermissionForWifi
import com.coblax.examlock.runtime.isLocationServicesEnabled
import com.coblax.examlock.runtime.registerPackageInventoryInvalidationReceiver
import com.coblax.examlock.runtime.SecurityDetectorCache
import com.coblax.examlock.resolveLockTaskSecurityRequirement
import com.coblax.examlock.SplitLocationSecurityStatus
import com.coblax.examlock.updateCacheModeForNetworkStability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf

/**
 * Every effect of the exam session: startup scans, network and server checks, the
 * guards that watch pinning, overlays, app switches and the clipboard, and cleanup.
 * Split out of the session composable so no single method is too large for the
 * Android runtime to verify and compile cheaply on low-end phones.
 */
@Composable
internal fun ExamRuntimeSessionEffects(
    core: ExamRuntimeSessionCore,
    runtimeMonitoringOps: ExamRuntimeMonitoringOps,
    runtimeSecurityOps: ExamRuntimeSecurityOps,
    startExamController: ExamRuntimeStartExamController,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    examSessionRecoveryNonce: Long,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit
) {
    val screenPinningMode = core.bypass.screenPinningMode
    val overlayBypassState = core.bypass.overlayBypassState
    val clipboardBypassState = core.bypass.clipboardBypassState
    val geofenceBypassState = core.bypass.geofenceBypassState
    val fakeLocationBypassState = core.bypass.fakeLocationBypassState
    val deviceTimeBypassState = core.bypass.deviceTimeBypassState
    val bypassBluetooth = core.bypass.bypassBluetooth
    val bypassKeyboardPolicy = core.bypass.bypassKeyboardPolicy
    val bypassClipboard = core.bypass.bypassClipboard
    val bypassOverlay = core.bypass.bypassOverlay
    val bypassGeofence = core.bypass.bypassGeofence
    val bypassFakeLocation = core.bypass.bypassFakeLocation
    val bypassScreenRecorder = core.bypass.bypassScreenRecorder
    val bypassDisplayMirror = core.bypass.bypassDisplayMirror
    val bypassMultiWindow = core.bypass.bypassMultiWindow
    val geofenceConfigParseResult = core.bypass.geofenceConfigParseResult
    val warmLocationPolicySignature = core.bypass.warmLocationPolicySignature
    val geofenceEnabled = core.bypass.geofenceEnabled
    var webViewInstance by core.webViewUiState.instance
    var examSessionStarted by core.flowUiState.examSessionStarted
    var lockTaskRequestPending by core.flowUiState.lockTaskRequestPending
    var showExitExamDialog by core.flowUiState.showExitExamDialog
    var examServerStatus by core.examServerStatusState
    var baseNetworkReadiness by core.baseNetworkReadinessState
    var networkUnstableEpisodeStartedElapsedMs by core.networkUiState.networkUnstableEpisodeStartedElapsedMs
    var showOfflineWarningDialog by core.networkUiState.showOfflineWarningDialog
    var dpcRuntimeStatus by core.dpcRuntimeStatusState
    var dpcExamPolicyAppliedForSession by core.dpcExamPolicyAppliedForSessionState
    var fullScreenCustomView by core.webViewUiState.fullScreenCustomView
    var useBuiltInExamKeyboard by core.flowUiState.useBuiltInExamKeyboard
    var showBuiltInExamKeyboard by core.flowUiState.showBuiltInExamKeyboard
    var geofencePermissionRequestInFlight by core.flowUiState.geofencePermissionRequestInFlight
    var geofenceStartValidationInFlight by core.flowUiState.geofenceStartValidationInFlight
    var webViewSessionResetInFlight by core.flowUiState.webViewSessionResetInFlight
    var geofenceManualRefreshInFlight by core.flowUiState.geofenceManualRefreshInFlight
    var retryStartExamAfterLocationPermissionGrant by core.flowUiState.retryStartExamAfterLocationPermissionGrant
    var lastGeofenceRefreshAt by core.flowUiState.lastGeofenceRefreshAt
    var locationWarmupInFlight by core.locationWarmupUiState.locationWarmupInFlight
    var reusableWarmLocationValidation by core.locationWarmupUiState.reusableWarmLocationValidation
    var bluetoothPermissionGranted by core.securityUiState.bluetoothPermissionGranted
    var developerOptionsEnabled by core.securityUiState.developerOptionsEnabled
    var virtualEnvironmentDetected by core.securityUiState.virtualEnvironmentDetected
    var packageInventoryChangeNonce by core.packageInventoryChangeNonceState
    var geofenceSecurityStatus by core.securityUiState.geofenceSecurityStatus
    var securityIssueDialogMessage by core.adminUiState.securityIssueDialogMessage
    var exitOnSecurityIssueDialogDismiss by core.adminUiState.exitOnSecurityIssueDialogDismiss
    var screenPinningBypassTamperLogged by core.adminUiState.screenPinningBypassTamperLogged
    var accessibilityBypassTamperLogged by core.adminUiState.accessibilityBypassTamperLogged
    var adbBypassTamperLogged by core.adminUiState.adbBypassTamperLogged
    var clipboardBypassTamperLogged by core.adminUiState.clipboardBypassTamperLogged
    var overlayBypassTamperLogged by core.adminUiState.overlayBypassTamperLogged
    var geofenceBypassTamperLogged by core.adminUiState.geofenceBypassTamperLogged
    var fakeLocationBypassTamperLogged by core.adminUiState.fakeLocationBypassTamperLogged
    var deviceTimeBypassTamperLogged by core.adminUiState.deviceTimeBypassTamperLogged
    var vpnBypassTamperLogged by core.adminUiState.vpnBypassTamperLogged
    var appSwitchBypassTamperLogged by core.adminUiState.appSwitchBypassTamperLogged
    var rootBypassTamperLogged by core.adminUiState.rootBypassTamperLogged
    var appSwitchSuppressionReason by core.adminUiState.appSwitchSuppressionReason
    var appSwitchSuppressedUntilElapsedMs by core.adminUiState.appSwitchSuppressedUntilElapsedMs
    var appSwitchFallbackArmedLogged by core.adminUiState.appSwitchFallbackArmedLogged
    var accessibilityGuardEnabled by core.accessibilityGuardEnabledState
    var accessibilityGuardFallbackActive by core.accessibilityGuardFallbackActiveState
    var pendingSection by core.adminUiState.pendingSection
    var bugReportFeedbackMessage by core.adminUiState.bugReportFeedbackMessage
    var examSessionStartedAtElapsedMs by core.adminUiState.examSessionStartedAtElapsedMs
    val appSwitchLockTaskActive = core.runtimeDiagnosticsOps.appSwitchLockTaskActive
    val appSwitchProtectionMode = core.runtimeDiagnosticsOps.appSwitchProtectionMode
    val appSwitchStatus = core.runtimeDiagnosticsOps.appSwitchStatus
    fun currentNetworkPollingIntervalMillis(): Long = core.runtimeDiagnosticsOps.currentNetworkPollingIntervalMillis()
    fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long = core.runtimeDiagnosticsOps.currentScreenPinningMonitorIntervalMillis(nowElapsedMs)
    fun clearAppSwitchSuppression() = core.runtimeDiagnosticsOps.clearAppSwitchSuppression()
    fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? = core.runtimeDiagnosticsOps.currentAppSwitchSuppressionReason()
    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String = core.runtimeDiagnosticsOps.currentAppSwitchEventDetails(signal, suppressionReason)
    fun currentOverlayEventDetails(
        signal: OverlaySignal,
        extraContext: String? = null
    ): String = core.runtimeDiagnosticsOps.currentOverlayEventDetails(signal, extraContext)
    fun currentInternalDialogReason(): String? = core.runtimeDiagnosticsOps.currentInternalDialogReason()
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = core.runtimeDiagnosticsOps.recordAction(code, details, level)
    fun recordDpcRuntimeStatus() = core.recordDpcRuntimeStatus()
    fun clearDpcExamPoliciesForSession(reason: String) = core.clearDpcExamPoliciesForSession(reason)

    suspend fun runExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) = core.runExamServerProbe(trigger, markChecking)
    fun launchExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) = core.launchExamServerProbe(trigger, markChecking)
    fun handleAccessibilityGuardViolation(violation: AccessibilityGuardRuntimeViolation) =
        core.handleAccessibilityGuardViolation(violation)
    fun currentNetworkEventDetails(
        trigger: String,
        status: NetworkReadinessStatus,
        extraContext: String? = null
    ): String = core.runtimeDiagnosticsOps.currentNetworkEventDetails(trigger, status, extraContext)
    fun refreshDeviceTimeSecurity(
        trigger: String,
        emitDiagnosticEvent: Boolean = true
    ): DeviceTimeSecurityStatus = core.runtimeDiagnosticsOps.refreshDeviceTimeSecurity(trigger, emitDiagnosticEvent)
    fun updateNetworkReadiness(source: String) = core.runtimeDiagnosticsOps.updateNetworkReadiness(source)
    suspend fun refreshGeofenceStatus(
        preferFresh: Boolean,
        trigger: String,
        allowRuntimeViolation: Boolean
    ): SplitLocationSecurityStatus = core.runtimeDiagnosticsOps.refreshGeofenceStatus(preferFresh, trigger, allowRuntimeViolation)
    fun invalidateWarmLocationValidationCache() = core.runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
    fun disarmExamRuntimeMonitoring() = runtimeMonitoringOps.disarmExamRuntimeMonitoring()
    fun recordAppSwitchEvent(
        code: String,
        signal: AppSwitchSignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        updateLastDetectedAt: Boolean = true
    ) = runtimeMonitoringOps.recordAppSwitchEvent(code, signal, level, updateLastDetectedAt)
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
    fun hideCustomView() = runtimeMonitoringOps.hideCustomView()
    fun cleanupActiveExamWebViewInstance() = runtimeMonitoringOps.cleanupActiveExamWebViewInstance()
    fun launchExitSessionClearBestEffort(reason: String) =
        runtimeMonitoringOps.launchExitSessionClearBestEffort(reason)
    fun handleWebViewRendererGone(
        view: SecureExamWebView?,
        didCrash: Boolean,
        rendererPriorityAtExit: Int?
    ): Boolean = runtimeMonitoringOps.handleWebViewRendererGone(view, didCrash, rendererPriorityAtExit)
    fun handleRuntimeTrimMemory(level: Int) = runtimeMonitoringOps.handleRuntimeTrimMemory(level)
    fun refreshKeyboardSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshKeyboardSecurity(triggerViolation)
    fun refreshBluetoothSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshBluetoothSecurity(triggerViolation)
    fun refreshScreenPinningDiagnostics() = runtimeSecurityOps.refreshScreenPinningDiagnostics()
    fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshDeviceIntegritySecurity(triggerViolation)
    val accessibilityGuardLastForeignPackageState = core.accessibilityGuardLastForeignPackageState
    val clipboardUiState = core.clipboardUiState
    val examAlarmController = core.examAlarmController
    val flowUiState = core.flowUiState
    val accessibilityGuardLastDetectedAtState = core.accessibilityGuardLastDetectedAtState
    val examGuardArmed = core.examGuardArmed
    val context = core.context
    val accessibilityGuardFallbackActiveState = core.accessibilityGuardFallbackActiveState
    val mainActivity = core.mainActivity
    val lastTrustedRuntimeChromeActionReasonState = core.lastTrustedRuntimeChromeActionReasonState
    val accessibilityGuardEnabledState = core.accessibilityGuardEnabledState
    val lockTaskBridge = core.lockTaskBridge
    val networkMainHandler = core.networkMainHandler
    val coroutineScope = core.coroutineScope
    val deviceCompatibilityProfile = core.deviceCompatibilityProfile
    val isIndonesian = core.isIndonesian
    val componentActivity = core.componentActivity
    val deviceTimeBaseline = core.deviceTimeBaseline
    val lastTrustedRuntimeChromeActionElapsedMsState = core.lastTrustedRuntimeChromeActionElapsedMsState
    val nativeExamFullscreenActive = core.nativeExamFullscreenActive
    val networkReadinessStatus = core.networkReadinessStatus
    val lowRamProfile = core.lowRamProfile
    val payload = core.payload
    val adminUiState = core.adminUiState
    val accessibilityGuardLastReasonState = core.accessibilityGuardLastReasonState
    val clipboardMainHandler = core.clipboardMainHandler
    val batteryStatusState = core.batteryStatusState
    val networkUiState = core.networkUiState
    val deviceQuirkProfile = core.deviceQuirkProfile
    val accessibilityGuardAlarmSeverityState = core.accessibilityGuardAlarmSeverityState
    val securityUiState = core.securityUiState
    val adminSettings = core.adminSettings
    val networkFlapElapsedMs = core.networkFlapElapsedMs
    val accessibilityGuardLastEventTypeState = core.accessibilityGuardLastEventTypeState
    val networkStatus = core.networkReadinessStatus.examStatus
    fun handleScreenPinningTransitionInterrupted() = core.handleScreenPinningTransitionInterrupted()

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
    LaunchedEffect(context, fakeLocationBypassState) {
        // A throwing detector used to leave the scan "pending" for good: preparation
        // showed "Checking…" and Start Exam stayed locked. Retry, then let preparation
        // continue; Start Exam re-reads every one of these detectors with forceRefresh,
        // so an unfinished first pass cannot let a device through.
        repeat(InitialStaticSecurityScanAttempts) { attempt ->
            val snapshot = try {
                readInitialStaticSecuritySnapshotOnIo(
                    context = context,
                    forceRefresh = attempt > 0
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                android.util.Log.w(
                    ExamRuntimeHardeningLogTag,
                    "STATIC_SECURITY_INITIAL_SCAN_FAILED attempt=${attempt + 1} error=${throwable.javaClass.simpleName}",
                    throwable
                )
                null
            }
            if (snapshot != null) {
                applyInitialStaticSecuritySnapshot(
                    snapshot = snapshot,
                    securityUiState = securityUiState,
                    permissionGranted = hasLocationPermissionForWifi(context),
                    locationServicesEnabled = isLocationServicesEnabled(context),
                    fixQualityStatus = geofenceSecurityStatus.fixQualityStatus,
                    developerOptionsEnabled = developerOptionsEnabled,
                    fakeLocationBypassState = fakeLocationBypassState,
                    fakeLocationMonitoringEnabled = isGeofenceEnforced(geofenceConfigParseResult, geofenceBypassState)
                )
                return@LaunchedEffect
            }
            delay(InitialStaticSecurityScanRetryDelayMillis * (attempt + 1))
        }
        securityUiState.staticSecurityInitialScanComplete.value = true
    }
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

    LaunchedEffect(networkUnstableEpisodeStartedElapsedMs) {
        if (networkUnstableEpisodeStartedElapsedMs != null) {
            examServerStatus = ExamServerFooterStatus.Unstable
        } else if (examServerStatus == ExamServerFooterStatus.Unstable) {
            launchExamServerProbe("network_stabilized", markChecking = true)
        }
    }
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
            if (MemoryPressureCoordinator.sideChecksPaused()) {
                recordSideCheckMemoryPause("exam_server_probe") { code, details, level ->
                    recordAction(code, details, level)
                }
            } else {
                runExamServerProbe(
                    trigger = if (firstProbe) "exam_start" else "periodic",
                    markChecking = examServerStatus == ExamServerFooterStatus.Checking
                )
                firstProbe = false
            }
            delay(examServerProbeIntervalMillis(lowRamProfile))
        }
    }

    // Recover failed GET navigations only after a real offline -> online transition.
    // HTTP/SSL failures and form submissions require an explicit user action.
    var previousNetworkConnected by remember { mutableStateOf(networkStatus.isConnected) }
    LaunchedEffect(examSessionStarted, networkStatus.isConnected) {
        val reconnected = !previousNetworkConnected && networkStatus.isConnected
        previousNetworkConnected = networkStatus.isConnected
        if (!examSessionStarted || !reconnected) return@LaunchedEffect
        val webView = webViewInstance ?: return@LaunchedEffect
        val retryUrl = webView.navigationState.failedUrl ?: return@LaunchedEffect
        if (!webView.navigationState.canRecoverOnConnection) return@LaunchedEffect
        delay(2_000L)
        if (webView !== webViewInstance || !webView.navigationState.canRecoverOnConnection ||
            webView.navigationState.failedUrl != retryUrl
        ) return@LaunchedEffect
        recordAction(
            "WEBVIEW_AUTO_RELOAD_ON_RECOVERY",
            "host=${safeExamServerHost(retryUrl)} | transport=${networkReadinessStatus.transportLabel}",
            DiagnosticEventLevel.INFO
        )
        webView.cancelPendingConnectionRetries()
        webView.loadExamUrlSafely(retryUrl)
        launchExamServerProbe("network_recovery", true)
    }

    // Proactive memory monitoring: periodically check JVM heap usage and
    // preemptively clear non-critical WebView cache when memory is high.
    // This helps prevent the WebView renderer from being killed by Android
    // on low-RAM devices during long exam sessions.
    LaunchedEffect(examSessionStarted) {
        if (!examSessionStarted) return@LaunchedEffect
        while (true) {
            delay(60_000L) // Check every 1 minute
            val runtime = Runtime.getRuntime()
            val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
            val memoryUsagePercent = if (maxMemoryMb > 0) (usedMemoryMb * 100) / maxMemoryMb else 0
            if (memoryUsagePercent > 85) {
                recordAction(
                    "MEMORY_PRESSURE_HIGH",
                    "used=${usedMemoryMb}MB | max=${maxMemoryMb}MB | pct=$memoryUsagePercent%",
                    DiagnosticEventLevel.WARNING
                )
                // Preemptive: clear in-memory cache to reduce pressure.
                // Wrapped in runCatching because the WebView may have been destroyed
                // (renderer gone) but the reference not yet nulled — clearCache() would
                // throw IllegalStateException and kill this monitoring loop.
                runCatching { webViewInstance?.clearCache(false) }
            }
        }
    }

    // Dynamic cache mode switching: adapt WebView caching strategy to real-time
    // network conditions. Stable network → LOAD_DEFAULT (fresh content from server),
    // unstable/offline → LOAD_CACHE_ELSE_NETWORK (serve from cache, fall back to net).
    LaunchedEffect(
        examSessionStarted,
        networkStatus.isConnected,
        networkUnstableEpisodeStartedElapsedMs
    ) {
        if (!examSessionStarted) return@LaunchedEffect
        val networkStable = networkStatus.isConnected &&
            networkUnstableEpisodeStartedElapsedMs == null
        webViewInstance?.let { webView ->
            val changed = webView.updateCacheModeForNetworkStability(networkStable)
            if (changed) {
                recordAction(
                    "WEBVIEW_CACHE_MODE_SWITCHED",
                    "stable=$networkStable | mode=${if (networkStable) "LOAD_DEFAULT" else "LOAD_CACHE_ELSE_NETWORK"}",
                    DiagnosticEventLevel.INFO
                )
            }
        }
    }

    RuntimeRecoveryAndMemoryEffects(
        pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
        pendingRecoveryEventDetails = pendingRecoveryEventDetails,
        examSessionRecoveryNonce = examSessionRecoveryNonce,
        recordInfoAction = { code, details -> recordAction(code = code, details = details) },
        onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
        onRecoveryEventConsumed = onRecoveryEventConsumed,
        refreshReverseEngineeringStatus = { refreshReverseEngineeringStatus() },
        refreshIntegrityGuard = { refreshIntegrityGuard() },
        onSimulateRendererGone = {
            handleWebViewRendererGone(
                view = webViewInstance,
                didCrash = false,
                rendererPriorityAtExit = null
            )
        },
        onTrimMemory = { level -> handleRuntimeTrimMemory(level) }
    )

    LaunchedEffect(retryStartExamAfterLocationPermissionGrant) {
        if (retryStartExamAfterLocationPermissionGrant) {
            retryStartExamAfterLocationPermissionGrant = false
            startExamController.showStartExamPreflight()
            startExamController.startExamSession()
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
        refreshScreenPinningDiagnostics = { refreshScreenPinningDiagnostics() },
        refreshKeyboardSecurity = { triggerViolation -> refreshKeyboardSecurity(triggerViolation) },
        refreshBluetoothSecurity = { triggerViolation -> refreshBluetoothSecurity(triggerViolation) },
        refreshDeviceIntegritySecurity = { triggerViolation -> refreshDeviceIntegritySecurity(triggerViolation) },
        updateBluetoothPermissionGranted = { bluetoothPermissionGranted = it },
        updateUseBuiltInExamKeyboard = { useBuiltInExamKeyboard = it },
        updateShowBuiltInExamKeyboard = { showBuiltInExamKeyboard = it },
        cleanupActiveExamWebViewInstance = { cleanupActiveExamWebViewInstance() }
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
        refreshGeofenceStatus = { preferFresh, trigger, allowRuntimeViolation -> refreshGeofenceStatus(preferFresh, trigger, allowRuntimeViolation) }
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
        recordAction = { code, details, level -> recordAction(code, details, level) }
    )

    DisposableEffect(mainActivity) {
        onDispose {
            mainActivity?.setExamLockMode(enabled = false, allowLockTask = false)
            securityUiState.overlayGuardActive.value = false
        }
    }

    RuntimeAppSwitchFallbackLoggingEffect(
        examGuardArmed = examGuardArmed,
        appSwitchStatus = appSwitchStatus,
        screenPinningMode = screenPinningMode,
        appSwitchFallbackArmedLogged = appSwitchFallbackArmedLogged,
        updateAppSwitchFallbackArmedLogged = { appSwitchFallbackArmedLogged = it },
        recordAction = { code, details, level -> recordAction(code, details, level) }
    )

    AccessibilityExamGuardViolationEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        onViolation = { violation -> handleAccessibilityGuardViolation(violation) }
    )

    AccessibilityExamGuardLivenessEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        recordAction = { code, details, level -> recordAction(code, details, level) }
    )

    RuntimeDisposeCleanupEffect(
        examSessionStarted = examSessionStarted,
        lockTaskRequestPending = lockTaskRequestPending,
        lockTaskBridge = lockTaskBridge,
        cleanupActiveExamWebViewInstance = { cleanupActiveExamWebViewInstance() },
        launchExitSessionClearBestEffort = {
            launchExitSessionClearBestEffort("runtime_dispose")
        },
        clearDpcExamPoliciesForSession = { reason -> clearDpcExamPoliciesForSession(reason) },
        disarmAccessibilityGuard = { AccessibilityExamGuardStore.disarm(context) },
        stopAlarm = { examAlarmController.stop() }
    )

    val runtimeLockTaskRequirement = resolveLockTaskSecurityRequirement(
        dpcExamPolicyAppliedForSession || dpcRuntimeStatus.deviceOwner
    )

    RuntimeScreenPinningActivationEffect(
        mainActivity = mainActivity,
        lockTaskBridge = lockTaskBridge,
        lockTaskRequirement = runtimeLockTaskRequirement,
        isIndonesian = isIndonesian,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        coroutineScope = coroutineScope,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        clearAppSwitchSuppression = { clearAppSwitchSuppression() },
        disarmExamRuntimeMonitoring = { disarmExamRuntimeMonitoring() },
        resetPreparationSecurityEpisodes = { startExamController.resetPreparationSecurityEpisodes() },
        prepareCleanExamWebViewSessionForStart = { startExamController.prepareCleanExamWebViewSessionForStart() },
        finalizeExamSessionStart = { lockTaskAlreadyActive -> startExamController.finalizeExamSessionStart(lockTaskAlreadyActive) }
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
        lockTaskRequirement = runtimeLockTaskRequirement,
        isIndonesian = isIndonesian,
        deviceQuirkProfile = deviceQuirkProfile,
        currentScreenPinningMonitorIntervalMillis = { currentScreenPinningMonitorIntervalMillis() },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        applyFatalSecuritySignal = { signal -> applyFatalSecuritySignal(signal) }
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
        currentAppSwitchSuppressionReason = { currentAppSwitchSuppressionReason() },
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
        currentInternalDialogReason = { currentInternalDialogReason() },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        recordAppSwitchEvent = { code, signal, level -> recordAppSwitchEvent(code, signal, level) },
        onScreenPinningTransitionInterrupted = { handleScreenPinningTransitionInterrupted() },
        armClipboardResumeCheck = { reason -> armClipboardResumeCheck(reason) },
        startAlarm = { examAlarmController.start() }
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
        recordAction = { code, details, level -> recordAction(code, details, level) },
        startAlarm = { examAlarmController.start() }
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
        currentAppSwitchSuppressionReason = { currentAppSwitchSuppressionReason() },
        currentAppSwitchEventDetails = { signal -> currentAppSwitchEventDetails(signal) },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        recordAppSwitchEvent = { code, signal, level -> recordAppSwitchEvent(code, signal, level) },
        armClipboardResumeCheck = { reason -> armClipboardResumeCheck(reason) },
        refreshReverseEngineeringStatus = { refreshReverseEngineeringStatus() },
        refreshKeyboardSecurity = { triggerViolation -> refreshKeyboardSecurity(triggerViolation) },
        refreshBluetoothSecurity = { triggerViolation -> refreshBluetoothSecurity(triggerViolation) },
        refreshDeviceIntegritySecurity = { triggerViolation -> refreshDeviceIntegritySecurity(triggerViolation) },
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
        diagnosticTimestamp = { diagnosticTimestamp() }
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
        refreshDeviceTimeSecurity = { trigger, emitDiagnosticEvent -> refreshDeviceTimeSecurity(trigger, emitDiagnosticEvent) },
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
        diagnosticTimestamp = { diagnosticTimestamp() }
    )

    RuntimeConnectivityEffects(
        context = context,
        examSessionStarted = examSessionStarted,
        networkReadinessStatus = networkReadinessStatus,
        baseNetworkReadiness = baseNetworkReadiness,
        networkUiState = networkUiState,
        batteryStatusState = batteryStatusState,
        networkMainHandler = networkMainHandler,
        updateNetworkReadiness = { source -> updateNetworkReadiness(source) },
        currentNetworkPollingIntervalMillis = { currentNetworkPollingIntervalMillis() },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        currentNetworkEventDetails = { trigger, status, extraContext -> currentNetworkEventDetails(trigger, status, extraContext) },
        clearNetworkFlapHistory = { networkFlapElapsedMs.clear() },
        diagnosticTimestamp = { diagnosticTimestamp() }
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
}
