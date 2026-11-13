package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStringDecoderReferenceTest {
    @Test
    fun referenceDecoderMatchesExpectedFastExamUrl() {
        assertEquals(
            "https://exam.smkn1tanjungpandan.sch.id",
            RuntimeStringDecoderParityAccess.decodeReference(SecureStrings.OBF_FAST_EXAM_URL)
        )
        assertEquals(
            "https://skansatp.web.id/?examkey=223611",
            RuntimeStringDecoderParityAccess.decodeReference(SecureStrings.OBF_LEGACY_FAST_EXAM_URL)
        )
    }

    @Test
    fun savedOldDefaultLinkMovesToTheNewDefault() {
        val newDefault = "https://exam.smkn1tanjungpandan.sch.id"
        assertEquals(newDefault, SecureStrings.resolveSavedFastExamUrl(null))
        assertEquals(newDefault, SecureStrings.resolveSavedFastExamUrl("  "))
        assertEquals(newDefault, SecureStrings.resolveSavedFastExamUrl("https://skansatp.web.id/?examkey=223611"))
        assertEquals(newDefault, SecureStrings.resolveSavedFastExamUrl(" https://skansatp.web.id/?examkey=223611 "))
        // A link the admin chose is kept, even another page on the old server.
        assertEquals(
            "https://skansatp.web.id/ujian/kelas-12",
            SecureStrings.resolveSavedFastExamUrl("https://skansatp.web.id/ujian/kelas-12")
        )
        assertEquals("http://192.168.1.10", SecureStrings.resolveSavedFastExamUrl("http://192.168.1.10"))
    }

    @Test
    fun defaultAppKeyIs223611() {
        assertEquals("223611", RuntimeStringDecoderParityAccess.decodeReference(SecureStrings.OBF_DEFAULT_EXAM_APP_KEY))
    }

    @Test
    fun referenceDecoderMatchesExpectedScreenPinningSignal() {
        assertEquals(
            "SCREEN_PINNING_ACTIVE",
            RuntimeStringDecoderParityAccess.decodeReference("IDAhNjY9LCM6PT06PTQsMjAnOiU2")
        )
    }
}
