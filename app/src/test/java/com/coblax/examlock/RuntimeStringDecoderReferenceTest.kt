package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStringDecoderReferenceTest {
    @Test
    fun referenceDecoderMatchesExpectedFastExamUrl() {
        assertEquals(
            "https://www.smkn1tanjungpandan.sch.id/",
            RuntimeStringDecoderParityAccess.decodeReference(SecureStrings.OBF_FAST_EXAM_URL)
        )
    }

    @Test
    fun referenceDecoderMatchesExpectedScreenPinningSignal() {
        assertEquals(
            "SCREEN_PINNING_ACTIVE",
            RuntimeStringDecoderParityAccess.decodeReference("IDAhNjY9LCM6PT06PTQsMjAnOiU2")
        )
    }
}
