package com.example.coblaxexamlock.runtime

import android.content.Context
import android.os.Build
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.ExamParticipantContext
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.IntegrityCheckResult
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.config.TelegramMessageChunkLimit
import com.example.coblaxexamlock.format.diagnosticSectionEventCodes
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.format.formatElapsedDuration
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TelegramSectionReportDelivery(
    val token: String,
    val chatId: String,
    val chunks: List<String>
)

internal suspend fun sendTelegramSectionReport(
    context: Context,
    section: DiagnosticSection,
    examName: String,
    examUserAgent: String,
    examUserAgentSource: String,
    participantContext: ExamParticipantContext?,
    examSessionStarted: Boolean,
    examRuntimeGuardsArmed: Boolean,
    adminOverridesSummary: String,
    keyboardPackage: String,
    keyboardAllowed: Boolean,
    usingBuiltInExamKeyboard: Boolean,
    bluetoothPermissionGranted: Boolean,
    bluetoothEnabled: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAccessibility: Boolean,
    accessibilityBypassTampered: Boolean,
    adbInspection: AdbInspection,
    adbBypassState: AdbBypassState,
    rootSecurityStatus: RootSecurityStatus,
    rootBypassState: RootBypassState,
    clipboardSignature: String,
    clipboardViolationCount: Int,
    lastClipboardChangeEvent: String,
    networkStatus: ExamNetworkStatus,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    offlineRuntimeStatus: ExamOfflineRuntimeStatus,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    overlayViolationCount: Int,
    overlayRiskResult: OverlayRiskResult,
    overlayBypassTampered: Boolean,
    appSwitchStatus: AppSwitchStatus,
    appSwitchBypassTampered: Boolean,
    screenPinningAvailable: Boolean,
    screenPinningEnabledInSystem: String,
    lockTaskStateBeforePinningRequest: String,
    lockTaskStateAfterPinningRequest: String,
    screenPinningRequestOutcome: String,
    screenPinningDialogLikelyShown: Boolean,
    screenPinningUserActionInference: String,
    screenPinningActivationDurationMs: Long?,
    examSessionCancelledByPinningFailure: Boolean,
    isScreenPinningActive: Boolean,
    bypassScreenPinning: Boolean,
    bypassOverlay: Boolean,
    bypassAppSwitch: Boolean,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    bypassDeviceTime: Boolean,
    bypassVpn: Boolean,
    vpnBypassTampered: Boolean,
    integritySummary: String,
    reverseEngineeringDetected: Boolean = false,
    reverseEngineeringBypass: Boolean = false,
    reverseEngineeringBypassTampered: Boolean = false,
    reverseEngineeringSignals: String = "-",
    apkIntegrityDetected: Boolean = false,
    apkIntegrityBypass: Boolean = false,
    apkIntegrityBypassTampered: Boolean = false,
    integrityIssues: String = "-",
    diagnosticEvents: List<DiagnosticEvent>,
    uiLanguage: UiLanguage,
    healthIntegrityResult: IntegrityCheckResult? = null,
    healthReverseResult: ReverseEngineeringResult? = null,
    healthLastCheckedAt: String? = null,
    webViewCompatibilityStatus: WebViewCompatibilityStatus? = null,
    lastExamRefreshDecision: String? = null,
    networkReadinessStatus: NetworkReadinessStatus? = null,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus? = null,
    networkTimelinePreview: List<NetworkTimelineEntry> = emptyList(),
    lastNetworkChangeAt: String? = null,
    lastNetworkChangeSource: String? = null,
    lastConnectedNetworkLabel: String? = null,
    screenRecorderPackages: List<String> = emptyList(),
    bypassScreenRecorder: Boolean = false,
    screenRecorderBypassTampered: Boolean = false,
    screenRecorderViolationCount: Int = 0,
    screenRecorderDialogActive: Boolean = false,
    externalDisplayDetected: Boolean = false,
    externalDisplayCount: Int = 0,
    bypassDisplayMirror: Boolean = false,
    displayMirrorBypassTampered: Boolean = false,
    displayMirrorViolationCount: Int = 0,
    displayMirrorDialogActive: Boolean = false,
    multiWindowDetected: Boolean = false,
    bypassMultiWindow: Boolean = false,
    multiWindowBypassTampered: Boolean = false,
    multiWindowViolationCount: Int = 0,
    multiWindowDialogActive: Boolean = false,
    compactReport: Boolean = false
): Result<Unit> {
    val deliveryResult = withContext(LowRamDispatchers.detectorIo) {
        runCatching {
        val token = SecureStrings.telegramBotToken.trim()
        val chatId = SecureStrings.telegramBugChatId.trim()

        require(token.isNotBlank()) { "Token Telegram belum dikonfigurasi." }
        require(chatId.isNotBlank()) { "Chat ID Telegram belum dikonfigurasi." }

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME
        val timestamp = diagnosticTimestamp()
        val sectionLabel = diagnosticSectionLabel(section, uiLanguage)
        val deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim()
        val osLabel = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val keyboardRawInputMethod = getCurrentInputMethodRawValue(context)
        val packageInventory = SecurityDetectorCache.readPackageInventory(
            context = context,
            forceRefresh = section == DiagnosticSection.Keyboard
        )
        val keyboardMetadata = SecurityDetectorCache.readPackageMetadata(
            context = context,
            packageName = keyboardPackage,
            forceRefresh = section == DiagnosticSection.Keyboard,
            packageInventory = packageInventory
        )
        val keyboardVersion = keyboardMetadata?.versionName ?: getAppVersionName(context, keyboardPackage)
        val enabledKeyboardPackages = getEnabledInputMethodPackages(context)
        val keyboardSystemApp = keyboardMetadata?.systemOrUpdatedSystemApp ?: isSystemAppPackage(
            context = context,
            packageName = keyboardPackage,
            packageInventory = packageInventory
        )
        val bluetoothAdapterState = getBluetoothAdapterStateLabel(context)
        val bluetoothConnectedDevicesCount = getBluetoothConnectedDevicesCount(context)
        val bluetoothHeadsetConnected = isBluetoothA2dpOrHeadsetConnected(context)
        val accessibilityInspection = inspectAccessibility(context)
        val accessibilityPackages = accessibilityInspection.activePackages
        val allowedAccessibilityPackages = accessibilityInspection.allowedPackages
        val allowedAccessibilityServices = accessibilityInspection.allowedServiceComponents
        val effectiveAccessibilityPackages = accessibilityInspection.effectivePackages
        val accessibilityManagerEnabled = isAccessibilityManagerEnabled(context)
        val touchExplorationEnabled = isTouchExplorationEnabled(context)
        val accessibilityRawValue = accessibilityInspection.rawEnabledServices
        val riskyAccessibilityPackages = accessibilityInspection.riskyPackages
        val usbConnected = isUsbConnected(context)
        val installSource = getInstallSourceSummary(context)
        val appDebuggable = isAppDebuggable(context)
        val virtualEnvironmentDiagnostics =
            if (section == DiagnosticSection.VirtualEnvironment) {
                getVirtualEnvironmentDiagnostics(context, forceRefresh = true)
            } else {
                null
            }
        val clipboardDiagnostics = getClipboardDiagnostics(context)
        val signatureIntegrityResult =
            if (section == DiagnosticSection.Signature) {
                val expectedFingerprints = resolveExpectedSigningFingerprints(
                    isDebugBuild = BuildConfig.DEBUG,
                    releaseFingerprint = SecureStrings.signingFingerprintRelease,
                    debugFingerprint = SecureStrings.signingFingerprintDebug
                )
                SignatureIntegrity.check(context, expectedFingerprints)
            } else {
                null
            }
        val relevantEventCodes = diagnosticSectionEventCodes(section)
        val maxEvents = if (compactReport) 6 else 12
        val relevantEvents = diagnosticEvents.filter { it.code in relevantEventCodes }.take(maxEvents)
        val message = buildString {
            appendLine("DIAGNOSTIK CBX LOCK - $sectionLabel")
            appendLine("Waktu: $timestamp")
            appendLine("Ujian: ${examName.ifBlank { "-" }}")
            appendLine("Sesi ujian dimulai: ${if (examSessionStarted) "Ya" else "Belum"}")
            appendLine("Runtime guards armed: ${if (examRuntimeGuardsArmed) "Ya" else "Tidak"}")
            appendLine("App version: $versionName")
            appendLine("Perangkat: ${deviceLabel.ifBlank { "-" }}")
            appendLine("OS: $osLabel")
            appendLine("Admin overrides: $adminOverridesSummary")
            appendLine("IntegrityGuard: ${integritySummary.ifBlank { "-" }}")
            appendLine(
                "Reverse engineering: detected=${telegramYesNo(reverseEngineeringDetected || healthReverseResult?.tamperDetected == true)} " +
                    "bypass=${telegramYesNo(reverseEngineeringBypass)} " +
                    "tampered=${telegramYesNo(reverseEngineeringBypassTampered)} " +
                    "signals=${reverseEngineeringSignals.ifBlank { "-" }}"
            )
            appendLine(
                "APK integrity: detected=${telegramYesNo(apkIntegrityDetected || healthIntegrityResult?.ok == false || integrityIssues.isNotBlank() && integrityIssues != "-")} " +
                    "bypass=${telegramYesNo(apkIntegrityBypass)} " +
                    "tampered=${telegramYesNo(apkIntegrityBypassTampered)} " +
                    "issues=${integrityIssues.ifBlank { "-" }}"
            )
            participantContext?.appendTelegramLines(this)
            appendLine()

            appendTelegramSectionDetails(
                details = TelegramSectionDetailsContext(
                    context = context,
                    section = section,
                    examUserAgent = examUserAgent,
                    examUserAgentSource = examUserAgentSource,
                    keyboardPackage = keyboardPackage,
                    keyboardAllowed = keyboardAllowed,
                    usingBuiltInExamKeyboard = usingBuiltInExamKeyboard,
                    keyboardRawInputMethod = keyboardRawInputMethod,
                    keyboardVersion = keyboardVersion,
                    enabledKeyboardPackages = enabledKeyboardPackages,
                    keyboardSystemApp = keyboardSystemApp,
                    bluetoothPermissionGranted = bluetoothPermissionGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    bluetoothAdapterState = bluetoothAdapterState,
                    bluetoothConnectedDevicesCount = bluetoothConnectedDevicesCount,
                    bluetoothHeadsetConnected = bluetoothHeadsetConnected,
                    accessibilityServiceEnabled = accessibilityServiceEnabled,
                    bypassAccessibility = bypassAccessibility,
                    accessibilityBypassTampered = accessibilityBypassTampered,
                    accessibilityInspection = accessibilityInspection,
                    accessibilityManagerEnabled = accessibilityManagerEnabled,
                    touchExplorationEnabled = touchExplorationEnabled,
                    accessibilityPackages = accessibilityPackages,
                    accessibilityRawValue = accessibilityRawValue,
                    allowedAccessibilityServices = allowedAccessibilityServices,
                    allowedAccessibilityPackages = allowedAccessibilityPackages,
                    effectiveAccessibilityPackages = effectiveAccessibilityPackages,
                    riskyAccessibilityPackages = riskyAccessibilityPackages,
                    bypassOverlay = bypassOverlay,
                    overlayBypassTampered = overlayBypassTampered,
                    overlayRiskResult = overlayRiskResult,
                    overlayViolationCount = overlayViolationCount,
                    geofenceRuntimeStatus = geofenceRuntimeStatus,
                    fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
                    bypassAppSwitch = bypassAppSwitch,
                    appSwitchBypassTampered = appSwitchBypassTampered,
                    appSwitchStatus = appSwitchStatus,
                    adbBypassState = adbBypassState,
                    adbInspection = adbInspection,
                    usbConnected = usbConnected,
                    installSource = installSource,
                    appDebuggable = appDebuggable,
                    rootBypassState = rootBypassState,
                    rootSecurityStatus = rootSecurityStatus,
                    signatureIntegrityResult = signatureIntegrityResult,
                    virtualEnvironmentDiagnostics = virtualEnvironmentDiagnostics,
                    clipboardDiagnostics = clipboardDiagnostics,
                    clipboardSignature = clipboardSignature,
                    clipboardViolationCount = clipboardViolationCount,
                    lastClipboardChangeEvent = lastClipboardChangeEvent,
                    clipboardRuntimeStatus = clipboardRuntimeStatus,
                    screenPinningAvailable = screenPinningAvailable,
                    screenPinningEnabledInSystem = screenPinningEnabledInSystem,
                    lockTaskStateBeforePinningRequest = lockTaskStateBeforePinningRequest,
                    lockTaskStateAfterPinningRequest = lockTaskStateAfterPinningRequest,
                    screenPinningRequestOutcome = screenPinningRequestOutcome,
                    screenPinningDialogLikelyShown = screenPinningDialogLikelyShown,
                    screenPinningUserActionInference = screenPinningUserActionInference,
                    screenPinningActivationDurationMs = screenPinningActivationDurationMs,
                    examSessionCancelledByPinningFailure = examSessionCancelledByPinningFailure,
                    isScreenPinningActive = isScreenPinningActive,
                    bypassScreenPinning = bypassScreenPinning,
                    integritySummary = integritySummary,
                    networkStatus = networkStatus,
                    offlineRuntimeStatus = offlineRuntimeStatus,
                    deviceTimeSecurityStatus = deviceTimeSecurityStatus,
                    bypassDeviceTime = bypassDeviceTime,
                    bypassVpn = bypassVpn,
                    vpnBypassTampered = vpnBypassTampered,
                    uiLanguage = uiLanguage,
                    healthIntegrityResult = healthIntegrityResult,
                    healthReverseResult = healthReverseResult,
                    healthLastCheckedAt = healthLastCheckedAt,
                    webViewCompatibilityStatus = webViewCompatibilityStatus,
                    lastExamRefreshDecision = lastExamRefreshDecision,
                    networkReadinessStatus = networkReadinessStatus,
                    networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
                    networkTimelinePreview = networkTimelinePreview,
                    lastNetworkChangeAt = lastNetworkChangeAt,
                    lastNetworkChangeSource = lastNetworkChangeSource,
                    lastConnectedNetworkLabel = lastConnectedNetworkLabel,
                    screenRecorderPackages = screenRecorderPackages,
                    bypassScreenRecorder = bypassScreenRecorder,
                    screenRecorderBypassTampered = screenRecorderBypassTampered,
                    screenRecorderViolationCount = screenRecorderViolationCount,
                    screenRecorderDialogActive = screenRecorderDialogActive,
                    externalDisplayDetected = externalDisplayDetected,
                    externalDisplayCount = externalDisplayCount,
                    bypassDisplayMirror = bypassDisplayMirror,
                    displayMirrorBypassTampered = displayMirrorBypassTampered,
                    displayMirrorViolationCount = displayMirrorViolationCount,
                    displayMirrorDialogActive = displayMirrorDialogActive,
                    multiWindowDetected = multiWindowDetected,
                    bypassMultiWindow = bypassMultiWindow,
                    multiWindowBypassTampered = multiWindowBypassTampered,
                    multiWindowViolationCount = multiWindowViolationCount,
                    multiWindowDialogActive = multiWindowDialogActive
                )
            )

            appendLine()
            appendLine("[LAST ACTION LOG]")
            if (relevantEvents.isEmpty()) {
                appendLine("-")
            } else {
                relevantEvents.forEach { appendLine(formatDiagnosticEvent(it)) }
            }
        }

            TelegramSectionReportDelivery(
                token = token,
                chatId = chatId,
                chunks = buildTelegramMessageChunks(message)
            )
        }
    }

    return deliveryResult.fold(
        onSuccess = { delivery ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val queue = TelegramMessageQueueHolder.instance
                    delivery.chunks.forEach { chunk ->
                        queue.send(
                            token = delivery.token,
                            chatId = delivery.chatId,
                            message = chunk
                        )
                    }
                }
            }
        },
        onFailure = { throwable -> Result.failure(throwable) }
    )
}

private fun telegramYesNo(value: Boolean): String = if (value) "Ya" else "Tidak"
