package com.coblax.examlock.ui.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrExportHelper
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.LowRamProfileOverride
import com.coblax.examlock.MainActivity
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.calculateQrExportBitmapSpec
import com.coblax.examlock.formatExamScheduleDateTime
import com.coblax.examlock.saveLowRamProfileOverride
import com.coblax.examlock.ui.geofence.GeofenceCoordinate
import com.coblax.examlock.ui.geofence.toVertex
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

/**
 * A student picks the saved QR poster from the gallery: the picture is decoded, the
 * review dialog describes the exam, and confirming opens preparation. The system picker
 * itself is replaced by a monitor that answers with the poster.
 */
@RunWith(AndroidJUnit4::class)
class ScanQrFromFileFlowTest {
    /**
     * Android 7 with 2 GB counts as Ultra, which opens the lighter View-based home; pin
     * the profile to Normal so the Compose scan flow is the one under test.
     */
    @get:Rule(order = 0)
    val normalProfile = object : ExternalResource() {
        override fun before() {
            saveLowRamProfileOverride(InstrumentationRegistry.getInstrumentation().targetContext, LowRamProfileOverride.Normal)
        }

        override fun after() {
            saveLowRamProfileOverride(InstrumentationRegistry.getInstrumentation().targetContext, LowRamProfileOverride.Auto)
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var monitor: Instrumentation.ActivityMonitor? = null

    @After
    fun removeMonitor() {
        monitor?.let(instrumentation::removeMonitor)
    }

    @Test
    fun aSavedPosterOpensTheReviewAndThenPreparation() {
        val now = System.currentTimeMillis()
        val payload = ExamQrPayload(
            examUrl = "https://cbt.sekolah.sch.id/",
            examName = "Tes Scan Galeri",
            startDateTime = formatExamScheduleDateTime(now - 10 * 60_000L),
            endDateTime = formatExamScheduleDateTime(now + 2 * 60 * 60_000L),
            locationPolicy = ExamQrLocationPolicy(
                shapeType = GeofenceShapeType.Circle,
                radiusMeters = "150",
                circleCenters = listOf(GeofenceCoordinate(-7.025123, 110.417456).toVertex())
            ),
            locationPolicySource = LocationPolicySource.CustomQr
        )
        val encrypted = ExamQrCodec.encrypt(payload)
        val context = instrumentation.targetContext
        val poster = ExamQrExportHelper.createShareBitmap(
            encryptedPayload = encrypted,
            examName = payload.examName,
            startTime = payload.startDateTime,
            endTime = payload.endDateTime,
            locationPolicy = payload.locationPolicy!!,
            exportSpec = calculateQrExportBitmapSpec(qrModules = QrCodeGenerator.moduleCount(encrypted))
        )
        val posterUri = ExamQrExportHelper.writeShareFile(context, poster, payload.examName)
        poster.recycle()
        monitor = instrumentation.addMonitor(
            IntentFilter(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addDataType("image/*")
            },
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().setData(posterUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ),
            true
        )

        clickWhenShown("Scan exam QR", "Pindai QR ujian")
        clickWhenShown("File QR", "QR File", "File")
        waitForAny("Review Exam QR", "Review QR Ujian")
        waitForAny("Tes Scan Galeri")
        waitForAny("Circle · 1 center · radius 150 m", "Lingkaran · 1 titik pusat · radius 150 m")
        clickWhenShown("Yes, continue to Preparation", "Ya, lanjut ke Preparation")
        waitForAny("Start exam", "Mulai ujian", timeoutMillis = 30_000)
    }

    private fun waitForAny(vararg texts: String, timeoutMillis: Long = 15_000): String {
        var found: String? = null
        composeRule.waitUntil(timeoutMillis) {
            found = texts.firstOrNull { text ->
                composeRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
            found != null
        }
        return found!!
    }

    private fun clickWhenShown(vararg texts: String) {
        val text = waitForAny(*texts)
        composeRule.onNodeWithText(text, substring = true).performClick()
    }
}
