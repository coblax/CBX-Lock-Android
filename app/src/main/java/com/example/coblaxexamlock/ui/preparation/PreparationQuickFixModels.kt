package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.ui.performance.resolvePreparationActionRenderBudget

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

internal enum class PreparationSection {
    DeviceSetup,
    Connectivity,
    DeviceHealth,
    RuntimeInteraction,
    DeviceIntegrity,
    Clipboard,
    Location,
    DeviceLock,
    RuntimeSecurity
}

internal const val QuickFixStartScreenPinningCode = "start_screen_pinning"
internal const val QuickFixScreenPinningDeferredCode = "screen_pinning_deferred_until_blockers_clear"
internal const val QuickFixRefreshAllSecurityChecksCode = "refresh_all_security_checks"

internal data class PreparationQuickFixAction(
    val code: String,
    val text: String,
    val reason: String? = null,
    val severity: QuickFixSeverity,
    val target: QuickFixTarget?,
    val priority: Int,
    val section: PreparationSection? = null,
    val fieldText: String? = null,
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
    lowRamProfile: LowRamProfile,
    hasGlobalBlockingIssues: Boolean = actions.any {
        it.severity == QuickFixSeverity.Blocking &&
            !it.isNotice &&
            it.code != QuickFixRefreshAllSecurityChecksCode
    }
): PreparationQuickFixDisplayActions {
    val notices = actions.filter { it.isNotice }
    val actionable = actions.filterNot { it.isNotice }
    val refresh = actionable.firstOrNull {
        it.code == QuickFixRefreshAllSecurityChecksCode
    }
    val issues = actionable.filterNot {
        it.code == QuickFixRefreshAllSecurityChecksCode
    }
    val renderBudget = resolvePreparationActionRenderBudget(lowRamProfile)
    val suppressWarnings = !renderBudget.renderWarningsWhileBlocking && hasGlobalBlockingIssues
    val primary = if (suppressWarnings) {
        issues.firstOrNull { it.severity == QuickFixSeverity.Blocking } ?: refresh
    } else {
        issues.firstOrNull() ?: refresh
    }
    val remainingIssues = if (primary != null) {
        issues.filterNot { it.code == primary.code }
    } else {
        issues
    }
    return PreparationQuickFixDisplayActions(
        notices = notices,
        primary = primary,
        blocking = remainingIssues
            .filter { it.severity == QuickFixSeverity.Blocking }
            .take(renderBudget.maxBlockingActions),
        warnings = if (suppressWarnings) {
            emptyList()
        } else {
            remainingIssues
                .filter { it.severity == QuickFixSeverity.Warning }
                .take(renderBudget.maxWarningActions)
        },
        refresh = refresh.takeIf { it != null && it != primary },
        blockingCount = issues.count { it.severity == QuickFixSeverity.Blocking },
        warningCount = issues.count { it.severity == QuickFixSeverity.Warning }
    )
}

internal fun PreparationQuickFixAction.displayTextForProfile(
    lowRamProfile: LowRamProfile
): String {
    return if (lowRamProfile.enabled) {
        fieldText ?: text
    } else {
        text
    }
}
