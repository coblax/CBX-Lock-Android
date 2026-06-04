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
    val reverseEngineeringReady: Boolean,
    val integrityReady: Boolean,
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
            reverseEngineeringReady &&
            integrityReady
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
        staticSecurityInitialScanComplete = runtimeSecurity.staticSecurityInitialScanComplete,
        canStartExam = canStartExam,
        hasBypassIndicators = hasBypassIndicators
    )
}

internal data class PreparationReadinessSummary(
    val blockingCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val firstBlockingReason: String?
)

internal fun buildPreparationReadinessSummary(
    readiness: PreparationChecklistReadiness,
    blockingReasonEN: String?,
    blockingReasonID: String?
): PreparationReadinessSummary {
    val checks = listOf(
        readiness.bluetoothReady,
        readiness.accessibilityReady,
        readiness.adbReady,
        readiness.rootReady,
        readiness.virtualEnvironmentReady,
        readiness.vpnReady,
        readiness.deviceTimeReady,
        readiness.geofenceReady,
        readiness.fakeLocationReady,
        readiness.overlayReady,
        readiness.accessibilityGuardReady,
        readiness.screenPinningReady,
        readiness.appSwitchReady,
        readiness.screenRecorderReady,
        readiness.displayMirrorReady,
        readiness.multiWindowReady,
        readiness.reverseEngineeringReady,
        readiness.integrityReady,
        readiness.signatureReady
    )
    val safeCount = checks.count { it }
    val blockingCount = checks.count { !it }
    return PreparationReadinessSummary(
        blockingCount = blockingCount,
        warningCount = 0, // warnings are derived from quick fix actions, not readiness
        safeCount = safeCount,
        firstBlockingReason = if (!readiness.canStartExam) {
            blockingReasonEN // caller provides the localized string
        } else null
    )
}

internal fun resolveFirstBlockingReason(
    readiness: PreparationChecklistReadiness,
    en: Boolean = true
): String? {
    if (readiness.canStartExam) return null
    if (!readiness.staticSecurityInitialScanComplete) return if (en) "Security scan in progress" else "Pemindaian keamanan sedang berlangsung"
    if (!readiness.adbReady) return if (en) "USB Debugging is active" else "USB Debugging masih aktif"
    if (!readiness.deviceTimeReady) return if (en) "Automatic date & time not enabled" else "Tanggal & waktu otomatis belum aktif"
    if (!readiness.rootReady) return if (en) "Root device detected" else "Perangkat root terdeteksi"
    if (!readiness.virtualEnvironmentReady) return if (en) "Emulator detected" else "Emulator terdeteksi"
    if (!readiness.reverseEngineeringReady) return if (en) "Debugging or hooking tool detected" else "Tool debugging atau hooking terdeteksi"
    if (!readiness.integrityReady) return if (en) "APK integrity check failed" else "Cek integritas APK gagal"
    if (!readiness.signatureReady) return if (en) "App signature mismatch" else "Signature aplikasi tidak cocok"
    if (!readiness.vpnReady) return if (en) "VPN is active" else "VPN masih aktif"
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
    if (!readiness.appSwitchReady) return if (en) "App switch violation" else "Pelanggaran app switch"
    return if (en) "Device check not passed" else "Pemeriksaan perangkat belum lulus"
}

/**
 * Per-section health for collapsible sections (#8) and smart order (#6).
 */
internal data class SectionHealth(
    val title: String,
    val allClear: Boolean,
    val issueCount: Int
)

internal fun buildSectionHealthMap(
    readiness: PreparationChecklistReadiness
): Map<String, SectionHealth> {
    return mapOf(
        "checklist_device_setup" to SectionHealth(
            title = "Device Setup",
            allClear = readiness.keyboardReady && readiness.bluetoothReady,
            issueCount = listOf(readiness.keyboardReady, readiness.bluetoothReady).count { !it }
        ),
        "checklist_connectivity" to SectionHealth(
            title = "Connectivity",
            allClear = true, // network readiness is not in readiness flags (derived differently)
            issueCount = 0
        ),
        "checklist_device_health" to SectionHealth(
            title = "Device Health",
            allClear = readiness.deviceTimeReady,
            issueCount = listOf(readiness.deviceTimeReady).count { !it }
        ),
        "checklist_runtime_interaction" to SectionHealth(
            title = "Runtime Interaction",
            allClear = readiness.accessibilityReady && readiness.overlayReady,
            issueCount = listOf(readiness.accessibilityReady, readiness.overlayReady).count { !it }
        ),
        "checklist_device_integrity" to SectionHealth(
            title = "Device Integrity",
            allClear = readiness.adbReady && readiness.rootReady && readiness.signatureReady && readiness.virtualEnvironmentReady,
            issueCount = listOf(readiness.adbReady, readiness.rootReady, readiness.signatureReady, readiness.virtualEnvironmentReady).count { !it }
        ),
        "checklist_runtime_clipboard" to SectionHealth(
            title = "Clipboard",
            allClear = readiness.clipboardReady,
            issueCount = listOf(readiness.clipboardReady).count { !it }
        ),
        "checklist_location" to SectionHealth(
            title = "Location",
            allClear = readiness.geofenceReady && readiness.fakeLocationReady,
            issueCount = listOf(readiness.geofenceReady, readiness.fakeLocationReady).count { !it }
        ),
        "checklist_device_lock" to SectionHealth(
            title = "Device Lock",
            allClear = readiness.screenPinningReady && readiness.accessibilityGuardReady,
            issueCount = listOf(readiness.screenPinningReady, readiness.accessibilityGuardReady).count { !it }
        ),
        "checklist_runtime_static_security" to SectionHealth(
            title = "Runtime Security",
            allClear = readiness.screenRecorderReady && readiness.displayMirrorReady && readiness.multiWindowReady && readiness.appSwitchReady,
            issueCount = listOf(readiness.screenRecorderReady, readiness.displayMirrorReady, readiness.multiWindowReady, readiness.appSwitchReady).count { !it }
        )
    )
}
