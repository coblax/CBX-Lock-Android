package com.coblax.examlock.ui.exam

import android.content.Context
import android.os.SystemClock
import com.coblax.examlock.ActivityLockTaskBridge
import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DpcRuntimeStatus
import com.coblax.examlock.FakeLocationBypassState
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.GeofenceBypassState
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.ScreenPinningPlatformBridge
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.buildDeviceSurvivalPolicy
import com.coblax.examlock.isExamGuardAccessibilityAvailable
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.ui.preparation.PreExamHealthCheckInput
import com.coblax.examlock.ui.preparation.PreExamHealthSnapshot
import com.coblax.examlock.ui.preparation.buildPreExamHealthSnapshot

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
