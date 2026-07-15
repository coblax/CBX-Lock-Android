package com.coblax.examlock.ui.preparation

import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AdbInspection
import com.coblax.examlock.AppSwitchStatus
import com.coblax.examlock.ClipboardBypassState
import com.coblax.examlock.ClipboardRuntimeStatus
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DpcRuntimeStatus
import com.coblax.examlock.FakeLocationBypassState
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.GeofenceBypassState
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.PinningActivationState
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.RootSecurityStatus
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkTimelineEntry
import com.coblax.examlock.model.NetworkUnstableRuntimeStatus
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.PreviousExamSessionBreadcrumb
import com.coblax.examlock.defaultDpcRuntimeStatus
import com.coblax.examlock.runtime.ExternalDisplayInfo
import com.coblax.examlock.runtime.MultiWindowModeInfo
import com.coblax.examlock.runtime.OverlayAppInfo

internal data class PreparationSessionState(
    val examName: String,
    val sendingSection: DiagnosticSection?,
    val isStartingExam: Boolean,
    val pinningActivationState: PinningActivationState,
    val screenPinningMessage: String?,
    val webViewSessionResetInFlight: Boolean,
    val webViewSessionResetError: String?,
    val showChecklistDetails: Boolean
)

internal data class PreparationNetworkState(
    val networkReadinessStatus: NetworkReadinessStatus,
    val networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    val networkTimelinePreview: List<NetworkTimelineEntry>,
    val lastNetworkChangeAt: String?,
    val lastNetworkChangeSource: String?,
    val lastConnectedNetworkLabel: String?,
    val isRefreshingNetwork: Boolean,
    val bypassVpn: Boolean,
    val vpnBypassState: VpnBypassState
)

internal data class PreparationDeviceState(
    val keyboardPackage: String,
    val keyboardAllowed: Boolean,
    val usingBuiltInExamKeyboard: Boolean,
    val bluetoothPermissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val adbInspection: AdbInspection,
    val adbBypassState: AdbBypassState,
    val rootSecurityStatus: RootSecurityStatus,
    val rootBypassState: RootBypassState,
    val signatureMismatchDetected: Boolean,
    val virtualEnvironmentDetected: Boolean,
    val screenPinningAvailable: Boolean,
    val isScreenPinningActive: Boolean,
    val screenPinningFixNeeded: Boolean,
    val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    val deviceTimeBypassState: DeviceTimeBypassState,
    val reinstallApkFixNeeded: Boolean
)

internal data class PreparationLocationState(
    val geofenceRuntimeStatus: GeofenceRuntimeStatus,
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    val isRefreshingGeofence: Boolean,
    val isWarmingLocation: Boolean,
    val lastGeofenceRefreshAt: String?,
    val geofenceBypassState: GeofenceBypassState,
    val fakeLocationBypassState: FakeLocationBypassState
)

internal data class PreparationRuntimeSecurityState(
    val accessibilityServiceEnabled: Boolean,
    val overlayRiskResult: OverlayRiskResult,
    val appSwitchStatus: AppSwitchStatus,
    val clipboardViolationCount: Int,
    val clipboardRuntimeStatus: ClipboardRuntimeStatus,
    val clipboardBypassState: ClipboardBypassState,
    val screenRecorderPackages: List<String>,
    val externalDisplayDetected: Boolean,
    val externalDisplayCount: Int,
    val externalDisplayInfoList: List<ExternalDisplayInfo>,
    val multiWindowDetected: Boolean,
    val multiWindowModeInfo: MultiWindowModeInfo,
    val staticSecurityInitialScanComplete: Boolean,
    val tamperDetected: Boolean,
    val reverseEngineeringDetected: Boolean,
    val reverseEngineeringSummary: String,
    val reverseEngineeringBypassActive: Boolean,
    val integrityDetected: Boolean,
    val integritySummary: String,
    val integrityBypassActive: Boolean,
    val dpcRuntimeStatus: DpcRuntimeStatus = defaultDpcRuntimeStatus(),
    val overlayAppsDetected: List<OverlayAppInfo> = emptyList(),
    val overlayGuardPermissionGranted: Boolean = false
)

internal data class PreparationBypassState(
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
    val bypassScreenRecorder: Boolean,
    val bypassDisplayMirror: Boolean,
    val bypassMultiWindow: Boolean,
    val bypassReverseEngineering: Boolean,
    val bypassApkIntegrity: Boolean
)

internal data class PreparationDiagnosticsState(
    val preExamHealthCheckSnapshot: PreExamHealthSnapshot,
    val deviceSurvivalPolicy: DeviceSurvivalPolicy,
    val previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb
)

internal data class PreparationScreenState(
    val session: PreparationSessionState,
    val network: PreparationNetworkState,
    val device: PreparationDeviceState,
    val location: PreparationLocationState,
    val runtimeSecurity: PreparationRuntimeSecurityState,
    val bypass: PreparationBypassState,
    val diagnostics: PreparationDiagnosticsState
) {
    val examName: String get() = session.examName
    val keyboardPackage: String get() = device.keyboardPackage
    val keyboardAllowed: Boolean get() = device.keyboardAllowed
    val usingBuiltInExamKeyboard: Boolean get() = device.usingBuiltInExamKeyboard
    val bluetoothPermissionGranted: Boolean get() = device.bluetoothPermissionGranted
    val bluetoothEnabled: Boolean get() = device.bluetoothEnabled
    val accessibilityServiceEnabled: Boolean get() = runtimeSecurity.accessibilityServiceEnabled
    val adbInspection: AdbInspection get() = device.adbInspection
    val adbBypassState: AdbBypassState get() = device.adbBypassState
    val rootSecurityStatus: RootSecurityStatus get() = device.rootSecurityStatus
    val rootBypassState: RootBypassState get() = device.rootBypassState
    val signatureMismatchDetected: Boolean get() = device.signatureMismatchDetected
    val virtualEnvironmentDetected: Boolean get() = device.virtualEnvironmentDetected
    val tamperDetected: Boolean get() = runtimeSecurity.tamperDetected
    val sendingSection: DiagnosticSection? get() = session.sendingSection
    val isStartingExam: Boolean get() = session.isStartingExam
    val pinningActivationState: PinningActivationState get() = session.pinningActivationState
    val screenPinningMessage: String? get() = session.screenPinningMessage
    val webViewSessionResetInFlight: Boolean get() = session.webViewSessionResetInFlight
    val webViewSessionResetError: String? get() = session.webViewSessionResetError
    val isRefreshingGeofence: Boolean get() = location.isRefreshingGeofence
    val isWarmingLocation: Boolean get() = location.isWarmingLocation
    val isRefreshingNetwork: Boolean get() = network.isRefreshingNetwork
    val lastGeofenceRefreshAt: String? get() = location.lastGeofenceRefreshAt
    val networkReadinessStatus: NetworkReadinessStatus get() = network.networkReadinessStatus
    val networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus get() = network.networkUnstableRuntimeStatus
    val networkTimelinePreview: List<NetworkTimelineEntry> get() = network.networkTimelinePreview
    val lastNetworkChangeAt: String? get() = network.lastNetworkChangeAt
    val lastNetworkChangeSource: String? get() = network.lastNetworkChangeSource
    val lastConnectedNetworkLabel: String? get() = network.lastConnectedNetworkLabel
    val screenPinningAvailable: Boolean get() = device.screenPinningAvailable
    val isScreenPinningActive: Boolean get() = device.isScreenPinningActive
    val screenPinningFixNeeded: Boolean get() = device.screenPinningFixNeeded
    val clipboardViolationCount: Int get() = runtimeSecurity.clipboardViolationCount
    val clipboardRuntimeStatus: ClipboardRuntimeStatus get() = runtimeSecurity.clipboardRuntimeStatus
    val clipboardBypassState: ClipboardBypassState get() = runtimeSecurity.clipboardBypassState
    val webViewCompatibilityStatus: WebViewCompatibilityStatus get() = device.webViewCompatibilityStatus
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus get() = device.deviceTimeSecurityStatus
    val deviceTimeBypassState: DeviceTimeBypassState get() = device.deviceTimeBypassState
    val geofenceRuntimeStatus: GeofenceRuntimeStatus get() = location.geofenceRuntimeStatus
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus get() = location.fakeLocationRuntimeStatus
    val overlayRiskResult: OverlayRiskResult get() = runtimeSecurity.overlayRiskResult
    val appSwitchStatus: AppSwitchStatus get() = runtimeSecurity.appSwitchStatus
    val reinstallApkFixNeeded: Boolean get() = device.reinstallApkFixNeeded
    val bypassScreenPinning: Boolean get() = bypass.bypassScreenPinning
    val bypassBluetooth: Boolean get() = bypass.bypassBluetooth
    val bypassAccessibility: Boolean get() = bypass.bypassAccessibility
    val bypassAdb: Boolean get() = bypass.bypassAdb
    val bypassRoot: Boolean get() = bypass.bypassRoot
    val bypassVirtualEnvironment: Boolean get() = bypass.bypassVirtualEnvironment
    val bypassVpn: Boolean get() = network.bypassVpn
    val vpnBypassState: VpnBypassState get() = network.vpnBypassState
    val bypassKeyboardPolicy: Boolean get() = bypass.bypassKeyboardPolicy
    val bypassClipboard: Boolean get() = bypass.bypassClipboard
    val bypassOverlay: Boolean get() = bypass.bypassOverlay
    val bypassGeofence: Boolean get() = bypass.bypassGeofence
    val geofenceBypassState: GeofenceBypassState get() = location.geofenceBypassState
    val bypassFakeLocation: Boolean get() = bypass.bypassFakeLocation
    val fakeLocationBypassState: FakeLocationBypassState get() = location.fakeLocationBypassState
    val bypassDeviceTime: Boolean get() = bypass.bypassDeviceTime
    val bypassAppSwitch: Boolean get() = bypass.bypassAppSwitch
    val screenRecorderPackages: List<String> get() = runtimeSecurity.screenRecorderPackages
    val bypassScreenRecorder: Boolean get() = bypass.bypassScreenRecorder
    val externalDisplayDetected: Boolean get() = runtimeSecurity.externalDisplayDetected
    val externalDisplayCount: Int get() = runtimeSecurity.externalDisplayCount
    val externalDisplayInfoList: List<ExternalDisplayInfo> get() = runtimeSecurity.externalDisplayInfoList
    val bypassDisplayMirror: Boolean get() = bypass.bypassDisplayMirror
    val multiWindowDetected: Boolean get() = runtimeSecurity.multiWindowDetected
    val multiWindowModeInfo: MultiWindowModeInfo get() = runtimeSecurity.multiWindowModeInfo
    val bypassMultiWindow: Boolean get() = bypass.bypassMultiWindow
    val staticSecurityInitialScanComplete: Boolean get() = runtimeSecurity.staticSecurityInitialScanComplete
    val overlayAppsDetected: List<OverlayAppInfo> get() = runtimeSecurity.overlayAppsDetected
    val overlayGuardPermissionGranted: Boolean get() = runtimeSecurity.overlayGuardPermissionGranted
    val preExamHealthCheckSnapshot: PreExamHealthSnapshot get() = diagnostics.preExamHealthCheckSnapshot
    val deviceSurvivalPolicy: DeviceSurvivalPolicy get() = diagnostics.deviceSurvivalPolicy
    val previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb get() = diagnostics.previousExamSessionBreadcrumb
    val showChecklistDetails: Boolean get() = session.showChecklistDetails
}

internal data class PreparationSessionActions(
    val onRefreshStatus: () -> Unit,
    val onRefreshAllSecurityChecks: () -> Unit,
    val onRefreshHealthCheck: () -> Unit,
    val onRequestSectionReport: (DiagnosticSection) -> Unit,
    val onExportDiagnostics: () -> Unit,
    val onAutoFixShown: (String) -> Unit,
    val onPreviousSessionRecoveryHintShown: (String) -> Unit,
    val onAutoFixActionOpened: (String) -> Unit,
    val onScreenPinningDeferred: (String) -> Unit,
    val onStartExam: () -> Unit,
    val onBackHome: () -> Unit
)

internal data class PreparationNetworkActions(
    val onOpenInternetSettings: () -> Unit,
    val onOpenVpnSettings: () -> Unit,
    val onOpenWifiSettings: () -> Unit,
    val onOpenCellularSettings: () -> Unit,
    val onOpenAirplaneModeSettings: () -> Unit,
    val onRefreshNetworkStatus: () -> Unit
)

internal data class PreparationDeviceActions(
    val onChooseKeyboard: () -> Unit,
    val onOpenKeyboardSettings: () -> Unit,
    val onGrantBluetoothPermission: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenOverlayAccessibilitySettings: () -> Unit,
    val onOpenDeveloperOptionsSettings: () -> Unit,
    val onOpenDateTimeSettings: () -> Unit,
    val onOpenScreenPinningSettings: () -> Unit,
    val onStartScreenPinning: () -> Unit,
    val onOpenOverlaySettings: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenCastSettings: () -> Unit,
    val onOpenWebViewProviderSettings: () -> Unit,
    val onReinstallOfficialApk: () -> Unit
)

internal data class PreparationLocationActions(
    val onRequestLocationPermission: () -> Unit,
    val onOpenLocationServicesSettings: () -> Unit,
    val onRefreshGeofenceLocation: () -> Unit,
    val onOpenGeofenceMapViewer: () -> Unit,
    val onOpenFakeLocationDeveloperOptionsSettings: () -> Unit
)

internal data class PreparationRuntimeSecurityActions(
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenOverlayAccessibilitySettings: () -> Unit,
    val onOpenOverlaySettings: () -> Unit,
    val onOpenOverlayGuardSettings: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenCastSettings: () -> Unit
)

internal data class PreparationScreenActions(
    val session: PreparationSessionActions,
    val network: PreparationNetworkActions,
    val device: PreparationDeviceActions,
    val location: PreparationLocationActions,
    val runtimeSecurity: PreparationRuntimeSecurityActions
) {
    val onChooseKeyboard: () -> Unit get() = device.onChooseKeyboard
    val onOpenKeyboardSettings: () -> Unit get() = device.onOpenKeyboardSettings
    val onGrantBluetoothPermission: () -> Unit get() = device.onGrantBluetoothPermission
    val onOpenBluetoothSettings: () -> Unit get() = device.onOpenBluetoothSettings
    val onOpenAccessibilitySettings: () -> Unit get() = device.onOpenAccessibilitySettings
    val onOpenOverlayAccessibilitySettings: () -> Unit get() = device.onOpenOverlayAccessibilitySettings
    val onOpenDeveloperOptionsSettings: () -> Unit get() = device.onOpenDeveloperOptionsSettings
    val onRequestLocationPermission: () -> Unit get() = location.onRequestLocationPermission
    val onOpenLocationServicesSettings: () -> Unit get() = location.onOpenLocationServicesSettings
    val onRefreshGeofenceLocation: () -> Unit get() = location.onRefreshGeofenceLocation
    val onOpenGeofenceMapViewer: () -> Unit get() = location.onOpenGeofenceMapViewer
    val onOpenInternetSettings: () -> Unit get() = network.onOpenInternetSettings
    val onOpenVpnSettings: () -> Unit get() = network.onOpenVpnSettings
    val onOpenWifiSettings: () -> Unit get() = network.onOpenWifiSettings
    val onOpenCellularSettings: () -> Unit get() = network.onOpenCellularSettings
    val onOpenAirplaneModeSettings: () -> Unit get() = network.onOpenAirplaneModeSettings
    val onRefreshNetworkStatus: () -> Unit get() = network.onRefreshNetworkStatus
    val onOpenDateTimeSettings: () -> Unit get() = device.onOpenDateTimeSettings
    val onOpenFakeLocationDeveloperOptionsSettings: () -> Unit
        get() = location.onOpenFakeLocationDeveloperOptionsSettings
    val onOpenScreenPinningSettings: () -> Unit get() = device.onOpenScreenPinningSettings
    val onStartScreenPinning: () -> Unit get() = device.onStartScreenPinning
    val onOpenOverlaySettings: () -> Unit get() = device.onOpenOverlaySettings
    val onOpenAppSettings: () -> Unit get() = device.onOpenAppSettings
    val onOpenCastSettings: () -> Unit get() = device.onOpenCastSettings
    val onOpenWebViewProviderSettings: () -> Unit get() = device.onOpenWebViewProviderSettings
    val onReinstallOfficialApk: () -> Unit get() = device.onReinstallOfficialApk
    val onRefreshStatus: () -> Unit get() = session.onRefreshStatus
    val onRefreshAllSecurityChecks: () -> Unit get() = session.onRefreshAllSecurityChecks
    val onRefreshHealthCheck: () -> Unit get() = session.onRefreshHealthCheck
    val onRequestSectionReport: (DiagnosticSection) -> Unit get() = session.onRequestSectionReport
    val onExportDiagnostics: () -> Unit get() = session.onExportDiagnostics
    val onAutoFixShown: (String) -> Unit get() = session.onAutoFixShown
    val onPreviousSessionRecoveryHintShown: (String) -> Unit get() = session.onPreviousSessionRecoveryHintShown
    val onAutoFixActionOpened: (String) -> Unit get() = session.onAutoFixActionOpened
    val onScreenPinningDeferred: (String) -> Unit get() = session.onScreenPinningDeferred
    val onStartExam: () -> Unit get() = session.onStartExam
    val onBackHome: () -> Unit get() = session.onBackHome
}
