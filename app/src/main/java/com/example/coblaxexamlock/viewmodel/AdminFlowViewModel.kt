package com.example.coblaxexamlock.viewmodel
import com.example.coblaxexamlock.ExamScheduleDefaults
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.GeofenceVertex
import com.example.coblaxexamlock.model.AppScreen
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

internal data class AdminFlowUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val showSecretAdmin: Boolean = false,
    val showCustomQrAdmin: Boolean = false,
    val showScanSourceDialog: Boolean = false,
    val scanErrorMessage: String? = null,
    val showAdminPasswordDialog: Boolean = false,
    val adminPasswordInput: String = "",
    val adminPasswordError: String? = null,
    val selectedSecretTab: String = "setup",
    val selectedCustomQrTab: String = "exam",
    val customQrDraft: CustomQrDraftState = CustomQrDraftState(),
    val showCircleMapEditor: Boolean = false,
    val showPolygonMapEditor: Boolean = false,
    val generatedQrPayload: String? = null,
    val generationStatus: String? = null,
    val generationIsError: Boolean = false,
    val directLinkDraftUrl: String = "",
    val infoDialogTitle: String? = null,
    val infoDialogMessage: String? = null
)

internal data class CustomQrDraftState(
    val examUrl: String = "",
    val examName: String = "",
    val startTime: String = ExamScheduleDefaults.defaultQrWindow().startDateTime,
    val endTime: String = ExamScheduleDefaults.defaultQrWindow().endDateTime,
    val geofenceEnabled: Boolean = false,
    val geofenceShapeTypeName: String = GeofenceShapeType.Circle.name,
    val geofenceCenterLat: String = "",
    val geofenceCenterLng: String = "",
    val geofenceRadiusMeters: String = "",
    val polygonVertices: List<GeofenceVertex> = emptyList(),
    val geofenceCircleCenters: List<GeofenceVertex> = emptyList(),
    val saveToDirectLink: Boolean = false
)

internal sealed interface AdminFlowUiAction {
    data class SetCurrentScreen(val screen: AppScreen) : AdminFlowUiAction
    data object OpenSecretAdmin : AdminFlowUiAction
    data object CloseSecretAdmin : AdminFlowUiAction
    data object OpenCustomQrAdmin : AdminFlowUiAction
    data object CloseCustomQrAdmin : AdminFlowUiAction
    data object ShowScanSourceDialog : AdminFlowUiAction
    data object HideScanSourceDialog : AdminFlowUiAction
    data class SetScanErrorMessage(val message: String?) : AdminFlowUiAction
    data object ShowAdminPasswordDialog : AdminFlowUiAction
    data object HideAdminPasswordDialog : AdminFlowUiAction
    data class SetAdminPasswordInput(val value: String) : AdminFlowUiAction
    data class SetAdminPasswordError(val message: String?) : AdminFlowUiAction
    data class SelectSecretTab(val tab: String) : AdminFlowUiAction
    data class SelectCustomQrTab(val tab: String) : AdminFlowUiAction
    data class SetCustomQrDraft(val draft: CustomQrDraftState) : AdminFlowUiAction
    data object ResetCustomQrDraft : AdminFlowUiAction
    data class SetShowCircleMapEditor(val show: Boolean) : AdminFlowUiAction
    data class SetShowPolygonMapEditor(val show: Boolean) : AdminFlowUiAction
    data class SetGeneratedQrPayload(val payload: String?) : AdminFlowUiAction
    data class SetGenerationStatus(val message: String?) : AdminFlowUiAction
    data class SetGenerationIsError(val isError: Boolean) : AdminFlowUiAction
    data class SetDirectLinkDraftUrl(val value: String) : AdminFlowUiAction
    data class ShowInfoDialog(val title: String, val message: String) : AdminFlowUiAction
    data object HideInfoDialog : AdminFlowUiAction
}

internal sealed interface AdminFlowUiEffect {
    data object RequestQrScanSource : AdminFlowUiEffect
    data object RequestQrFilePicker : AdminFlowUiEffect
}

internal class AdminFlowViewModel : ViewModel() {
    val instanceId: String = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(AdminFlowUiState())
    val uiState: StateFlow<AdminFlowUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AdminFlowUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AdminFlowUiEffect> = _effects.asSharedFlow()

    fun dispatch(action: AdminFlowUiAction) {
        when (action) {
            is AdminFlowUiAction.SetCurrentScreen -> _uiState.update { it.copy(currentScreen = action.screen) }
            AdminFlowUiAction.OpenSecretAdmin -> _uiState.update {
                it.copy(
                    currentScreen = AppScreen.SecretAdmin,
                    showSecretAdmin = true,
                    showCustomQrAdmin = false,
                    selectedSecretTab = "setup"
                )
            }
            AdminFlowUiAction.CloseSecretAdmin -> _uiState.update {
                it.copy(currentScreen = AppScreen.Home, showSecretAdmin = false, selectedSecretTab = "setup")
            }
            AdminFlowUiAction.OpenCustomQrAdmin -> _uiState.update {
                it.copy(
                    currentScreen = AppScreen.CustomQrAdmin,
                    showCustomQrAdmin = true,
                    showSecretAdmin = false,
                    selectedCustomQrTab = "exam",
                    customQrDraft = CustomQrDraftState(),
                    showCircleMapEditor = false,
                    showPolygonMapEditor = false,
                    generatedQrPayload = null,
                    generationStatus = null,
                    generationIsError = false
                )
            }
            AdminFlowUiAction.CloseCustomQrAdmin -> _uiState.update {
                it.copy(
                    currentScreen = AppScreen.Home,
                    showCustomQrAdmin = false,
                    selectedCustomQrTab = "exam",
                    customQrDraft = CustomQrDraftState(),
                    showCircleMapEditor = false,
                    showPolygonMapEditor = false,
                    generatedQrPayload = null,
                    generationStatus = null,
                    generationIsError = false
                )
            }
            AdminFlowUiAction.ShowScanSourceDialog -> _uiState.update { it.copy(showScanSourceDialog = true) }
            AdminFlowUiAction.HideScanSourceDialog -> _uiState.update { it.copy(showScanSourceDialog = false) }
            is AdminFlowUiAction.SetScanErrorMessage -> _uiState.update { it.copy(scanErrorMessage = action.message) }
            AdminFlowUiAction.ShowAdminPasswordDialog -> _uiState.update { it.copy(showAdminPasswordDialog = true) }
            AdminFlowUiAction.HideAdminPasswordDialog -> _uiState.update { it.copy(showAdminPasswordDialog = false) }
            is AdminFlowUiAction.SetAdminPasswordInput -> _uiState.update { it.copy(adminPasswordInput = action.value) }
            is AdminFlowUiAction.SetAdminPasswordError -> _uiState.update { it.copy(adminPasswordError = action.message) }
            is AdminFlowUiAction.SelectSecretTab -> _uiState.update { it.copy(selectedSecretTab = action.tab) }
            is AdminFlowUiAction.SelectCustomQrTab -> _uiState.update { it.copy(selectedCustomQrTab = action.tab) }
            is AdminFlowUiAction.SetCustomQrDraft -> _uiState.update { it.copy(customQrDraft = action.draft) }
            AdminFlowUiAction.ResetCustomQrDraft -> _uiState.update { it.copy(customQrDraft = CustomQrDraftState()) }
            is AdminFlowUiAction.SetShowCircleMapEditor -> _uiState.update {
                it.copy(showCircleMapEditor = action.show)
            }
            is AdminFlowUiAction.SetShowPolygonMapEditor -> _uiState.update {
                it.copy(showPolygonMapEditor = action.show)
            }
            is AdminFlowUiAction.SetGeneratedQrPayload -> _uiState.update { it.copy(generatedQrPayload = action.payload) }
            is AdminFlowUiAction.SetGenerationStatus -> _uiState.update { it.copy(generationStatus = action.message) }
            is AdminFlowUiAction.SetGenerationIsError -> _uiState.update { it.copy(generationIsError = action.isError) }
            is AdminFlowUiAction.SetDirectLinkDraftUrl -> _uiState.update { it.copy(directLinkDraftUrl = action.value) }
            is AdminFlowUiAction.ShowInfoDialog -> _uiState.update {
                it.copy(infoDialogTitle = action.title, infoDialogMessage = action.message)
            }
            AdminFlowUiAction.HideInfoDialog -> _uiState.update {
                it.copy(infoDialogTitle = null, infoDialogMessage = null)
            }
        }
    }
}

