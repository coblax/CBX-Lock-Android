package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ExamSecurityPreparationScreen(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    ExamSecurityPreparationScreenContent(
        state = state,
        actions = actions,
        modifier = modifier
    )
}
