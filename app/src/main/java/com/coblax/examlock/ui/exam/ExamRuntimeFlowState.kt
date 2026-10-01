package com.coblax.examlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.coblax.examlock.PinningActivationPurpose
import com.coblax.examlock.PinningActivationState
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.getCurrentInputMethodPackage
import com.coblax.examlock.runtime.isAllowedExamKeyboard
import com.coblax.examlock.runtime.resolveKeyboardAppLabel
import kotlinx.coroutines.CancellationException

internal enum class StartExamPreflightStep {
    Idle,
    Starting,
    TamperAndIntegrity,
    DeviceSecurity,
    DeviceTime,
    NetworkDns,
    ServerProbe,
    HealthSnapshot,
    StaticSecurity,
    LocationPermission,
    LocationValidation,
    PreparingWebView,
    Complete,
    Failed
}

internal class StartExamPreflightUiState(
    val visible: MutableState<Boolean>,
    val step: MutableState<StartExamPreflightStep>,
    val detail: MutableState<String?>,
    val startedAtElapsedMs: MutableState<Long?>,
    val slowHintVisible: MutableState<Boolean>,
    /** The student pressed Cancel; the running start stops at its next step instead of going on. */
    val cancelRequested: MutableState<Boolean> = mutableStateOf(false)
)

internal fun showStartExamPreflight(
    state: StartExamPreflightUiState,
    step: StartExamPreflightStep = StartExamPreflightStep.Starting,
    detail: String? = null,
    startedAtElapsedMs: Long
) {
    if (!state.visible.value) {
        state.startedAtElapsedMs.value = startedAtElapsedMs
        state.cancelRequested.value = false
    }
    state.visible.value = true
    state.step.value = step
    state.detail.value = detail
    state.slowHintVisible.value = false
}

internal fun updateStartExamPreflightStep(
    state: StartExamPreflightUiState,
    step: StartExamPreflightStep,
    detail: String? = null
) {
    if (!state.visible.value) {
        return
    }
    state.step.value = step
    state.detail.value = detail
    state.slowHintVisible.value = false
}

/**
 * Cancel on the "Preparing exam" dialog. It used to only hide the dialog while the start kept
 * running, so the exam could still start after the student had cancelled it.
 */
internal fun cancelStartExamPreflight(state: StartExamPreflightUiState) {
    state.cancelRequested.value = true
    hideStartExamPreflight(state)
}

/** Ends the running start (a coroutine) when the student cancelled it. */
internal fun StartExamPreflightUiState.throwIfCancelledByStudent() {
    if (cancelRequested.value) {
        throw CancellationException(StartExamCancelledByStudent)
    }
}

internal const val StartExamCancelledByStudent = "start_cancelled_by_student"

internal fun hideStartExamPreflight(state: StartExamPreflightUiState) {
    state.visible.value = false
    state.step.value = StartExamPreflightStep.Idle
    state.detail.value = null
    state.startedAtElapsedMs.value = null
    state.slowHintVisible.value = false
}

/**
 * The few phases the student sees as a checklist while Start runs. The steps underneath
 * are too many and too technical to list; the final clock check runs again at the end,
 * so the phase shown never goes back (see [resolveStartExamPreflightPhaseIndex]).
 */
internal enum class StartExamPreflightPhase {
    AppSecurity,
    TimeAndNetwork,
    DeviceChecks,
    ExamBrowser
}

internal fun StartExamPreflightStep.preflightPhase(): StartExamPreflightPhase = when (this) {
    StartExamPreflightStep.Idle,
    StartExamPreflightStep.Starting,
    StartExamPreflightStep.TamperAndIntegrity,
    StartExamPreflightStep.DeviceSecurity -> StartExamPreflightPhase.AppSecurity
    StartExamPreflightStep.DeviceTime,
    StartExamPreflightStep.NetworkDns,
    StartExamPreflightStep.ServerProbe -> StartExamPreflightPhase.TimeAndNetwork
    StartExamPreflightStep.HealthSnapshot,
    StartExamPreflightStep.StaticSecurity,
    StartExamPreflightStep.LocationPermission,
    StartExamPreflightStep.LocationValidation,
    StartExamPreflightStep.Failed -> StartExamPreflightPhase.DeviceChecks
    StartExamPreflightStep.PreparingWebView,
    StartExamPreflightStep.Complete -> StartExamPreflightPhase.ExamBrowser
}

/** The phase to show: never lower than one already reached in this run. */
internal fun resolveStartExamPreflightPhaseIndex(
    step: StartExamPreflightStep,
    highestReachedIndex: Int
): Int = maxOf(step.preflightPhase().ordinal, highestReachedIndex)

internal fun startExamPreflightPhaseLabel(
    phase: StartExamPreflightPhase,
    uiLanguage: UiLanguage
): String = when (phase) {
    StartExamPreflightPhase.AppSecurity -> localized(uiLanguage, "App and screen lock", "Aplikasi & kunci layar")
    StartExamPreflightPhase.TimeAndNetwork -> localized(uiLanguage, "Clock and network", "Jam & jaringan")
    StartExamPreflightPhase.DeviceChecks -> localized(uiLanguage, "Phone checks", "Pemeriksaan HP")
    StartExamPreflightPhase.ExamBrowser -> localized(uiLanguage, "Exam browser", "Browser ujian")
}

internal fun startExamPreflightStepLabel(
    step: StartExamPreflightStep,
    uiLanguage: UiLanguage
): String {
    return when (step) {
        StartExamPreflightStep.Idle -> localized(uiLanguage, "Waiting", "Menunggu")
        StartExamPreflightStep.Starting -> localized(uiLanguage, "Starting preflight", "Memulai preflight")
        StartExamPreflightStep.TamperAndIntegrity -> localized(uiLanguage, "Checking app integrity", "Cek integritas aplikasi")
        StartExamPreflightStep.DeviceSecurity -> localized(uiLanguage, "Checking device security", "Cek keamanan perangkat")
        StartExamPreflightStep.DeviceTime -> localized(uiLanguage, "Checking device time", "Cek waktu perangkat")
        StartExamPreflightStep.NetworkDns -> localized(uiLanguage, "Checking network and DNS", "Cek jaringan dan DNS")
        StartExamPreflightStep.ServerProbe -> localized(uiLanguage, "Checking exam server", "Cek server ujian")
        StartExamPreflightStep.HealthSnapshot -> localized(uiLanguage, "Building health snapshot", "Menyusun health snapshot")
        StartExamPreflightStep.StaticSecurity -> localized(uiLanguage, "Checking runtime security", "Cek keamanan runtime")
        StartExamPreflightStep.LocationPermission -> localized(uiLanguage, "Waiting for location permission", "Menunggu izin lokasi")
        StartExamPreflightStep.LocationValidation -> localized(uiLanguage, "Validating location", "Validasi lokasi")
        StartExamPreflightStep.PreparingWebView -> localized(uiLanguage, "Preparing exam browser", "Menyiapkan browser ujian")
        StartExamPreflightStep.Complete -> localized(uiLanguage, "Opening exam", "Membuka ujian")
        StartExamPreflightStep.Failed -> localized(uiLanguage, "Showing issue details", "Menampilkan detail kendala")
    }
}

internal fun startExamPreflightStepDetail(
    step: StartExamPreflightStep,
    uiLanguage: UiLanguage
): String {
    return when (step) {
        StartExamPreflightStep.Idle -> localized(uiLanguage, "Start Exam has not been pressed yet.", "Mulai Ujian belum ditekan.")
        StartExamPreflightStep.Starting -> localized(uiLanguage, "Preparing checks before the exam opens.", "Menyiapkan pemeriksaan sebelum ujian dibuka.")
        StartExamPreflightStep.TamperAndIntegrity -> localized(uiLanguage, "Checking whether the app package is still trusted.", "Memastikan paket aplikasi masih terpercaya.")
        StartExamPreflightStep.DeviceSecurity -> localized(uiLanguage, "Checking screen pinning, keyboard, Bluetooth, and device integrity.", "Memeriksa screen pinning, keyboard, Bluetooth, dan integritas perangkat.")
        StartExamPreflightStep.DeviceTime -> localized(uiLanguage, "Comparing device time with trusted network time.", "Mencocokkan waktu perangkat dengan waktu jaringan tepercaya.")
        StartExamPreflightStep.NetworkDns -> localized(uiLanguage, "Checking global DNS and the exam host DNS.", "Cek DNS global dan DNS host ujian.")
        StartExamPreflightStep.ServerProbe -> localized(uiLanguage, "Checking whether the exam server can be reached.", "Memastikan server ujian bisa dijangkau.")
        StartExamPreflightStep.HealthSnapshot -> localized(uiLanguage, "Reviewing the latest readiness snapshot.", "Meninjau snapshot kesiapan terbaru.")
        StartExamPreflightStep.StaticSecurity -> localized(uiLanguage, "Checking accessibility, ADB, root, recorder, display, and multi-window signals.", "Memeriksa Accessibility, ADB, root, recorder, display, dan multi-window.")
        StartExamPreflightStep.LocationPermission -> localized(uiLanguage, "Allow the Android permission prompt to continue.", "Izinkan prompt Android agar proses bisa lanjut.")
        StartExamPreflightStep.LocationValidation -> localized(uiLanguage, "Checking geofence and fake-location signals.", "Memeriksa geofence dan sinyal fake-location.")
        StartExamPreflightStep.PreparingWebView -> localized(uiLanguage, "Clearing the previous exam browser session.", "Membersihkan sesi browser ujian sebelumnya.")
        StartExamPreflightStep.Complete -> localized(uiLanguage, "The exam page is being opened.", "Halaman ujian sedang dibuka.")
        StartExamPreflightStep.Failed -> localized(uiLanguage, "Read the issue dialog for the next action.", "Baca dialog kendala untuk tindakan berikutnya.")
    }
}

internal class ExamRuntimeFlowUiState(
    val examSessionStarted: MutableState<Boolean>,
    val lockTaskRequestPending: MutableState<Boolean>,
    val startExamPreflight: StartExamPreflightUiState,
    val pinningActivationPurpose: MutableState<PinningActivationPurpose>,
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
    val startExamPreflightVisible = remember { mutableStateOf(false) }
    val startExamPreflightStep = remember { mutableStateOf(StartExamPreflightStep.Idle) }
    val startExamPreflightDetail = remember { mutableStateOf<String?>(null) }
    val startExamPreflightStartedAtElapsedMs = remember { mutableStateOf<Long?>(null) }
    val startExamPreflightSlowHintVisible = remember { mutableStateOf(false) }
    val startExamPreflight = remember {
        StartExamPreflightUiState(
            visible = startExamPreflightVisible,
            step = startExamPreflightStep,
            detail = startExamPreflightDetail,
            startedAtElapsedMs = startExamPreflightStartedAtElapsedMs,
            slowHintVisible = startExamPreflightSlowHintVisible
        )
    }
    val pinningActivationPurpose = rememberSaveable {
        mutableStateOf(PinningActivationPurpose.ExamStart)
    }
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
            startExamPreflight = startExamPreflight,
            pinningActivationPurpose = pinningActivationPurpose,
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
