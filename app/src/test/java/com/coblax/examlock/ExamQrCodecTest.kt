package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamQrCodecTest {
    @Test
    fun encryptedPayloadCanBeDecodedBack() {
        val original = ExamQrPayload(
            examUrl = "https://example.com/ujian",
            examName = "Matematika Kelas 12",
            startDateTime = "12/03/2026 07:00",
            endDateTime = "12/03/2026 09:00",
            issuedAt = 123456789L
        )

        val encrypted = ExamQrCodec.encrypt(original)
        val decrypted = ExamQrCodec.decrypt(encrypted)
        val expected = original.copy(
            locationPolicy = ExamQrLocationPolicy(),
            locationPolicySource = LocationPolicySource.CustomQr
        )

        assertTrue(encrypted.startsWith("CBXEL2:"))
        assertEquals(expected, decrypted)
    }

    @Test
    fun legacyQrPrefixIsRejectedExplicitly() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ExamQrCodec.decrypt("CBXEL1:any-legacy-value")
        }

        assertEquals(
            "Format QR lama tidak lagi didukung. Buat ulang QR dari aplikasi terbaru.",
            error.message
        )
    }
}
