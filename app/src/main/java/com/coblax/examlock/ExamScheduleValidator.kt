package com.coblax.examlock

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

private const val NetworkTimeDriftThresholdMillis = 30 * 60 * 1000L
private const val IssuedAtFutureToleranceMillis = 5 * 60 * 1000L

enum class ExamScheduleValidationResult {
    Valid,
    NotStarted,
    Finished,
    InvalidSchedule,
    TimeSpoofDetected
}

object ExamScheduleValidator {
    fun validate(
        payload: ExamQrPayload,
        nowMillis: Long,
        networkNowMillis: Long? = null
    ): ExamScheduleValidationResult {
        val scheduleTimeZone = resolveScheduleTimeZone(payload.timezoneId)
        val startMillis = parseDateTimeToMillis(payload.startDateTime, scheduleTimeZone)
        val endMillis = parseDateTimeToMillis(payload.endDateTime, scheduleTimeZone)

        if (startMillis == null || endMillis == null || endMillis < startMillis) {
            return ExamScheduleValidationResult.InvalidSchedule
        }

        if (networkNowMillis != null && abs(nowMillis - networkNowMillis) > NetworkTimeDriftThresholdMillis) {
            return ExamScheduleValidationResult.TimeSpoofDetected
        }

        val effectiveNowMillis = networkNowMillis ?: nowMillis
        if (payload.issuedAt > 0L && payload.issuedAt - effectiveNowMillis > IssuedAtFutureToleranceMillis) {
            return ExamScheduleValidationResult.TimeSpoofDetected
        }
        return when {
            effectiveNowMillis < startMillis -> ExamScheduleValidationResult.NotStarted
            effectiveNowMillis > endMillis -> ExamScheduleValidationResult.Finished
            else -> ExamScheduleValidationResult.Valid
        }
    }

    internal fun validateAfterDeviceTimeCheck(
        payload: ExamQrPayload,
        deviceTimeStatus: DeviceTimeSecurityStatus,
        networkNowMillis: Long? = null
    ): ExamScheduleValidationResult {
        if (deviceTimeStatus.blocking) {
            return ExamScheduleValidationResult.TimeSpoofDetected
        }
        return validate(
            payload = payload,
            nowMillis = deviceTimeStatus.wallClockNowMillis,
            networkNowMillis = networkNowMillis
        )
    }

    private fun resolveScheduleTimeZone(timezoneId: String): TimeZone {
        if (timezoneId.isBlank()) {
            return TimeZone.getDefault()
        }
        val tz = TimeZone.getTimeZone(timezoneId)
        // TimeZone.getTimeZone returns "GMT" for unrecognized IDs — treat that as fallback
        if (tz.id == "GMT" && !timezoneId.equals("GMT", ignoreCase = true)) {
            android.util.Log.w("ExamScheduleValidator",
                "Unrecognized timezone ID '$timezoneId'; falling back to device timezone '${TimeZone.getDefault().id}'. " +
                "Exam schedule may be evaluated in an unexpected timezone.")
            return TimeZone.getDefault()
        }
        return tz
    }

    private fun parseDateTimeToMillis(value: String, timeZone: TimeZone): Long? {
        val rawValue = value.trim()
        if (rawValue.isBlank()) {
            return null
        }

        val sections = rawValue.split(" ")
        if (sections.size != 2) {
            return null
        }

        val dateParts = sections[0].split("/")
        val timeParts = sections[1].split(":")
        if (dateParts.size != 3 || timeParts.size != 2) {
            return null
        }

        val day = dateParts[0].toIntOrNull() ?: return null
        val month = dateParts[1].toIntOrNull() ?: return null
        val year = dateParts[2].toIntOrNull() ?: return null
        val hour = timeParts[0].toIntOrNull() ?: return null
        val minute = timeParts[1].toIntOrNull() ?: return null

        return runCatching {
            Calendar.getInstance(timeZone).apply {
                isLenient = false
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }
}
