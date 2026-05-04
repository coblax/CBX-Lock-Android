package com.example.coblaxexamlock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.example.coblaxexamlock.runtime.getTimezoneSummary
import kotlin.math.abs

internal const val DeviceTimeDriftThresholdMillis = 120_000L

internal data class DeviceTimeBaseline(
    val wallClockMillis: Long,
    val elapsedRealtimeMillis: Long
)

internal enum class DeviceTimeBypassState {
    Active,
    Inactive,
    Tampered
}

internal object DeviceTimeBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): DeviceTimeBypassState {
        return when {
            tampered -> DeviceTimeBypassState.Tampered
            enabled -> DeviceTimeBypassState.Active
            else -> DeviceTimeBypassState.Inactive
        }
    }
}

internal enum class DeviceTimeSecurityVerdict {
    Safe,
    AutoTimeDisabled,
    AutoTimeZoneDisabled,
    ClockDriftDetected
}

internal data class DeviceTimeSecurityStatus(
    val autoTimeEnabled: Boolean,
    val autoTimeZoneEnabled: Boolean,
    val clockDriftDetected: Boolean,
    val clockDriftMillis: Long,
    val timezoneSummary: String,
    val wallClockNowMillis: Long,
    val elapsedNowMillis: Long,
    val baselineWallClockMillis: Long,
    val baselineElapsedRealtimeMillis: Long,
    val bypassState: DeviceTimeBypassState,
    val finalVerdict: DeviceTimeSecurityVerdict
) {
    val bypassActive: Boolean
        get() = bypassState == DeviceTimeBypassState.Active

    val blocking: Boolean
        get() = !bypassActive && finalVerdict != DeviceTimeSecurityVerdict.Safe
}

internal fun captureDeviceTimeBaseline(): DeviceTimeBaseline {
    return DeviceTimeBaseline(
        wallClockMillis = System.currentTimeMillis(),
        elapsedRealtimeMillis = SystemClock.elapsedRealtime()
    )
}

internal fun inspectDeviceTimeSecurity(
    context: Context,
    baseline: DeviceTimeBaseline,
    bypassState: DeviceTimeBypassState,
    nowWallClockMillis: Long = System.currentTimeMillis(),
    nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
): DeviceTimeSecurityStatus {
    return evaluateDeviceTimeSecurityStatus(
        autoTimeEnabled = readGlobalBoolean(context, Settings.Global.AUTO_TIME),
        autoTimeZoneEnabled = readGlobalBoolean(context, Settings.Global.AUTO_TIME_ZONE),
        baseline = baseline,
        bypassState = bypassState,
        timezoneSummary = getTimezoneSummary(),
        nowWallClockMillis = nowWallClockMillis,
        nowElapsedRealtimeMillis = nowElapsedRealtimeMillis
    )
}

internal fun evaluateDeviceTimeSecurityStatus(
    autoTimeEnabled: Boolean,
    autoTimeZoneEnabled: Boolean,
    baseline: DeviceTimeBaseline,
    bypassState: DeviceTimeBypassState,
    timezoneSummary: String,
    nowWallClockMillis: Long,
    nowElapsedRealtimeMillis: Long
): DeviceTimeSecurityStatus {
    val expectedWallClockMillis =
        baseline.wallClockMillis + (nowElapsedRealtimeMillis - baseline.elapsedRealtimeMillis)
    val clockDriftMillis = abs(nowWallClockMillis - expectedWallClockMillis)
    val clockDriftDetected = clockDriftMillis > DeviceTimeDriftThresholdMillis
    val finalVerdict = when {
        !autoTimeEnabled -> DeviceTimeSecurityVerdict.AutoTimeDisabled
        !autoTimeZoneEnabled -> DeviceTimeSecurityVerdict.AutoTimeZoneDisabled
        clockDriftDetected -> DeviceTimeSecurityVerdict.ClockDriftDetected
        else -> DeviceTimeSecurityVerdict.Safe
    }
    return DeviceTimeSecurityStatus(
        autoTimeEnabled = autoTimeEnabled,
        autoTimeZoneEnabled = autoTimeZoneEnabled,
        clockDriftDetected = clockDriftDetected,
        clockDriftMillis = clockDriftMillis,
        timezoneSummary = timezoneSummary,
        wallClockNowMillis = nowWallClockMillis,
        elapsedNowMillis = nowElapsedRealtimeMillis,
        baselineWallClockMillis = baseline.wallClockMillis,
        baselineElapsedRealtimeMillis = baseline.elapsedRealtimeMillis,
        bypassState = bypassState,
        finalVerdict = finalVerdict
    )
}

private fun readGlobalBoolean(context: Context, key: String): Boolean {
    return runCatching {
        Settings.Global.getInt(context.contentResolver, key, 0) == 1
    }.getOrDefault(false)
}
