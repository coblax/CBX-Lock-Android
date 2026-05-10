package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayQuickFixTarget
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.NetworkReadinessVerdict

@Composable
internal fun buildPreparationQuickFixActions(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean,
    geofenceReady: Boolean,
    fakeLocationReady: Boolean,
    needsBluetoothPermission: Boolean,
    runQuickFix: (QuickFixTarget?, String, () -> Unit) -> Unit
): List<PreparationQuickFixAction> {
    with(state) {
        with(actions) {
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
                    NetworkReadinessVerdict.AirplaneMode,
                    NetworkReadinessVerdict.Unstable
                )
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
                        text = tr("Enable Automatic Date & Time", "Aktifkan Tanggal & Waktu Otomatis"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.DeviceTime,
                        priority = 10,
                        filled = true,
                        onClick = onOpenDateTimeSettings
                    )
                }
                if (reinstallApkFixNeeded) {
                    addQuickFix(
                        text = tr("Install Official APK Again", "Instal Ulang APK Resmi"),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 15,
                        filled = true,
                        onClick = onReinstallOfficialApk
                    )
                }
                if (showAdbFix) {
                    addQuickFix(
                        text = tr("Turn Off USB Debugging", "Matikan USB Debugging"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 20,
                        onClick = onOpenDeveloperOptionsSettings
                    )
                }
                if (showAdbInsecurePropertyFix) {
                    addQuickFix(
                        code = "adb_insecure_property",
                        text = tr(
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
                        text = tr(
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
                        text = tr(
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
                        text = tr(
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
                        text = tr(
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
                        text = tr("Turn Off Mock Location App", "Matikan Aplikasi Lokasi Palsu"),
                        severity = if (fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 30,
                        onClick = onOpenFakeLocationDeveloperOptionsSettings
                    )
                }
                if (showAccessibilityFix) {
                    addQuickFix(
                        text = tr("Disable Risky Accessibility Services", "Nonaktifkan Layanan Aksesibilitas Berisiko"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 35,
                        onClick = onOpenAccessibilitySettings
                    )
                }
                if (showAccessibilityGuardFix) {
                    addQuickFix(
                        text = tr("Enable CBX Lock Exam Guard", "Aktifkan CBX Lock Exam Guard"),
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
                            tr("Allow Precise Location", "Izinkan Lokasi Presisi")
                        } else {
                            tr("Allow Location Permission", "Izinkan Akses Lokasi")
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
                        text = tr("Turn On Location Services", "Aktifkan Layanan Lokasi"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.Location,
                        priority = 45,
                        onClick = onOpenLocationServicesSettings
                    )
                }
                if (showLocationRefreshFix) {
                    addQuickFix(
                        text = if (isRefreshingGeofence) {
                            tr("Refreshing Location...", "Sedang Refresh Lokasi...")
                        } else {
                            tr("Refresh Location Now", "Refresh Lokasi Sekarang")
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
                        text = tr("Open Geofence Map", "Buka Peta Geofence"),
                        severity = if (geofenceReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                        target = null,
                        priority = 55,
                        onClick = onOpenGeofenceMapViewer
                    )
                }
                if (showBluetoothPermissionFix) {
                    addQuickFix(
                        text = tr("Allow Bluetooth Access", "Izinkan Akses Bluetooth"),
                        severity = QuickFixSeverity.Blocking,
                        target = null,
                        priority = 60,
                        filled = true,
                        onClick = onGrantBluetoothPermission
                    )
                }
                if (showBluetoothFix) {
                    addQuickFix(
                        text = tr("Turn Off Bluetooth", "Matikan Bluetooth"),
                        severity = QuickFixSeverity.Blocking,
                        target = QuickFixTarget.All,
                        priority = 65,
                        onClick = onOpenBluetoothSettings
                    )
                }

                val networkPrimaryIsRefresh = networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable
                if (showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = tr("Turn Off Airplane Mode", "Matikan Mode Pesawat"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        onClick = onOpenAirplaneModeSettings
                    )
                } else if (networkPrimaryIsRefresh && showNetworkRefreshFix) {
                    addQuickFix(
                        text = if (isRefreshingNetwork) {
                            tr("Refreshing Network...", "Sedang Refresh Network...")
                        } else {
                            tr("Refresh Network Status", "Refresh Status Network")
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
                        text = tr("Open Internet Settings", "Buka Pengaturan Internet"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 70,
                        onClick = onOpenInternetSettings
                    )
                }
                if (showNetworkWifiSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = tr("Open Wi-Fi Settings", "Buka Pengaturan Wi-Fi"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 75,
                        onClick = onOpenWifiSettings
                    )
                }
                if (showNetworkCellularSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                    addQuickFix(
                        text = tr("Open Cellular Settings", "Buka Pengaturan Seluler"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.Network,
                        priority = 76,
                        onClick = onOpenCellularSettings
                    )
                }
                if (showNetworkRefreshFix && !networkPrimaryIsRefresh) {
                    addQuickFix(
                        text = if (isRefreshingNetwork) {
                            tr("Refreshing Network...", "Sedang Refresh Network...")
                        } else {
                            tr("Refresh Network Status", "Refresh Status Network")
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
                        text = if (webViewHealthItem.verdict == PreExamHealthVerdict.Blocking) {
                            tr("Enable Android WebView", "Aktifkan Android WebView")
                        } else {
                            tr("Check WebView / Chrome", "Cek WebView / Chrome")
                        },
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
                        text = tr("Choose System Keyboard", "Pilih Keyboard Sistem"),
                        severity = QuickFixSeverity.Warning,
                        target = null,
                        priority = 200,
                        filled = true,
                        onClick = onChooseKeyboard
                    )
                    addQuickFix(
                        text = tr("Open Keyboard Settings", "Buka Pengaturan Keyboard"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 205,
                        onClick = onOpenKeyboardSettings
                    )
                }
                if (screenPinningFixNeeded) {
                    addQuickFix(
                        text = tr("Enable Screen Pinning", "Aktifkan Screen Pinning"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.ScreenPinning,
                        priority = 210,
                        onClick = onOpenScreenPinningSettings
                    )
                }
                if (showOverlayAccessibilityFix) {
                    addQuickFix(
                        text = tr("Review Accessibility for Overlay Risk", "Tinjau Aksesibilitas untuk Risiko Overlay"),
                        severity = QuickFixSeverity.Warning,
                        target = QuickFixTarget.All,
                        priority = 220,
                        onClick = onOpenOverlayAccessibilitySettings
                    )
                }
                if (showOverlaySettingsFix) {
                    addQuickFix(
                        text = tr("Open Overlay Settings", "Buka Izin Overlay"),
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
                        tr("Refreshing Checks...", "Sedang Refresh Pemeriksaan...")
                    } else {
                        tr("Refresh All Security Checks", "Refresh Semua Pemeriksaan Keamanan")
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
