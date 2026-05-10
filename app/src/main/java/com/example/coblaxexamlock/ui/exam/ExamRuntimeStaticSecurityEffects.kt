package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.runtime.detectScreenRecorderPackages
import com.example.coblaxexamlock.runtime.getExternalDisplayCount
import com.example.coblaxexamlock.runtime.isInAnySplitMode
import kotlinx.coroutines.delay

private const val RuntimeStaticSecurityPollIntervalMillis = 2_000L

internal data class RuntimeStaticSecurityUiMessage(
    val key: String,
    val title: String,
    val message: String
)

@Composable
internal fun RuntimeStaticSecurityEffects(
    mainActivity: MainActivity?,
    examSessionStarted: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    refreshRuntimeStaticSecurity: (String) -> Unit
) {
    LaunchedEffect(
        examSessionStarted,
        bypassScreenRecorder,
        bypassDisplayMirror,
        bypassMultiWindow
    ) {
        refreshRuntimeStaticSecurity("static_security_effect_start")
        if (!examSessionStarted) {
            return@LaunchedEffect
        }
        while (true) {
            delay(RuntimeStaticSecurityPollIntervalMillis)
            refreshRuntimeStaticSecurity("runtime_static_security_poll")
        }
    }

    DisposableEffect(mainActivity, examSessionStarted, bypassMultiWindow) {
        val hostActivity = mainActivity
        if (hostActivity == null || !examSessionStarted) {
            hostActivity?.setOnExamMultiWindowModeChangedHandler(null)
            onDispose { hostActivity?.setOnExamMultiWindowModeChangedHandler(null) }
        } else {
            hostActivity.setOnExamMultiWindowModeChangedHandler {
                refreshRuntimeStaticSecurity("multi_window_mode_changed")
            }
            onDispose {
                hostActivity.setOnExamMultiWindowModeChangedHandler(null)
            }
        }
    }
}

internal fun refreshRuntimeStaticSecurityState(
    context: Context,
    examSessionStarted: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    trigger: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit
) {
    val previousScreenRecorderDetected = securityUiState.screenRecorderPackages.value.isNotEmpty()
    val previousDisplayMirrorDetected = securityUiState.externalDisplayDetected.value
    val previousMultiWindowDetected = securityUiState.multiWindowDetected.value

    val latestScreenRecorderPackages = detectScreenRecorderPackages(context)
    val latestExternalDisplayCount = getExternalDisplayCount(context)
    val latestExternalDisplayDetected = latestExternalDisplayCount > 0
    val latestMultiWindowDetected = isInAnySplitMode(context)

    securityUiState.screenRecorderPackages.value = latestScreenRecorderPackages
    securityUiState.externalDisplayCount.intValue = latestExternalDisplayCount
    securityUiState.externalDisplayDetected.value = latestExternalDisplayDetected
    securityUiState.multiWindowDetected.value = latestMultiWindowDetected

    fun boolLabel(value: Boolean): String = if (value) "yes" else "no"
    fun screenRecorderDetails(): String =
        "trigger=$trigger | count=${latestScreenRecorderPackages.size} | " +
            "packages=${latestScreenRecorderPackages.joinToString().ifBlank { "-" }} | " +
            "bypass=${boolLabel(bypassScreenRecorder)}"
    fun displayMirrorDetails(): String =
        "trigger=$trigger | external_display_count=$latestExternalDisplayCount | " +
            "bypass=${boolLabel(bypassDisplayMirror)}"
    fun multiWindowDetails(): String =
        "trigger=$trigger | detected=${boolLabel(latestMultiWindowDetected)} | " +
            "bypass=${boolLabel(bypassMultiWindow)}"

    if (trigger == "multi_window_mode_changed" && previousMultiWindowDetected != latestMultiWindowDetected) {
        recordAction(
            ExamRuntimeHardeningDiagnostics.MultiWindowModeChanged,
            multiWindowDetails(),
            if (latestMultiWindowDetected && !bypassMultiWindow) {
                DiagnosticEventLevel.SECURITY
            } else {
                DiagnosticEventLevel.INFO
            }
        )
    }

    if (!examSessionStarted) {
        securityUiState.showScreenRecorderViolationDialog.value = false
        securityUiState.showDisplayMirrorViolationDialog.value = false
        securityUiState.showMultiWindowViolationDialog.value = false
        return
    }

    val screenRecorderDetected = latestScreenRecorderPackages.isNotEmpty()
    when {
        screenRecorderDetected && !bypassScreenRecorder -> {
            if (!securityUiState.showScreenRecorderViolationDialog.value) {
                securityUiState.screenRecorderViolationCount.intValue += 1
                recordAction(
                    ExamRuntimeHardeningDiagnostics.ScreenRecorderDetected,
                    screenRecorderDetails(),
                    DiagnosticEventLevel.SECURITY
                )
            }
            securityUiState.showScreenRecorderViolationDialog.value = true
            startAlarm()
        }
        previousScreenRecorderDetected && !screenRecorderDetected -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.ScreenRecorderCleared,
                screenRecorderDetails(),
                DiagnosticEventLevel.INFO
            )
            securityUiState.showScreenRecorderViolationDialog.value = false
        }
        else -> securityUiState.showScreenRecorderViolationDialog.value = false
    }

    when {
        latestExternalDisplayDetected && !bypassDisplayMirror -> {
            if (!securityUiState.showDisplayMirrorViolationDialog.value) {
                securityUiState.displayMirrorViolationCount.intValue += 1
                recordAction(
                    ExamRuntimeHardeningDiagnostics.DisplayMirrorDetected,
                    displayMirrorDetails(),
                    DiagnosticEventLevel.SECURITY
                )
            }
            securityUiState.showDisplayMirrorViolationDialog.value = true
            startAlarm()
        }
        previousDisplayMirrorDetected && !latestExternalDisplayDetected -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.DisplayMirrorCleared,
                displayMirrorDetails(),
                DiagnosticEventLevel.INFO
            )
            securityUiState.showDisplayMirrorViolationDialog.value = false
        }
        else -> securityUiState.showDisplayMirrorViolationDialog.value = false
    }

    when {
        latestMultiWindowDetected && !bypassMultiWindow -> {
            if (!securityUiState.showMultiWindowViolationDialog.value) {
                securityUiState.multiWindowViolationCount.intValue += 1
                recordAction(
                    ExamRuntimeHardeningDiagnostics.MultiWindowDetected,
                    multiWindowDetails(),
                    DiagnosticEventLevel.SECURITY
                )
            }
            securityUiState.showMultiWindowViolationDialog.value = true
            startAlarm()
        }
        previousMultiWindowDetected && !latestMultiWindowDetected -> {
            recordAction(
                ExamRuntimeHardeningDiagnostics.MultiWindowCleared,
                multiWindowDetails(),
                DiagnosticEventLevel.INFO
            )
            securityUiState.showMultiWindowViolationDialog.value = false
        }
        else -> securityUiState.showMultiWindowViolationDialog.value = false
    }
}

internal fun resolveRuntimeStaticSecurityUiMessage(
    securityUiState: ExamRuntimeSecurityUiState
): RuntimeStaticSecurityUiMessage? {
    return when {
        securityUiState.showScreenRecorderViolationDialog.value ->
            RuntimeStaticSecurityUiMessage(
                key = "screen_recorder",
                title = "Screen Recorder Terdeteksi",
                message = "Aplikasi screen recorder terdeteksi saat ujian berjalan. Hapus/nonaktifkan aplikasi tersebut, lalu refresh status keamanan."
            )
        securityUiState.showDisplayMirrorViolationDialog.value ->
            RuntimeStaticSecurityUiMessage(
                key = "display_mirror",
                title = "Display Mirror Terdeteksi",
                message = "Display eksternal atau casting aktif saat ujian berjalan. Putuskan koneksi display/cast, lalu refresh status keamanan."
            )
        securityUiState.showMultiWindowViolationDialog.value ->
            RuntimeStaticSecurityUiMessage(
                key = "multi_window",
                title = "Split-Screen Aktif",
                message = "Aplikasi ujian berada di mode split-screen atau picture-in-picture. Kembali ke mode satu aplikasi, lalu refresh status keamanan."
            )
        else -> null
    }
}
