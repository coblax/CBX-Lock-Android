package com.example.coblaxexamlock.ui.preparation

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
    val signatureReady: Boolean,
    val staticSecurityInitialScanComplete: Boolean,
    val canStartExam: Boolean,
    val hasBypassIndicators: Boolean
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
    accessibilityGuardEnabled: Boolean
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
    accessibilityGuardEnabled: Boolean
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
    val overlayReady = bypass.bypassOverlay || !runtimeSecurity.overlayRiskResult.hasAnyRisk
    val overlayBlockingReady =
        bypass.bypassOverlay || !runtimeSecurity.overlayRiskResult.confirmedInteractionDetected
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
    val signatureReady = !device.signatureMismatchDetected
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
            signatureReady &&
            screenRecorderReady &&
            displayMirrorReady &&
            multiWindowReady &&
            !runtimeSecurity.tamperDetected
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
        signatureReady = signatureReady,
        staticSecurityInitialScanComplete = runtimeSecurity.staticSecurityInitialScanComplete,
        canStartExam = canStartExam,
        hasBypassIndicators = hasBypassIndicators
    )
}
