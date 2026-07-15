package com.coblax.examlock.ui.exam

import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

internal class ExamRuntimeWebViewUiState(
    val loadingProgress: MutableFloatState,
    val stopRequested: MutableState<Boolean>,
    val instance: MutableState<SecureExamWebView?>,
    val generation: MutableState<Long>,
    val destroyedGeneration: MutableState<Long?>,
    val fullScreenCustomView: MutableState<View?>,
    val fullScreenCustomViewCallback: MutableState<WebChromeClient.CustomViewCallback?>,
    val fullScreenContainer: FrameLayout,
    val recoveryState: MutableState<ExamRuntimeRecoveryState>
)

@Composable
internal fun rememberExamRuntimeWebViewUiState(context: Context): ExamRuntimeWebViewUiState {
    val loadingProgress = remember { mutableFloatStateOf(0f) }
    val stopRequested = remember { mutableStateOf(false) }
    val instance = remember { mutableStateOf<SecureExamWebView?>(null) }
    val generation = remember { mutableStateOf(0L) }
    val destroyedGeneration = remember { mutableStateOf<Long?>(null) }
    val fullScreenCustomView = remember { mutableStateOf<View?>(null) }
    val fullScreenCustomViewCallback =
        remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val fullScreenContainer = remember {
        FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }
    val recoveryState = rememberSaveable { mutableStateOf(ExamRuntimeRecoveryState.Idle) }
    return remember(context) {
        ExamRuntimeWebViewUiState(
            loadingProgress = loadingProgress,
            stopRequested = stopRequested,
            instance = instance,
            generation = generation,
            destroyedGeneration = destroyedGeneration,
            fullScreenCustomView = fullScreenCustomView,
            fullScreenCustomViewCallback = fullScreenCustomViewCallback,
            fullScreenContainer = fullScreenContainer,
            recoveryState = recoveryState
        )
    }
}
