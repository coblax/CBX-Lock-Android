package com.coblax.examlock.ui.admin

import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.viewmodel.CustomQrDraftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUiFlowTest {
    @Test
    fun activeSecurityOverrideCountIncludesEveryVisibleOverride() {
        val settings = AdminSettings(
            bypassScreenPinning = true,
            bypassVpn = true,
            bypassApkIntegrity = true,
            bypassMultiWindow = true
        )

        assertEquals(4, activeSecurityOverrideCount(settings))
    }

    @Test
    fun customQrForwardNavigationRequiresCompleteExamData() {
        val incomplete = CustomQrDraftState(examUrl = "https://exam.example")

        assertFalse(isCustomQrExamStepComplete(incomplete))
        assertFalse(
            canOpenCustomQrStep(
                target = CustomQrAdminTab.Location,
                draft = incomplete,
                locationConfigurationValid = true
            )
        )
    }

    @Test
    fun customQrGenerateRequiresValidEnabledGeofence() {
        val draft = CustomQrDraftState(
            examUrl = "https://exam.example",
            examName = "Final Exam",
            startTime = "2026-07-30 08:00",
            endTime = "2026-07-30 10:00",
            geofenceEnabled = true
        )

        assertTrue(isCustomQrExamStepComplete(draft))
        assertFalse(
            canOpenCustomQrStep(
                target = CustomQrAdminTab.Generate,
                draft = draft,
                locationConfigurationValid = false
            )
        )
        assertTrue(
            canOpenCustomQrStep(
                target = CustomQrAdminTab.Generate,
                draft = draft,
                locationConfigurationValid = true
            )
        )
    }
}
