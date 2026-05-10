package com.example.coblaxexamlock.ui.exam

import android.os.SystemClock
import android.util.Log
import com.example.coblaxexamlock.BuildConfig

private const val ExamStartPerfTag = "ExamStartPerf"
internal const val RuntimeMemoryPerfTag = "RuntimeMemory"

internal fun <T> debugMeasureExamStartWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                ExamStartPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

internal suspend fun <T> debugMeasureExamStartSuspendWork(
    label: String,
    block: suspend () -> T
): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                ExamStartPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

internal fun debugLogExamStart(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(ExamStartPerfTag, message)
    }
}
