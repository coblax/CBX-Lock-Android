package com.coblax.examlock.installer

import android.app.Application

/** Keeps a report of any crash, so even the installer failing reaches the admins. */
class InstallerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { Diagnostics.recordCrash(this, thread, error) }
            previous?.uncaughtException(thread, error)
        }
        // A report kept offline goes out as soon as there is a connection.
        if (TelegramReporter.pendingCount(this) > 0) {
            ReportRetryJobService.schedule(this)
        }
    }
}
