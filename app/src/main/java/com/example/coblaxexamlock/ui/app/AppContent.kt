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
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.persistence.HomeAdminSettings
import com.example.coblaxexamlock.persistence.readHomeAdminSettings
import com.example.coblaxexamlock.persistence.readSavedUiLanguage
import com.example.coblaxexamlock.persistence.saveUiLanguage
import com.example.coblaxexamlock.resolveLowRamProfile
import com.example.coblaxexamlock.ui.admin.ExamLockLowRamHomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun AppContent(
    initialHomeActionRaw: String? = null,
    initialLowRamProfile: LowRamProfile? = null
) {
    val context = LocalContext.current
    val lowRamProfile = remember(context, initialLowRamProfile) {
        initialLowRamProfile ?: resolveLowRamProfile(context)
    }
    val initialUiLanguage = remember { context.readSavedUiLanguage() }
    var shellUiLanguage by rememberSaveable { mutableStateOf(initialUiLanguage) }
    var persistedShellUiLanguage by remember { mutableStateOf(initialUiLanguage) }
    var shellHomeSettings by remember { mutableStateOf(HomeAdminSettings()) }
    var shellShowDeferredChrome by rememberSaveable { mutableStateOf(false) }
    val startRuntimeInitially = !lowRamProfile.severe || initialHomeActionRaw != null
    var startRuntime by rememberSaveable {
        mutableStateOf(startRuntimeInitially)
    }
    var pendingHomeActionRaw by rememberSaveable { mutableStateOf(initialHomeActionRaw) }
    val pendingHomeAction = pendingHomeActionRaw?.let { raw ->
        runCatching { PendingHomeAction.valueOf(raw) }.getOrNull()
    }

    remember {
        val startupMode = when {
            !lowRamProfile.severe -> "shell=runtime"
            startRuntimeInitially -> "shell=runtime action=${initialHomeActionRaw.orEmpty()}"
            else -> "shell=survival"
        }
        StartupTrace.mark("app_content_start", startupMode)
        true
    }

    if (lowRamProfile.severe && !startRuntime) {
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
            LocalLowRamProfile provides lowRamProfile
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
