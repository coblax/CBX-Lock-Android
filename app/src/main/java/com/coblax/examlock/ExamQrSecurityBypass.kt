package com.coblax.examlock

import com.coblax.examlock.model.AdminSettings

/**
 * A security check an exam QR switches off for the exam it opens. [key] matches the
 * Secret Admin switch of the same check; [bit] is its position in the QR bitmask and
 * must never be moved or reused, or QRs already handed out would change meaning.
 */
enum class ExamQrSecurityBypass(val key: String, val bit: Int) {
    ScreenPinning("screen_pinning", 0),
    AppSwitch("app_switch", 1),
    MultiWindow("multi_window", 2),
    Overlay("overlay", 3),
    Bluetooth("bluetooth", 4),
    Keyboard("keyboard", 5),
    Accessibility("accessibility", 6),
    Clipboard("clipboard", 7),
    DeviceTime("device_time", 8),
    Adb("adb", 9),
    Root("root", 10),
    ReverseEngineering("reverse_engineering", 11),
    ApkIntegrity("apk_integrity", 12),
    VirtualEnvironment("virtual_environment", 13),
    Vpn("vpn", 14),
    Geofence("geofence", 15),
    FakeLocation("fake_location", 16),
    ScreenRecorder("screen_recorder", 17),
    DisplayMirror("display_mirror", 18);

    companion object {
        fun fromKey(key: String): ExamQrSecurityBypass? = entries.firstOrNull { it.key == key }

        fun toMask(bypasses: Set<ExamQrSecurityBypass>): Long =
            bypasses.fold(0L) { mask, bypass -> mask or (1L shl bypass.bit) }

        /** Bits this version does not know are dropped, so a newer QR never relaxes more here. */
        fun fromMask(mask: Long): Set<ExamQrSecurityBypass> =
            entries.filterTo(linkedSetOf()) { mask and (1L shl it.bit) != 0L }
    }
}

internal fun AdminSettings.isBypassed(bypass: ExamQrSecurityBypass): Boolean = when (bypass) {
    ExamQrSecurityBypass.ScreenPinning -> bypassScreenPinning
    ExamQrSecurityBypass.AppSwitch -> bypassAppSwitch
    ExamQrSecurityBypass.MultiWindow -> bypassMultiWindow
    ExamQrSecurityBypass.Overlay -> bypassOverlay
    ExamQrSecurityBypass.Bluetooth -> bypassBluetooth
    ExamQrSecurityBypass.Keyboard -> bypassKeyboardPolicy
    ExamQrSecurityBypass.Accessibility -> bypassAccessibility
    ExamQrSecurityBypass.Clipboard -> bypassClipboard
    ExamQrSecurityBypass.DeviceTime -> bypassDeviceTime
    ExamQrSecurityBypass.Adb -> bypassAdb
    ExamQrSecurityBypass.Root -> bypassRoot
    ExamQrSecurityBypass.ReverseEngineering -> bypassReverseEngineering
    ExamQrSecurityBypass.ApkIntegrity -> bypassApkIntegrity
    ExamQrSecurityBypass.VirtualEnvironment -> bypassVirtualEnvironment
    ExamQrSecurityBypass.Vpn -> bypassVpn
    ExamQrSecurityBypass.Geofence -> bypassGeofence
    ExamQrSecurityBypass.FakeLocation -> bypassFakeLocation
    ExamQrSecurityBypass.ScreenRecorder -> bypassScreenRecorder
    ExamQrSecurityBypass.DisplayMirror -> bypassDisplayMirror
}

internal fun AdminSettings.activeSecurityBypasses(): Set<ExamQrSecurityBypass> =
    ExamQrSecurityBypass.entries.filterTo(linkedSetOf()) { isBypassed(it) }

/**
 * The settings an exam runs with when its QR carries [bypasses]: each one is switched on
 * for this exam only, on top of the device's own Secret Admin switches, which stay as saved.
 * A QR bypass also wins over a tampered bypass store; the QR is a separate, signed source.
 */
internal fun AdminSettings.withExamQrBypasses(bypasses: Set<ExamQrSecurityBypass>): AdminSettings {
    if (bypasses.isEmpty()) return this
    val merged = bypasses.fold(this) { settings, bypass ->
        when (bypass) {
            ExamQrSecurityBypass.ScreenPinning ->
                settings.copy(bypassScreenPinning = true, screenPinningBypassTampered = false)
            ExamQrSecurityBypass.AppSwitch ->
                settings.copy(bypassAppSwitch = true, appSwitchBypassTampered = false)
            ExamQrSecurityBypass.MultiWindow ->
                settings.copy(bypassMultiWindow = true, multiWindowBypassTampered = false)
            ExamQrSecurityBypass.Overlay ->
                settings.copy(bypassOverlay = true, overlayBypassTampered = false)
            ExamQrSecurityBypass.Bluetooth -> settings.copy(bypassBluetooth = true)
            ExamQrSecurityBypass.Keyboard -> settings.copy(bypassKeyboardPolicy = true)
            ExamQrSecurityBypass.Accessibility ->
                settings.copy(bypassAccessibility = true, accessibilityBypassTampered = false)
            ExamQrSecurityBypass.Clipboard ->
                settings.copy(bypassClipboard = true, clipboardBypassTampered = false)
            ExamQrSecurityBypass.DeviceTime ->
                settings.copy(bypassDeviceTime = true, deviceTimeBypassTampered = false)
            ExamQrSecurityBypass.Adb -> settings.copy(bypassAdb = true, adbBypassTampered = false)
            ExamQrSecurityBypass.Root -> settings.copy(bypassRoot = true, rootBypassTampered = false)
            ExamQrSecurityBypass.ReverseEngineering ->
                settings.copy(bypassReverseEngineering = true, reverseEngineeringBypassTampered = false)
            ExamQrSecurityBypass.ApkIntegrity ->
                settings.copy(bypassApkIntegrity = true, apkIntegrityBypassTampered = false)
            ExamQrSecurityBypass.VirtualEnvironment -> settings.copy(bypassVirtualEnvironment = true)
            ExamQrSecurityBypass.Vpn -> settings.copy(bypassVpn = true, vpnBypassTampered = false)
            ExamQrSecurityBypass.Geofence ->
                settings.copy(bypassGeofence = true, geofenceBypassTampered = false)
            ExamQrSecurityBypass.FakeLocation ->
                settings.copy(bypassFakeLocation = true, fakeLocationBypassTampered = false)
            ExamQrSecurityBypass.ScreenRecorder ->
                settings.copy(bypassScreenRecorder = true, screenRecorderBypassTampered = false)
            ExamQrSecurityBypass.DisplayMirror ->
                settings.copy(bypassDisplayMirror = true, displayMirrorBypassTampered = false)
        }
    }
    return merged.copy(
        bypassLocation = merged.bypassGeofence || merged.bypassFakeLocation,
        locationBypassTampered = merged.geofenceBypassTampered || merged.fakeLocationBypassTampered
    )
}

/** The override summary for reports, naming what the exam QR relaxed on top of the device. */
internal fun examOverridesSummary(settings: AdminSettings, qrBypasses: Set<ExamQrSecurityBypass>): String {
    val summary = settings.overrideSummary()
    if (qrBypasses.isEmpty()) return summary
    val relaxedByQr = ExamQrSecurityBypass.entries.filter { it in qrBypasses }.joinToString { it.key }
    return "$summary | from exam QR: $relaxedByQr"
}
