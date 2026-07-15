package com.coblax.examlock.ui.app

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider

import com.coblax.examlock.AdminAuth
import com.coblax.examlock.AdminAuthSession
import com.coblax.examlock.applyLowRamProfileOverride
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.captureDeviceTimeBaseline
import com.coblax.examlock.config.FastExamName
import com.coblax.examlock.config.SecretTapWindowMs
import com.coblax.examlock.currentDeviceCompatibilityProfile
import com.coblax.examlock.DeviceTimeBaseline
import com.coblax.examlock.DeviceTimeBypassResolver
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DeviceTimeSecurityVerdict
import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.ExamScheduleDefaults
import com.coblax.examlock.ExamScheduleValidationResult
import com.coblax.examlock.ExamScheduleValidator
import com.coblax.examlock.ExamUrlValidationError
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.inspectDeviceTimeSecurity
import com.coblax.examlock.LocalDeviceCompatibilityProfile
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.LowRamProfileOverride
import com.coblax.examlock.MemoryPressureCoordinator
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.AppScreen
import com.coblax.examlock.model.directLinkLocationPolicy
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.model.withDirectLinkLocationPolicy
import com.coblax.examlock.persistence.HomeAdminSettings
import com.coblax.examlock.persistence.readAdminSettings
import com.coblax.examlock.persistence.readHomeAdminSettings
import com.coblax.examlock.persistence.readSavedUiLanguage
import com.coblax.examlock.persistence.saveAdminSettings
import com.coblax.examlock.persistence.saveUiLanguage
import com.coblax.examlock.resolveDetectedLowRamProfile
import com.coblax.examlock.resolveLowRamProfile
import com.coblax.examlock.resolveRuntimePressureProfile
import com.coblax.examlock.runtime.SecurityDetectorCache
import com.coblax.examlock.save.ExamQrPayloadSaver
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.TrustedNetworkTimeCoordinator
import com.coblax.examlock.ui.admin.AdminPasswordDialog
import com.coblax.examlock.ui.admin.ExamLockHomeScreen
import com.coblax.examlock.ui.admin.ExamLockLowRamHomeScreen
import com.coblax.examlock.ui.admin.InfoDialog
import com.coblax.examlock.ui.admin.PublicPerformanceProfileDialog
import com.coblax.examlock.ui.admin.ScanSourceDialog
import com.coblax.examlock.ui.exam.ExamRuntimeHardeningDiagnostics
import com.coblax.examlock.ui.exam.ExamRuntimeHardeningLogTag
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockDangerBgSubtle
import com.coblax.examlock.ui.theme.LockDialogDangerIcon
import com.coblax.examlock.ui.theme.LockSafeStrong
import com.coblax.examlock.validateExamUrl
import com.coblax.examlock.viewmodel.AdminFlowUiAction
import com.coblax.examlock.viewmodel.AdminFlowUiState
import com.coblax.examlock.viewmodel.AdminFlowViewModel
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockBlueFill
import com.coblax.examlock.ui.theme.LockBlueTint
import com.coblax.examlock.ui.theme.LockOutlineStrong

import java.net.URL
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AdminSettingsPerfTag = "AdminSettingsPerf"
private const val LowRamProfilePerfTag = "LowRamProfile"

private enum class AppRecoveryRoute {
    Home,
    ExamFlowPreparation,
    ExamFlowRuntime,
    CustomQrAdmin,
    SecretAdmin
}

internal enum class PendingHomeAction {
    RuntimeHome,
    ScanExam,
    CustomQrAdmin,
    DirectLink,
    SecretAdmin
}

private fun parseAppRecoveryRoute(rawValue: String?): AppRecoveryRoute =
    rawValue
        ?.let { value -> runCatching { AppRecoveryRoute.valueOf(value) }.getOrNull() }
        ?: AppRecoveryRoute.Home

private fun readLowRamRuntimeMemoryInfo(context: Context): ActivityManager.MemoryInfo? {
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
        ?: return null
    return ActivityManager.MemoryInfo().also { info ->
        runCatching { activityManager.getMemoryInfo(info) }
    }
}

private fun isFreshAdminFlowUiState(uiState: AdminFlowUiState): Boolean = uiState == AdminFlowUiState()

private fun detectProcessDeathRecovery(
    shellStateRestored: Boolean,
    currentViewModelInstanceId: String,
    savedViewModelInstanceId: String,
    uiState: AdminFlowUiState,
    routeSnapshot: AppRecoveryRoute,
    activeExamPayload: ExamQrPayload?
): Boolean {
    if (!shellStateRestored) {
        return false
    }
    if (savedViewModelInstanceId == currentViewModelInstanceId) {
        return false
    }
    if (!isFreshAdminFlowUiState(uiState)) {
        return false
    }
    return routeSnapshot != AppRecoveryRoute.Home || activeExamPayload != null
}

private fun currentDeviceTimeBypassState(settings: AdminSettings): DeviceTimeBypassState {
    return DeviceTimeBypassResolver.stateOf(
        enabled = settings.bypassDeviceTime,
        tampered = settings.deviceTimeBypassTampered
    )
}

private fun deviceTimeQrBlockMessage(
    status: DeviceTimeSecurityStatus,
    uiLanguage: UiLanguage
): String {
    return when {
        status.bypassState == DeviceTimeBypassState.Tampered -> localized(
            uiLanguage,
            "Device Time bypass storage was tampered with. Device Time enforcement remains active.",
            "Tamper terdeteksi pada storage bypass Waktu Perangkat. Enforcement Waktu Perangkat tetap aktif."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> localized(
            uiLanguage,
            "Turn on automatic date & time before opening this QR.",
            "Aktifkan tanggal & waktu otomatis sebelum membuka QR ini."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> localized(
            uiLanguage,
            "Turn on automatic time zone before opening this QR.",
            "Aktifkan zona waktu otomatis sebelum membuka QR ini."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> localized(
            uiLanguage,
            "A suspicious clock change was detected. Turn automatic date & time back on, then scan again.",
            "Terdeteksi perubahan jam yang mencurigakan. Aktifkan kembali tanggal & waktu otomatis, lalu pindai lagi."
        )
        else -> localized(
            uiLanguage,
            "Device time could not be trusted. Check the date & time settings, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Periksa pengaturan tanggal & waktu, lalu coba lagi."
        )
    }
}

private fun deviceTimeEventDetails(status: DeviceTimeSecurityStatus, trigger: String): String {
    return buildString {
        append("trigger=")
        append(trigger)
        append(" | verdict=")
        append(status.finalVerdict.name.lowercase(Locale.US))
        append(" | auto_time=")
        append(if (status.autoTimeEnabled) "on" else "off")
        append(" | auto_time_zone=")
        append(if (status.autoTimeZoneEnabled) "on" else "off")
        append(" | drift_ms=")
        append(status.clockDriftMillis)
        append(" | timezone=")
        append(status.timezoneSummary)
        append(" | bypass=")
        append(status.bypassState.name.lowercase(Locale.US))
    }
}

@Composable
internal fun AppHostRuntimeContent(
    initialUiLanguageOverride: UiLanguage? = null,
    initialHomeAdminSettings: HomeAdminSettings? = null,
    initialLowRamProfile: LowRamProfile? = null,
    initialHomeAction: PendingHomeAction? = null,
    onInitialHomeActionConsumed: () -> Unit = {}
) {
    remember {
        StartupTrace.mark("app_runtime_content_start")
        true
    }
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()
    val detectedLowRamProfile = remember(context) {
        resolveDetectedLowRamProfile(context)
    }
    var lowRamProfile by remember(context, initialLowRamProfile) {
        mutableStateOf(initialLowRamProfile ?: resolveLowRamProfile(context))
    }
    LaunchedEffect(lowRamProfile) {
        applyLowRamRuntimeDetectorBudget(lowRamProfile)
    }
    val deviceCompatibilityProfile = remember(lowRamProfile) {
        currentDeviceCompatibilityProfile(lowRamProfile)
    }
    val adminFlowViewModel = remember(activity) {
        ViewModelProvider(activity)[AdminFlowViewModel::class.java]
    }
    val adminFlowUiState by adminFlowViewModel.uiState.collectAsState()
    val initialUiLanguage = remember(initialUiLanguageOverride) {
        initialUiLanguageOverride ?: context.readSavedUiLanguage()
    }
    var persistedUiLanguage by remember { mutableStateOf(initialUiLanguage) }
    var uiLanguage by rememberSaveable { mutableStateOf(initialUiLanguage) }
    var homeAdminSettings by remember(initialHomeAdminSettings) {
        mutableStateOf(initialHomeAdminSettings ?: HomeAdminSettings())
    }
    var adminSettings by remember { mutableStateOf<AdminSettings?>(null) }
    var showDeferredHomeChrome by rememberSaveable { mutableStateOf(!lowRamProfile.severe) }
    var showPerformanceProfileDialog by rememberSaveable { mutableStateOf(false) }
    val adminSettingsSaveRequests = remember { Channel<AdminSettings>(capacity = Channel.CONFLATED) }
    var activeExamPayload by rememberSaveable(stateSaver = ExamQrPayloadSaver) {
        mutableStateOf(null as ExamQrPayload?)
    }
    var pendingScanConfirmPayload by remember { mutableStateOf<ExamQrPayload?>(null) }
    var pendingScanConfirmError by remember { mutableStateOf<String?>(null) }
    var pendingScanConfirmInFlight by remember { mutableStateOf(false) }
    var pendingDirectLinkSaveLog by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryEventDetails by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryNoticeTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryNoticeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var savedRouteSnapshotRaw by rememberSaveable { mutableStateOf(AppRecoveryRoute.Home.name) }
    var examSessionRecoveryNonce by rememberSaveable { mutableLongStateOf(0L) }
    val shellInstanceId = remember { "shell-${SystemClock.elapsedRealtimeNanos()}" }
    var savedShellInstanceId by rememberSaveable { mutableStateOf(shellInstanceId) }
    var savedViewModelInstanceId by rememberSaveable { mutableStateOf(adminFlowViewModel.instanceId) }
    var secretTapCount by rememberSaveable { mutableIntStateOf(0) }
    var lastSecretTapAt by rememberSaveable { mutableLongStateOf(0L) }
    var homeDeferredChromeMarked by remember { mutableStateOf(false) }
    val deviceTimeBaseline = remember { captureDeviceTimeBaseline() }
    val savedRouteSnapshot = parseAppRecoveryRoute(savedRouteSnapshotRaw)
    val latestCurrentScreen by rememberUpdatedState(adminFlowUiState.currentScreen)
    val processDeathRecoveryPending = detectProcessDeathRecovery(
        shellStateRestored = savedShellInstanceId != shellInstanceId,
        currentViewModelInstanceId = adminFlowViewModel.instanceId,
        savedViewModelInstanceId = savedViewModelInstanceId,
        uiState = adminFlowUiState,
        routeSnapshot = savedRouteSnapshot,
        activeExamPayload = activeExamPayload
    )

    val directLinkLabel = homeAdminSettings.fastExamLabel.trim().ifBlank { FastExamName }
    val directLinkUrl = homeAdminSettings.fastExamUrl.trim().ifBlank { SecureStrings.fastExamUrl }
    val latestLowRamProfile by rememberUpdatedState(lowRamProfile)

    fun cacheAdminSettings(loaded: AdminSettings): AdminSettings {
        adminSettings = loaded
        lowRamProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = loaded.lowRamProfileOverride
        )
        applyLowRamRuntimeDetectorBudget(lowRamProfile)
        homeAdminSettings = HomeAdminSettings(
            fastExamUrl = loaded.fastExamUrl,
            fastExamLabel = loaded.fastExamLabel
        )
        return loaded
    }

    suspend fun loadCurrentAdminSettings(): AdminSettings {
        adminSettings?.let { cached -> return cached }
        val startedAt = SystemClock.elapsedRealtime()
        StartupTrace.mark("admin_settings_load_start", "thread=io")
        val loaded = withContext(Dispatchers.IO) {
            context.readAdminSettings()
        }
        StartupTrace.mark(
            "admin_settings_loaded",
            "duration_ms=${SystemClock.elapsedRealtime() - startedAt}"
        )
        return cacheAdminSettings(loaded)
    }

    fun activeAdminSettingsSnapshot(): AdminSettings {
        return adminSettings ?: run {
            StartupTrace.mark("admin_settings_sync_fallback", "screen=${adminFlowUiState.currentScreen.name}")
            cacheAdminSettings(context.readAdminSettings())
        }
    }

    fun updateAdminSettings(updated: AdminSettings) {
        val normalized = updated.copy(examUserAgent = updated.effectiveExamUserAgent())
        adminSettings = normalized
        val updatedProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = normalized.lowRamProfileOverride
        )
        lowRamProfile = updatedProfile
        applyLowRamRuntimeDetectorBudget(updatedProfile)
        homeAdminSettings = HomeAdminSettings(
            fastExamUrl = normalized.fastExamUrl,
            fastExamLabel = normalized.fastExamLabel
        )
        val sendResult = adminSettingsSaveRequests.trySend(normalized)
        if (BuildConfig.DEBUG && sendResult.isFailure) {
            Log.d(AdminSettingsPerfTag, "Admin settings save request was dropped before enqueue.")
        }
    }

    fun updateLowRamProfileOverride(override: LowRamProfileOverride) {
        val updatedProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = override
        )
        lowRamProfile = updatedProfile
        applyLowRamRuntimeDetectorBudget(updatedProfile)
        adminSettings?.let { cachedSettings ->
            updateAdminSettings(cachedSettings.copy(lowRamProfileOverride = override))
            return
        }
        coroutineScope.launch {
            val currentSettings = withContext(Dispatchers.IO) {
                context.readAdminSettings()
            }
            updateAdminSettings(currentSettings.copy(lowRamProfileOverride = override))
        }
    }

    suspend fun persistAdminSettingsImmediately(updated: AdminSettings): AdminSettings {
        val normalized = updated.copy(examUserAgent = updated.effectiveExamUserAgent())
        val refreshed = withContext(Dispatchers.IO) {
            context.saveAdminSettings(normalized)
            context.readAdminSettings()
        }
        return cacheAdminSettings(refreshed)
    }

    fun invalidExamUrlMessage(error: ExamUrlValidationError?): String {
        return when (error) {
            ExamUrlValidationError.Blank -> localized(
                uiLanguage,
                "Exam URL is required.",
                "URL ujian wajib diisi."
            )

            ExamUrlValidationError.Invalid,
            null -> localized(
                uiLanguage,
                "Exam URL must start with https:// and include a domain.",
                "URL ujian harus diawali https:// dan memiliki domain."
            )
        }
    }

    fun directLinkSavedFromQrLog(
        payload: ExamQrPayload,
        normalizedExamUrl: String,
        updatedLabel: String,
        savedLocationPolicy: ExamQrLocationPolicy
    ): String {
        return "url=$normalizedExamUrl | label=$updatedLabel | geofence_shape=${
            savedLocationPolicy.shapeType.name.lowercase(Locale.US)
        } | polygon_points=${savedLocationPolicy.vertices.size} | circle_centers=${
            savedLocationPolicy.effectiveCircleCenters.size
        } | center=${
            savedLocationPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                "${center.latitude.ifBlank { "-" }},${center.longitude.ifBlank { "-" }}"
            } ?: "${savedLocationPolicy.centerLat.ifBlank { "-" }},${savedLocationPolicy.centerLng.ifBlank { "-" }}"
        } | radius_m=${
            savedLocationPolicy.radiusMeters.ifBlank { "-" }
        } | exam=${payload.examName.trim().ifBlank { FastExamName }}"
    }

    suspend fun saveDirectLinkFromConfirmedQr(
        payload: ExamQrPayload,
        normalizedExamUrl: String
    ): String {
        val activeSettings = loadCurrentAdminSettings()
        val updatedLabel = payload.examName.trim().ifBlank { FastExamName }
        val savedLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
        persistAdminSettingsImmediately(
            activeSettings.copy(
                fastExamUrl = normalizedExamUrl,
                fastExamLabel = updatedLabel
            ).withDirectLinkLocationPolicy(savedLocationPolicy)
        )
        return directLinkSavedFromQrLog(
            payload = payload,
            normalizedExamUrl = normalizedExamUrl,
            updatedLabel = updatedLabel,
            savedLocationPolicy = savedLocationPolicy
        )
    }

    fun confirmPendingScanPayload(payload: ExamQrPayload) {
        if (pendingScanConfirmInFlight) {
            return
        }
        coroutineScope.launch {
            val examUrlValidation = validateExamUrl(payload.examUrl)
            val normalizedExamUrl = examUrlValidation.normalizedUrl
            if (normalizedExamUrl == null) {
                pendingScanConfirmError = invalidExamUrlMessage(examUrlValidation.error)
                return@launch
            }

            pendingScanConfirmInFlight = true
            pendingScanConfirmError = null
            try {
                val normalizedPayload = payload.copy(examUrl = normalizedExamUrl)
                if (normalizedPayload.saveToDirectLink) {
                    pendingDirectLinkSaveLog = saveDirectLinkFromConfirmedQr(
                        payload = normalizedPayload,
                        normalizedExamUrl = normalizedExamUrl
                    )
                }
                activeExamPayload = normalizedPayload
                pendingScanConfirmPayload = null
                pendingScanConfirmError = null
                savedRouteSnapshotRaw = AppRecoveryRoute.ExamFlowPreparation.name
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
            } catch (throwable: Throwable) {
                pendingScanConfirmError = throwable.message ?: localized(
                    uiLanguage,
                    "The QR could not be opened.",
                    "QR tidak dapat dibuka."
                )
            } finally {
                pendingScanConfirmInFlight = false
            }
        }
    }

    LaunchedEffect(context) {
        withFrameNanos { }
        homeAdminSettings = withContext(Dispatchers.IO) {
            context.readHomeAdminSettings()
        }
        StartupTrace.mark(
            "home_settings_loaded",
            "direct_link_label=${homeAdminSettings.fastExamLabel.trim().ifBlank { FastExamName }}"
        )
    }

    LaunchedEffect(lowRamProfile.severe, adminFlowUiState.currentScreen, showDeferredHomeChrome) {
        if (!lowRamProfile.severe) {
            showDeferredHomeChrome = true
            if (adminFlowUiState.currentScreen == AppScreen.Home && !homeDeferredChromeMarked) {
                homeDeferredChromeMarked = true
                StartupTrace.mark("home_deferred_chrome_shown", "mode=normal")
            }
            return@LaunchedEffect
        }
        if (adminFlowUiState.currentScreen == AppScreen.Home && !showDeferredHomeChrome) {
            withFrameNanos { }
            delay(750)
            showDeferredHomeChrome = true
            if (!homeDeferredChromeMarked) {
                homeDeferredChromeMarked = true
                StartupTrace.mark("home_deferred_chrome_shown", "mode=severe")
            }
        }
    }

    DisposableEffect(context) {
        val listener: (Int) -> Unit = { level ->
            val baseProfile = latestLowRamProfile
            val memoryInfo = readLowRamRuntimeMemoryInfo(context)
            val escalatedProfile = resolveRuntimePressureProfile(
                baseProfile = baseProfile,
                trimLevel = level,
                availableMemoryBytes = memoryInfo?.availMem,
                memoryLow = memoryInfo?.lowMemory == true
            )
            if (escalatedProfile != baseProfile) {
                lowRamProfile = escalatedProfile
                applyLowRamRuntimeDetectorBudget(escalatedProfile)
                SecurityDetectorCache.invalidateStaticSecurity()
                Log.i(
                    ExamRuntimeHardeningLogTag,
                    "code=LOW_RAM_RUNTIME_ESCALATED level=INFO details=trim=$level " +
                        "| avail=${escalatedProfile.availableMemoryMb ?: "-"}MB " +
                        "| detectorCacheMax=${escalatedProfile.detectorMetadataCacheMaxEntries}"
                )
            }
            val effectiveProfile = if (escalatedProfile != baseProfile) escalatedProfile else baseProfile
            if (
                effectiveProfile.enabled &&
                latestCurrentScreen == AppScreen.Home &&
                MemoryPressureCoordinator.shouldReleaseUiBitmaps(level)
            ) {
                showDeferredHomeChrome = false
                Log.i("HomeMemory", "trim=$level action=hide_deferred_home_chrome")
            }
        }
        MemoryPressureCoordinator.addListener(listener)
        onDispose {
            MemoryPressureCoordinator.removeListener(listener)
        }
    }

    LaunchedEffect(context) {
        for (pendingSettings in adminSettingsSaveRequests) {
            val startedAt = SystemClock.elapsedRealtime()
            val refreshed = withContext(Dispatchers.IO) {
                val saveStartedAt = SystemClock.elapsedRealtime()
                context.saveAdminSettings(pendingSettings)
                val saveFinishedAt = SystemClock.elapsedRealtime()
                val refreshedSettings = context.readAdminSettings()
                Triple(refreshedSettings, saveFinishedAt - saveStartedAt, SystemClock.elapsedRealtime() - saveFinishedAt)
            }
            if (BuildConfig.DEBUG) {
                Log.d(
                    AdminSettingsPerfTag,
                    "Admin settings persisted in ${refreshed.second} ms and reloaded in ${refreshed.third} ms (total ${SystemClock.elapsedRealtime() - startedAt} ms)"
                )
            }
            if (adminSettings == pendingSettings) {
                adminSettings = refreshed.first
            }
        }
    }

    DisposableEffect(adminSettingsSaveRequests) {
        onDispose {
            adminSettingsSaveRequests.close()
        }
    }

    fun registerSecretTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSecretTapAt > SecretTapWindowMs) {
            secretTapCount = 0
        }
        lastSecretTapAt = now
        secretTapCount += 1
        if (secretTapCount >= 4) {
            secretTapCount = 0
            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
            adminFlowViewModel.dispatch(AdminFlowUiAction.ShowAdminPasswordDialog)
        }
    }

    fun queueRecoveryNotice(title: String, message: String) {
        pendingRecoveryNoticeTitle = title
        pendingRecoveryNoticeMessage = message
    }

    LaunchedEffect(uiLanguage) {
        if (uiLanguage != persistedUiLanguage) {
            context.saveUiLanguage(uiLanguage)
            persistedUiLanguage = uiLanguage
        }
    }

    LaunchedEffect(lowRamProfile) {
        Log.i(
            LowRamProfilePerfTag,
            lowRamProfile.diagnosticSummary()
        )
    }

    LaunchedEffect(deviceCompatibilityProfile) {
        Log.i(
            ExamRuntimeHardeningLogTag,
            "code=${ExamRuntimeHardeningDiagnostics.DeviceCompatProfileResolved} " +
                "level=INFO details=${deviceCompatibilityProfile.diagnosticSummary()}"
        )
    }

    LaunchedEffect(
        shellInstanceId,
        adminFlowViewModel.instanceId,
        adminFlowUiState,
        savedRouteSnapshotRaw,
        activeExamPayload,
        uiLanguage
    ) {
        if (processDeathRecoveryPending) {
            when (savedRouteSnapshot) {
                AppRecoveryRoute.ExamFlowPreparation,
                AppRecoveryRoute.ExamFlowRuntime -> {
                    val payload = activeExamPayload
                    val recoveryAdminSettings = payload?.let {
                        loadCurrentAdminSettings()
                    }
                    val recoveryDeviceTimeStatus = if (payload != null && recoveryAdminSettings != null) {
                        inspectDeviceTimeSecurity(
                            context = context,
                            baseline = deviceTimeBaseline,
                            bypassState = currentDeviceTimeBypassState(recoveryAdminSettings)
                        )
                    } else {
                        null
                    }
                    val validationResult = if (payload != null && recoveryDeviceTimeStatus != null) {
                        val recoveryNetworkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                        ExamScheduleValidator.validateAfterDeviceTimeCheck(
                            payload = payload,
                            deviceTimeStatus = recoveryDeviceTimeStatus,
                            networkNowMillis = recoveryNetworkNowMillis
                        )
                    } else {
                        null
                    }
                    if (payload != null && validationResult == ExamScheduleValidationResult.Valid) {
                        val previousRoute = savedRouteSnapshot.name
                        examSessionRecoveryNonce = SystemClock.elapsedRealtime()
                        pendingRecoveryEventDetails =
                            "route=$previousRoute | payload_restored=yes | validation=valid"
                        queueRecoveryNotice(
                            title = localized(uiLanguage, "Session Restored", "Sesi Dipulihkan"),
                            message = localized(
                                uiLanguage,
                                "The app was restarted by Android. Review the checks and start the exam again.",
                                "Aplikasi dimulai ulang oleh Android. Periksa kembali pengecekan lalu mulai ujian lagi."
                            )
                        )
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
                    } else {
                        activeExamPayload = null
                        queueRecoveryNotice(
                            title = localized(uiLanguage, "Session Unavailable", "Sesi Tidak Tersedia"),
                            message = when (validationResult) {
                                ExamScheduleValidationResult.NotStarted -> localized(
                                    uiLanguage,
                                    "The saved exam session is not active yet. Please scan or open the exam again later.",
                                    "Sesi ujian yang tersimpan belum aktif. Silakan scan atau buka lagi nanti."
                                )

                                ExamScheduleValidationResult.Finished -> localized(
                                    uiLanguage,
                                    "The saved exam session has expired. Please scan or open a valid exam again.",
                                    "Sesi ujian yang tersimpan sudah berakhir. Silakan scan atau buka lagi ujian yang masih valid."
                                )

                                ExamScheduleValidationResult.InvalidSchedule -> localized(
                                    uiLanguage,
                                    "The saved exam session is no longer valid because its schedule is invalid.",
                                    "Sesi ujian yang tersimpan tidak lagi valid karena jadwalnya tidak valid."
                                )

                                ExamScheduleValidationResult.TimeSpoofDetected -> localized(
                                    uiLanguage,
                                    "The saved exam session could not be restored because the device time could not be trusted. Check automatic date, time, and time zone, then scan again.",
                                    "Sesi ujian yang tersimpan tidak dapat dipulihkan karena waktu perangkat tidak dapat dipercaya. Periksa tanggal, waktu, dan zona waktu otomatis, lalu scan ulang."
                                )

                                else -> localized(
                                    uiLanguage,
                                    "The saved exam session could not be restored. Please scan or open the exam again.",
                                    "Sesi ujian yang tersimpan tidak dapat dipulihkan. Silakan scan atau buka lagi ujian."
                                )
                            }
                        )
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                    }
                }

                AppRecoveryRoute.CustomQrAdmin,
                AppRecoveryRoute.SecretAdmin -> {
                    AdminAuthSession.clear()
                    activeExamPayload = null
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }

                AppRecoveryRoute.Home -> {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }
            }
        }
        savedShellInstanceId = shellInstanceId
        savedViewModelInstanceId = adminFlowViewModel.instanceId
    }

    fun handleExamQrRawPayload(rawPayload: String) {
        coroutineScope.launch {
            val payload = runCatching {
                withContext(Dispatchers.Default) {
                    ExamQrCodec.decrypt(rawPayload)
                }
            }.getOrElse {
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        it.message ?: localized(
                            uiLanguage,
                            "The QR code could not be read.",
                            "QR tidak dapat dibaca."
                        )
                    )
                )
                return@launch
            }

            try {
                val activeSettings = loadCurrentAdminSettings()
                val deviceTimeStatus = inspectDeviceTimeSecurity(
                    context = context,
                    baseline = deviceTimeBaseline,
                    bypassState = currentDeviceTimeBypassState(activeSettings)
                )
                if (deviceTimeStatus.blocking) {
                    Log.w(
                        "AppHostDeviceTime",
                        "QR_BLOCKED_DEVICE_TIME ${deviceTimeEventDetails(deviceTimeStatus, "qr_scan")}"
                    )
                    adminFlowViewModel.dispatch(
                        AdminFlowUiAction.SetScanErrorMessage(
                            deviceTimeQrBlockMessage(deviceTimeStatus, uiLanguage)
                        )
                    )
                    return@launch
                }
                val networkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                when (
                    ExamScheduleValidator.validateAfterDeviceTimeCheck(
                        payload = payload,
                        deviceTimeStatus = deviceTimeStatus,
                        networkNowMillis = networkNowMillis
                    )
                ) {
                    ExamScheduleValidationResult.Valid -> {
                        pendingScanConfirmError = null
                        pendingScanConfirmPayload = payload
                    }

                    ExamScheduleValidationResult.NotStarted -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR is not active yet. The exam can only be opened starting ${payload.startDateTime}.",
                                    "QR ini belum aktif. Ujian baru bisa dibuka mulai ${payload.startDateTime}."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.Finished -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR is no longer valid. The exam ended at ${payload.endDateTime}.",
                                    "QR ini sudah tidak berlaku. Waktu ujian berakhir pada ${payload.endDateTime}."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.InvalidSchedule -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR has an invalid exam schedule. Please check the start and end time.",
                                    "QR ini memiliki jadwal ujian yang tidak valid. Periksa waktu mulai dan selesai."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.TimeSpoofDetected -> {
                        Log.w(
                            "AppHostDeviceTime",
                            "QR_BLOCKED_DEVICE_TIME schedule_result=time_spoof_detected | network_now_ms=${networkNowMillis ?: "unavailable"} | " +
                                deviceTimeEventDetails(deviceTimeStatus, "qr_scan_network_time")
                        )
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                deviceTimeQrBlockMessage(deviceTimeStatus, uiLanguage)
                            )
                        )
                    }
                }
            } catch (throwable: Throwable) {
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        throwable.message ?: localized(
                            uiLanguage,
                            "The QR code could not be read.",
                            "QR tidak dapat dibaca."
                        )
                    )
                )
            }
        }
    }

    fun launchDirectLink() {
        coroutineScope.launch {
            try {
                val activeSettings = loadCurrentAdminSettings()
                val activeDirectLinkLabel = activeSettings.fastExamLabel.trim().ifBlank { FastExamName }
                val configuredDirectLinkUrl = activeSettings.fastExamUrl.trim().ifBlank {
                    SecureStrings.fastExamUrl
                }
                val directLinkUrlValidation = validateExamUrl(configuredDirectLinkUrl)
                val normalizedDirectLinkUrl = directLinkUrlValidation.normalizedUrl
                if (normalizedDirectLinkUrl == null) {
                    error(
                        localized(
                            uiLanguage,
                            "Direct Link URL must start with https:// and include a domain. Update it from Secret Admin.",
                            "URL Direct Link harus diawali https:// dan memiliki domain. Perbarui dari Secret Admin."
                        )
                    )
                }
                val nowMillis = System.currentTimeMillis()
                val scheduleWindow = ExamScheduleDefaults.defaultDirectLinkWindow(nowMillis = nowMillis)
                val directLinkLocationPolicy = runCatching {
                    activeSettings.directLinkLocationPolicy()
                }.getOrNull()
                val directLinkLocationPolicySource = when {
                    directLinkLocationPolicy != null && activeSettings.directLinkLocationPolicySaved ->
                        LocationPolicySource.DirectLinkSaved
                    else -> LocationPolicySource.DisabledNoPolicy
                }
                val directLinkPayload = ExamQrPayload(
                    examUrl = normalizedDirectLinkUrl,
                    examName = activeDirectLinkLabel,
                    startDateTime = scheduleWindow.startDateTime,
                    endDateTime = scheduleWindow.endDateTime,
                    issuedAt = nowMillis,
                    locationPolicy = directLinkLocationPolicy,
                    locationPolicySource = directLinkLocationPolicySource
                )
                activeExamPayload = directLinkPayload
                savedRouteSnapshotRaw = AppRecoveryRoute.ExamFlowPreparation.name
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
            } catch (throwable: Throwable) {
                activeExamPayload = null
                savedRouteSnapshotRaw = AppRecoveryRoute.Home.name
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        throwable.message ?: localized(
                            uiLanguage,
                            "Direct Link could not be opened.",
                            "Direct Link tidak dapat dibuka."
                        )
                    )
                )
            }
        }
    }

    LaunchedEffect(initialHomeAction) {
        when (initialHomeAction) {
            PendingHomeAction.RuntimeHome -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.RuntimeHome.name}")
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.ScanExam -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.ScanExam.name}")
                adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog)
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.CustomQrAdmin -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.CustomQrAdmin.name}")
                loadCurrentAdminSettings()
                adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.DirectLink -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.DirectLink.name}")
                launchDirectLink()
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.SecretAdmin -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.SecretAdmin.name}")
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                adminFlowViewModel.dispatch(AdminFlowUiAction.ShowAdminPasswordDialog)
                onInitialHomeActionConsumed()
            }

            null -> Unit
        }
    }

    LaunchedEffect(adminFlowUiState.currentScreen, processDeathRecoveryPending) {
        if (processDeathRecoveryPending && adminFlowUiState.currentScreen == AppScreen.Home) {
            return@LaunchedEffect
        }
        savedRouteSnapshotRaw = when (adminFlowUiState.currentScreen) {
            AppScreen.Home -> AppRecoveryRoute.Home.name
            AppScreen.CustomQrAdmin -> AppRecoveryRoute.CustomQrAdmin.name
            AppScreen.SecretAdmin -> AppRecoveryRoute.SecretAdmin.name
            AppScreen.ExamWebView -> {
                if (parseAppRecoveryRoute(savedRouteSnapshotRaw) == AppRecoveryRoute.ExamFlowRuntime) {
                    AppRecoveryRoute.ExamFlowRuntime.name
                } else {
                    AppRecoveryRoute.ExamFlowPreparation.name
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalUiLanguage provides uiLanguage,
        LocalLowRamProfile provides lowRamProfile,
        LocalDeviceCompatibilityProfile provides deviceCompatibilityProfile
    ) {
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.CustomQrAdmin) {
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseCustomQrAdmin)
        }
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.SecretAdmin) {
            AdminAuthSession.clear()
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseSecretAdmin)
        }

        if (adminFlowUiState.currentScreen == AppScreen.Home) {
            remember {
                StartupTrace.mark("home_compose_start")
                true
            }
            if (lowRamProfile.severe) {
                ExamLockLowRamHomeScreen(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = { uiLanguage = it },
                    onScanExam = { adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog) },
                    onOpenAdmin = {
                        coroutineScope.launch {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                        }
                    },
                    onOpenFastExam = ::launchDirectLink,
                    directLinkLabel = directLinkLabel,
                    onSecretTap = ::registerSecretTap,
                    onOpenPerformanceProfile = { showPerformanceProfileDialog = true },
                    showDeferredChrome = showDeferredHomeChrome
                )
            } else {
                ExamLockHomeScreen(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = { uiLanguage = it },
                    onScanExam = { adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog) },
                    onOpenAdmin = {
                        coroutineScope.launch {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                        }
                    },
                    onOpenFastExam = ::launchDirectLink,
                    directLinkLabel = directLinkLabel,
                    onSecretTap = ::registerSecretTap,
                    onOpenPerformanceProfile = { showPerformanceProfileDialog = true },
                    showDeferredChrome = showDeferredHomeChrome
                )
            }
        } else {
            AppNonHomeRouteHost(
                screen = adminFlowUiState.currentScreen,
                uiState = adminFlowUiState,
                activeExamPayload = activeExamPayload,
                adminSettingsSnapshot = ::activeAdminSettingsSnapshot,
                updateAdminSettings = ::updateAdminSettings,
                dispatch = adminFlowViewModel::dispatch,
                pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
                pendingRecoveryEventDetails = pendingRecoveryEventDetails,
                onDirectLinkSaveLogConsumed = { pendingDirectLinkSaveLog = null },
                onRecoveryEventConsumed = { pendingRecoveryEventDetails = null },
                examSessionRecoveryNonce = examSessionRecoveryNonce,
                deviceTimeBaselineWallClockMillis = deviceTimeBaseline.wallClockMillis,
                deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaseline.elapsedRealtimeMillis,
                onExamSessionStartedStateChange = { started ->
                    savedRouteSnapshotRaw = if (started) {
                        AppRecoveryRoute.ExamFlowRuntime.name
                    } else {
                        AppRecoveryRoute.ExamFlowPreparation.name
                    }
                },
                onExamExit = {
                    activeExamPayload = null
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                },
                onMissingExamPayload = {
                        savedRouteSnapshotRaw = AppRecoveryRoute.Home.name
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }
            )
        }

        if (showPerformanceProfileDialog) {
            PublicPerformanceProfileDialog(
                selectedOverride = lowRamProfile.lowRamOverride,
                detectedProfile = detectedLowRamProfile,
                effectiveProfile = lowRamProfile,
                onOverrideChange = ::updateLowRamProfileOverride,
                onDismiss = { showPerformanceProfileDialog = false }
            )
        }

        if (adminFlowUiState.showAdminPasswordDialog) {
            AdminPasswordDialog(
                password = adminFlowUiState.adminPasswordInput,
                errorMessage = adminFlowUiState.adminPasswordError,
                onPasswordChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(it))
                    if (adminFlowUiState.adminPasswordError != null) {
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                    }
                },
                onConfirm = {
                    val passwordInput = adminFlowUiState.adminPasswordInput
                    coroutineScope.launch {
                        val verified = withContext(Dispatchers.Default) {
                            AdminAuth.verify(context, passwordInput)
                        }
                        if (verified) {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.HideAdminPasswordDialog)
                            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenSecretAdmin)
                        } else {
                            adminFlowViewModel.dispatch(
                                AdminFlowUiAction.SetAdminPasswordError(
                                    localized(uiLanguage, "Incorrect password.", "Password salah.")
                                )
                            )
                        }
                    }
                },
                onDismiss = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideAdminPasswordDialog)
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                }
            )
        }

        adminFlowUiState.scanErrorMessage?.let { message ->
            InfoDialog(
                title = tr("QR Scan", "Scan QR"),
                message = message,
                onDismiss = { adminFlowViewModel.dispatch(AdminFlowUiAction.SetScanErrorMessage(null)) }
            )
        }

        pendingScanConfirmPayload?.let { payload ->
            val geofenceInfo = when (payload.locationPolicy?.shapeType) {
                GeofenceShapeType.Circle -> "Circle | ${payload.locationPolicy.effectiveCircleCenters.size} centers | ${payload.locationPolicy.radiusMeters} m"
                GeofenceShapeType.Polygon -> "Polygon | ${payload.locationPolicy.vertices.size} points"
                else -> "Disabled"
            }
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LockBlueFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "QR",
                                color = LockBlueDeep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = tr("Review Exam QR", "Review QR Ujian"),
                                color = LockTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = tr(
                                    "Check details before opening preparation.",
                                    "Cek detail sebelum membuka preparation."
                                ),
                                color = LockTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = LockSurfaceSoft,
                            border = BorderStroke(1.dp, LockOutlineStrong)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tr("Exam", "Ujian"),
                                        color = LockTextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = payload.examName.trim().ifBlank { "-" },
                                        color = LockTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tr("Start", "Mulai"),
                                            color = LockTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = payload.startDateTime,
                                            color = LockTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tr("End", "Selesai"),
                                            color = LockTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = payload.endDateTime,
                                            color = LockTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tr("Geofence", "Geofence"),
                                        color = LockTextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = geofenceInfo,
                                        color = LockTextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(UiTokens.RadiusMd),
                            color = if (payload.saveToDirectLink) {
                                Color(0xFFEAF7EF)
                            } else {
                                LockBlueTint
                            },
                            border = BorderStroke(
                                1.dp,
                                if (payload.saveToDirectLink) {
                                    LockSafeStrong.copy(alpha = 0.22f)
                                } else {
                                    LockBlue.copy(alpha = 0.16f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (payload.saveToDirectLink) {
                                                LockSafeStrong
                                            } else {
                                                LockBlue
                                            }
                                        )
                                )
                                Text(
                                    text = if (payload.saveToDirectLink) {
                                        tr(
                                            "Direct Link will be saved after you tap Yes.",
                                            "Direct Link akan disimpan setelah tombol Ya ditekan."
                                        )
                                    } else {
                                        tr(
                                            "Direct Link will not be changed.",
                                            "Direct Link tidak akan diubah."
                                        )
                                    },
                                    color = if (payload.saveToDirectLink) {
                                        Color(0xFF155C3B)
                                    } else {
                                        LockBlueDeep
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        pendingScanConfirmError?.let { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(UiTokens.RadiusSm),
                                color = LockDangerBgSubtle,
                                border = BorderStroke(1.dp, LockDialogDangerIcon.copy(alpha = 0.30f))
                            ) {
                                Text(
                                    text = message,
                                    color = LockDialogDangerIcon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                        confirmPendingScanPayload(payload)
                        },
                        enabled = !pendingScanConfirmInFlight,
                        shape = RoundedCornerShape(UiTokens.RadiusSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark,
                            disabledContainerColor = LockBlue.copy(alpha = 0.45f),
                            disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                        )
                    ) {
                        Text(
                            text = when {
                                pendingScanConfirmInFlight -> tr("Processing...", "Memproses...")
                                payload.saveToDirectLink -> tr(
                                    "Yes, save & continue",
                                    "Ya, simpan & lanjut"
                                )
                                else -> tr(
                                    "Yes, continue to Preparation",
                                    "Ya, lanjut ke Preparation"
                                )
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingScanConfirmPayload = null
                        pendingScanConfirmError = null
                    }, enabled = !pendingScanConfirmInFlight) {
                        Text(
                            text = tr("Cancel", "Batal"),
                            color = LockTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        if (pendingRecoveryNoticeTitle != null && pendingRecoveryNoticeMessage != null) {
            InfoDialog(
                title = pendingRecoveryNoticeTitle.orEmpty(),
                message = pendingRecoveryNoticeMessage.orEmpty(),
                onDismiss = {
                    pendingRecoveryNoticeTitle = null
                    pendingRecoveryNoticeMessage = null
                }
            )
        }

        if (adminFlowUiState.showScanSourceDialog) {
            ExamScanSourceDialogHost(
                uiLanguage = uiLanguage,
                onRawPayload = ::handleExamQrRawPayload,
                onScanError = { message ->
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetScanErrorMessage(message))
                },
                onDismiss = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideScanSourceDialog)
                }
            )
        }
    }
}
