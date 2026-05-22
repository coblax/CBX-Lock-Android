package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile

internal fun initialPreparationWizardMode(lowRamProfile: LowRamProfile): Boolean =
    lowRamProfile.enabled

@Composable
internal fun ExamSecurityPreparationScreen(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    val lowRamProfile = LocalLowRamProfile.current
    var wizardMode by rememberSaveable(lowRamProfile.enabled) {
        mutableStateOf(initialPreparationWizardMode(lowRamProfile))
    }

    if (wizardMode) {
        PreparationWizardScreen(
            state = state,
            actions = actions,
            onSwitchToChecklist = { wizardMode = false },
            modifier = modifier
        )
    } else {
        ExamSecurityPreparationScreenContent(
            state = state,
            actions = actions,
            onSwitchToWizard = { wizardMode = true },
            modifier = modifier
        )
    }
}
