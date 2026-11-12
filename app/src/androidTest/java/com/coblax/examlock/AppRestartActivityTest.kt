package com.coblax.examlock

import android.app.Activity
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The restart's second half: the trampoline in the ":restart" process opens the app again.
 * The first half (the app ending its own process) cannot run here, since the test runs
 * inside that process; the trampoline is given no process to end.
 */
class AppRestartActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var opened: Activity? = null

    @After
    fun cleanup() {
        instrumentation.runOnMainSync { opened?.finish() }
    }

    @Test
    fun trampolineOpensTheAppAgainMarkedAsRestarted() {
        val context = instrumentation.targetContext
        val relaunch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AppProcessRestarter.ExtraRestarted, true)
        }
        val trampoline = Intent(context, AppRestartActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AppProcessRestarter.ExtraMainPid, -1)
            putExtra(AppProcessRestarter.ExtraRelaunch, relaunch)
        }

        context.startActivity(trampoline)

        val deadline = System.currentTimeMillis() + 20_000
        while (opened == null && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                opened = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it is MainActivity }
            }
            if (opened == null) Thread.sleep(250)
        }
        assertNotNull("MainActivity was not opened again", opened)
        assertTrue(opened!!.intent.getBooleanExtra(AppProcessRestarter.ExtraRestarted, false))
    }
}
