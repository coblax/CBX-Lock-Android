package com.example.coblaxexamlock.viewmodel

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import kotlinx.coroutines.flow.collectLatest


@Composable
internal fun rememberBoundExamRuntimeViewModel(
    activity: ComponentActivity,
    bypassKeyboardPolicy: Boolean,
    isKeyboardAllowed: Boolean,
    useBuiltInExamKeyboard: Boolean,
    bypassBluetooth: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothPermissionGranted: Boolean,
    bypassAccessibility: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAdb: Boolean,
    adbInspection: AdbInspection,
    bypassRoot: Boolean,
    rootSecurityStatus: RootSecurityStatus,
    bypassVirtualEnvironment: Boolean,
    virtualEnvironmentDetected: Boolean,
    bypassVpn: Boolean,
    bypassGeofence: Boolean,
    geofenceBypassState: GeofenceBypassState,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    bypassFakeLocation: Boolean,
    fakeLocationBypassState: FakeLocationBypassState,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bypassDeviceTime: Boolean,
    deviceTimeBypassState: DeviceTimeBypassState,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    bypassOverlay: Boolean,
    overlayRiskResult: OverlayRiskResult,
    bypassAppSwitch: Boolean,
    appSwitchStatus: AppSwitchStatus,
    signatureMismatchDetected: Boolean,
    securityTamperDetected: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    examSessionStarted: Boolean,
    loadingProgress: Float,
    webViewErrorMessage: String?,
    hasFullscreenCustomView: Boolean,
    builtInKeyboardVisible: Boolean,
    hasEditableFocus: Boolean,
    pendingSection: DiagnosticSection?,
    showForcedExitAlarm: Boolean,
    showOfflineWarningDialog: Boolean,
    showNetworkUnstableDialog: Boolean,
    showGeofenceViolationDialog: Boolean,
    showFakeLocationViolationDialog: Boolean,
    showKeyboardViolationDialog: Boolean,
    showOverlayViolationDialog: Boolean,
    showBluetoothViolationDialog: Boolean,
    showClipboardViolationDialog: Boolean,
    showExitExamDialog: Boolean,
    onRefreshAllSecurityChecks: () -> Unit,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenInternetSettings: () -> Unit
): ExamRuntimeViewModel {
    val examRuntimeViewModel = remember(activity) {
        ViewModelProvider(activity)[ExamRuntimeViewModel::class.java]
    }
    val checklistUiSnapshot = buildPreparationChecklistUiState(
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        isKeyboardAllowed = isKeyboardAllowed,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        bypassBluetooth = bypassBluetooth,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        bypassAdb = bypassAdb,
        adbInspection = adbInspection,
        bypassRoot = bypassRoot,
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassVpn = bypassVpn,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeBypassState = deviceTimeBypassState,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        signatureMismatchDetected = signatureMismatchDetected,
        securityTamperDetected = securityTamperDetected,
        networkReadinessStatus = networkReadinessStatus
    )

    LaunchedEffect(examRuntimeViewModel) {
        examRuntimeViewModel.effects.collectLatest { effect ->
            when (effect) {
                ExamRuntimeUiEffect.RefreshSystemChecks -> onRefreshAllSecurityChecks()
                ExamRuntimeUiEffect.RequestLocationPermission -> onRequestLocationPermission()
                ExamRuntimeUiEffect.OpenInternetSettings -> onOpenInternetSettings()
                is ExamRuntimeUiEffect.RequestSectionReport -> onRequestSectionReport(effect.section)
            }
        }
    }

    val chromeUiSnapshot = ExamRuntimeChromeUiState(
        loadingProgress = loadingProgress,
        hasWebViewError = !webViewErrorMessage.isNullOrBlank(),
        hasFullscreenCustomView = hasFullscreenCustomView,
        builtInKeyboardVisible = builtInKeyboardVisible,
        hasEditableFocus = hasEditableFocus
    )
    val dialogsUiSnapshot = ExamRuntimeDialogsUiState(
        pendingDiagnosticSection = pendingSection,
        showForcedExitAlarm = showForcedExitAlarm,
        showOfflineWarning = showOfflineWarningDialog,
        showNetworkUnstableWarning = showNetworkUnstableDialog,
        showGeofenceWarning = showGeofenceViolationDialog,
        showFakeLocationWarning = showFakeLocationViolationDialog,
        showKeyboardWarning = showKeyboardViolationDialog,
        showOverlayWarning = showOverlayViolationDialog,
        showBluetoothWarning = showBluetoothViolationDialog,
        showClipboardWarning = showClipboardViolationDialog,
        showExitConfirmation = showExitExamDialog
    )

    LaunchedEffect(
        checklistUiSnapshot,
        chromeUiSnapshot,
        dialogsUiSnapshot,
        examSessionStarted,
        showOfflineWarningDialog,
        showNetworkUnstableDialog,
        showGeofenceViolationDialog,
        showFakeLocationViolationDialog
    ) {
        val currentState = examRuntimeViewModel.uiState.value
        if (currentState.checklist != checklistUiSnapshot) {
            examRuntimeViewModel.dispatch(ExamRuntimeUiAction.UpdateChecklist(checklistUiSnapshot))
        }
        if (currentState.chrome != chromeUiSnapshot) {
            examRuntimeViewModel.dispatch(ExamRuntimeUiAction.UpdateChrome(chromeUiSnapshot))
        }
        if (currentState.dialogs != dialogsUiSnapshot) {
            examRuntimeViewModel.dispatch(ExamRuntimeUiAction.UpdateDialogs(dialogsUiSnapshot))
        }
        if (examSessionStarted != currentState.examStarted) {
            examRuntimeViewModel.dispatch(
                if (examSessionStarted) ExamRuntimeUiAction.StartExamRequested
                else ExamRuntimeUiAction.EndExamRequested
            )
        }
        if (showOfflineWarningDialog != currentState.showOfflineWarning) {
            examRuntimeViewModel.dispatch(
                if (showOfflineWarningDialog) ExamRuntimeUiAction.ShowOfflineWarning
                else ExamRuntimeUiAction.HideOfflineWarning
            )
        }
        if (showNetworkUnstableDialog != currentState.showNetworkUnstableWarning) {
            examRuntimeViewModel.dispatch(
                if (showNetworkUnstableDialog) ExamRuntimeUiAction.ShowNetworkUnstableWarning
                else ExamRuntimeUiAction.HideNetworkUnstableWarning
            )
        }
        if (showGeofenceViolationDialog != currentState.showGeofenceWarning) {
            examRuntimeViewModel.dispatch(
                if (showGeofenceViolationDialog) ExamRuntimeUiAction.ShowGeofenceWarning
                else ExamRuntimeUiAction.HideGeofenceWarning
            )
        }
        if (showFakeLocationViolationDialog != currentState.showFakeLocationWarning) {
            examRuntimeViewModel.dispatch(
                if (showFakeLocationViolationDialog) ExamRuntimeUiAction.ShowFakeLocationWarning
                else ExamRuntimeUiAction.HideFakeLocationWarning
            )
        }
    }

    return examRuntimeViewModel
}

@Composable
private fun buildPreparationChecklistUiState(
    bypassKeyboardPolicy: Boolean,
    isKeyboardAllowed: Boolean,
    useBuiltInExamKeyboard: Boolean,
    bypassBluetooth: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothPermissionGranted: Boolean,
    bypassAccessibility: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAdb: Boolean,
    adbInspection: AdbInspection,
    bypassRoot: Boolean,
    rootSecurityStatus: RootSecurityStatus,
    bypassVirtualEnvironment: Boolean,
    virtualEnvironmentDetected: Boolean,
    bypassVpn: Boolean,
    bypassGeofence: Boolean,
    geofenceBypassState: GeofenceBypassState,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    bypassFakeLocation: Boolean,
    fakeLocationBypassState: FakeLocationBypassState,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bypassDeviceTime: Boolean,
    deviceTimeBypassState: DeviceTimeBypassState,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    bypassOverlay: Boolean,
    overlayRiskResult: OverlayRiskResult,
    bypassAppSwitch: Boolean,
    appSwitchStatus: AppSwitchStatus,
    signatureMismatchDetected: Boolean,
    securityTamperDetected: Boolean,
    networkReadinessStatus: NetworkReadinessStatus
): PreparationChecklistUiState {
    val bluetoothPermissionReady = bluetoothPermissionGranted || !requiresBluetoothExamPermission()
    val keyboardReady = bypassKeyboardPolicy || isKeyboardAllowed || useBuiltInExamKeyboard
    val bluetoothReady = bypassBluetooth || (!bluetoothEnabled && bluetoothPermissionReady)
    val accessibilityReady = bypassAccessibility || !accessibilityServiceEnabled
    val adbReady = bypassAdb || !adbInspection.blocking
    val rootReady = bypassRoot || !rootSecurityStatus.blocking
    val virtualEnvironmentReady = bypassVirtualEnvironment || !virtualEnvironmentDetected
    val vpnReady = bypassVpn || !networkReadinessStatus.diagnostics.isVpnActive
    val geofenceReady =
        bypassGeofence ||
            !geofenceRuntimeStatus.evaluation.enabled ||
            !geofenceRuntimeStatus.securityStatus.blocking
    val fakeLocationReady =
        bypassFakeLocation ||
            !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled ||
            !fakeLocationRuntimeStatus.securityStatus.blocking
    val deviceTimeReady = bypassDeviceTime || !deviceTimeSecurityStatus.blocking
    val overlayReady = bypassOverlay || !overlayRiskResult.hasAnyRisk
    val appSwitchReady = bypassAppSwitch || !appSwitchStatus.hasViolations
    val signatureReady = !signatureMismatchDetected
    val canStartExam =
        bluetoothReady &&
            accessibilityReady &&
            adbReady &&
            rootReady &&
            deviceTimeReady &&
            geofenceReady &&
            fakeLocationReady &&
            virtualEnvironmentReady &&
            vpnReady &&
            signatureReady &&
            overlayReady &&
            keyboardReady &&
            appSwitchReady &&
            !securityTamperDetected

    val networkLabel = when (networkReadinessStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> tr("Stable", "Stabil")
        NetworkReadinessVerdict.Offline -> tr("Offline", "Offline")
        NetworkReadinessVerdict.Unvalidated -> tr("Unvalidated", "Belum Tervalidasi")
        NetworkReadinessVerdict.CaptivePortal -> tr("Captive Portal", "Captive Portal")
        NetworkReadinessVerdict.VpnActive -> tr("VPN Active", "VPN Aktif")
        NetworkReadinessVerdict.AirplaneMode -> tr("Airplane Mode", "Mode Pesawat")
        NetworkReadinessVerdict.Unstable -> tr("Unstable", "Tidak Stabil")
    }
    val geofenceLabel = when {
        geofenceBypassState == GeofenceBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassGeofence -> tr("Bypassed", "Bypass")
        !geofenceRuntimeStatus.evaluation.enabled -> tr("Policy Off", "Policy Nonaktif")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Inside ->
            tr("Inside Area", "Di Dalam Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside ->
            tr("Outside Area", "Di Luar Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix ->
            tr("Stale Fix", "Fix Kedaluwarsa")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy ->
            tr("Low Accuracy", "Akurasi Rendah")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy ->
            tr("Missing Accuracy", "Akurasi Tidak Ada")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix ->
            tr("No Fix", "Belum Ada Fix")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid ->
            tr("Config Error", "Konfigurasi Salah")
        else -> tr("Needs Fix", "Perlu Perbaikan")
    }
    val fakeLocationLabel = when {
        fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassFakeLocation -> tr("Bypassed", "Bypass")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired ->
            tr("Needs Location Permission", "Butuh Izin Lokasi")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            tr("Location Services Off", "Layanan Lokasi Off")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable ->
            tr("Waiting for Location", "Menunggu Lokasi")
        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr("Policy Off", "Policy Nonaktif")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical ->
            tr("Spoof Critical", "Spoof Kritis")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong ->
            tr("Spoof Strong", "Spoof Kuat")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PackageWarning ->
            tr("Package Warning", "Peringatan Paket")
        else -> tr("Clean", "Bersih")
    }
    val deviceTimeLabel = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassDeviceTime -> tr("Bypassed", "Bypass")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr("Safe", "Aman")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled ->
            tr("Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled ->
            tr("Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
        else -> tr("Clock Change", "Perubahan Jam")
    }

    return PreparationChecklistUiState(
        hasWarnings =
            !canStartExam ||
                networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable ||
                fakeLocationRuntimeStatus.securityStatus.warningOnly,
        canStartExam = canStartExam,
        networkLabel = networkLabel,
        geofenceLabel = geofenceLabel,
        fakeLocationLabel = fakeLocationLabel,
        deviceTimeLabel = deviceTimeLabel
    )
}
