package com.example.coblaxexamlock




internal enum class OverlayBypassState {
    Active,
    Inactive,
    Tampered
}

internal object OverlayBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): OverlayBypassState {
        return when {
            tampered -> OverlayBypassState.Tampered
            enabled -> OverlayBypassState.Active
            else -> OverlayBypassState.Inactive
        }
    }
}

internal enum class OverlaySignal {
    ObscuredTouch,
    WindowFocusLoss,
    AccessibilityServiceEnabled,
    RiskyAccessibilityPackage,
    OverlayShieldActive,
    OverlayShieldUnsupported
}

internal enum class OverlayQuickFixTarget {
    AccessibilitySettings,
    OverlaySettings
}

internal fun OverlaySignal.diagnosticLabel(): String {
    return when (this) {
        OverlaySignal.ObscuredTouch -> "obscured_touch"
        OverlaySignal.WindowFocusLoss -> "window_focus_loss"
        OverlaySignal.AccessibilityServiceEnabled -> "accessibility_enabled"
        OverlaySignal.RiskyAccessibilityPackage -> "risky_accessibility_package"
        OverlaySignal.OverlayShieldActive -> "overlay_shield_active"
        OverlaySignal.OverlayShieldUnsupported -> "overlay_shield_unsupported"
    }
}

internal fun OverlayQuickFixTarget.diagnosticLabel(): String {
    return when (this) {
        OverlayQuickFixTarget.AccessibilitySettings -> "accessibility_settings"
        OverlayQuickFixTarget.OverlaySettings -> "overlay_settings"
    }
}

internal data class OverlayShieldStatus(
    val supported: Boolean,
    val requested: Boolean,
    val lastApplySucceeded: Boolean?,
    val lastApplyAt: String?
) {
    val active: Boolean
        get() = supported && requested && lastApplySucceeded == true
}

internal data class OverlayRiskResult(
    val bypassed: Boolean,
    val confirmedInteractionDetected: Boolean,
    val heuristicRisk: Boolean,
    val accessibilityEnabled: Boolean,
    val riskyAccessibilityPackages: List<String>,
    val violationCount: Int,
    val signals: Set<OverlaySignal>,
    val quickFixTargets: Set<OverlayQuickFixTarget>,
    val shieldStatus: OverlayShieldStatus,
    val lastTrigger: String?,
    val lastDetectedAt: String?,
    val lastContext: String?
) {
    val hasAnyRisk: Boolean
        get() = confirmedInteractionDetected || heuristicRisk

    val hasBlockingRisk: Boolean
        get() = confirmedInteractionDetected
}

internal object OverlayRiskAnalyzer {
    fun inspect(
        bypassed: Boolean,
        accessibilityEnabled: Boolean,
        riskyAccessibilityPackages: List<String>,
        violationCount: Int,
        shieldStatus: OverlayShieldStatus,
        lastTrigger: String?,
        lastDetectedAt: String?,
        lastContext: String?
    ): OverlayRiskResult {
        if (bypassed) {
            return OverlayRiskResult(
                bypassed = true,
                confirmedInteractionDetected = false,
                heuristicRisk = false,
                accessibilityEnabled = accessibilityEnabled,
                riskyAccessibilityPackages = emptyList(),
                violationCount = violationCount,
                signals = emptySet(),
                quickFixTargets = emptySet(),
                shieldStatus = shieldStatus,
                lastTrigger = lastTrigger,
                lastDetectedAt = lastDetectedAt,
                lastContext = lastContext
            )
        }

        val signals = linkedSetOf<OverlaySignal>()
        if (shieldStatus.active) {
            signals.add(OverlaySignal.OverlayShieldActive)
        } else if (shieldStatus.requested && !shieldStatus.supported) {
            signals.add(OverlaySignal.OverlayShieldUnsupported)
        }
        if (accessibilityEnabled) {
            signals.add(OverlaySignal.AccessibilityServiceEnabled)
        }
        if (riskyAccessibilityPackages.isNotEmpty()) {
            signals.add(OverlaySignal.RiskyAccessibilityPackage)
        }
        val confirmedSignal = when {
            violationCount <= 0 -> null
            lastTrigger == OverlaySignal.WindowFocusLoss.diagnosticLabel() -> null
            else -> OverlaySignal.ObscuredTouch
        }
        val confirmedInteractionDetected = confirmedSignal != null
        if (confirmedSignal != null) {
            signals.add(confirmedSignal)
        }

        // heuristicRisk is intentionally NOT triggered by accessibilityEnabled alone.
        // A legitimate accessibility service (keyboard, TalkBack, switch access) should not
        // be flagged as an overlay risk. Only risky packages confirmed by the package inspector trigger this.
        val heuristicRisk = riskyAccessibilityPackages.isNotEmpty()
        val quickFixTargets = linkedSetOf<OverlayQuickFixTarget>()
        if (confirmedInteractionDetected) {
            quickFixTargets.add(OverlayQuickFixTarget.OverlaySettings)
        }
        if (heuristicRisk) {
            quickFixTargets.add(OverlayQuickFixTarget.AccessibilitySettings)
        }

        return OverlayRiskResult(
            bypassed = false,
            confirmedInteractionDetected = confirmedInteractionDetected,
            heuristicRisk = heuristicRisk,
            accessibilityEnabled = accessibilityEnabled,
            riskyAccessibilityPackages = riskyAccessibilityPackages,
            violationCount = violationCount,
            signals = signals,
            quickFixTargets = quickFixTargets,
            shieldStatus = shieldStatus,
            lastTrigger = lastTrigger,
            lastDetectedAt = lastDetectedAt,
            lastContext = lastContext
        )
    }
}
