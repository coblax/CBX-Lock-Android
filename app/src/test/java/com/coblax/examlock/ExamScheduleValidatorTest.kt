package com.coblax.examlock

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamScheduleValidatorTest {
    @Test
    fun validWhenCurrentTimeInsideExamWindow() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 8, 0)
        )

        assertEquals(ExamScheduleValidationResult.Valid, result)
    }

    @Test
    fun invalidWhenExamHasNotStartedYet() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 6, 59)
        )

        assertEquals(ExamScheduleValidationResult.NotStarted, result)
    }

    @Test
    fun invalidWhenExamAlreadyFinished() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 9, 1)
        )

        assertEquals(ExamScheduleValidationResult.Finished, result)
    }

    @Test
    fun invalidWhenExamScheduleIsBroken() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 10:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 8, 0)
        )

        assertEquals(ExamScheduleValidationResult.InvalidSchedule, result)
    }

    @Test
    fun defaultQrWindowIsActiveImmediately() {
        val nowMillis = millisOf(10, 3, 2026, 8, 0)
        val scheduleWindow = ExamScheduleDefaults.defaultQrWindow(nowMillis = nowMillis)
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = scheduleWindow.startDateTime,
            endDateTime = scheduleWindow.endDateTime,
            issuedAt = nowMillis
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = nowMillis
        )

        assertEquals(ExamScheduleValidationResult.Valid, result)
    }

    @Test
    fun defaultDirectLinkWindowIsActiveImmediately() {
        val nowMillis = millisOf(10, 3, 2026, 8, 0)
        val scheduleWindow = ExamScheduleDefaults.defaultDirectLinkWindow(nowMillis = nowMillis)
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Direct Link",
            startDateTime = scheduleWindow.startDateTime,
            endDateTime = scheduleWindow.endDateTime,
            issuedAt = nowMillis
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = nowMillis
        )

        assertEquals(ExamScheduleValidationResult.Valid, result)
    }

    @Test
    fun invalidWhenNetworkTimeDiffersTooFarFromLocalClock() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 11:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 8, 0),
            networkNowMillis = millisOf(10, 3, 2026, 8, 31)
        )

        assertEquals(ExamScheduleValidationResult.TimeSpoofDetected, result)
    }

    @Test
    fun networkTimeBecomesEffectiveScheduleTimeWhenTrusted() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 7, 5),
            networkNowMillis = millisOf(10, 3, 2026, 6, 59)
        )

        assertEquals(ExamScheduleValidationResult.NotStarted, result)
    }

    @Test
    fun invalidWhenQrIssuedAtIsTooFarInTheFuture() {
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 8, 10)
        )

        val result = ExamScheduleValidator.validate(
            payload = payload,
            nowMillis = millisOf(10, 3, 2026, 8, 0)
        )

        assertEquals(ExamScheduleValidationResult.TimeSpoofDetected, result)
    }

    @Test
    fun invalidWhenDeviceTimeStatusIsBlocking() {
        val nowMillis = millisOf(10, 3, 2026, 8, 0)
        val payload = ExamQrPayload(
            examUrl = "https://example.com",
            examName = "Simulasi",
            startDateTime = "10/03/2026 07:00",
            endDateTime = "10/03/2026 09:00",
            issuedAt = millisOf(10, 3, 2026, 6, 0)
        )
        val deviceTimeStatus = DeviceTimeSecurityStatus(
            autoTimeEnabled = false,
            autoTimeZoneEnabled = true,
            clockDriftDetected = false,
            clockDriftMillis = 0L,
            timezoneSummary = "UTC+07:00",
            wallClockNowMillis = nowMillis,
            elapsedNowMillis = 1_000L,
            baselineWallClockMillis = nowMillis,
            baselineElapsedRealtimeMillis = 1_000L,
            bypassState = DeviceTimeBypassState.Inactive,
            finalVerdict = DeviceTimeSecurityVerdict.AutoTimeDisabled
        )

        val result = ExamScheduleValidator.validateAfterDeviceTimeCheck(
            payload = payload,
            deviceTimeStatus = deviceTimeStatus
        )

        assertEquals(ExamScheduleValidationResult.TimeSpoofDetected, result)
    }

    private fun millisOf(
        day: Int,
        month: Int,
        year: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
