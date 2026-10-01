package com.coblax.examlock.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile so the code students actually run is compiled at
 * install time instead of being interpreted and JIT-compiled mid-exam, which costs a
 * low-end phone CPU and memory exactly when it has the least to spare.
 *
 * Run on an emulator (API 33+, or a rooted image from API 28):
 *   ./gradlew :app:generateBaselineProfile \
 *     -Pandroid.testInstrumentationRunnerArguments.cbxAdminPassword=<Secret Admin password>
 * Without the password only the home and preparation journey runs; the exam journey needs
 * Secret Admin bypasses because an emulator always trips the emulator and root checks.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun startupHomeAndPreparation() {
        device.executeShellCommand("pm clear $PackageName")

        baselineProfileRule.collect(
            packageName = PackageName,
            includeInStartupProfile = true
        ) {
            device.pressHome()
            startActivityAndWait()
            device.waitForHome()

            device.openHomeEntrypoint("Scan exam QR", "Pindai QR ujian")
            device.pressBack()
            device.waitForHome()

            device.openHomeEntrypoint("Custom QR")
            device.pressBack()
            device.waitForHome()

            device.openHomeEntrypoint("EXAM_SKANSATP")
            device.browsePreparation()
            device.backToHome()
        }
    }

    @Test
    fun examSession() {
        val password = InstrumentationRegistry.getArguments().getString(AdminPasswordArgument)
        assumeTrue(
            "Pass -Pandroid.testInstrumentationRunnerArguments.$AdminPasswordArgument=… to profile the exam.",
            !password.isNullOrBlank()
        )
        device.executeShellCommand("pm clear $PackageName")
        device.executeShellCommand("settings put secure lock_to_app_enabled 1")
        var bypassesReady = false

        baselineProfileRule.collect(
            packageName = PackageName,
            maxIterations = 4,
            stableIterations = 2
        ) {
            device.pressHome()
            startActivityAndWait()
            device.waitForHome()
            if (!bypassesReady) {
                device.enableEmulatorBypasses(password!!)
                bypassesReady = true
            }

            device.openHomeEntrypoint("EXAM_SKANSATP")
            device.browsePreparation()
            device.startExam()
            // Let the exam page settle so the runtime guards, pollers and the footer run.
            SystemClock.sleep(ExamDwellMs)
            device.wait(Until.findObject(By.text("Check")), ShortWaitMs)?.let { check ->
                check.click()
                SystemClock.sleep(EntrypointSettleMs)
                device.pressBack()
            }
            device.exitExam()
        }
    }

    /**
     * Presses Back until the home screen shows. A sheet may need a press of its own, and
     * Back on the preparation screen asks whether to leave the exam.
     */
    private fun UiDevice.backToHome() {
        repeat(3) {
            pressBack()
            waitForAnyText("Exit exam", "Keluar ujian", timeoutMs = ShortWaitMs * 2)?.click()
            if (waitForAnyText("Scan exam QR", "Pindai QR ujian", timeoutMs = HomeBackWaitMs) != null) {
                return
            }
        }
        waitForHome()
    }

    /** The vertical list of the current screen; the tab row is scrollable too, sideways. */
    private fun UiDevice.verticalList(): UiObject2? =
        findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }

    private fun UiDevice.waitForHome() {
        waitForAnyText(
            "Scan exam QR",
            "Pindai QR ujian",
            "Custom QR",
            "EXAM_SKANSATP",
            timeoutMs = LongWaitMs
        ) ?: error("CBX Home did not become visible.")
    }

    private fun UiDevice.openHomeEntrypoint(vararg labels: String) {
        val entrypoint = waitForAnyText(*labels, timeoutMs = LongWaitMs)
            ?: error("Home entrypoint '${labels.joinToString()}' was not found.")
        entrypoint.click()
        waitForIdle()
        SystemClock.sleep(EntrypointSettleMs)
    }

    /** Scrolls the checklist and opens one detail sheet, the way a student checks it. */
    private fun UiDevice.browsePreparation() {
        waitForAnyText("Start exam", "Mulai ujian", timeoutMs = LongWaitMs)
            ?: error("Preparation did not open.")
        SystemClock.sleep(PreparationSettleMs)
        verticalList()?.let { list ->
            list.scroll(Direction.DOWN, 1f)
            SystemClock.sleep(EntrypointSettleMs)
            list.scroll(Direction.UP, 1f)
        }
        waitForAnyText("Details", "Detail", timeoutMs = ShortWaitMs)?.let { details ->
            details.click()
            SystemClock.sleep(EntrypointSettleMs)
            pressBack()
            SystemClock.sleep(EntrypointSettleMs)
        }
    }

    private fun UiDevice.startExam() {
        val start = waitForAnyText("Start exam", "Mulai ujian", timeoutMs = LongWaitMs)
            ?: error("Start exam was not found.")
        if (start.visibleBounds.isEmpty) {
            // Immersive layouts can report the bottom bar outside the window; it sits there.
            click(displayWidth / 2, displayHeight - displayHeight / 20)
        } else {
            start.click()
        }
        wait(Until.findObject(By.text("Check")), ExamStartWaitMs)
            ?: error("The exam did not start.")
    }

    /** Back leaves the exam through its exit dialog (after any page history). */
    private fun UiDevice.exitExam() = backToHome()

    /** Turns on the bypasses an emulator needs to reach the exam (Secret Admin, 4 taps). */
    private fun UiDevice.enableEmulatorBypasses(password: String) {
        val profileCell = wait(Until.findObject(By.text(Pattern.compile("Low|Normal|Ultra"))), LongWaitMs)
            ?: error("The footer profile cell was not found.")
        repeat(4) { profileCell.click() }
        val field = wait(Until.findObject(By.clazz("android.widget.EditText")), LongWaitMs)
            ?: error("Secret Admin did not ask for the password.")
        field.text = password
        (waitForAnyText("Unlock", "Buka", timeoutMs = LongWaitMs) ?: error("Unlock not found.")).click()
        (wait(Until.findObject(By.text("Bypass")), LongWaitMs) ?: error("Bypass tab not found.")).click()
        SystemClock.sleep(EntrypointSettleMs)
        listOf(
            "Screen pinning",
            "Bluetooth check",
            "USB debugging (ADB)",
            "Root check",
            // The profiling build is signed differently and carries no release hash.
            "APK integrity",
            "Emulator detection"
        ).forEach { label -> enableBypassRow(label) }
        (waitForAnyText("Apply", "Terapkan", timeoutMs = LongWaitMs) ?: error("Apply not found.")).click()
        SystemClock.sleep(EntrypointSettleMs)
        pressBack()
        waitForHome()
    }

    private fun UiDevice.enableBypassRow(label: String) {
        val rowSelector = By.clickable(true).hasDescendant(By.text(label))
        val list = verticalList() ?: error("Bypass list not found.")
        var row = findObject(rowSelector)
            ?: list.scrollUntil(Direction.DOWN, Until.findObject(rowSelector))
            ?: list.scrollUntil(Direction.UP, Until.findObject(rowSelector))
            ?: error("Bypass '$label' not found.")
        // Once something changed, an Apply bar covers the bottom of the list; tapping a
        // row under it would hit the bar instead, so bring the row up first.
        if (row.visibleCenter.y > displayHeight * 6 / 10) {
            list.scroll(Direction.DOWN, 0.4f)
            SystemClock.sleep(ShortWaitMs)
            row = findObject(rowSelector) ?: error("Bypass '$label' scrolled away.")
        }
        if (row.isChecked) {
            return
        }
        row.click()
        wait(Until.findObject(By.text("Turn on")), ShortWaitMs * 6)?.click()
        val deadline = SystemClock.uptimeMillis() + LongWaitMs / 3
        while (SystemClock.uptimeMillis() < deadline) {
            if (findObject(rowSelector)?.isChecked == true) {
                return
            }
            SystemClock.sleep(ShortWaitMs / 2)
        }
        error("Bypass '$label' did not turn on.")
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
        const val PackageName = "com.coblax.examlock"
        const val AdminPasswordArgument = "cbxAdminPassword"
        const val ShortWaitMs = 500L
        const val LongWaitMs = 15_000L
        const val EntrypointSettleMs = 1_000L
        const val PreparationSettleMs = 4_000L
        const val ExamStartWaitMs = 90_000L
        const val ExamDwellMs = 20_000L
        const val HomeBackWaitMs = 3_000L
    }
}
