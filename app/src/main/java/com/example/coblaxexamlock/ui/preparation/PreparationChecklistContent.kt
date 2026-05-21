package com.example.coblaxexamlock.ui.preparation

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val lowRamProfile = LocalLowRamProfile.current
    val severeLowRamPreparation = lowRamProfile.severe
    val ultraLowRamPreparation = lowRamProfile.ultra
    val showFullChecklist = !ultraLowRamPreparation || showChecklistDetails
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
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingQuickFixTarget by rememberSaveable { mutableStateOf<QuickFixTarget?>(null) }
    var pendingQuickFixCode by rememberSaveable { mutableStateOf<String?>(null) }
    var quickFixFeedbackText by remember { mutableStateOf<String?>(null) }
    val refreshAllSecurityChecks by rememberUpdatedState(onRefreshAllSecurityChecks)
    val refreshPreparationStatus by rememberUpdatedState(onRefreshStatus)
    val refreshNetworkStatus by rememberUpdatedState(onRefreshNetworkStatus)
    val refreshLocationStatus by rememberUpdatedState(onRefreshGeofenceLocation)
    val manualRefreshScope = rememberCoroutineScope()
    var lastManualRefreshAt by remember { mutableLongStateOf(0L) }
    var lastManualRefreshKey by remember { mutableStateOf<String?>(null) }
    var pendingManualRefreshJob by remember { mutableStateOf<Job?>(null) }
    var pendingManualRefreshAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun runManualRefreshWithCooldown(key: String, action: () -> Unit) {
        val cooldownMillis = lowRamProfile.manualRefreshCooldownMillis
        if (cooldownMillis <= 0L) {
            action()
            return
        }
        val nowElapsedMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowElapsedMs - lastManualRefreshAt
        if (lastManualRefreshKey != key || elapsedMs >= cooldownMillis) {
            pendingManualRefreshJob?.cancel()
            pendingManualRefreshJob = null
            pendingManualRefreshAction = null
            lastManualRefreshKey = key
            lastManualRefreshAt = nowElapsedMs
            action()
            return
        }
        pendingManualRefreshAction = action
        if (pendingManualRefreshJob == null) {
            pendingManualRefreshJob = manualRefreshScope.launch {
                delay((cooldownMillis - elapsedMs).coerceAtLeast(0L))
                val queuedAction = pendingManualRefreshAction
                pendingManualRefreshAction = null
                pendingManualRefreshJob = null
                lastManualRefreshKey = key
                lastManualRefreshAt = SystemClock.elapsedRealtime()
                queuedAction?.invoke()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pendingManualRefreshJob?.cancel()
        }
    }
    val throttledActions = actions.copy(
        session = actions.session.copy(
            onRefreshStatus = {
                runManualRefreshWithCooldown("status", refreshPreparationStatus)
            },
            onRefreshAllSecurityChecks = {
                runManualRefreshWithCooldown("all", refreshAllSecurityChecks)
            }
        ),
        network = actions.network.copy(
            onRefreshNetworkStatus = {
                runManualRefreshWithCooldown("network", refreshNetworkStatus)
            }
        ),
        location = actions.location.copy(
            onRefreshGeofenceLocation = {
                runManualRefreshWithCooldown("location", refreshLocationStatus)
            }
        )
    )
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
            // #12 Delayed re-check: some states (Bluetooth, ADB) need settling time
            manualRefreshScope.launch {
                delay(600L)
                refreshPreparationStatus()
            }
            // #7 Return indicator: show brief feedback about which fix was attempted
            val fixCode = pendingQuickFixCode
            if (fixCode != null) {
                pendingQuickFixCode = null
                val fixLabel = fixCode.replace("_", " ").replaceFirstChar { it.uppercase() }
                quickFixFeedbackText = "\u21a9 $fixLabel"
                manualRefreshScope.launch {
                    delay(3000L)
                    quickFixFeedbackText = null
                }
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
        pendingQuickFixCode = actionCode
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
    val checklistText = remember(
        state.session,
        state.network,
        state.device,
        state.location,
        state.runtimeSecurity,
        state.bypass,
        state.diagnostics,
        uiLanguage,
        accessibilityInspection,
        accessibilityGuardEnabled,
        accessibilityGuardAvailable,
        accessibilityGuardRequired,
        needsBluetoothPermission,
        showFullChecklist
    ) {
        debugMeasurePreparationWork("buildPreparationChecklistText") {
            if (showFullChecklist) {
                buildPreparationChecklistText(
                    state = state,
                    uiLanguage = uiLanguage,
                    accessibilityInspection = accessibilityInspection,
                    accessibilityGuardEnabled = accessibilityGuardEnabled,
                    accessibilityGuardAvailable = accessibilityGuardAvailable,
                    accessibilityGuardRequired = accessibilityGuardRequired,
                    needsBluetoothPermission = needsBluetoothPermission
                )
            } else {
                null
            }
        }
    }
    val readiness = remember(
        state.network,
        state.device,
        state.location,
        state.runtimeSecurity,
        state.bypass,
        needsBluetoothPermission,
        accessibilityGuardRequired,
        accessibilityGuardAvailable,
        accessibilityGuardEnabled
    ) {
        buildPreparationChecklistReadiness(
            network = state.network,
            device = state.device,
            location = state.location,
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            needsBluetoothPermission = needsBluetoothPermission,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardEnabled = accessibilityGuardEnabled
        )
    }
    val geofenceReady = readiness.geofenceReady
    val fakeLocationReady = readiness.fakeLocationReady
    val canStartExam = readiness.canStartExam
    val hasBypassIndicators = readiness.hasBypassIndicators
    val blockingReasonEN = resolveFirstBlockingReason(readiness, en = true)
    val blockingReasonID = resolveFirstBlockingReason(readiness, en = false)
    val firstBlockingReason = tr(blockingReasonEN ?: "", blockingReasonID ?: "")
        .takeIf { it.isNotBlank() }
    val readinessSummary = remember(readiness) {
        buildPreparationReadinessSummary(readiness, blockingReasonEN, blockingReasonID)
    }
    val sectionHealthMap = remember(readiness) {
        buildSectionHealthMap(readiness)
    }
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 14.dp,
                end = 16.dp,
                bottom = 118.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "preparation_header") {
                PreparationChecklistHeader(
                    examTitle = examTitle,
                    severeLowRamPreparation = severeLowRamPreparation,
                    blockingCount = readinessSummary.blockingCount,
                    warningCount = readinessSummary.warningCount,
                    safeCount = readinessSummary.safeCount,
                    canStartExam = canStartExam,
                    firstBlockingReason = firstBlockingReason,
                    onBackHome = onBackHome
                )
            }
            item(key = "checklist_intro") {
                PreparationChecklistIntroItem(
                    checklistTitle = checklistTitle,
                    checklistSubtitle = checklistSubtitle,
                    telegramHelperText = telegramHelperText
                )
            }
            val visibleChecklistText = checklistText
            if (visibleChecklistText != null) {
                item(key = "checklist_device_setup") {
                    CollapsibleChecklistSection("checklist_device_setup", sectionHealthMap["checklist_device_setup"]) {
                        PreparationDeviceSetupSection(
                            device = state.device,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            needsBluetoothPermission = needsBluetoothPermission,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_connectivity") {
                    CollapsibleChecklistSection("checklist_connectivity", sectionHealthMap["checklist_connectivity"]) {
                        PreparationConnectivitySection(
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_device_health") {
                    CollapsibleChecklistSection("checklist_device_health", sectionHealthMap["checklist_device_health"]) {
                        PreparationDeviceHealthSection(
                            device = state.device,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_runtime_interaction") {
                    CollapsibleChecklistSection("checklist_runtime_interaction", sectionHealthMap["checklist_runtime_interaction"]) {
                        PreparationRuntimeInteractionSection(
                            runtimeSecurity = state.runtimeSecurity,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            accessibilityInspection = accessibilityInspection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_device_integrity") {
                    CollapsibleChecklistSection("checklist_device_integrity", sectionHealthMap["checklist_device_integrity"]) {
                        PreparationDeviceIntegritySection(
                            device = state.device,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_runtime_clipboard") {
                    CollapsibleChecklistSection("checklist_runtime_clipboard", sectionHealthMap["checklist_runtime_clipboard"]) {
                        PreparationRuntimeClipboardSection(
                            runtimeSecurity = state.runtimeSecurity,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_location") {
                    CollapsibleChecklistSection("checklist_location", sectionHealthMap["checklist_location"]) {
                        PreparationLocationSection(
                            location = state.location,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_device_lock") {
                    CollapsibleChecklistSection("checklist_device_lock", sectionHealthMap["checklist_device_lock"]) {
                        PreparationDeviceLockSection(
                            device = state.device,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            accessibilityGuardAvailable = accessibilityGuardAvailable,
                            accessibilityGuardRequired = accessibilityGuardRequired,
                            accessibilityGuardEnabled = accessibilityGuardEnabled,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
                item(key = "checklist_runtime_static_security") {
                    CollapsibleChecklistSection("checklist_runtime_static_security", sectionHealthMap["checklist_runtime_static_security"]) {
                        PreparationRuntimeStaticSecuritySection(
                            runtimeSecurity = state.runtimeSecurity,
                            bypass = state.bypass,
                            text = visibleChecklistText,
                            sendingSection = state.session.sendingSection,
                            onRequestSectionReport = actions.session.onRequestSectionReport
                        )
                    }
                }
            } else {
                item(key = "ultra_low_ram_checklist_collapsed") {
                    PreparationNoticeCard(
                        title = tr("Ultra Low-RAM Mode", "Mode Ultra Low-RAM"),
                        message = tr(
                            "Technical checklist details are hidden to keep this phone responsive. Open Detail Teknis from admin settings if a full audit is needed.",
                            "Detail checklist teknis disembunyikan agar HP tetap responsif. Buka Detail Teknis dari pengaturan admin jika perlu audit lengkap."
                        ),
                        accentColor = LockGold,
                        backgroundColor = Color(0xFFFFF8E6)
                    )
                }
            }

            if (tamperDetected) {
                item(key = "tamper_notice") {
                    PreparationNoticeCard(
                        title = tr("Security Check Failed", "Pemeriksaan Keamanan Gagal"),
                        message = tr(
                            "Security checks failed. Close debugging or hooking tools and reopen the app.",
                            "Pemeriksaan keamanan gagal. Tutup tool debugging/hooking lalu buka ulang aplikasi."
                        ),
                        accentColor = Color(0xFFB34A4A),
                        backgroundColor = Color(0xFFFFEFEF)
                    )
                }
            }

            item(key = "notice_stack") {
                PreparationNoticeStack(
                    state = state,
                    actions = throttledActions,
                    runQuickFix = ::runQuickFix
                )
            }

            item(key = "quick_fix_panel") {
                PreparationQuickFixPanel(
                    state = state,
                    actions = throttledActions,
                    accessibilityGuardRequired = accessibilityGuardRequired,
                    accessibilityGuardEnabled = accessibilityGuardEnabled,
                    geofenceReady = geofenceReady,
                    fakeLocationReady = fakeLocationReady,
                    needsBluetoothPermission = needsBluetoothPermission,
                    accessibilityInspection = accessibilityInspection,
                    runQuickFix = ::runQuickFix
                )
            }

            if (canStartExam) {
                item(key = "celebration_banner") {
                    PreparationCelebrationBanner()
                }
            }

            item(key = "preparation_bottom_spacer") {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        PreparationFloatingActionBar(
            startButtonColor = startButtonColor,
            startButtonContentColor = startButtonContentColor,
            canStartExam = canStartExam,
            isStartingExam = isStartingExam,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            blockingReason = firstBlockingReason,
            onRefreshStatus = throttledActions.onRefreshStatus,
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

