package com.example.coblaxexamlock.ui.preparation

internal data class PreparationChecklistReadiness(
    val keyboardReady: Boolean,
    val bluetoothReady: Boolean,
    val accessibilityReady: Boolean,
    val adbReady: Boolean,
    val rootReady: Boolean,
    val virtualEnvironmentReady: Boolean,
    val clipboardReady: Boolean,
    val deviceTimeReady: Boolean,
    val geofenceReady: Boolean,
    val fakeLocationReady: Boolean,
    val overlayReady: Boolean,
    val accessibilityGuardReady: Boolean,
    val screenPinningReady: Boolean,
    val appSwitchReady: Boolean,
    val signatureReady: Boolean,
    val canStartExam: Boolean,
    val hasBypassIndicators: Boolean
)

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
    val rootReady = bypassRoot || (!rootSecurityStatus.blocking && !rootSecurityStatus.selinuxPermissive)
    val virtualEnvironmentReady = bypassVirtualEnvironment || !virtualEnvironmentDetected
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
    val accessibilityGuardReady = !accessibilityGuardRequired || accessibilityGuardEnabled
    val screenPinningReady =
        bypassScreenPinning || screenPinningAvailable || (accessibilityGuardAvailable && accessibilityGuardEnabled)
    val appSwitchReady = bypassAppSwitch || !appSwitchStatus.hasViolations
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
            virtualEnvironmentReady &&
            signatureReady &&
            !tamperDetected
    val hasBypassIndicators = listOf(
        bypassKeyboardPolicy,
        bypassBluetooth,
        bypassAccessibility,
        bypassAdb,
        bypassRoot,
        bypassVirtualEnvironment,
        bypassClipboard,
        bypassScreenPinning,
        bypassOverlay,
        bypassGeofence,
        bypassFakeLocation,
        bypassDeviceTime,
        bypassAppSwitch,
        tamperDetected
    ).any { it }

    PreparationChecklistReadiness(
        keyboardReady = keyboardReady,
        bluetoothReady = bluetoothReady,
        accessibilityReady = accessibilityReady,
        adbReady = adbReady,
        rootReady = rootReady,
        virtualEnvironmentReady = virtualEnvironmentReady,
        clipboardReady = clipboardReady,
        deviceTimeReady = deviceTimeReady,
        geofenceReady = geofenceReady,
        fakeLocationReady = fakeLocationReady,
        overlayReady = overlayReady,
        accessibilityGuardReady = accessibilityGuardReady,
        screenPinningReady = screenPinningReady,
        appSwitchReady = appSwitchReady,
        signatureReady = signatureReady,
        canStartExam = canStartExam,
        hasBypassIndicators = hasBypassIndicators
    )
}
