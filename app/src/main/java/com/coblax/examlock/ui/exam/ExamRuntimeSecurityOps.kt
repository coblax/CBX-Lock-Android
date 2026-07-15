package com.coblax.examlock.ui.exam

import android.content.Context
import androidx.compose.runtime.MutableState
import com.coblax.examlock.AccessibilityInspectionResult
import com.coblax.examlock.AccessibilityBypassState
import com.coblax.examlock.ActivityLockTaskBridge
import com.coblax.examlock.AdbInspection
import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AppSwitchBypassState
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.DpcRuntimeStatus
import com.coblax.examlock.ExamParticipantContext
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.MainActivity
import com.coblax.examlock.OverlayBypassState
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.ScreenPinningPlatformBridge
import com.coblax.examlock.ScreenPinningMode
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.SignatureIntegrityResult
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.buildRootSecurityStatus
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.inspectAccessibility
import com.coblax.examlock.inspectAdb
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.model.VirtualEnvironmentDiagnostics
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.resolveExpectedSigningFingerprints
import com.coblax.examlock.RootSecurityStatus
import com.coblax.examlock.runtime.SecurityDetectorCache
import com.coblax.examlock.runtime.getCachedVirtualEnvironmentDiagnostics
import com.coblax.examlock.runtime.getCurrentInputMethodPackage
import com.coblax.examlock.runtime.getVirtualEnvironmentDiagnosticsOnIo
import com.coblax.examlock.runtime.hasBluetoothExamPermission
import com.coblax.examlock.runtime.isAllowedExamKeyboard
import com.coblax.examlock.runtime.isBluetoothEnabledForExam
import com.coblax.examlock.runtime.resolveKeyboardAppLabel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ExamRuntimeKeyboardSecurityCallbacks(
    val setCurrentKeyboardPackage: (String) -> Unit,
    val setCurrentKeyboardLabel: (String) -> Unit,
    val setLastKeyboardAllowed: (Boolean) -> Unit,
    val setUseBuiltInExamKeyboard: (Boolean) -> Unit,
    val setShowBuiltInExamKeyboard: (Boolean) -> Unit,
    val setHasEditableFocus: (Boolean) -> Unit,
    val incrementKeyboardViolationCount: () -> Unit,
    val setShowKeyboardViolationDialog: (Boolean) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun refreshExamRuntimeKeyboardSecurity(
    context: Context,
    bypassKeyboardPolicy: Boolean,
    examSessionStarted: Boolean,
    useBuiltInExamKeyboard: Boolean,
    lastKeyboardAllowed: Boolean,
    triggerViolation: Boolean,
    examAlarmController: ExamAlarmController,
    callbacks: ExamRuntimeKeyboardSecurityCallbacks
) {
    val latestPackage = getCurrentInputMethodPackage(context).orEmpty()
    val latestLabel = resolveKeyboardAppLabel(context, latestPackage)
    val allowedNow = if (bypassKeyboardPolicy) true else isAllowedExamKeyboard(context, latestPackage)

    if (bypassKeyboardPolicy) {
        callbacks.setCurrentKeyboardPackage(latestPackage)
        callbacks.setCurrentKeyboardLabel(latestLabel)
        callbacks.setLastKeyboardAllowed(true)
        if (!examSessionStarted) {
            callbacks.setUseBuiltInExamKeyboard(false)
            callbacks.setShowBuiltInExamKeyboard(false)
            callbacks.setHasEditableFocus(false)
        }
        return
    }

    if (!examSessionStarted && allowedNow) {
        callbacks.setUseBuiltInExamKeyboard(false)
    }

    if (triggerViolation && !useBuiltInExamKeyboard && lastKeyboardAllowed && !allowedNow) {
        callbacks.recordAction(
            "KEYBOARD_POLICY_VIOLATION",
            latestPackage,
            DiagnosticEventLevel.SECURITY
        )
        callbacks.incrementKeyboardViolationCount()
        callbacks.setShowKeyboardViolationDialog(true)
        examAlarmController.start()
    }

    callbacks.setCurrentKeyboardPackage(latestPackage)
    callbacks.setCurrentKeyboardLabel(latestLabel)
    callbacks.setLastKeyboardAllowed(allowedNow)
}

internal class ExamRuntimeBluetoothSecurityCallbacks(
    val setBluetoothPermissionGranted: (Boolean) -> Unit,
    val setBluetoothEnabled: (Boolean) -> Unit,
    val incrementBluetoothViolationCount: () -> Unit,
    val setShowBluetoothViolationDialog: (Boolean) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun refreshExamRuntimeBluetoothSecurity(
    context: Context,
    bypassBluetooth: Boolean,
    examSessionStarted: Boolean,
    triggerViolation: Boolean,
    examAlarmController: ExamAlarmController,
    callbacks: ExamRuntimeBluetoothSecurityCallbacks
) {
    val bluetoothPermissionGranted = hasBluetoothExamPermission(context)
    val enabledNow = if (bluetoothPermissionGranted) {
        isBluetoothEnabledForExam(context)
    } else {
        false
    }

    if (!bypassBluetooth && triggerViolation && examSessionStarted && enabledNow) {
        callbacks.recordAction(
            "BLUETOOTH_ENABLED_DURING_EXAM",
            "-",
            DiagnosticEventLevel.SECURITY
        )
        callbacks.incrementBluetoothViolationCount()
        callbacks.setShowBluetoothViolationDialog(true)
        examAlarmController.start()
    }

    callbacks.setBluetoothPermissionGranted(bluetoothPermissionGranted)
    callbacks.setBluetoothEnabled(enabledNow)
}

internal class ExamRuntimeScreenPinningDiagnosticsCallbacks(
    val setScreenPinningAvailable: (Boolean) -> Unit,
    val setScreenPinningEnabledInSystem: (String) -> Unit,
    val setLockTaskStateBeforePinningRequest: (String) -> Unit,
    val setLockTaskStateAfterPinningRequest: (String) -> Unit
)

internal fun refreshExamRuntimeScreenPinningDiagnostics(
    context: Context,
    lockTaskBridge: ActivityLockTaskBridge,
    screenPinningRequestOutcome: String,
    callbacks: ExamRuntimeScreenPinningDiagnosticsCallbacks
) {
    callbacks.setScreenPinningAvailable(ScreenPinningPlatformBridge.isAvailable())
    callbacks.setScreenPinningEnabledInSystem(ScreenPinningPlatformBridge.readSystemSetting(context))
    val currentLockTaskState = lockTaskBridge.stateLabel()
    callbacks.setLockTaskStateAfterPinningRequest(currentLockTaskState)
    if (screenPinningRequestOutcome == "Belum diminta") {
        callbacks.setLockTaskStateBeforePinningRequest(currentLockTaskState)
    }
}

internal class ExamRuntimeSignatureCallbacks(
    val setSignatureMismatchDetected: (Boolean) -> Unit,
    val setSecurityIssueDialogTitle: (String) -> Unit,
    val setSecurityIssueDialogMessage: (String) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun checkExamRuntimeSignatureIntegrity(
    context: Context,
    uiLanguage: UiLanguage,
    triggerViolation: Boolean,
    callbacks: ExamRuntimeSignatureCallbacks
): SignatureIntegrityResult {
    val expectedFingerprints = resolveExpectedSigningFingerprints(
        isDebugBuild = BuildConfig.DEBUG,
        releaseFingerprint = SecureStrings.signingFingerprintRelease,
        debugFingerprint = SecureStrings.signingFingerprintDebug
    )
    val result = SecurityDetectorCache.checkSignatureIntegrity(
        context = context,
        expectedFingerprints = expectedFingerprints,
        forceRefresh = triggerViolation
    )
    callbacks.setSignatureMismatchDetected(!result.isMatch)
    if (!result.isMatch && triggerViolation) {
        callbacks.recordAction(
            "SIGNATURE_MISMATCH_DETECTED",
            result.reason,
            DiagnosticEventLevel.SECURITY
        )
        callbacks.setSecurityIssueDialogTitle(
            localized(uiLanguage, "App Integrity Warning", "Integritas Aplikasi Bermasalah")
        )
        callbacks.setSecurityIssueDialogMessage(
            localized(
                uiLanguage,
                "The app signature does not match the official release. Reinstall the official APK.",
                "Signature aplikasi tidak cocok dengan APK resmi. Instal ulang APK resmi."
            )
        )
    }
    return result
}

internal class ExamRuntimeVirtualEnvironmentCallbacks(
    val getVirtualEnvironmentDetected: () -> Boolean,
    val setVirtualEnvironmentDetected: (Boolean) -> Unit,
    val setSecurityIssueDialogTitle: (String) -> Unit,
    val setSecurityIssueDialogMessage: (String) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun applyExamRuntimeVirtualEnvironmentDiagnostics(
    diagnostics: VirtualEnvironmentDiagnostics,
    triggerViolation: Boolean,
    examSessionStarted: Boolean,
    bypassVirtualEnvironment: Boolean,
    uiLanguage: UiLanguage,
    examAlarmController: ExamAlarmController,
    callbacks: ExamRuntimeVirtualEnvironmentCallbacks
) {
    val latestVirtualEnvironmentDetected = diagnostics.detected
    if (
        triggerViolation &&
        examSessionStarted &&
        !bypassVirtualEnvironment &&
        !callbacks.getVirtualEnvironmentDetected() &&
        latestVirtualEnvironmentDetected
    ) {
        callbacks.recordAction(
            "VIRTUAL_ENVIRONMENT_DETECTED",
            diagnostics.indicators.joinToString().ifBlank { "-" },
            DiagnosticEventLevel.SECURITY
        )
        callbacks.setSecurityIssueDialogTitle(localized(uiLanguage, "Virtual Environment Detected", "Virtual Environment Terdeteksi"))
        callbacks.setSecurityIssueDialogMessage(
            localized(uiLanguage, "This device is detected running in an emulator/VM. Use a physical device to continue the exam.", "Perangkat ini terdeteksi berjalan di emulator/VM. Gunakan perangkat fisik untuk melanjutkan ujian.")
        )
        examAlarmController.start()
    }
    callbacks.setVirtualEnvironmentDetected(latestVirtualEnvironmentDetected)
}

internal class ExamRuntimeDeviceIntegrityCallbacks(
    val getAccessibilityServiceEnabled: () -> Boolean,
    val getDeveloperOptionsEnabled: () -> Boolean,
    val getAdbEnabled: () -> Boolean,
    val getRootDetected: () -> Boolean,
    val setAccessibilityInspection: (AccessibilityInspectionResult) -> Unit,
    val setAccessibilityServiceEnabled: (Boolean) -> Unit,
    val setAccessibilityGuardEnabled: (Boolean) -> Unit,
    val setAdbInspection: (AdbInspection) -> Unit,
    val setDeveloperOptionsEnabled: (Boolean) -> Unit,
    val setAdbEnabled: (Boolean) -> Unit,
    val setRootSecurityStatus: (RootSecurityStatus) -> Unit,
    val setRootDetected: (Boolean) -> Unit,
    val setSelinuxPermissiveWarning: (Boolean) -> Unit,
    val setSecurityIssueDialogTitle: (String) -> Unit,
    val setSecurityIssueDialogMessage: (String) -> Unit,
    val checkSignatureIntegrity: () -> Unit,
    val applyVirtualEnvironmentDiagnostics: (VirtualEnvironmentDiagnostics, Boolean) -> Unit,
    val launchVirtualEnvironmentDiagnostics: () -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun refreshExamRuntimeDeviceIntegritySecurity(
    context: Context,
    uiLanguage: UiLanguage,
    examSessionStarted: Boolean,
    triggerViolation: Boolean,
    bypassAccessibility: Boolean,
    bypassAdb: Boolean,
    bypassRoot: Boolean,
    examAlarmController: ExamAlarmController,
    callbacks: ExamRuntimeDeviceIntegrityCallbacks
) {
    val latestAccessibilityInspection = inspectAccessibility(context)
    val latestAccessibilityServiceEnabled = latestAccessibilityInspection.blockingServiceActive
    val latestAdbInspection = inspectAdb(context)
    val rootDetectionDetails = SecurityDetectorCache.readRootDetectionDetails(
        context = context,
        forceRefresh = triggerViolation
    )
    val latestRootSecurityStatus = buildRootSecurityStatus(rootDetectionDetails)
    val cachedVirtualEnvironmentDiagnostics = getCachedVirtualEnvironmentDiagnostics()
    callbacks.checkSignatureIntegrity()

    if (triggerViolation && examSessionStarted) {
        if (
            !bypassAccessibility &&
            !callbacks.getAccessibilityServiceEnabled() &&
            latestAccessibilityServiceEnabled
        ) {
            callbacks.recordAction(
                "ACCESSIBILITY_ENABLED_DURING_EXAM",
                "-",
                DiagnosticEventLevel.SECURITY
            )
            callbacks.setSecurityIssueDialogTitle(localized(uiLanguage, "Accessibility Service Detected", "Accessibility Service Terdeteksi"))
            callbacks.setSecurityIssueDialogMessage(
                localized(uiLanguage, "Accessibility is active during the exam. Disable the accessibility service to keep the exam secure.", "Aksesibilitas aktif saat ujian berjalan. Nonaktifkan accessibility service agar ujian tetap aman.")
            )
            examAlarmController.start()
        }

        if (!bypassAdb && !callbacks.getDeveloperOptionsEnabled() && latestAdbInspection.developerOptionsEnabled) {
            callbacks.recordAction(
                "DEVELOPER_OPTIONS_ENABLED_DURING_EXAM",
                "-",
                DiagnosticEventLevel.SECURITY
            )
            callbacks.setSecurityIssueDialogTitle(localized(uiLanguage, "Developer Mode Active", "Developer Mode Aktif"))
            callbacks.setSecurityIssueDialogMessage(
                localized(uiLanguage, "Developer Mode was detected while the exam is running. Disable it before continuing.", "Developer Mode terdeteksi aktif saat ujian berjalan. Nonaktifkan sebelum melanjutkan.")
            )
            examAlarmController.start()
        }

        if (!bypassAdb && !callbacks.getAdbEnabled() && latestAdbInspection.adbEnabled) {
            callbacks.recordAction(
                "ADB_ENABLED_DURING_EXAM",
                "-",
                DiagnosticEventLevel.SECURITY
            )
            callbacks.setSecurityIssueDialogTitle(localized(uiLanguage, "USB Debugging (ADB) Active", "USB Debugging (ADB) Aktif"))
            callbacks.setSecurityIssueDialogMessage(
                localized(uiLanguage, "USB debugging was detected while the exam is running. Disable ADB before continuing.", "USB debugging terdeteksi aktif saat ujian berjalan. Nonaktifkan ADB sebelum melanjutkan.")
            )
            examAlarmController.start()
        }

        if (!bypassRoot && !callbacks.getRootDetected() && latestRootSecurityStatus.detected) {
            callbacks.recordAction(
                "ROOT_INDICATOR_DETECTED",
                "-",
                DiagnosticEventLevel.SECURITY
            )
            callbacks.setSecurityIssueDialogTitle(localized(uiLanguage, "Rooted Device Detected", "Root Device Terdeteksi"))
            callbacks.setSecurityIssueDialogMessage(
                com.coblax.examlock.runtime.buildRootIssueMessage(latestRootSecurityStatus.details)
            )
            examAlarmController.start()
        }
    }

    callbacks.setAccessibilityInspection(latestAccessibilityInspection)
    callbacks.setAccessibilityServiceEnabled(latestAccessibilityServiceEnabled)
    callbacks.setAccessibilityGuardEnabled(com.coblax.examlock.isExamGuardAccessibilityEnabled(context))
    callbacks.setAdbInspection(latestAdbInspection)
    callbacks.setDeveloperOptionsEnabled(latestAdbInspection.developerOptionsEnabled)
    callbacks.setAdbEnabled(latestAdbInspection.adbEnabled)
    callbacks.setRootSecurityStatus(latestRootSecurityStatus)
    callbacks.setRootDetected(latestRootSecurityStatus.detected)
    callbacks.setSelinuxPermissiveWarning(latestRootSecurityStatus.selinuxPermissive)
    if (cachedVirtualEnvironmentDiagnostics != null) {
        callbacks.applyVirtualEnvironmentDiagnostics(cachedVirtualEnvironmentDiagnostics, triggerViolation)
    } else {
        callbacks.launchVirtualEnvironmentDiagnostics()
    }
}

internal class ExamRuntimeSecurityOps(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val lockTaskBridge: ActivityLockTaskBridge,
    private val uiLanguage: UiLanguage,
    private val mainActivity: MainActivity?,
    private val adminSettings: AdminSettings,
    private val payload: ExamQrPayload,
    private val participantContext: ExamParticipantContext?,
    private val lowRamProfile: LowRamProfile,
    private val screenPinningMode: ScreenPinningMode,
    private val accessibilityBypassState: AccessibilityBypassState,
    private val overlayBypassState: OverlayBypassState,
    private val appSwitchBypassState: AppSwitchBypassState,
    private val adbBypassState: AdbBypassState,
    private val rootBypassState: RootBypassState,
    private val deviceTimeBypassState: DeviceTimeBypassState,
    private val vpnBypassState: VpnBypassState,
    private val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    private val runtimeDiagnosticsOps: ExamRuntimeDiagnosticsOps,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val clipboardUiState: ExamRuntimeClipboardUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val networkUiState: ExamRuntimeNetworkUiState,
    private val dpcRuntimeStatusProvider: () -> DpcRuntimeStatus,
    private val accessibilityGuardEnabledState: MutableState<Boolean>,
    private val accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    private val accessibilityGuardLastReasonState: MutableState<String?>,
    private val accessibilityGuardLastForeignPackageState: MutableState<String?>,
    private val accessibilityGuardLastEventTypeState: MutableState<String?>,
    private val accessibilityGuardLastDetectedAtState: MutableState<String?>,
    private val accessibilityGuardAlarmSeverityState: MutableState<String>,
    private val examAlarmController: ExamAlarmController,
    private val refreshIntegrityGuard: () -> Unit
) {
    // Guard for all fire-and-forget launches to prevent silent failures.
    private val launchExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e(
            ExamRuntimeHardeningLogTag,
            "SecurityOps uncaught coroutine exception: ${throwable.javaClass.simpleName}",
            throwable
        )
    }
    private val bypassAccessibility: Boolean
        get() = accessibilityBypassState == AccessibilityBypassState.Active
    private val bypassAdb: Boolean
        get() = adbBypassState == AdbBypassState.Active
    private val bypassRoot: Boolean
        get() = rootBypassState == RootBypassState.Active
    private val bypassDeviceTime: Boolean
        get() = deviceTimeBypassState == DeviceTimeBypassState.Active
    private val bypassVpn: Boolean
        get() = vpnBypassState == VpnBypassState.Active
    private val isKeyboardAllowed: Boolean
        get() = adminSettings.bypassKeyboardPolicy ||
            isAllowedExamKeyboard(context, flowUiState.currentKeyboardPackage.value)

    private val examSessionStarted: Boolean
        get() = flowUiState.examSessionStarted.value

    private val examGuardArmed: Boolean
        get() = adminUiState.examRuntimeMonitoringArmed.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.examSessionStarted.value

    fun refreshKeyboardSecurity(triggerViolation: Boolean) {
        refreshExamRuntimeKeyboardSecurity(
            context = context,
            bypassKeyboardPolicy = adminSettings.bypassKeyboardPolicy,
            examSessionStarted = examSessionStarted,
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            lastKeyboardAllowed = flowUiState.lastKeyboardAllowed.value,
            triggerViolation = triggerViolation,
            examAlarmController = examAlarmController,
            callbacks = ExamRuntimeKeyboardSecurityCallbacks(
                setCurrentKeyboardPackage = { flowUiState.currentKeyboardPackage.value = it },
                setCurrentKeyboardLabel = { flowUiState.currentKeyboardLabel.value = it },
                setLastKeyboardAllowed = { flowUiState.lastKeyboardAllowed.value = it },
                setUseBuiltInExamKeyboard = { flowUiState.useBuiltInExamKeyboard.value = it },
                setShowBuiltInExamKeyboard = { flowUiState.showBuiltInExamKeyboard.value = it },
                setHasEditableFocus = { flowUiState.hasEditableFocus.value = it },
                incrementKeyboardViolationCount = { securityUiState.keyboardViolationCount.intValue += 1 },
                setShowKeyboardViolationDialog = { securityUiState.showKeyboardViolationDialog.value = it },
                recordAction = runtimeDiagnosticsOps::recordAction
            )
        )
    }

    fun refreshBluetoothSecurity(triggerViolation: Boolean) {
        refreshExamRuntimeBluetoothSecurity(
            context = context,
            bypassBluetooth = adminSettings.bypassBluetooth,
            examSessionStarted = examSessionStarted,
            triggerViolation = triggerViolation,
            examAlarmController = examAlarmController,
            callbacks = ExamRuntimeBluetoothSecurityCallbacks(
                setBluetoothPermissionGranted = { securityUiState.bluetoothPermissionGranted.value = it },
                setBluetoothEnabled = { securityUiState.bluetoothEnabled.value = it },
                incrementBluetoothViolationCount = { securityUiState.bluetoothViolationCount.intValue += 1 },
                setShowBluetoothViolationDialog = { securityUiState.showBluetoothViolationDialog.value = it },
                recordAction = runtimeDiagnosticsOps::recordAction
            )
        )
    }

    fun refreshScreenPinningDiagnostics() {
        refreshExamRuntimeScreenPinningDiagnostics(
            context = context,
            lockTaskBridge = lockTaskBridge,
            screenPinningRequestOutcome = adminUiState.screenPinningRequestOutcome.value,
            callbacks = ExamRuntimeScreenPinningDiagnosticsCallbacks(
                setScreenPinningAvailable = { adminUiState.screenPinningAvailable.value = it },
                setScreenPinningEnabledInSystem = { adminUiState.screenPinningEnabledInSystem.value = it },
                setLockTaskStateBeforePinningRequest = {
                    adminUiState.lockTaskStateBeforePinningRequest.value = it
                },
                setLockTaskStateAfterPinningRequest = {
                    adminUiState.lockTaskStateAfterPinningRequest.value = it
                }
            )
        )
    }

    fun checkSignatureIntegrity(triggerViolation: Boolean): SignatureIntegrityResult =
        checkExamRuntimeSignatureIntegrity(
            context = context,
            uiLanguage = uiLanguage,
            triggerViolation = triggerViolation,
            callbacks = ExamRuntimeSignatureCallbacks(
                setSignatureMismatchDetected = { securityUiState.signatureMismatchDetected.value = it },
                setSecurityIssueDialogTitle = { adminUiState.securityIssueDialogTitle.value = it },
                setSecurityIssueDialogMessage = { adminUiState.securityIssueDialogMessage.value = it },
                recordAction = runtimeDiagnosticsOps::recordAction
            )
        )

    fun applyVirtualEnvironmentDiagnostics(
        diagnostics: VirtualEnvironmentDiagnostics,
        triggerViolation: Boolean
    ) {
        applyExamRuntimeVirtualEnvironmentDiagnostics(
            diagnostics = diagnostics,
            triggerViolation = triggerViolation,
            examSessionStarted = examSessionStarted,
            bypassVirtualEnvironment = adminSettings.bypassVirtualEnvironment,
            uiLanguage = uiLanguage,
            examAlarmController = examAlarmController,
            callbacks = ExamRuntimeVirtualEnvironmentCallbacks(
                getVirtualEnvironmentDetected = { securityUiState.virtualEnvironmentDetected.value },
                setVirtualEnvironmentDetected = { securityUiState.virtualEnvironmentDetected.value = it },
                setSecurityIssueDialogTitle = { adminUiState.securityIssueDialogTitle.value = it },
                setSecurityIssueDialogMessage = { adminUiState.securityIssueDialogMessage.value = it },
                recordAction = runtimeDiagnosticsOps::recordAction
            )
        )
    }

    fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) {
        refreshExamRuntimeDeviceIntegritySecurity(
            context = context,
            uiLanguage = uiLanguage,
            examSessionStarted = examSessionStarted,
            triggerViolation = triggerViolation,
            bypassAccessibility = bypassAccessibility,
            bypassAdb = bypassAdb,
            bypassRoot = bypassRoot,
            examAlarmController = examAlarmController,
            callbacks = ExamRuntimeDeviceIntegrityCallbacks(
                getAccessibilityServiceEnabled = { securityUiState.accessibilityServiceEnabled.value },
                getDeveloperOptionsEnabled = { securityUiState.developerOptionsEnabled.value },
                getAdbEnabled = { securityUiState.adbEnabled.value },
                getRootDetected = { securityUiState.rootDetected.value },
                setAccessibilityInspection = { securityUiState.accessibilityInspection.value = it },
                setAccessibilityServiceEnabled = { securityUiState.accessibilityServiceEnabled.value = it },
                setAccessibilityGuardEnabled = { accessibilityGuardEnabledState.value = it },
                setAdbInspection = { securityUiState.adbInspection.value = it },
                setDeveloperOptionsEnabled = { securityUiState.developerOptionsEnabled.value = it },
                setAdbEnabled = { securityUiState.adbEnabled.value = it },
                setRootSecurityStatus = { securityUiState.rootSecurityStatus.value = it },
                setRootDetected = { securityUiState.rootDetected.value = it },
                setSelinuxPermissiveWarning = { securityUiState.selinuxPermissiveWarning.value = it },
                setSecurityIssueDialogTitle = { adminUiState.securityIssueDialogTitle.value = it },
                setSecurityIssueDialogMessage = { adminUiState.securityIssueDialogMessage.value = it },
                checkSignatureIntegrity = { checkSignatureIntegrity(triggerViolation) },
                applyVirtualEnvironmentDiagnostics = ::applyVirtualEnvironmentDiagnostics,
                launchVirtualEnvironmentDiagnostics = {
                    coroutineScope.launch(launchExceptionHandler) {
                        val diagnostics = getVirtualEnvironmentDiagnosticsOnIo(
                            context = context,
                            forceRefresh = triggerViolation
                        )
                        applyVirtualEnvironmentDiagnostics(diagnostics, triggerViolation)
                    }
                },
                recordAction = runtimeDiagnosticsOps::recordAction
            )
        )
    }

    fun launchTelegramSectionReport(section: DiagnosticSection) {
        launchExamRuntimeTelegramSectionReport(
            scope = coroutineScope,
            context = context,
            section = section,
            uiLanguage = uiLanguage,
            mainActivity = mainActivity,
            adminSettings = adminSettings,
            payload = payload,
            effectiveExamUserAgent = adminSettings.effectiveExamUserAgent(),
            participantContext = participantContext,
            lowRamProfile = lowRamProfile,
            lockTaskBridge = lockTaskBridge,
            screenPinningMode = screenPinningMode,
            securityUiState = securityUiState,
            overlayBypassState = overlayBypassState,
            overlayViolationCount = securityUiState.overlayViolationCount.intValue,
            overlayShieldStatus = runtimeDiagnosticsOps.overlayShieldStatus,
            lastOverlayTrigger = securityUiState.lastOverlayTrigger.value,
            lastOverlayAt = securityUiState.lastOverlayAt.value,
            lastOverlayContext = securityUiState.lastOverlayContext.value,
            appSwitchBypassState = appSwitchBypassState,
            forcedExitViolationCount = securityUiState.forcedExitViolationCount.intValue,
            pendingForcedExitViolation = securityUiState.pendingForcedExitViolation.value,
            lastAppSwitchTrigger = adminUiState.lastAppSwitchTrigger.value,
            lastAppSwitchAt = adminUiState.lastAppSwitchAt.value,
            lastAppSwitchContext = adminUiState.lastAppSwitchContext.value,
            accessibilityGuardEnabled = accessibilityGuardEnabledState.value,
            accessibilityGuardFallbackActive = accessibilityGuardFallbackActiveState.value,
            accessibilityGuardLastReason = accessibilityGuardLastReasonState.value,
            accessibilityGuardLastForeignPackage = accessibilityGuardLastForeignPackageState.value,
            accessibilityGuardLastEventType = accessibilityGuardLastEventTypeState.value,
            accessibilityGuardLastDetectedAt = accessibilityGuardLastDetectedAtState.value,
            accessibilityGuardAlarmSeverity = accessibilityGuardAlarmSeverityState.value,
            examSessionStarted = examSessionStarted,
            examGuardArmed = examGuardArmed,
            adminOverridesSummary = adminSettings.overrideSummary(),
            currentKeyboardPackage = flowUiState.currentKeyboardPackage.value,
            isKeyboardAllowed = isKeyboardAllowed,
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            bluetoothPermissionGranted = securityUiState.bluetoothPermissionGranted.value,
            bluetoothEnabled = securityUiState.bluetoothEnabled.value,
            accessibilityServiceEnabled = securityUiState.accessibilityServiceEnabled.value,
            bypassAccessibility = bypassAccessibility,
            adbBypassState = adbBypassState,
            rootBypassState = rootBypassState,
            clipboardSignature = clipboardUiState.clipboardSignature.value,
            clipboardViolationCount = clipboardUiState.clipboardViolationCount.intValue,
            lastClipboardChangeEvent = clipboardUiState.lastClipboardChangeEvent.value,
            networkStatus = runtimeDiagnosticsOps.networkReadinessStatus.examStatus,
            clipboardRuntimeStatus = runtimeDiagnosticsOps.clipboardRuntimeStatus,
            offlineRuntimeStatus = runtimeDiagnosticsOps.offlineRuntimeStatus,
            effectiveLocationPolicySource = runtimeDiagnosticsOps.effectiveLocationPolicySource,
            geofenceViolationCount = flowUiState.geofenceViolationCount.intValue,
            lastGeofenceTrigger = flowUiState.lastGeofenceTrigger.value,
            lastGeofenceAt = flowUiState.lastGeofenceAt.value,
            lastGeofenceContext = flowUiState.lastGeofenceContext.value,
            fakeLocationViolationCount = flowUiState.fakeLocationViolationCount.intValue,
            lastFakeLocationTrigger = flowUiState.lastFakeLocationTrigger.value,
            lastFakeLocationAt = flowUiState.lastFakeLocationAt.value,
            lastFakeLocationContext = flowUiState.lastFakeLocationContext.value,
            screenPinningAvailable = adminUiState.screenPinningAvailable.value,
            screenPinningEnabledInSystem = adminUiState.screenPinningEnabledInSystem.value,
            lockTaskStateBeforePinningRequest = adminUiState.lockTaskStateBeforePinningRequest.value,
            lockTaskStateAfterPinningRequest = adminUiState.lockTaskStateAfterPinningRequest.value,
            screenPinningRequestOutcome = adminUiState.screenPinningRequestOutcome.value,
            screenPinningDialogLikelyShown = adminUiState.screenPinningDialogLikelyShown.value,
            screenPinningUserActionInference = adminUiState.screenPinningUserActionInference.value,
            screenPinningActivationDurationMs = adminUiState.screenPinningActivationDurationMs.value,
            examSessionCancelledByPinningFailure = adminUiState.examSessionCancelledByPinningFailure.value,
            bypassScreenPinning = adminSettings.bypassScreenPinning,
            bypassOverlay = adminSettings.bypassOverlay,
            bypassAppSwitch = adminSettings.bypassAppSwitch,
            bypassDeviceTime = bypassDeviceTime,
            bypassVpn = bypassVpn,
            integrityPublicSummary = securityUiState.integrityPublicSummary.value,
            diagnosticEvents = adminUiState.diagnosticEvents.value,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            lastExamRefreshDecision = securityUiState.lastExamRefreshDecision.value,
            networkReadinessStatus = runtimeDiagnosticsOps.networkReadinessStatus,
            networkUnstableRuntimeStatus = runtimeDiagnosticsOps.networkUnstableRuntimeStatus,
            networkTimelinePreview = runtimeDiagnosticsOps.networkTimelinePreview,
            lastNetworkChangeAt = networkUiState.lastNetworkChangeAt.value,
            lastNetworkChangeSource = networkUiState.lastNetworkChangeSource.value,
            lastConnectedNetworkLabel = networkUiState.lastConnectedNetworkLabel.value,
            bypassScreenRecorder = adminSettings.bypassScreenRecorder,
            bypassDisplayMirror = adminSettings.bypassDisplayMirror,
            bypassMultiWindow = adminSettings.bypassMultiWindow,
            dpcRuntimeStatus = dpcRuntimeStatusProvider(),
            callbacks = ExamRuntimeTelegramReportCallbacks(
                isSendingSection = { adminUiState.sendingSection.value != null },
                setSendingSection = { adminUiState.sendingSection.value = it },
                refreshScreenPinningDiagnostics = ::refreshScreenPinningDiagnostics,
                refreshKeyboardSecurity = { refreshKeyboardSecurity(triggerViolation = false) },
                refreshBluetoothSecurity = { refreshBluetoothSecurity(triggerViolation = false) },
                refreshDeviceIntegritySecurity = {
                    refreshDeviceIntegritySecurity(triggerViolation = false)
                },
                refreshIntegrityGuard = refreshIntegrityGuard,
                refreshRuntimeStaticSecurity = {
                    refreshRuntimeStaticSecurityForSession(
                        context = context,
                        examSessionStarted = examSessionStarted,
                        bypassScreenRecorder = adminSettings.bypassScreenRecorder,
                        bypassDisplayMirror = adminSettings.bypassDisplayMirror,
                        bypassMultiWindow = adminSettings.bypassMultiWindow,
                        securityUiState = securityUiState,
                        trigger = "diagnostic_request",
                        recordAction = runtimeDiagnosticsOps::recordAction,
                        startAlarm = examAlarmController::start,
                        forceRefresh = true
                    )
                },
                refreshDeviceTimeSecurity = {
                    runtimeDiagnosticsOps.refreshDeviceTimeSecurity(
                        trigger = "diagnostic_request",
                        emitDiagnosticEvent = false
                    )
                },
                refreshGeofenceStatus = {
                    runtimeDiagnosticsOps.refreshGeofenceStatus(
                        preferFresh = false,
                        trigger = "diagnostic_request",
                        allowRuntimeViolation = false
                    )
                },
                recordAction = runtimeDiagnosticsOps::recordAction,
                showFeedback = { title, message ->
                    adminUiState.bugReportFeedbackTitle.value = title
                    adminUiState.bugReportFeedbackMessage.value = message
                }
            )
        )
    }
}
