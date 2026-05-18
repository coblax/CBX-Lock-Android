package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.LocationFixQualityStatus
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.runtime.ExternalDisplayInfo
import com.example.coblaxexamlock.runtime.ExternalDisplaySnapshot
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.readMultiWindowModeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val RuntimeFastStaticSecurityPollIntervalMillis = 2_000L
private const val RuntimeScreenRecorderPollIntervalMillis = 15_000L
private const val RuntimeUltraScreenRecorderPollIntervalMillis = 45_000L

internal fun runtimeFastStaticSecurityPollIntervalMillis(lowRamProfile: LowRamProfile): Long =
    RuntimeFastStaticSecurityPollIntervalMillis * lowRamProfile.slowPollingMultiplier

internal fun runtimeScreenRecorderPollIntervalMillis(lowRamProfile: LowRamProfile): Long =
    if (lowRamProfile.ultra) {
        RuntimeUltraScreenRecorderPollIntervalMillis
    } else {
        RuntimeScreenRecorderPollIntervalMillis
    }

internal data class RuntimeStaticSecurityUiMessage(
    val key: String,
    val title: String,
    val message: String
)

internal data class InitialStaticSecuritySnapshot(
    val rootSecurityStatus: RootSecurityStatus,
    val screenRecorderPackages: List<String>,
    val externalDisplaySnapshot: ExternalDisplaySnapshot,
    val suspiciousFakeLocationPackages: List<String>
)

internal data class RuntimeFastStaticSecuritySnapshot(
    val externalDisplayCount: Int,
    val externalDisplayInfoList: List<ExternalDisplayInfo>,
    val multiWindowModeInfo: MultiWindowModeInfo
) {
    val externalDisplayDetected: Boolean
        get() = externalDisplayCount > 0
    val multiWindowDetected: Boolean
        get() = multiWindowModeInfo.inAnySplitMode
}

internal data class RuntimeScreenRecorderSnapshot(
    val screenRecorderPackages: List<String>
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

internal suspend fun readInitialStaticSecuritySnapshotOnIo(
    context: Context,
    forceRefresh: Boolean = false
): InitialStaticSecuritySnapshot = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    InitialStaticSecuritySnapshot(
        rootSecurityStatus = buildRootSecurityStatus(
            SecurityDetectorCache.readRootDetectionDetails(
                context = appContext,
                forceRefresh = forceRefresh
            )
        ),
        screenRecorderPackages = SecurityDetectorCache.readScreenRecorderPackages(
            context = appContext,
            forceRefresh = forceRefresh
        ),
        externalDisplaySnapshot = SecurityDetectorCache.readExternalDisplaySnapshot(
            context = appContext,
            forceRefresh = forceRefresh
        ),
        suspiciousFakeLocationPackages = SecurityDetectorCache.readSuspiciousFakeLocationPackages(
            context = appContext,
            forceRefresh = forceRefresh
        )
    )
}

internal fun applyInitialStaticSecuritySnapshot(
    snapshot: InitialStaticSecuritySnapshot,
    securityUiState: ExamRuntimeSecurityUiState,
    permissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    fixQualityStatus: LocationFixQualityStatus,
    developerOptionsEnabled: Boolean,
    fakeLocationBypassState: FakeLocationBypassState
) {
    securityUiState.rootSecurityStatus.setIfChanged(snapshot.rootSecurityStatus)
    securityUiState.rootDetected.setIfChanged(snapshot.rootSecurityStatus.detected)
    securityUiState.selinuxPermissiveWarning.setIfChanged(snapshot.rootSecurityStatus.selinuxPermissive)
    securityUiState.screenRecorderPackages.setIfChanged(snapshot.screenRecorderPackages)
    securityUiState.externalDisplayCount.setIfChanged(snapshot.externalDisplaySnapshot.count)
    securityUiState.externalDisplayInfoList.setIfChanged(snapshot.externalDisplaySnapshot.infoList)
    securityUiState.externalDisplayDetected.setIfChanged(snapshot.externalDisplaySnapshot.detected)
    securityUiState.fakeLocationSecurityStatus.setIfChanged(
        evaluateFakeLocationSecurity(
            monitoringEnabled = true,
            permissionGranted = permissionGranted,
            locationServicesEnabled = locationServicesEnabled,
            locationSnapshot = fixQualityStatus.snapshot,
            fixQualityStatus = fixQualityStatus,
            developerOptionsEnabled = developerOptionsEnabled,
            suspiciousFakeLocationPackages = snapshot.suspiciousFakeLocationPackages,
            bypassState = fakeLocationBypassState
        )
    )
    securityUiState.staticSecurityInitialScanComplete.setIfChanged(true)
}

internal fun readRuntimeFastStaticSecuritySnapshot(
    displaySnapshotReader: () -> ExternalDisplaySnapshot,
    multiWindowInfoReader: () -> MultiWindowModeInfo
): RuntimeFastStaticSecuritySnapshot {
    val displaySnapshot = displaySnapshotReader()
    return RuntimeFastStaticSecuritySnapshot(
        externalDisplayCount = displaySnapshot.count,
        externalDisplayInfoList = displaySnapshot.infoList,
        multiWindowModeInfo = multiWindowInfoReader()
    )
}

internal fun readRuntimeFastStaticSecuritySnapshot(
    context: Context,
    forceRefresh: Boolean = false
): RuntimeFastStaticSecuritySnapshot {
    return readRuntimeFastStaticSecuritySnapshot(
        displaySnapshotReader = {
            SecurityDetectorCache.readExternalDisplaySnapshot(
                context = context,
                forceRefresh = forceRefresh
            )
        },
        multiWindowInfoReader = { readMultiWindowModeInfo(context) }
    )
}

internal fun readRuntimeScreenRecorderSnapshot(
    screenRecorderPackagesReader: () -> List<String>
): RuntimeScreenRecorderSnapshot {
    return RuntimeScreenRecorderSnapshot(
        screenRecorderPackages = screenRecorderPackagesReader()
    )
}

internal fun readRuntimeScreenRecorderSnapshot(
    context: Context,
    forceRefresh: Boolean = false
): RuntimeScreenRecorderSnapshot {
    return readRuntimeScreenRecorderSnapshot(
        screenRecorderPackagesReader = {
            SecurityDetectorCache.readScreenRecorderPackages(
                context = context,
                forceRefresh = forceRefresh
            )
        }
    )
}

internal fun readRuntimeStaticSecuritySnapshot(
    context: Context,
    forceRefresh: Boolean = false
): RuntimeStaticSecuritySnapshot {
    val fastSnapshot = readRuntimeFastStaticSecuritySnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    val screenRecorderSnapshot = readRuntimeScreenRecorderSnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    return RuntimeStaticSecuritySnapshot(
        screenRecorderPackages = screenRecorderSnapshot.screenRecorderPackages,
        externalDisplayCount = fastSnapshot.externalDisplayCount,
        externalDisplayInfoList = fastSnapshot.externalDisplayInfoList,
        multiWindowModeInfo = fastSnapshot.multiWindowModeInfo
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
    packageInventoryChangeNonce: Int,
    securityUiState: ExamRuntimeSecurityUiState,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit
) {
    val initialScanComplete = securityUiState.staticSecurityInitialScanComplete.value
    val lowRamProfile = LocalLowRamProfile.current

    fun refreshRuntimeFastStaticSecurity(trigger: String, forceRefresh: Boolean = false) {
        refreshRuntimeFastStaticSecurityState(
            context = context,
            examSessionStarted = examSessionStarted,
            bypassDisplayMirror = bypassDisplayMirror,
            bypassMultiWindow = bypassMultiWindow,
            securityUiState = securityUiState,
            trigger = trigger,
            recordAction = recordAction,
            startAlarm = startAlarm,
            forceRefresh = forceRefresh
        )
    }

    fun refreshRuntimeScreenRecorder(trigger: String, forceRefresh: Boolean = false) {
        refreshRuntimeScreenRecorderState(
            context = context,
            examSessionStarted = examSessionStarted,
            bypassScreenRecorder = bypassScreenRecorder,
            securityUiState = securityUiState,
            trigger = trigger,
            recordAction = recordAction,
            startAlarm = startAlarm,
            forceRefresh = forceRefresh
        )
    }

    LaunchedEffect(
        examSessionStarted,
        bypassDisplayMirror,
        bypassMultiWindow,
        initialScanComplete,
        lowRamProfile
    ) {
        if (!initialScanComplete) {
            return@LaunchedEffect
        }
        refreshRuntimeFastStaticSecurity("static_security_fast_effect_start")
        if (!examSessionStarted) {
            return@LaunchedEffect
        }
        while (true) {
            delay(runtimeFastStaticSecurityPollIntervalMillis(lowRamProfile))
            refreshRuntimeFastStaticSecurity("runtime_static_security_fast_poll")
        }
    }

    LaunchedEffect(
        examSessionStarted,
        bypassScreenRecorder,
        initialScanComplete,
        lowRamProfile
    ) {
        if (!initialScanComplete) {
            return@LaunchedEffect
        }
        refreshRuntimeScreenRecorder("screen_recorder_effect_start")
        if (!examSessionStarted) {
            return@LaunchedEffect
        }
        while (true) {
            delay(runtimeScreenRecorderPollIntervalMillis(lowRamProfile))
            refreshRuntimeScreenRecorder("runtime_screen_recorder_poll")
        }
    }

    LaunchedEffect(packageInventoryChangeNonce, bypassScreenRecorder, initialScanComplete) {
        if (packageInventoryChangeNonce > 0 && initialScanComplete) {
            refreshRuntimeScreenRecorder(
                trigger = "package_inventory_changed",
                forceRefresh = true
            )
        }
    }

    DisposableEffect(mainActivity, examSessionStarted, bypassMultiWindow) {
        val hostActivity = mainActivity
        if (hostActivity == null || !examSessionStarted) {
            hostActivity?.setOnExamMultiWindowModeChangedHandler(null)
            onDispose { hostActivity?.setOnExamMultiWindowModeChangedHandler(null) }
        } else {
            hostActivity.setOnExamMultiWindowModeChangedHandler {
                if (securityUiState.staticSecurityInitialScanComplete.value) {
                    refreshRuntimeFastStaticSecurity("multi_window_mode_changed")
                }
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
    refreshRuntimeFastStaticSecurityState(
        context = context,
        examSessionStarted = examSessionStarted,
        bypassDisplayMirror = bypassDisplayMirror,
        bypassMultiWindow = bypassMultiWindow,
        securityUiState = securityUiState,
        trigger = trigger,
        recordAction = recordAction,
        startAlarm = startAlarm,
        forceRefresh = forceRefresh
    )
    refreshRuntimeScreenRecorderState(
        context = context,
        examSessionStarted = examSessionStarted,
        bypassScreenRecorder = bypassScreenRecorder,
        securityUiState = securityUiState,
        trigger = trigger,
        recordAction = recordAction,
        startAlarm = startAlarm,
        forceRefresh = forceRefresh
    )
}

internal fun refreshRuntimeFastStaticSecurityState(
    context: Context,
    examSessionStarted: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    trigger: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit,
    forceRefresh: Boolean = false
) {
    val previousDisplayMirrorDetected = securityUiState.externalDisplayDetected.value
    val previousMultiWindowDetected = securityUiState.multiWindowDetected.value

    val latestSnapshot = readRuntimeFastStaticSecuritySnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    val latestExternalDisplayCount = latestSnapshot.externalDisplayCount
    val latestExternalDisplayDetected = latestSnapshot.externalDisplayDetected
    val latestMultiWindowDetected = latestSnapshot.multiWindowDetected

    securityUiState.externalDisplayCount.setIfChanged(latestExternalDisplayCount)
    securityUiState.externalDisplayInfoList.setIfChanged(latestSnapshot.externalDisplayInfoList)
    securityUiState.externalDisplayDetected.setIfChanged(latestExternalDisplayDetected)
    securityUiState.multiWindowModeInfo.setIfChanged(latestSnapshot.multiWindowModeInfo)
    securityUiState.multiWindowDetected.setIfChanged(latestMultiWindowDetected)

    fun boolLabel(value: Boolean): String = if (value) "yes" else "no"
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
        securityUiState.showDisplayMirrorViolationDialog.value = false
        securityUiState.showMultiWindowViolationDialog.value = false
        return
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

internal fun refreshRuntimeScreenRecorderState(
    context: Context,
    examSessionStarted: Boolean,
    bypassScreenRecorder: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    trigger: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    startAlarm: () -> Unit,
    forceRefresh: Boolean = false
) {
    val previousScreenRecorderDetected = securityUiState.screenRecorderPackages.value.isNotEmpty()
    val latestSnapshot = readRuntimeScreenRecorderSnapshot(
        context = context,
        forceRefresh = forceRefresh
    )
    val latestScreenRecorderPackages = latestSnapshot.screenRecorderPackages

    securityUiState.screenRecorderPackages.setIfChanged(latestScreenRecorderPackages)

    fun boolLabel(value: Boolean): String = if (value) "yes" else "no"
    fun screenRecorderDetails(): String =
        "trigger=$trigger | count=${latestScreenRecorderPackages.size} | " +
            "packages=${latestScreenRecorderPackages.joinToString().ifBlank { "-" }} | " +
            "bypass=${boolLabel(bypassScreenRecorder)}"

    if (!examSessionStarted) {
        securityUiState.showScreenRecorderViolationDialog.value = false
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
