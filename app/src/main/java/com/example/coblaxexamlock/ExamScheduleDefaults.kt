package com.example.coblaxexamlock

import java.util.Calendar
import java.util.Locale

internal data class ExamScheduleWindow(
    val startDateTime: String,
    val endDateTime: String
)

internal object ExamScheduleDefaults {
    const val DEFAULT_START_BACKDATE_MS: Long = 5 * 60 * 1000L
    const val DEFAULT_QR_DURATION_MS: Long = 2 * 60 * 60 * 1000L
    const val DEFAULT_DIRECT_LINK_DURATION_MS: Long = 24 * 60 * 60 * 1000L

    fun defaultQrWindow(nowMillis: Long = System.currentTimeMillis()): ExamScheduleWindow =
        defaultActiveWindow(nowMillis = nowMillis, durationMillis = DEFAULT_QR_DURATION_MS)

    fun defaultDirectLinkWindow(nowMillis: Long = System.currentTimeMillis()): ExamScheduleWindow =
        defaultActiveWindow(nowMillis = nowMillis, durationMillis = DEFAULT_DIRECT_LINK_DURATION_MS)

    fun defaultActiveWindow(
        nowMillis: Long = System.currentTimeMillis(),
        durationMillis: Long
    ): ExamScheduleWindow {
        return ExamScheduleWindow(
            startDateTime = formatExamScheduleDateTime(nowMillis - DEFAULT_START_BACKDATE_MS),
            endDateTime = formatExamScheduleDateTime(nowMillis + durationMillis)
        )
    }
}

internal fun formatExamScheduleDateTime(millis: Long): String {
    return formatExamScheduleDateTime(
        Calendar.getInstance().apply {
            timeInMillis = millis
        }
    )
}

internal fun formatExamScheduleDateTime(calendar: Calendar): String {
    return String.format(
        Locale.US,
        "%02d/%02d/%04d %02d:%02d",
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}
