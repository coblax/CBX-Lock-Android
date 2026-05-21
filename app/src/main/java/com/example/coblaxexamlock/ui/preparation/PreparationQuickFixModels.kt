package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.LowRamProfile

internal enum class QuickFixTarget {
    All,
    Network,
    Location,
    DeviceTime,
    ScreenPinning,
    WebView,
    Battery,
    ScreenRecorder,
    DisplayMirror,
    MultiWindow
}

internal enum class QuickFixSeverity {
    Blocking,
    Warning
}

internal const val QuickFixStartScreenPinningCode = "start_screen_pinning"
internal const val QuickFixScreenPinningDeferredCode = "screen_pinning_deferred_until_blockers_clear"
internal const val QuickFixRefreshAllSecurityChecksCode = "refresh_all_security_checks"

internal data class PreparationQuickFixAction(
    val code: String,
    val text: String,
    val severity: QuickFixSeverity,
    val target: QuickFixTarget?,
    val priority: Int,
    val filled: Boolean = false,
    val loading: Boolean = false,
    val enabled: Boolean = true,
    val opensExternalSettings: Boolean = false,
    val isNotice: Boolean = false,
    val diagnosticDetails: String? = null,
    val onClick: () -> Unit
)

internal data class PreparationQuickFixDisplayActions(
    val notices: List<PreparationQuickFixAction>,
    val primary: PreparationQuickFixAction?,
    val blocking: List<PreparationQuickFixAction>,
    val warnings: List<PreparationQuickFixAction>,
    val refresh: PreparationQuickFixAction?,
    val blockingCount: Int,
    val warningCount: Int
)

internal fun selectPreparationQuickFixActionsForDisplay(
    actions: List<PreparationQuickFixAction>,
    lowRamProfile: LowRamProfile
): PreparationQuickFixDisplayActions {
    val notices = actions.filter { it.isNotice }
    val actionable = actions.filterNot { it.isNotice }
    val refresh = actionable.firstOrNull {
        it.code == QuickFixRefreshAllSecurityChecksCode
    }
    val issues = actionable.filterNot {
        it.code == QuickFixRefreshAllSecurityChecksCode
    }
    val primary = issues.firstOrNull() ?: refresh
    val remainingIssues = if (primary != null && issues.firstOrNull() == primary) {
        issues.drop(1)
    } else {
        issues
    }
    val maxBlocking = when {
        lowRamProfile.ultra -> 0
        lowRamProfile.enabled -> 3
        else -> Int.MAX_VALUE
    }
    val maxWarnings = when {
        lowRamProfile.ultra -> 0
        lowRamProfile.enabled -> 2
        else -> Int.MAX_VALUE
    }
    return PreparationQuickFixDisplayActions(
        notices = notices,
        primary = primary,
        blocking = remainingIssues
            .filter { it.severity == QuickFixSeverity.Blocking }
            .take(maxBlocking),
        warnings = remainingIssues
            .filter { it.severity == QuickFixSeverity.Warning }
            .take(maxWarnings),
        refresh = refresh.takeIf { it != null && it != primary },
        blockingCount = issues.count { it.severity == QuickFixSeverity.Blocking },
        warningCount = issues.count { it.severity == QuickFixSeverity.Warning }
    )
}
