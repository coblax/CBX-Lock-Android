package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coblax.examlock.AccessibilityBypassResolver
import com.coblax.examlock.AccessibilityBypassState
import com.coblax.examlock.AdbBypassResolver
import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AppSwitchBypassResolver
import com.coblax.examlock.ClipboardBypassResolver
import com.coblax.examlock.DeviceTimeBypassResolver
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.FakeLocationBypassResolver
import com.coblax.examlock.FakeLocationBypassState
import com.coblax.examlock.GeofenceBypassResolver
import com.coblax.examlock.GeofenceBypassState
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.OverlayBypassResolver
import com.coblax.examlock.RootBypassResolver
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.ScreenPinningBypassResolver
import com.coblax.examlock.VpnBypassResolver
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.examOverridesSummary
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.parseGeofenceConfig
import com.coblax.examlock.ui.geofence.effectiveCircleCenters

/**
 * Everything the exam session derives from the admin settings and the exam QR alone:
 * which bypasses are active and the location policy. It used to be fourteen separate
 * `remember` blocks inside the session composable, which made that one method too
 * large for the runtime to compile cheaply on low-end phones.
 */
internal class ExamRuntimeBypassContext(
    adminSettings: AdminSettings,
    payload: ExamQrPayload
) {
    val screenPinningBypassState = ScreenPinningBypassResolver.stateOf(
        enabled = adminSettings.bypassScreenPinning,
        tampered = adminSettings.screenPinningBypassTampered
    )
    val screenPinningMode = ScreenPinningBypassResolver.modeOf(screenPinningBypassState)
    val overlayBypassState = OverlayBypassResolver.stateOf(
        enabled = adminSettings.bypassOverlay,
        tampered = adminSettings.overlayBypassTampered
    )
    val appSwitchBypassState = AppSwitchBypassResolver.stateOf(
        enabled = adminSettings.bypassAppSwitch,
        tampered = adminSettings.appSwitchBypassTampered
    )
    val accessibilityBypassState = AccessibilityBypassResolver.stateOf(
        enabled = adminSettings.bypassAccessibility,
        tampered = adminSettings.accessibilityBypassTampered
    )
    val clipboardBypassState = ClipboardBypassResolver.stateOf(
        enabled = adminSettings.bypassClipboard,
        tampered = adminSettings.clipboardBypassTampered
    )
    val adbBypassState = AdbBypassResolver.stateOf(
        enabled = adminSettings.bypassAdb,
        tampered = adminSettings.adbBypassTampered
    )
    val rootBypassState = RootBypassResolver.stateOf(
        enabled = adminSettings.bypassRoot,
        tampered = adminSettings.rootBypassTampered
    )
    val geofenceBypassState = GeofenceBypassResolver.stateOf(
        enabled = adminSettings.bypassGeofence,
        tampered = adminSettings.geofenceBypassTampered
    )
    val fakeLocationBypassState = FakeLocationBypassResolver.stateOf(
        enabled = adminSettings.bypassFakeLocation,
        tampered = adminSettings.fakeLocationBypassTampered
    )
    val deviceTimeBypassState = DeviceTimeBypassResolver.stateOf(
        enabled = adminSettings.bypassDeviceTime,
        tampered = adminSettings.deviceTimeBypassTampered
    )
    val vpnBypassState = VpnBypassResolver.stateOf(
        enabled = adminSettings.bypassVpn,
        tampered = adminSettings.vpnBypassTampered
    )
    val bypassScreenPinning = adminSettings.bypassScreenPinning
    val bypassBluetooth = adminSettings.bypassBluetooth
    val bypassAccessibility = accessibilityBypassState == AccessibilityBypassState.Active
    val bypassAdb = adbBypassState == AdbBypassState.Active
    val bypassRoot = rootBypassState == RootBypassState.Active
    val bypassReverseEngineering = adminSettings.bypassReverseEngineering
    val bypassApkIntegrity = adminSettings.bypassApkIntegrity
    val bypassVirtualEnvironment = adminSettings.bypassVirtualEnvironment
    val bypassKeyboardPolicy = adminSettings.bypassKeyboardPolicy
    val bypassClipboard = adminSettings.bypassClipboard
    val bypassOverlay = adminSettings.bypassOverlay
    val bypassGeofence = geofenceBypassState == GeofenceBypassState.Active
    val bypassFakeLocation = fakeLocationBypassState == FakeLocationBypassState.Active
    val bypassDeviceTime = deviceTimeBypassState == DeviceTimeBypassState.Active
    val bypassVpn = vpnBypassState == VpnBypassState.Active
    val bypassAppSwitch = adminSettings.bypassAppSwitch
    val bypassScreenRecorder = adminSettings.bypassScreenRecorder
    val bypassDisplayMirror = adminSettings.bypassDisplayMirror
    val bypassMultiWindow = adminSettings.bypassMultiWindow
    val adminOverridesSummary = examOverridesSummary(adminSettings, payload.securityBypasses)
    val effectiveLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
    val effectiveLocationPolicySource = if (bypassGeofence) {
        LocationPolicySource.Bypassed
    } else if (payload.locationPolicy != null) {
        payload.locationPolicySource
    } else {
        LocationPolicySource.DisabledNoPolicy
    }
    val geofenceConfigParseResult = parseGeofenceConfig(
        enabled = effectiveLocationPolicy.geofenceEnabled,
        centerLatRaw = effectiveLocationPolicy.centerLat,
        centerLngRaw = effectiveLocationPolicy.centerLng,
        radiusMetersRaw = effectiveLocationPolicy.radiusMeters,
        shapeType = effectiveLocationPolicy.shapeType,
        polygonVertices = effectiveLocationPolicy.vertices,
        circleCenters = effectiveLocationPolicy.effectiveCircleCenters
    )
    val warmLocationPolicySignature = buildWarmLocationValidationPolicySignature(
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState
    )
    val geofenceEnabled = geofenceConfigParseResult.enabled

    // The exam QR can say where to get the build it requires; students never set this up.
    val officialApkUrl = payload.appUpdateUrl.trim().ifBlank { adminSettings.officialApkUrl.trim() }
}

@Composable
internal fun rememberExamRuntimeBypassContext(
    adminSettings: AdminSettings,
    payload: ExamQrPayload
): ExamRuntimeBypassContext = remember(adminSettings, payload) {
    ExamRuntimeBypassContext(adminSettings, payload)
}
