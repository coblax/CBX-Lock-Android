package com.example.coblaxexamlock.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coblaxexamlock.ExamQrCodec
import com.example.coblaxexamlock.ExamQrExportHelper
import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.GeofenceVertex
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrDecodeRuntimeTest {
    @Test
    fun generatedQrBitmapRemainsReadable() {
        val encryptedPayload = ExamQrCodec.encrypt(samplePayload())
        val bitmap = QrCodeGenerator.generateBitmap(encryptedPayload, size = 960)

        try {
            assertEquals(encryptedPayload, decodeQrPayloadFromBitmap(bitmap))
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    @Test
    fun generatedSharePosterQrRemainsReadable() {
        val payload = samplePayload()
        val encryptedPayload = ExamQrCodec.encrypt(payload)
        val bitmap = ExamQrExportHelper.createShareBitmap(
            encryptedPayload = encryptedPayload,
            examName = payload.examName,
            startTime = payload.startDateTime,
            endTime = payload.endDateTime,
            locationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
        )

        try {
            assertEquals(encryptedPayload, decodeQrPayloadFromBitmap(bitmap))
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun samplePayload(): ExamQrPayload {
        return ExamQrPayload(
            examUrl = "https://exam.coblax.example.com/session/final-tryout/kelas-12/token-ABC123XYZ",
            examName = "Tryout Akhir Matematika Kelas 12",
            startDateTime = "21/04/2026 07:00",
            endDateTime = "21/04/2026 09:30",
            issuedAt = 1_776_000_000_000L,
            locationPolicy = ExamQrLocationPolicy(
                shapeType = GeofenceShapeType.Circle,
                radiusMeters = "75",
                circleCenters = listOf(
                    GeofenceVertex("-6.200000", "106.816666"),
                    GeofenceVertex("-6.199450", "106.817120"),
                    GeofenceVertex("-6.200550", "106.815980")
                )
            ),
            locationPolicySource = LocationPolicySource.CustomQr
        )
    }
}
