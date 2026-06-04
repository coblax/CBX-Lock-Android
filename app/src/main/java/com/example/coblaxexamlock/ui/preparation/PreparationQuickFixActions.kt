package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayQuickFixTarget
import com.example.coblaxexamlock.accessibilityQuickFixButtonText
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.UiLanguage

internal fun buildPreparationQuickFixActions(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    uiLanguage: UiLanguage,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean,
    geofenceReady: Boolean,
    fakeLocationReady: Boolean,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: AccessibilityInspectionResult,
    runQuickFix: (QuickFixTarget?, String, Boolean, () -> Unit) -> Unit
): List<PreparationQuickFixAction> {
    with(state) {
        with(actions) {
            fun t(english: String, indonesian: String): String =
                localized(uiLanguage, english, indonesian)
            val showKeyboardFix = usingBuiltInExamKeyboard && !bypassKeyboardPolicy
            val showBluetoothPermissionFix =
                !bypassBluetooth && needsBluetoothPermission && !bluetoothPermissionGranted
            val showBluetoothFix = !bypassBluetooth && bluetoothEnabled
            val showAccessibilityFix = !bypassAccessibility && accessibilityServiceEnabled
            val showAccessibilityGuardFix = accessibilityGuardRequired && !accessibilityGuardEnabled
            val showOverlayAccessibilityFix =
                !bypassOverlay &&
                    overlayRiskResult.quickFixTargets.contains(OverlayQuickFixTarget.AccessibilitySettings) &&
                    !showAccessibilityFix
            val showAdbFix = !bypassAdb && adbInspection.blocking
            val showAdbInsecurePropertyFix = !bypassAdb && !adbInspection.blocking && adbInspection.insecureSystemProperty
            val showRootBlockingFix = !bypassRoot && rootSecurityStatus.blocking
            val showRootSelinuxFix = !bypassRoot && !rootSecurityStatus.blocking && rootSecurityStatus.selinuxPermissive && rootSecurityStatus.detected
            val showVirtualEnvFix = !bypassVirtualEnvironment && virtualEnvironmentDetected
            val showAppSwitchViolationFix = !bypassAppSwitch && appSwitchStatus.hasViolations
            val showGeofenceRequestPermissionFix =
                !bypassGeofence &&
                    geofenceRuntimeStatus.evaluation.enabled &&
                    geofenceRuntimeStatus.securityStatus.finalVerdict in setOf(
                        GeofenceSecurityVerdict.PermissionMissing,
                        GeofenceSecurityVerdict.PreciseRequired
                    )
            val showGeofenceOpenLocationServicesFix =
                !bypassGeofence &&
                    geofenceRuntimeStatus.evaluation.enabled &&
                    geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled
            val showGeofenceRefreshFix =
                !bypassGeofence &&
                    geofenceRuntimeStatus.evaluation.enabled &&
                    geofenceRuntimeStatus.securityStatus.finalVerdict in setOf(
                        GeofenceSecurityVerdict.NoFix,
                        GeofenceSecurityVerdict.StaleFix,
                        GeofenceSecurityVerdict.LowAccuracy,
                        GeofenceSecurityVerdict.MissingAccuracy,
                        GeofenceSecurityVerdict.Outside
                    )
            val showGeofenceMapFix =
                !bypassGeofence &&
                    geofenceRuntimeStatus.evaluation.enabled &&
                    geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside
            val showFakeLocationRequestPermissionFix =
                !bypassFakeLocation &&
                    fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                    fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired
            val showFakeLocationOpenLocationServicesFix =
                !bypassFakeLocation &&
                    fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                    fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled
            val showFakeLocationRefreshFix =
                !bypassFakeLocation &&
                    fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                    fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable
            val showLocationPermissionFix = showGeofenceRequestPermissionFix || showFakeLocationRequestPermissionFix
            val showLocationServicesFix = showGeofenceOpenLocationServicesFix || showFakeLocationOpenLocationServicesFix
            val showLocationRefreshFix = showGeofenceRefreshFix || showFakeLocationRefreshFix
            val showNetworkInternetSettingsFix =
                networkReadinessStatus.verdict in setOf(
                    NetworkReadinessVerdict.Offline,
                    NetworkReadinessVerdict.Unvalidated,
                    NetworkReadinessVerdict.CaptivePortal,
                    NetworkReadinessVerdict.VpnActive,
                    NetworkReadinessVerdict.AirplaneMode,
                    NetworkReadinessVerdict.Unstable
                )
            val showNetworkVpnSettingsFix =
                networkReadinessStatus.verdict == NetworkReadinessVerdict.VpnActive && !bypassVpn
            val showNetworkWifiSettingsFix =
                networkReadinessStatus.verdict in setOf(
                    NetworkReadinessVerdict.Offline,
                    NetworkReadinessVerdict.Unvalidated,
                    NetworkReadinessVerdict.CaptivePortal,
                    NetworkReadinessVerdict.Unstable
                )
            val networkLooksCellular =
                networkReadinessStatus.transportLabel.contains("cellular", ignoreCase = true) ||
                    (lastConnectedNetworkLabel?.contains("cellular", ignoreCase = true) == true)
            val showNetworkCellularSettingsFix =
                networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable &&
                    networkLooksCellular
            val showNetworkAirplaneModeSettingsFix =
                networkReadinessStatus.verdict == NetworkReadinessVerdict.AirplaneMode
            val showNetworkRefreshFix = networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable
            val showDeviceTimeFix = !bypassDeviceTime && deviceTimeSecurityStatus.blocking
            val showFakeLocationDeveloperOptionsFix =
                !showAdbFix &&
                    !bypassFakeLocation &&
                    fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                    fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled &&
                    (
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PackageWarning ||
                            (
                                fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.SpoofDetected &&
                                    fakeLocationRuntimeStatus.securityStatus.confidenceTier in setOf(
                                        LocationSpoofConfidenceTier.Strong,
                                        LocationSpoofConfidenceTier.Critical
                                    )
                                )
                        )
            val quickFixIssueActions = buildList<PreparationQuickFixAction> {
                fun addQuickFix(
                    code: String = "",
                    text: String,
                    reason: String? = null,
                    severity: QuickFixSeverity,
                    target: QuickFixTarget?,
                    priority: Int,
                    section: PreparationSection? = null,
                    fieldText: String? = null,
                    filled: Boolean = false,
                    loading: Boolean = false,
                    enabled: Boolean = true,
                    opensExternalSettings: Boolean = false,
                    isNotice: Boolean = false,
                    diagnosticDetails: String? = null,
                    onClick: () -> Unit = {}
                ) {
                    val actionCode = code.ifBlank { "quick_fix_$priority" }
                    val externalBlockedByPinning = isScreenPinningActive && opensExternalSettings
                    val resolvedText = if (externalBlockedByPinning) {
                        "$text - ${t("Turn off Screen Pinning first", "Matikan Screen Pinning dulu")}"
                    } else {
                        text
                    }
                    val resolvedFieldText = fieldText?.let { label ->
                        if (externalBlockedByPinning) {
                            "$label - ${t("Turn off Screen Pinning first", "Matikan Screen Pinning dulu")}"
                        } else {
                            label
                        }
                    }
                    add(
                        PreparationQuickFixAction(
                            code = actionCode,
                            text = resolvedText,
                            reason = reason,
                            severity = severity,
                            target = target,
                            priority = priority,
                            section = section,
                            fieldText = resolvedFieldText,
                            filled = filled,
                            loading = loading,
                            enabled = enabled && !externalBlockedByPinning,
                            opensExternalSettings = opensExternalSettings,
                            isNotice = isNotice,
                            diagnosticDetails = diagnosticDetails,
                            onClick = if (isNotice) {
                                {}
                            } else {
                                { runQuickFix(target, actionCode, opensExternalSettings, onClick) }
                            }
                        )
                    )
                }

                if (showDeviceTimeFix) {
                    addQuickFix(
                        text = t("Enable Automatic Date & Time", "Aktifkan Tanggal & Waktu Otomatis"),
                        reason = t("Manual time can be used to bypass exam schedules.", "Waktu manual bisa dipakai untuk melewati jadwal ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.DeviceTime,
                        priority = 10,
                        section = PreparationSection.DeviceHealth,
                        fieldText = t("Fix Device Clock", "Perbaiki Jam Perangkat"),
                        filled = true,
                        opensExternalSettings = true,
                        onClick = onOpenDateTimeSettings
                    )
                }
                if (reinstallApkFixNeeded) {
                    addQuickFix(
                        text = t("Install Official APK Again", "Instal Ulang APK Resmi"),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 15,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Install Official APK", "Instal APK Resmi"),
                        filled = true,
                        onClick = onReinstallOfficialApk
                    )
                }
                if (showAdbFix) {
                    addQuickFix(
                        text = t("Turn Off USB Debugging", "Matikan USB Debugging"),
                        reason = t("USB Debugging allows external access to exam data.", "USB Debugging memungkinkan akses eksternal ke data ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 20,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Turn Off USB Debugging", "Matikan USB Debugging"),
                        opensExternalSettings = true,
                        onClick = onOpenDeveloperOptionsSettings
                    )
                }
                if (showAdbInsecurePropertyFix) {
                    addQuickFix(
                        code = "adb_insecure_property",
                        text = t(
                            "ADB system property insecure — contact your admin or check Developer Options",
                            "Properti sistem ADB tidak aman — hubungi admin atau periksa Developer Options"
                        ),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 21,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Check Developer Options", "Cek Developer Options"),
                        opensExternalSettings = true,
                        onClick = onOpenDeveloperOptionsSettings
                    )
                }
                if (showRootBlockingFix) {
                    addQuickFix(
                        code = "root_detected",
                        text = t(
                            "Root detected — contact your administrator or refresh checks",
                            "Root terdeteksi — hubungi administrator atau refresh pemeriksaan"
                        ),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 25,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Refresh Root Check", "Cek Ulang Root"),
                        onClick = onRefreshAllSecurityChecks
                    )
                }
                if (showRootSelinuxFix) {
                    addQuickFix(
                        code = "selinux_permissive",
                        text = t(
                            "SELinux is permissive — contact your administrator",
                            "SELinux permissive — hubungi administrator Anda"
                        ),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 26,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Contact Admin", "Hubungi Admin"),
                        onClick = onRefreshAllSecurityChecks
                    )
                }
                if (showVirtualEnvFix) {
                    addQuickFix(
                        code = "virtual_env_detected",
                        text = t(
                            "Emulator detected — exam must be on a physical device",
                            "Emulator terdeteksi — ujian harus dijalankan di perangkat fisik"
                        ),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 27,
                        section = PreparationSection.DeviceIntegrity,
                        fieldText = t("Use Physical Device", "Pakai HP Fisik"),
                        onClick = onRefreshAllSecurityChecks
                    )
                }
                if (showAppSwitchViolationFix) {
                    addQuickFix(
                        code = "app_switch_violations",
                        text = t(
                            "App switch violation recorded — refresh to clear",
                            "Pelanggaran app switch tercatat — refresh untuk menghapus"
                        ),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 28,
                        section = PreparationSection.RuntimeSecurity,
                        fieldText = t("Refresh App Switch Check", "Cek Ulang App Switch"),
                        onClick = onRefreshAllSecurityChecks
                    )
                }
                if (showFakeLocationDeveloperOptionsFix) {
                    addQuickFix(
                        text = t("Turn Off Mock Location App", "Matikan Aplikasi Lokasi Palsu"),
                        reason = t("Mock location apps can fake your exam position.", "Aplikasi lokasi palsu bisa memalsukan posisi ujian Anda."),
                        severity = if (fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 30,
                        section = PreparationSection.Location,
                        fieldText = t("Turn Off Fake Location", "Matikan Lokasi Palsu"),
                        opensExternalSettings = true,
                        onClick = onOpenFakeLocationDeveloperOptionsSettings
                    )
                }
                if (showAccessibilityFix) {
                    addQuickFix(
                        text = accessibilityQuickFixButtonText(accessibilityInspection, uiLanguage),
                        reason = t("Active accessibility services can read exam content.", "Layanan aksesibilitas aktif bisa membaca konten ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 35,
                        section = PreparationSection.RuntimeInteraction,
                        fieldText = t("Turn Off Accessibility", "Matikan Aksesibilitas"),
                        opensExternalSettings = true,
                        onClick = onOpenAccessibilitySettings
                    )
                }
                if (showAccessibilityGuardFix) {
                    addQuickFix(
                        text = t("Enable CBX Lock Exam Guard", "Aktifkan CBX Lock Exam Guard"),
                        reason = t("Exam Guard prevents app switching during the exam.", "Exam Guard mencegah perpindahan aplikasi selama ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 36,
                        section = PreparationSection.RuntimeInteraction,
                        fieldText = t("Enable Exam Guard", "Aktifkan Exam Guard"),
                        filled = true,
                        opensExternalSettings = true,
                        onClick = onOpenAccessibilitySettings
                    )
                }
                if (showLocationPermissionFix) {
                    addQuickFix(
                        text = if (showGeofenceRequestPermissionFix) {
                            t("Allow Precise Location", "Izinkan Lokasi Presisi")
                        } else {
                            t("Allow Location Permission", "Izinkan Akses Lokasi")
                        },
                        reason = t("Geofence needs precise location to verify your exam position.", "Geofence butuh lokasi presisi untuk memverifikasi posisi ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 40,
                        section = PreparationSection.Location,
                        fieldText = if (showGeofenceRequestPermissionFix) {
                            t("Allow Precise Location", "Izinkan Lokasi Presisi")
                        } else {
                            t("Allow Location", "Izinkan Lokasi")
                        },
                        filled = true,
                        onClick = onRequestLocationPermission
                    )
                }
                if (showLocationServicesFix) {
                    addQuickFix(
                        text = t("Turn On Location Services", "Aktifkan Layanan Lokasi"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 45,
                        section = PreparationSection.Location,
                        fieldText = t("Turn On Location", "Nyalakan Lokasi"),
                        opensExternalSettings = true,
                        onClick = onOpenLocationServicesSettings
                    )
                }
                if (showLocationRefreshFix) {
                    addQuickFix(
                        text = if (isRefreshingGeofence) {
                            t("Refreshing Location...", "Sedang Refresh Lokasi...")
                        } else {
                            t("Refresh Location Now", "Refresh Lokasi Sekarang")
                        },
                        severity = if (geofenceReady && fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 50,
                        section = PreparationSection.Location,
                        fieldText = if (isRefreshingGeofence) {
                            t("Checking Location...", "Sedang Cek Lokasi...")
                        } else {
                            t("Check Location Again", "Cek Ulang Lokasi")
                        },
                        loading = isRefreshingGeofence,
                        enabled = !isRefreshingGeofence,
                        onClick = onRefreshGeofenceLocation
                    )
                }
                if (showGeofenceMapFix) {
                    addQuickFix(
                        text = t("Open Geofence Map", "Buka Peta Geofence"),
                        severity = if (geofenceReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 55,
                        section = PreparationSection.Location,
                        fieldText = t("Open Location Map", "Buka Peta Lokasi"),
                        onClick = onOpenGeofenceMapViewer
                    )
                }
                if (showBluetoothPermissionFix) {
                    addQuickFix(
                        text = t("Allow Bluetooth Access", "Izinkan Akses Bluetooth"),
                        reason = t("Needed to verify no Bluetooth cheating devices are connected.", "Diperlukan untuk memverifikasi tidak ada perangkat curang Bluetooth."),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 60,
                        section = PreparationSection.DeviceSetup,
                        fieldText = t("Allow Bluetooth", "Izinkan Bluetooth"),
                        filled = true,
                        onClick = onGrantBluetoothPermission
                    )
                }
                if (showBluetoothFix) {
                    addQuickFix(
                        text = t("Turn Off Bluetooth", "Matikan Bluetooth"),
                        reason = t("Bluetooth can be used to share exam answers.", "Bluetooth bisa dipakai untuk berbagi jawaban ujian."),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 65,
                        section = PreparationSection.DeviceSetup,
                        fieldText = t("Turn Off Bluetooth", "Matikan Bluetooth"),
                        opensExternalSettings = true,
                        onClick = onOpenBluetoothSettings
                    )
                }

                val networkPrimaryIsRefresh = networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable
                if (showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = t("Turn Off Airplane Mode", "Matikan Mode Pesawat"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        section = PreparationSection.Connectivity,
                        fieldText = t("Turn Off Airplane Mode", "Matikan Mode Pesawat"),
                        opensExternalSettings = true,
                        onClick = onOpenAirplaneModeSettings
                    )
                } else if (showNetworkVpnSettingsFix) {
                    addQuickFix(
                        code = "vpn_settings_opened",
                        text = t("Open VPN Settings", "Buka Setelan VPN"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        section = PreparationSection.Connectivity,
                        fieldText = t("Turn Off VPN", "Matikan VPN"),
                        filled = true,
                        opensExternalSettings = true,
                        onClick = onOpenVpnSettings
                    )
                } else if (networkPrimaryIsRefresh && showNetworkRefreshFix) {
                    addQuickFix(
                        text = if (isRefreshingNetwork) {
                            t("Refreshing Network...", "Sedang Refresh Network...")
                        } else {
                            t("Refresh Network Status", "Refresh Status Network")
                        },
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        section = PreparationSection.Connectivity,
                        fieldText = if (isRefreshingNetwork) {
                            t("Checking Network...", "Sedang Cek Jaringan...")
                        } else {
                            t("Check Network Again", "Cek Ulang Jaringan")
                        },
                        loading = isRefreshingNetwork,
                        enabled = !isRefreshingNetwork,
                        onClick = onRefreshNetworkStatus
                    )
                } else if (showNetworkInternetSettingsFix) {
                    addQuickFix(
                        text = t("Open Internet Settings", "Buka Setelan Internet"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        section = PreparationSection.Connectivity,
                        fieldText = t("Open Internet Settings", "Buka Setelan Internet"),
                        opensExternalSettings = true,
                        onClick = onOpenInternetSettings
                    )
                }
                if (showNetworkWifiSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = t("Open Wi-Fi Settings", "Buka Setelan Wi-Fi"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 75,
                        section = PreparationSection.Connectivity,
                        fieldText = t("Open Wi-Fi Settings", "Buka Setelan Wi-Fi"),
                        opensExternalSettings = true,
                        onClick = onOpenWifiSettings
                    )
                }
                if (showNetworkCellularSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = t("Open Cellular Settings", "Buka Setelan Seluler"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 76,
                        section = PreparationSection.Connectivity,
                        fieldText = t("Open Cellular Settings", "Buka Setelan Seluler"),
                        opensExternalSettings = true,
                        onClick = onOpenCellularSettings
                    )
                }
                if (showNetworkRefreshFix && !networkPrimaryIsRefresh) {
                    addQuickFix(
                        text = if (isRefreshingNetwork) {
                            t("Refreshing Network...", "Sedang Refresh Network...")
                        } else {
                            t("Refresh Network Status", "Refresh Status Network")
                        },
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 80,
                        section = PreparationSection.Connectivity,
                        fieldText = if (isRefreshingNetwork) {
                            t("Checking Network...", "Sedang Cek Jaringan...")
                        } else {
                            t("Check Network Again", "Cek Ulang Jaringan")
                        },
                        loading = isRefreshingNetwork,
                        enabled = !isRefreshingNetwork,
                        onClick = onRefreshNetworkStatus
                    )
                }

                val webViewHealthItem = preExamHealthCheckSnapshot.items.firstOrNull {
                    it.category == PreExamHealthCategory.WebView &&
                        it.verdict != PreExamHealthVerdict.Stable
                }
                if (webViewHealthItem != null) {
                    addQuickFix(
                        code = "webview_provider_settings",
                        text = t("Open WebView Settings", "Buka Setelan WebView"),
                        severity = if (webViewHealthItem.verdict == PreExamHealthVerdict.Blocking) {
                            QuickFixSeverity.Blocking
                        } else {
                            QuickFixSeverity.Warning
                        },
                        target = QuickFixTarget.WebView,
                        priority = 90,
                        section = PreparationSection.DeviceHealth,
                        fieldText = t("Fix WebView", "Perbaiki WebView"),
                        filled = webViewHealthItem.verdict == PreExamHealthVerdict.Blocking,
                        opensExternalSettings = true,
                        onClick = onOpenWebViewProviderSettings
                    )
                }

                if (showKeyboardFix) {
                    addQuickFix(
                        text = t("Choose System Keyboard", "Pilih Keyboard Sistem"),
                        severity = QuickFixSeverity.Warning,
                        target = null,
                        priority = 200,
                        section = PreparationSection.DeviceSetup,
                        fieldText = t("Choose Keyboard", "Pilih Keyboard"),
                        filled = true,
                        opensExternalSettings = true,
                        onClick = onChooseKeyboard
                    )
                    addQuickFix(
                        text = t("Open Keyboard Settings", "Buka Pengaturan Keyboard"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 205,
                        section = PreparationSection.DeviceSetup,
                        fieldText = t("Open Keyboard Settings", "Buka Pengaturan Keyboard"),
                        opensExternalSettings = true,
                        onClick = onOpenKeyboardSettings
                    )
                }
                if (screenRecorderPackages.isNotEmpty() && !bypassScreenRecorder) {
                    addQuickFix(
                        text = t("Open App Settings", "Buka Setelan App"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.ScreenRecorder,
                        priority = 50,
                        section = PreparationSection.RuntimeSecurity,
                        fieldText = t("Close Screen Recorder", "Tutup Perekam Layar"),
                        opensExternalSettings = true,
                        onClick = onOpenAppSettings
                    )
                }
                if (externalDisplayDetected && !bypassDisplayMirror) {
                    addQuickFix(
                        text = t("Open Cast Settings", "Buka Setelan Cast"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.DisplayMirror,
                        priority = 45,
                        section = PreparationSection.RuntimeSecurity,
                        fieldText = t("Turn Off Cast or Mirror", "Matikan Cast/Mirroring"),
                        opensExternalSettings = true,
                        onClick = onOpenCastSettings
                    )
                }
                if (multiWindowDetected && !bypassMultiWindow) {
                    addQuickFix(
                        text = t("Refresh Status", "Refresh Status"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.MultiWindow,
                        priority = 40,
                        section = PreparationSection.RuntimeSecurity,
                        fieldText = t("Exit Split Screen", "Keluar Split Screen"),
                        onClick = onRefreshStatus
                    )
                }
                if (showOverlayAccessibilityFix) {
                    addQuickFix(
                        text = t("Review Accessibility for Overlay Risk", "Tinjau Aksesibilitas untuk Risiko Overlay"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 220,
                        section = PreparationSection.RuntimeInteraction,
                        fieldText = t("Review Accessibility Risk", "Cek Risiko Aksesibilitas"),
                        opensExternalSettings = true,
                        onClick = onOpenOverlayAccessibilitySettings
                    )
                }
                if (!bypassScreenPinning && screenPinningAvailable && !isScreenPinningActive) {
                    val blockingActionsBeforePinning = this.filter {
                        it.severity == QuickFixSeverity.Blocking && !it.isNotice
                    }
                    if (blockingActionsBeforePinning.isEmpty()) {
                        addQuickFix(
                            code = QuickFixStartScreenPinningCode,
                            text = t("Start Screen Pinning", "Start Screen Pinning"),
                            severity = QuickFixSeverity.Blocking,
                            target = QuickFixTarget.ScreenPinning,
                            priority = 210,
                            section = PreparationSection.DeviceLock,
                            fieldText = t("Start Screen Pinning", "Aktifkan Screen Pinning"),
                            filled = true,
                            onClick = onStartScreenPinning
                        )
                    } else {
                        val blockerCodes = blockingActionsBeforePinning
                            .map { it.code }
                            .distinct()
                            .take(6)
                            .joinToString(",")
                        addQuickFix(
                            code = QuickFixScreenPinningDeferredCode,
                            text = t(
                                "Finish the other required fixes first. Screen Pinning will appear as the final step.",
                                "Selesaikan perbaikan wajib lain dulu. Screen Pinning akan muncul sebagai langkah terakhir."
                            ),
                            severity = QuickFixSeverity.Warning,
                            target = QuickFixTarget.ScreenPinning,
                            priority = 210,
                            section = PreparationSection.DeviceLock,
                            enabled = false,
                            isNotice = true,
                            diagnosticDetails = "blockers=${blockingActionsBeforePinning.size} | blocker_codes=$blockerCodes"
                        )
                    }
                }
            }
            val quickFixActions = if (quickFixIssueActions.isEmpty()) {
                emptyList()
            } else {
                quickFixIssueActions + PreparationQuickFixAction(
                    code = QuickFixRefreshAllSecurityChecksCode,
                    text = if (isRefreshingGeofence || isRefreshingNetwork) {
                        t("Refreshing Checks...", "Sedang Refresh Pemeriksaan...")
                    } else {
                        t("Refresh All Security Checks", "Refresh Semua Pemeriksaan Keamanan")
                    },
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 900,
                    fieldText = if (isRefreshingGeofence || isRefreshingNetwork) {
                        t("Checking All...", "Sedang Cek Semua...")
                    } else {
                        t("Check All Again", "Cek Ulang Semua")
                    },
                    filled = false,
                    loading = isRefreshingGeofence || isRefreshingNetwork,
                    enabled = !(isRefreshingGeofence || isRefreshingNetwork),
                    onClick = {
                        onAutoFixActionOpened(QuickFixRefreshAllSecurityChecksCode)
                        onRefreshAllSecurityChecks()
                    }
                )
            }.sortedWith(
                compareBy<PreparationQuickFixAction> { action ->
                    if (action.severity == QuickFixSeverity.Blocking) 0 else 1
                }.thenBy { it.priority }
            )
            return quickFixActions
        }
    }
}
