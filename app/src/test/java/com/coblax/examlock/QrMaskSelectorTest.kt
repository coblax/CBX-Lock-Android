package com.coblax.examlock

import java.util.Base64
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class QrMaskSelectorTest {
    /** Same shape as a real exam QR: "CBXEL2:" plus base64url ciphertext, 479 chars. */
    private fun examLikePayload(random: Random): String =
        "CBXEL2:" + Base64.getUrlEncoder().withoutPadding().encodeToString(random.nextBytes(354))

    @Test
    fun everyGeneratedPayloadGetsAMaskTheScannerCanLocate() {
        val random = Random(20260924)
        var defaultMaskUnreadable = 0
        repeat(150) {
            val payload = examLikePayload(random)
            if (!QrMaskSelector.isReadableByScanner(payload, mask = null)) {
                defaultMaskUnreadable++
            }
            val mask = QrMaskSelector.readableMaskFor(payload)
            assertTrue(
                "payload #$it is unreadable with mask $mask",
                QrMaskSelector.isReadableByScanner(payload, mask)
            )
        }
        // Proves the corpus hits the ZXing blind spot this selector exists for.
        assertTrue("default mask never failed", defaultMaskUnreadable > 0)
    }

    @Test
    fun readableDefaultMaskIsKept() {
        val random = Random(7)
        val payload = generateSequence { examLikePayload(random) }
            .first { QrMaskSelector.isReadableByScanner(it, mask = null) }
        assertTrue(QrMaskSelector.readableMaskFor(payload) == null)
    }
}
