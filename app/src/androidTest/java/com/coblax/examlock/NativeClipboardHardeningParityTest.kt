package com.coblax.examlock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeClipboardHardeningParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForClipboardParity() {
        assertTrue(NativeSecurityBridge.isNativeAvailableForTests())
    }

    @Test
    fun runtimeLiteClipboardCoreMatchesKotlinReferenceAcrossCorpus() {
        clipboardRuntimeLiteCorpus().forEachIndexed { index, inputs ->
            val nativeCore = ClipboardHardeningParityAccess.buildSnapshotCoreWithBackend(
                mode = ClipboardSnapshotMode.RuntimeLite,
                items = inputs,
                backendMode = NativeBridgeBackendMode.ForceNative
            )
            val kotlinCore = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
                mode = ClipboardSnapshotMode.RuntimeLite,
                items = inputs
            )

            assertEquals("RuntimeLite parity mismatch for case $index", kotlinCore, nativeCore)
        }
    }

    @Test
    fun diagnosticFullClipboardCoreMatchesKotlinReferenceAcrossCorpus() {
        clipboardDiagnosticFullCorpus().forEachIndexed { index, inputs ->
            val nativeCore = ClipboardHardeningParityAccess.buildSnapshotCoreWithBackend(
                mode = ClipboardSnapshotMode.DiagnosticFull,
                items = inputs,
                backendMode = NativeBridgeBackendMode.ForceNative
            )
            val kotlinCore = ClipboardHardeningParityAccess.buildSnapshotCoreReference(
                mode = ClipboardSnapshotMode.DiagnosticFull,
                items = inputs
            )

            assertEquals("DiagnosticFull parity mismatch for case $index", kotlinCore, nativeCore)
        }
    }

    private fun clipboardRuntimeLiteCorpus(): List<Array<ClipboardNormalizedItemInput>> {
        return listOf(
            emptyArray(),
            arrayOf(ClipboardHardeningParityAccess.buildNormalizedItemInputForTests()),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = "  runtime  clipboard\nchange  ",
                    includeDiagnosticFields = false
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    html = "<p>Hello</p>&amp;<b>World</b>",
                    includeDiagnosticFields = false
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    uri = "content://examlock/runtime/item/7",
                    includeDiagnosticFields = false
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    intentAction = "android.intent.action.SEND",
                    intentData = "https://example.com/share",
                    intentComponent = "com.example/.ShareActivity",
                    includeDiagnosticFields = false
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = "Halo ?? dunia",
                    includeDiagnosticFields = false
                ),
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    uri = "content://examlock/runtime/item/99",
                    includeDiagnosticFields = false
                )
            )
        )
    }

    private fun clipboardDiagnosticFullCorpus(): List<Array<ClipboardNormalizedItemInput>> {
        return listOf(
            arrayOf(ClipboardHardeningParityAccess.buildNormalizedItemInputForTests()),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = " Line one\nLine two ",
                    html = "<b>ignored</b>",
                    includeDiagnosticFields = true
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    html = "<div>Alpha&nbsp;<span>Beta</span></div>",
                    includeDiagnosticFields = true
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    uri = "content://examlock/diagnostic/item/4",
                    includeDiagnosticFields = true
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    intentAction = "android.intent.action.VIEW",
                    intentData = "https://example.com/detail",
                    intentComponent = "com.example/.DetailActivity",
                    includeDiagnosticFields = true
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = "x".repeat(400),
                    includeDiagnosticFields = true
                )
            ),
            arrayOf(
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    text = "Pertama",
                    includeDiagnosticFields = true
                ),
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    html = "<i>Kedua</i>",
                    includeDiagnosticFields = true
                ),
                ClipboardHardeningParityAccess.buildNormalizedItemInputForTests(
                    uri = "content://examlock/diagnostic/item/8",
                    includeDiagnosticFields = true
                )
            )
        )
    }
}
