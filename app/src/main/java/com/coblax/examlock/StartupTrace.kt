package com.coblax.examlock

import android.os.SystemClock
import android.os.Trace
import android.util.Log

internal object StartupTrace {
    private const val Tag = "StartupTimeline"
    private val processStartMs = SystemClock.elapsedRealtime()

    fun mark(name: String, extra: String = "") {
        val nowMs = SystemClock.elapsedRealtime()
        val safeExtra = extra.trim()
        val suffix = if (safeExtra.isNotEmpty()) " | $safeExtra" else ""
        Log.println(
            Log.INFO,
            Tag,
            "event=$name | elapsed_ms=${nowMs - processStartMs} | uptime_ms=$nowMs$suffix"
        )
    }

    fun <T> section(name: String, block: () -> T): T {
        val traceName = name.take(127)
        Trace.beginSection(traceName)
        return try {
            mark("${name}_start")
            block()
        } finally {
            try {
                mark("${name}_end")
            } finally {
                Trace.endSection()
            }
        }
    }
}
