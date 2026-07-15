package com.coblax.examlock.ui.exam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import com.coblax.examlock.config.NetworkUnstableRecoveryQuietPeriodMillis
import com.coblax.examlock.config.OfflineTooLongWarningThresholdMillis
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.runtime.readExamBatteryStatus
import kotlinx.coroutines.delay

internal const val NetworkReadinessPollingStableIntervalMillis = 30_000L
internal const val NetworkReadinessPollingUnstableIntervalMillis = 5_000L
internal const val NetworkReadinessPollingCallbackDebounceMillis = 800L

/**
 * Returns a polling interval multiplier based on battery state.
 * - Charging: no slowdown (1x)
 * - Battery < 15%: triple the interval (3x) to conserve power
 * - Battery < 30%: double the interval (2x) to reduce load
 * - Ultra low-RAM profile: double the interval (2x) regardless of battery
 * - Otherwise: no slowdown (1x)
 *
 * This prevents excessive CPU wake-ups on low-battery devices during long exams.
 */
internal fun adaptiveBatteryPollingMultiplier(
    batteryPercent: Int,
    isCharging: Boolean,
    lowRamUltra: Boolean
): Long {
    return when {
        isCharging -> 1L
        batteryPercent < 15 -> 3L
        batteryPercent < 30 -> 2L
        lowRamUltra -> 2L
        else -> 1L
    }
}

internal class ExamRuntimeNetworkUiState(
    val networkUnstableEpisodeStartedAt: MutableState<String?>,
    val networkUnstableEpisodeStartedElapsedMs: MutableState<Long?>,
    val networkUnstableLastFlapAt: MutableState<String?>,
    val networkUnstableLastFlapElapsedMs: MutableState<Long?>,
    val networkUnstableWarningShown: MutableState<Boolean>,
    val lastNetworkUnstableWarningAt: MutableState<String?>,
    val showNetworkUnstableDialog: MutableState<Boolean>,
    val networkUnstableFlapCount: MutableIntState,
    val networkUnstableLastTransportLabel: MutableState<String?>,
    val lastNetworkChangeAt: MutableState<String?>,
    val lastNetworkChangeSource: MutableState<String?>,
    val networkManualRefreshInFlight: MutableState<Boolean>,
    val lastConnectedNetworkLabel: MutableState<String?>,
    val offlineStartedAtElapsedMs: MutableState<Long?>,
    val offlineStartedAtTimestamp: MutableState<String?>,
    val offlineWarningShown: MutableState<Boolean>,
    val lastOfflineWarningAt: MutableState<String?>,
    val lastOfflineWarningElapsedMs: MutableState<Long?>,
    val lastOfflineDurationMs: MutableState<Long?>,
    val offlineWarningDurationMs: MutableState<Long?>,
    val showOfflineWarningDialog: MutableState<Boolean>
)

@Composable
internal fun rememberExamRuntimeNetworkUiState(
    baseNetworkReadiness: NetworkReadinessStatus
): ExamRuntimeNetworkUiState {
    val networkUnstableEpisodeStartedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val networkUnstableEpisodeStartedElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val networkUnstableLastFlapAt = rememberSaveable { mutableStateOf<String?>(null) }
    val networkUnstableLastFlapElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val networkUnstableWarningShown = rememberSaveable { mutableStateOf(false) }
    val lastNetworkUnstableWarningAt = rememberSaveable { mutableStateOf<String?>(null) }
    val showNetworkUnstableDialog = rememberSaveable { mutableStateOf(false) }
    val networkUnstableFlapCount = rememberSaveable { mutableIntStateOf(0) }
    val networkUnstableLastTransportLabel = rememberSaveable { mutableStateOf<String?>(null) }
    val lastNetworkChangeAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastNetworkChangeSource = rememberSaveable { mutableStateOf<String?>(null) }
    val networkManualRefreshInFlight = rememberSaveable { mutableStateOf(false) }
    val lastConnectedNetworkLabel = rememberSaveable {
        mutableStateOf<String?>(
            baseNetworkReadiness.transportLabel.takeIf { baseNetworkReadiness.examStatus.isConnected }
        )
    }
    val offlineStartedAtElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val offlineStartedAtTimestamp = rememberSaveable { mutableStateOf<String?>(null) }
    val offlineWarningShown = rememberSaveable { mutableStateOf(false) }
    val lastOfflineWarningAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOfflineWarningElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val lastOfflineDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val offlineWarningDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val showOfflineWarningDialog = rememberSaveable { mutableStateOf(false) }
    return remember {
        ExamRuntimeNetworkUiState(
            networkUnstableEpisodeStartedAt = networkUnstableEpisodeStartedAt,
            networkUnstableEpisodeStartedElapsedMs = networkUnstableEpisodeStartedElapsedMs,
            networkUnstableLastFlapAt = networkUnstableLastFlapAt,
            networkUnstableLastFlapElapsedMs = networkUnstableLastFlapElapsedMs,
            networkUnstableWarningShown = networkUnstableWarningShown,
            lastNetworkUnstableWarningAt = lastNetworkUnstableWarningAt,
            showNetworkUnstableDialog = showNetworkUnstableDialog,
            networkUnstableFlapCount = networkUnstableFlapCount,
            networkUnstableLastTransportLabel = networkUnstableLastTransportLabel,
            lastNetworkChangeAt = lastNetworkChangeAt,
            lastNetworkChangeSource = lastNetworkChangeSource,
            networkManualRefreshInFlight = networkManualRefreshInFlight,
            lastConnectedNetworkLabel = lastConnectedNetworkLabel,
            offlineStartedAtElapsedMs = offlineStartedAtElapsedMs,
            offlineStartedAtTimestamp = offlineStartedAtTimestamp,
            offlineWarningShown = offlineWarningShown,
            lastOfflineWarningAt = lastOfflineWarningAt,
            lastOfflineWarningElapsedMs = lastOfflineWarningElapsedMs,
            lastOfflineDurationMs = lastOfflineDurationMs,
            offlineWarningDurationMs = offlineWarningDurationMs,
            showOfflineWarningDialog = showOfflineWarningDialog
        )
    }
}

@Composable
internal fun RuntimeConnectivityEffects(
    context: Context,
    examSessionStarted: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    baseNetworkReadiness: NetworkReadinessStatus,
    networkUiState: ExamRuntimeNetworkUiState,
    batteryStatusState: MutableState<ExamBatteryStatus>,
    networkMainHandler: Handler,
    updateNetworkReadiness: (String) -> Unit,
    currentNetworkPollingIntervalMillis: () -> Long,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    currentNetworkEventDetails: (String, NetworkReadinessStatus, String?) -> String,
    clearNetworkFlapHistory: () -> Unit,
    diagnosticTimestamp: () -> String
) {
    val networkStatus = networkReadinessStatus.examStatus

    DisposableEffect(context, examSessionStarted) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        var lastCallbackPostElapsedMs = 0L
        // Track the pending debounced runnable so we can cancel it properly.
        // The previous approach used removeCallbacksAndMessages("network_debounce")
        // which never matched because callbacks were posted without a token object.
        var pendingDebouncedRunnable: Runnable? = null
        val pushNetworkStatusUpdate = { source: String ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastCallbackPostElapsedMs >= NetworkReadinessPollingCallbackDebounceMillis) {
                lastCallbackPostElapsedMs = now
                pendingDebouncedRunnable?.let(networkMainHandler::removeCallbacks)
                pendingDebouncedRunnable = null
                networkMainHandler.post {
                    updateNetworkReadiness(source)
                }
            } else {
                pendingDebouncedRunnable?.let(networkMainHandler::removeCallbacks)
                val runnable = Runnable {
                    lastCallbackPostElapsedMs = SystemClock.elapsedRealtime()
                    pendingDebouncedRunnable = null
                    updateNetworkReadiness(source)
                }
                pendingDebouncedRunnable = runnable
                networkMainHandler.postDelayed(
                    runnable,
                    NetworkReadinessPollingCallbackDebounceMillis
                )
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                pushNetworkStatusUpdate("callback_available")
            }

            override fun onLost(network: Network) {
                pushNetworkStatusUpdate("callback_lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                pushNetworkStatusUpdate("callback_capabilities")
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties
            ) {
                pushNetworkStatusUpdate("callback_link_properties")
            }

            override fun onUnavailable() {
                pushNetworkStatusUpdate("callback_unavailable")
            }
        }

        networkMainHandler.post { updateNetworkReadiness("initial") }
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        }

        onDispose {
            networkMainHandler.removeCallbacksAndMessages(null)
            runCatching {
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
    }

    LaunchedEffect(context, examSessionStarted) {
        if (!examSessionStarted) {
            return@LaunchedEffect
        }
        // Stagger: offset this loop by 900ms relative to other polling loops
        // to prevent all loops from waking up and consuming CPU simultaneously.
        delay(900L)
        while (true) {
            delay(currentNetworkPollingIntervalMillis())
            updateNetworkReadiness("poll")
        }
    }

    LaunchedEffect(examSessionStarted, networkStatus.isConnected, networkStatus.label) {
        if (!examSessionStarted) {
            networkUiState.offlineStartedAtElapsedMs.value = null
            networkUiState.offlineStartedAtTimestamp.value = null
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
            networkUiState.showNetworkUnstableDialog.value = false
            if (networkStatus.isConnected) {
                networkUiState.lastConnectedNetworkLabel.value = networkReadinessStatus.transportLabel
            }
            return@LaunchedEffect
        }

        if (networkStatus.isConnected) {
            networkUiState.lastConnectedNetworkLabel.value = networkReadinessStatus.transportLabel
            val previousOfflineStarted = networkUiState.offlineStartedAtElapsedMs.value
            if (previousOfflineStarted != null) {
                val recoveredDurationMs =
                    (SystemClock.elapsedRealtime() - previousOfflineStarted).coerceAtLeast(0L)
                recordAction(
                    "NETWORK_OFFLINE_RECOVERED",
                    buildString {
                        append("transport=")
                        append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                        append(" | duration_ms=")
                        append(recoveredDurationMs)
                        append(" | warning_shown=")
                        append(if (networkUiState.offlineWarningShown.value) "yes" else "no")
                    },
                    DiagnosticEventLevel.INFO
                )
            }
            networkUiState.offlineStartedAtElapsedMs.value = null
            networkUiState.offlineStartedAtTimestamp.value = null
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
        } else if (networkUiState.offlineStartedAtElapsedMs.value == null) {
            networkUiState.offlineStartedAtElapsedMs.value = SystemClock.elapsedRealtime()
            networkUiState.offlineStartedAtTimestamp.value = diagnosticTimestamp()
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
            recordAction(
                "NETWORK_OFFLINE_STARTED",
                buildString {
                    append("last_transport=")
                    append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                    append(" | threshold_ms=")
                    append(OfflineTooLongWarningThresholdMillis)
                },
                DiagnosticEventLevel.WARNING
            )
        }
    }

    LaunchedEffect(
        examSessionStarted,
        networkStatus.isConnected,
        networkUiState.offlineStartedAtElapsedMs.value,
        networkUiState.lastOfflineWarningElapsedMs.value,
        networkUiState.showOfflineWarningDialog.value
    ) {
        val startedAt = networkUiState.offlineStartedAtElapsedMs.value ?: return@LaunchedEffect
        val previousWarningElapsed = networkUiState.lastOfflineWarningElapsedMs.value
        if (!examSessionStarted || networkStatus.isConnected || networkUiState.showOfflineWarningDialog.value) {
            return@LaunchedEffect
        }
        val referenceElapsed = previousWarningElapsed ?: startedAt
        val elapsedMs = (SystemClock.elapsedRealtime() - referenceElapsed).coerceAtLeast(0L)
        val remainingMs = OfflineTooLongWarningThresholdMillis - elapsedMs
        if (remainingMs > 0L) {
            delay(remainingMs)
        }

        if (
            examSessionStarted &&
            !networkStatus.isConnected &&
            networkUiState.offlineStartedAtElapsedMs.value == startedAt &&
            networkUiState.lastOfflineWarningElapsedMs.value == previousWarningElapsed &&
            !networkUiState.showOfflineWarningDialog.value
        ) {
            val warningElapsed = SystemClock.elapsedRealtime()
            val warningDurationMs = (warningElapsed - startedAt).coerceAtLeast(0L)
            networkUiState.offlineWarningShown.value = true
            networkUiState.lastOfflineWarningAt.value = diagnosticTimestamp()
            networkUiState.lastOfflineWarningElapsedMs.value = warningElapsed
            networkUiState.lastOfflineDurationMs.value = warningDurationMs
            networkUiState.offlineWarningDurationMs.value = warningDurationMs
            networkUiState.showOfflineWarningDialog.value = true
            recordAction(
                "NETWORK_OFFLINE_TOO_LONG_WARNING",
                buildString {
                    append("last_transport=")
                    append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                    append(" | duration_ms=")
                    append(warningDurationMs)
                    append(" | threshold_ms=")
                    append(OfflineTooLongWarningThresholdMillis)
                },
                DiagnosticEventLevel.WARNING
            )
        }
    }

    LaunchedEffect(
        networkStatus.isConnected,
        networkUiState.networkUnstableEpisodeStartedElapsedMs.value,
        networkUiState.networkUnstableLastFlapElapsedMs.value
    ) {
        val episodeStartedAt = networkUiState.networkUnstableEpisodeStartedElapsedMs.value ?: return@LaunchedEffect
        val lastFlapElapsed = networkUiState.networkUnstableLastFlapElapsedMs.value ?: return@LaunchedEffect
        if (!networkStatus.isConnected) {
            return@LaunchedEffect
        }
        val elapsedSinceLastFlap = (SystemClock.elapsedRealtime() - lastFlapElapsed).coerceAtLeast(0L)
        val remainingMs = NetworkUnstableRecoveryQuietPeriodMillis - elapsedSinceLastFlap
        if (remainingMs > 0L) {
            delay(remainingMs)
        }
        if (
            networkStatus.isConnected &&
            networkUiState.networkUnstableEpisodeStartedElapsedMs.value == episodeStartedAt &&
            networkUiState.networkUnstableLastFlapElapsedMs.value == lastFlapElapsed
        ) {
            recordAction(
                "NETWORK_UNSTABLE_EPISODE_RECOVERED",
                currentNetworkEventDetails(
                    "unstable_recovered",
                    baseNetworkReadiness,
                    "flap_count=${networkUiState.networkUnstableFlapCount.intValue}"
                ),
                DiagnosticEventLevel.INFO
            )
            networkUiState.networkUnstableEpisodeStartedElapsedMs.value = null
            networkUiState.networkUnstableEpisodeStartedAt.value = null
            networkUiState.networkUnstableWarningShown.value = false
            clearNetworkFlapHistory()
            networkUiState.networkUnstableFlapCount.intValue = 0
        }
    }

    DisposableEffect(context, examSessionStarted) {
        if (!examSessionStarted) {
            batteryStatusState.value = readExamBatteryStatus(context)
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    batteryStatusState.value = readExamBatteryStatus(intent)
                }
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            batteryStatusState.value = readExamBatteryStatus(stickyIntent)

            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }
}
