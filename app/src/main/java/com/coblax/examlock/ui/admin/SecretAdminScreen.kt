package com.coblax.examlock.ui.admin

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.coblax.examlock.AdbBypassResolver
import com.coblax.examlock.AppSwitchBypassResolver
import com.coblax.examlock.AppSwitchMonitor
import com.coblax.examlock.AppSwitchProtectionMode
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.buildDeviceSurvivalPolicy
import com.coblax.examlock.buildRootSecurityStatus
import com.coblax.examlock.ClipboardChangeDecision
import com.coblax.examlock.ClipboardRuntimeStatus
import com.coblax.examlock.config.DefaultExamUserAgent
import com.coblax.examlock.config.FastExamName
import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.DeviceTimeBypassResolver
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DeviceTimeSecurityVerdict
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.evaluateFakeLocationSecurity
import com.coblax.examlock.evaluateGeofence
import com.coblax.examlock.evaluateGeofenceSecurity
import com.coblax.examlock.evaluateLocationFixQuality
import com.coblax.examlock.ExamDeviceOwnerController
import com.coblax.examlock.FakeLocationBypassResolver
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.format.buildIntegrityPublicSummary
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.GeofenceBypassResolver
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.diagnosticSectionLabel
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.inspectAccessibility
import com.coblax.examlock.inspectAdb
import com.coblax.examlock.inspectDeviceTimeSecurity
import com.coblax.examlock.IntegrityCheckResult
import com.coblax.examlock.IntegrityGuard
import com.coblax.examlock.isExamGuardAccessibilityAvailable
import com.coblax.examlock.isExamGuardAccessibilityEnabled
import com.coblax.examlock.launchFirstPlatformIntentSafely
import com.coblax.examlock.LocalDeviceCompatibilityProfile
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.directLinkLocationPolicy
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.model.ExamOfflineRuntimeStatus
import com.coblax.examlock.model.SecretAdminTab
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.model.usesDefaultExamUserAgent
import com.coblax.examlock.model.withoutDirectLinkLocationPolicy
import com.coblax.examlock.openOverlaySettings

import com.coblax.examlock.openWebViewProviderSettings
import com.coblax.examlock.OverlayRiskAnalyzer
import com.coblax.examlock.OverlayShieldStatus
import com.coblax.examlock.parseGeofenceConfig
import com.coblax.examlock.R
import com.coblax.examlock.readWebViewCompatibilityStatus
import com.coblax.examlock.ReverseEngineeringGuard
import com.coblax.examlock.ReverseEngineeringResult
import com.coblax.examlock.RootBypassResolver
import com.coblax.examlock.runtime.getRootDetectionDetails
import com.coblax.examlock.runtime.hasFineLocationPermission
import com.coblax.examlock.runtime.hasLocationPermissionForWifi
import com.coblax.examlock.runtime.isLocationServicesEnabled
import com.coblax.examlock.runtime.LowRamDispatchers
import com.coblax.examlock.runtime.readExamBatteryStatus
import com.coblax.examlock.runtime.readExamNetworkStatus
import com.coblax.examlock.runtime.readNetworkReadinessStatusWithProbe
import com.coblax.examlock.runtime.sendTelegramSectionReport
import com.coblax.examlock.ScreenPinningPlatformBridge
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.ui.exam.ExamRuntimeHardeningDiagnostics
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.LocalTelegramDiagnosticsEnabled
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.StatusBanner as UiStatusBanner
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialog
import com.coblax.examlock.ui.dialog.AppAlertStyle
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.WebViewHealthSeverity

import java.net.URL
import java.util.Date
import java.util.TimeZone

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SecretAdminPerfTag = "SecretAdminPerf"
private const val SecretAdminRapidRecomposeWindowMs = 250L
private const val SecretAdminRapidRecomposeThreshold = 3

private inline fun <T> debugMeasureSecretAdminWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                SecretAdminPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

private suspend inline fun <T> debugMeasureSecretAdminSuspendWork(
    label: String,
    crossinline block: suspend () -> T
): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                SecretAdminPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

@Composable
private fun DebugSecretAdminRecomposeTrace(selectedTab: SecretAdminTab) {
    if (!BuildConfig.DEBUG) return

    var burstCount by remember { mutableIntStateOf(0) }
    var lastCommitAtMs by remember { mutableLongStateOf(0L) }

    SideEffect {
        val now = SystemClock.elapsedRealtime()
        burstCount = if (
            lastCommitAtMs != 0L &&
            now - lastCommitAtMs <= SecretAdminRapidRecomposeWindowMs
        ) {
            burstCount + 1
        } else {
            1
        }
        lastCommitAtMs = now
        if (burstCount == SecretAdminRapidRecomposeThreshold) {
            Log.d(
                SecretAdminPerfTag,
                "Rapid recomposition burst detected on tab=${selectedTab.name}"
            )
        }
    }
}

@Composable
internal fun SecretAdminScreen(
    settings: AdminSettings,
    examName: String,
    onSettingsChange: (AdminSettings) -> Unit,
    onResetDirectLink: () -> Unit,
    onBack: () -> Unit,
    deviceTimeBaselineWallClockMillis: Long,
    deviceTimeBaselineElapsedRealtimeMillis: Long,
    modifier: Modifier = Modifier,
    selectedTabName: String = SecretAdminTab.Setup.name,
    onSelectedTabNameChange: (String) -> Unit = {},
    externalDraftSettings: AdminSettings? = null,
    onDraftSettingsChange: (AdminSettings) -> Unit = {},
    onApplySettings: (AdminSettings) -> Unit = onSettingsChange,
    onRevertSettings: () -> Unit = {},
    isApplyInProgress: Boolean = false,
    applyStatusMessage: String? = null,
    applyStatusIsError: Boolean = false
) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val lowRamProfile = LocalLowRamProfile.current
    val deviceCompatibilityProfile = LocalDeviceCompatibilityProfile.current
    var localDraftSettings by remember { mutableStateOf(settings) }
    var previousPersistedSettings by remember { mutableStateOf(settings) }
    var submittedDraftSettings by remember { mutableStateOf<AdminSettings?>(null) }
    var pendingExitConfirmation by rememberSaveable { mutableStateOf(false) }
    var exitAfterSuccessfulApply by rememberSaveable { mutableStateOf(false) }
    val draftAdminSettings = externalDraftSettings ?: localDraftSettings
    val adminSettingsDirty = draftAdminSettings != settings

    fun updateDraftSettings(updated: AdminSettings) {
        if (externalDraftSettings == null) {
            localDraftSettings = updated
        }
        onDraftSettingsChange(updated)
    }

    fun revertDraftSettings() {
        if (externalDraftSettings == null) {
            localDraftSettings = settings
        }
        submittedDraftSettings = null
        onRevertSettings()
    }

    fun applyDraftSettings(exitAfterApply: Boolean = false) {
        if (!adminSettingsDirty || isApplyInProgress) return
        submittedDraftSettings = draftAdminSettings
        exitAfterSuccessfulApply = exitAfterApply
        onApplySettings(draftAdminSettings)
    }

    fun requestBack() {
        if (adminSettingsDirty || isApplyInProgress) {
            pendingExitConfirmation = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(settings, externalDraftSettings) {
        if (externalDraftSettings == null) {
            val mayFollowPersistedState =
                localDraftSettings == previousPersistedSettings || submittedDraftSettings != null
            if (mayFollowPersistedState) {
                localDraftSettings = settings
            }
        }
        previousPersistedSettings = settings
        if (submittedDraftSettings != null && !isApplyInProgress && !applyStatusIsError) {
            submittedDraftSettings = null
        }
    }

    LaunchedEffect(
        exitAfterSuccessfulApply,
        isApplyInProgress,
        draftAdminSettings,
        settings,
        applyStatusIsError
    ) {
        if (
            exitAfterSuccessfulApply &&
            !isApplyInProgress &&
            draftAdminSettings == settings &&
            !applyStatusIsError
        ) {
            exitAfterSuccessfulApply = false
            onBack()
        }
    }

    BackHandler(enabled = adminSettingsDirty || isApplyInProgress) {
        requestBack()
    }
    val vendorChecklist = remember(deviceCompatibilityProfile.manufacturer, deviceCompatibilityProfile.brand) {
        resolveDeviceVendorChecklist(
            manufacturer = deviceCompatibilityProfile.manufacturer,
            brand = deviceCompatibilityProfile.brand
        )
    }
    val effectiveExamUserAgent = remember(settings.examUserAgent) {
        debugMeasureSecretAdminWork("effectiveExamUserAgent") {
            settings.effectiveExamUserAgent()
        }
    }
    val deviceTimeBaseline = remember(
        deviceTimeBaselineWallClockMillis,
        deviceTimeBaselineElapsedRealtimeMillis
    ) {
        DeviceTimeBaseline(
            wallClockMillis = deviceTimeBaselineWallClockMillis,
            elapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        )
    }
    val overridesActive = remember(
        draftAdminSettings
    ) {
        debugMeasureSecretAdminWork("hasAnyBypass") {
            draftAdminSettings.hasAnyBypass()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var healthIntegritySummary by rememberSaveable { mutableStateOf("OK") }
    var healthReverseDetected by rememberSaveable { mutableStateOf(false) }
    var healthLastCheckedAt by rememberSaveable { mutableStateOf<String?>(null) }
    var healthBaselineFingerprint by rememberSaveable { mutableStateOf<String?>(null) }
    var healthChecking by rememberSaveable { mutableStateOf(false) }
    var healthIntegrityResult by remember { mutableStateOf<IntegrityCheckResult?>(null) }
    var healthReverseResult by remember { mutableStateOf<ReverseEngineeringResult?>(null) }
    var healthDeviceTimeStatus by remember { mutableStateOf<DeviceTimeSecurityStatus?>(null) }
    var pendingSecurityHealthReport by rememberSaveable { mutableStateOf(false) }
    var sendingSecurityHealthReport by rememberSaveable { mutableStateOf(false) }
    var securityHealthFeedbackTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var securityHealthFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var advancedDiagnosticsExpanded by rememberSaveable { mutableStateOf(false) }
    var fieldReadinessRunning by rememberSaveable { mutableStateOf(false) }
    var fieldReadinessReport by remember { mutableStateOf<FieldReadinessReport?>(null) }
    var adminWebViewRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    val adminWebViewCompatibilityStatus = remember(context, adminWebViewRefreshKey) {
        readWebViewCompatibilityStatus(context.applicationContext)
    }
    val fieldSurvivalPolicy = remember(
        lowRamProfile,
        deviceCompatibilityProfile,
        adminWebViewCompatibilityStatus,
        fieldReadinessReport
    ) {
        buildDeviceSurvivalPolicy(
            lowRamProfile = lowRamProfile,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            webViewCompatibilityStatus = adminWebViewCompatibilityStatus,
            fieldReadinessReport = fieldReadinessReport
        )
    }
    val adminReadinessSummary = remember(
        fieldReadinessReport,
        adminWebViewCompatibilityStatus,
        vendorChecklist
    ) {
        buildAdminReadinessSummary(
            report = fieldReadinessReport,
            webViewCompatibilityStatus = adminWebViewCompatibilityStatus,
            vendorChecklist = vendorChecklist
        )
    }
    LaunchedEffect(adminWebViewCompatibilityStatus.diagnosticSummary()) {
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthResolved} level=INFO details=${adminWebViewCompatibilityStatus.diagnosticSummary()}"
        )
        if (adminWebViewCompatibilityStatus.severity != WebViewHealthSeverity.Stable) {
            Log.w(
                "ExamRuntimeHardening",
                "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthWarning} level=WARNING details=${adminWebViewCompatibilityStatus.adminDetail}"
            )
        }
    }

    fun runFieldReadinessTest() {
        if (fieldReadinessRunning) return
        fieldReadinessRunning = true
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.FieldReadinessTestStarted} level=INFO details=family=${deviceCompatibilityProfile.family.name}"
        )
        coroutineScope.launch {
            runCatching {
                val appContext = context.applicationContext
                val accessibilityInspection = inspectAccessibility(appContext)
                val directLinkPolicy = settings.directLinkLocationPolicy()
                val screenPinningAvailable = ScreenPinningPlatformBridge.isAvailable()
                val overlayRiskResult = OverlayRiskAnalyzer.inspect(
                    bypassed = settings.bypassOverlay,
                    accessibilityEnabled = accessibilityInspection.blockingServiceActive,
                    riskyAccessibilityPackages = accessibilityInspection.riskyPackages,
                    violationCount = 0,
                    shieldStatus = OverlayShieldStatus(
                        supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        requested = false,
                        lastApplySucceeded = null,
                        lastApplyAt = null
                    ),
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                )
                buildFieldReadinessReport(
                    FieldReadinessInput(
                        generatedAt = diagnosticTimestamp(),
                        compatibilityProfile = deviceCompatibilityProfile,
                        screenPinningAvailable = screenPinningAvailable,
                        screenPinningSystemSetting = ScreenPinningPlatformBridge.readSystemSetting(appContext),
                        lockTaskState = readSecretAdminLockTaskStateLabel(appContext),
                        accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(appContext),
                        accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(appContext),
                        overlayRiskResult = overlayRiskResult,
                        webViewCompatibilityStatus = readWebViewCompatibilityStatus(appContext),
                        networkReadinessStatus = withContext(Dispatchers.IO) {
                            readNetworkReadinessStatusWithProbe(appContext)
                        },
                        batteryStatus = readExamBatteryStatus(appContext),
                        locationPermissionGranted = hasLocationPermissionForWifi(appContext),
                        preciseLocationGranted = hasFineLocationPermission(appContext),
                        locationServicesEnabled = isLocationServicesEnabled(appContext),
                        geofencePolicyEnabled = directLinkPolicy?.geofenceEnabled == true,
                        fakeLocationMonitoringEnabled = !settings.bypassFakeLocation,
                        deviceTimeSecurityStatus = inspectDeviceTimeSecurity(
                            context = appContext,
                            baseline = deviceTimeBaseline,
                            bypassState = DeviceTimeBypassResolver.stateOf(
                                enabled = settings.bypassDeviceTime,
                                tampered = settings.deviceTimeBypassTampered
                            )
                        )
                    )
                )
            }.onSuccess { report ->
                fieldReadinessReport = report
                Log.i(
                    "ExamRuntimeHardening",
                    "code=${ExamRuntimeHardeningDiagnostics.FieldReadinessTestCompleted} level=INFO details=${report.diagnosticSummary()}"
                )
            }.onFailure { throwable ->
                securityHealthFeedbackTitle = localized(
                    uiLanguage,
                    "Field test failed",
                    "Field test gagal"
                )
                securityHealthFeedbackMessage =
                    throwable.message ?: throwable.javaClass.simpleName
            }
            fieldReadinessRunning = false
        }
    }

    fun openSettingsIntent(action: String) {
        launchFirstPlatformIntentSafely(
            context,
            listOf(
                Intent(action),
                Intent(Settings.ACTION_SETTINGS)
            )
        )
    }

    fun openAdminWebViewProviderSettings() {
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthFixOpened} level=INFO details=${adminWebViewCompatibilityStatus.adminDetail}"
        )
        openWebViewProviderSettings(
            context = context,
            providerPackageName = adminWebViewCompatibilityStatus.packageName
        )
    }
    val directLinkPolicySummary = remember(
        draftAdminSettings.directLinkLocationPolicySaved,
        draftAdminSettings.directLinkLocationPolicySerialized,
        draftAdminSettings.directLinkGeofenceEnabled,
        draftAdminSettings.directLinkGeofenceCenterLat,
        draftAdminSettings.directLinkGeofenceCenterLng,
        draftAdminSettings.directLinkGeofenceRadiusMeters,
        uiLanguage
    ) {
        debugMeasureSecretAdminWork("directLinkPolicySummary") {
            if (draftAdminSettings.directLinkLocationPolicySaved) {
                when (val directLinkPolicy = draftAdminSettings.directLinkLocationPolicy()) {
                    null -> localized(
                        uiLanguage,
                        "Direct Link has no saved geofence policy.",
                        "Direct Link belum punya policy geofence tersimpan."
                    )
                    else -> when (directLinkPolicy.shapeType) {
                        GeofenceShapeType.Circle -> localized(
                            uiLanguage,
                            "Direct Link circle geofence saved from QR: ${directLinkPolicy.effectiveCircleCenters.size} centers | ${directLinkPolicy.radiusMeters} m | primary ${
                                directLinkPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                                    "${center.latitude}, ${center.longitude}"
                                } ?: "-"
                            }",
                            "Geofence lingkaran Direct Link tersimpan dari QR: ${directLinkPolicy.effectiveCircleCenters.size} center | ${directLinkPolicy.radiusMeters} m | utama ${
                                directLinkPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                                    "${center.latitude}, ${center.longitude}"
                                } ?: "-"
                            }"
                        )
                        GeofenceShapeType.Polygon -> localized(
                            uiLanguage,
                            "Direct Link polygon geofence saved from QR: ${directLinkPolicy.vertices.size} points.",
                            "Geofence polygon Direct Link tersimpan dari QR: ${directLinkPolicy.vertices.size} titik."
                        )
                        GeofenceShapeType.Disabled -> localized(
                            uiLanguage,
                            "Direct Link location policy saved from QR: geofence disabled.",
                            "Policy lokasi Direct Link tersimpan dari QR: geofence nonaktif."
                        )
                    }
                }
            } else {
                localized(
                    uiLanguage,
                    "Direct Link has no saved geofence policy.",
                    "Direct Link belum punya policy geofence tersimpan."
                )
            }
        }
    }
    val healthDeviceTimeLabel = remember(healthDeviceTimeStatus, uiLanguage) {
        when {
            healthDeviceTimeStatus == null -> "-"
            healthDeviceTimeStatus?.bypassState == DeviceTimeBypassState.Tampered ->
                localized(uiLanguage, "Tampered", "Tampered")
            healthDeviceTimeStatus?.bypassActive == true ->
                localized(uiLanguage, "Bypassed", "Bypass")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.Safe ->
                localized(uiLanguage, "Safe", "Aman")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled ->
                localized(uiLanguage, "Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled ->
                localized(uiLanguage, "Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected ->
                localized(uiLanguage, "Clock Change", "Perubahan Jam")
            else -> localized(uiLanguage, "Action Needed", "Perlu Aksi")
        }
    }

    suspend fun refreshSecurityHealth() {
        if (healthChecking) return
        healthChecking = true
        try {
            val integrityResult = debugMeasureSecretAdminSuspendWork("refreshSecurityHealth:integrity") {
                withContext(LowRamDispatchers.detectorIo) {
                    IntegrityGuard.check(context, healthBaselineFingerprint)
                }
            }
            val reverseResult = debugMeasureSecretAdminSuspendWork("refreshSecurityHealth:reverse") {
                withContext(LowRamDispatchers.detectorIo) {
                    ReverseEngineeringGuard.inspect(context)
                }
            }
            if (healthBaselineFingerprint.isNullOrBlank() &&
                integrityResult.currentFingerprint.isNotBlank() &&
                integrityResult.currentFingerprint != "-"
            ) {
                healthBaselineFingerprint = integrityResult.currentFingerprint
            }
            val checkedAt = diagnosticTimestamp()
            val deviceTimeStatus = inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = DeviceTimeBypassResolver.stateOf(
                    enabled = settings.bypassDeviceTime,
                    tampered = settings.deviceTimeBypassTampered
                )
            )
            healthIntegrityResult = integrityResult
            healthReverseResult = reverseResult
            healthDeviceTimeStatus = deviceTimeStatus
            healthIntegritySummary = buildIntegrityPublicSummary(integrityResult.issues)
            healthReverseDetected = reverseResult.tamperDetected
            healthLastCheckedAt = checkedAt
        } finally {
            healthChecking = false
            if (BuildConfig.DEBUG) {
                Log.d(SecretAdminPerfTag, "refreshSecurityHealth state updated")
            }
        }
    }

    suspend fun sendSecurityHealthReport() {
        if (sendingSecurityHealthReport || healthChecking) return
        sendingSecurityHealthReport = true
        try {
            debugMeasureSecretAdminSuspendWork("sendSecurityHealthReport:refresh") {
                refreshSecurityHealth()
            }
            val latestIntegrityResult = healthIntegrityResult
            val latestReverseResult = healthReverseResult
            val latestDeviceTimeStatus =
                healthDeviceTimeStatus ?: inspectDeviceTimeSecurity(
                    context = context,
                    baseline = deviceTimeBaseline,
                    bypassState = DeviceTimeBypassResolver.stateOf(
                        enabled = settings.bypassDeviceTime,
                        tampered = settings.deviceTimeBypassTampered
                    )
                )
            val latestCheckedAt = healthLastCheckedAt ?: diagnosticTimestamp()
            val resolvedExamName = examName.trim()
                .ifBlank { settings.fastExamLabel.trim() }
                .ifBlank { "-" }
            val section = DiagnosticSection.SecurityHealth
            val sectionLabel = diagnosticSectionLabel(section, uiLanguage)

            if (latestIntegrityResult == null || latestReverseResult == null) {
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                securityHealthFeedbackMessage = localized(
                    uiLanguage,
                    "Security Health data is not ready yet. Refresh and try again.",
                    "Data Security Health belum siap. Refresh lalu coba lagi."
                )
                return
            }

            debugMeasureSecretAdminSuspendWork("sendSecurityHealthReport:telegram") {
                sendTelegramSectionReport(
                context = context,
                section = section,
                examName = resolvedExamName,
                examUserAgent = effectiveExamUserAgent,
                examUserAgentSource = if (settings.usesDefaultExamUserAgent()) "default" else "custom",
                participantContext = null,
                examSessionStarted = false,
                examRuntimeGuardsArmed = false,
                adminOverridesSummary = settings.overrideSummary(),
                keyboardPackage = "",
                keyboardAllowed = false,
                usingBuiltInExamKeyboard = false,
                bluetoothPermissionGranted = false,
                bluetoothEnabled = false,
                accessibilityServiceEnabled = false,
                bypassAccessibility = settings.bypassAccessibility,
                accessibilityBypassTampered = settings.accessibilityBypassTampered,
                adbInspection = inspectAdb(context),
                adbBypassState = AdbBypassResolver.stateOf(
                    enabled = settings.bypassAdb,
                    tampered = settings.adbBypassTampered
                ),
                rootSecurityStatus = buildRootSecurityStatus(getRootDetectionDetails(context)),
                rootBypassState = RootBypassResolver.stateOf(
                    enabled = settings.bypassRoot,
                    tampered = settings.rootBypassTampered
                ),
                clipboardSignature = "",
                clipboardViolationCount = 0,
                lastClipboardChangeEvent = "-",
                networkStatus = readExamNetworkStatus(context),
                clipboardRuntimeStatus = ClipboardRuntimeStatus(
                    lastObservedAt = null,
                    lastConfirmedAt = null,
                    lastObservedSignature = null,
                    lastDecision = ClipboardChangeDecision.Idle.diagnosticLabel(),
                    baselineSemanticSignature = null,
                    detectedSemanticSignature = null,
                    currentSemanticSignature = null
                ),
                offlineRuntimeStatus = ExamOfflineRuntimeStatus(
                    offlineActive = false,
                    offlineStartedAt = null,
                    currentOfflineDurationMs = null,
                    offlineWarningShown = false,
                    lastOfflineWarningAt = null,
                    lastOfflineDurationMs = null
                ),
                geofenceRuntimeStatus = GeofenceRuntimeStatus(
                    evaluation = evaluateGeofence(
                        configResult = parseGeofenceConfig(false, "", "", ""),
                        permissionGranted = hasLocationPermissionForWifi(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null
                    ),
                    securityStatus = evaluateGeofenceSecurity(
                        configResult = parseGeofenceConfig(false, "", "", ""),
                        permissionGranted = hasLocationPermissionForWifi(context),
                        preciseLocationGranted = hasFineLocationPermission(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null,
                        bypassState = GeofenceBypassResolver.stateOf(
                            enabled = settings.bypassGeofence,
                            tampered = settings.geofenceBypassTampered
                        )
                    ),
                    policySource = if (settings.bypassGeofence) {
                        LocationPolicySource.Bypassed
                    } else {
                        LocationPolicySource.DisabledNoPolicy
                    },
                    violationCount = 0,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                fakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
                    securityStatus = evaluateFakeLocationSecurity(
                        monitoringEnabled = true,
                        permissionGranted = hasLocationPermissionForWifi(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null,
                        fixQualityStatus = evaluateLocationFixQuality(null),
                        developerOptionsEnabled = false,
                        suspiciousFakeLocationPackages = emptyList(),
                        bypassState = FakeLocationBypassResolver.stateOf(
                            enabled = settings.bypassFakeLocation,
                            tampered = settings.fakeLocationBypassTampered
                        )
                    ),
                    violationCount = 0,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                overlayViolationCount = 0,
                overlayRiskResult = OverlayRiskAnalyzer.inspect(
                    bypassed = settings.bypassOverlay,
                    accessibilityEnabled = false,
                    riskyAccessibilityPackages = emptyList(),
                    violationCount = 0,
                    shieldStatus = OverlayShieldStatus(
                        supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        requested = false,
                        lastApplySucceeded = null,
                        lastApplyAt = null
                    ),
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                overlayBypassTampered = settings.overlayBypassTampered,
                appSwitchStatus = AppSwitchMonitor.statusOf(
                    bypassState = AppSwitchBypassResolver.stateOf(
                        enabled = settings.bypassAppSwitch,
                        tampered = settings.appSwitchBypassTampered
                    ),
                    runtimeMonitoringActive = false,
                    protectionMode = if (settings.bypassAppSwitch) {
                        AppSwitchProtectionMode.Bypassed
                    } else {
                        AppSwitchProtectionMode.ProtectedByPinning
                    },
                    lockTaskActive = false,
                    violationCount = 0,
                    pendingViolation = false,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                appSwitchBypassTampered = settings.appSwitchBypassTampered,
                screenPinningAvailable = false,
                screenPinningEnabledInSystem = "-",
                lockTaskStateBeforePinningRequest = "-",
                lockTaskStateAfterPinningRequest = "-",
                screenPinningRequestOutcome = "-",
                screenPinningDialogLikelyShown = false,
                screenPinningUserActionInference = "-",
                screenPinningActivationDurationMs = null,
                examSessionCancelledByPinningFailure = false,
                isScreenPinningActive = false,
                bypassScreenPinning = settings.bypassScreenPinning,
                bypassOverlay = settings.bypassOverlay,
                bypassAppSwitch = settings.bypassAppSwitch,
                deviceTimeSecurityStatus = latestDeviceTimeStatus,
                bypassDeviceTime = settings.bypassDeviceTime,
                bypassVpn = settings.bypassVpn,
                vpnBypassTampered = settings.vpnBypassTampered,
                integritySummary = healthIntegritySummary,
                diagnosticEvents = emptyList(),
                uiLanguage = uiLanguage,
                healthIntegrityResult = latestIntegrityResult,
                healthReverseResult = latestReverseResult,
                healthLastCheckedAt = latestCheckedAt,
                webViewCompatibilityStatus = adminWebViewCompatibilityStatus,
                dpcRuntimeStatus = ExamDeviceOwnerController.readStatus(context)
            )
            }.onSuccess {
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics sent", "Diagnostik terkirim")
                securityHealthFeedbackMessage = localized(
                    uiLanguage,
                    "$sectionLabel diagnostics have been sent to Telegram.",
                    "Diagnostik $sectionLabel sudah dikirim ke Telegram."
                )
            }.onFailure { throwable ->
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                securityHealthFeedbackMessage =
                    throwable.message ?: localized(
                        uiLanguage,
                        "Diagnostics could not be sent to Telegram.",
                        "Data diagnostik belum berhasil dikirim ke Telegram."
                    )
            }
        } finally {
            sendingSecurityHealthReport = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSecurityHealth()
    }

    val selectedSecretAdminTab = remember(selectedTabName) {
        resolveSecretAdminTab(selectedTabName)
    }
    val activeOverrideCount = remember(draftAdminSettings) {
        activeSecurityOverrideCount(draftAdminSettings)
    }
    val showApplyBar = adminSettingsDirty || isApplyInProgress || !applyStatusMessage.isNullOrBlank()

    DebugSecretAdminRecomposeTrace(selectedSecretAdminTab)

    UpgradeUiScope {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.current.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val barModifier = Modifier
                .widthIn(max = SecretAdminContentMaxWidth)
                .fillMaxWidth()
            SecretAdminTopBar(
                subtitle = examName.ifBlank {
                    tr("Exam device settings", "Pengaturan perangkat ujian")
                },
                activeOverrideCount = activeOverrideCount,
                onBack = { requestBack() },
                modifier = barModifier
            )
            SecretAdminTabs(
                selected = selectedSecretAdminTab,
                activeOverrideCount = activeOverrideCount,
                onSelect = { onSelectedTabNameChange(it.name) },
                modifier = barModifier
            )
        }

        // Each tab starts at its top instead of inheriting the last tab's scroll offset.
        key(selectedSecretAdminTab) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                    .then(if (showApplyBar) Modifier else Modifier.navigationBarsPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = SecretAdminContentMaxWidth)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (settings.bypassMigrationResetNotice) {
                        UiStatusBanner(
                            message = tr(
                                "Security storage was upgraded. Existing bypasses were reset to safe OFF and must be re-enabled manually.",
                                "Penyimpanan keamanan telah ditingkatkan. Semua bypass lama direset ke OFF aman dan harus diaktifkan ulang secara manual."
                            ),
                            tone = UiStatusTone.Info
                        )
                    }
                    when (selectedSecretAdminTab) {
                        SecretAdminTab.Setup,
                        SecretAdminTab.Location -> SecretAdminSetupTab(
                            draft = draftAdminSettings,
                            directLinkPolicySummary = directLinkPolicySummary,
                            onDraftChange = { updateDraftSettings(it) }
                        )
                        SecretAdminTab.Security -> SecretAdminSecurityTab(
                            draft = draftAdminSettings,
                            healthChecking = healthChecking,
                            integritySummary = healthIntegritySummary,
                            reverseDetected = healthReverseDetected,
                            deviceTimeLabel = healthDeviceTimeLabel,
                            deviceTimeStatus = healthDeviceTimeStatus,
                            lastCheckedAt = healthLastCheckedAt,
                            sendEnabled = !healthChecking && !sendingSecurityHealthReport &&
                                healthIntegrityResult != null && healthReverseResult != null,
                            sending = sendingSecurityHealthReport,
                            onRefresh = { coroutineScope.launch { refreshSecurityHealth() } },
                            onSendReport = { pendingSecurityHealthReport = true },
                            onDraftChange = { updateDraftSettings(it) }
                        )
                        SecretAdminTab.Overrides -> SecretAdminSecurityOverridesCard(
                            settings = draftAdminSettings,
                            overridesActive = overridesActive,
                            onSettingsChange = { updated -> updateDraftSettings(updated) }
                        )
                        SecretAdminTab.Diagnostics -> {
                            AdminReadinessSummaryCard(
                                summary = adminReadinessSummary,
                                fieldReadinessRunning = fieldReadinessRunning,
                                webViewStatus = adminWebViewCompatibilityStatus,
                                onRunCheck = { runFieldReadinessTest() },
                                onOpenWebViewSettings = { openAdminWebViewProviderSettings() },
                                onOpenAdvanced = { advancedDiagnosticsExpanded = true }
                            )
                            AdminAdvancedDiagnosticsCard(
                                expanded = advancedDiagnosticsExpanded,
                                onToggleExpanded = {
                                    advancedDiagnosticsExpanded = !advancedDiagnosticsExpanded
                                    if (advancedDiagnosticsExpanded) {
                                        Log.i(
                                            "ExamRuntimeHardening",
                                            "code=${ExamRuntimeHardeningDiagnostics.VendorChecklistOpened} level=INFO details=vendor=${vendorChecklist.family.name}"
                                        )
                                    }
                                },
                                report = fieldReadinessReport,
                                survivalPolicy = fieldSurvivalPolicy,
                                webViewStatus = adminWebViewCompatibilityStatus,
                                vendorChecklist = vendorChecklist,
                                deviceCompatibilityProfile = deviceCompatibilityProfile,
                                onRefreshWebView = { adminWebViewRefreshKey += 1 },
                                onOpenWebViewSettings = { openAdminWebViewProviderSettings() },
                                onOpenBatterySettings = { openSettingsIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
                                onOpenLocationSettings = { openSettingsIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
                                onOpenOverlaySettings = { openOverlaySettings(context) },
                                onOpenAppSettings = {
                                    launchFirstPlatformIntentSafely(
                                        context,
                                        listOf(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${context.packageName}")
                                            ),
                                            Intent(Settings.ACTION_SETTINGS)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showApplyBar) {
            SecretAdminApplyBar(
                message = applyStatusMessage ?: when {
                    isApplyInProgress -> tr("Applying settings…", "Menerapkan pengaturan…")
                    else -> tr(
                        "Changes are not applied yet.",
                        "Perubahan belum diterapkan."
                    )
                },
                isError = applyStatusIsError,
                canApply = adminSettingsDirty,
                applying = isApplyInProgress,
                onRevert = { revertDraftSettings() },
                onApply = { applyDraftSettings() }
            )
        }
    }

    if (pendingExitConfirmation) {
        AppAlertDialog(
            tone = UiStatusTone.Warning,
            icon = Icons.Rounded.EditNote,
            title = tr("Unapplied changes", "Perubahan belum diterapkan"),
            message = tr(
                "Apply your changes before leaving, discard them, or stay on this screen.",
                "Terapkan perubahan sebelum keluar, buang perubahan, atau tetap di layar ini."
            ),
            primaryAction = AppAlertAction(
                label = tr("Apply and leave", "Terapkan lalu keluar"),
                enabled = adminSettingsDirty && !isApplyInProgress,
                onClick = {
                    pendingExitConfirmation = false
                    applyDraftSettings(exitAfterApply = true)
                }
            ),
            secondaryActions = listOf(
                AppAlertAction(
                    label = tr("Discard", "Buang perubahan"),
                    enabled = !isApplyInProgress,
                    style = AppAlertStyle.Destructive,
                    onClick = {
                        pendingExitConfirmation = false
                        exitAfterSuccessfulApply = false
                        revertDraftSettings()
                        onBack()
                    }
                ),
                AppAlertAction(
                    label = tr("Stay", "Tetap di sini"),
                    onClick = { pendingExitConfirmation = false }
                )
            ),
            dismissible = true,
            onDismissRequest = { pendingExitConfirmation = false }
        )
    }

    if (pendingSecurityHealthReport) {
        val sectionLabel = diagnosticSectionLabel(DiagnosticSection.SecurityHealth, uiLanguage)
        AppAlertDialog(
            tone = UiStatusTone.Info,
            icon = Icons.AutoMirrored.Rounded.Send,
            title = tr("Send diagnostics?", "Kirim diagnostik?"),
            message = localized(
                uiLanguage,
                "Send diagnostics for $sectionLabel to Telegram?",
                "Kirim diagnostik $sectionLabel ke Telegram?"
            ),
            primaryAction = AppAlertAction(
                label = tr("Send", "Kirim"),
                onClick = {
                    pendingSecurityHealthReport = false
                    coroutineScope.launch { sendSecurityHealthReport() }
                }
            ),
            secondaryActions = listOf(
                AppAlertAction(
                    label = tr("Cancel", "Batal"),
                    onClick = { pendingSecurityHealthReport = false }
                )
            ),
            dismissible = true,
            onDismissRequest = { pendingSecurityHealthReport = false }
        )
    }

    securityHealthFeedbackMessage?.let { message ->
        InfoDialog(
            title = securityHealthFeedbackTitle ?: "Info",
            message = message,
            onDismiss = {
                securityHealthFeedbackTitle = null
                securityHealthFeedbackMessage = null
            }
        )
    }
    }
}

@Composable
private fun SecretAdminSetupTab(
    draft: AdminSettings,
    directLinkPolicySummary: String,
    onDraftChange: (AdminSettings) -> Unit
) {
    val policySaved = draft.directLinkLocationPolicySaved
    SecretAdminSection(
        title = tr("Direct Link", "Direct Link"),
        trailing = {
            TextButton(
                onClick = {
                    onDraftChange(
                        draft.copy(
                            fastExamUrl = SecureStrings.fastExamUrl,
                            fastExamLabel = FastExamName
                        ).withoutDirectLinkLocationPolicy()
                    )
                },
                modifier = Modifier.heightIn(min = ScopedUiTokens.current.touchTarget)
            ) {
                Text(
                    text = tr("Reset to default", "Reset ke default"),
                    color = AppColors.current.brandText,
                    style = AppTextStyles.button
                )
            }
        }
    ) {
        SecretAdminTextField(
            label = tr("Exam URL", "URL ujian"),
            value = draft.fastExamUrl,
            onValueChange = {
                // A new URL invalidates the location policy saved for the old one.
                onDraftChange(draft.copy(fastExamUrl = it).withoutDirectLinkLocationPolicy())
            },
            keyboardType = KeyboardType.Uri
        )
        SecretAdminTextField(
            label = tr("Label", "Label"),
            value = draft.fastExamLabel,
            onValueChange = { onDraftChange(draft.copy(fastExamLabel = it)) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Badge on the title line; the summary gets the full width below it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Location policy", "Policy lokasi"),
                    modifier = Modifier.weight(1f),
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold)
                )
                StatusBadge(
                    label = if (policySaved) tr("Saved", "Tersimpan") else tr("None", "Belum ada"),
                    tone = if (policySaved) UiStatusTone.Success else UiStatusTone.Neutral
                )
            }
            Text(
                text = directLinkPolicySummary,
                color = AppColors.current.textSecondary,
                style = AppTextStyles.diagnostic
            )
        }
        SecretAdminNote(
            text = tr(
                "Copied only from a confirmed Custom QR. Changing the URL clears it.",
                "Hanya disalin dari Custom QR yang dikonfirmasi. Mengubah URL akan menghapusnya."
            )
        )
        if (policySaved) {
            OutlinedButton(
                onClick = { onDraftChange(draft.withoutDirectLinkLocationPolicy()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ScopedUiTokens.current.touchTarget),
                shape = RoundedCornerShape(ScopedUiTokens.current.radiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.current.brandText),
                border = BorderStroke(1.dp, AppColors.current.outline)
            ) {
                Text(text = tr("Clear location policy", "Hapus policy lokasi"), style = AppTextStyles.button)
            }
        }
    }

    SecretAdminSection(title = tr("Custom QR", "Custom QR"), padded = false) {
        SecretAdminSwitchRow(
            title = tr("Save to Direct Link", "Simpan ke Direct Link"),
            description = tr(
                "Show the save-to-Direct-Link option on Custom QR.",
                "Tampilkan pilihan simpan ke Direct Link di Custom QR."
            ),
            checked = draft.customQrSaveToDirectLinkEnabled,
            onCheckedChange = { onDraftChange(draft.copy(customQrSaveToDirectLinkEnabled = it)) }
        )
    }

    SecretAdminSection(title = tr("Exam browser & app", "Browser ujian & aplikasi")) {
        SecretAdminTextField(
            label = "User-Agent",
            value = draft.examUserAgent,
            onValueChange = { onDraftChange(draft.copy(examUserAgent = it)) },
            placeholder = DefaultExamUserAgent,
            supportingText = if (draft.usesDefaultExamUserAgent()) {
                tr("Default: $DefaultExamUserAgent", "Default: $DefaultExamUserAgent")
            } else {
                tr(
                    "Custom. Leave blank to go back to $DefaultExamUserAgent.",
                    "Custom. Kosongkan untuk kembali ke $DefaultExamUserAgent."
                )
            }
        )
        SecretAdminTextField(
            label = tr("Official APK URL", "URL APK resmi"),
            value = draft.officialApkUrl,
            onValueChange = { onDraftChange(draft.copy(officialApkUrl = it)) },
            keyboardType = KeyboardType.Uri,
            supportingText = tr(
                "Opened by \"Install official APK\" when an unofficial app is detected.",
                "Dibuka tombol \"Instal APK resmi\" saat aplikasi tidak resmi terdeteksi."
            )
        )
    }
}

@Composable
private fun SecretAdminSecurityTab(
    draft: AdminSettings,
    healthChecking: Boolean,
    integritySummary: String,
    reverseDetected: Boolean,
    deviceTimeLabel: String,
    deviceTimeStatus: DeviceTimeSecurityStatus?,
    lastCheckedAt: String?,
    sendEnabled: Boolean,
    sending: Boolean,
    onRefresh: () -> Unit,
    onSendReport: () -> Unit,
    onDraftChange: (AdminSettings) -> Unit
) {
    val tokens = ScopedUiTokens.current
    SecretAdminSection(
        title = tr("Security health", "Kesehatan keamanan"),
        trailing = {
            Box(
                modifier = Modifier.size(tokens.touchTarget),
                contentAlignment = Alignment.Center
            ) {
                if (healthChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.current.brandText
                    )
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(tokens.touchTarget)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = tr("Check again", "Cek ulang"),
                            tint = AppColors.current.brandText
                        )
                    }
                }
            }
        }
    ) {
        Column {
            SecretAdminInfoRow(
                label = "IntegrityGuard",
                value = integritySummary,
                tone = if (integritySummary == "OK") UiStatusTone.Success else UiStatusTone.Danger
            )
            SecretAdminInfoRow(
                label = "Reverse engineering",
                value = if (reverseDetected) tr("Detected", "Terdeteksi") else "OK",
                tone = if (reverseDetected) UiStatusTone.Danger else UiStatusTone.Success
            )
            SecretAdminInfoRow(
                label = tr("Device time", "Waktu perangkat"),
                value = deviceTimeLabel,
                tone = when {
                    deviceTimeStatus == null -> UiStatusTone.Neutral
                    deviceTimeStatus.blocking -> UiStatusTone.Danger
                    deviceTimeStatus.bypassActive ||
                        deviceTimeStatus.bypassState == DeviceTimeBypassState.Tampered -> UiStatusTone.Warning
                    else -> UiStatusTone.Success
                }
            )
        }
        Text(
            text = lastCheckedAt?.let { tr("Last checked $it", "Terakhir dicek $it") }
                ?: tr("Not checked yet", "Belum dicek"),
            color = AppColors.current.textMuted,
            style = AppTextStyles.diagnostic
        )
        if (LocalTelegramDiagnosticsEnabled.current) {
            OutlinedButton(
                onClick = onSendReport,
                enabled = sendEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.touchTarget),
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.current.brandText),
                border = BorderStroke(1.dp, AppColors.current.outline)
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.current.brandText
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = tr("Send to Telegram", "Kirim ke Telegram"), style = AppTextStyles.button)
            }
        }
    }

    SecretAdminSection(title = tr("Preparation & reports", "Persiapan & laporan"), padded = false) {
        SecretAdminSwitchRow(
            title = tr("Technical checklist details", "Detail teknis checklist"),
            description = tr(
                "Show package names and raw values in each preparation category.",
                "Tampilkan nama paket dan nilai mentah di detail kategori persiapan."
            ),
            checked = draft.showChecklistDetails,
            onCheckedChange = { onDraftChange(draft.copy(showChecklistDetails = it)) }
        )
        SecretAdminRowDivider()
        SecretAdminSwitchRow(
            title = tr("Telegram diagnostics", "Diagnostik Telegram"),
            description = if (BuildConfig.REMOTE_DIAGNOSTICS_ENABLED) {
                tr(
                    "When off, every \"Send to Telegram\" button is hidden.",
                    "Jika mati, semua tombol \"Kirim ke Telegram\" disembunyikan."
                )
            } else {
                tr(
                    "This build has no Telegram diagnostics; sending stays unavailable.",
                    "Build ini tanpa diagnostik Telegram; pengiriman tetap tidak tersedia."
                )
            },
            checked = draft.telegramDiagnosticsEnabled,
            onCheckedChange = { onDraftChange(draft.copy(telegramDiagnosticsEnabled = it)) }
        )
    }
}
