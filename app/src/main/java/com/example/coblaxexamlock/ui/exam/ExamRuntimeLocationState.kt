package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import kotlinx.coroutines.delay

private const val StartExamWarmLocationReuseWindowMillis = 12_000L
internal const val PreparationLocationWarmupIntervalMillis = 10_000L

internal data class WarmLocationValidationCache(
    val result: SplitLocationSecurityStatus,
    val validationKey: String,
    val completedAtElapsedMs: Long,
    val completedAtTimestamp: String
)

internal fun buildWarmLocationValidationPolicySignature(
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState
): String {
    val config = geofenceConfigParseResult.config
    val verticesSignature = config?.vertices?.joinToString(";") {
        "${it.latitude},${it.longitude}"
    } ?: "-"
    val circleCentersSignature = config?.circleCenters?.joinToString(";") {
        "${it.latitude},${it.longitude}"
    } ?: "-"
    return buildString {
        append("enabled=").append(geofenceConfigParseResult.enabled)
        append("|error=").append(geofenceConfigParseResult.error ?: "-")
        append("|shape=").append(config?.shapeType?.name ?: "-")
        append("|center=").append(config?.centerLat ?: "-")
        append(',').append(config?.centerLng ?: "-")
        append("|radius=").append(config?.radiusMeters ?: "-")
        append("|vertices=").append(verticesSignature)
        append("|circle_centers=").append(circleCentersSignature)
        append("|geofence_bypass=").append(geofenceBypassState.name)
        append("|fake_location_bypass=").append(fakeLocationBypassState.name)
    }
}

internal fun buildWarmLocationValidationKey(
    permissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    policySignature: String
): String {
    return buildString {
        append(policySignature)
        append("|permission=").append(permissionGranted)
        append("|location_services=").append(locationServicesEnabled)
    }
}

private fun SplitLocationSecurityStatus.reuseBlockingReason(): String? {
    return when {
        !geofenceStatus.safe -> "geofence_unsafe"
        !fakeLocationStatus.safe -> "fake_location_unsafe"
        !geofenceStatus.fixQualityStatus.usableForGeofence -> "geofence_fix_quality_unusable"
        !fakeLocationStatus.fixQualityEligible -> "fake_location_fix_quality_ineligible"
        else -> null
    }
}

internal fun WarmLocationValidationCache.reuseFailureReason(
    currentValidationKey: String,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): String? {
    val ageMs = (nowElapsedMs - completedAtElapsedMs).coerceAtLeast(0L)
    return when {
        validationKey != currentValidationKey -> "location_inputs_changed"
        ageMs > StartExamWarmLocationReuseWindowMillis -> "warm_cache_stale"
        else -> result.reuseBlockingReason()
    }
}

internal fun WarmLocationValidationCache.isReusableForStart(
    currentValidationKey: String,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return reuseFailureReason(
        currentValidationKey = currentValidationKey,
        nowElapsedMs = nowElapsedMs
    ) == null
}

internal class ExamRuntimeLocationWarmupUiState(
    val locationWarmupInFlight: MutableState<Boolean>,
    val reusableWarmLocationValidation: MutableState<WarmLocationValidationCache?>
)

@Composable
internal fun rememberExamRuntimeLocationWarmupUiState(): ExamRuntimeLocationWarmupUiState {
    val locationWarmupInFlight = rememberSaveable { mutableStateOf(false) }
    val reusableWarmLocationValidation = remember { mutableStateOf<WarmLocationValidationCache?>(null) }
    return remember {
        ExamRuntimeLocationWarmupUiState(
            locationWarmupInFlight = locationWarmupInFlight,
            reusableWarmLocationValidation = reusableWarmLocationValidation
        )
    }
}

@Composable
internal fun PreparationLocationWarmupEffect(
    context: Context,
    examSessionStarted: Boolean,
    geofenceEnabled: Boolean,
    warmLocationPolicySignature: String,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState,
    geofencePermissionRequestInFlight: Boolean,
    geofenceStartValidationInFlight: Boolean,
    geofenceManualRefreshInFlight: Boolean,
    webViewSessionResetInFlight: Boolean,
    locationWarmupInFlight: Boolean,
    warmupIntervalMillis: Long,
    updateLocationWarmupInFlight: (Boolean) -> Unit,
    updateReusableWarmLocationValidation: (WarmLocationValidationCache?) -> Unit,
    updateLastGeofenceRefreshAt: (String?) -> Unit,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> SplitLocationSecurityStatus
) {
    LaunchedEffect(
        examSessionStarted,
        geofenceEnabled,
        warmLocationPolicySignature,
        geofenceBypassState,
        fakeLocationBypassState,
        geofencePermissionRequestInFlight,
        geofenceStartValidationInFlight,
        geofenceManualRefreshInFlight,
        webViewSessionResetInFlight,
        warmupIntervalMillis
    ) {
        val geofenceMonitoringActive =
            geofenceEnabled && geofenceBypassState != GeofenceBypassState.Active
        val fakeLocationMonitoringActive =
            fakeLocationBypassState != FakeLocationBypassState.Active

        if (examSessionStarted || (!geofenceMonitoringActive && !fakeLocationMonitoringActive)) {
            updateLocationWarmupInFlight(false)
            updateReusableWarmLocationValidation(null)
            return@LaunchedEffect
        }

        while (!examSessionStarted && (geofenceMonitoringActive || fakeLocationMonitoringActive)) {
            val permissionsReady = hasLocationPermissionForWifi(context)
            val servicesEnabled = isLocationServicesEnabled(context)
            val canWarmNow =
                permissionsReady &&
                    servicesEnabled &&
                    !geofencePermissionRequestInFlight &&
                    !geofenceStartValidationInFlight &&
                    !geofenceManualRefreshInFlight &&
                    !webViewSessionResetInFlight &&
                    !locationWarmupInFlight

            if (canWarmNow) {
                updateLocationWarmupInFlight(true)
                try {
                    val warmStatus = debugMeasureExamStartSuspendWork("preparationWarmup:location_validation") {
                        refreshGeofenceStatus(true, "preparation_warmup", false)
                    }
                    val completedAtElapsedMs = SystemClock.elapsedRealtime()
                    val completedAtTimestamp = diagnosticTimestamp()
                    updateLastGeofenceRefreshAt(completedAtTimestamp)
                    val warmValidationKey = buildWarmLocationValidationKey(
                        permissionGranted = permissionsReady,
                        locationServicesEnabled = servicesEnabled,
                        policySignature = warmLocationPolicySignature
                    )
                    updateReusableWarmLocationValidation(
                        WarmLocationValidationCache(
                            result = warmStatus,
                            validationKey = warmValidationKey,
                            completedAtElapsedMs = completedAtElapsedMs,
                            completedAtTimestamp = completedAtTimestamp
                        ).takeIf {
                            it.isReusableForStart(
                                currentValidationKey = warmValidationKey,
                                nowElapsedMs = completedAtElapsedMs
                            )
                        }
                    )
                } finally {
                    updateLocationWarmupInFlight(false)
                }
            } else if (!permissionsReady || !servicesEnabled) {
                updateReusableWarmLocationValidation(null)
            }

            delay(warmupIntervalMillis)
        }
    }
}
