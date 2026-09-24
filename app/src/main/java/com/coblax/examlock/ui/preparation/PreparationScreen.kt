package com.coblax.examlock.ui.preparation

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.PinningActivationState
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.LowRamDispatchers
import com.coblax.examlock.runtime.requiresBluetoothExamPermission
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBanner
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.currentUiMotionPolicy
import com.coblax.examlock.ui.theme.primaryActionColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The single pre-exam screen: overall status, what must be fixed (with the fix button
 * right under each problem), and a grid of every check category whose technical rows
 * open in a sheet on demand. Start Exam stays docked at the bottom.
 */
@Composable
internal fun ExamSecurityPreparationScreen(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    modifier: Modifier = Modifier
) {
    val uiLanguage = LocalUiLanguage.current
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val motionPolicy = currentUiMotionPolicy()
    val interaction = rememberPreparationInteraction(
        actions = actions,
        cooldownMillis = lowRamProfile.manualRefreshCooldownMillis,
        uiLanguage = uiLanguage
    )
    val screenActions = interaction.actions

    var accessibilityUiState by remember(context) {
        mutableStateOf(initialPreparationAccessibilityState())
    }
    // Re-inspect on every resume too: enabling Exam Guard in Settings does not always
    // flip accessibilityServiceEnabled, and a stale "Exam Guard is off" would trap the student.
    LaunchedEffect(context, state.accessibilityServiceEnabled, interaction.resumeCount) {
        accessibilityUiState = loadPreparationAccessibilityState(context)
    }
    val accessibilityInspection = accessibilityUiState.inspection
    val accessibilityGuardEnabled = accessibilityUiState.guardEnabled
    val accessibilityGuardAvailable = accessibilityUiState.guardAvailable
    val accessibilityGuardRequired =
        !state.screenPinningAvailable && !state.bypassScreenPinning && accessibilityGuardAvailable
    val needsBluetoothPermission = requiresBluetoothExamPermission()
    // The QR was valid when scanned, but the exam window can close while the student is
    // still here; Start Exam would then refuse, so say it first. Ticks only with an end.
    val examEndsAtMillis = state.session.examEndsAtMillis
    val examScheduleEnded by produceState(
        initialValue = examEndsAtMillis != null && System.currentTimeMillis() > examEndsAtMillis,
        key1 = examEndsAtMillis
    ) {
        val endsAt = examEndsAtMillis ?: return@produceState
        while (System.currentTimeMillis() <= endsAt) {
            delay(ExamScheduleTickMillis)
        }
        value = true
    }

    val autoFixSuggestions = remember(state.preExamHealthCheckSnapshot, state.deviceSurvivalPolicy) {
        buildPreparationAutoFixSuggestions(
            snapshot = state.preExamHealthCheckSnapshot,
            survivalPolicy = state.deviceSurvivalPolicy
        )
    }
    LaunchedEffect(autoFixSuggestions.size, state.deviceSurvivalPolicy.score) {
        if (autoFixSuggestions.isNotEmpty()) {
            screenActions.onAutoFixShown(
                "score=${state.deviceSurvivalPolicy.score.name} | suggestions=${autoFixSuggestions.size} | " +
                    "blocking=${autoFixSuggestions.count { it.severity == PreparationAutoFixSeverity.Blocking }}"
            )
        }
    }
    val previousSessionRecoveryHint = state.previousExamSessionBreadcrumb.latestRecoveryHint
    LaunchedEffect(previousSessionRecoveryHint) {
        if (!previousSessionRecoveryHint.isNullOrBlank()) {
            screenActions.onPreviousSessionRecoveryHintShown(
                "hint=${previousSessionRecoveryHint.take(120)} | " +
                    "trail=${state.previousExamSessionBreadcrumb.diagnosticSummary()}"
            )
        }
    }

    val readiness = remember(
        state.network, state.device, state.location,
        state.runtimeSecurity, state.bypass, state.preExamHealthCheckSnapshot,
        examScheduleEnded,
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
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            preExamHealthSnapshot = state.preExamHealthCheckSnapshot,
            examScheduleEnded = examScheduleEnded
        )
    }
    val quickFixActions = remember(
        state, screenActions, uiLanguage,
        accessibilityGuardRequired, accessibilityGuardEnabled,
        readiness.geofenceReady, readiness.fakeLocationReady,
        needsBluetoothPermission, accessibilityInspection
    ) {
        buildPreparationQuickFixActions(
            state = state,
            actions = screenActions,
            uiLanguage = uiLanguage,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            geofenceReady = readiness.geofenceReady,
            fakeLocationReady = readiness.fakeLocationReady,
            needsBluetoothPermission = needsBluetoothPermission,
            accessibilityInspection = accessibilityInspection,
            runQuickFix = interaction.runQuickFix
        )
    }
    val pinningDeferredDetails = quickFixActions
        .firstOrNull { it.code == QuickFixScreenPinningDeferredCode }
        ?.diagnosticDetails
    LaunchedEffect(pinningDeferredDetails) {
        pinningDeferredDetails?.let(screenActions.onScreenPinningDeferred)
    }
    val hasGlobalBlockingIssues = remember(quickFixActions) {
        quickFixActions.any {
            it.severity == QuickFixSeverity.Blocking &&
                !it.isNotice &&
                it.code != QuickFixRefreshAllSecurityChecksCode
        }
    }
    val overview = remember(
        state, readiness, quickFixActions, needsBluetoothPermission,
        accessibilityGuardAvailable, accessibilityUiState.loaded, uiLanguage
    ) {
        buildPreparationOverview(
            state = state,
            readiness = readiness,
            quickFixActions = quickFixActions,
            needsBluetoothPermission = needsBluetoothPermission,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            uiLanguage = uiLanguage,
            accessibilityStateLoaded = accessibilityUiState.loaded
        )
    }

    var openCategory by rememberSaveable { mutableStateOf<PreparationCategory?>(null) }
    // Technical text is built for the open category only, off the main thread.
    var sheetText by remember { mutableStateOf<Pair<PreparationCategory, PreparationChecklistText>?>(null) }
    LaunchedEffect(
        openCategory,
        state.session, state.network, state.device, state.location,
        state.runtimeSecurity, state.bypass, state.diagnostics,
        uiLanguage, accessibilityInspection,
        accessibilityGuardEnabled, accessibilityGuardAvailable,
        accessibilityGuardRequired, needsBluetoothPermission
    ) {
        val category = openCategory ?: return@LaunchedEffect
        val text = withContext(LowRamDispatchers.detectorIo) {
            buildPreparationCategoryText(
                category = category,
                state = state,
                uiLanguage = uiLanguage,
                accessibilityInspection = accessibilityInspection,
                accessibilityGuardEnabled = accessibilityGuardEnabled,
                accessibilityGuardAvailable = accessibilityGuardAvailable,
                accessibilityGuardRequired = accessibilityGuardRequired,
                needsBluetoothPermission = needsBluetoothPermission
            )
        }
        sheetText = category to text
    }

    val examTitle = state.examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val isRefreshing = state.isRefreshingGeofence || state.isRefreshingNetwork
    val screenPadding = adaptiveScreenPadding()
    val tokens = ScopedUiTokens.current
    val listState = rememberLazyListState()
    val itemModifier = Modifier
        .widthIn(max = PreparationContentMaxWidth)
        .fillMaxWidth()
    val blockingAttention = overview.attention.filter { it.tone == PreparationTone.Blocking }
    val warningAttention = overview.attention.filter { it.tone == PreparationTone.Warning }
    val showSessionNotices = hasPreparationSessionNotices(state)

    UpgradeUiScope(motionPolicy = motionPolicy) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.current.background)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // While the sheet is open it is modal: hide the page underneath from
                    // TalkBack so focus cannot wander behind the scrim.
                    .then(if (openCategory != null) Modifier.clearAndSetSemantics {} else Modifier)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = screenPadding,
                        top = 4.dp,
                        end = screenPadding,
                        bottom = 16.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tokens.spaceMedium)
                ) {
                    item(key = "top_bar") {
                        PreparationTopBar(
                            examTitle = examTitle,
                            isRefreshing = isRefreshing,
                            onBackHome = screenActions.onBackHome,
                            onRefresh = screenActions.session.onRefreshAllSecurityChecks,
                            modifier = itemModifier
                        )
                    }
                    item(key = "status") {
                        PreparationStatusCard(
                            overview = overview,
                            hasBypassIndicators = readiness.hasBypassIndicators,
                            modifier = itemModifier
                        )
                    }
                    interaction.feedbackText?.let { feedback ->
                        item(key = "feedback") {
                            StatusBanner(
                                message = feedback,
                                tone = UiStatusTone.Info,
                                modifier = itemModifier
                            )
                        }
                    }
                    if (showSessionNotices) {
                        item(key = "session_notices") {
                            PreparationSessionNotices(
                                state = state,
                                actions = screenActions,
                                runQuickFix = interaction.runQuickFix,
                                modifier = itemModifier
                            )
                        }
                    }
                    if (blockingAttention.isNotEmpty()) {
                        item(key = "blocking_header") {
                            PreparationSectionHeader(
                                text = tr("Fix these first", "Perbaiki dulu"),
                                modifier = itemModifier
                            )
                        }
                        items(blockingAttention, key = { "attention_${it.category.key}" }) { status ->
                            PreparationAttentionCard(
                                status = status,
                                hasGlobalBlockingIssues = hasGlobalBlockingIssues,
                                onOpenDetails = { openCategory = status.category },
                                modifier = itemModifier
                            )
                        }
                    }
                    if (warningAttention.isNotEmpty()) {
                        item(key = "warning_header") {
                            PreparationSectionHeader(
                                text = tr("Suggestions (optional)", "Saran (opsional)"),
                                modifier = itemModifier
                            )
                        }
                        items(warningAttention, key = { "attention_${it.category.key}" }) { status ->
                            PreparationAttentionCard(
                                status = status,
                                hasGlobalBlockingIssues = hasGlobalBlockingIssues,
                                onOpenDetails = { openCategory = status.category },
                                modifier = itemModifier
                            )
                        }
                    }
                    item(key = "all_checks_header") {
                        PreparationSectionHeader(
                            text = tr("All checks · tap for details", "Semua pemeriksaan · ketuk untuk detail"),
                            modifier = itemModifier
                        )
                    }
                    item(key = "all_checks_grid") {
                        PreparationCategoryGrid(
                            categories = overview.categories,
                            onOpenCategory = { openCategory = it },
                            scanPending = overview.scanPending,
                            modifier = itemModifier
                        )
                    }
                }
                PreparationStartBar(
                    overview = overview,
                    isStartingExam = state.isStartingExam,
                    webViewSessionResetInFlight = state.webViewSessionResetInFlight,
                    hasBypassIndicators = readiness.hasBypassIndicators,
                    onStartExam = screenActions.onStartExam
                )
            }

            PreparationCategorySheetHost(
                openCategory = openCategory,
                onDismiss = { openCategory = null }
            ) { category ->
                val status = overview.status(category)
                val text = sheetText
                    ?.takeIf { it.first == category }
                    ?.second
                    ?: loadingPreparationChecklistText(uiLanguage)
                val hasMapAction = status.actions.any { it.target == QuickFixTarget.Location && it.code == "quick_fix_55" }
                PreparationCategorySheetContent(
                    status = status,
                    hasGlobalBlockingIssues = hasGlobalBlockingIssues,
                    showGeofenceMapAction = category == PreparationCategory.Location &&
                        state.geofenceRuntimeStatus.evaluation.enabled &&
                        !hasMapAction,
                    onOpenGeofenceMap = screenActions.onOpenGeofenceMapViewer,
                    onDismiss = { openCategory = null }
                ) {
                    PreparationCategorySectionContent(
                        category = category,
                        state = state,
                        actions = screenActions,
                        text = text,
                        needsBluetoothPermission = needsBluetoothPermission,
                        accessibilityInspection = accessibilityInspection,
                        accessibilityGuardAvailable = accessibilityGuardAvailable,
                        accessibilityGuardRequired = accessibilityGuardRequired,
                        accessibilityGuardEnabled = accessibilityGuardEnabled
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Session notices: things in progress, not device problems
// ──────────────────────────────────────────────────────────────

internal fun hasPreparationSessionNotices(state: PreparationScreenState): Boolean {
    val pinningPending = state.pinningActivationState.isPending()
    val pinningRetryReady = state.pinningActivationState == PinningActivationState.TimeoutRetryReady
    return state.webViewSessionResetInFlight ||
        state.webViewSessionResetError != null ||
        ((pinningPending || pinningRetryReady) && !state.bypassScreenPinning)
}

@Composable
private fun PreparationSessionNotices(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    runQuickFix: (QuickFixTarget?, String, Boolean, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spaceSmall)
    ) {
        if (state.webViewSessionResetInFlight) {
            StatusBanner(
                title = tr("Preparing a clean exam browser", "Menyiapkan browser ujian bersih"),
                message = tr(
                    "Clearing cookies, local storage, and cached browser data before the exam opens.",
                    "Membersihkan cookie, local storage, dan cache browser sebelum ujian dibuka."
                ),
                tone = UiStatusTone.Info
            )
        }

        state.webViewSessionResetError?.let { resetError ->
            // The health item detail is English-only; this note is in the student's language.
            val webViewProviderNote = webViewProviderNote(
                state.webViewCompatibilityStatus,
                LocalUiLanguage.current
            ).orEmpty()
            StatusBanner(
                title = tr("Exam browser recovered safely", "Browser ujian dipulihkan dengan aman"),
                message = listOf(
                    resetError,
                    tr(
                        "Export diagnostics if an admin needs evidence, then start again.",
                        "Export diagnostik bila admin butuh bukti, lalu mulai lagi."
                    ),
                    webViewProviderNote
                ).filter { it.isNotBlank() }.joinToString("\n\n"),
                tone = UiStatusTone.Danger
            )
            val shape = RoundedCornerShape(tokens.radiusMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spaceSmall)
            ) {
                OutlinedButton(
                    onClick = actions.onExportDiagnostics,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = tokens.touchTarget),
                    shape = shape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.current.brandText),
                    border = BorderStroke(1.dp, AppColors.current.outline)
                ) {
                    Text(
                        text = tr("Export diagnostics", "Export diagnostik"),
                        style = AppTextStyles.button,
                        textAlign = TextAlign.Center
                    )
                }
                val primaryColors = primaryActionColors()
                Button(
                    onClick = actions.onStartExam,
                    enabled = !(state.isStartingExam || state.webViewSessionResetInFlight),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = tokens.touchTarget),
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColors.container,
                        contentColor = primaryColors.content,
                        disabledContainerColor = AppColors.current.surfaceSoft,
                        disabledContentColor = AppColors.current.textSecondary
                    )
                ) {
                    Text(
                        text = tr("Start again", "Mulai lagi"),
                        style = AppTextStyles.button,
                        textAlign = TextAlign.Center
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    runQuickFix(
                        QuickFixTarget.WebView,
                        "webview_provider_recovery_fix",
                        true,
                        actions.onOpenWebViewProviderSettings
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.touchTarget),
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.current.brandText),
                border = BorderStroke(1.dp, AppColors.current.outline)
            ) {
                Text(
                    text = tr("Open WebView settings", "Buka setelan WebView"),
                    style = AppTextStyles.button,
                    textAlign = TextAlign.Center
                )
            }
        }

        val pinningPending = state.pinningActivationState.isPending()
        val pinningRetryReady = state.pinningActivationState == PinningActivationState.TimeoutRetryReady
        if ((pinningPending || pinningRetryReady) && !state.bypassScreenPinning) {
            StatusBanner(
                title = if (pinningRetryReady) {
                    tr("Screen pinning not active yet", "Screen pinning belum aktif")
                } else {
                    tr("Starting screen pinning…", "Menjalankan screen pinning…")
                },
                message = if (pinningRetryReady) {
                    tr(
                        "1. Tap \"Start Screen Pinning\" again.\n2. When Android asks, tap \"Got it\" or \"Pin\".\n3. Stay on this screen; do not press Home or Recents.",
                        "1. Ketuk \"Aktifkan Screen Pinning\" lagi.\n2. Saat Android bertanya, ketuk \"Got it\" atau \"Pin\".\n3. Tetap di layar ini; jangan tekan Home atau Recent."
                    )
                } else {
                    state.screenPinningMessage ?: tr(
                        "1. When Android asks, tap \"Got it\" or \"Pin\".\n2. Stay on this screen; do not press Home or Recents.\n3. Wait until screen pinning is active.",
                        "1. Saat Android bertanya, ketuk \"Got it\" atau \"Pin\".\n2. Tetap di layar ini; jangan tekan Home atau Recent.\n3. Tunggu sampai screen pinning aktif."
                    )
                },
                tone = if (pinningRetryReady) UiStatusTone.Danger else UiStatusTone.Warning
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Refresh throttling and quick-fix return handling
// ──────────────────────────────────────────────────────────────

private const val ExamScheduleTickMillis = 15_000L

@Stable
internal class PreparationInteraction(
    val actions: PreparationScreenActions,
    val runQuickFix: (QuickFixTarget?, String, Boolean, () -> Unit) -> Unit,
    private val feedbackState: androidx.compose.runtime.State<String?>,
    private val resumeState: androidx.compose.runtime.State<Int>
) {
    val feedbackText: String? get() = feedbackState.value
    val resumeCount: Int get() = resumeState.value
}

private class ManualRefreshThrottle {
    var lastAtElapsedMs: Long = 0L
    var lastKey: String? = null
    var pendingJob: Job? = null
    var pendingAction: (() -> Unit)? = null
}

@Composable
private fun rememberPreparationInteraction(
    actions: PreparationScreenActions,
    cooldownMillis: Long,
    uiLanguage: UiLanguage
): PreparationInteraction {
    val latestActions by rememberUpdatedState(actions)
    val latestCooldownMillis by rememberUpdatedState(cooldownMillis)
    val latestLanguage by rememberUpdatedState(uiLanguage)
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    // Saveable: on low-RAM phones the activity can be recreated while the student is in
    // Settings, and the return must still trigger a re-check.
    val pendingTarget = rememberSaveable { mutableStateOf<QuickFixTarget?>(null) }
    val pendingCode = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingOpenedExternalSettings = rememberSaveable { mutableStateOf(false) }
    val feedbackText = remember { mutableStateOf<String?>(null) }
    val resumeCount = remember { mutableIntStateOf(0) }
    val throttle = remember { ManualRefreshThrottle() }

    fun showFeedback(text: String, durationMillis: Long) {
        feedbackText.value = text
        scope.launch {
            delay(durationMillis)
            if (feedbackText.value == text) {
                feedbackText.value = null
            }
        }
    }

    fun runWithCooldown(key: String, action: () -> Unit) {
        val cooldown = latestCooldownMillis
        if (cooldown <= 0L) {
            action()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - throttle.lastAtElapsedMs
        if (throttle.lastKey != key || elapsed >= cooldown) {
            throttle.pendingJob?.cancel()
            throttle.pendingJob = null
            throttle.pendingAction = null
            throttle.lastKey = key
            throttle.lastAtElapsedMs = now
            action()
            return
        }
        val wait = (cooldown - elapsed).coerceAtLeast(0L)
        throttle.pendingAction = action
        showFeedback(
            localized(latestLanguage, "Refresh queued; running shortly.", "Refresh dijadwalkan; segera berjalan."),
            wait + 500L
        )
        if (throttle.pendingJob == null) {
            throttle.pendingJob = scope.launch {
                delay(wait)
                val queued = throttle.pendingAction
                throttle.pendingAction = null
                throttle.pendingJob = null
                throttle.lastKey = key
                throttle.lastAtElapsedMs = SystemClock.elapsedRealtime()
                queued?.invoke()
            }
        }
    }

    val runQuickFix: (QuickFixTarget?, String, Boolean, () -> Unit) -> Unit = remember {
        { target, actionCode, opensExternalSettings, action ->
            latestActions.onAutoFixActionOpened(actionCode)
            if (target != null || opensExternalSettings) {
                pendingTarget.value = target ?: QuickFixTarget.All
            }
            pendingCode.value = actionCode
            pendingOpenedExternalSettings.value = opensExternalSettings
            if (opensExternalSettings) {
                showFeedback(
                    localized(
                        latestLanguage,
                        "Return to the app; status will be checked again.",
                        "Kembali ke aplikasi, status akan dicek ulang."
                    ),
                    15_000L
                )
            }
            action()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) {
                return@LifecycleEventObserver
            }
            resumeCount.intValue += 1
            val target = pendingTarget.value
            val fixCode = pendingCode.value
            if (target == null && fixCode == null) {
                return@LifecycleEventObserver
            }
            pendingTarget.value = null
            val current = latestActions
            when (target) {
                null -> Unit
                QuickFixTarget.Network -> current.onRefreshNetworkStatus()
                QuickFixTarget.Location -> current.onRefreshGeofenceLocation()
                QuickFixTarget.DeviceTime,
                QuickFixTarget.ScreenPinning,
                QuickFixTarget.WebView,
                QuickFixTarget.Battery,
                QuickFixTarget.ScreenRecorder,
                QuickFixTarget.DisplayMirror,
                QuickFixTarget.MultiWindow -> current.onRefreshStatus()
                QuickFixTarget.All -> current.onRefreshAllSecurityChecks()
            }
            // Some states (Bluetooth, ADB) need a moment to settle after Settings closes.
            scope.launch {
                delay(600L)
                latestActions.onRefreshStatus()
            }
            if (fixCode != null) {
                pendingCode.value = null
                pendingOpenedExternalSettings.value = false
                showFeedback(
                    localized(latestLanguage, "Status checked again.", "Status dicek ulang."),
                    3_000L
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            throttle.pendingJob?.cancel()
        }
    }

    // Built once: every lambda forwards to the latest actions, so the quick-fix list and
    // the cards only rebuild when the device state changes, not on every upstream pass.
    val throttledActions = remember {
        val forwarding = forwardingPreparationActions { latestActions }
        forwarding.copy(
            session = forwarding.session.copy(
                onRefreshStatus = {
                    runWithCooldown("status") { latestActions.onRefreshStatus() }
                },
                onRefreshAllSecurityChecks = {
                    runWithCooldown("all") { latestActions.onRefreshAllSecurityChecks() }
                }
            ),
            network = forwarding.network.copy(
                onRefreshNetworkStatus = {
                    runWithCooldown("network") { latestActions.onRefreshNetworkStatus() }
                }
            ),
            location = forwarding.location.copy(
                onRefreshGeofenceLocation = {
                    runWithCooldown("location") { latestActions.onRefreshGeofenceLocation() }
                }
            )
        )
    }

    return remember(throttledActions) {
        PreparationInteraction(
            actions = throttledActions,
            runQuickFix = runQuickFix,
            feedbackState = feedbackText,
            resumeState = resumeCount
        )
    }
}
