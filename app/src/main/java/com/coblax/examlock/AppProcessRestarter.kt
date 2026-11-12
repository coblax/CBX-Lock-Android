package com.coblax.examlock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.webkit.CookieManager

/**
 * Restarts the app's process, the part of "restart the phone" that the app can do itself.
 *
 * WebView's network stack lives in the app process. After mobile data or tethering drops
 * and comes back, a connection to the exam server can be left hanging there, and every
 * later request to that server waits on it: white page, progress bar frozen. "Exit, open
 * again" keeps the same process, so it kept the same hung connection; only restarting the
 * phone cleared it. Killing the process clears it too, in a few seconds.
 *
 * The new launch is started from [AppRestartActivity] in its own process (the pattern of
 * ProcessPhoenix): an app may not start an activity once its own process is gone, and a
 * pending alarm is blocked from starting one on current Android versions.
 *
 * Callers end the exam session first (lock task off, session cleared): this only restarts.
 */
internal object AppProcessRestarter {
    const val ExtraMainPid = "com.coblax.examlock.extra.RESTART_MAIN_PID"
    const val ExtraRelaunch = "com.coblax.examlock.extra.RESTART_RELAUNCH"
    /** Set on the relaunch intent, so the home screen can say what happened. */
    const val ExtraRestarted = "com.coblax.examlock.extra.RESTARTED"

    /** [context] is normally the foreground activity; it must be in the foreground to start the trampoline. */
    fun restart(context: Context) {
        runCatching { CookieManager.getInstance().flush() }
        val relaunch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(ExtraRestarted, true)
            }
            ?: return
        val trampoline = Intent(context, AppRestartActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ExtraMainPid, Process.myPid())
            putExtra(ExtraRelaunch, relaunch)
        }
        context.startActivity(trampoline)
        (context as? Activity)?.finishAffinity()
        Runtime.getRuntime().exit(0)
    }
}

/** Runs in the ":restart" process: ends the old app process, opens the app again, exits. */
class AppRestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainPid = intent.getIntExtra(AppProcessRestarter.ExtraMainPid, -1)
        if (mainPid > 0 && mainPid != Process.myPid()) {
            Process.killProcess(mainPid)
        }
        val relaunch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(AppProcessRestarter.ExtraRelaunch, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(AppProcessRestarter.ExtraRelaunch)
        }
        if (relaunch != null) {
            runCatching { startActivity(relaunch) }
        }
        finish()
        Runtime.getRuntime().exit(0)
    }
}
