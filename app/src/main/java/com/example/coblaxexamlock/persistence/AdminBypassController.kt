package com.example.coblaxexamlock.persistence

import android.content.Context
import android.util.Log
import com.example.coblaxexamlock.AdminAuthSession
import com.example.coblaxexamlock.GateKeys
import com.example.coblaxexamlock.model.AdminSettings

private const val AdminBypassControllerTag = "AdminBypassController"

internal object AdminBypassController {
    fun persistBypassSettings(context: Context, settings: AdminSettings): Boolean {
        if (!AdminAuthSession.hasActiveToken()) {
            Log.w(AdminBypassControllerTag, "Blocked bypass write because no active admin auth token is present.")
            return false
        }
        val gateStates = linkedMapOf(
            GateKeys.ScreenPinning.id to settings.bypassScreenPinning,
            GateKeys.Bluetooth.id to settings.bypassBluetooth,
            GateKeys.Accessibility.id to settings.bypassAccessibility,
            GateKeys.Adb.id to settings.bypassAdb,
            GateKeys.Root.id to settings.bypassRoot,
            GateKeys.VirtualEnv.id to settings.bypassVirtualEnvironment,
            GateKeys.Vpn.id to settings.bypassVpn,
            GateKeys.KeyboardPolicy.id to settings.bypassKeyboardPolicy,
            GateKeys.Clipboard.id to settings.bypassClipboard,
            GateKeys.Overlay.id to settings.bypassOverlay,
            GateKeys.AppSwitch.id to settings.bypassAppSwitch,
            GateKeys.Location.id to false,
            GateKeys.Geofence.id to settings.bypassGeofence,
            GateKeys.FakeLocation.id to settings.bypassFakeLocation,
            GateKeys.DeviceTime.id to settings.bypassDeviceTime,
            GateKeys.ScreenRecorder.id to settings.bypassScreenRecorder,
            GateKeys.DisplayMirror.id to settings.bypassDisplayMirror,
            GateKeys.MultiWindow.id to settings.bypassMultiWindow,
            GateKeys.ReverseEngineering.id to settings.bypassReverseEngineering,
            GateKeys.ApkIntegrity.id to settings.bypassApkIntegrity
        )
        return BypassStorageRepository.writeAllStates(context, gateStates)
    }
}
