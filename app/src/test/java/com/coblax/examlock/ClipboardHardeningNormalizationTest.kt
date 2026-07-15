package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardHardeningNormalizationTest {
    @Test
    fun emptyItemProducesEmptyClipboardCore() {
        val input = ClipboardHardeningParityAccess.buildNormalizedItemInputForTests()

        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.RuntimeLite,
            arrayOf(input)
        )

        assertEquals("clipboard_empty", result.decisionFingerprint)
        assertEquals("clipboard_empty", result.semanticSignature)
        assertEquals("", result.rawSignature)
        assertTrue(result.isEmpty)
        assertFalse(result.hasText)
        assertFalse(result.hasHtml)
        assertFalse(result.hasUri)
        assertFalse(result.hasIntent)
    }

    @Test
    fun whitespaceHeavyTextNormalizesToSingleSpaces() {
        val input = ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
            text = "  hello\n\tworld   from\r\nclipboard  ",
            includeDiagnosticFields = false
        )

        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.RuntimeLite,
            arrayOf(input)
        )

        assertEquals("item0=text|hello world from clipboard", result.semanticSignature)
        assertTrue(result.hasText)
        assertFalse(result.isEmpty)
    }

    @Test
    fun htmlOnlyItemUsesPlainTextHtmlSemanticValue() {
        val input = ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
            html = "<b>Hello</b>&nbsp;<i>World</i>&amp;Friends",
            includeDiagnosticFields = true
        )

        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.DiagnosticFull,
            arrayOf(input)
        )

        assertEquals("item0=html|Hello World &Friends", result.semanticSignature)
        assertEquals(
            "item0=|<b>Hello</b>&nbsp;<i>World</i>&amp;Friends||||",
            result.rawSignature
        )
        assertTrue(result.hasHtml)
    }

    @Test
    fun intentOnlyItemUsesIntentSemanticValue() {
        val input = ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
            intentAction = " android.intent.action.VIEW ",
            intentData = " https://example.com/path ",
            intentComponent = " com.example/.MainActivity ",
            includeDiagnosticFields = false
        )

        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.RuntimeLite,
            arrayOf(input)
        )

        assertEquals(
            "item0=intent|android.intent.action.VIEW|https://example.com/path|com.example/.MainActivity",
            result.semanticSignature
        )
        assertTrue(result.hasIntent)
    }

    @Test
    fun longSemanticSignatureIsClippedForDiagnostics() {
        val longText = "x".repeat(400)
        val input = ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
            text = longText,
            includeDiagnosticFields = false
        )

        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.RuntimeLite,
            arrayOf(input)
        )

        assertTrue(result.semanticSignature.startsWith("item0=text|"))
        assertTrue(result.semanticSignature.endsWith("…"))
        assertTrue(result.semanticSignature.length <= 322)
    }

    @Test
    fun unicodeAndMultipleItemsPreserveParityInputs() {
        val result = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
            ClipboardSnapshotMode.DiagnosticFull,
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = "Halo ?? dunia",
                    includeDiagnosticFields = true
                ),
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    uri = "content://examlock/item/42",
                    includeDiagnosticFields = true
                )
            )
        )

        assertEquals(2, result.itemCount)
        assertTrue(result.hasText)
        assertTrue(result.hasUri)
        assertTrue(result.semanticSignature.contains("Halo ?? dunia"))
        assertTrue(result.semanticSignature.contains("content://examlock/item/42"))
    }
}
