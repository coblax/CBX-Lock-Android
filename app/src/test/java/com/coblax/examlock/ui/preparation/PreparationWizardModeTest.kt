package com.coblax.examlock.ui.preparation

import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.model.UiLanguage
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
    fun lowRamWizardPayloadUsesActiveStepOnlyEvenWhenTechnicalDetailsOpen() {
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
            PreparationWizardPayloadBuildMode.ActiveStepOnly,
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
    fun lowRamChecklistDoesNotBuildFullTechnicalTextEagerly() {
        assertTrue(
            shouldBuildFullPreparationChecklistText(
                lowRamProfile = LowRamProfile(),
                showFullChecklist = true
            )
        )
        assertFalse(
            shouldBuildFullPreparationChecklistText(
                lowRamProfile = LowRamProfile(enabled = true),
                showFullChecklist = true
            )
        )
        assertFalse(
            shouldBuildFullPreparationChecklistText(
                lowRamProfile = LowRamProfile(enabled = true, severe = true, ultra = true),
                showFullChecklist = false
            )
        )
    }

    @Test
    fun loadingChecklistTextKeepsWizardResponsiveBeforeDetailsAreReady() {
        val english = loadingPreparationChecklistText(UiLanguage.English)
        val indonesian = loadingPreparationChecklistText(UiLanguage.Indonesian)

        assertEquals("Loading details...", english.networkValue)
        assertEquals("Checking", english.networkStatusLabel)
        assertEquals("Memuat detail...", indonesian.networkValue)
        assertEquals("Mengecek", indonesian.networkStatusLabel)
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

    @Test
    fun wizardFilteringUsesExplicitSectionMetadataFirst() {
        val actionsBySection = WizardStep.entries.map { step ->
            PreparationQuickFixAction(
                code = "section_${step.name}",
                text = step.name,
                severity = QuickFixSeverity.Blocking,
                target = null,
                priority = 1,
                section = step.preparationSection(),
                onClick = {}
            )
        } + PreparationQuickFixAction(
            code = QuickFixRefreshAllSecurityChecksCode,
            text = "Refresh",
            severity = QuickFixSeverity.Warning,
            target = null,
            priority = 900,
            onClick = {}
        )

        WizardStep.entries.forEach { step ->
            val filtered = filterQuickFixActionsForStep(step, actionsBySection)

            assertEquals(listOf("section_${step.name}"), filtered.map { it.code })
        }
    }

    @Test
    fun firstIssueWizardStepIndexFocusesLocationIssue() {
        val states = WizardStep.entries.map { step ->
            WizardStepState(
                step = step,
                isCompleted = step != WizardStep.Location,
                issueCount = if (step == WizardStep.Location) 1 else 0
            )
        }

        assertEquals(
            WizardStep.Location.ordinal,
            resolveFirstIssueWizardStepIndex(states)
        )
    }

    @Test
    fun userSelectedWizardStepPreventsAutoFocusMove() {
        val states = WizardStep.entries.map { step ->
            WizardStepState(
                step = step,
                isCompleted = step != WizardStep.Location,
                issueCount = if (step == WizardStep.Location) 1 else 0
            )
        }

        assertEquals(
            WizardStep.DeviceSetup.ordinal,
            resolveWizardStepIndexForAutoFocus(
                currentStepIndex = WizardStep.DeviceSetup.ordinal,
                stepStates = states,
                userSelectedWizardStep = true,
                autoFocusApplied = false
            )
        )
    }

    @Test
    fun wizardStepActionCoverageShowsHintWhenIssueHasNoAction() {
        val coverage = resolveWizardStepActionCoverage(
            stepState = WizardStepState(
                step = WizardStep.Location,
                isCompleted = false,
                issueCount = 1
            ),
            quickFixActions = listOf(
                PreparationQuickFixAction(
                    code = QuickFixRefreshAllSecurityChecksCode,
                    text = "Refresh",
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 900,
                    onClick = {}
                )
            )
        )

        assertTrue(coverage.showManualFixHint)
    }

    @Test
    fun wizardStepActionCoverageHidesHintWhenSectionHasNoIssue() {
        val coverage = resolveWizardStepActionCoverage(
            stepState = WizardStepState(
                step = WizardStep.Location,
                isCompleted = true,
                issueCount = 0
            ),
            quickFixActions = emptyList()
        )

        assertFalse(coverage.showManualFixHint)
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
            reverseEngineeringReady = true,
            integrityReady = true,
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
