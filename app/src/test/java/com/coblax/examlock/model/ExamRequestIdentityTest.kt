package com.coblax.examlock.model

import com.coblax.examlock.RuntimeStringDecoder
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.config.DefaultExamUserAgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRequestIdentityTest {
    private val webViewUa =
        "Mozilla/5.0 (Linux; Android 11; sdk_gphone_x86_64 Build/RSR1; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Mobile Safari/537.36"

    @Test
    fun defaultMarkerIsAppendedToTheWebViewUserAgent() {
        // The bug: the default marker used to be replaced by the bare WebView UA.
        val ua = composeExamWebViewUserAgent(webViewUa, DefaultExamUserAgent)

        assertEquals("$webViewUa $DefaultExamUserAgent", ua)
        assertTrue(ua.contains("CBXExamLockAndroid"))
    }

    @Test
    fun blankSettingFallsBackToTheDefaultMarker() {
        assertEquals("$webViewUa $DefaultExamUserAgent", composeExamWebViewUserAgent(webViewUa, "  "))
        assertEquals("$webViewUa $DefaultExamUserAgent", composeExamWebViewUserAgent(webViewUa, null))
    }

    @Test
    fun customMarkerIsAppendedToo() {
        assertEquals(
            "$webViewUa CBXExamAndroid",
            composeExamWebViewUserAgent(webViewUa, " CBXExamAndroid ")
        )
    }

    @Test
    fun aWholeBrowserUserAgentIsSentAsEntered() {
        val custom = "Mozilla/5.0 (Custom) CBXExamLockAndroid"
        assertEquals(custom, composeExamWebViewUserAgent(webViewUa, custom))
    }

    @Test
    fun markerIsNotDuplicatedWhenAlreadyPresent() {
        val already = "$webViewUa CBXExamLockAndroid"
        assertEquals(already, composeExamWebViewUserAgent(already, DefaultExamUserAgent))
    }

    @Test
    fun missingWebViewUserAgentSendsTheMarkerAlone() {
        assertEquals(DefaultExamUserAgent, composeExamWebViewUserAgent("", DefaultExamUserAgent))
    }

    @Test
    fun lineBreaksCannotReachTheHeader() {
        val ua = composeExamWebViewUserAgent(webViewUa, "CBXExamLockAndroid\r\nX-Evil: 1")
        assertTrue(!ua.contains('\r') && !ua.contains('\n'))
        assertEquals("abc", normalizeExamAppKey(" a\nb\tc "))
    }

    @Test
    fun appKeyDefaultsToTheAgreedValueAndBlankTurnsItOff() {
        assertEquals("223611", RuntimeStringDecoder.decodeBase64XorReference(SecureStrings.OBF_DEFAULT_EXAM_APP_KEY))
        assertEquals("223611", AdminSettings().effectiveExamAppKey())
        assertEquals("", AdminSettings(examAppKey = "   ").effectiveExamAppKey())
        assertEquals("rahasia", AdminSettings(examAppKey = " rahasia ").effectiveExamAppKey())
    }
}
