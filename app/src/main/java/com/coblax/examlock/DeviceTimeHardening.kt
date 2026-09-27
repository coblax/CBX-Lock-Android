package com.coblax.examlock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.coblax.examlock.runtime.getTimezoneSummary
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
    val finalVerdict: DeviceTimeSecurityVerdict,
    /** A clock jump this check accepted because network time says the clock is right now. */
    val acceptedClockCorrectionMillis: Long = 0L
) {
    val bypassActive: Boolean
        get() = bypassState == DeviceTimeBypassState.Active

    /**
     * A clock change seen while automatic date, time and zone are all on: most likely the
     * student just turned them back on as told, so network time is worth asking before blocking.
     */
    val clockChangeMayBeAFix: Boolean
        get() = finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected &&
            autoTimeEnabled && autoTimeZoneEnabled

    val blocking: Boolean
        get() = !bypassActive && finalVerdict != DeviceTimeSecurityVerdict.Safe
}

/**
 * Clock jumps accepted since this process captured its baseline. The baseline is taken once at
 * launch, so without this a student who opened the app with a wrong clock and then fixed it as
 * instructed stayed blocked as "clock changed" until the app was killed.
 */
internal object DeviceTimeClockCorrection {
    @Volatile
    var acceptedOffsetMillis: Long = 0L
        private set

    fun accept(correctionMillis: Long) {
        acceptedOffsetMillis += correctionMillis
    }

    internal fun resetForTests() {
        acceptedOffsetMillis = 0L
    }
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
    val status = evaluateDeviceTimeSecurityStatus(
        autoTimeEnabled = readGlobalBoolean(context, Settings.Global.AUTO_TIME),
        autoTimeZoneEnabled = readGlobalBoolean(context, Settings.Global.AUTO_TIME_ZONE),
        baseline = baseline,
        bypassState = bypassState,
        timezoneSummary = getTimezoneSummary(),
        nowWallClockMillis = nowWallClockMillis,
        nowElapsedRealtimeMillis = nowElapsedRealtimeMillis,
        acceptedCorrectionMillis = DeviceTimeClockCorrection.acceptedOffsetMillis,
        trustedNetworkNowMillis = TrustedNetworkTimeCoordinator.cachedNowMillis(nowElapsedRealtimeMillis)
    )
    if (status.acceptedClockCorrectionMillis != 0L) {
        DeviceTimeClockCorrection.accept(status.acceptedClockCorrectionMillis)
    }
    return status
}

/**
 * [inspectDeviceTimeSecurity], but a clock change that may be a fix is checked against
 * network time first, so a corrected clock passes on the first try.
 */
internal suspend fun inspectDeviceTimeSecurityConfirmingClockChange(
    context: Context,
    baseline: DeviceTimeBaseline,
    bypassState: DeviceTimeBypassState
): DeviceTimeSecurityStatus {
    val status = inspectDeviceTimeSecurity(context, baseline, bypassState)
    if (!status.clockChangeMayBeAFix) {
        return status
    }
    TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context, forceRefresh = true) ?: return status
    return inspectDeviceTimeSecurity(context, baseline, bypassState)
}

internal fun evaluateDeviceTimeSecurityStatus(
    autoTimeEnabled: Boolean,
    autoTimeZoneEnabled: Boolean,
    baseline: DeviceTimeBaseline,
    bypassState: DeviceTimeBypassState,
    timezoneSummary: String,
    nowWallClockMillis: Long,
    nowElapsedRealtimeMillis: Long,
    acceptedCorrectionMillis: Long = 0L,
    trustedNetworkNowMillis: Long? = null
): DeviceTimeSecurityStatus {
    val expectedWallClockMillis = baseline.wallClockMillis +
        (nowElapsedRealtimeMillis - baseline.elapsedRealtimeMillis) +
        acceptedCorrectionMillis
    val clockDriftMillis = abs(nowWallClockMillis - expectedWallClockMillis)
    // A jump to a clock that network time confirms, with automatic time and zone on, is the
    // student fixing the clock, not spoofing it: there is nothing wrong left to exploit.
    val clockConfirmedByNetwork = trustedNetworkNowMillis != null &&
        abs(nowWallClockMillis - trustedNetworkNowMillis) <= DeviceTimeDriftThresholdMillis
    val clockChangeAccepted = clockDriftMillis > DeviceTimeDriftThresholdMillis &&
        autoTimeEnabled && autoTimeZoneEnabled && clockConfirmedByNetwork
    val clockDriftDetected = clockDriftMillis > DeviceTimeDriftThresholdMillis && !clockChangeAccepted
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
        finalVerdict = finalVerdict,
        acceptedClockCorrectionMillis = if (clockChangeAccepted) {
            nowWallClockMillis - expectedWallClockMillis
        } else {
            0L
        }
    )
}

private fun readGlobalBoolean(context: Context, key: String): Boolean {
    return runCatching {
        Settings.Global.getInt(context.contentResolver, key, 0) == 1
    }.getOrDefault(false)
}
