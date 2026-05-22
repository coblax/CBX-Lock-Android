package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.LowRamProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationWizardModeTest {
    @Test
    fun lowRamProfilesStartInWizardMode() {
        assertFalse(initialPreparationWizardMode(LowRamProfile()))
        assertTrue(initialPreparationWizardMode(LowRamProfile(enabled = true)))
        assertTrue(
            initialPreparationWizardMode(
                LowRamProfile(enabled = true, severe = true, ultra = true)
            )
        )
    }

    @Test
    fun lowRamWizardPayloadUsesActiveStepOnlyUntilTechnicalDetailsOpen() {
        assertEquals(
            PreparationWizardPayloadBuildMode.ActiveStepOnly,
            resolvePreparationWizardPayloadBuildMode(
                LowRamProfile(enabled = true),
                showChecklistDetails = false
            )
        )
        assertEquals(
            PreparationWizardPayloadBuildMode.ActiveStepOnly,
            resolvePreparationWizardPayloadBuildMode(
                LowRamProfile(enabled = true, severe = true, ultra = true),
                showChecklistDetails = false
            )
        )
        assertEquals(
            PreparationWizardPayloadBuildMode.FullChecklist,
            resolvePreparationWizardPayloadBuildMode(
                LowRamProfile(enabled = true, severe = true, ultra = true),
                showChecklistDetails = true
            )
        )
        assertEquals(
            PreparationWizardPayloadBuildMode.FullChecklist,
            resolvePreparationWizardPayloadBuildMode(
                LowRamProfile(),
                showChecklistDetails = false
            )
        )
    }

    @Test
    fun wizardStepPayloadCarriesOnlyCurrentStepContext() {
        val text = checklistText()
        val readiness = readiness()
        val action = PreparationQuickFixAction(
            code = "quick_fix_01",
            text = "Fix",
            severity = QuickFixSeverity.Blocking,
            target = QuickFixTarget.Network,
            priority = 1,
            onClick = {}
        )
        val payload = createPreparationWizardStepPayload(
            currentStep = WizardStep.Connectivity,
            readiness = readiness,
            sectionHealthMap = mapOf(
                WizardStep.Connectivity.sectionKey to SectionHealth(
                    title = "Connectivity",
                    allClear = false,
                    issueCount = 1
                ),
                WizardStep.DeviceSetup.sectionKey to SectionHealth(
                    title = "Device Setup",
                    allClear = true,
                    issueCount = 0
                )
            ),
            sectionText = text,
            quickFixActions = listOf(action),
            buildMode = PreparationWizardPayloadBuildMode.ActiveStepOnly
        )

        assertEquals(WizardStep.Connectivity, payload.currentStep)
        assertSame(readiness, payload.readiness)
        assertSame(text, payload.sectionText)
        assertEquals(listOf(action), payload.quickFixActions)
        assertEquals("Connectivity", payload.sectionHealth?.title)
        assertFalse(payload.fullChecklistBuilt)
    }

    private fun readiness(): PreparationChecklistReadiness =
        PreparationChecklistReadiness(
            keyboardReady = true,
            bluetoothReady = true,
            accessibilityReady = true,
            adbReady = true,
            rootReady = true,
            virtualEnvironmentReady = true,
            vpnReady = true,
            clipboardReady = true,
            deviceTimeReady = true,
            geofenceReady = true,
            fakeLocationReady = true,
            overlayReady = true,
            accessibilityGuardReady = true,
            screenPinningReady = true,
            appSwitchReady = true,
            screenRecorderReady = true,
            displayMirrorReady = true,
            multiWindowReady = true,
            signatureReady = true,
            staticSecurityInitialScanComplete = true,
            canStartExam = true,
            hasBypassIndicators = false
        )

    private fun checklistText(): PreparationChecklistText =
        PreparationChecklistText(
            accessibilityStatusLabel = "Safe",
            overlayStatusLabel = "Safe",
            geofenceStatusLabel = "Inside",
            geofenceMeta = null,
            fakeLocationStatusLabel = "Clean",
            deviceTimeStatusLabel = "Safe",
            networkStatusLabel = "Stable",
            networkValue = "Connected",
            networkMeta = null,
            networkDetail = null,
            webViewProviderStatusLabel = "Ready",
            webViewProviderValue = "Ready",
            webViewProviderDetail = null,
            deviceTimeDetail = null,
            bluetoothStatusLabel = "Safe",
            developerStatusLabel = "Safe",
            keyboardStatusLabel = "Ready",
            rootStatusLabel = "Safe",
            signatureStatusLabel = "Safe",
            signatureValue = "Signature matches",
            virtualEnvironmentStatusLabel = "Safe",
            screenPinningStatusLabel = "Active",
            accessibilityGuardStatusLabel = "Optional",
            appSwitchStatusLabel = "Ready",
            keyboardDetail = null,
            bluetoothDetail = null,
            accessibilityDetail = null,
            overlayDetail = null,
            developerDetail = null,
            rootDetail = null,
            signatureDetail = null,
            virtualEnvironmentDetail = null,
            clipboardDetail = null,
            geofenceDetail = null,
            fakeLocationDetail = null,
            screenPinningDetail = null,
            accessibilityGuardDetail = null,
            screenRecorderDetail = null,
            displayMirrorDetail = null,
            multiWindowDetail = null,
            appSwitchDetail = null
        )
}
