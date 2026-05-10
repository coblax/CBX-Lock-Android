package com.example.coblaxexamlock.ui.exam

import android.provider.Settings
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.ExamScheduleValidationResult
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.UiLanguage
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun buildDeviceTimeEventDetails(
    trigger: String,
    status: DeviceTimeSecurityStatus
): String {
    return buildString {
        append("trigger=")
        append(trigger)
        append(" | verdict=")
        append(status.finalVerdict.name.lowercase(Locale.US))
        append(" | auto_time=")
        append(if (status.autoTimeEnabled) "on" else "off")
        append(" | auto_time_zone=")
        append(if (status.autoTimeZoneEnabled) "on" else "off")
        append(" | drift_ms=")
        append(status.clockDriftMillis)
        append(" | timezone=")
        append(status.timezoneSummary)
        append(" | bypass=")
        append(status.bypassState.name.lowercase(Locale.US))
    }
}

internal fun deviceTimeBlockedTitle(uiLanguage: UiLanguage): String {
    return localized(
        uiLanguage,
        "Device Time Check Required",
        "Pemeriksaan Waktu Perangkat Diperlukan"
    )
}

internal fun deviceTimeBlockedMessage(
    uiLanguage: UiLanguage,
    status: DeviceTimeSecurityStatus
): String {
    return when {
        status.bypassState == DeviceTimeBypassState.Tampered -> localized(
            uiLanguage,
            "Device Time bypass storage was tampered with. Device Time enforcement remains active.",
            "Tamper terdeteksi pada storage bypass Waktu Perangkat. Enforcement Waktu Perangkat tetap aktif."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> localized(
            uiLanguage,
            "Turn on automatic date & time before starting the exam.",
            "Aktifkan tanggal & waktu otomatis sebelum memulai ujian."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> localized(
            uiLanguage,
            "Turn on automatic time zone before starting the exam.",
            "Aktifkan zona waktu otomatis sebelum memulai ujian."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> localized(
            uiLanguage,
            "A suspicious clock change was detected. Turn automatic date & time back on, then try again.",
            "Terdeteksi perubahan jam yang mencurigakan. Aktifkan kembali tanggal & waktu otomatis, lalu coba lagi."
        )
        else -> localized(
            uiLanguage,
            "Device time could not be trusted. Check the date & time settings, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Periksa pengaturan tanggal & waktu, lalu coba lagi."
        )
    }
}

internal fun scheduleBlockedMessage(
    uiLanguage: UiLanguage,
    payload: ExamQrPayload,
    validationResult: ExamScheduleValidationResult
): String {
    return when (validationResult) {
        ExamScheduleValidationResult.NotStarted -> localized(
            uiLanguage,
            "This exam has not started yet. It becomes active at ${payload.startDateTime}.",
            "Ujian ini belum dimulai. Ujian baru aktif pada ${payload.startDateTime}."
        )
        ExamScheduleValidationResult.Finished -> localized(
            uiLanguage,
            "This exam is no longer valid. It ended at ${payload.endDateTime}.",
            "Ujian ini sudah tidak berlaku. Ujian berakhir pada ${payload.endDateTime}."
        )
        ExamScheduleValidationResult.InvalidSchedule -> localized(
            uiLanguage,
            "This exam QR has an invalid schedule. Check the start and end time in Custom QR.",
            "QR ujian ini memiliki jadwal yang tidak valid. Periksa waktu mulai dan selesai di Custom QR."
        )
        ExamScheduleValidationResult.TimeSpoofDetected -> localized(
            uiLanguage,
            "Device time could not be trusted. Enable automatic date, time, and time zone, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Aktifkan tanggal, waktu, dan zona waktu otomatis, lalu coba lagi."
        )
        ExamScheduleValidationResult.Valid -> localized(
            uiLanguage,
            "The exam schedule is valid.",
            "Jadwal ujian valid."
        )
    }
}
