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
): PreparationChecklistReadiness = with(state) {
    val keyboardReady = bypassKeyboardPolicy || keyboardAllowed || usingBuiltInExamKeyboard
    val bluetoothReady =
        bypassBluetooth || (!bluetoothEnabled && (!needsBluetoothPermission || bluetoothPermissionGranted))
    val accessibilityReady = bypassAccessibility || !accessibilityServiceEnabled
    val adbReady = bypassAdb || (!adbInspection.blocking && !adbInspection.insecureSystemProperty)
    val rootReady = bypassRoot || !rootSecurityStatus.blocking
    val virtualEnvironmentReady = bypassVirtualEnvironment || !virtualEnvironmentDetected
    val vpnReady = bypassVpn || !networkReadinessStatus.diagnostics.isVpnActive
    val clipboardReady = true
    val deviceTimeReady = bypassDeviceTime || !deviceTimeSecurityStatus.blocking
    val geofenceReady =
        bypassGeofence ||
            !geofenceRuntimeStatus.evaluation.enabled ||
            !geofenceRuntimeStatus.securityStatus.blocking
    val fakeLocationReady =
        bypassFakeLocation ||
            !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled ||
            (!fakeLocationRuntimeStatus.securityStatus.blocking &&
                !(fakeLocationRuntimeStatus.securityStatus.warningOnly &&
                    fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled))
    val overlayReady = bypassOverlay || !overlayRiskResult.hasAnyRisk
    val overlayBlockingReady = bypassOverlay || !overlayRiskResult.confirmedInteractionDetected
    val accessibilityGuardReady = !accessibilityGuardRequired || accessibilityGuardEnabled
    val screenPinningReady = resolvePreparationScreenPinningReady(
        bypassScreenPinning = bypassScreenPinning,
        screenPinningAvailable = screenPinningAvailable,
        isScreenPinningActive = isScreenPinningActive,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityGuardEnabled = accessibilityGuardEnabled
    )
    val appSwitchReady = bypassAppSwitch || !appSwitchStatus.hasViolations
    val screenRecorderReady = bypassScreenRecorder || screenRecorderPackages.isEmpty()
    val displayMirrorReady = bypassDisplayMirror || !externalDisplayDetected
    val multiWindowReady = bypassMultiWindow || !multiWindowDetected
    val signatureReady = !signatureMismatchDetected
    val canStartExam =
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
            !tamperDetected
    val hasBypassIndicators = listOf(
        bypassKeyboardPolicy,
        bypassBluetooth,
        bypassAccessibility,
        bypassAdb,
        bypassRoot,
        bypassVirtualEnvironment,
        bypassVpn,
        bypassClipboard,
        bypassScreenPinning,
        bypassOverlay,
        bypassGeofence,
        bypassFakeLocation,
        bypassDeviceTime,
        bypassAppSwitch,
        bypassScreenRecorder,
        bypassDisplayMirror,
        bypassMultiWindow,
        tamperDetected
    ).any { it }

    PreparationChecklistReadiness(
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
        canStartExam = canStartExam,
        hasBypassIndicators = hasBypassIndicators
    )
}
