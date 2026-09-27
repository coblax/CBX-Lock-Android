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

    /** Students on an older app can still scan every QR that relaxes nothing. */
    @Test
    fun qrWithoutBypassesKeepsTheFormatOlderAppsRead() {
        val encrypted = ExamQrCodec.encrypt(samplePayload())
        val fields = ExamQrCodec.ParityAccess.plaintextOf(encrypted).split("|")

        assertEquals("7", fields.first())
        assertEquals(14, fields.size)
        assertEquals(emptySet<ExamQrSecurityBypass>(), ExamQrCodec.decrypt(encrypted).securityBypasses)
    }

    @Test
    fun bypassesTravelInTheQrAsACompactMask() {
        val bypasses = setOf(ExamQrSecurityBypass.Bluetooth, ExamQrSecurityBypass.Geofence)
        val encrypted = ExamQrCodec.encrypt(samplePayload().copy(securityBypasses = bypasses))
        val fields = ExamQrCodec.ParityAccess.plaintextOf(encrypted).split("|")

        assertEquals("8", fields.first())
        assertEquals(15, fields.size)
        assertEquals("8010", fields.last())
        assertEquals(bypasses, ExamQrCodec.decrypt(encrypted).securityBypasses)
    }

    @Test
    fun everyBypassSurvivesTheRoundTrip() {
        val original = samplePayload().copy(securityBypasses = ExamQrSecurityBypass.entries.toSet())
        val decrypted = ExamQrCodec.decrypt(ExamQrCodec.encrypt(original))

        assertEquals(original.securityBypasses, decrypted.securityBypasses)
        assertEquals(original.examUrl, decrypted.examUrl)
        assertEquals(original.timezoneId, decrypted.timezoneId)
    }

    @Test
    fun aMalformedBypassMaskRejectsTheQr() {
        val fields = ExamQrCodec.ParityAccess.plaintextOf(
            ExamQrCodec.encrypt(samplePayload().copy(securityBypasses = setOf(ExamQrSecurityBypass.Vpn)))
        ).split("|")
        listOf("zz", "", "-1").forEach { badMask ->
            val tampered = ExamQrCodec.ParityAccess.encryptPlaintext(
                (fields.dropLast(1) + badMask).joinToString("|")
            )
            val error = assertThrows(badMask, IllegalArgumentException::class.java) {
                ExamQrCodec.decrypt(tampered)
            }
            assertEquals("Format payload QR tidak dikenal.", error.message)
        }
    }

    private fun samplePayload() = ExamQrPayload(
        examUrl = "https://example.com/ujian",
        examName = "Matematika Kelas 12",
        startDateTime = "12/03/2026 07:00",
        endDateTime = "12/03/2026 09:00",
        issuedAt = 123456789L,
        timezoneId = "Asia/Jakarta"
    )

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
