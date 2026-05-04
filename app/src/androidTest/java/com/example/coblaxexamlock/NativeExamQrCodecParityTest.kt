package com.example.coblaxexamlock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeSecurityBridge
import org.junit.Assert.assertThrows
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeExamQrCodecParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForQrParity() {
        assertTrue(NativeSecurityBridge.isNativeAvailableForTests())
    }

    @Test
    fun nativeEncryptRoundTripsBackToPayload() {
        val payload = samplePayload()

        val encrypted = ExamQrCodec.ParityAccess.encryptWithBackend(
            payload,
            NativeBridgeBackendMode.ForceNative
        )
        assertTrue(encrypted.startsWith("CBXEL2:"))
        val decrypted = ExamQrCodec.ParityAccess.decryptWithBackend(
            encrypted,
            NativeBridgeBackendMode.ForceNative
        )

        assertEquals(expectedPayload(payload), decrypted)
    }

    @Test
    fun kotlinFallbackEncryptedPayloadCanBeDecryptedByNative() {
        val payload = samplePayload()
        val encrypted = ExamQrCodec.ParityAccess.encryptReference(payload)

        val decrypted = ExamQrCodec.ParityAccess.decryptWithBackend(
            encrypted,
            NativeBridgeBackendMode.ForceNative
        )

        assertEquals(expectedPayload(payload), decrypted)
    }

    @Test
    fun nativeEncryptedPayloadCanBeDecryptedByKotlinFallback() {
        val payload = samplePayload()
        val encrypted = ExamQrCodec.ParityAccess.encryptWithBackend(
            payload,
            NativeBridgeBackendMode.ForceNative
        )

        val decrypted = ExamQrCodec.ParityAccess.decryptReference(encrypted)

        assertEquals(expectedPayload(payload), decrypted)
    }

    @Test
    fun forceFallbackAndForceNativeProduceEquivalentPayloads() {
        val payload = samplePayload()
        val fallbackEncrypted = ExamQrCodec.ParityAccess.encryptWithBackend(
            payload,
            NativeBridgeBackendMode.ForceKotlinFallback
        )
        val nativeEncrypted = ExamQrCodec.ParityAccess.encryptWithBackend(
            payload,
            NativeBridgeBackendMode.ForceNative
        )

        val fallbackDecrypted = ExamQrCodec.ParityAccess.decryptWithBackend(
            fallbackEncrypted,
            NativeBridgeBackendMode.ForceKotlinFallback
        )
        val nativeDecrypted = ExamQrCodec.ParityAccess.decryptWithBackend(
            nativeEncrypted,
            NativeBridgeBackendMode.ForceNative
        )

        assertEquals(expectedPayload(payload), fallbackDecrypted)
        assertEquals(expectedPayload(payload), nativeDecrypted)
    }

    @Test
    fun legacyPrefixIsRejectedExplicitly() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ExamQrCodec.decrypt("CBXEL1:legacy")
        }

        assertEquals(
            "Format QR lama tidak lagi didukung. Buat ulang QR dari aplikasi terbaru.",
            error.message
        )
    }

    private fun samplePayload(): ExamQrPayload {
        return ExamQrPayload(
            examUrl = "https://example.com/ujian?id=42",
            examName = "Ujian Geometri Final",
            startDateTime = "12/03/2026 07:00",
            endDateTime = "12/03/2026 09:30",
            saveToDirectLink = true,
            issuedAt = 123456789L,
            locationPolicy = ExamQrLocationPolicy(
                shapeType = GeofenceShapeType.Circle,
                centerLat = "-6.200000",
                centerLng = "106.816666",
                radiusMeters = "75",
                circleCenters = listOf(
                    GeofenceVertex("-6.200000", "106.816666"),
                    GeofenceVertex("-6.201000", "106.817000")
                )
            ),
            locationPolicySource = LocationPolicySource.CustomQr
        )
    }

    private fun expectedPayload(payload: ExamQrPayload): ExamQrPayload {
        return payload.copy(locationPolicySource = LocationPolicySource.CustomQr)
    }
}
