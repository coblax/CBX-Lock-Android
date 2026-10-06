package com.coblax.examlock.ui.app

import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamQrScanMessagesTest {
    private fun messageFor(raw: String, language: UiLanguage = UiLanguage.English): String {
        val failure = runCatching { ExamQrCodec.decrypt(raw) }.exceptionOrNull()
            ?: error("$raw unexpectedly decoded")
        return examQrReadFailureMessage(failure, language)
    }

    @Test
    fun aForeignQrIsNamedAsSuch() {
        assertEquals(
            "This is not a CBX Lock exam QR. Scan the QR the admin gave for this exam.",
            messageFor("https://example.com/not-an-exam")
        )
    }

    @Test
    fun aDamagedQrNeverShowsTheCipherError() {
        listOf("CBXEL2:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "CBXEL2:%%%%", "CBXEL2:").forEach { raw ->
            assertEquals(
                raw,
                "QR ini tidak dapat dibaca. Mungkin rusak atau bukan buatan CBX Lock; minta QR baru ke admin.",
                messageFor(raw, UiLanguage.Indonesian)
            )
        }
    }

    @Test
    fun anOldQrAsksForANewOne() {
        assertEquals(
            "This QR was made by an old CBX Lock. Ask the admin to create it again.",
            messageFor("CBXEL1:legacy")
        )
    }

    @Test
    fun aNewerPayloadAsksForAnUpdate() {
        val future = ExamQrCodec.ParityAccess.encryptPlaintext("99|a|b")
        assertEquals(
            "This QR was made by a newer CBX Lock. Update CBX Lock, then scan again.",
            messageFor(future)
        )
    }
}
