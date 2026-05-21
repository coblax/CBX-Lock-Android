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

    @Test
    fun lowRamDisplayKeepsPrimaryAndLimitsExtraActions() {
        val actions = listOf(
            action("block_1", QuickFixSeverity.Blocking, 10),
            action("block_2", QuickFixSeverity.Blocking, 20),
            action("block_3", QuickFixSeverity.Blocking, 30),
            action("block_4", QuickFixSeverity.Blocking, 40),
            action("block_5", QuickFixSeverity.Blocking, 50),
            action("warn_1", QuickFixSeverity.Warning, 60),
            action("warn_2", QuickFixSeverity.Warning, 70),
            action("warn_3", QuickFixSeverity.Warning, 80),
            action(QuickFixRefreshAllSecurityChecksCode, QuickFixSeverity.Warning, 900)
        )

        val display = selectPreparationQuickFixActionsForDisplay(
            actions = actions,
            lowRamProfile = LowRamProfile(enabled = true, severe = false, ultra = false)
        )

        assertEquals("block_1", display.primary?.code)
        assertEquals(listOf("block_2", "block_3", "block_4"), display.blocking.map { it.code })
        assertEquals(listOf("warn_1", "warn_2"), display.warnings.map { it.code })
        assertEquals(QuickFixRefreshAllSecurityChecksCode, display.refresh?.code)
        assertEquals(5, display.blockingCount)
        assertEquals(3, display.warningCount)
    }

    @Test
    fun ultraDisplayKeepsOnlyPrimaryRefreshAndPinningNotice() {
        val actions = listOf(
            action(
                QuickFixScreenPinningDeferredCode,
                QuickFixSeverity.Warning,
                5,
                isNotice = true
            ),
            action("block_1", QuickFixSeverity.Blocking, 10),
            action("block_2", QuickFixSeverity.Blocking, 20),
            action("warn_1", QuickFixSeverity.Warning, 30),
            action(QuickFixRefreshAllSecurityChecksCode, QuickFixSeverity.Warning, 900)
        )

        val display = selectPreparationQuickFixActionsForDisplay(
            actions = actions,
            lowRamProfile = LowRamProfile(enabled = true, severe = true, ultra = true)
        )

        assertEquals(QuickFixScreenPinningDeferredCode, display.notices.single().code)
        assertEquals("block_1", display.primary?.code)
        assertTrue(display.blocking.isEmpty())
        assertTrue(display.warnings.isEmpty())
        assertEquals(QuickFixRefreshAllSecurityChecksCode, display.refresh?.code)
    }

    private fun snapshot(vararg items: PreExamHealthItem): PreExamHealthSnapshot =
        PreExamHealthSnapshot(
            compatibilityFamily = DeviceCompatibilityFamily.Generic,
            compatibilityLabel = "Android",
            generatedAtElapsedMs = 0L,
            items = items.toList()
        )

    private fun action(
        code: String,
        severity: QuickFixSeverity,
        priority: Int,
        isNotice: Boolean = false
    ): PreparationQuickFixAction =
        PreparationQuickFixAction(
            code = code,
            text = code,
            severity = severity,
            target = QuickFixTarget.All,
            priority = priority,
            isNotice = isNotice,
            onClick = {}
        )
}
