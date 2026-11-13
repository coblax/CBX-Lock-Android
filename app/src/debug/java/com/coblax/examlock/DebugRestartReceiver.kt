package com.coblax.examlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log

/**
 * Debug builds only: lets a developer check the whole app restart from adb, which an
 * instrumentation test cannot (it runs inside the process being ended):
 *   adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver             # restart
 *   adb shell am broadcast -n com.coblax.examlock/.DebugRestartReceiver --ez pin true  # pin first
 * It goes through MainActivity, which unpins first like the real exit does.
 */
class DebugRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pin = intent.getBooleanExtra("pin", false)
        Log.i("CBX_RESTART_E2E", "CBX_RESTART_E2E ${if (pin) "pin" else "old_pid=${Process.myPid()}"}")
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                .putExtra(
                    if (pin) AppProcessRestarter.ExtraDebugPin else AppProcessRestarter.ExtraDebugRestart,
                    true
                )
        )
    }
}
