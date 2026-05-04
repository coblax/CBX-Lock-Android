package com.example.coblaxexamlock.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupAndHomeEntrypoints() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm clear $PackageName")

        baselineProfileRule.collect(
            packageName = PackageName,
            includeInStartupProfile = true
        ) {
            device.pressHome()
            startActivityAndWait()
            device.waitForHome()

            device.openHomeEntrypoint("SCAN")
            device.pressBack()
            device.waitForHome()

            device.openHomeEntrypoint("CUSTOM QR")
            device.pressBack()
            device.waitForHome()

            device.openHomeEntrypoint("EXAM_SKANSATP")
            device.pressBack()
            device.waitForHome()
        }
    }

    private fun UiDevice.waitForHome() {
        waitForAnyText(
            "SCAN EXAM QR",
            "SCAN QR UJIAN",
            "CUSTOM QR",
            "EXAM_SKANSATP",
            timeoutMs = LongWaitMs
        ) ?: error("CBX Home did not become visible.")
    }

    private fun UiDevice.openHomeEntrypoint(text: String) {
        val entrypoint = waitForAnyText(text, timeoutMs = LongWaitMs)
            ?: error("Home entrypoint '$text' was not found.")
        entrypoint.click()
        waitForIdle()
        SystemClock.sleep(EntrypointSettleMs)
    }

    private fun UiDevice.waitForAnyText(
        vararg textContains: String,
        timeoutMs: Long
    ): UiObject2? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            textContains.forEach { text ->
                wait(Until.findObject(By.textContains(text)), ShortWaitMs)?.let { return it }
            }
        }
        return null
    }

    private companion object {
        const val PackageName = "com.example.coblaxexamlock"
        const val ShortWaitMs = 500L
        const val LongWaitMs = 15_000L
        const val EntrypointSettleMs = 1_000L
    }
}
