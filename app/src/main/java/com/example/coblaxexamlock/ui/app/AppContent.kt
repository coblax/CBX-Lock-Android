package com.example.coblaxexamlock.ui.app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import com.example.coblaxexamlock.LocalDeviceCompatibilityProfile
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.currentDeviceCompatibilityProfile
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.persistence.HomeAdminSettings
import com.example.coblaxexamlock.persistence.readHomeAdminSettings
import com.example.coblaxexamlock.persistence.readSavedUiLanguage
import com.example.coblaxexamlock.persistence.saveUiLanguage
import com.example.coblaxexamlock.resolveLowRamProfile
import com.example.coblaxexamlock.ui.admin.ExamLockLowRamHomeScreen
import com.example.coblaxexamlock.ui.exam.ExamRuntimeHardeningDiagnostics
import com.example.coblaxexamlock.ui.exam.ExamRuntimeHardeningLogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal fun shouldStartRuntimeImmediately(
    lowRamProfile: LowRamProfile,
    initialHomeActionRaw: String?
): Boolean = !lowRamProfile.deferHeavyUi || initialHomeActionRaw != null

@Composable
internal fun AppContent(
    initialHomeActionRaw: String? = null,
    initialLowRamProfile: LowRamProfile? = null
) {
    val context = LocalContext.current
    val lowRamProfile = remember(context, initialLowRamProfile) {
        initialLowRamProfile ?: resolveLowRamProfile(context)
    }
    val deviceCompatibilityProfile = remember(lowRamProfile) {
        currentDeviceCompatibilityProfile(lowRamProfile)
    }
    val initialUiLanguage = remember { context.readSavedUiLanguage() }
    var shellUiLanguage by rememberSaveable { mutableStateOf(initialUiLanguage) }
    var persistedShellUiLanguage by remember { mutableStateOf(initialUiLanguage) }
    var shellHomeSettings by remember { mutableStateOf(HomeAdminSettings()) }
    var shellShowDeferredChrome by rememberSaveable { mutableStateOf(false) }
    val startRuntimeInitially = shouldStartRuntimeImmediately(lowRamProfile, initialHomeActionRaw)
    var startRuntime by rememberSaveable {
        mutableStateOf(startRuntimeInitially)
    }
    var pendingHomeActionRaw by rememberSaveable { mutableStateOf(initialHomeActionRaw) }
    val pendingHomeAction = pendingHomeActionRaw?.let { raw ->
        runCatching { PendingHomeAction.valueOf(raw) }.getOrNull()
    }

    remember {
        val startupMode = when {
            startRuntimeInitially -> "shell=runtime action=${initialHomeActionRaw.orEmpty()} low_ram=${lowRamProfile.enabled} severe=${lowRamProfile.severe}"
            else -> "shell=survival low_ram=${lowRamProfile.enabled} severe=${lowRamProfile.severe}"
        }
        StartupTrace.mark("app_content_start", startupMode)
        true
    }

    if (lowRamProfile.deferHeavyUi && !startRuntime) {
        remember {
            StartupTrace.mark("home_compose_start", "shell=survival")
            true
        }
        LaunchedEffect(context) {
            withFrameNanos { }
            shellHomeSettings = withContext(Dispatchers.IO) {
                context.readHomeAdminSettings()
            }
            StartupTrace.mark(
                "home_settings_loaded",
                "shell=survival | direct_link_label=${
                    shellHomeSettings.fastExamLabel.trim().ifBlank { FastExamName }
                }"
            )
            delay(900)
            shellShowDeferredChrome = true
            StartupTrace.mark("home_deferred_chrome_shown", "mode=survival")
        }
        LaunchedEffect(shellUiLanguage) {
            if (shellUiLanguage != persistedShellUiLanguage) {
                context.saveUiLanguage(shellUiLanguage)
                persistedShellUiLanguage = shellUiLanguage
            }
        }
        LaunchedEffect(deviceCompatibilityProfile) {
            Log.i(
                ExamRuntimeHardeningLogTag,
                "code=${ExamRuntimeHardeningDiagnostics.DeviceCompatProfileResolved} " +
                    "level=INFO details=${deviceCompatibilityProfile.diagnosticSummary()} | shell=survival"
            )
        }
        DisposableEffect(lowRamProfile) {
            val listener: (Int) -> Unit = { level ->
                if (MemoryPressureCoordinator.shouldReleaseUiBitmaps(level)) {
                    shellShowDeferredChrome = false
                    Log.i("HomeMemory", "trim=$level action=hide_survival_home_chrome")
                }
            }
            MemoryPressureCoordinator.addListener(listener)
            onDispose {
                MemoryPressureCoordinator.removeListener(listener)
            }
        }
        CompositionLocalProvider(
            LocalUiLanguage provides shellUiLanguage,
            LocalLowRamProfile provides lowRamProfile,
            LocalDeviceCompatibilityProfile provides deviceCompatibilityProfile
        ) {
            ExamLockLowRamHomeScreen(
                uiLanguage = shellUiLanguage,
                onUiLanguageChange = { shellUiLanguage = it },
                onScanExam = {
                    pendingHomeActionRaw = PendingHomeAction.ScanExam.name
                    startRuntime = true
                },
                onOpenAdmin = {
                    pendingHomeActionRaw = PendingHomeAction.CustomQrAdmin.name
                    startRuntime = true
                },
                onOpenFastExam = {
                    pendingHomeActionRaw = PendingHomeAction.DirectLink.name
                    startRuntime = true
                },
                directLinkLabel = shellHomeSettings.fastExamLabel.trim().ifBlank { FastExamName },
                onSecretTap = { startRuntime = true },
                showDeferredChrome = shellShowDeferredChrome
            )
        }
        return
    }

    AppHostRuntimeContent(
        initialUiLanguageOverride = shellUiLanguage,
        initialHomeAdminSettings = shellHomeSettings,
        initialLowRamProfile = lowRamProfile,
        initialHomeAction = pendingHomeAction,
        onInitialHomeActionConsumed = { pendingHomeActionRaw = null }
    )
}
