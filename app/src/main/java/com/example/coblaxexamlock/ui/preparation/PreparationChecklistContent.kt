package com.example.coblaxexamlock.ui.preparation

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold

private const val PreparationPerfTag = "PreparationPerf"

private inline fun <T> debugMeasurePreparationWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                PreparationPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

@Composable
internal fun ExamSecurityPreparationScreenContent(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    with(state) {
        with(actions) {
    val uiLanguage = LocalUiLanguage.current
    val context = LocalContext.current
    val severeLowRamPreparation = LocalLowRamProfile.current.severe
    val accessibilityInspection = remember(
        context,
        accessibilityServiceEnabled,
        showChecklistDetails,
        bypassAccessibility
    ) {
        debugMeasurePreparationWork("inspectAccessibility") {
            inspectAccessibility(context)
        }
    }
    val scrollState = rememberScrollState()
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingQuickFixTarget by rememberSaveable { mutableStateOf<QuickFixTarget?>(null) }
    val refreshAllSecurityChecks by rememberUpdatedState(onRefreshAllSecurityChecks)
    val refreshPreparationStatus by rememberUpdatedState(onRefreshStatus)
    val refreshNetworkStatus by rememberUpdatedState(onRefreshNetworkStatus)
    val refreshLocationStatus by rememberUpdatedState(onRefreshGeofenceLocation)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) {
                return@LifecycleEventObserver
            }
            val target = pendingQuickFixTarget ?: return@LifecycleEventObserver
            pendingQuickFixTarget = null
            when (target) {
                QuickFixTarget.Network -> refreshNetworkStatus()
                QuickFixTarget.Location -> refreshLocationStatus()
                QuickFixTarget.DeviceTime,
                QuickFixTarget.ScreenPinning,
                QuickFixTarget.WebView,
                QuickFixTarget.Battery,
                QuickFixTarget.ScreenRecorder,
                QuickFixTarget.DisplayMirror,
                QuickFixTarget.MultiWindow -> refreshPreparationStatus()
                QuickFixTarget.All -> refreshAllSecurityChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    fun runQuickFix(target: QuickFixTarget?, actionCode: String, action: () -> Unit) {
        onAutoFixActionOpened(actionCode)
        if (target != null) {
            pendingQuickFixTarget = target
        }
        action()
    }
    val autoFixSuggestions = remember(preExamHealthCheckSnapshot, deviceSurvivalPolicy) {
        buildPreparationAutoFixSuggestions(
            snapshot = preExamHealthCheckSnapshot,
            survivalPolicy = deviceSurvivalPolicy
        )
    }
    LaunchedEffect(autoFixSuggestions.size, deviceSurvivalPolicy.score) {
        if (autoFixSuggestions.isNotEmpty()) {
            onAutoFixShown(
                "score=${deviceSurvivalPolicy.score.name} | suggestions=${autoFixSuggestions.size} | " +
                    "blocking=${autoFixSuggestions.count { it.severity == PreparationAutoFixSeverity.Blocking }}"
            )
        }
    }
    val previousSessionRecoveryHint = previousExamSessionBreadcrumb.latestRecoveryHint
    LaunchedEffect(previousSessionRecoveryHint) {
        if (!previousSessionRecoveryHint.isNullOrBlank()) {
            onPreviousSessionRecoveryHintShown(
                "hint=${previousSessionRecoveryHint.take(120)} | trail=${previousExamSessionBreadcrumb.diagnosticSummary()}"
            )
        }
    }
    val checklistTitle = tr("Automatic Checklist", "Checklist Otomatis")
    val checklistSubtitle = tr("Quick checks before the exam starts.", "Pemeriksaan singkat sebelum mulai.")
    val examTitle = examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val telegramHelperText = tr(
        "Tap the Telegram icon on each checklist item to send diagnostics for that section.",
        "Ketuk ikon Telegram di setiap item checklist untuk kirim diagnostik bagian tersebut."
    )
    val needsBluetoothPermission = requiresBluetoothExamPermission()
    val accessibilityGuardEnabled = remember(context, accessibilityInspection.rawEnabledServices) {
        isExamGuardAccessibilityEnabled(context)
    }
    val accessibilityGuardAvailable = remember(context) {
        isExamGuardAccessibilityAvailable(context)
    }
    val accessibilityGuardRequired =
        !screenPinningAvailable && !bypassScreenPinning && accessibilityGuardAvailable
    val checklistText = buildPreparationChecklistText(
        state = state,
        uiLanguage = uiLanguage,
        accessibilityInspection = accessibilityInspection,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityGuardRequired = accessibilityGuardRequired,
        needsBluetoothPermission = needsBluetoothPermission
    )
    val readiness = buildPreparationChecklistReadiness(
        state = state,
        needsBluetoothPermission = needsBluetoothPermission,
        accessibilityGuardRequired = accessibilityGuardRequired,
        accessibilityGuardAvailable = accessibilityGuardAvailable,
        accessibilityGuardEnabled = accessibilityGuardEnabled
    )
    val geofenceReady = readiness.geofenceReady
    val fakeLocationReady = readiness.fakeLocationReady
    val canStartExam = readiness.canStartExam
    val hasBypassIndicators = readiness.hasBypassIndicators
    val startButtonColor = when {
        !canStartExam -> Color(0xFFB34A4A)
        hasBypassIndicators -> LockGold
        else -> Color(0xFF2F8F63)
    }
    val startButtonContentColor =
        if (hasBypassIndicators && canStartExam) LockBlueDeep else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 118.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        PreparationChecklistHeader(
            examTitle = examTitle,
            severeLowRamPreparation = severeLowRamPreparation,
            onBackHome = onBackHome
        )
        Spacer(modifier = Modifier.height(14.dp))

        PreparationChecklistItemsCard(
            state = state,
            actions = actions,
            text = checklistText,
            needsBluetoothPermission = needsBluetoothPermission,
            accessibilityInspection = accessibilityInspection,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            checklistTitle = checklistTitle,
            checklistSubtitle = checklistSubtitle,
            telegramHelperText = telegramHelperText
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (tamperDetected) {
            PreparationNoticeCard(
                title = tr("Security Check Failed", "Pemeriksaan Keamanan Gagal"),
                message = tr(
                    "Security checks failed. Close debugging or hooking tools and reopen the app.",
                    "Pemeriksaan keamanan gagal. Tutup tool debugging/hooking lalu buka ulang aplikasi."
                ),
                accentColor = Color(0xFFB34A4A),
                backgroundColor = Color(0xFFFFEFEF)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        PreparationNoticeStack(
            state = state,
            actions = actions,
            runQuickFix = ::runQuickFix
        )

        PreparationQuickFixPanel(
            state = state,
            actions = actions,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            geofenceReady = geofenceReady,
            fakeLocationReady = fakeLocationReady,
            needsBluetoothPermission = needsBluetoothPermission,
            runQuickFix = ::runQuickFix
        )

            Spacer(modifier = Modifier.height(6.dp))
        }

        PreparationFloatingActionBar(
            startButtonColor = startButtonColor,
            startButtonContentColor = startButtonContentColor,
            canStartExam = canStartExam,
            isStartingExam = isStartingExam,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            onRefreshStatus = onRefreshStatus,
            onStartExam = onStartExam,
            onBackHome = onBackHome,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
        }
    }
}

