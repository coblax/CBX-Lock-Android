package com.example.coblaxexamlock.ui.exam

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbStore
import com.example.coblaxexamlock.ScreenPinningPlatformBridge
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.buildDeviceSurvivalPolicy
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.launchPlatformIntentSafely
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.directLinkLocationPolicy
import com.example.coblaxexamlock.runtime.readExamBatteryStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatus
import com.example.coblaxexamlock.ui.admin.FieldReadinessReport
import com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot
import java.io.File
import java.net.URI
import java.util.Locale

private const val MaxDiagnosticSnapshotEvents = 20

internal data class ExamDiagnosticSnapshotEvent(
    val timestamp: String,
    val level: String,
    val code: String,
    val screen: String,
    val appElapsedMs: Long,
    val sessionElapsedMs: Long?,
    val details: String
)

internal data class ExamDiagnosticSnapshot(
    val generatedAt: String,
    val source: String,
    val appVersionName: String,
    val versionCode: Int,
    val buildType: String,
    val lowRamEnabled: Boolean,
    val lowRamSevere: Boolean,
    val lowRamUltra: Boolean,
    val lowRamTotalMemoryMb: Long?,
    val lowRamAvailableMemoryMb: Long?,
    val lowRamMemoryLow: Boolean,
    val lowRamOverride: String,
    val lowRamDetectedProfile: String,
    val lowRamEffectiveProfile: String,
    val qrMaxEdgePx: Int,
    val slowPollingMultiplier: Int,
    val compatibilityFamily: String,
    val compatibilityLabel: String,
    val compatibilityManufacturer: String,
    val compatibilityBrand: String,
    val compatibilityModel: String,
    val compatibilitySdkInt: Int,
    val screenPinningSkipRequestWhenAlreadyActive: Boolean,
    val survivalScore: String,
    val survivalPolicySummary: String,
    val previousSessionBreadcrumbSummary: String,
    val previousSessionRecoveryHint: String?,
    val examName: String,
    val examUrlHost: String,
    val examSessionStarted: Boolean,
    val examGuardArmed: Boolean,
    val webViewPresent: Boolean,
    val webViewProviderAvailable: Boolean,
    val webViewProviderPackage: String,
    val webViewProviderVersion: String,
    val webViewProviderMajor: Int?,
    val webViewProviderOutdatedLikely: Boolean,
    val webViewProviderSource: String,
    val webViewProviderHealthVerdict: String,
    val webViewProviderHealthSeverity: String,
    val webViewProviderRiskLabel: String,
    val webViewProviderQuickFix: String?,
    val webViewError: String?,
    val rendererGone: Boolean,
    val recoveryState: String,
    val lastTrimMemoryAction: String?,
    val networkVerdict: String,
    val networkUserVerdict: String,
    val networkTransport: String,
    val networkValidated: Boolean,
    val networkCaptivePortal: Boolean,
    val networkDnsProbe: String,
    val networkDnsProbeHost: String,
    val networkDnsLatencyBucket: String,
    val networkQuickFix: String?,
    val geofenceVerdict: String,
    val geofenceEnabled: Boolean,
    val geofencePolicySource: String,
    val geofenceViolationCount: Int,
    val fakeLocationVerdict: String,
    val fakeLocationViolationCount: Int,
    val deviceTimeVerdict: String,
    val deviceTimeBlocking: Boolean,
    val lastPinningDecision: String?,
    val lastOverlayDecision: String?,
    val lastRefreshDecision: String?,
    val preExamHealthSummary: String?,
    val preExamHealthItems: List<String>,
    val events: List<ExamDiagnosticSnapshotEvent>
) {
    fun toJsonString(): String = jsonObject(
        "generatedAt" to generatedAt,
        "source" to source,
        "app" to mapOf(
            "versionName" to appVersionName,
            "versionCode" to versionCode,
            "buildType" to buildType
        ),
        "lowRam" to mapOf(
            "enabled" to lowRamEnabled,
            "severe" to lowRamSevere,
            "ultra" to lowRamUltra,
            "totalMemoryMb" to lowRamTotalMemoryMb,
            "availableMemoryMb" to lowRamAvailableMemoryMb,
            "memoryLow" to lowRamMemoryLow,
            "override" to lowRamOverride,
            "detectedProfile" to lowRamDetectedProfile,
            "effectiveProfile" to lowRamEffectiveProfile,
            "qrMaxEdgePx" to qrMaxEdgePx,
            "slowPollingMultiplier" to slowPollingMultiplier
        ),
        "compatibility" to mapOf(
            "family" to compatibilityFamily,
            "label" to compatibilityLabel,
            "manufacturer" to compatibilityManufacturer,
            "brand" to compatibilityBrand,
            "model" to compatibilityModel,
            "sdkInt" to compatibilitySdkInt,
            "screenPinningSkipRequestWhenAlreadyActive" to screenPinningSkipRequestWhenAlreadyActive
        ),
        "survival" to mapOf(
            "score" to survivalScore,
            "summary" to survivalPolicySummary
        ),
        "previousSession" to mapOf(
            "breadcrumbSummary" to previousSessionBreadcrumbSummary,
            "recoveryHint" to previousSessionRecoveryHint
        ),
        "exam" to mapOf(
            "name" to examName,
            "urlHost" to examUrlHost,
            "sessionStarted" to examSessionStarted,
            "guardArmed" to examGuardArmed
        ),
        "webView" to mapOf(
            "present" to webViewPresent,
            "providerAvailable" to webViewProviderAvailable,
            "providerPackage" to webViewProviderPackage,
            "providerVersion" to webViewProviderVersion,
            "providerMajor" to webViewProviderMajor,
            "providerOutdatedLikely" to webViewProviderOutdatedLikely,
            "providerSource" to webViewProviderSource,
            "healthVerdict" to webViewProviderHealthVerdict,
            "healthSeverity" to webViewProviderHealthSeverity,
            "riskLabel" to webViewProviderRiskLabel,
            "quickFix" to webViewProviderQuickFix,
            "error" to webViewError,
            "rendererGone" to rendererGone,
            "recoveryState" to recoveryState,
            "lastTrimMemoryAction" to lastTrimMemoryAction
        ),
        "network" to mapOf(
            "verdict" to networkVerdict,
            "userVerdict" to networkUserVerdict,
            "transport" to networkTransport,
            "validated" to networkValidated,
            "captivePortal" to networkCaptivePortal,
            "dnsProbe" to networkDnsProbe,
            "dnsProbeHost" to networkDnsProbeHost,
            "dnsLatencyBucket" to networkDnsLatencyBucket,
            "quickFix" to networkQuickFix
        ),
        "location" to mapOf(
            "geofenceVerdict" to geofenceVerdict,
            "geofenceEnabled" to geofenceEnabled,
            "geofencePolicySource" to geofencePolicySource,
            "geofenceViolationCount" to geofenceViolationCount,
            "fakeLocationVerdict" to fakeLocationVerdict,
            "fakeLocationViolationCount" to fakeLocationViolationCount
        ),
        "deviceTime" to mapOf(
            "verdict" to deviceTimeVerdict,
            "blocking" to deviceTimeBlocking
        ),
        "runtimeDecisions" to mapOf(
            "lastPinningDecision" to lastPinningDecision,
            "lastOverlayDecision" to lastOverlayDecision,
            "lastRefreshDecision" to lastRefreshDecision
        ),
        "preExamHealth" to mapOf(
            "summary" to preExamHealthSummary,
            "items" to preExamHealthItems
        ),
        "events" to events.map { event ->
            mapOf(
                "timestamp" to event.timestamp,
                "level" to event.level,
                "code" to event.code,
                "screen" to event.screen,
                "appElapsedMs" to event.appElapsedMs,
                "sessionElapsedMs" to event.sessionElapsedMs,
                "details" to event.details
            )
        }
    )

    fun toTextString(): String = buildString {
        appendLine("CBX Exam Lock Diagnostic Snapshot")
        appendLine("Generated: $generatedAt")
        appendLine("Source: $source")
        appendLine("App: $appVersionName ($versionCode) $buildType")
        appendLine(
            "Low RAM: enabled=$lowRamEnabled severe=$lowRamSevere ultra=$lowRamUltra " +
                "avail=${lowRamAvailableMemoryMb ?: "-"}MB total=${lowRamTotalMemoryMb ?: "-"}MB " +
                "memoryLow=$lowRamMemoryLow override=$lowRamOverride detected=$lowRamDetectedProfile " +
                "effective=$lowRamEffectiveProfile qrMaxEdgePx=$qrMaxEdgePx polling=${slowPollingMultiplier}x"
        )
        appendLine("Compatibility: family=$compatibilityFamily label=$compatibilityLabel model=$compatibilityManufacturer/$compatibilityBrand/$compatibilityModel sdk=$compatibilitySdkInt skipPinningIfActive=$screenPinningSkipRequestWhenAlreadyActive")
        appendLine("Device survival: score=$survivalScore summary=$survivalPolicySummary")
        appendLine("Previous session: trail=$previousSessionBreadcrumbSummary recovery=${previousSessionRecoveryHint ?: "-"}")
        appendLine("Exam: ${examName.ifBlank { "-" }} host=$examUrlHost started=$examSessionStarted guard=$examGuardArmed")
        appendLine("WebView: present=$webViewPresent rendererGone=$rendererGone recovery=$recoveryState error=${webViewError ?: "-"}")
        appendLine("WebView provider: verdict=$webViewProviderHealthVerdict severity=$webViewProviderHealthSeverity available=$webViewProviderAvailable package=$webViewProviderPackage version=${webViewProviderVersion.ifBlank { "-" }} major=${webViewProviderMajor ?: "-"} old=$webViewProviderOutdatedLikely source=$webViewProviderSource risk=$webViewProviderRiskLabel")
        appendLine("WebView provider fix: ${webViewProviderQuickFix ?: "-"}")
        appendLine("Memory trim: ${lastTrimMemoryAction ?: "-"}")
        appendLine("Network: verdict=$networkVerdict user=$networkUserVerdict transport=$networkTransport validated=$networkValidated captivePortal=$networkCaptivePortal")
        appendLine("DNS: $networkDnsProbe host=$networkDnsProbeHost latency=$networkDnsLatencyBucket")
        appendLine("Network fix: ${networkQuickFix ?: "-"}")
        appendLine("Geofence: verdict=$geofenceVerdict enabled=$geofenceEnabled source=$geofencePolicySource violations=$geofenceViolationCount")
        appendLine("Fake location: verdict=$fakeLocationVerdict violations=$fakeLocationViolationCount")
        appendLine("Device time: verdict=$deviceTimeVerdict blocking=$deviceTimeBlocking")
        appendLine("Last pinning decision: ${lastPinningDecision ?: "-"}")
        appendLine("Last overlay decision: ${lastOverlayDecision ?: "-"}")
        appendLine("Last refresh decision: ${lastRefreshDecision ?: "-"}")
        appendLine("Pre-exam health: ${preExamHealthSummary ?: "-"}")
        preExamHealthItems.forEach { item ->
            appendLine("- $item")
        }
        appendLine()
        appendLine("Last ${events.size} diagnostic events")
        events.forEach { event ->
            appendLine("- ${event.timestamp} [${event.level}] ${event.code} ${event.screen}: ${event.details}")
        }
    }
}

internal data class ExamDiagnosticSnapshotInput(
    val source: String,
    val lowRamProfile: LowRamProfile,
    val deviceCompatibilityProfile: DeviceCompatibilityProfile,
    val deviceSurvivalPolicy: DeviceSurvivalPolicy?,
    val previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb?,
    val payload: ExamQrPayload?,
    val examSessionStarted: Boolean,
    val examGuardArmed: Boolean,
    val webViewPresent: Boolean,
    val webViewCompatibilityStatus: WebViewCompatibilityStatus? = null,
    val webViewError: String?,
    val rendererGone: Boolean,
    val recoveryState: ExamRuntimeRecoveryState,
    val lastTrimMemoryAction: String?,
    val networkReadinessStatus: NetworkReadinessStatus,
    val geofenceRuntimeStatus: GeofenceRuntimeStatus?,
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus?,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus?,
    val preExamHealthSnapshot: PreExamHealthSnapshot?,
    val lastPinningDecision: String?,
    val lastOverlayDecision: String?,
    val lastRefreshDecision: String? = null,
    val diagnosticEvents: List<DiagnosticEvent>
)

internal fun buildExamDiagnosticSnapshot(input: ExamDiagnosticSnapshotInput): ExamDiagnosticSnapshot {
    val network = input.networkReadinessStatus
    val compatibility = input.deviceCompatibilityProfile
    val webViewStatus = input.webViewCompatibilityStatus ?: WebViewCompatibilityStatus(
        available = input.webViewPresent || !input.rendererGone,
        packageName = "-",
        versionName = "",
        majorVersion = null,
        outdatedLikely = false,
        providerSource = "runtime_snapshot",
        quickFix = null
    )
    val survivalPolicy = input.deviceSurvivalPolicy ?: buildDeviceSurvivalPolicy(
        lowRamProfile = input.lowRamProfile,
        deviceCompatibilityProfile = compatibility,
        webViewCompatibilityStatus = webViewStatus,
        preExamHealthSnapshot = input.preExamHealthSnapshot
    )
    val previousSession = input.previousExamSessionBreadcrumb ?: PreviousExamSessionBreadcrumb(emptyList())
    return ExamDiagnosticSnapshot(
        generatedAt = diagnosticTimestamp(),
        source = input.source,
        appVersionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        lowRamEnabled = input.lowRamProfile.enabled,
        lowRamSevere = input.lowRamProfile.severe,
        lowRamUltra = input.lowRamProfile.ultra,
        lowRamTotalMemoryMb = input.lowRamProfile.totalMemoryMb,
        lowRamAvailableMemoryMb = input.lowRamProfile.availableMemoryMb,
        lowRamMemoryLow = input.lowRamProfile.memoryLow,
        lowRamOverride = input.lowRamProfile.lowRamOverride.name,
        lowRamDetectedProfile = input.lowRamProfile.detectedTier?.name ?: input.lowRamProfile.tier.name,
        lowRamEffectiveProfile = input.lowRamProfile.tier.name,
        qrMaxEdgePx = input.lowRamProfile.qrMaxEdgePx,
        slowPollingMultiplier = input.lowRamProfile.slowPollingMultiplier,
        compatibilityFamily = compatibility.family.name,
        compatibilityLabel = compatibility.vendorDisplayName,
        compatibilityManufacturer = compatibility.manufacturer,
        compatibilityBrand = compatibility.brand,
        compatibilityModel = compatibility.model,
        compatibilitySdkInt = compatibility.sdkInt,
        screenPinningSkipRequestWhenAlreadyActive = compatibility.skipScreenPinningRequestWhenAlreadyActive,
        survivalScore = survivalPolicy.score.name,
        survivalPolicySummary = redactDiagnosticDetail(survivalPolicy.diagnosticSummary()),
        previousSessionBreadcrumbSummary = redactDiagnosticDetail(previousSession.diagnosticSummary()),
        previousSessionRecoveryHint = previousSession.latestRecoveryHint?.let(::redactDiagnosticDetail),
        examName = redactFreeText(input.payload?.examName.orEmpty()),
        examUrlHost = redactUrlToHost(input.payload?.examUrl),
        examSessionStarted = input.examSessionStarted,
        examGuardArmed = input.examGuardArmed,
        webViewPresent = input.webViewPresent,
        webViewProviderAvailable = webViewStatus.available,
        webViewProviderPackage = webViewStatus.packageName,
        webViewProviderVersion = webViewStatus.versionName,
        webViewProviderMajor = webViewStatus.majorVersion,
        webViewProviderOutdatedLikely = webViewStatus.outdatedLikely,
        webViewProviderSource = webViewStatus.providerSource,
        webViewProviderHealthVerdict = webViewStatus.verdict.name,
        webViewProviderHealthSeverity = webViewStatus.severity.name,
        webViewProviderRiskLabel = redactDiagnosticDetail(webViewStatus.riskLabel),
        webViewProviderQuickFix = webViewStatus.quickFix?.let(::redactDiagnosticDetail),
        webViewError = input.webViewError?.let(::redactDiagnosticDetail),
        rendererGone = input.rendererGone,
        recoveryState = input.recoveryState.name,
        lastTrimMemoryAction = input.lastTrimMemoryAction?.let(::redactDiagnosticDetail),
        networkVerdict = network.verdict.name,
        networkUserVerdict = network.userFacingVerdict.name,
        networkTransport = network.transportLabel.ifBlank { "-" },
        networkValidated = network.diagnostics.isValidated,
        networkCaptivePortal = network.diagnostics.isCaptivePortal,
        networkDnsProbe = network.dnsProbeStatus.verdict.name,
        networkDnsProbeHost = redactUrlToHost(network.dnsProbeStatus.host),
        networkDnsLatencyBucket = network.dnsProbeStatus.latencyBucket.name,
        networkQuickFix = network.userFacingQuickFixText?.let(::redactDiagnosticDetail),
        geofenceVerdict = input.geofenceRuntimeStatus?.securityStatus?.finalVerdict?.name ?: "-",
        geofenceEnabled = input.geofenceRuntimeStatus?.evaluation?.enabled == true,
        geofencePolicySource = input.geofenceRuntimeStatus?.policySource?.name ?: "-",
        geofenceViolationCount = input.geofenceRuntimeStatus?.violationCount ?: 0,
        fakeLocationVerdict = input.fakeLocationRuntimeStatus?.securityStatus?.finalVerdict?.name ?: "-",
        fakeLocationViolationCount = input.fakeLocationRuntimeStatus?.violationCount ?: 0,
        deviceTimeVerdict = input.deviceTimeSecurityStatus?.finalVerdict?.name ?: "-",
        deviceTimeBlocking = input.deviceTimeSecurityStatus?.blocking == true,
        lastPinningDecision = input.lastPinningDecision?.let(::redactDiagnosticDetail),
        lastOverlayDecision = input.lastOverlayDecision?.let(::redactDiagnosticDetail),
        lastRefreshDecision = input.lastRefreshDecision?.let(::redactDiagnosticDetail),
        preExamHealthSummary = input.preExamHealthSnapshot?.diagnosticSummary()?.let(::redactDiagnosticDetail),
        preExamHealthItems = input.preExamHealthSnapshot?.items.orEmpty().map { item ->
            redactDiagnosticDetail("${item.category.name}:${item.verdict.name}:${item.title}:${item.detail}")
        },
        events = input.diagnosticEvents
            .take(MaxDiagnosticSnapshotEvents)
            .map(::toSnapshotEvent)
    )
}

internal fun buildAdminExamDiagnosticSnapshot(
    context: Context,
    settings: AdminSettings,
    lowRamProfile: LowRamProfile,
    deviceCompatibilityProfile: DeviceCompatibilityProfile
): ExamDiagnosticSnapshot {
    val networkStatus = readNetworkReadinessStatus(context)
    val directLinkPolicy = settings.directLinkLocationPolicy()
    val webViewStatus = com.example.coblaxexamlock.readWebViewCompatibilityStatus(context)
    val survivalPolicy = buildDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewStatus
    )
    val previousSession = PreviousExamSessionBreadcrumbStore.read(context)
    return ExamDiagnosticSnapshot(
        generatedAt = diagnosticTimestamp(),
        source = "secret_admin_security",
        appVersionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        lowRamEnabled = lowRamProfile.enabled,
        lowRamSevere = lowRamProfile.severe,
        lowRamUltra = lowRamProfile.ultra,
        lowRamTotalMemoryMb = lowRamProfile.totalMemoryMb,
        lowRamAvailableMemoryMb = lowRamProfile.availableMemoryMb,
        lowRamMemoryLow = lowRamProfile.memoryLow,
        lowRamOverride = lowRamProfile.lowRamOverride.name,
        lowRamDetectedProfile = lowRamProfile.detectedTier?.name ?: lowRamProfile.tier.name,
        lowRamEffectiveProfile = lowRamProfile.tier.name,
        qrMaxEdgePx = lowRamProfile.qrMaxEdgePx,
        slowPollingMultiplier = lowRamProfile.slowPollingMultiplier,
        compatibilityFamily = deviceCompatibilityProfile.family.name,
        compatibilityLabel = deviceCompatibilityProfile.vendorDisplayName,
        compatibilityManufacturer = deviceCompatibilityProfile.manufacturer,
        compatibilityBrand = deviceCompatibilityProfile.brand,
        compatibilityModel = deviceCompatibilityProfile.model,
        compatibilitySdkInt = deviceCompatibilityProfile.sdkInt,
        screenPinningSkipRequestWhenAlreadyActive =
            deviceCompatibilityProfile.skipScreenPinningRequestWhenAlreadyActive,
        survivalScore = survivalPolicy.score.name,
        survivalPolicySummary = redactDiagnosticDetail(survivalPolicy.diagnosticSummary()),
        previousSessionBreadcrumbSummary = redactDiagnosticDetail(previousSession.diagnosticSummary()),
        previousSessionRecoveryHint = previousSession.latestRecoveryHint?.let(::redactDiagnosticDetail),
        examName = redactFreeText(settings.fastExamLabel),
        examUrlHost = redactUrlToHost(settings.fastExamUrl),
        examSessionStarted = false,
        examGuardArmed = false,
        webViewPresent = false,
        webViewProviderAvailable = webViewStatus.available,
        webViewProviderPackage = webViewStatus.packageName,
        webViewProviderVersion = webViewStatus.versionName,
        webViewProviderMajor = webViewStatus.majorVersion,
        webViewProviderOutdatedLikely = webViewStatus.outdatedLikely,
        webViewProviderSource = webViewStatus.providerSource,
        webViewProviderHealthVerdict = webViewStatus.verdict.name,
        webViewProviderHealthSeverity = webViewStatus.severity.name,
        webViewProviderRiskLabel = redactDiagnosticDetail(webViewStatus.riskLabel),
        webViewProviderQuickFix = webViewStatus.quickFix?.let(::redactDiagnosticDetail),
        webViewError = null,
        rendererGone = false,
        recoveryState = ExamRuntimeRecoveryState.Idle.name,
        lastTrimMemoryAction = null,
        networkVerdict = networkStatus.verdict.name,
        networkUserVerdict = networkStatus.userFacingVerdict.name,
        networkTransport = networkStatus.transportLabel,
        networkValidated = networkStatus.diagnostics.isValidated,
        networkCaptivePortal = networkStatus.diagnostics.isCaptivePortal,
        networkDnsProbe = "NotRun",
        networkDnsProbeHost = "-",
        networkDnsLatencyBucket = "Unknown",
        networkQuickFix = networkStatus.userFacingQuickFixText,
        geofenceVerdict = "-",
        geofenceEnabled = directLinkPolicy?.geofenceEnabled == true,
        geofencePolicySource = if (settings.directLinkLocationPolicySaved) "DirectLinkSaved" else "-",
        geofenceViolationCount = 0,
        fakeLocationVerdict = "-",
        fakeLocationViolationCount = 0,
        deviceTimeVerdict = "-",
        deviceTimeBlocking = false,
        lastPinningDecision = "admin_export_no_active_exam",
        lastOverlayDecision = "admin_export_no_active_exam",
        lastRefreshDecision = "admin_export_no_active_exam",
        preExamHealthSummary = null,
        preExamHealthItems = emptyList(),
        events = emptyList()
    )
}

internal data class ExamDeviceFieldReport(
    val generatedAt: String,
    val source: String,
    val appVersionName: String,
    val versionCode: Int,
    val buildType: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val sdkInt: Int,
    val lowRamEnabled: Boolean,
    val lowRamSevere: Boolean,
    val lowRamUltra: Boolean,
    val lowRamTotalMemoryMb: Long?,
    val lowRamAvailableMemoryMb: Long?,
    val lowRamMemoryLow: Boolean,
    val lowRamOverride: String,
    val lowRamDetectedProfile: String,
    val lowRamEffectiveProfile: String,
    val qrMaxEdgePx: Int,
    val slowPollingMultiplier: Int,
    val compatibilityFamily: String,
    val compatibilityLabel: String,
    val compatibilitySummary: String,
    val survivalScore: String,
    val survivalPolicySummary: String,
    val survivalRecommendedActions: List<String>,
    val previousSessionBreadcrumbSummary: String,
    val previousSessionRecoveryHint: String?,
    val screenPinningSystemSetting: String,
    val lockTaskState: String,
    val overlayPolicy: String,
    val webViewAvailable: Boolean,
    val webViewPackage: String,
    val webViewVersion: String,
    val webViewMajor: Int? = null,
    val webViewOutdatedLikely: Boolean,
    val webViewProviderSource: String,
    val webViewHealthVerdict: String = "Unknown",
    val webViewHealthSeverity: String = "Warning",
    val webViewRiskLabel: String = "WebView provider version unknown",
    val webViewQuickFix: String? = null,
    val networkVerdict: String,
    val networkUserVerdict: String,
    val networkTransport: String,
    val networkValidated: Boolean,
    val networkCaptivePortal: Boolean,
    val networkDnsProbe: String,
    val batteryLevelPercent: Int,
    val batteryCharging: Boolean,
    val directLinkHost: String,
    val directLinkGeofenceEnabled: Boolean,
    val preExamHealthSummary: String?,
    val fieldReadinessVerdict: String?,
    val fieldReadinessSummary: String?,
    val fieldReadinessItems: List<String>
) {
    fun toJsonString(): String = jsonObject(
        "generatedAt" to generatedAt,
        "source" to source,
        "app" to mapOf(
            "versionName" to appVersionName,
            "versionCode" to versionCode,
            "buildType" to buildType
        ),
        "device" to mapOf(
            "manufacturer" to manufacturer,
            "brand" to brand,
            "model" to model,
            "sdkInt" to sdkInt
        ),
        "lowRam" to mapOf(
            "enabled" to lowRamEnabled,
            "severe" to lowRamSevere,
            "ultra" to lowRamUltra,
            "totalMemoryMb" to lowRamTotalMemoryMb,
            "availableMemoryMb" to lowRamAvailableMemoryMb,
            "memoryLow" to lowRamMemoryLow,
            "override" to lowRamOverride,
            "detectedProfile" to lowRamDetectedProfile,
            "effectiveProfile" to lowRamEffectiveProfile,
            "qrMaxEdgePx" to qrMaxEdgePx,
            "slowPollingMultiplier" to slowPollingMultiplier
        ),
        "compatibility" to mapOf(
            "family" to compatibilityFamily,
            "label" to compatibilityLabel,
            "summary" to compatibilitySummary
        ),
        "survival" to mapOf(
            "score" to survivalScore,
            "summary" to survivalPolicySummary,
            "recommendedActions" to survivalRecommendedActions
        ),
        "previousSession" to mapOf(
            "breadcrumbSummary" to previousSessionBreadcrumbSummary,
            "recoveryHint" to previousSessionRecoveryHint
        ),
        "screenPinning" to mapOf(
            "systemSetting" to screenPinningSystemSetting,
            "lockTaskState" to lockTaskState
        ),
        "overlay" to mapOf(
            "policy" to overlayPolicy
        ),
        "webView" to mapOf(
            "available" to webViewAvailable,
            "package" to webViewPackage,
            "version" to webViewVersion,
            "major" to webViewMajor,
            "outdatedLikely" to webViewOutdatedLikely,
            "providerSource" to webViewProviderSource,
            "healthVerdict" to webViewHealthVerdict,
            "healthSeverity" to webViewHealthSeverity,
            "riskLabel" to webViewRiskLabel,
            "quickFix" to webViewQuickFix
        ),
        "network" to mapOf(
            "verdict" to networkVerdict,
            "userVerdict" to networkUserVerdict,
            "transport" to networkTransport,
            "validated" to networkValidated,
            "captivePortal" to networkCaptivePortal,
            "dnsProbe" to networkDnsProbe
        ),
        "battery" to mapOf(
            "levelPercent" to batteryLevelPercent,
            "charging" to batteryCharging
        ),
        "directLink" to mapOf(
            "host" to directLinkHost,
            "geofenceEnabled" to directLinkGeofenceEnabled
        ),
        "preExamHealth" to mapOf(
            "summary" to preExamHealthSummary
        ),
        "fieldReadiness" to mapOf(
            "verdict" to fieldReadinessVerdict,
            "summary" to fieldReadinessSummary,
            "items" to fieldReadinessItems
        )
    )

    fun toTextString(): String = buildString {
        appendLine("CBX Exam Lock Device Field Report")
        appendLine("Generated: $generatedAt")
        appendLine("Source: $source")
        appendLine("App: $appVersionName ($versionCode) $buildType")
        appendLine("Device: $manufacturer / $brand / $model sdk=$sdkInt")
        appendLine(
            "Low RAM: enabled=$lowRamEnabled severe=$lowRamSevere ultra=$lowRamUltra " +
                "avail=${lowRamAvailableMemoryMb ?: "-"}MB total=${lowRamTotalMemoryMb ?: "-"}MB " +
                "memoryLow=$lowRamMemoryLow override=$lowRamOverride detected=$lowRamDetectedProfile " +
                "effective=$lowRamEffectiveProfile qrMaxEdgePx=$qrMaxEdgePx polling=${slowPollingMultiplier}x"
        )
        appendLine("Compatibility: family=$compatibilityFamily label=$compatibilityLabel")
        appendLine("Compatibility summary: $compatibilitySummary")
        appendLine("Device survival: score=$survivalScore summary=$survivalPolicySummary")
        survivalRecommendedActions.forEach { action ->
            appendLine("- Survival action: $action")
        }
        appendLine("Previous session: trail=$previousSessionBreadcrumbSummary recovery=${previousSessionRecoveryHint ?: "-"}")
        appendLine("Screen Pinning: setting=$screenPinningSystemSetting lockTask=$lockTaskState")
        appendLine("Overlay policy: $overlayPolicy")
        appendLine("WebView: verdict=$webViewHealthVerdict severity=$webViewHealthSeverity available=$webViewAvailable package=$webViewPackage version=${webViewVersion.ifBlank { "-" }} major=${webViewMajor ?: "-"} old=$webViewOutdatedLikely source=$webViewProviderSource risk=$webViewRiskLabel")
        appendLine("WebView fix: ${webViewQuickFix ?: "-"}")
        appendLine("Network: verdict=$networkVerdict user=$networkUserVerdict transport=$networkTransport validated=$networkValidated captivePortal=$networkCaptivePortal dns=$networkDnsProbe")
        appendLine("Battery: level=$batteryLevelPercent charging=$batteryCharging")
        appendLine("Direct Link: host=$directLinkHost geofence=$directLinkGeofenceEnabled")
        appendLine("Pre-exam health: ${preExamHealthSummary ?: "-"}")
        appendLine("Field readiness: ${fieldReadinessVerdict ?: "-"} ${fieldReadinessSummary ?: "-"}")
        fieldReadinessItems.forEach { item ->
            appendLine("- $item")
        }
        appendLine()
        appendLine("Sensitive data is redacted: full URLs, QR payloads, passwords, tokens, SSID/BSSID, and raw coordinates are not exported.")
    }
}

internal fun buildAdminDeviceFieldReport(
    context: Context,
    settings: AdminSettings,
    lowRamProfile: LowRamProfile,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    networkReadinessStatus: NetworkReadinessStatus = readNetworkReadinessStatus(context),
    batteryStatus: ExamBatteryStatus = readExamBatteryStatus(context),
    preExamHealthSnapshot: PreExamHealthSnapshot? = null,
    fieldReadinessReport: FieldReadinessReport? = null
): ExamDeviceFieldReport {
    val directLinkPolicy = settings.directLinkLocationPolicy()
    val survivalPolicy = buildDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        preExamHealthSnapshot = preExamHealthSnapshot,
        fieldReadinessReport = fieldReadinessReport
    )
    val previousSession = PreviousExamSessionBreadcrumbStore.read(context)
    return ExamDeviceFieldReport(
        generatedAt = diagnosticTimestamp(),
        source = "secret_admin_device_report",
        appVersionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        manufacturer = redactDiagnosticDetail(deviceCompatibilityProfile.manufacturer),
        brand = redactDiagnosticDetail(deviceCompatibilityProfile.brand),
        model = redactDiagnosticDetail(deviceCompatibilityProfile.model),
        sdkInt = deviceCompatibilityProfile.sdkInt,
        lowRamEnabled = lowRamProfile.enabled,
        lowRamSevere = lowRamProfile.severe,
        lowRamUltra = lowRamProfile.ultra,
        lowRamTotalMemoryMb = lowRamProfile.totalMemoryMb,
        lowRamAvailableMemoryMb = lowRamProfile.availableMemoryMb,
        lowRamMemoryLow = lowRamProfile.memoryLow,
        lowRamOverride = lowRamProfile.lowRamOverride.name,
        lowRamDetectedProfile = lowRamProfile.detectedTier?.name ?: lowRamProfile.tier.name,
        lowRamEffectiveProfile = lowRamProfile.tier.name,
        qrMaxEdgePx = lowRamProfile.qrMaxEdgePx,
        slowPollingMultiplier = lowRamProfile.slowPollingMultiplier,
        compatibilityFamily = deviceCompatibilityProfile.family.name,
        compatibilityLabel = deviceCompatibilityProfile.vendorDisplayName,
        compatibilitySummary = redactDiagnosticDetail(deviceCompatibilityProfile.diagnosticSummary()),
        survivalScore = survivalPolicy.score.name,
        survivalPolicySummary = redactDiagnosticDetail(survivalPolicy.diagnosticSummary()),
        survivalRecommendedActions = survivalPolicy.recommendedActions.map { action ->
            redactDiagnosticDetail("${action.code}:${if (action.blocking) "blocking" else "warning"}:${action.label}")
        },
        previousSessionBreadcrumbSummary = redactDiagnosticDetail(previousSession.diagnosticSummary()),
        previousSessionRecoveryHint = previousSession.latestRecoveryHint?.let(::redactDiagnosticDetail),
        screenPinningSystemSetting = ScreenPinningPlatformBridge.readSystemSetting(context),
        lockTaskState = readLockTaskStateLabel(context),
        overlayPolicy = buildOverlayPolicySummary(deviceCompatibilityProfile),
        webViewAvailable = webViewCompatibilityStatus.available,
        webViewPackage = webViewCompatibilityStatus.packageName,
        webViewVersion = webViewCompatibilityStatus.versionName,
        webViewMajor = webViewCompatibilityStatus.majorVersion,
        webViewOutdatedLikely = webViewCompatibilityStatus.outdatedLikely,
        webViewProviderSource = webViewCompatibilityStatus.providerSource,
        webViewHealthVerdict = webViewCompatibilityStatus.verdict.name,
        webViewHealthSeverity = webViewCompatibilityStatus.severity.name,
        webViewRiskLabel = redactDiagnosticDetail(webViewCompatibilityStatus.riskLabel),
        webViewQuickFix = webViewCompatibilityStatus.quickFix?.let(::redactDiagnosticDetail),
        networkVerdict = networkReadinessStatus.verdict.name,
        networkUserVerdict = networkReadinessStatus.userFacingVerdict.name,
        networkTransport = networkReadinessStatus.transportLabel.ifBlank { "-" },
        networkValidated = networkReadinessStatus.diagnostics.isValidated,
        networkCaptivePortal = networkReadinessStatus.diagnostics.isCaptivePortal,
        networkDnsProbe = networkReadinessStatus.dnsProbeStatus.verdict.name,
        batteryLevelPercent = batteryStatus.levelPercent,
        batteryCharging = batteryStatus.isCharging,
        directLinkHost = redactUrlToHost(settings.fastExamUrl),
        directLinkGeofenceEnabled = directLinkPolicy?.geofenceEnabled == true,
        preExamHealthSummary = preExamHealthSnapshot?.diagnosticSummary()?.let(::redactDiagnosticDetail),
        fieldReadinessVerdict = fieldReadinessReport?.finalVerdict?.name,
        fieldReadinessSummary = fieldReadinessReport?.diagnosticSummary()?.let(::redactDiagnosticDetail),
        fieldReadinessItems = fieldReadinessReport?.items.orEmpty().map { item ->
            redactDiagnosticDetail("${item.category.name}:${item.verdict.name}:${item.title}:${item.detail}")
        }
    )
}

internal object ExamDeviceFieldReportExportHelper {
    fun share(context: Context, report: ExamDeviceFieldReport) {
        shareRedactedTextFiles(
            context = context,
            directoryName = "shared_device_reports",
            filePrefix = "CBX_device_report",
            safeSource = report.source,
            subject = "CBX Device Field Report",
            intro = "CBX Exam Lock device field report. Sensitive fields are redacted by default.",
            chooserTitle = "Export Device Report",
            json = report.toJsonString(),
            text = report.toTextString()
        )
    }
}

internal object ExamDiagnosticExportHelper {
    fun share(context: Context, snapshot: ExamDiagnosticSnapshot) {
        shareRedactedTextFiles(
            context = context,
            directoryName = "shared_diagnostics",
            filePrefix = "CBX_exam_diagnostics",
            safeSource = snapshot.source,
            subject = "CBX Exam Diagnostics",
            intro = "CBX Exam Lock diagnostic export. Sensitive fields are redacted by default.",
            chooserTitle = "Export Exam Diagnostics",
            json = snapshot.toJsonString(),
            text = snapshot.toTextString()
        )
    }
}

private fun buildOverlayPolicySummary(profile: DeviceCompatibilityProfile): String {
    return "partial_policy=${profile.partialObscuredWebViewPolicy.name}" +
        " | allow_partial=${profile.allowPartialObscuredWebViewTouch}" +
        " | chrome_suppression_ms=${profile.overlayChromeActionSuppressionMillis}" +
        " | samsung_legacy=${profile.samsungLegacyTablet}"
}

private fun readLockTaskStateLabel(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return "Unsupported"
    }
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val state = runCatching { activityManager?.lockTaskModeState }.getOrNull()
    return when (state) {
        ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
        ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
        ActivityManager.LOCK_TASK_MODE_NONE -> "NONE"
        null -> "Unknown"
        else -> "Unknown($state)"
    }
}

private fun shareRedactedTextFiles(
    context: Context,
    directoryName: String,
    filePrefix: String,
    safeSource: String,
    subject: String,
    intro: String,
    chooserTitle: String,
    json: String,
    text: String
) {
    val exportDir = File(context.cacheDir, directoryName).apply {
        mkdirs()
        listFiles()?.forEach { oldFile ->
            runCatching { oldFile.delete() }
        }
    }
    val normalizedSource = safeSource
        .replace(Regex("[^A-Za-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "exam" }
        .lowercase(Locale.US)
    val stamp = System.currentTimeMillis()
    val jsonFile = File(exportDir, "${filePrefix}_${normalizedSource}_$stamp.json")
    val textFile = File(exportDir, "${filePrefix}_${normalizedSource}_$stamp.txt")
    jsonFile.writeText(json, Charsets.UTF_8)
    textFile.writeText(text, Charsets.UTF_8)

    val uris = arrayListOf(jsonFile, textFile).mapTo(ArrayList()) { file ->
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "text/plain"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, intro)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, chooserTitle)
    if (!launchPlatformIntentSafely(context, chooserIntent)) {
        throw IllegalStateException("No compatible diagnostics share target is available.")
    }
}

internal fun redactUrlToHost(rawUrl: String?): String {
    val value = rawUrl.orEmpty().trim()
    if (value.isBlank() || value == "-") {
        return "-"
    }
    fun hostFrom(candidate: String): String? = runCatching {
        URI(candidate).host
    }.getOrNull()?.takeIf { it.isNotBlank() }
    val host = hostFrom(value)
        ?: hostFrom("https://$value")
        ?: value
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .takeIf { it.isNotBlank() }
    return host?.lowercase(Locale.US) ?: "[redacted-host]"
}

internal fun redactWifiValue(rawValue: String?): String {
    val normalized = rawValue.orEmpty().trim().removePrefix("\"").removeSuffix("\"")
    if (normalized.isBlank() || normalized == "-") {
        return "-"
    }
    return "masked-${normalized.hashCode().toUInt().toString(16).take(8)}"
}

internal fun redactDiagnosticDetail(rawDetail: String): String {
    var redacted = rawDetail
    redacted = redacted.replace(Regex("""https?://[^\s|]+""", RegexOption.IGNORE_CASE)) { match ->
        "${redactUrlToHost(match.value)}[redacted-path]"
    }
    redacted = redacted.replace(
        Regex("""(?i)\b(token|password|passcode|payload|qr_payload|encrypted_payload|secret)\s*[:=]\s*[^|\s]+""")
    ) { match ->
        val key = match.groupValues[1]
        "$key=[redacted]"
    }
    redacted = redacted.replace(
        Regex("""(?i)\bssid\s*[:=]\s*[^|\n]+""")
    ) { "ssid=${redactWifiValue(it.value.substringAfter('=').substringAfter(':'))}" }
    redacted = redacted.replace(
        Regex("""(?i)\bbssid\s*[:=]\s*([0-9a-f]{2}:){5}[0-9a-f]{2}""")
    ) { "bssid=**:**:**:**:**:**" }
    redacted = redacted.replace(
        Regex("""(?<!\w)-?\d{1,3}\.\d{4,}(?!\w)""")
    ) { "[coord]" }
    return redactFreeText(redacted)
}

private fun toSnapshotEvent(event: DiagnosticEvent): ExamDiagnosticSnapshotEvent =
    ExamDiagnosticSnapshotEvent(
        timestamp = event.timestamp,
        level = event.level,
        code = event.code,
        screen = event.screen,
        appElapsedMs = event.appElapsedMs,
        sessionElapsedMs = event.sessionElapsedMs,
        details = redactDiagnosticDetail(event.details)
    )

private fun redactFreeText(raw: String): String =
    raw.take(240).replace(Regex("""[\u0000-\u001F]"""), " ").trim()

private fun jsonObject(vararg pairs: Pair<String, Any?>): String =
    pairs.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "${jsonString(key)}:${jsonValue(value)}"
    }

@Suppress("UNCHECKED_CAST")
private fun jsonValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> jsonString(value)
    is Number -> value.toString()
    is Boolean -> value.toString()
    is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { entry ->
        "${jsonString(entry.key.toString())}:${jsonValue(entry.value)}"
    }
    is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { item -> jsonValue(item) }
    else -> jsonString(value.toString())
}

private fun jsonString(value: String): String =
    buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
