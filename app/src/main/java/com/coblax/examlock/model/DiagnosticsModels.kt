package com.coblax.examlock.model
internal enum class DiagnosticSection {
    Keyboard,
    Bluetooth,
    Network,
    Accessibility,
    Overlay,
    Geofence,
    FakeLocation,
    AppSwitch,
    DeveloperAdb,
    Root,
    Signature,
    VirtualEnvironment,
    Clipboard,
    ScreenPinning,
    DeviceTime,
    SecurityHealth,
    ScreenRecorder,
    DisplayMirror,
    MultiWindow
}

internal data class RootDetectionDetails(
    val hasTestKeys: Boolean,
    val hasSuBinary: Boolean,
    val foundRootPackages: List<String>,
    val rootBinaryPaths: List<String>,
    val magiskPaths: List<String>,
    val zygiskDetected: Boolean,
    val xposedBridgeDetected: Boolean,
    val verifiedBootState: String,
    val vbmetaDeviceState: String,
    val flashLocked: String,
    val bootloaderUnlocked: Boolean,
    val selinuxEnabled: Boolean?,
    val selinuxEnforced: Boolean?,
    val dangerousSystemProperties: List<String>,
    val roDebuggable: String,
    val roSecure: String,
    val roAdbSecure: String,
    val roBuildType: String
)

internal enum class RootIndicatorType {
    Zygisk,
    Magisk,
    RootBinary,
    XposedBridge,
    SelinuxDisabled,
    SelinuxPermissive,
    Bootloader,
    DangerousProps,
    TestKeys
}

internal data class VirtualEnvironmentDiagnostics(
    val detected: Boolean,
    val indicators: List<String>,
    val qemuProperty: String,
    val emulatorPackages: List<String>,
    val qemuFiles: List<String>,
    val abis: List<String>
)

internal data class ClipboardDiagnostics(
    val hasData: Boolean,
    val itemCount: Int,
    val currentSemanticSignature: String
)
internal enum class DiagnosticEventLevel {
    INFO,
    WARNING,
    SECURITY,
    ERROR
}

internal data class DiagnosticEvent(
    val timestamp: String,
    val level: String,
    val code: String,
    val screen: String,
    val appElapsedMs: Long,
    val sessionElapsedMs: Long?,
    val details: String
)
