package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.DeviceSurvivalPolicy
import java.util.Locale

internal enum class PreparationAutoFixSeverity {
    Blocking,
    Warning
}

internal enum class PreparationAutoFixTarget {
    Network,
    Location,
    DeviceTime,
    ScreenPinning,
    Accessibility,
    Overlay,
    Battery,
    WebView,
    All
}

internal data class PreparationAutoFixSuggestion(
    val code: String,
    val label: String,
    val severity: PreparationAutoFixSeverity,
    val target: PreparationAutoFixTarget,
    val priority: Int,
    val quickFix: String?
)

internal fun buildPreparationAutoFixSuggestions(
    snapshot: PreExamHealthSnapshot,
    survivalPolicy: DeviceSurvivalPolicy
): List<PreparationAutoFixSuggestion> {
    val healthSuggestions = snapshot.items
        .filter { it.verdict != PreExamHealthVerdict.Stable }
        .map { item ->
            PreparationAutoFixSuggestion(
                code = "health_${item.category.name.lowercase(Locale.US)}",
                label = item.quickFix?.takeIf { it.isNotBlank() } ?: "Review ${item.title}",
                severity = if (item.verdict == PreExamHealthVerdict.Blocking) {
                    PreparationAutoFixSeverity.Blocking
                } else {
                    PreparationAutoFixSeverity.Warning
                },
                target = item.category.toAutoFixTarget(item.quickFix),
                priority = item.category.toAutoFixPriority(item.verdict),
                quickFix = item.quickFix
            )
        }
    val policySuggestions = survivalPolicy.recommendedActions
        .filterNot { action -> healthSuggestions.any { it.code == action.code } }
        .map { action ->
            PreparationAutoFixSuggestion(
                code = action.code,
                label = action.label,
                severity = if (action.blocking) {
                    PreparationAutoFixSeverity.Blocking
                } else {
                    PreparationAutoFixSeverity.Warning
                },
                target = if (action.code.contains("webview", ignoreCase = true)) {
                    PreparationAutoFixTarget.WebView
                } else {
                    PreparationAutoFixTarget.All
                },
                priority = if (action.blocking) 20 else 220,
                quickFix = action.label
            )
        }
    return (healthSuggestions + policySuggestions)
        .distinctBy { "${it.code}:${it.severity.name}" }
        .sortedWith(
            compareBy<PreparationAutoFixSuggestion> {
                if (it.severity == PreparationAutoFixSeverity.Blocking) 0 else 1
            }.thenBy { it.priority }
        )
}

private fun PreExamHealthCategory.toAutoFixTarget(quickFix: String?): PreparationAutoFixTarget {
    val lowerFix = quickFix.orEmpty().lowercase(Locale.US)
    return when (this) {
        PreExamHealthCategory.ScreenPinning -> {
            if (lowerFix.contains("accessibility")) {
                PreparationAutoFixTarget.Accessibility
            } else {
                PreparationAutoFixTarget.ScreenPinning
            }
        }
        PreExamHealthCategory.FloatingAppOverlay -> PreparationAutoFixTarget.Overlay
        PreExamHealthCategory.Network -> PreparationAutoFixTarget.Network
        PreExamHealthCategory.WebView -> PreparationAutoFixTarget.WebView
        PreExamHealthCategory.Location -> PreparationAutoFixTarget.Location
        PreExamHealthCategory.DeviceTime -> PreparationAutoFixTarget.DeviceTime
        PreExamHealthCategory.BatteryPower -> PreparationAutoFixTarget.Battery
    }
}

private fun PreExamHealthCategory.toAutoFixPriority(verdict: PreExamHealthVerdict): Int {
    val base = when (this) {
        PreExamHealthCategory.WebView -> 10
        PreExamHealthCategory.ScreenPinning -> 20
        PreExamHealthCategory.DeviceTime -> 30
        PreExamHealthCategory.Location -> 40
        PreExamHealthCategory.FloatingAppOverlay -> 50
        PreExamHealthCategory.Network -> 60
        PreExamHealthCategory.BatteryPower -> 90
    }
    return if (verdict == PreExamHealthVerdict.Blocking) base else base + 200
}
