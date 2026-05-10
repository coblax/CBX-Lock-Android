package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb

internal data class PreparationScreenState(
    val examName: String,
    val keyboardPackage: String,
    val keyboardAllowed: Boolean,
    val usingBuiltInExamKeyboard: Boolean,
    val bluetoothPermissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val accessibilityServiceEnabled: Boolean,
    val adbInspection: AdbInspection,
    val adbBypassState: AdbBypassState,
    val rootSecurityStatus: RootSecurityStatus,
    val rootBypassState: RootBypassState,
    val signatureMismatchDetected: Boolean,
    val virtualEnvironmentDetected: Boolean,
    val tamperDetected: Boolean,
    val sendingSection: DiagnosticSection?,
    val isStartingExam: Boolean,
    val pinningActivationState: PinningActivationState,
    val screenPinningMessage: String?,
    val webViewSessionResetInFlight: Boolean,
    val webViewSessionResetError: String?,
    val isRefreshingGeofence: Boolean,
    val isWarmingLocation: Boolean,
    val isRefreshingNetwork: Boolean,
    val lastGeofenceRefreshAt: String?,
    val networkReadinessStatus: NetworkReadinessStatus,
    val networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    val networkTimelinePreview: List<NetworkTimelineEntry>,
    val lastNetworkChangeAt: String?,
    val lastNetworkChangeSource: String?,
    val lastConnectedNetworkLabel: String?,
    val screenPinningAvailable: Boolean,
    val isScreenPinningActive: Boolean,
    val screenPinningFixNeeded: Boolean,
    val clipboardViolationCount: Int,
    val clipboardRuntimeStatus: ClipboardRuntimeStatus,
    val clipboardBypassState: ClipboardBypassState,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    val deviceTimeBypassState: DeviceTimeBypassState,
    val geofenceRuntimeStatus: GeofenceRuntimeStatus,
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    val overlayRiskResult: OverlayRiskResult,
    val appSwitchStatus: AppSwitchStatus,
    val reinstallApkFixNeeded: Boolean,
    val bypassScreenPinning: Boolean,
    val bypassBluetooth: Boolean,
    val bypassAccessibility: Boolean,
    val bypassAdb: Boolean,
    val bypassRoot: Boolean,
    val bypassVirtualEnvironment: Boolean,
    val bypassVpn: Boolean,
    val vpnBypassState: VpnBypassState,
    val bypassKeyboardPolicy: Boolean,
    val bypassClipboard: Boolean,
    val bypassOverlay: Boolean,
    val bypassGeofence: Boolean,
    val geofenceBypassState: GeofenceBypassState,
    val bypassFakeLocation: Boolean,
    val fakeLocationBypassState: FakeLocationBypassState,
    val bypassDeviceTime: Boolean,
    val bypassAppSwitch: Boolean,
    val screenRecorderPackages: List<String>,
    val bypassScreenRecorder: Boolean,
    val externalDisplayDetected: Boolean,
    val bypassDisplayMirror: Boolean,
    val multiWindowDetected: Boolean,
    val bypassMultiWindow: Boolean,
    val preExamHealthCheckSnapshot: PreExamHealthSnapshot,
    val deviceSurvivalPolicy: DeviceSurvivalPolicy,
    val previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb,
    val showChecklistDetails: Boolean
)

internal data class PreparationScreenActions(
    val onChooseKeyboard: () -> Unit,
    val onOpenKeyboardSettings: () -> Unit,
    val onGrantBluetoothPermission: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenOverlayAccessibilitySettings: () -> Unit,
    val onOpenDeveloperOptionsSettings: () -> Unit,
    val onRequestLocationPermission: () -> Unit,
    val onOpenLocationServicesSettings: () -> Unit,
    val onRefreshGeofenceLocation: () -> Unit,
    val onOpenGeofenceMapViewer: () -> Unit,
    val onOpenInternetSettings: () -> Unit,
    val onOpenVpnSettings: () -> Unit,
    val onOpenWifiSettings: () -> Unit,
    val onOpenCellularSettings: () -> Unit,
    val onOpenAirplaneModeSettings: () -> Unit,
    val onRefreshNetworkStatus: () -> Unit,
    val onOpenDateTimeSettings: () -> Unit,
    val onOpenFakeLocationDeveloperOptionsSettings: () -> Unit,
    val onOpenScreenPinningSettings: () -> Unit,
    val onOpenOverlaySettings: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenCastSettings: () -> Unit,
    val onOpenWebViewProviderSettings: () -> Unit,
    val onReinstallOfficialApk: () -> Unit,
    val onRefreshStatus: () -> Unit,
    val onRefreshAllSecurityChecks: () -> Unit,
    val onRefreshHealthCheck: () -> Unit,
    val onRequestSectionReport: (DiagnosticSection) -> Unit,
    val onExportDiagnostics: () -> Unit,
    val onAutoFixShown: (String) -> Unit,
    val onPreviousSessionRecoveryHintShown: (String) -> Unit,
    val onAutoFixActionOpened: (String) -> Unit,
    val onStartExam: () -> Unit,
    val onBackHome: () -> Unit
)
