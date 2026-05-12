package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayQuickFixTarget
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
    runQuickFix: (QuickFixTarget?, String, () -> Unit) -> Unit
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
            val showOverlaySettingsFix =
                !bypassOverlay &&
                    overlayRiskResult.quickFixTargets.contains(OverlayQuickFixTarget.OverlaySettings)
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
                    severity: QuickFixSeverity,
                    target: QuickFixTarget?,
                    priority: Int,
                    filled: Boolean = false,
                    loading: Boolean = false,
                    enabled: Boolean = true,
                    onClick: () -> Unit
                ) {
                    val actionCode = code.ifBlank { "quick_fix_$priority" }
                    add(
                        PreparationQuickFixAction(
                            code = actionCode,
                            text = text,
                            severity = severity,
                            target = target,
                            priority = priority,
                            filled = filled,
                            loading = loading,
                            enabled = enabled,
                            onClick = { runQuickFix(target, actionCode, onClick) }
                        )
                    )
                }

                if (showDeviceTimeFix) {
                    addQuickFix(
                        text = t("Enable Automatic Date & Time", "Aktifkan Tanggal & Waktu Otomatis"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.DeviceTime,
                        priority = 10,
                        filled = true,
                        onClick = onOpenDateTimeSettings
                    )
                }
                if (reinstallApkFixNeeded) {
                    addQuickFix(
                        text = t("Install Official APK Again", "Instal Ulang APK Resmi"),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 15,
                        filled = true,
                        onClick = onReinstallOfficialApk
                    )
                }
                if (showAdbFix) {
                    addQuickFix(
                        text = t("Turn Off USB Debugging", "Matikan USB Debugging"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 20,
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
                        onClick = onRefreshAllSecurityChecks
                    )
                }
                if (showFakeLocationDeveloperOptionsFix) {
                    addQuickFix(
                        text = t("Turn Off Mock Location App", "Matikan Aplikasi Lokasi Palsu"),
                        severity = if (fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 30,
                        onClick = onOpenFakeLocationDeveloperOptionsSettings
                    )
                }
                if (showAccessibilityFix) {
                    addQuickFix(
                        text = t("Disable Risky Accessibility Services", "Nonaktifkan Layanan Aksesibilitas Berisiko"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 35,
                        onClick = onOpenAccessibilitySettings
                    )
                }
                if (showAccessibilityGuardFix) {
                    addQuickFix(
                        text = t("Enable CBX Lock Exam Guard", "Aktifkan CBX Lock Exam Guard"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 36,
                        filled = true,
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
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 40,
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
                        target = null,
                        priority = 50,
                        loading = isRefreshingGeofence,
                        enabled = !isRefreshingGeofence,
                        onClick = onRefreshGeofenceLocation
                    )
                }
                if (showGeofenceMapFix) {
                    addQuickFix(
                        text = t("Open Geofence Map", "Buka Peta Geofence"),
                        severity = if (geofenceReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = null,
                        priority = 55,
                        onClick = onOpenGeofenceMapViewer
                    )
                }
                if (showBluetoothPermissionFix) {
                    addQuickFix(
                        text = t("Allow Bluetooth Access", "Izinkan Akses Bluetooth"),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 60,
                        filled = true,
                        onClick = onGrantBluetoothPermission
                    )
                }
                if (showBluetoothFix) {
                    addQuickFix(
                        text = t("Turn Off Bluetooth", "Matikan Bluetooth"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 65,
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
                        onClick = onOpenAirplaneModeSettings
                    )
                } else if (showNetworkVpnSettingsFix) {
                    addQuickFix(
                        code = "vpn_settings_opened",
                        text = t("Open VPN Settings", "Buka Setelan VPN"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        filled = true,
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
                        target = null,
                        priority = 70,
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
                        onClick = onOpenInternetSettings
                    )
                }
                if (showNetworkWifiSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = t("Open Wi-Fi Settings", "Buka Setelan Wi-Fi"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 75,
                        onClick = onOpenWifiSettings
                    )
                }
                if (showNetworkCellularSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = t("Open Cellular Settings", "Buka Setelan Seluler"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 76,
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
                        target = null,
                        priority = 80,
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
                        filled = webViewHealthItem.verdict == PreExamHealthVerdict.Blocking,
                        onClick = onOpenWebViewProviderSettings
                    )
                }

                if (showKeyboardFix) {
                    addQuickFix(
                        text = t("Choose System Keyboard", "Pilih Keyboard Sistem"),
                        severity = QuickFixSeverity.Warning,
                        target = null,
                        priority = 200,
                        filled = true,
                        onClick = onChooseKeyboard
                    )
                    addQuickFix(
                        text = t("Open Keyboard Settings", "Buka Pengaturan Keyboard"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 205,
                        onClick = onOpenKeyboardSettings
                    )
                }
                if (!bypassScreenPinning && screenPinningAvailable && !isScreenPinningActive) {
                    addQuickFix(
                        code = "start_screen_pinning",
                        text = t("Start Screen Pinning", "Start Screen Pinning"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.ScreenPinning,
                        priority = 210,
                        filled = true,
                        onClick = onStartScreenPinning
                    )
                }
                if (screenRecorderPackages.isNotEmpty() && !bypassScreenRecorder) {
                    addQuickFix(
                        text = t("Open App Settings", "Buka Setelan App"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.ScreenRecorder,
                        priority = 50,
                        onClick = onOpenAppSettings
                    )
                }
                if (externalDisplayDetected && !bypassDisplayMirror) {
                    addQuickFix(
                        text = t("Open Cast Settings", "Buka Setelan Cast"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.DisplayMirror,
                        priority = 45,
                        onClick = onOpenCastSettings
                    )
                }
                if (multiWindowDetected && !bypassMultiWindow) {
                    addQuickFix(
                        text = t("Refresh Status", "Refresh Status"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.MultiWindow,
                        priority = 40,
                        onClick = onRefreshStatus
                    )
                }
                if (showOverlayAccessibilityFix) {
                    addQuickFix(
                        text = t("Review Accessibility for Overlay Risk", "Tinjau Aksesibilitas untuk Risiko Overlay"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 220,
                        onClick = onOpenOverlayAccessibilitySettings
                    )
                }
                if (showOverlaySettingsFix) {
                    addQuickFix(
                        text = t("Open Overlay Settings", "Buka Izin Overlay"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 225,
                        onClick = onOpenOverlaySettings
                    )
                }
            }
            val quickFixActions = if (quickFixIssueActions.isEmpty()) {
                emptyList()
            } else {
                quickFixIssueActions + PreparationQuickFixAction(
                    code = "refresh_all_security_checks",
                    text = if (isRefreshingGeofence || isRefreshingNetwork) {
                        t("Refreshing Checks...", "Sedang Refresh Pemeriksaan...")
                    } else {
                        t("Refresh All Security Checks", "Refresh Semua Pemeriksaan Keamanan")
                    },
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 900,
                    filled = false,
                    loading = isRefreshingGeofence || isRefreshingNetwork,
                    enabled = !(isRefreshingGeofence || isRefreshingNetwork),
                    onClick = {
                        onAutoFixActionOpened("refresh_all_security_checks")
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
