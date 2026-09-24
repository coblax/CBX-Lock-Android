package com.coblax.examlock.ui.preparation

import com.coblax.examlock.model.NetworkReadinessVerdict

internal data class PreparationChecklistReadiness(
    val keyboardReady: Boolean,
    val bluetoothReady: Boolean,
    val accessibilityReady: Boolean,
    val adbReady: Boolean,
    val rootReady: Boolean,
    val virtualEnvironmentReady: Boolean,
    val vpnReady: Boolean,
    val clipboardReady: Boolean,
    val deviceTimeReady: Boolean,
    val geofenceReady: Boolean,
    val fakeLocationReady: Boolean,
    val overlayReady: Boolean,
    val accessibilityGuardReady: Boolean,
    val screenPinningReady: Boolean,
    val appSwitchReady: Boolean,
    val screenRecorderReady: Boolean,
    val displayMirrorReady: Boolean,
    val multiWindowReady: Boolean,
    val reverseEngineeringReady: Boolean,
    val integrityReady: Boolean,
    val signatureReady: Boolean,
    /** No start-time health blocker the specific checks above do not already cover. */
    val startHealthReady: Boolean,
    /** Start Exam refuses while offline or in airplane mode, with no bypass. */
    val networkReachableReady: Boolean,
    /** False once the exam window has closed; Start Exam refuses then too. */
    val scheduleReady: Boolean,
    val staticSecurityInitialScanComplete: Boolean,
    val canStartExam: Boolean,
    val hasBypassIndicators: Boolean
)

/**
 * Start Exam re-runs the pre-exam health check and refuses on any Blocking item
 * (preExamHealthStartBlocker). These categories have no specific readiness flag, so
 * without this gate the screen said "ready" and the refusal only came after Start.
 * Screen pinning, network, location and device time are gated by their own flags.
 */
internal val StartHealthGateCategories: Set<PreExamHealthCategory> = setOf(
    PreExamHealthCategory.WebView,
    PreExamHealthCategory.FloatingAppOverlay
)

internal fun resolvePreparationScreenPinningReady(
    bypassScreenPinning: Boolean,
    screenPinningAvailable: Boolean,
    isScreenPinningActive: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardEnabled: Boolean
): Boolean {
    return bypassScreenPinning ||
        isScreenPinningActive ||
        (!screenPinningAvailable && accessibilityGuardAvailable && accessibilityGuardEnabled)
}

internal fun buildPreparationChecklistReadiness(
    state: PreparationScreenState,
    needsBluetoothPermission: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardEnabled: Boolean,
    examScheduleEnded: Boolean = false
): PreparationChecklistReadiness = buildPreparationChecklistReadiness(
    network = state.network,
    device = state.device,
    location = state.location,
    runtimeSecurity = state.runtimeSecurity,
    bypass = state.bypass,
    needsBluetoothPermission = needsBluetoothPermission,
    accessibilityGuardRequired = accessibilityGuardRequired,
    accessibilityGuardAvailable = accessibilityGuardAvailable,
    accessibilityGuardEnabled = accessibilityGuardEnabled
,
    preExamHealthSnapshot = state.preExamHealthCheckSnapshot,
    examScheduleEnded = examScheduleEnded
)

internal fun buildPreparationChecklistReadiness(
    network: PreparationNetworkState,
    device: PreparationDeviceState,
    location: PreparationLocationState,
    runtimeSecurity: PreparationRuntimeSecurityState,
    bypass: PreparationBypassState,
    needsBluetoothPermission: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardEnabled: Boolean,
    preExamHealthSnapshot: PreExamHealthSnapshot? = null,
    examScheduleEnded: Boolean = false
): PreparationChecklistReadiness {
    val keyboardReady = bypass.bypassKeyboardPolicy ||
        device.keyboardAllowed ||
        device.usingBuiltInExamKeyboard
    val bluetoothReady =
        bypass.bypassBluetooth ||
            (!device.bluetoothEnabled && (!needsBluetoothPermission || device.bluetoothPermissionGranted))
    val accessibilityReady = bypass.bypassAccessibility || !runtimeSecurity.accessibilityServiceEnabled
    val adbReady = bypass.bypassAdb ||
        (!device.adbInspection.blocking && !device.adbInspection.insecureSystemProperty)
    val rootReady = bypass.bypassRoot || !device.rootSecurityStatus.blocking
    val virtualEnvironmentReady = bypass.bypassVirtualEnvironment || !device.virtualEnvironmentDetected
    val vpnReady = network.bypassVpn || !network.networkReadinessStatus.diagnostics.isVpnActive
    val scheduleReady = !examScheduleEnded
    val networkReachableReady = network.networkReadinessStatus.verdict !in setOf(
        NetworkReadinessVerdict.Offline,
        NetworkReadinessVerdict.AirplaneMode
    )
    val clipboardReady = true
    val deviceTimeReady = bypass.bypassDeviceTime || !device.deviceTimeSecurityStatus.blocking
    val geofenceReady =
        bypass.bypassGeofence ||
            !location.geofenceRuntimeStatus.evaluation.enabled ||
            !location.geofenceRuntimeStatus.securityStatus.blocking
    val fakeLocationReady =
        bypass.bypassFakeLocation ||
            !location.fakeLocationRuntimeStatus.securityStatus.monitoringEnabled ||
            (!location.fakeLocationRuntimeStatus.securityStatus.blocking &&
                !(location.fakeLocationRuntimeStatus.securityStatus.warningOnly &&
                    location.fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled))
    val overlayReady = bypass.bypassOverlay || !runtimeSecurity.overlayRiskResult.hasBlockingRisk
    val overlayBlockingReady =
        bypass.bypassOverlay || !runtimeSecurity.overlayRiskResult.hasBlockingRisk
    val accessibilityGuardReady = !accessibilityGuardRequired || accessibilityGuardEnabled
    val screenPinningReady = resolvePreparationScreenPinningReady(
        bypassScreenPinning = bypass.bypassScreenPinning,
        screenPinningAvailable = device.screenPinningAvailable,
        isScreenPinningActive = device.isScreenPinningActive,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityGuardEnabled = accessibilityGuardEnabled
    )
    val appSwitchReady = bypass.bypassAppSwitch || !runtimeSecurity.appSwitchStatus.hasViolations
    val screenRecorderReady =
        bypass.bypassScreenRecorder || runtimeSecurity.screenRecorderPackages.isEmpty()
    val displayMirrorReady = bypass.bypassDisplayMirror || !runtimeSecurity.externalDisplayDetected
    val multiWindowReady = bypass.bypassMultiWindow || !runtimeSecurity.multiWindowDetected
    val reverseEngineeringReady =
        runtimeSecurity.reverseEngineeringBypassActive || !runtimeSecurity.reverseEngineeringDetected
    val integrityReady =
        runtimeSecurity.integrityBypassActive ||
            (!runtimeSecurity.integrityDetected && !device.signatureMismatchDetected)
    val signatureReady = integrityReady
    val startHealthReady = preExamHealthSnapshot == null ||
        preExamHealthSnapshot.items.none { it.category in StartHealthGateCategories && it.verdict == PreExamHealthVerdict.Blocking }
    val canStartExam =
        runtimeSecurity.staticSecurityInitialScanComplete &&
            bluetoothReady &&
            accessibilityReady &&
            adbReady &&
            rootReady &&
            deviceTimeReady &&
            screenPinningReady &&
            accessibilityGuardReady &&
            geofenceReady &&
            fakeLocationReady &&
            overlayBlockingReady &&
            virtualEnvironmentReady &&
            vpnReady &&
            networkReachableReady &&
            scheduleReady &&
            signatureReady &&
            screenRecorderReady &&
            displayMirrorReady &&
            multiWindowReady &&
            reverseEngineeringReady &&
            integrityReady &&
            startHealthReady
    val hasBypassIndicators = listOf(
        bypass.bypassKeyboardPolicy,
        bypass.bypassBluetooth,
        bypass.bypassAccessibility,
        bypass.bypassAdb,
        bypass.bypassRoot,
        bypass.bypassVirtualEnvironment,
        network.bypassVpn,
        bypass.bypassClipboard,
        bypass.bypassScreenPinning,
        bypass.bypassOverlay,
        bypass.bypassGeofence,
        bypass.bypassFakeLocation,
        bypass.bypassDeviceTime,
        bypass.bypassAppSwitch,
        bypass.bypassScreenRecorder,
        bypass.bypassDisplayMirror,
        bypass.bypassMultiWindow,
        bypass.bypassReverseEngineering,
        bypass.bypassApkIntegrity,
        runtimeSecurity.tamperDetected
    ).any { it }

    return PreparationChecklistReadiness(
        keyboardReady = keyboardReady,
        bluetoothReady = bluetoothReady,
        accessibilityReady = accessibilityReady,
        adbReady = adbReady,
        rootReady = rootReady,
        virtualEnvironmentReady = virtualEnvironmentReady,
        vpnReady = vpnReady,
        clipboardReady = clipboardReady,
        deviceTimeReady = deviceTimeReady,
        geofenceReady = geofenceReady,
        fakeLocationReady = fakeLocationReady,
        overlayReady = overlayReady,
        accessibilityGuardReady = accessibilityGuardReady,
        screenPinningReady = screenPinningReady,
        appSwitchReady = appSwitchReady,
        screenRecorderReady = screenRecorderReady,
        displayMirrorReady = displayMirrorReady,
        multiWindowReady = multiWindowReady,
        reverseEngineeringReady = reverseEngineeringReady,
        integrityReady = integrityReady,
        signatureReady = signatureReady,
        startHealthReady = startHealthReady,
        networkReachableReady = networkReachableReady,
        scheduleReady = scheduleReady,
        staticSecurityInitialScanComplete = runtimeSecurity.staticSecurityInitialScanComplete,
        canStartExam = canStartExam,
        hasBypassIndicators = hasBypassIndicators
    )
}

internal fun resolveFirstBlockingReason(
    readiness: PreparationChecklistReadiness,
    en: Boolean = true
): String? {
    if (readiness.canStartExam) return null
    if (!readiness.scheduleReady) return if (en) "The exam has ended" else "Waktu ujian sudah berakhir"
    if (!readiness.staticSecurityInitialScanComplete) return if (en) "Security scan in progress" else "Pemindaian keamanan sedang berlangsung"
    if (!readiness.adbReady) return if (en) "USB Debugging is active" else "USB Debugging masih aktif"
    if (!readiness.deviceTimeReady) return if (en) "Automatic date & time not enabled" else "Tanggal & waktu otomatis belum aktif"
    if (!readiness.rootReady) return if (en) "Root device detected" else "Perangkat root terdeteksi"
    if (!readiness.virtualEnvironmentReady) return if (en) "Emulator detected" else "Emulator terdeteksi"
    if (!readiness.reverseEngineeringReady) return if (en) "Debugging or hooking tool detected" else "Tool debugging atau hooking terdeteksi"
    if (!readiness.integrityReady) return if (en) "APK integrity check failed" else "Cek integritas APK gagal"
    if (!readiness.signatureReady) return if (en) "App signature mismatch" else "Signature aplikasi tidak cocok"
    if (!readiness.vpnReady) return if (en) "VPN is active" else "VPN masih aktif"
    if (!readiness.networkReachableReady) return if (en) "No internet connection" else "Tidak ada koneksi internet"
    if (!readiness.accessibilityReady) return if (en) "Accessibility service is active" else "Layanan aksesibilitas masih aktif"
    if (!readiness.accessibilityGuardReady) return if (en) "Exam Guard not enabled" else "Exam Guard belum diaktifkan"
    if (!readiness.bluetoothReady) return if (en) "Bluetooth is active" else "Bluetooth masih aktif"
    if (!readiness.screenPinningReady) return if (en) "Screen Pinning not active" else "Screen Pinning belum aktif"
    if (!readiness.geofenceReady) return if (en) "Geofence check not passed" else "Pemeriksaan geofence belum lulus"
    if (!readiness.fakeLocationReady) return if (en) "Fake location risk detected" else "Risiko lokasi palsu terdeteksi"
    if (!readiness.overlayReady) return if (en) "Overlay risk detected" else "Risiko overlay terdeteksi"
    if (!readiness.screenRecorderReady) return if (en) "Screen recorder detected" else "Screen recorder terdeteksi"
    if (!readiness.displayMirrorReady) return if (en) "External display detected" else "Layar eksternal terdeteksi"
    if (!readiness.multiWindowReady) return if (en) "Multi-window mode active" else "Mode multi-window aktif"
    if (!readiness.startHealthReady) return if (en) "Pre-exam health check not passed" else "Health check sebelum ujian belum lulus"
    if (!readiness.appSwitchReady) return if (en) "App switch violation" else "Pelanggaran app switch"
    return if (en) "Device check not passed" else "Pemeriksaan perangkat belum lulus"
}
