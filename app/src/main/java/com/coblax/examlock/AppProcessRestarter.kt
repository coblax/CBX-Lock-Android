package com.coblax.examlock

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.webkit.CookieManager
import kotlinx.coroutines.delay

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
    /** Debug builds only: DebugRestartReceiver asks MainActivity to unpin and restart. */
    const val ExtraDebugRestart = "com.coblax.examlock.extra.DEBUG_RESTART"
    /** Debug builds only: pins the screen as the exam start does, to test the restart pinned. */
    const val ExtraDebugPin = "com.coblax.examlock.extra.DEBUG_PIN"
    private const val UnpinTimeoutMillis = 5_000L
    private const val UnpinPollMillis = 100L
    private const val UnpinSettleMillis = 300L

    /**
     * Ends screen pinning and waits until Android reports it gone. Restarting while still
     * pinned failed badly: Android blocks the trampoline (a launch outside the pinned task)
     * with its "unpin" prompt, and the process then died still pinned, so that prompt kept
     * coming back. The exam screen's own teardown also unpins, but only once it has left
     * the screen, after the exit call returns; and stopLockTask() takes effect later still.
     *
     * @return false when the phone is still pinned after [timeoutMillis]: do not restart.
     */
    suspend fun awaitUnpinned(activity: Activity, timeoutMillis: Long = UnpinTimeoutMillis): Boolean {
        val activityManager = activity.getSystemService(ActivityManager::class.java) ?: return true
        fun pinned() = runCatching {
            activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }.getOrDefault(false)
        if (!pinned()) return true
        if (activity is MainActivity) {
            activity.setExamLockMode(enabled = false, allowLockTask = false)
        } else {
            runCatching { activity.stopLockTask() }
        }
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (pinned()) {
            if (SystemClock.elapsedRealtime() >= deadline) return false
            delay(UnpinPollMillis)
        }
        // A moment for the exam screen's teardown (device-owner policies, overlay shield).
        delay(UnpinSettleMillis)
        return true
    }

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
