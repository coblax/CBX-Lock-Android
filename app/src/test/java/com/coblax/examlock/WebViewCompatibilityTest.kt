package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCompatibilityTest {
    @Test
    fun missingProviderIsUnavailableAndHasQuickFix() {
        val status = resolveWebViewCompatibilityStatus(
            packageName = null,
            versionName = null
        )

        assertFalse(status.available)
        assertEquals("-", status.packageName)
        assertEquals(WebViewHealthVerdict.Unavailable, status.verdict)
        assertEquals(WebViewHealthSeverity.Blocking, status.severity)
        assertTrue(status.quickFix.orEmpty().contains("WebView", ignoreCase = true))
    }

    @Test
    fun oldMajorVersionIsWarningOnly() {
        val status = resolveWebViewCompatibilityStatus(
            packageName = "com.android.webview",
            versionName = "74.0.3729.186"
        )

        assertTrue(status.available)
        assertEquals(74, status.majorVersion)
        assertTrue(status.outdatedLikely)
        assertEquals(WebViewHealthVerdict.NeedsUpdate, status.verdict)
        assertEquals(WebViewHealthSeverity.Warning, status.severity)
    }

    @Test
    fun modernProviderIsReady() {
        val status = resolveWebViewCompatibilityStatus(
            packageName = "com.google.android.webview",
            versionName = "120.0.6099.231"
        )

        assertTrue(status.available)
        assertEquals(120, status.majorVersion)
        assertFalse(status.outdatedLikely)
        assertEquals(WebViewHealthVerdict.Ready, status.verdict)
        assertEquals(WebViewHealthSeverity.Stable, status.severity)
        assertNull(status.quickFix)
    }

    @Test
    fun malformedAvailableVersionIsUnknownWarningOnly() {
        val status = resolveWebViewCompatibilityStatus(
            packageName = "com.android.chrome",
            versionName = "vendor-build"
        )

        assertTrue(status.available)
        assertNull(status.majorVersion)
        assertFalse(status.outdatedLikely)
        assertEquals(WebViewHealthVerdict.Unknown, status.verdict)
        assertEquals(WebViewHealthSeverity.Warning, status.severity)
        assertTrue(status.quickFix.orEmpty().contains("refresh", ignoreCase = true))
    }
}
