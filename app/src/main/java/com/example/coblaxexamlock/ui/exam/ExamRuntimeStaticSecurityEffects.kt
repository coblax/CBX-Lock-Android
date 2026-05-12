package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.runtime.ExternalDisplayInfo
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.readMultiWindowModeInfo
import kotlinx.coroutines.delay

private const val RuntimeStaticSecurityPollIntervalMillis = 2_000L

internal data class RuntimeStaticSecurityUiMessage(
    val key: String,
    val title: String,
    val message: String
)

internal data class RuntimeStaticSecuritySnapshot(
    val screenRecorderPackages: List<String>,
    val externalDisplayCount: Int,
    val externalDisplayInfoList: List<ExternalDisplayInfo>,
    val multiWindowModeInfo: MultiWindowModeInfo
) {
    val externalDisplayDetected: Boolean
        get() = externalDisplayCount > 0
    val multiWindowDetected: Boolean
        get() = multiWindowModeInfo.inAnySplitMode
}

internal fun readRuntimeStaticSecuritySnapshot(
    context: Context,
    forceRefresh: Boolean = false
): RuntimeStaticSecuritySnapshot {
    val displaySnapshot = SecurityDetectorCache.readExternalDisplaySnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    return RuntimeStaticSecuritySnapshot(
        screenRecorderPackages = SecurityDetectorCache.readScreenRecorderPackages(
            context = context,
            forceRefresh = forceRefresh
        ),
        externalDisplayCount = displaySnapshot.count,
        externalDisplayInfoList = displaySnapshot.infoList,
        multiWindowModeInfo = readMultiWindowModeInfo(context)
    )
}

@Composable
internal fun RuntimeStaticSecurityEffects(
    context: Context,
    mainActivity: MainActivity?,
    examSessionStarted: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit
) {
    fun refreshRuntimeStaticSecurity(trigger: String) {
        refreshRuntimeStaticSecurityForSession(
            context = context,
            examSessionStarted = examSessionStarted,
            bypassScreenRecorder = bypassScreenRecorder,
            bypassDisplayMirror = bypassDisplayMirror,
            bypassMultiWindow = bypassMultiWindow,
            securityUiState = securityUiState,
            trigger = trigger,
            recordAction = recordAction,
            startAlarm = startAlarm,
            forceRefresh = false
        )
    }

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

internal fun refreshRuntimeStaticSecurityForSession(
    context: Context,
    examSessionStarted: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    trigger: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit,
    forceRefresh: Boolean = false
) {
    refreshRuntimeStaticSecurityState(
        context = context,
        examSessionStarted = examSessionStarted,
        bypassScreenRecorder = bypassScreenRecorder,
        bypassDisplayMirror = bypassDisplayMirror,
        bypassMultiWindow = bypassMultiWindow,
        securityUiState = securityUiState,
        trigger = trigger,
        recordAction = recordAction,
        startAlarm = startAlarm,
        forceRefresh = forceRefresh
    )
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
    startAlarm: () -> Unit,
    forceRefresh: Boolean = false
) {
    val previousScreenRecorderDetected = securityUiState.screenRecorderPackages.value.isNotEmpty()
    val previousDisplayMirrorDetected = securityUiState.externalDisplayDetected.value
    val previousMultiWindowDetected = securityUiState.multiWindowDetected.value

    val latestSnapshot = readRuntimeStaticSecuritySnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    val latestScreenRecorderPackages = latestSnapshot.screenRecorderPackages
    val latestExternalDisplayCount = latestSnapshot.externalDisplayCount
    val latestExternalDisplayDetected = latestSnapshot.externalDisplayDetected
    val latestMultiWindowDetected = latestSnapshot.multiWindowDetected

    securityUiState.screenRecorderPackages.setIfChanged(latestScreenRecorderPackages)
    securityUiState.externalDisplayCount.setIfChanged(latestExternalDisplayCount)
    securityUiState.externalDisplayInfoList.setIfChanged(latestSnapshot.externalDisplayInfoList)
    securityUiState.externalDisplayDetected.setIfChanged(latestExternalDisplayDetected)
    securityUiState.multiWindowModeInfo.setIfChanged(latestSnapshot.multiWindowModeInfo)
    securityUiState.multiWindowDetected.setIfChanged(latestMultiWindowDetected)

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
            if (!previousScreenRecorderDetected) {
                startAlarm()
            }
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
            if (!previousDisplayMirrorDetected) {
                startAlarm()
            }
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
            if (!previousMultiWindowDetected) {
                startAlarm()
            }
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
