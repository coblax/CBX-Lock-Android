package com.example.coblaxexamlock




internal enum class AppSwitchBypassState {
    Active,
    Inactive,
    Tampered
}

internal object AppSwitchBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): AppSwitchBypassState {
        return when {
            tampered -> AppSwitchBypassState.Tampered
            enabled -> AppSwitchBypassState.Active
            else -> AppSwitchBypassState.Inactive
        }
    }
}

internal enum class AppSwitchSignal {
    UserLeaveHint,
    ResumeAfterLeave,
    SuppressedInternalFlow,
    LifecycleResumeFallback,
    AccessibilityGuard
}

internal fun AppSwitchSignal.diagnosticLabel(): String {
    return when (this) {
        AppSwitchSignal.UserLeaveHint -> "user_leave_hint"
        AppSwitchSignal.ResumeAfterLeave -> "resume_after_leave"
        AppSwitchSignal.SuppressedInternalFlow -> "suppressed_internal_flow"
        AppSwitchSignal.LifecycleResumeFallback -> "lifecycle_resume_fallback"
        AppSwitchSignal.AccessibilityGuard -> "accessibility_guard"
    }
}

internal enum class AppSwitchProtectionMode {
    ProtectedByPinning,
    FallbackGuardOnly,
    Bypassed
}

internal fun AppSwitchProtectionMode.diagnosticLabel(): String {
    return when (this) {
        AppSwitchProtectionMode.ProtectedByPinning -> "protected_by_pinning"
        AppSwitchProtectionMode.FallbackGuardOnly -> "fallback_guard_only"
        AppSwitchProtectionMode.Bypassed -> "bypassed"
    }
}

internal enum class AppSwitchSuppressionReason {
    ScreenPinningRequest,
    AppDrivenExit
}

internal fun AppSwitchSuppressionReason.diagnosticLabel(): String {
    return when (this) {
        AppSwitchSuppressionReason.ScreenPinningRequest -> "screen_pinning_request"
        AppSwitchSuppressionReason.AppDrivenExit -> "app_driven_exit"
    }
}

internal data class AppSwitchStatus(
    val bypassState: AppSwitchBypassState,
    val monitoringEnabled: Boolean,
    val runtimeMonitoringActive: Boolean,
    val protectionMode: AppSwitchProtectionMode,
    val lockTaskActive: Boolean,
    val violationCount: Int,
    val pendingViolation: Boolean,
    val lastTrigger: String?,
    val lastDetectedAt: String?,
    val lastContext: String?,
    val accessibilityGuardEnabled: Boolean,
    val accessibilityFallbackActive: Boolean,
    val accessibilityViolationCount: Int,
    val accessibilityLastReason: String?,
    val accessibilityLastForeignPackage: String?,
    val accessibilityLastEventType: String?,
    val accessibilityLastDetectedAt: String?,
    val accessibilityAlarmSeverity: String?
) {
    val bypassed: Boolean
        get() = bypassState == AppSwitchBypassState.Active

    val tampered: Boolean
        get() = bypassState == AppSwitchBypassState.Tampered

    val hasViolations: Boolean
        get() = violationCount > 0

    val fallbackGuardActive: Boolean
        get() = protectionMode == AppSwitchProtectionMode.FallbackGuardOnly
}

internal object AppSwitchMonitor {
    fun shouldMonitor(
        hostAvailable: Boolean,
        guardArmed: Boolean,
        bypassState: AppSwitchBypassState
    ): Boolean {
        return hostAvailable &&
            guardArmed &&
            bypassState != AppSwitchBypassState.Active
    }

    fun protectionModeOf(
        bypassState: AppSwitchBypassState,
        screenPinningMode: ScreenPinningMode,
        guardArmed: Boolean,
        lockTaskActive: Boolean
    ): AppSwitchProtectionMode {
        return when {
            bypassState == AppSwitchBypassState.Active -> AppSwitchProtectionMode.Bypassed
            guardArmed && (screenPinningMode == ScreenPinningMode.Bypassed || !lockTaskActive) ->
                AppSwitchProtectionMode.FallbackGuardOnly

            else -> AppSwitchProtectionMode.ProtectedByPinning
        }
    }

    fun eventDetails(
        protectionMode: AppSwitchProtectionMode,
        screenPinningMode: ScreenPinningMode,
        lockTaskActive: Boolean,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String {
        return buildString {
            append("protection=")
            append(protectionMode.diagnosticLabel())
            append(" | screen_pinning_mode=")
            append(screenPinningMode.name.lowercase())
            append(" | lock_task=")
            append(if (lockTaskActive) "active" else "inactive")
            append(" | fallback_guard=")
            append(if (protectionMode == AppSwitchProtectionMode.FallbackGuardOnly) "yes" else "no")
            suppressionReason?.let {
                append(" | suppression_reason=")
                append(it.diagnosticLabel())
            }
        }
    }

    fun eventDetails(
        signal: AppSwitchSignal,
        protectionMode: AppSwitchProtectionMode,
        screenPinningMode: ScreenPinningMode,
        lockTaskActive: Boolean,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String {
        return buildString {
            append("trigger=")
            append(signal.diagnosticLabel())
            append(" | ")
            append(
                eventDetails(
                    protectionMode = protectionMode,
                    screenPinningMode = screenPinningMode,
                    lockTaskActive = lockTaskActive,
                    suppressionReason = suppressionReason
                )
            )
        }
    }

    fun statusOf(
        bypassState: AppSwitchBypassState,
        runtimeMonitoringActive: Boolean,
        protectionMode: AppSwitchProtectionMode,
        lockTaskActive: Boolean,
        violationCount: Int,
        pendingViolation: Boolean,
        lastTrigger: String?,
        lastDetectedAt: String?,
        lastContext: String?,
        accessibilityGuardEnabled: Boolean = false,
        accessibilityFallbackActive: Boolean = false,
        accessibilityViolationCount: Int = 0,
        accessibilityLastReason: String? = null,
        accessibilityLastForeignPackage: String? = null,
        accessibilityLastEventType: String? = null,
        accessibilityLastDetectedAt: String? = null,
        accessibilityAlarmSeverity: String? = null
    ): AppSwitchStatus {
        return AppSwitchStatus(
            bypassState = bypassState,
            monitoringEnabled = bypassState != AppSwitchBypassState.Active,
            runtimeMonitoringActive = runtimeMonitoringActive,
            protectionMode = protectionMode,
            lockTaskActive = lockTaskActive,
            violationCount = violationCount,
            pendingViolation = pendingViolation,
            lastTrigger = lastTrigger,
            lastDetectedAt = lastDetectedAt,
            lastContext = lastContext,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            accessibilityFallbackActive = accessibilityFallbackActive,
            accessibilityViolationCount = accessibilityViolationCount,
            accessibilityLastReason = accessibilityLastReason,
            accessibilityLastForeignPackage = accessibilityLastForeignPackage,
            accessibilityLastEventType = accessibilityLastEventType,
            accessibilityLastDetectedAt = accessibilityLastDetectedAt,
            accessibilityAlarmSeverity = accessibilityAlarmSeverity
        )
    }
}
