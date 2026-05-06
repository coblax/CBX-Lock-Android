package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.DeviceCompatibilityFamily
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.buildDeviceSurvivalPolicy
import com.example.coblaxexamlock.resolveWebViewCompatibilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationAutoFixTest {
    @Test
    fun blockingWebViewActionIsFirstAndTargetsWebViewSettings() {
        val snapshot = snapshot(
            PreExamHealthItem(
                category = PreExamHealthCategory.WebView,
                verdict = PreExamHealthVerdict.Blocking,
                title = "Exam WebView",
                detail = "Provider unavailable.",
                quickFix = "Install or enable Android System WebView/Chrome."
            ),
            PreExamHealthItem(
                category = PreExamHealthCategory.Network,
                verdict = PreExamHealthVerdict.Warning,
                title = "Network",
                detail = "DNS failed.",
                quickFix = "Check Wi-Fi or mobile data."
            )
        )
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = null,
                versionName = null
            ),
            preExamHealthSnapshot = snapshot
        )

        val suggestions = buildPreparationAutoFixSuggestions(snapshot, policy)

        assertEquals(PreparationAutoFixSeverity.Blocking, suggestions.first().severity)
        assertEquals(PreparationAutoFixTarget.WebView, suggestions.first().target)
        assertTrue(suggestions.any { it.target == PreparationAutoFixTarget.Network })
    }

    @Test
    fun accessibilityFallbackSuggestionUsesAccessibilityTarget() {
        val snapshot = snapshot(
            PreExamHealthItem(
                category = PreExamHealthCategory.ScreenPinning,
                verdict = PreExamHealthVerdict.Warning,
                title = "Screen Pinning",
                detail = "Screen Pinning unavailable, fallback active.",
                quickFix = "Keep the accessibility guard enabled during the exam."
            )
        )
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            ),
            preExamHealthSnapshot = snapshot
        )

        val suggestions = buildPreparationAutoFixSuggestions(snapshot, policy)

        assertEquals(PreparationAutoFixTarget.Accessibility, suggestions.first().target)
        assertEquals(PreparationAutoFixSeverity.Warning, suggestions.first().severity)
    }

    private fun snapshot(vararg items: PreExamHealthItem): PreExamHealthSnapshot =
        PreExamHealthSnapshot(
            compatibilityFamily = DeviceCompatibilityFamily.Generic,
            compatibilityLabel = "Android",
            generatedAtElapsedMs = 0L,
            items = items.toList()
        )
}
