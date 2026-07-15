package com.coblax.examlock

import com.coblax.examlock.ui.preparation.PreExamHealthCategory
import com.coblax.examlock.ui.preparation.PreExamHealthItem
import com.coblax.examlock.ui.preparation.PreExamHealthSnapshot
import com.coblax.examlock.ui.preparation.PreExamHealthVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSurvivalPolicyTest {
    @Test
    fun normalHealthyDeviceScoresExcellent() {
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(enabled = false, severe = false),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            )
        )

        assertEquals(CompatibilityScore.Excellent, policy.score)
        assertEquals(DeviceSurvivalRuntimeTier.Standard, policy.runtimeTier)
        assertTrue(policy.startExamAllowedByHealth)
    }

    @Test
    fun severeLowRamWithoutBlockerScoresGoodButConstrained() {
        val lowRam = LowRamProfile(enabled = true, severe = true)
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = lowRam,
            deviceCompatibilityProfile = DeviceCompatibilityProfile(lowRamProfile = lowRam),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            )
        )

        assertEquals(CompatibilityScore.Good, policy.score)
        assertEquals(DeviceSurvivalRuntimeTier.Constrained, policy.runtimeTier)
        assertEquals(DeviceSurvivalUiTier.SevereLowRam, policy.uiTier)
    }

    @Test
    fun healthWarningIsAdvisoryAndDoesNotBlockStart() {
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            ),
            preExamHealthSnapshot = snapshot(
                PreExamHealthItem(
                    category = PreExamHealthCategory.Network,
                    verdict = PreExamHealthVerdict.Warning,
                    title = "Network",
                    detail = "Captive portal",
                    quickFix = "Open internet settings."
                )
            )
        )

        assertTrue(policy.startExamAllowedByHealth)
        assertEquals(CompatibilityScore.Good, policy.score)
        assertEquals(1, policy.warningActionCount)
    }

    @Test
    fun unavailableWebViewIsNotRecommendedAndBlocksHealthStart() {
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = null,
                versionName = null
            )
        )

        assertEquals(CompatibilityScore.NotRecommended, policy.score)
        assertFalse(policy.startExamAllowedByHealth)
        assertTrue(policy.recommendedActions.any { it.code == "webview_provider" && it.blocking })
    }

    @Test
    fun oldWebViewNeedsSetupWithoutBlockingStart() {
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "74.0.3729.186"
            )
        )

        assertEquals(CompatibilityScore.NeedsSetup, policy.score)
        assertTrue(policy.startExamAllowedByHealth)
        assertTrue(policy.recommendedActions.any { it.code == "webview_provider" && !it.blocking })
    }

    @Test
    fun unknownWebViewVersionNeedsSetupWithoutBlockingStart() {
        val policy = buildDeviceSurvivalPolicy(
            lowRamProfile = LowRamProfile(),
            deviceCompatibilityProfile = DeviceCompatibilityProfile(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.chrome",
                versionName = "vendor-build"
            )
        )

        assertEquals(CompatibilityScore.NeedsSetup, policy.score)
        assertTrue(policy.startExamAllowedByHealth)
        assertTrue(policy.recommendedActions.any { it.code == "webview_provider" && !it.blocking })
    }

    private fun snapshot(vararg items: PreExamHealthItem): PreExamHealthSnapshot =
        PreExamHealthSnapshot(
            compatibilityFamily = DeviceCompatibilityFamily.Generic,
            compatibilityLabel = "Android",
            generatedAtElapsedMs = 0L,
            items = items.toList()
        )
}
