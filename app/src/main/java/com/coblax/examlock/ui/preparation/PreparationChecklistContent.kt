package com.coblax.examlock.ui.preparation

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import com.coblax.examlock.BuildConfig
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.runtime.LowRamDispatchers
import com.coblax.examlock.runtime.requiresBluetoothExamPermission
import com.coblax.examlock.ui.exam.formatApkIntegrityBlockReason
import com.coblax.examlock.ui.exam.formatReverseEngineeringBlockReason
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.isExpandedLayout
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockDangerBgSoft
import com.coblax.examlock.ui.theme.LockIssueText
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.ui.theme.LockWarnBgSoft

import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

internal fun shouldBuildFullPreparationChecklistText(
    lowRamProfile: LowRamProfile,
    showFullChecklist: Boolean
): Boolean = showFullChecklist && !lowRamProfile.enabled

@Composable
internal fun ExamSecurityPreparationScreenContent(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    onSwitchToWizard: () -> Unit = {},
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
    val useLazyChecklistSectionText = lowRamProfile.enabled
    var accessibilityUiState by remember(context) {
        mutableStateOf(initialPreparationAccessibilityState())
    }
    LaunchedEffect(context, accessibilityServiceEnabled) {
        accessibilityUiState = loadPreparationAccessibilityState(context)
    }
    val accessibilityInspection = accessibilityUiState.inspection
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingQuickFixTarget by rememberSaveable { mutableStateOf<QuickFixTarget?>(null) }
    var pendingQuickFixCode by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingQuickFixOpenedExternalSettings by rememberSaveable { mutableStateOf(false) }
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
        val queuedFeedback = localized(
            uiLanguage,
            "Refresh queued; running shortly.",
            "Refresh dijadwalkan; segera berjalan."
        )
        quickFixFeedbackText = queuedFeedback
        manualRefreshScope.launch {
            delay((cooldownMillis - elapsedMs).coerceAtLeast(0L) + 500L)
            if (quickFixFeedbackText == queuedFeedback) {
                quickFixFeedbackText = null
            }
        }
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
            val target = pendingQuickFixTarget
            val fixCode = pendingQuickFixCode
            if (target == null && fixCode == null) {
                return@LifecycleEventObserver
            }
            pendingQuickFixTarget = null
            if (target != null) {
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
            // #12 Delayed re-check: some states (Bluetooth, ADB) need settling time
            manualRefreshScope.launch {
                delay(600L)
                refreshPreparationStatus()
            }
            // #7 Return indicator: show brief feedback about which fix was attempted
            if (fixCode != null) {
                val openedExternalSettings = pendingQuickFixOpenedExternalSettings
                pendingQuickFixCode = null
                pendingQuickFixOpenedExternalSettings = false
                quickFixFeedbackText = if (openedExternalSettings) {
                    localized(uiLanguage, "Status checked again.", "Status dicek ulang.")
                } else {
                    val fixLabel = fixCode.replace("_", " ").replaceFirstChar { it.uppercase() }
                    "\u21a9 $fixLabel"
                }
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
    fun runQuickFix(
        target: QuickFixTarget?,
        actionCode: String,
        opensExternalSettings: Boolean = false,
        action: () -> Unit
    ) {
        onAutoFixActionOpened(actionCode)
        if (target != null || opensExternalSettings) {
            pendingQuickFixTarget = target ?: QuickFixTarget.All
        }
        pendingQuickFixCode = actionCode
        pendingQuickFixOpenedExternalSettings = opensExternalSettings
        if (opensExternalSettings) {
            quickFixFeedbackText = localized(
                uiLanguage,
                "Return to the app; status will be checked again.",
                "Kembali ke aplikasi, status akan dicek ulang."
            )
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
    val accessibilityGuardEnabled = accessibilityUiState.guardEnabled
    val accessibilityGuardAvailable = accessibilityUiState.guardAvailable
    val accessibilityGuardRequired =
        !screenPinningAvailable && !bypassScreenPinning && accessibilityGuardAvailable
    var checklistText by remember { mutableStateOf<PreparationChecklistText?>(null) }
    LaunchedEffect(
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
        showFullChecklist,
        useLazyChecklistSectionText
    ) {
        checklistText = null
        checklistText = if (shouldBuildFullPreparationChecklistText(lowRamProfile, showFullChecklist)) {
            withContext(LowRamDispatchers.detectorIo) {
                debugMeasurePreparationWork("buildPreparationChecklistText") {
                    buildPreparationChecklistText(
                        state = state,
                        uiLanguage = uiLanguage,
                        accessibilityInspection = accessibilityInspection,
                        accessibilityGuardEnabled = accessibilityGuardEnabled,
                        accessibilityGuardAvailable = accessibilityGuardAvailable,
                        accessibilityGuardRequired = accessibilityGuardRequired,
                        needsBluetoothPermission = needsBluetoothPermission
                    )
                }
            }
        } else {
            null
        }
    }
    @Composable
    fun rememberChecklistTextForStep(step: WizardStep): PreparationChecklistText? {
        if (!showFullChecklist) {
            return null
        }
        checklistText?.let { return it }
        var stepChecklistText by remember { mutableStateOf<PreparationChecklistText?>(null) }
        LaunchedEffect(
            step,
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
            needsBluetoothPermission
        ) {
            stepChecklistText = null
            stepChecklistText = withContext(LowRamDispatchers.detectorIo) {
                debugMeasurePreparationWork("buildPreparationWizardStepText:${step.name}") {
                    buildPreparationWizardStepText(
                        step = step,
                        state = state,
                        uiLanguage = uiLanguage,
                        accessibilityInspection = accessibilityInspection,
                        accessibilityGuardEnabled = accessibilityGuardEnabled,
                        accessibilityGuardAvailable = accessibilityGuardAvailable,
                        accessibilityGuardRequired = accessibilityGuardRequired,
                        needsBluetoothPermission = needsBluetoothPermission
                    )
                }
            }
        }
        return stepChecklistText
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
        !canStartExam -> LockIssueText
        hasBypassIndicators -> LockGold
        else -> LockSafeEmphasis
    }
    val startButtonContentColor =
        if (hasBypassIndicators && canStartExam) LockBlueDeep else Color.White

    val screenPadding = adaptiveScreenPadding()
    val useExpandedGrid = isExpandedLayout()
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
                start = screenPadding,
                top = 14.dp,
                end = screenPadding,
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
                    onBackHome = onBackHome,
                    onSwitchToWizard = onSwitchToWizard
                )
            }
            quickFixFeedbackText?.let { feedbackText ->
                item(key = "quick_fix_return_status") {
                    PreparationNoticeCard(
                        title = tr("Status", "Status"),
                        message = feedbackText,
                        accentColor = LockGold,
                        backgroundColor = LockWarnBgSoft
                    )
                }
            }
            item(key = "checklist_intro") {
                PreparationChecklistIntroItem(
                    checklistTitle = checklistTitle,
                    checklistSubtitle = checklistSubtitle,
                    telegramHelperText = telegramHelperText
                )
            }
            if (showFullChecklist) {
                if (useExpandedGrid) {
                    // â”€â”€ Expanded (tablet): 2-column grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    item(key = "checklist_row_1") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_device_setup", sectionHealthMap["checklist_device_setup"]) {
                                    rememberChecklistTextForStep(WizardStep.DeviceSetup)?.let { visibleChecklistText ->
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
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_connectivity", sectionHealthMap["checklist_connectivity"]) {
                                    rememberChecklistTextForStep(WizardStep.Connectivity)?.let { visibleChecklistText ->
                                        PreparationConnectivitySection(
                                            text = visibleChecklistText,
                                            sendingSection = state.session.sendingSection,
                                            onRequestSectionReport = actions.session.onRequestSectionReport
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "checklist_row_2") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_device_health", sectionHealthMap["checklist_device_health"]) {
                                    rememberChecklistTextForStep(WizardStep.DeviceHealth)?.let { visibleChecklistText ->
                                        PreparationDeviceHealthSection(
                                            device = state.device,
                                            bypass = state.bypass,
                                            text = visibleChecklistText,
                                            sendingSection = state.session.sendingSection,
                                            onRequestSectionReport = actions.session.onRequestSectionReport
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_runtime_interaction", sectionHealthMap["checklist_runtime_interaction"]) {
                                    rememberChecklistTextForStep(WizardStep.RuntimeInteraction)?.let { visibleChecklistText ->
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
                            }
                        }
                    }
                    item(key = "checklist_row_3") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_device_integrity", sectionHealthMap["checklist_device_integrity"]) {
                                    rememberChecklistTextForStep(WizardStep.DeviceIntegrity)?.let { visibleChecklistText ->
                                        PreparationDeviceIntegritySection(
                                            device = state.device,
                                            bypass = state.bypass,
                                            text = visibleChecklistText,
                                            sendingSection = state.session.sendingSection,
                                            onRequestSectionReport = actions.session.onRequestSectionReport
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_runtime_clipboard", sectionHealthMap["checklist_runtime_clipboard"]) {
                                    rememberChecklistTextForStep(WizardStep.Clipboard)?.let { visibleChecklistText ->
                                        PreparationRuntimeClipboardSection(
                                            runtimeSecurity = state.runtimeSecurity,
                                            bypass = state.bypass,
                                            text = visibleChecklistText,
                                            sendingSection = state.session.sendingSection,
                                            onRequestSectionReport = actions.session.onRequestSectionReport
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "checklist_row_4") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_location", sectionHealthMap["checklist_location"]) {
                                    rememberChecklistTextForStep(WizardStep.Location)?.let { visibleChecklistText ->
                                        PreparationLocationSection(
                                            location = state.location,
                                            bypass = state.bypass,
                                            text = visibleChecklistText,
                                            sendingSection = state.session.sendingSection,
                                            onRequestSectionReport = actions.session.onRequestSectionReport
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CollapsibleChecklistSection("checklist_device_lock", sectionHealthMap["checklist_device_lock"]) {
                                    rememberChecklistTextForStep(WizardStep.DeviceLock)?.let { visibleChecklistText ->
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
                            }
                        }
                    }
                    // Odd section (9th): full width
                    item(key = "checklist_runtime_static_security") {
                        CollapsibleChecklistSection("checklist_runtime_static_security", sectionHealthMap["checklist_runtime_static_security"]) {
                            rememberChecklistTextForStep(WizardStep.RuntimeSecurity)?.let { visibleChecklistText ->
                                PreparationRuntimeStaticSecuritySection(
                                    runtimeSecurity = state.runtimeSecurity,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                } else {
                    // â”€â”€ Compact (phone): single column â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    item(key = "checklist_device_setup") {
                        CollapsibleChecklistSection("checklist_device_setup", sectionHealthMap["checklist_device_setup"]) {
                            rememberChecklistTextForStep(WizardStep.DeviceSetup)?.let { visibleChecklistText ->
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
                    }
                    item(key = "checklist_connectivity") {
                        CollapsibleChecklistSection("checklist_connectivity", sectionHealthMap["checklist_connectivity"]) {
                            rememberChecklistTextForStep(WizardStep.Connectivity)?.let { visibleChecklistText ->
                                PreparationConnectivitySection(
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                    item(key = "checklist_device_health") {
                        CollapsibleChecklistSection("checklist_device_health", sectionHealthMap["checklist_device_health"]) {
                            rememberChecklistTextForStep(WizardStep.DeviceHealth)?.let { visibleChecklistText ->
                                PreparationDeviceHealthSection(
                                    device = state.device,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                    item(key = "checklist_runtime_interaction") {
                        CollapsibleChecklistSection("checklist_runtime_interaction", sectionHealthMap["checklist_runtime_interaction"]) {
                            rememberChecklistTextForStep(WizardStep.RuntimeInteraction)?.let { visibleChecklistText ->
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
                    }
                    item(key = "checklist_device_integrity") {
                        CollapsibleChecklistSection("checklist_device_integrity", sectionHealthMap["checklist_device_integrity"]) {
                            rememberChecklistTextForStep(WizardStep.DeviceIntegrity)?.let { visibleChecklistText ->
                                PreparationDeviceIntegritySection(
                                    device = state.device,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                    item(key = "checklist_runtime_clipboard") {
                        CollapsibleChecklistSection("checklist_runtime_clipboard", sectionHealthMap["checklist_runtime_clipboard"]) {
                            rememberChecklistTextForStep(WizardStep.Clipboard)?.let { visibleChecklistText ->
                                PreparationRuntimeClipboardSection(
                                    runtimeSecurity = state.runtimeSecurity,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                    item(key = "checklist_location") {
                        CollapsibleChecklistSection("checklist_location", sectionHealthMap["checklist_location"]) {
                            rememberChecklistTextForStep(WizardStep.Location)?.let { visibleChecklistText ->
                                PreparationLocationSection(
                                    location = state.location,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                    item(key = "checklist_device_lock") {
                        CollapsibleChecklistSection("checklist_device_lock", sectionHealthMap["checklist_device_lock"]) {
                            rememberChecklistTextForStep(WizardStep.DeviceLock)?.let { visibleChecklistText ->
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
                    }
                    item(key = "checklist_runtime_static_security") {
                        CollapsibleChecklistSection("checklist_runtime_static_security", sectionHealthMap["checklist_runtime_static_security"]) {
                            rememberChecklistTextForStep(WizardStep.RuntimeSecurity)?.let { visibleChecklistText ->
                                PreparationRuntimeStaticSecuritySection(
                                    runtimeSecurity = state.runtimeSecurity,
                                    bypass = state.bypass,
                                    text = visibleChecklistText,
                                    sendingSection = state.session.sendingSection,
                                    onRequestSectionReport = actions.session.onRequestSectionReport
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "ultra_low_ram_checklist_collapsed") {
                    PreparationNoticeCard(
                        title = tr("Ultra Low-RAM Mode", "Mode Ultra Low-RAM"),
                        message = tr(
                            "Technical checklist details are hidden to keep this phone responsive. Open technical details only when a full audit is needed.",
                            "Detail checklist teknis disembunyikan agar HP tetap responsif. Buka Detail Teknis hanya saat perlu audit lengkap."
                        ),
                        accentColor = LockGold,
                        backgroundColor = LockWarnBgSoft
                    )
                }
            }

            if (state.runtimeSecurity.reverseEngineeringDetected) {
                item(key = "reverse_engineering_notice") {
                    val bypassActive = state.runtimeSecurity.reverseEngineeringBypassActive
                    PreparationNoticeCard(
                        title = if (bypassActive) {
                            tr(
                                "Reverse Engineering Check Bypassed",
                                "Cek Reverse Engineering Dilewati"
                            )
                        } else {
                            tr(
                                "Reverse Engineering Check Failed",
                                "Cek Reverse Engineering Gagal"
                            )
                        },
                        message = if (bypassActive) {
                            tr(
                                "Reason: ${formatReverseEngineeringBlockReason(state.runtimeSecurity.reverseEngineeringSummary)}\nDilewati oleh Secret Admin. Detection remains logged in diagnostics.",
                                "Alasan: ${formatReverseEngineeringBlockReason(state.runtimeSecurity.reverseEngineeringSummary)}\nDilewati oleh Secret Admin. Deteksi tetap dicatat di diagnostik."
                            )
                        } else {
                            tr(
                                "Reason: ${formatReverseEngineeringBlockReason(state.runtimeSecurity.reverseEngineeringSummary)}\nClose debugger, tracer, hooking/root tools, then reopen the app.",
                                "Alasan: ${formatReverseEngineeringBlockReason(state.runtimeSecurity.reverseEngineeringSummary)}\nTutup debugger, tracer, tool hooking/root, lalu buka ulang aplikasi."
                            )
                        },
                        accentColor = if (bypassActive) LockGold else LockIssueText,
                        backgroundColor = if (bypassActive) LockWarnBgSoft else LockDangerBgSoft
                    )
                }
            }

            if (state.runtimeSecurity.integrityDetected || state.device.signatureMismatchDetected) {
                item(key = "apk_integrity_notice") {
                    val bypassActive = state.runtimeSecurity.integrityBypassActive
                    val integritySummary = state.runtimeSecurity.integritySummary
                        .takeIf { it.isNotBlank() && it != "-" }
                        ?: if (state.device.signatureMismatchDetected) {
                            "signature_changed"
                        } else {
                            "-"
                        }
                    PreparationNoticeCard(
                        title = if (bypassActive) {
                            tr("APK Integrity Check Bypassed", "Cek Integritas APK Dilewati")
                        } else {
                            tr("APK Integrity Check Failed", "Cek Integritas APK Gagal")
                        },
                        message = if (bypassActive) {
                            tr(
                                "Reason: ${formatApkIntegrityBlockReason(integritySummary)}\nDilewati oleh Secret Admin. Detection remains logged in diagnostics.",
                                "Alasan: ${formatApkIntegrityBlockReason(integritySummary)}\nDilewati oleh Secret Admin. Deteksi tetap dicatat di diagnostik."
                            )
                        } else {
                            tr(
                                "Reason: ${formatApkIntegrityBlockReason(integritySummary)}\nReinstall the official APK, then reopen the app.",
                                "Alasan: ${formatApkIntegrityBlockReason(integritySummary)}\nInstal ulang APK resmi, lalu buka ulang aplikasi."
                            )
                        },
                        accentColor = if (bypassActive) LockGold else LockIssueText,
                        backgroundColor = if (bypassActive) LockWarnBgSoft else LockDangerBgSoft
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
