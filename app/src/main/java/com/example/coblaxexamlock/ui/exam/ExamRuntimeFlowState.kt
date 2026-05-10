package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.runtime.getCurrentInputMethodPackage
import com.example.coblaxexamlock.runtime.isAllowedExamKeyboard
import com.example.coblaxexamlock.runtime.resolveKeyboardAppLabel

internal class ExamRuntimeFlowUiState(
    val examSessionStarted: MutableState<Boolean>,
    val lockTaskRequestPending: MutableState<Boolean>,
    val pinningActivationState: MutableState<PinningActivationState>,
    val pinningActivationStartedAtElapsedMs: MutableState<Long?>,
    val pinningSuppressedTransitionCount: MutableIntState,
    val screenPinningMessage: MutableState<String?>,
    val showExitExamDialog: MutableState<Boolean>,
    val exitSessionClearInFlight: MutableState<Boolean>,
    val webViewErrorMessage: MutableState<String?>,
    val useBuiltInExamKeyboard: MutableState<Boolean>,
    val showBuiltInExamKeyboard: MutableState<Boolean>,
    val sideArrowControlsVisible: MutableState<Boolean>,
    val hasEditableFocus: MutableState<Boolean>,
    val builtInKeyboardShiftEnabled: MutableState<Boolean>,
    val geofencePermissionRequestInFlight: MutableState<Boolean>,
    val geofenceStartValidationInFlight: MutableState<Boolean>,
    val webViewSessionResetInFlight: MutableState<Boolean>,
    val webViewSessionResetError: MutableState<String?>,
    val geofenceManualRefreshInFlight: MutableState<Boolean>,
    val pendingStartExamAfterLocationPermission: MutableState<Boolean>,
    val retryStartExamAfterLocationPermissionGrant: MutableState<Boolean>,
    val geofenceViolationCount: MutableIntState,
    val showGeofenceViolationDialog: MutableState<Boolean>,
    val showGeofenceMapViewer: MutableState<Boolean>,
    val lastGeofenceTrigger: MutableState<String?>,
    val lastGeofenceAt: MutableState<String?>,
    val lastGeofenceContext: MutableState<String?>,
    val lastGeofenceRefreshAt: MutableState<String?>,
    val geofenceRuntimeEpisodeKey: MutableState<String?>,
    val fakeLocationViolationCount: MutableIntState,
    val showFakeLocationViolationDialog: MutableState<Boolean>,
    val lastFakeLocationTrigger: MutableState<String?>,
    val lastFakeLocationAt: MutableState<String?>,
    val lastFakeLocationContext: MutableState<String?>,
    val fakeLocationRuntimeEpisodeKey: MutableState<String?>,
    val lastFakeLocationWarningKey: MutableState<String?>,
    val currentKeyboardPackage: MutableState<String>,
    val currentKeyboardLabel: MutableState<String>,
    val lastKeyboardAllowed: MutableState<Boolean>
)

@Composable
internal fun rememberExamRuntimeFlowUiState(
    context: Context,
    bypassKeyboardPolicy: Boolean
): ExamRuntimeFlowUiState {
    val examSessionStarted = rememberSaveable { mutableStateOf(false) }
    val lockTaskRequestPending = rememberSaveable { mutableStateOf(false) }
    val pinningActivationState = rememberSaveable {
        mutableStateOf(PinningActivationState.Idle)
    }
    val pinningActivationStartedAtElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val pinningSuppressedTransitionCount = rememberSaveable { mutableIntStateOf(0) }
    val screenPinningMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val showExitExamDialog = rememberSaveable { mutableStateOf(false) }
    val exitSessionClearInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewErrorMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val useBuiltInExamKeyboard = rememberSaveable { mutableStateOf(false) }
    val showBuiltInExamKeyboard = rememberSaveable { mutableStateOf(false) }
    val sideArrowControlsVisible = rememberSaveable { mutableStateOf(true) }
    val hasEditableFocus = rememberSaveable { mutableStateOf(false) }
    val builtInKeyboardShiftEnabled = rememberSaveable { mutableStateOf(false) }
    val geofencePermissionRequestInFlight = rememberSaveable { mutableStateOf(false) }
    val geofenceStartValidationInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewSessionResetInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewSessionResetError = rememberSaveable { mutableStateOf<String?>(null) }
    val geofenceManualRefreshInFlight = rememberSaveable { mutableStateOf(false) }
    val pendingStartExamAfterLocationPermission = rememberSaveable { mutableStateOf(false) }
    val retryStartExamAfterLocationPermissionGrant = rememberSaveable { mutableStateOf(false) }
    val geofenceViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showGeofenceViolationDialog = rememberSaveable { mutableStateOf(false) }
    val showGeofenceMapViewer = rememberSaveable { mutableStateOf(false) }
    val lastGeofenceTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceContext = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceRefreshAt = rememberSaveable { mutableStateOf<String?>(null) }
    val geofenceRuntimeEpisodeKey = rememberSaveable { mutableStateOf<String?>(null) }
    val fakeLocationViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showFakeLocationViolationDialog = rememberSaveable { mutableStateOf(false) }
    val lastFakeLocationTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationContext = rememberSaveable { mutableStateOf<String?>(null) }
    val fakeLocationRuntimeEpisodeKey = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationWarningKey = rememberSaveable { mutableStateOf<String?>(null) }
    val currentKeyboardPackage = rememberSaveable {
        mutableStateOf(getCurrentInputMethodPackage(context).orEmpty())
    }
    val currentKeyboardLabel = rememberSaveable {
        mutableStateOf(resolveKeyboardAppLabel(context, currentKeyboardPackage.value))
    }
    val lastKeyboardAllowed = rememberSaveable {
        mutableStateOf(
            bypassKeyboardPolicy || isAllowedExamKeyboard(context, currentKeyboardPackage.value)
        )
    }
    return remember {
        ExamRuntimeFlowUiState(
            examSessionStarted = examSessionStarted,
            lockTaskRequestPending = lockTaskRequestPending,
            pinningActivationState = pinningActivationState,
            pinningActivationStartedAtElapsedMs = pinningActivationStartedAtElapsedMs,
            pinningSuppressedTransitionCount = pinningSuppressedTransitionCount,
            screenPinningMessage = screenPinningMessage,
            showExitExamDialog = showExitExamDialog,
            exitSessionClearInFlight = exitSessionClearInFlight,
            webViewErrorMessage = webViewErrorMessage,
            useBuiltInExamKeyboard = useBuiltInExamKeyboard,
            showBuiltInExamKeyboard = showBuiltInExamKeyboard,
            sideArrowControlsVisible = sideArrowControlsVisible,
            hasEditableFocus = hasEditableFocus,
            builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
            geofencePermissionRequestInFlight = geofencePermissionRequestInFlight,
            geofenceStartValidationInFlight = geofenceStartValidationInFlight,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            webViewSessionResetError = webViewSessionResetError,
            geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
            pendingStartExamAfterLocationPermission = pendingStartExamAfterLocationPermission,
            retryStartExamAfterLocationPermissionGrant = retryStartExamAfterLocationPermissionGrant,
            geofenceViolationCount = geofenceViolationCount,
            showGeofenceViolationDialog = showGeofenceViolationDialog,
            showGeofenceMapViewer = showGeofenceMapViewer,
            lastGeofenceTrigger = lastGeofenceTrigger,
            lastGeofenceAt = lastGeofenceAt,
            lastGeofenceContext = lastGeofenceContext,
            lastGeofenceRefreshAt = lastGeofenceRefreshAt,
            geofenceRuntimeEpisodeKey = geofenceRuntimeEpisodeKey,
            fakeLocationViolationCount = fakeLocationViolationCount,
            showFakeLocationViolationDialog = showFakeLocationViolationDialog,
            lastFakeLocationTrigger = lastFakeLocationTrigger,
            lastFakeLocationAt = lastFakeLocationAt,
            lastFakeLocationContext = lastFakeLocationContext,
            fakeLocationRuntimeEpisodeKey = fakeLocationRuntimeEpisodeKey,
            lastFakeLocationWarningKey = lastFakeLocationWarningKey,
            currentKeyboardPackage = currentKeyboardPackage,
            currentKeyboardLabel = currentKeyboardLabel,
            lastKeyboardAllowed = lastKeyboardAllowed
        )
    }
}
