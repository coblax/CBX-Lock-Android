package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

internal class ExamRuntimeRuntimeCacheState(
    val reverseEngineeringRefreshCache: MutableState<RuntimeReverseEngineeringRefreshCache?>,
    val integrityRefreshCache: MutableState<RuntimeIntegrityRefreshCache?>,
    val lastRuntimeMemoryActionSummary: MutableState<String?>
)

@Composable
internal fun rememberExamRuntimeRuntimeCacheState(): ExamRuntimeRuntimeCacheState {
    val reverseEngineeringRefreshCache =
        remember { mutableStateOf<RuntimeReverseEngineeringRefreshCache?>(null) }
    val integrityRefreshCache = remember { mutableStateOf<RuntimeIntegrityRefreshCache?>(null) }
    val lastRuntimeMemoryActionSummary = rememberSaveable { mutableStateOf<String?>(null) }
    return remember {
        ExamRuntimeRuntimeCacheState(
            reverseEngineeringRefreshCache = reverseEngineeringRefreshCache,
            integrityRefreshCache = integrityRefreshCache,
            lastRuntimeMemoryActionSummary = lastRuntimeMemoryActionSummary
        )
    }
}
