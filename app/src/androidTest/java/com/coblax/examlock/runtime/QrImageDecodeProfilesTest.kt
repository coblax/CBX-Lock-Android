package com.coblax.examlock.runtime

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrExportHelper
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.calculateQrExportBitmapSpec
import com.coblax.examlock.ui.geofence.GeofenceCoordinate
import com.coblax.examlock.ui.geofence.MaxPolygonPoints
import com.coblax.examlock.ui.geofence.toVertex
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Picking a QR picture from the gallery has to work on every RAM profile, for the
 * densest QR Custom QR can make, whether the picture is the saved poster, the poster
 * after a chat app recompressed it, or a phone photo of a printed poster.
 */
private const val Rounds = 4

@RunWith(AndroidJUnit4::class)
class QrImageDecodeProfilesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val profiles = listOf(
        "normal" to LowRamProfile(),
        "low" to LowRamProfile(enabled = true, qrMaxEdgePx = 1024),
        "ultra" to LowRamProfile(enabled = true, severe = true, ultra = true, qrMaxEdgePx = 720)
    )

    /**
     * The densest QR (15 corners, long URL) must read from the app's own poster, from that
     * poster after a chat app resized it, and from a phone photo of the printed poster. At
     * error correction H the photo left about four pixels a module and often failed; the
     * code now steps down to M when it is this dense, so the photo is required too.
     */
    @Test
    fun theDensestQrReadsFromEveryKindOfPictureOnEveryProfile() =
        assertReadable(
            densestPayload(),
            strictPictures = setOf("poster_png", "chat_jpeg", "photo_jpeg", "handheld_photo_jpeg")
        )

    @Test
    fun anEverydayQrReadsFromEveryKindOfPictureOnEveryProfile() = assertReadable(
        strictPictures = setOf("poster_png", "chat_jpeg", "photo_jpeg", "handheld_photo_jpeg"),
        payload = ExamQrPayload(
            examUrl = "https://cbt.sekolah.sch.id/",
            examName = "PAS Matematika",
            startDateTime = "04/10/2026 07:00",
            endDateTime = "04/10/2026 09:00",
            locationPolicy = ExamQrLocationPolicy(
                shapeType = GeofenceShapeType.Circle,
                radiusMeters = "100",
                circleCenters = listOf(GeofenceCoordinate(-7.025123, 110.417456).toVertex())
            )
        )
    )

    // Every encryption draws a fresh IV, so each round is a different QR pattern.
    private fun assertReadable(payload: ExamQrPayload, strictPictures: Set<String>) = runBlocking {
        val failures = mutableListOf<String>()
        repeat(Rounds) { round -> collectFailures(payload, round, failures) }
        val (strict, reported) = failures.partition { failure -> strictPictures.any { "picture=$it " in failure } }
        if (reported.isNotEmpty()) {
            Log.w("CBX_QR_DECODE", "Best-effort pictures not read: ${reported.joinToString("; ")}")
        }
        assertTrue("Unreadable: ${strict.joinToString("; ")}", strict.isEmpty())
    }

    private suspend fun collectFailures(payload: ExamQrPayload, round: Int, failures: MutableList<String>) {
        val encrypted = ExamQrCodec.encrypt(payload)
        profiles.forEach { (exportName, exportProfile) ->
            val poster = ExamQrExportHelper.createShareBitmap(
                encryptedPayload = encrypted,
                examName = payload.examName,
                startTime = payload.startDateTime,
                endTime = payload.endDateTime,
                locationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy(),
                exportSpec = calculateQrExportBitmapSpec(exportProfile, QrCodeGenerator.moduleCount(encrypted))
            )
            val pictures = listOf(
                "poster_png" to save(poster, Bitmap.CompressFormat.PNG, 100),
                "chat_jpeg" to scaleToLongEdge(poster, 1600).let { save(it, Bitmap.CompressFormat.JPEG, 75).also { _ -> it.recycle() } },
                "photo_jpeg" to photoOf(poster).let { save(it, Bitmap.CompressFormat.JPEG, 85).also { _ -> it.recycle() } },
                "handheld_photo_jpeg" to handheldPhotoOf(poster).let {
                    save(it, Bitmap.CompressFormat.JPEG, 80).also { _ -> it.recycle() }
                }
            )
            poster.recycle()
            pictures.forEach { (pictureName, uri) ->
                profiles.forEach { (readerName, readerProfile) ->
                    // The 12 MP test photo leaves the heap fragmented; collect before each read.
                    System.gc()
                    val result = runCatching { decodeQrPayloadFromImageUri(context, uri, readerProfile) }
                    if (result.getOrNull() != encrypted) {
                        val cause = result.exceptionOrNull()?.let { " (${it.javaClass.simpleName})" }.orEmpty()
                        failures += "round=$round export=$exportName picture=$pictureName reader=$readerName$cause"
                    }
                }
            }
        }
    }

    private fun densestPayload(): ExamQrPayload {
        val vertices = (0 until MaxPolygonPoints).map { index ->
            GeofenceCoordinate(-7.123456 - index * 0.000731, 110.123456 + index * 0.000917).toVertex()
        }
        return ExamQrPayload(
            examUrl = "https://cbt.smkn1contoh.sch.id/ujian/semester-ganjil/kelas-12/matematika/token",
            examName = "Penilaian Akhir Semester Matematika Kelas XII",
            startDateTime = "04/10/2026 07:00",
            endDateTime = "04/10/2026 09:00",
            locationPolicy = ExamQrLocationPolicy(shapeType = GeofenceShapeType.Polygon, vertices = vertices)
        )
    }

    private fun save(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): Uri {
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val file = File(context.cacheDir, "qr_decode_${System.nanoTime()}.$extension")
        file.outputStream().use { bitmap.compress(format, quality, it) }
        return Uri.fromFile(file)
    }

    private fun scaleToLongEdge(bitmap: Bitmap, longEdge: Int): Bitmap {
        val scale = longEdge.toFloat() / maxOf(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    /** A 12 MP landscape photo with the poster filling about a third of its width. */
    private fun photoOf(poster: Bitmap): Bitmap {
        val photo = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(photo)
        canvas.drawColor(Color.rgb(120, 110, 100))
        val width = 1400
        val height = width * poster.height / poster.width
        val left = (4000 - width) / 2
        val top = (3000 - height) / 2
        canvas.drawBitmap(poster, null, Rect(left, top, left + width, top + height), null)
        return photo
    }

    /**
     * A handheld photo of the printed poster: smaller in the frame, turned 7 degrees, lit
     * unevenly and slightly out of focus, saved as a camera JPEG. The clean photo above
     * read even at error correction H; this one is what students actually take.
     */
    private fun handheldPhotoOf(poster: Bitmap): Bitmap {
        val photo = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(photo)
        canvas.drawColor(Color.rgb(150, 140, 125))
        val width = 1150f
        val height = width * poster.height / poster.width
        val placement = Matrix().apply {
            postScale(width / poster.width, height / poster.height)
            postRotate(7f, width / 2f, height / 2f)
            postTranslate((4000f - width) / 2f, (3000f - height) / 2f)
        }
        canvas.drawBitmap(poster, placement, Paint(Paint.FILTER_BITMAP_FLAG))
        val shade = Paint().apply {
            shader = LinearGradient(0f, 0f, 4000f, 3000f, Color.argb(0, 0, 0, 0), Color.argb(90, 0, 0, 0), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 4000f, 3000f, shade)
        // Slightly out of focus: down to three quarters and back up with filtering.
        val reduced = Bitmap.createScaledBitmap(photo, 3000, 2250, true)
        photo.recycle()
        return Bitmap.createScaledBitmap(reduced, 4000, 3000, true).also { reduced.recycle() }
    }
}
