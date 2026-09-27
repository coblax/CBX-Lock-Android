package com.coblax.examlock.viewmodel

import com.coblax.examlock.model.AdminSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminFlowViewModelTest {
    private val persisted = AdminSettings(
        fastExamUrl = "https://exam.example",
        fastExamLabel = "Exam"
    )

    @Test
    fun draftSurvivesRepeatedInitializationUntilReverted() {
        val viewModel = AdminFlowViewModel()
        viewModel.dispatch(AdminFlowUiAction.InitializeAdminSettings(persisted))
        viewModel.dispatch(
            AdminFlowUiAction.UpdateAdminSettingsDraft(
                persisted.copy(fastExamLabel = "Draft")
            )
        )
        viewModel.dispatch(
            AdminFlowUiAction.InitializeAdminSettings(
                persisted.copy(fastExamLabel = "External")
            )
        )

        assertTrue(viewModel.uiState.value.hasUnsavedAdminSettings)
        assertEquals("Draft", viewModel.uiState.value.draftAdminSettings?.fastExamLabel)

        viewModel.dispatch(AdminFlowUiAction.RevertAdminSettingsDraft)

        assertFalse(viewModel.uiState.value.hasUnsavedAdminSettings)
        assertEquals(persisted, viewModel.uiState.value.draftAdminSettings)
    }

    @Test
    fun committedSettingsBecomeNewPersistedAndDraftState() {
        val viewModel = AdminFlowViewModel()
        val updated = persisted.copy(fastExamLabel = "Updated")
        viewModel.dispatch(AdminFlowUiAction.InitializeAdminSettings(persisted))
        viewModel.dispatch(AdminFlowUiAction.UpdateAdminSettingsDraft(updated))
        viewModel.dispatch(AdminFlowUiAction.CommitAppliedAdminSettings(updated))

        val state = viewModel.uiState.value
        assertEquals(updated, state.persistedAdminSettings)
        assertEquals(updated, state.draftAdminSettings)
        assertEquals(AdminApplyState.Success, state.adminApplyState)
        assertFalse(state.hasUnsavedAdminSettings)
    }

    @Test
    fun closingSecretAdminClearsUncommittedDraft() {
        val viewModel = AdminFlowViewModel()
        viewModel.dispatch(AdminFlowUiAction.InitializeAdminSettings(persisted))
        viewModel.dispatch(
            AdminFlowUiAction.UpdateAdminSettingsDraft(
                persisted.copy(fastExamLabel = "Draft")
            )
        )

        viewModel.dispatch(AdminFlowUiAction.CloseSecretAdmin)

        assertNull(viewModel.uiState.value.persistedAdminSettings)
        assertNull(viewModel.uiState.value.draftAdminSettings)
        assertEquals(AdminApplyState.Idle, viewModel.uiState.value.adminApplyState)
    }

    /** A stray Back used to throw away the URL, schedule, drawn exam area and bypasses. */
    @Test
    fun leavingCustomQrKeepsTheDraftUntilStartingOver() {
        val viewModel = AdminFlowViewModel()
        val draft = CustomQrDraftState(examUrl = "https://exam.example", examName = "UTS")
        viewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
        assertFalse(viewModel.uiState.value.customQrDraftResumed)
        viewModel.dispatch(AdminFlowUiAction.SetCustomQrDraft(draft))
        viewModel.dispatch(AdminFlowUiAction.SetGeneratedQrPayload("CBXEL2:qr"))

        viewModel.dispatch(AdminFlowUiAction.CloseCustomQrAdmin)
        viewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)

        var state = viewModel.uiState.value
        assertEquals(draft, state.customQrDraft)
        assertEquals("CBXEL2:qr", state.generatedQrPayload)
        assertTrue(state.customQrDraftResumed)

        viewModel.dispatch(AdminFlowUiAction.ResetCustomQrDraft)

        state = viewModel.uiState.value
        assertFalse(state.customQrDraft.hasContent)
        assertNull(state.generatedQrPayload)
        assertFalse(state.customQrDraftResumed)
    }
}
