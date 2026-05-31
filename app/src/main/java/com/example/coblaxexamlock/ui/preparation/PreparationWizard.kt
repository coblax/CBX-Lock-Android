package com.example.coblaxexamlock.ui.preparation

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockBlueSoft
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WizardGreen = Color(0xFF2F8F63)
private val WizardRed = Color(0xFFB34A4A)

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
            val accessibilityInspection = remember(
                context,
                accessibilityServiceEnabled
            ) {
                inspectAccessibility(context)
            }
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
            val accessibilityGuardEnabled = remember(context, accessibilityInspection.rawEnabledServices) {
                isExamGuardAccessibilityEnabled(context)
            }
            val accessibilityGuardAvailable = remember(context) {
                isExamGuardAccessibilityAvailable(context)
            }
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
                    runQuickFix = ::runQuickFix
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
            val stepChecklistText = remember(
                wizardPayloadBuildMode, currentStep,
                state.session, state.network, state.device, state.location,
                state.runtimeSecurity, state.bypass, state.diagnostics,
                uiLanguage, accessibilityInspection,
                accessibilityGuardEnabled, accessibilityGuardAvailable,
                accessibilityGuardRequired, needsBluetoothPermission
            ) {
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
            val stepPayload = remember(
                currentStep,
                readiness,
                sectionHealthMap,
                stepChecklistText,
                stepQuickFixActions,
                wizardPayloadBuildMode
            ) {
                createPreparationWizardStepPayload(
                    currentStep = currentStep,
                    readiness = readiness,
                    sectionHealthMap = sectionHealthMap,
                    sectionText = stepChecklistText,
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
                stepPayload.readiness.hasBypassIndicators -> LockGold
                else -> WizardGreen
            }
            val startButtonContentColor =
                if (stepPayload.readiness.hasBypassIndicators && stepPayload.readiness.canStartExam) {
                    LockBlueDeep
                } else {
                    Color.White
                }

            val completedCount = wizardStepStates.count { it.isCompleted }
            val totalSteps = steps.size
            val overallProgress = if (totalSteps > 0) completedCount.toFloat() / totalSteps else 1f

            val listState = rememberLazyListState()

            // Scroll to top when step changes
            LaunchedEffect(currentStepIndex) {
                if (lowRamProfile.disableNonEssentialAnimations) {
                    listState.scrollToItem(0)
                } else {
                    listState.animateScrollToItem(0)
                }
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(LockBackground)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 14.dp,
                        end = 16.dp,
                        bottom = 138.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Wizard Header
                    item(key = "wizard_header") {
                        WizardHeader(
                            examTitle = examName.ifBlank { tr("Exam Session", "Sesi Ujian") },
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

                    // Current Step Content (the actual section)
                    item(key = "wizard_step_content_${currentStep.sectionKey}") {
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

                    // Per-step Quick Fix Actions
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
                                accentColor = LockGold,
                                backgroundColor = Color(0xFFFFF8E6)
                            )
                        }
                    }

                    // On final step and all checks passed → Celebration Banner
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

// ──────────────────────────────────────────────────────────────
// Wizard Header
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardHeader(
    examTitle: String,
    completedCount: Int,
    totalSteps: Int,
    overallProgress: Float,
    canStartExam: Boolean,
    onBackHome: () -> Unit,
    onSwitchToChecklist: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.60f), shape)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        0f to LockBlue.copy(alpha = 0.07f),
                        0.6f to LockBlueSoft.copy(alpha = 0.03f),
                        1f to Color.Transparent
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LockSurfaceSoft)
                            .border(1.dp, LockOutline.copy(alpha = 0.60f), CircleShape)
                            .clickable(onClick = onBackHome)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = tr("Back to home", "Kembali ke menu utama"),
                            tint = LockBlueDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Wizard badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(LockBlueDeep, LockBlue)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🧭 " + tr("WIZARD", "WIZARD"),
                            color = LockOnDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Toggle to checklist
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(LockSurfaceSoft)
                            .border(1.dp, LockOutline.copy(alpha = 0.60f), RoundedCornerShape(999.dp))
                            .clickable(onClick = onSwitchToChecklist)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tr("Technical Details", "Detail Teknis"),
                            color = LockBlueDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = examTitle,
                    color = LockBlueDeep,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tr(
                        "Step-by-step guided preparation.",
                        "Persiapan terpandu langkah demi langkah."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                val progressColor = if (canStartExam) WizardGreen
                    else if (overallProgress > 0.7f) LockGoldDark
                    else WizardRed
                val reduceMotion = LocalLowRamProfile.current.disableNonEssentialAnimations
                val displayedProgress = if (reduceMotion) {
                    overallProgress
                } else {
                    animateFloatAsState(
                        targetValue = overallProgress,
                        animationSpec = tween(400),
                        label = "wizard_progress"
                    ).value
                }
                LinearProgressIndicator(
                    progress = { displayedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.12f)
                )
                Text(
                    text = tr(
                        "$completedCount/$totalSteps steps completed",
                        "$completedCount/$totalSteps langkah selesai"
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Step Indicator (numbered circles)
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardStepIndicator(
    steps: List<WizardStep>,
    stepStates: List<WizardStepState>,
    currentStepIndex: Int,
    onStepClick: (Int) -> Unit
) {
    val reduceMotion = LocalLowRamProfile.current.disableNonEssentialAnimations
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val stepState = stepStates.getOrNull(index)
            val isCompleted = stepState?.isCompleted ?: false
            val isCurrent = index == currentStepIndex

            val targetBgColor = when {
                isCurrent && isCompleted -> WizardGreen
                isCurrent -> LockBlue
                isCompleted -> WizardGreen.copy(alpha = 0.15f)
                else -> LockSurfaceSoft
            }
            val bgColor = if (reduceMotion) {
                targetBgColor
            } else {
                animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(300),
                    label = "step_bg_$index"
                ).value
            }
            val targetTextColor = when {
                isCurrent -> Color.White
                isCompleted -> WizardGreen
                else -> LockTextMuted
            }
            val textColor = if (reduceMotion) {
                targetTextColor
            } else {
                animateColorAsState(
                    targetValue = targetTextColor,
                    animationSpec = tween(300),
                    label = "step_text_$index"
                ).value
            }
            val borderColor = when {
                isCurrent -> Color.Transparent
                isCompleted -> WizardGreen.copy(alpha = 0.25f)
                else -> LockOutline.copy(alpha = 0.40f)
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, borderColor, CircleShape)
                    .clickable { onStepClick(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted && !isCurrent) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Step Title Card
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardStepTitleCard(
    stepIndex: Int,
    step: WizardStep,
    stepState: WizardStepState?
) {
    val uiLanguage = LocalUiLanguage.current
    val isCompleted = stepState?.isCompleted ?: false
    val issueCount = stepState?.issueCount ?: 0
    val accentColor = when {
        isCompleted -> WizardGreen
        issueCount > 0 -> WizardRed
        else -> LockBlue
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.08f),
                        accentColor.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step number badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.iconEmoji,
                    fontSize = 18.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Step ${stepIndex + 1}", "Langkah ${stepIndex + 1}"),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = step.title(uiLanguage),
                    color = LockTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = step.description(uiLanguage),
                    color = LockTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isCompleted) Color(0xFFE8F6EE)
                        else if (issueCount > 0) Color(0xFFFFEAEA)
                        else LockSurfaceSoft
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isCompleted) {
                        "✅"
                    } else if (issueCount > 0) {
                        "⚠ $issueCount"
                    } else {
                        "—"
                    },
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Step Section Content (renders the actual checklist section)
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardStepSectionContent(
    step: WizardStep,
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    text: PreparationChecklistText,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: com.example.coblaxexamlock.AccessibilityInspectionResult,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean
) {
    when (step) {
        WizardStep.DeviceSetup -> PreparationDeviceSetupSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            needsBluetoothPermission = needsBluetoothPermission,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Connectivity -> PreparationConnectivitySection(
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceHealth -> PreparationDeviceHealthSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.RuntimeInteraction -> PreparationRuntimeInteractionSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityInspection = accessibilityInspection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceIntegrity -> PreparationDeviceIntegritySection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Clipboard -> PreparationRuntimeClipboardSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Location -> PreparationLocationSection(
            location = state.location,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceLock -> PreparationDeviceLockSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.RuntimeSecurity -> PreparationRuntimeStaticSecuritySection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Per-Step Quick Fix Card
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardManualFixHintCard() {
    PreparationNoticeCard(
        title = tr("Manual Fix Needed", "Perbaikan Manual Dibutuhkan"),
        message = tr(
            "No automatic button is available for this section yet. Open Technical Details or press Refresh after the manual fix.",
            "Belum ada tombol otomatis untuk bagian ini. Buka Detail Teknis atau tekan Refresh setelah perbaikan manual."
        ),
        accentColor = LockGoldDark,
        backgroundColor = Color(0xFFFFF8E6)
    )
}

@Composable
private fun WizardStepQuickFixCard(
    actions: List<PreparationQuickFixAction>,
    hasGlobalBlockingIssues: Boolean
) {
    val lowRamProfile = LocalLowRamProfile.current
    val displayActions = remember(actions, lowRamProfile.enabled, lowRamProfile.ultra, hasGlobalBlockingIssues) {
        selectPreparationQuickFixActionsForDisplay(
            actions = actions,
            lowRamProfile = lowRamProfile,
            hasGlobalBlockingIssues = hasGlobalBlockingIssues
        )
    }
    val visibleActions = remember(displayActions) {
        buildList {
            displayActions.primary
                ?.takeIf { !it.isNotice }
                ?.let(::add)
            addAll(displayActions.blocking)
            addAll(displayActions.warnings)
            displayActions.refresh?.let(::add)
        }.distinctBy { it.code }
    }
    val blockingActions = visibleActions.filter { it.severity == QuickFixSeverity.Blocking && !it.isNotice }
    val warningActions = visibleActions.filter { it.severity == QuickFixSeverity.Warning && !it.isNotice }
    val notices = displayActions.notices

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (blockingActions.isNotEmpty()) WizardRed.copy(alpha = 0.30f)
                else LockGoldDark.copy(alpha = 0.30f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = tr("Quick Fix", "Perbaikan Cepat"),
            color = LockTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        notices.forEach { notice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF8E6))
                    .border(1.dp, LockGoldDark.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = notice.text,
                    color = LockGoldDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val stepNumbers = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩")
        var stepIndex = 0

        blockingActions.forEach { action ->
            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
            stepIndex++
            PreparationAssistButton(
                text = action.displayTextForProfile(lowRamProfile),
                labelPrefix = stepLabel,
                compact = true,
                filled = action.filled,
                loading = action.loading,
                enabled = action.enabled,
                onClick = action.onClick
            )
            if (!action.reason.isNullOrBlank()) {
                Text(
                    text = action.reason,
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        warningActions.forEach { action ->
            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
            stepIndex++
            PreparationAssistButton(
                text = action.displayTextForProfile(lowRamProfile),
                labelPrefix = stepLabel,
                compact = true,
                filled = false,
                loading = action.loading,
                enabled = action.enabled,
                onClick = action.onClick
            )
            if (!action.reason.isNullOrBlank()) {
                Text(
                    text = action.reason,
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Bottom Navigation Bar
// ──────────────────────────────────────────────────────────────

@Composable
private fun WizardBottomBar(
    currentStepIndex: Int,
    totalSteps: Int,
    currentStepCompleted: Boolean,
    canStartExam: Boolean,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    startButtonColor: Color,
    startButtonContentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLastStep = currentStepIndex == totalSteps - 1
    val isFirstStep = currentStepIndex == 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LockBlueDeep)
            .border(1.dp, LockBlueDeep.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            Button(
                onClick = if (isFirstStep) onBackHome else onPrevious,
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.28f)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isFirstStep) Icons.Rounded.Home
                        else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFirstStep) "Menu" else tr("Back", "Kembali"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Middle button (Next or Start Exam on last step)
            if (isLastStep) {
                val startEnabled = canStartExam && !(isStartingExam || webViewSessionResetInFlight)
                Button(
                    onClick = onStartExam,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = startEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = startButtonColor,
                        contentColor = startButtonContentColor,
                        disabledContainerColor = startButtonColor.copy(alpha = 0.85f),
                        disabledContentColor = startButtonContentColor
                    )
                ) {
                    Text(
                        text = if (webViewSessionResetInFlight) {
                            tr("PREPARING...", "MENYIAPKAN...")
                        } else if (isStartingExam) {
                            tr("STARTING...", "MEMULAI...")
                        } else {
                            tr("START EXAM", "MULAI UJIAN")
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStepCompleted) WizardGreen else LockBlue,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentStepCompleted) {
                                tr("NEXT STEP", "LANGKAH BERIKUT")
                            } else {
                                tr("SKIP →", "LEWATI →")
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Step counter
            Button(
                onClick = if (isLastStep) onBackHome else onNext,
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.28f)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLastStep) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Menu",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${currentStepIndex + 1}/$totalSteps",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Blocking reason text
        if (isLastStep && !canStartExam) {
            Text(
                text = "⚠ " + tr(
                    "Fix all blocking issues to start the exam",
                    "Perbaiki semua masalah blocking untuk memulai ujian"
                ),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }
}
