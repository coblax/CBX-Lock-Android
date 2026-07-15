package com.coblax.examlock

import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityLabelsTest {
    @Test
    fun selectToSpeakServiceGetsUserFacingLabel() {
        val component =
            "com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService"

        assertEquals(
            "Select to Speak / Pilih untuk Diucapkan",
            accessibilityServiceFriendlyLabel(component)
        )
    }

    @Test
    fun blockingFixMentionsDetectedServiceAndOtherCommonCauses() {
        val inspection = AccessibilityInspectionResult(
            managerEnabled = true,
            touchExplorationEnabled = false,
            rawEnabledServices =
                "com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService",
            activeServiceComponents = listOf(
                "com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
            ),
            activePackages = listOf("com.google.android.marvin.talkback"),
            allowedServiceComponents = emptyList(),
            allowedPackages = emptyList(),
            effectiveServiceComponents = listOf(
                "com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
            ),
            effectivePackages = listOf("com.google.android.marvin.talkback"),
            riskyPackages = emptyList()
        )

        val fixText = accessibilityBlockingFixText(inspection, UiLanguage.Indonesian)

        assertTrue(fixText.contains("Select to Speak"))
        assertTrue(fixText.contains("TalkBack"))
        assertTrue(fixText.contains("auto clicker"))
        assertTrue(fixText.contains("Pengaturan > Aksesibilitas"))
    }
}
