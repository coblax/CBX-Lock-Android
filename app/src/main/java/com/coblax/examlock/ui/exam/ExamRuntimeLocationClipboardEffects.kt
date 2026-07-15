package com.coblax.examlock.ui.exam

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.coblax.examlock.ClipboardBypassState
import com.coblax.examlock.ClipboardChangeDecision
import com.coblax.examlock.ClipboardSnapshot
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.config.ClipboardListenerWarmupIgnoreMillis
import com.coblax.examlock.config.ClipboardSettleWindowMillis
import com.coblax.examlock.config.GeofenceRuntimeRecheckIntervalMillis
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.GeofenceConfigParseResult
import com.coblax.examlock.readClipboardSnapshotLite
import com.coblax.examlock.runtime.isBluetoothEnabledForExam
import kotlinx.coroutines.delay

private const val LowGeofenceRuntimeRecheckIntervalMillis = 60_000L
private const val UltraGeofenceRuntimeRecheckIntervalMillis = 90_000L

internal fun geofenceRuntimeRecheckIntervalMillis(lowRamProfile: LowRamProfile): Long =
    when {
        lowRamProfile.ultra -> UltraGeofenceRuntimeRecheckIntervalMillis
        lowRamProfile.enabled -> LowGeofenceRuntimeRecheckIntervalMillis
        else -> GeofenceRuntimeRecheckIntervalMillis
    }

@Composable
internal fun RuntimeLocationAndClipboardEffects(
    context: Context,
    deviceTimeBaseline: DeviceTimeBaseline,
    deviceTimeBypassState: DeviceTimeBypassState,
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceEnabled: Boolean,
    bypassGeofence: Boolean,
    bypassFakeLocation: Boolean,
    examGuardArmed: Boolean,
    bypassClipboard: Boolean,
    clipboardBypassState: ClipboardBypassState,
    bypassBluetooth: Boolean,
    flowUiState: ExamRuntimeFlowUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    clipboardMainHandler: Handler,
    refreshDeviceTimeSecurity: (String, Boolean) -> DeviceTimeSecurityStatus,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> Unit,
    confirmClipboardViolation: (
        ClipboardSnapshot,
        ClipboardChangeDecision,
        String,
        Boolean,
        String?
    ) -> Unit,
    examAlarmController: ExamAlarmController,
    diagnosticTimestamp: () -> String
) {
    val examSessionStarted = flowUiState.examSessionStarted.value
    val lowRamProfile = LocalLowRamProfile.current

    LaunchedEffect(deviceTimeBaseline, deviceTimeBypassState) {
        refreshDeviceTimeSecurity("runtime_initial", false)
    }

    LaunchedEffect(
        examSessionStarted,
        geofenceConfigParseResult,
        bypassGeofence,
        bypassFakeLocation,
        lowRamProfile
    ) {
        val geofenceMonitoringActive = geofenceEnabled && !bypassGeofence
        val fakeLocationMonitoringActive = !bypassFakeLocation
        if (!examSessionStarted || (!geofenceMonitoringActive && !fakeLocationMonitoringActive)) {
            flowUiState.geofenceRuntimeEpisodeKey.value = null
            flowUiState.fakeLocationRuntimeEpisodeKey.value = null
            if (!examSessionStarted) {
                flowUiState.showGeofenceViolationDialog.value = false
                flowUiState.showFakeLocationViolationDialog.value = false
            }
            if (!geofenceMonitoringActive) {
                flowUiState.geofenceStartValidationInFlight.value = false
                flowUiState.pendingStartExamAfterLocationPermission.value = false
                flowUiState.retryStartExamAfterLocationPermissionGrant.value = false
                flowUiState.showGeofenceViolationDialog.value = false
            }
            if (!fakeLocationMonitoringActive) {
                flowUiState.showFakeLocationViolationDialog.value = false
            }
            return@LaunchedEffect
        }

        while (flowUiState.examSessionStarted.value && (geofenceMonitoringActive || fakeLocationMonitoringActive)) {
            delay(geofenceRuntimeRecheckIntervalMillis(lowRamProfile))
            refreshGeofenceStatus(false, "periodic_recheck", true)
        }
    }

    DisposableEffect(context, examGuardArmed, bypassClipboard, clipboardBypassState) {
        if (!examGuardArmed || clipboardBypassState == ClipboardBypassState.Active || bypassClipboard) {
            clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
            clipboardUiState.clipboardResumeCheckRunnable.value = null
            clipboardUiState.clipboardResumeCheckPending.value = false
            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
            clipboardUiState.clipboardPreBackgroundSignature.value = null
            onDispose { }
        } else {
            val clipboardManager = context.getSystemService(ClipboardManager::class.java)
            val initialSnapshot = readClipboardSnapshotLite(context)
            clipboardUiState.clipboardDecisionFingerprint.value = initialSnapshot.decisionFingerprint
            clipboardUiState.clipboardDecisionSemanticSignature.value = initialSnapshot.semanticSignature
            if (
                clipboardUiState.lastClipboardObservedAt.value == null &&
                clipboardUiState.lastClipboardConfirmedAt.value == null &&
                clipboardUiState.lastClipboardDecision.value == ClipboardChangeDecision.Idle.diagnosticLabel()
            ) {
                clipboardUiState.lastClipboardDecision.value = ClipboardChangeDecision.Idle.diagnosticLabel()
            }
            val listenerAttachedAtElapsedMs = SystemClock.elapsedRealtime()
            var pendingObservedFingerprint: String? = null
            var pendingObservedSemanticSignature: String? = null
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val observedSnapshot = readClipboardSnapshotLite(context)
                clipboardUiState.lastClipboardObservedAt.value = diagnosticTimestamp()
                clipboardUiState.lastClipboardObservedSignature.value = null

                if (
                    observedSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value ||
                    observedSnapshot.decisionFingerprint == clipboardUiState.clipboardDecisionFingerprint.value
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        if (observedSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value) {
                            ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                        } else {
                            ClipboardChangeDecision.IgnoredNoSubstantiveChange.diagnosticLabel()
                        }
                    clipboardUiState.clipboardDecisionFingerprint.value = observedSnapshot.decisionFingerprint
                    clipboardUiState.clipboardDecisionSemanticSignature.value = observedSnapshot.semanticSignature
                    return@OnPrimaryClipChangedListener
                }

                val observedAtElapsedMs = SystemClock.elapsedRealtime()
                if (
                    observedAtElapsedMs - listenerAttachedAtElapsedMs <=
                        ClipboardListenerWarmupIgnoreMillis
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        ClipboardChangeDecision.IgnoredWarmup.diagnosticLabel()
                    clipboardUiState.clipboardDecisionFingerprint.value = observedSnapshot.decisionFingerprint
                    clipboardUiState.clipboardDecisionSemanticSignature.value = observedSnapshot.semanticSignature
                    return@OnPrimaryClipChangedListener
                }

                pendingObservedFingerprint = observedSnapshot.decisionFingerprint
                pendingObservedSemanticSignature = observedSnapshot.semanticSignature
                clipboardUiState.lastClipboardDecision.value =
                    ClipboardChangeDecision.ObservedPending.diagnosticLabel()
                clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                val confirmRunnable = Runnable {
                    val settledSnapshot = readClipboardSnapshotLite(context)
                    val expectedObservedFingerprint = pendingObservedFingerprint
                    val expectedObservedSemanticSignature = pendingObservedSemanticSignature
                    pendingObservedFingerprint = null
                    pendingObservedSemanticSignature = null
                    if (
                        settledSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value ||
                        settledSnapshot.decisionFingerprint == clipboardUiState.clipboardDecisionFingerprint.value ||
                        expectedObservedSemanticSignature == null ||
                        settledSnapshot.semanticSignature != expectedObservedSemanticSignature ||
                        expectedObservedFingerprint == null ||
                        settledSnapshot.decisionFingerprint != expectedObservedFingerprint
                    ) {
                        clipboardUiState.lastClipboardDecision.value =
                            if (settledSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value) {
                                ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                            } else {
                                ClipboardChangeDecision.IgnoredReturnedToBaseline.diagnosticLabel()
                            }
                        clipboardUiState.clipboardDecisionFingerprint.value = settledSnapshot.decisionFingerprint
                        clipboardUiState.clipboardDecisionSemanticSignature.value = settledSnapshot.semanticSignature
                        return@Runnable
                    }

                    confirmClipboardViolation(
                        settledSnapshot,
                        ClipboardChangeDecision.Confirmed,
                        "listener_settle",
                        false,
                        null
                    )
                }
                clipboardUiState.clipboardConfirmRunnable.value = confirmRunnable
                clipboardMainHandler.postDelayed(confirmRunnable, ClipboardSettleWindowMillis)
            }

            clipboardManager?.addPrimaryClipChangedListener(listener)
            onDispose {
                clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                clipboardUiState.clipboardConfirmRunnable.value = null
                runCatching {
                    clipboardManager?.removePrimaryClipChangedListener(listener)
                }
            }
        }
    }

    DisposableEffect(context, examSessionStarted, securityUiState.bluetoothPermissionGranted.value, bypassBluetooth) {
        if (!examSessionStarted || !securityUiState.bluetoothPermissionGranted.value || bypassBluetooth) {
            securityUiState.bluetoothEnabled.value = isBluetoothEnabledForExam(context)
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                        return
                    }

                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    val enabledNow =
                        state == BluetoothAdapter.STATE_ON ||
                            state == BluetoothAdapter.STATE_TURNING_ON

                    securityUiState.bluetoothEnabled.value = enabledNow

                    if (enabledNow) {
                        securityUiState.bluetoothViolationCount.intValue += 1
                        securityUiState.showBluetoothViolationDialog.value = true
                        examAlarmController.start()
                    }
                }
            }
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }
}
