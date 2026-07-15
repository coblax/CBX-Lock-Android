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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.webkit.WebViewCompat

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
import com.coblax.examlock.ui.exam.ExamRuntimeHardeningDiagnostics
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockDialogDangerIcon
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.WebViewHealthSeverity
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineStrong
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place

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
    onSelectedTabNameChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val lowRamProfile = LocalLowRamProfile.current
    val deviceCompatibilityProfile = LocalDeviceCompatibilityProfile.current
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
    val usesDefaultExamUserAgent = remember(settings.examUserAgent) {
        debugMeasureSecretAdminWork("usesDefaultExamUserAgent") {
            settings.usesDefaultExamUserAgent()
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
    val examUserAgentSourceLabel = remember(usesDefaultExamUserAgent, uiLanguage) {
        if (usesDefaultExamUserAgent) {
            localized(uiLanguage, "Default", "Default")
        } else {
            localized(uiLanguage, "Custom", "Custom")
        }
    }
    val overridesActive = remember(
        settings.bypassScreenPinning,
        settings.bypassBluetooth,
        settings.bypassAccessibility,
        settings.bypassAdb,
        settings.bypassRoot,
        settings.bypassVirtualEnvironment,
        settings.bypassVpn,
        settings.bypassKeyboardPolicy,
        settings.bypassClipboard,
        settings.bypassOverlay,
        settings.bypassGeofence,
        settings.bypassFakeLocation,
        settings.bypassDeviceTime,
        settings.bypassAppSwitch
    ) {
        debugMeasureSecretAdminWork("hasAnyBypass") {
            settings.hasAnyBypass()
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
        settings.directLinkLocationPolicySaved,
        settings.directLinkLocationPolicySerialized,
        settings.directLinkGeofenceEnabled,
        settings.directLinkGeofenceCenterLat,
        settings.directLinkGeofenceCenterLng,
        settings.directLinkGeofenceRadiusMeters,
        uiLanguage
    ) {
        debugMeasureSecretAdminWork("directLinkPolicySummary") {
            if (settings.directLinkLocationPolicySaved) {
                when (val directLinkPolicy = settings.directLinkLocationPolicy()) {
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
        debugMeasureSecretAdminWork("selectedSecretAdminTab") {
            runCatching {
                SecretAdminTab.valueOf(selectedTabName)
            }.getOrDefault(SecretAdminTab.Setup)
        }
    }
    val scrollState = rememberScrollState()

    DebugSecretAdminRecomposeTrace(selectedSecretAdminTab)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackPillButton(onClick = onBack)

            Surface(
                shape = RoundedCornerShape(UiTokens.RadiusPill),
                color = LockBlueDeep
            ) {
                Text(
                    text = "ADMIN",
                    color = LockOnDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tr("Secret Admin", "Admin Rahasia"),
            color = LockTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tr(
                "Direct link & security overrides",
                "Direct link & override keamanan"
            ),
            color = LockTextSecondary,
            fontSize = 13.sp
        )

        if (settings.bypassMigrationResetNotice) {
            Spacer(modifier = Modifier.height(14.dp))
            StatusBanner(
                message = tr(
                    "Security storage was upgraded. Existing bypasses were reset to safe OFF and must be re-enabled manually.",
                    "Penyimpanan keamanan telah ditingkatkan. Semua bypass lama direset ke OFF aman dan harus diaktifkan ulang secara manual."
                ),
                isError = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SecretAdminTabSelector(
            selectedTab = selectedSecretAdminTab,
            onTabSelected = { onSelectedTabNameChange(it.name) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (selectedSecretAdminTab == SecretAdminTab.Setup) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.RadiusLg))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusLg))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Text(
                    text = tr("Direct Link", "Direct Link"),
                    color = LockTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                AdminInputField(
                    value = settings.fastExamUrl,
                    onValueChange = {
                        onSettingsChange(settings.copy(fastExamUrl = it).withoutDirectLinkLocationPolicy())
                    },
                    placeholder = tr("Direct link URL", "URL Direct Link"),
                    keyboardType = KeyboardType.Uri
                )
                AdminInputField(
                    value = settings.fastExamLabel,
                    onValueChange = { onSettingsChange(settings.copy(fastExamLabel = it)) },
                    placeholder = tr("Direct link label", "Label Direct Link")
                )
                Text(
                    text = directLinkPolicySummary,
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = tr("Official APK URL", "URL APK Resmi"),
                    color = LockTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AdminInputField(
                    value = settings.officialApkUrl,
                    onValueChange = { onSettingsChange(settings.copy(officialApkUrl = it)) },
                    placeholder = tr("Official APK download URL", "URL unduhan APK resmi"),
                    keyboardType = KeyboardType.Uri
                )
                Text(
                    text = tr("WebView User-Agent", "User-Agent WebView"),
                    color = LockTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AdminInputField(
                    value = settings.examUserAgent,
                    onValueChange = { onSettingsChange(settings.copy(examUserAgent = it)) },
                    placeholder = DefaultExamUserAgent
                )
                Text(
                    text = localized(
                        uiLanguage,
                        "Used by the internal exam browser. Leave blank to reset to $DefaultExamUserAgent.",
                        "Dipakai oleh browser ujian internal. Kosongkan untuk kembali ke $DefaultExamUserAgent."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Active User-Agent", "User-Agent Aktif"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = settings.effectiveExamUserAgent(),
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("User-Agent source", "Sumber User-Agent"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (usesDefaultExamUserAgent) {
                            tr("Default", "Default")
                        } else {
                            tr("Custom", "Custom")
                        },
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AdminToggleRow(
                    title = tr(
                        "Enable Save to Direct Link in Custom QR",
                        "Aktifkan Simpan ke Direct Link di Custom QR"
                    ),
                    description = tr(
                        "Show the save-to-direct-link checkbox on Custom QR.",
                        "Tampilkan checkbox simpan ke Direct Link di Custom QR."
                    ),
                    checked = settings.customQrSaveToDirectLinkEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(customQrSaveToDirectLinkEnabled = it))
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onResetDirectLink) {
                        Text(tr("Reset to default", "Reset ke default"), color = LockBlue)
                    }
                }
        }

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Security) {
        AdminReadinessSummaryCard(
            summary = adminReadinessSummary,
            fieldReadinessRunning = fieldReadinessRunning,
            webViewStatus = adminWebViewCompatibilityStatus,
            onRunCheck = ::runFieldReadinessTest,
            onOpenWebViewSettings = ::openAdminWebViewProviderSettings,
            onOpenAdvanced = { advancedDiagnosticsExpanded = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

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
            onOpenWebViewSettings = ::openAdminWebViewProviderSettings,
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

        Spacer(modifier = Modifier.height(18.dp))

        SecretAdminSecurityOverridesCard(
            settings = settings,
            overridesActive = overridesActive,
            onSettingsChange = onSettingsChange
        )

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Security) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Checklist Details", "Detail Checklist"),
                    color = LockTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                AdminToggleRow(
                    title = tr("Show Checklist Details", "Tampilkan Detail Checklist"),
                    description = tr(
                        "Show the full technical checks on the preparation checklist.",
                        "Tampilkan detail teknis pemeriksaan di checklist persiapan."
                    ),
                    checked = settings.showChecklistDetails,
                    onCheckedChange = { onSettingsChange(settings.copy(showChecklistDetails = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Setup) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Security Health", "Kesehatan Keamanan"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (healthChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LockBlue
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IntegrityGuard",
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthIntegritySummary.ifBlank { "-" },
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reverse Engineering",
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (healthReverseDetected) {
                            tr("Detected", "Terdeteksi")
                        } else {
                            "OK"
                        },
                        color = if (healthReverseDetected) LockDialogDangerIcon else LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Device Time", "Waktu Perangkat"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthDeviceTimeLabel,
                        color = if (healthDeviceTimeStatus?.blocking == true) LockDialogDangerIcon else LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Last checked", "Terakhir dicek"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthLastCheckedAt ?: "-",
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Exam User-Agent", "User-Agent Ujian"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = effectiveExamUserAgent,
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("User-Agent source", "Sumber User-Agent"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = examUserAgentSourceLabel,
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val securityHealthSendEnabled =
                    !healthChecking && !sendingSecurityHealthReport &&
                        healthIntegrityResult != null && healthReverseResult != null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { coroutineScope.launch { refreshSecurityHealth() } },
                        enabled = !healthChecking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(UiTokens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark
                        )
                    ) {
                        Text(
                            text = tr("Refresh", "Refresh"),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { pendingSecurityHealthReport = true },
                        enabled = securityHealthSendEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(UiTokens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2AABEE),
                            contentColor = LockOnDark,
                            disabledContainerColor = Color(0xFFB5DDF3),
                            disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                        )
                    ) {
                        if (sendingSecurityHealthReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LockOnDark
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = tr(
                                    "Send Security Health diagnostics to Telegram",
                                    "Kirim diagnostik Security Health ke Telegram"
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Telegram",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (pendingSecurityHealthReport) {
        val sectionLabel = diagnosticSectionLabel(DiagnosticSection.SecurityHealth, uiLanguage)
        AlertDialog(
            onDismissRequest = { pendingSecurityHealthReport = false },
            title = { Text(tr("Send diagnostics?", "Kirim diagnostik?")) },
            text = {
                Text(
                    text = localized(
                        uiLanguage,
                        "Send diagnostics for $sectionLabel to Telegram?",
                        "Kirim diagnostik $sectionLabel ke Telegram?"
                    ),
                    color = LockTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSecurityHealthReport = false
                        coroutineScope.launch { sendSecurityHealthReport() }
                    }
                ) {
                    Text(tr("Send", "Kirim"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSecurityHealthReport = false }) {
                    Text(tr("Cancel", "Batal"))
                }
            }
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
