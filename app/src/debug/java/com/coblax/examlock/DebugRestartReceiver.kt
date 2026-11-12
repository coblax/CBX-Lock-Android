package com.coblax.examlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log

/**
 * Debug builds only: lets a developer check the whole app restart from adb, which an
 * instrumentation test cannot (it runs inside the process being ended):
 *   adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver
 */
class DebugRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("CBX_RESTART_E2E", "CBX_RESTART_E2E old_pid=${Process.myPid()}")
        AppProcessRestarter.restart(context)
    }
}
