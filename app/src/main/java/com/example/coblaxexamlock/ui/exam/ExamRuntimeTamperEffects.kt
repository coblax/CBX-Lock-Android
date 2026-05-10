package com.example.coblaxexamlock.ui.exam

import android.location.Location
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.ScreenPinningSignals

@Composable
internal fun BypassTamperLoggingEffects(
    adminSettings: AdminSettings,
    screenPinningBypassTamperLogged: Boolean,
    updateScreenPinningBypassTamperLogged: (Boolean) -> Unit,
    accessibilityBypassTamperLogged: Boolean,
    updateAccessibilityBypassTamperLogged: (Boolean) -> Unit,
    adbBypassTamperLogged: Boolean,
    updateAdbBypassTamperLogged: (Boolean) -> Unit,
    clipboardBypassTamperLogged: Boolean,
    updateClipboardBypassTamperLogged: (Boolean) -> Unit,
    overlayBypassTamperLogged: Boolean,
    updateOverlayBypassTamperLogged: (Boolean) -> Unit,
    geofenceBypassTamperLogged: Boolean,
    updateGeofenceBypassTamperLogged: (Boolean) -> Unit,
    fakeLocationBypassTamperLogged: Boolean,
    updateFakeLocationBypassTamperLogged: (Boolean) -> Unit,
    deviceTimeBypassTamperLogged: Boolean,
    updateDeviceTimeBypassTamperLogged: (Boolean) -> Unit,
    appSwitchBypassTamperLogged: Boolean,
    updateAppSwitchBypassTamperLogged: (Boolean) -> Unit,
    rootBypassTamperLogged: Boolean,
    updateRootBypassTamperLogged: (Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    LaunchedEffect(adminSettings.screenPinningBypassTampered) {
        if (adminSettings.screenPinningBypassTampered && !screenPinningBypassTamperLogged) {
            recordAction(
                ScreenPinningSignals.eventBypassTampered(),
                ScreenPinningSignals.bypassTamperDetail(),
                DiagnosticEventLevel.SECURITY
            )
            updateScreenPinningBypassTamperLogged(true)
        } else if (!adminSettings.screenPinningBypassTampered) {
            updateScreenPinningBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.accessibilityBypassTampered) {
        if (adminSettings.accessibilityBypassTampered && !accessibilityBypassTamperLogged) {
            recordAction(
                "ACCESSIBILITY_BYPASS_TAMPER_DETECTED",
                "Accessibility bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAccessibilityBypassTamperLogged(true)
        } else if (!adminSettings.accessibilityBypassTampered) {
            updateAccessibilityBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.adbBypassTampered) {
        if (adminSettings.adbBypassTampered && !adbBypassTamperLogged) {
            recordAction(
                "ADB_BYPASS_TAMPER_DETECTED",
                "ADB bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAdbBypassTamperLogged(true)
        } else if (!adminSettings.adbBypassTampered) {
            updateAdbBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.clipboardBypassTampered) {
        if (adminSettings.clipboardBypassTampered && !clipboardBypassTamperLogged) {
            recordAction(
                "CLIPBOARD_BYPASS_TAMPER_DETECTED",
                "Clipboard bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateClipboardBypassTamperLogged(true)
        } else if (!adminSettings.clipboardBypassTampered) {
            updateClipboardBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.overlayBypassTampered) {
        if (adminSettings.overlayBypassTampered && !overlayBypassTamperLogged) {
            recordAction(
                "OVERLAY_BYPASS_TAMPER_DETECTED",
                "Overlay bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateOverlayBypassTamperLogged(true)
        } else if (!adminSettings.overlayBypassTampered) {
            updateOverlayBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.geofenceBypassTampered) {
        if (adminSettings.geofenceBypassTampered && !geofenceBypassTamperLogged) {
            recordAction(
                "GEOFENCE_BYPASS_TAMPER_DETECTED",
                "Geofence bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateGeofenceBypassTamperLogged(true)
        } else if (!adminSettings.geofenceBypassTampered) {
            updateGeofenceBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.fakeLocationBypassTampered) {
        if (adminSettings.fakeLocationBypassTampered && !fakeLocationBypassTamperLogged) {
            recordAction(
                "FAKE_LOCATION_BYPASS_TAMPER_DETECTED",
                "Fake-location bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateFakeLocationBypassTamperLogged(true)
        } else if (!adminSettings.fakeLocationBypassTampered) {
            updateFakeLocationBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.deviceTimeBypassTampered) {
        if (adminSettings.deviceTimeBypassTampered && !deviceTimeBypassTamperLogged) {
            recordAction(
                "DEVICE_TIME_BYPASS_TAMPER_DETECTED",
                "Device Time bypass seal mismatch; bypass disabled automatically",
                DiagnosticEventLevel.SECURITY
            )
            updateDeviceTimeBypassTamperLogged(true)
        } else if (!adminSettings.deviceTimeBypassTampered) {
            updateDeviceTimeBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.appSwitchBypassTampered) {
        if (adminSettings.appSwitchBypassTampered && !appSwitchBypassTamperLogged) {
            recordAction(
                "APP_SWITCH_BYPASS_TAMPER_DETECTED",
                "App Switch bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAppSwitchBypassTamperLogged(true)
        } else if (!adminSettings.appSwitchBypassTampered) {
            updateAppSwitchBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.rootBypassTampered) {
        if (adminSettings.rootBypassTampered && !rootBypassTamperLogged) {
            recordAction(
                "ROOT_BYPASS_TAMPER_DETECTED",
                "Root bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateRootBypassTamperLogged(true)
        } else if (!adminSettings.rootBypassTampered) {
            updateRootBypassTamperLogged(false)
        }
    }
}
