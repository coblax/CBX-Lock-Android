package com.coblax.examlock.format

import com.coblax.examlock.model.DiagnosticSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticParityContractTest {
    @Test
    fun parityMatrixCoversCorePreparationTelegramAndRuntimeSections() {
        val sections = diagnosticParityContracts().map { it.section }.toSet()

        listOf(
            DiagnosticSection.Network,
            DiagnosticSection.SecurityHealth,
            DiagnosticSection.DeviceTime,
            DiagnosticSection.ScreenPinning,
            DiagnosticSection.ScreenRecorder,
            DiagnosticSection.DisplayMirror,
            DiagnosticSection.MultiWindow,
            DiagnosticSection.AppSwitch,
            DiagnosticSection.Overlay,
            DiagnosticSection.Geofence,
            DiagnosticSection.FakeLocation,
            DiagnosticSection.Clipboard,
            DiagnosticSection.Bluetooth,
            DiagnosticSection.Accessibility,
            DiagnosticSection.DeveloperAdb,
            DiagnosticSection.Root,
            DiagnosticSection.Signature,
            DiagnosticSection.VirtualEnvironment
        ).forEach { section ->
            assertTrue("Missing parity contract for $section", sections.contains(section))
        }
    }

    @Test
    fun parityContractsHaveRequiredSurfaceTokens() {
        diagnosticParityContracts().forEach { contract ->
            assertFalse("${contract.section} preparation tokens are empty", contract.preparationTokens.isEmpty())
            assertFalse("${contract.section} telegram tokens are empty", contract.telegramTokens.isEmpty())
        }
    }

    @Test
    fun parityContractEventCodesStayInTelegramFilter() {
        diagnosticParityContracts().forEach { contract ->
            val filteredCodes = diagnosticSectionEventCodes(contract.section)
            contract.eventCodes.forEach { expectedCode ->
                assertTrue(
                    "${contract.section} missing event code $expectedCode",
                    filteredCodes.contains(expectedCode)
                )
            }
        }
    }

    @Test
    fun sendReportActionsStayScopedToExistingRuntimeDialogs() {
        val contracts = diagnosticParityContracts().associateBy { it.section }
        val sectionsWithRuntimeReport = setOf(
            DiagnosticSection.Network,
            DiagnosticSection.ScreenRecorder,
            DiagnosticSection.DisplayMirror,
            DiagnosticSection.MultiWindow
        )

        assertTrue(
            contracts.getValue(DiagnosticSection.Network).primaryActions.contains("Send Network Report")
        )
        listOf(
            DiagnosticSection.ScreenRecorder,
            DiagnosticSection.DisplayMirror,
            DiagnosticSection.MultiWindow
        ).forEach { section ->
            assertTrue(
                "$section should keep Send Report as an existing runtime action",
                contracts.getValue(section).primaryActions.contains("Send Report")
            )
        }
        contracts.filterKeys { section -> section !in sectionsWithRuntimeReport }.forEach { (section, contract) ->
            assertFalse(
                "$section should not gain a runtime Send Report action in this polish pass",
                contract.primaryActions.any { action -> action.contains("Send Report") }
            )
        }
    }
}
