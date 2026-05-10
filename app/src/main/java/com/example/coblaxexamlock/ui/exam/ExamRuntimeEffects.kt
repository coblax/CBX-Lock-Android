package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.config.ExamNativeFullscreenBridgeInstallScript
import com.example.coblaxexamlock.format.buildExamNativeFullscreenStateSyncScript
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.runtime.hasBluetoothExamPermission
import com.example.coblaxexamlock.ScreenPinningMode

@Composable
internal fun RuntimeSetupEffects(
    context: Context,
    mainActivity: MainActivity?,
    bypassKeyboardPolicy: Boolean,
    examSessionStarted: Boolean,
    nativeExamFullscreenActive: Boolean,
    webViewInstance: WebView?,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    refreshScreenPinningDiagnostics: () -> Unit,
    refreshKeyboardSecurity: (Boolean) -> Unit,
    refreshBluetoothSecurity: (Boolean) -> Unit,
    refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    updateBluetoothPermissionGranted: (Boolean) -> Unit,
    updateUseBuiltInExamKeyboard: (Boolean) -> Unit,
    updateShowBuiltInExamKeyboard: (Boolean) -> Unit,
    cleanupActiveExamWebViewInstance: () -> Unit
) {
    LaunchedEffect(Unit) {
        refreshScreenPinningDiagnostics()
        refreshKeyboardSecurity(false)
        refreshBluetoothSecurity(false)
        refreshDeviceIntegritySecurity(false)
    }

    LaunchedEffect(Unit) {
        updateBluetoothPermissionGranted(hasBluetoothExamPermission(context))
    }

    LaunchedEffect(bypassKeyboardPolicy) {
        if (bypassKeyboardPolicy) {
            updateUseBuiltInExamKeyboard(false)
            updateShowBuiltInExamKeyboard(false)
        } else {
            refreshKeyboardSecurity(false)
        }
    }

    LaunchedEffect(mainActivity, nativeExamFullscreenActive, webViewInstance) {
        nativeFullscreenBridge.updateActive(nativeExamFullscreenActive)
        if (nativeExamFullscreenActive) {
            mainActivity?.setExamLockMode(
                enabled = true,
                allowLockTask = false
            )
        }
        // When nativeExamFullscreenActive becomes false, do NOT call
        // setExamLockMode(enabled=false) here — that is handled by the
        // pinning activation flow and exam session teardown explicitly.
        // Calling it here creates a race where the derived state briefly
        // flickers to false during state transitions, causing stopLockTask().
        webViewInstance?.evaluateExamJavascriptSafely(ExamNativeFullscreenBridgeInstallScript)
        webViewInstance?.evaluateExamJavascriptSafely(
            buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive)
        )
    }

    LaunchedEffect(examSessionStarted) {
        if (!examSessionStarted) {
            cleanupActiveExamWebViewInstance()
        }
    }
}

@Composable
internal fun RuntimeRecoveryAndMemoryEffects(
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    examSessionRecoveryNonce: Long,
    recordInfoAction: (String, String) -> Unit,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit,
    refreshReverseEngineeringStatus: () -> Unit,
    refreshIntegrityGuard: () -> Unit,
    onSimulateRendererGone: () -> Unit,
    onTrimMemory: (Int) -> Unit
) {
    LaunchedEffect(pendingDirectLinkSaveLog) {
        val details = pendingDirectLinkSaveLog ?: return@LaunchedEffect
        recordInfoAction("DIRECT_LINK_SAVED_FROM_QR", details)
        onDirectLinkSaveLogConsumed()
    }

    LaunchedEffect(pendingRecoveryEventDetails, examSessionRecoveryNonce) {
        val details = pendingRecoveryEventDetails ?: return@LaunchedEffect
        recordInfoAction("PROCESS_DEATH_RECOVERED", details)
        onRecoveryEventConsumed()
    }

    LaunchedEffect(Unit) {
        refreshReverseEngineeringStatus()
        refreshIntegrityGuard()
    }

    val latestRendererGoneRecoveryHandler by rememberUpdatedState(newValue = onSimulateRendererGone)
    DisposableEffect(Unit) {
        ExamWebViewRecoveryTestHooks.registerRendererGoneSimulation {
            latestRendererGoneRecoveryHandler()
        }
        onDispose {
            ExamWebViewRecoveryTestHooks.registerRendererGoneSimulation(null)
        }
    }

    val latestTrimMemoryHandler by rememberUpdatedState(newValue = onTrimMemory)
    DisposableEffect(Unit) {
        val listener: (Int) -> Unit = { level -> latestTrimMemoryHandler(level) }
        MemoryPressureCoordinator.addListener(listener)
        onDispose {
            MemoryPressureCoordinator.removeListener(listener)
        }
    }
}

@Composable
internal fun RuntimeAppSwitchFallbackLoggingEffect(
    examGuardArmed: Boolean,
    appSwitchStatus: AppSwitchStatus,
    screenPinningMode: ScreenPinningMode,
    appSwitchFallbackArmedLogged: Boolean,
    updateAppSwitchFallbackArmedLogged: (Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    LaunchedEffect(
        examGuardArmed,
        appSwitchStatus.runtimeMonitoringActive,
        appSwitchStatus.fallbackGuardActive,
        appSwitchStatus.bypassed
    ) {
        if (
            examGuardArmed &&
            appSwitchStatus.runtimeMonitoringActive &&
            appSwitchStatus.fallbackGuardActive &&
            !appSwitchStatus.bypassed &&
            !appSwitchFallbackArmedLogged
        ) {
            recordAction(
                "APP_SWITCH_FALLBACK_ARMED",
                AppSwitchMonitor.eventDetails(
                    protectionMode = appSwitchStatus.protectionMode,
                    screenPinningMode = screenPinningMode,
                    lockTaskActive = appSwitchStatus.lockTaskActive
                ),
                DiagnosticEventLevel.INFO
            )
            updateAppSwitchFallbackArmedLogged(true)
        } else if (!examGuardArmed || !appSwitchStatus.fallbackGuardActive) {
            updateAppSwitchFallbackArmedLogged(false)
        }
    }
}

@Composable
internal fun RuntimeDisposeCleanupEffect(
    examSessionStarted: Boolean,
    lockTaskRequestPending: Boolean,
    lockTaskBridge: ActivityLockTaskBridge,
    cleanupActiveExamWebViewInstance: () -> Unit,
    launchExitSessionClearBestEffort: () -> Unit,
    disarmAccessibilityGuard: () -> Unit,
    stopAlarm: () -> Unit
) {
    val latestExamSessionStarted by rememberUpdatedState(examSessionStarted)
    val latestLockTaskRequestPending by rememberUpdatedState(lockTaskRequestPending)
    val latestCleanupActiveExamWebViewInstance by rememberUpdatedState(cleanupActiveExamWebViewInstance)
    val latestLaunchExitSessionClearBestEffort by rememberUpdatedState(launchExitSessionClearBestEffort)
    val latestDisarmAccessibilityGuard by rememberUpdatedState(disarmAccessibilityGuard)
    val latestStopAlarm by rememberUpdatedState(stopAlarm)

    DisposableEffect(Unit) {
        onDispose {
            if (latestExamSessionStarted || latestLockTaskRequestPending) {
                lockTaskBridge.disengage()
                latestLaunchExitSessionClearBestEffort()
            }
            latestCleanupActiveExamWebViewInstance()
            latestDisarmAccessibilityGuard()
            latestStopAlarm()
        }
    }
}
