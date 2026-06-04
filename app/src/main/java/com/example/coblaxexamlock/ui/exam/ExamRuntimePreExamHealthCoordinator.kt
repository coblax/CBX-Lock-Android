package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DpcRuntimeStatus
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ScreenPinningPlatformBridge
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.buildDeviceSurvivalPolicy
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.ui.preparation.PreExamHealthCheckInput
import com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot
import com.example.coblaxexamlock.ui.preparation.buildPreExamHealthSnapshot

internal fun handleExamRuntimePreExamHealthRefresh(
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    refreshPreparationStatusChecks: () -> Unit
) {
    recordAction(
        ExamRuntimeHardeningDiagnostics.PreExamHealthCheckStarted,
        "source=pre_exam_health | ${deviceCompatibilityProfile.diagnosticSummary()}",
        DiagnosticEventLevel.INFO
    )
    refreshPreparationStatusChecks()
    recordAction(
        ExamRuntimeHardeningDiagnostics.PreExamHealthCheckCompleted,
        "source=pre_exam_health | family=${deviceCompatibilityProfile.family.name}",
        DiagnosticEventLevel.INFO
    )
}

internal fun buildExamRuntimePreExamHealthSnapshot(
    context: Context,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    lockTaskBridge: ActivityLockTaskBridge,
    adminSettings: AdminSettings,
    vpnBypassState: VpnBypassState,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState,
    deviceTimeBypassState: DeviceTimeBypassState,
    accessibilityGuardEnabled: Boolean,
    overlayRiskResult: OverlayRiskResult,
    networkReadinessStatus: NetworkReadinessStatus,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    examRuntimeRecoveryState: ExamRuntimeRecoveryState,
    flowUiState: ExamRuntimeFlowUiState,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    batteryStatus: ExamBatteryStatus,
    dpcRuntimeStatus: DpcRuntimeStatus
): PreExamHealthSnapshot =
    buildPreExamHealthSnapshot(
        PreExamHealthCheckInput(
            compatibilityProfile = deviceCompatibilityProfile,
            screenPinningAvailable = ScreenPinningPlatformBridge.isAvailable(),
            screenPinningActive = lockTaskBridge.active(),
            screenPinningBypassed = adminSettings.bypassScreenPinning,
            accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            overlayRiskResult = overlayRiskResult,
            overlayBypassed = adminSettings.bypassOverlay,
            networkReadinessStatus = networkReadinessStatus,
            vpnBypassed = vpnBypassState == VpnBypassState.Active,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewRecoveryState = examRuntimeRecoveryState.name,
            webViewSessionResetInFlight = flowUiState.webViewSessionResetInFlight.value,
            webViewSessionResetError = flowUiState.webViewSessionResetError.value,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            geofenceBypassed = geofenceBypassState == GeofenceBypassState.Active,
            fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
            fakeLocationBypassed = fakeLocationBypassState == FakeLocationBypassState.Active,
            deviceTimeSecurityStatus = deviceTimeSecurityStatus,
            deviceTimeBypassed = deviceTimeBypassState == DeviceTimeBypassState.Active,
            batteryStatus = batteryStatus,
            dpcRuntimeStatus = dpcRuntimeStatus,
            generatedAtElapsedMs = SystemClock.elapsedRealtime()
        )
    )

internal fun resolveExamRuntimeDeviceSurvivalPolicy(
    lowRamProfile: LowRamProfile,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    preExamHealthSnapshot: PreExamHealthSnapshot
): DeviceSurvivalPolicy =
    buildDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        preExamHealthSnapshot = preExamHealthSnapshot
    )
