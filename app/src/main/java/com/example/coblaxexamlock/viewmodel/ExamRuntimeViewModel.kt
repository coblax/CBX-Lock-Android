package com.example.coblaxexamlock.viewmodel
import com.example.coblaxexamlock.model.DiagnosticSection
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class PreparationChecklistUiState(
    val hasWarnings: Boolean = false,
    val canStartExam: Boolean = false,
    val networkLabel: String = "",
    val geofenceLabel: String = "",
    val fakeLocationLabel: String = "",
    val deviceTimeLabel: String = ""
)

internal data class ExamRuntimeChromeUiState(
    val loadingProgress: Float = 0f,
    val hasWebViewError: Boolean = false,
    val hasFullscreenCustomView: Boolean = false,
    val builtInKeyboardVisible: Boolean = false,
    val hasEditableFocus: Boolean = false
)

internal data class ExamRuntimeDialogsUiState(
    val pendingDiagnosticSection: DiagnosticSection? = null,
    val showForcedExitAlarm: Boolean = false,
    val showOfflineWarning: Boolean = false,
    val showNetworkUnstableWarning: Boolean = false,
    val showGeofenceWarning: Boolean = false,
    val showFakeLocationWarning: Boolean = false,
    val showKeyboardWarning: Boolean = false,
    val showOverlayWarning: Boolean = false,
    val showBluetoothWarning: Boolean = false,
    val showClipboardWarning: Boolean = false,
    val showExitConfirmation: Boolean = false
)

internal data class ExamRuntimeUiState(
    val checklist: PreparationChecklistUiState = PreparationChecklistUiState(),
    val examStarted: Boolean = false,
    val chrome: ExamRuntimeChromeUiState = ExamRuntimeChromeUiState(),
    val dialogs: ExamRuntimeDialogsUiState = ExamRuntimeDialogsUiState()
) {
    // Convenience accessors — single source of truth is always dialogs.*
    val showOfflineWarning: Boolean get() = dialogs.showOfflineWarning
    val showNetworkUnstableWarning: Boolean get() = dialogs.showNetworkUnstableWarning
    val showGeofenceWarning: Boolean get() = dialogs.showGeofenceWarning
    val showFakeLocationWarning: Boolean get() = dialogs.showFakeLocationWarning
}

internal sealed interface ExamRuntimeUiAction {
    data class UpdateChecklist(val checklist: PreparationChecklistUiState) : ExamRuntimeUiAction
    data class UpdateChrome(val chrome: ExamRuntimeChromeUiState) : ExamRuntimeUiAction
    data class UpdateDialogs(val dialogs: ExamRuntimeDialogsUiState) : ExamRuntimeUiAction
    data object StartExamRequested : ExamRuntimeUiAction
    data object EndExamRequested : ExamRuntimeUiAction
    data object ShowOfflineWarning : ExamRuntimeUiAction
    data object HideOfflineWarning : ExamRuntimeUiAction
    data object ShowNetworkUnstableWarning : ExamRuntimeUiAction
    data object HideNetworkUnstableWarning : ExamRuntimeUiAction
    data object ShowGeofenceWarning : ExamRuntimeUiAction
    data object HideGeofenceWarning : ExamRuntimeUiAction
    data object ShowFakeLocationWarning : ExamRuntimeUiAction
    data object HideFakeLocationWarning : ExamRuntimeUiAction
    data object RequestExitConfirmation : ExamRuntimeUiAction
    data object DismissExitConfirmation : ExamRuntimeUiAction
    data class RequestSectionReport(val section: DiagnosticSection) : ExamRuntimeUiAction
    data object RefreshRequested : ExamRuntimeUiAction
}

internal sealed interface ExamRuntimeUiEffect {
    data object RefreshSystemChecks : ExamRuntimeUiEffect
    data object RequestLocationPermission : ExamRuntimeUiEffect
    data object OpenInternetSettings : ExamRuntimeUiEffect
    data class RequestSectionReport(val section: DiagnosticSection) : ExamRuntimeUiEffect
}

internal class ExamRuntimeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExamRuntimeUiState())
    val uiState: StateFlow<ExamRuntimeUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ExamRuntimeUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ExamRuntimeUiEffect> = _effects.asSharedFlow()

    fun dispatch(action: ExamRuntimeUiAction) {
        when (action) {
            is ExamRuntimeUiAction.UpdateChecklist -> _uiState.update { it.copy(checklist = action.checklist) }
            is ExamRuntimeUiAction.UpdateChrome -> _uiState.update { it.copy(chrome = action.chrome) }
            is ExamRuntimeUiAction.UpdateDialogs -> _uiState.update {
                it.copy(dialogs = action.dialogs)
            }
            ExamRuntimeUiAction.StartExamRequested -> _uiState.update { it.copy(examStarted = true) }
            ExamRuntimeUiAction.EndExamRequested -> _uiState.update { it.copy(examStarted = false) }
            ExamRuntimeUiAction.ShowOfflineWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showOfflineWarning = true))
            }
            ExamRuntimeUiAction.HideOfflineWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showOfflineWarning = false))
            }
            ExamRuntimeUiAction.ShowNetworkUnstableWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showNetworkUnstableWarning = true))
            }
            ExamRuntimeUiAction.HideNetworkUnstableWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showNetworkUnstableWarning = false))
            }
            ExamRuntimeUiAction.ShowGeofenceWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showGeofenceWarning = true))
            }
            ExamRuntimeUiAction.HideGeofenceWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showGeofenceWarning = false))
            }
            ExamRuntimeUiAction.ShowFakeLocationWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showFakeLocationWarning = true))
            }
            ExamRuntimeUiAction.HideFakeLocationWarning -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showFakeLocationWarning = false))
            }
            ExamRuntimeUiAction.RequestExitConfirmation -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showExitConfirmation = true))
            }
            ExamRuntimeUiAction.DismissExitConfirmation -> _uiState.update {
                it.copy(dialogs = it.dialogs.copy(showExitConfirmation = false))
            }
            is ExamRuntimeUiAction.RequestSectionReport ->
                _effects.tryEmit(ExamRuntimeUiEffect.RequestSectionReport(action.section))
            ExamRuntimeUiAction.RefreshRequested -> _effects.tryEmit(ExamRuntimeUiEffect.RefreshSystemChecks)
        }
    }
}

