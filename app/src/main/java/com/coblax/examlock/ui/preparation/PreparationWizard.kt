package com.coblax.examlock.ui.preparation

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.runtime.LowRamDispatchers
import com.coblax.examlock.runtime.requiresBluetoothExamPermission
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.currentUiMotionPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val WizardGreen: Color
    @Composable get() = AppColors.current.safeEmphasis

internal val WizardRed: Color
    @Composable get() = AppColors.current.issueText

@Composable
internal fun PreparationWizardScreen(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    onSwitchToChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(state) {
        with(actions) {
            val uiLanguage = LocalUiLanguage.current
            val context = LocalContext.current
            val lowRamProfile = LocalLowRamProfile.current
            val motionPolicy = currentUiMotionPolicy()
            var accessibilityUiState by remember(context) {
                mutableStateOf(initialPreparationAccessibilityState())
            }
            LaunchedEffect(context, accessibilityServiceEnabled) {
                accessibilityUiState = loadPreparationAccessibilityState(context)
            }
            val accessibilityInspection = accessibilityUiState.inspection
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
                    manualRefreshScope.launch {
                        delay(600L)
                        refreshPreparationStatus()
                    }
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

            val needsBluetoothPermission = requiresBluetoothExamPermission()
            val accessibilityGuardEnabled = accessibilityUiState.guardEnabled
            val accessibilityGuardAvailable = accessibilityUiState.guardAvailable
            val accessibilityGuardRequired =
                !screenPinningAvailable && !bypassScreenPinning && accessibilityGuardAvailable

            val readiness = remember(
                state.network, state.device, state.location,
                state.runtimeSecurity, state.bypass,
                needsBluetoothPermission,
                accessibilityGuardRequired, accessibilityGuardAvailable,
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
            val canStartExam = readiness.canStartExam
            val sectionHealthMap = remember(readiness) { buildSectionHealthMap(readiness) }
            val wizardStepStates = remember(sectionHealthMap) { buildWizardStepStates(sectionHealthMap) }
            val steps = WizardStep.entries

            var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
            var userSelectedWizardStep by rememberSaveable { mutableStateOf(false) }
            var wizardAutoFocusApplied by rememberSaveable { mutableStateOf(false) }
            val autoFocusedStepIndex = remember(
                currentStepIndex,
                wizardStepStates,
                userSelectedWizardStep,
                wizardAutoFocusApplied
            ) {
                resolveWizardStepIndexForAutoFocus(
                    currentStepIndex = currentStepIndex,
                    stepStates = wizardStepStates,
                    userSelectedWizardStep = userSelectedWizardStep,
                    autoFocusApplied = wizardAutoFocusApplied
                )
            }
            LaunchedEffect(autoFocusedStepIndex, userSelectedWizardStep, wizardAutoFocusApplied) {
                if (!userSelectedWizardStep && !wizardAutoFocusApplied) {
                    currentStepIndex = autoFocusedStepIndex
                    wizardAutoFocusApplied = true
                }
            }
            val currentStep = steps[currentStepIndex.coerceIn(steps.indices)]
            val currentStepState = wizardStepStates.getOrNull(currentStepIndex)

            // Quick fix actions for current step
            val allQuickFixActions = remember(
                state.network, state.device, state.location,
                state.runtimeSecurity, state.bypass, state.diagnostics,
                throttledActions.session, throttledActions.network,
                throttledActions.device, throttledActions.location,
                throttledActions.runtimeSecurity,
                uiLanguage, accessibilityGuardRequired, accessibilityGuardEnabled,
                readiness.geofenceReady, readiness.fakeLocationReady,
                needsBluetoothPermission, accessibilityInspection
            ) {
                buildPreparationQuickFixActions(
                    state = state,
                    actions = throttledActions,
                    uiLanguage = uiLanguage,
                    accessibilityGuardRequired = accessibilityGuardRequired,
                    accessibilityGuardEnabled = accessibilityGuardEnabled,
                    geofenceReady = readiness.geofenceReady,
                    fakeLocationReady = readiness.fakeLocationReady,
                    needsBluetoothPermission = needsBluetoothPermission,
                    accessibilityInspection = accessibilityInspection,
                    runQuickFix = { target, actionCode, opensExternalSettings, action ->
                        runQuickFix(target, actionCode, opensExternalSettings, action)
                    }
                )
            }
            val stepQuickFixActions = remember(currentStep, currentStepState, allQuickFixActions) {
                val filteredActions = filterQuickFixActionsForStep(currentStep, allQuickFixActions)
                val refreshAction = allQuickFixActions.firstOrNull {
                    it.code == QuickFixRefreshAllSecurityChecksCode
                }
                val stepHasIssue = (currentStepState?.issueCount ?: 0) > 0
                if ((!stepHasIssue && filteredActions.isEmpty()) || refreshAction == null) {
                    filteredActions
                } else {
                    filteredActions + refreshAction
                }
            }
            val wizardPayloadBuildMode = remember(lowRamProfile.enabled, state.showChecklistDetails) {
                resolvePreparationWizardPayloadBuildMode(
                    lowRamProfile = lowRamProfile,
                    showChecklistDetails = state.showChecklistDetails
                )
            }
            var stepChecklistText by remember { mutableStateOf<PreparationChecklistText?>(null) }
            LaunchedEffect(
                wizardPayloadBuildMode, currentStep,
                state.session, state.network, state.device, state.location,
                state.runtimeSecurity, state.bypass, state.diagnostics,
                uiLanguage, accessibilityInspection,
                accessibilityGuardEnabled, accessibilityGuardAvailable,
                accessibilityGuardRequired, needsBluetoothPermission
            ) {
                stepChecklistText = null
                stepChecklistText = withContext(LowRamDispatchers.detectorIo) {
                    if (wizardPayloadBuildMode == PreparationWizardPayloadBuildMode.FullChecklist) {
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
                        buildPreparationWizardStepText(
                            step = currentStep,
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
            val stepPayload = remember(
                currentStep,
                readiness,
                sectionHealthMap,
                stepChecklistText,
                stepQuickFixActions,
                wizardPayloadBuildMode
            ) {
                val visibleStepText = stepChecklistText ?: loadingPreparationChecklistText(uiLanguage)
                createPreparationWizardStepPayload(
                    currentStep = currentStep,
                    readiness = readiness,
                    sectionHealthMap = sectionHealthMap,
                    sectionText = visibleStepText,
                    quickFixActions = stepQuickFixActions,
                    buildMode = wizardPayloadBuildMode
                )
            }
            val hasGlobalBlockingQuickFix = remember(allQuickFixActions) {
                allQuickFixActions.any {
                    it.severity == QuickFixSeverity.Blocking &&
                        !it.isNotice &&
                        it.code != QuickFixRefreshAllSecurityChecksCode
                }
            }
            val stepActionCoverage = remember(currentStepState, stepPayload.quickFixActions) {
                resolveWizardStepActionCoverage(
                    stepState = currentStepState,
                    quickFixActions = stepPayload.quickFixActions
                )
            }

            val startButtonColor = when {
                !stepPayload.readiness.canStartExam -> WizardRed
                stepPayload.readiness.hasBypassIndicators -> AppColors.current.gold
                else -> WizardGreen
            }
            val startButtonContentColor =
                if (stepPayload.readiness.hasBypassIndicators && stepPayload.readiness.canStartExam) {
                    AppColors.current.blueDeep
                } else {
                    Color.White
                }

            val completedCount = wizardStepStates.count { it.isCompleted }
            val totalSteps = steps.size
            val overallProgress = if (totalSteps > 0) completedCount.toFloat() / totalSteps else 1f

            val listState = rememberLazyListState()

            // Scroll to top when step changes
            LaunchedEffect(currentStepIndex, motionPolicy.enabled) {
                if (!motionPolicy.enabled) {
                    listState.scrollToItem(0)
                } else {
                    listState.animateScrollToItem(0)
                }
            }

            val screenPadding = adaptiveScreenPadding()
            UpgradeUiScope(motionPolicy = motionPolicy) {
                // Docked bottom bar (see PreparationChecklistContent): a floating bar let
                // step cards slide underneath and show through the strip below it.
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(AppColors.current.background)
                ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = screenPadding,
                        top = 10.dp,
                        end = screenPadding,
                        bottom = 10.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wizard Header
                    item(key = "wizard_header") {
                        WizardHeader(
                            examTitle = examName.ifBlank { tr("Exam Session", "Sesi Ujian") },
                            currentStepIndex = currentStepIndex,
                            completedCount = completedCount,
                            totalSteps = totalSteps,
                            overallProgress = overallProgress,
                            canStartExam = canStartExam,
                            onBackHome = onBackHome,
                            onSwitchToChecklist = onSwitchToChecklist
                        )
                    }

                    // Step Indicator
                    item(key = "wizard_step_indicator") {
                        WizardStepIndicator(
                            steps = steps,
                            stepStates = wizardStepStates,
                            currentStepIndex = currentStepIndex,
                            onStepClick = { index ->
                                userSelectedWizardStep = true
                                currentStepIndex = index
                            }
                        )
                    }

                    // Current Step Title Card
                    item(key = "wizard_step_title") {
                        WizardStepTitleCard(
                            stepIndex = currentStepIndex,
                            step = currentStep,
                            stepState = currentStepState
                        )
                    }

                    // Keep the primary problem and its repair actions above technical details.
                    if (stepPayload.quickFixActions.isNotEmpty()) {
                        item(key = "wizard_step_quick_fix") {
                            WizardStepQuickFixCard(
                                actions = stepPayload.quickFixActions,
                                hasGlobalBlockingIssues = hasGlobalBlockingQuickFix
                            )
                        }
                    }
                    if (stepActionCoverage.showManualFixHint) {
                        item(key = "wizard_manual_fix_hint_${currentStep.sectionKey}") {
                            WizardManualFixHintCard()
                        }
                    }

                    quickFixFeedbackText?.let { feedbackText ->
                        item(key = "wizard_quick_fix_return_status") {
                            PreparationNoticeCard(
                                title = tr("Status", "Status"),
                                message = feedbackText,
                                accentColor = AppColors.current.gold,
                                backgroundColor = AppColors.current.warnBgSoft
                            )
                        }
                    }

                    item(key = "wizard_step_details_${currentStep.sectionKey}") {
                        WizardStepDetailsCard(
                            step = currentStep,
                            issueCount = currentStepState?.issueCount ?: 0
                        ) {
                            WizardStepSectionContent(
                                step = stepPayload.currentStep,
                                state = state,
                                actions = throttledActions,
                                text = stepPayload.sectionText,
                                needsBluetoothPermission = needsBluetoothPermission,
                                accessibilityInspection = accessibilityInspection,
                                accessibilityGuardAvailable = accessibilityGuardAvailable,
                                accessibilityGuardRequired = accessibilityGuardRequired,
                                accessibilityGuardEnabled = accessibilityGuardEnabled
                            )
                        }
                    }

                    // Static final readiness summary.
                    if (currentStepIndex == steps.lastIndex && canStartExam) {
                        item(key = "wizard_celebration") {
                            PreparationCelebrationBanner()
                        }
                    }
                }

                // Bottom Navigation Bar
                WizardBottomBar(
                    currentStepIndex = currentStepIndex,
                    totalSteps = totalSteps,
                    currentStepCompleted = currentStepState?.isCompleted ?: true,
                    canStartExam = canStartExam,
                    isStartingExam = isStartingExam,
                    webViewSessionResetInFlight = webViewSessionResetInFlight,
                    startButtonColor = startButtonColor,
                    startButtonContentColor = startButtonContentColor,
                    onPrevious = {
                        if (currentStepIndex > 0) {
                            userSelectedWizardStep = true
                            currentStepIndex--
                        }
                    },
                    onNext = {
                        if (currentStepIndex < steps.lastIndex) {
                            userSelectedWizardStep = true
                            currentStepIndex++
                        }
                    },
                    onRecheck = throttledActions.session.onRefreshAllSecurityChecks,
                    onStartExam = onStartExam,
                    onBackHome = onBackHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            }
            }
        }
    }
}
