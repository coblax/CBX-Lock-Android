package com.example.coblaxexamlock

import com.example.coblaxexamlock.ui.admin.FieldReadinessReport
import com.example.coblaxexamlock.ui.admin.FieldReadinessVerdict
import com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot
import com.example.coblaxexamlock.ui.preparation.PreExamHealthVerdict
import java.util.Locale

internal enum class CompatibilityScore {
    Excellent,
    Good,
    NeedsSetup,
    NotRecommended
}

internal enum class DeviceSurvivalRuntimeTier {
    Standard,
    Cautious,
    Constrained,
    NotRecommended
}

internal enum class DeviceSurvivalUiTier {
    Rich,
    Lightweight,
    SevereLowRam
}

internal data class DeviceSurvivalRecommendedAction(
    val code: String,
    val label: String,
    val blocking: Boolean
)

internal data class DeviceSurvivalPolicy(
    val score: CompatibilityScore,
    val runtimeTier: DeviceSurvivalRuntimeTier,
    val uiTier: DeviceSurvivalUiTier,
    val vendorRiskLabel: String,
    val webViewRiskLabel: String,
    val startExamAllowedByHealth: Boolean,
    val healthBlockingCount: Int,
    val healthWarningCount: Int,
    val fieldBlockedCount: Int,
    val fieldWarningCount: Int,
    val recommendedActions: List<DeviceSurvivalRecommendedAction>
) {
    val blockingActionCount: Int
        get() = recommendedActions.count { it.blocking }

    val warningActionCount: Int
        get() = recommendedActions.size - blockingActionCount

    fun diagnosticSummary(): String = buildString {
        append("score="); append(score.name)
        append(" | runtime="); append(runtimeTier.name)
        append(" | ui="); append(uiTier.name)
        append(" | start_allowed="); append(startExamAllowedByHealth)
        append(" | health_blocking="); append(healthBlockingCount)
        append(" | health_warning="); append(healthWarningCount)
        append(" | field_blocking="); append(fieldBlockedCount)
        append(" | field_warning="); append(fieldWarningCount)
        append(" | vendor="); append(vendorRiskLabel.ifBlank { "-" })
        append(" | webview="); append(webViewRiskLabel.ifBlank { "-" })
        append(" | actions="); append(
            recommendedActions.joinToString(",") { action ->
                "${action.code}:${if (action.blocking) "block" else "warn"}"
            }.ifBlank { "-" }
        )
    }
}

internal fun buildDeviceSurvivalPolicy(
    lowRamProfile: LowRamProfile,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    preExamHealthSnapshot: PreExamHealthSnapshot? = null,
    fieldReadinessReport: FieldReadinessReport? = null
): DeviceSurvivalPolicy {
    val healthBlockingCount = preExamHealthSnapshot?.blockingCount ?: 0
    val healthWarningCount = preExamHealthSnapshot?.warningCount ?: 0
    val fieldBlockedCount = fieldReadinessReport?.blockedCount ?: 0
    val fieldWarningCount = fieldReadinessReport?.warningCount ?: 0
    val webViewUnavailablePenalty =
        if (webViewCompatibilityStatus.severity == WebViewHealthSeverity.Blocking) 1 else 0
    val totalBlocking = healthBlockingCount + fieldBlockedCount + webViewUnavailablePenalty
    val totalWarnings =
        healthWarningCount +
            fieldWarningCount +
            if (webViewCompatibilityStatus.severity == WebViewHealthSeverity.Warning) 1 else 0

    val score = when {
        totalBlocking > 0 -> CompatibilityScore.NotRecommended
        webViewCompatibilityStatus.severity == WebViewHealthSeverity.Warning || totalWarnings >= 2 ->
            CompatibilityScore.NeedsSetup
        lowRamProfile.enabled || deviceCompatibilityProfile.samsungLegacyTablet || totalWarnings == 1 ->
            CompatibilityScore.Good
        else -> CompatibilityScore.Excellent
    }
    val runtimeTier = when {
        score == CompatibilityScore.NotRecommended -> DeviceSurvivalRuntimeTier.NotRecommended
        lowRamProfile.severe || deviceCompatibilityProfile.samsungLegacyTablet || totalWarnings >= 2 ->
            DeviceSurvivalRuntimeTier.Constrained
        lowRamProfile.enabled || score == CompatibilityScore.NeedsSetup || totalWarnings == 1 ->
            DeviceSurvivalRuntimeTier.Cautious
        else -> DeviceSurvivalRuntimeTier.Standard
    }
    val uiTier = when {
        lowRamProfile.severe -> DeviceSurvivalUiTier.SevereLowRam
        lowRamProfile.enabled || deviceCompatibilityProfile.useLightweightPreparationUi ->
            DeviceSurvivalUiTier.Lightweight
        else -> DeviceSurvivalUiTier.Rich
    }

    return DeviceSurvivalPolicy(
        score = score,
        runtimeTier = runtimeTier,
        uiTier = uiTier,
        vendorRiskLabel = resolveVendorRiskLabel(deviceCompatibilityProfile),
        webViewRiskLabel = resolveWebViewRiskLabel(webViewCompatibilityStatus),
        startExamAllowedByHealth =
            healthBlockingCount == 0 && webViewCompatibilityStatus.severity != WebViewHealthSeverity.Blocking,
        healthBlockingCount = healthBlockingCount,
        healthWarningCount = healthWarningCount,
        fieldBlockedCount = fieldBlockedCount,
        fieldWarningCount = fieldWarningCount,
        recommendedActions = buildSurvivalRecommendedActions(
            preExamHealthSnapshot = preExamHealthSnapshot,
            fieldReadinessReport = fieldReadinessReport,
            webViewCompatibilityStatus = webViewCompatibilityStatus
        )
    )
}

private fun resolveVendorRiskLabel(profile: DeviceCompatibilityProfile): String {
    return when {
        profile.samsungLegacyTablet -> "Samsung legacy compatibility mode"
        profile.family != DeviceCompatibilityFamily.Generic -> "${profile.vendorDisplayName} compatibility profile"
        else -> "Generic Android profile"
    }
}

private fun resolveWebViewRiskLabel(status: WebViewCompatibilityStatus): String {
    return status.riskLabel
}

private fun buildSurvivalRecommendedActions(
    preExamHealthSnapshot: PreExamHealthSnapshot?,
    fieldReadinessReport: FieldReadinessReport?,
    webViewCompatibilityStatus: WebViewCompatibilityStatus
): List<DeviceSurvivalRecommendedAction> {
    val actions = mutableListOf<DeviceSurvivalRecommendedAction>()
    preExamHealthSnapshot?.items.orEmpty()
        .filter { it.verdict != PreExamHealthVerdict.Stable }
        .forEach { item ->
            actions += DeviceSurvivalRecommendedAction(
                code = "health_${item.category.name.lowercase(Locale.US)}",
                label = item.quickFix?.takeIf { it.isNotBlank() } ?: item.title,
                blocking = item.verdict == PreExamHealthVerdict.Blocking
            )
        }
    fieldReadinessReport?.items.orEmpty()
        .filter { it.verdict != FieldReadinessVerdict.Ready }
        .forEach { item ->
            actions += DeviceSurvivalRecommendedAction(
                code = "field_${item.category.name.lowercase(Locale.US)}",
                label = item.quickFix?.takeIf { it.isNotBlank() } ?: item.title,
                blocking = item.verdict == FieldReadinessVerdict.Blocked
            )
        }
    if (webViewCompatibilityStatus.severity != WebViewHealthSeverity.Stable) {
        actions += DeviceSurvivalRecommendedAction(
            code = "webview_provider",
            label = webViewCompatibilityStatus.quickFix ?: "Check Android System WebView or Chrome.",
            blocking = webViewCompatibilityStatus.severity == WebViewHealthSeverity.Blocking
        )
    }
    return actions
        .distinctBy { "${it.code}:${it.blocking}" }
        .sortedWith(compareBy<DeviceSurvivalRecommendedAction> { if (it.blocking) 0 else 1 }.thenBy { it.code })
}
