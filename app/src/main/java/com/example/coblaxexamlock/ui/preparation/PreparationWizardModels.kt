package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.UiLanguage

/**
 * Represents one wizard step. Each step maps 1-to-1 to a checklist section.
 */
internal enum class WizardStep(
    val sectionKey: String,
    val titleEN: String,
    val titleID: String,
    val iconEmoji: String,
    val descEN: String,
    val descID: String
) {
    DeviceSetup(
        sectionKey = "checklist_device_setup",
        titleEN = "Device Setup",
        titleID = "Pengaturan Perangkat",
        iconEmoji = "📱",
        descEN = "Keyboard & Bluetooth settings",
        descID = "Pengaturan keyboard & Bluetooth"
    ),
    Connectivity(
        sectionKey = "checklist_connectivity",
        titleEN = "Connectivity",
        titleID = "Konektivitas",
        iconEmoji = "🌐",
        descEN = "Network & VPN checks",
        descID = "Pemeriksaan jaringan & VPN"
    ),
    DeviceHealth(
        sectionKey = "checklist_device_health",
        titleEN = "Device Health",
        titleID = "Kesehatan Perangkat",
        iconEmoji = "💊",
        descEN = "WebView & time settings",
        descID = "Pengaturan WebView & waktu"
    ),
    RuntimeInteraction(
        sectionKey = "checklist_runtime_interaction",
        titleEN = "Runtime Interaction",
        titleID = "Interaksi Runtime",
        iconEmoji = "🛡️",
        descEN = "Accessibility & overlay checks",
        descID = "Pemeriksaan aksesibilitas & overlay"
    ),
    DeviceIntegrity(
        sectionKey = "checklist_device_integrity",
        titleEN = "Device Integrity",
        titleID = "Integritas Perangkat",
        iconEmoji = "🔒",
        descEN = "ADB, root & signature verification",
        descID = "Verifikasi ADB, root & signature"
    ),
    Clipboard(
        sectionKey = "checklist_runtime_clipboard",
        titleEN = "Clipboard",
        titleID = "Clipboard",
        iconEmoji = "📋",
        descEN = "Clipboard monitoring status",
        descID = "Status monitoring clipboard"
    ),
    Location(
        sectionKey = "checklist_location",
        titleEN = "Location",
        titleID = "Lokasi",
        iconEmoji = "📍",
        descEN = "Geofence & anti-fake-location",
        descID = "Geofence & anti-lokasi-palsu"
    ),
    DeviceLock(
        sectionKey = "checklist_device_lock",
        titleEN = "Device Lock",
        titleID = "Kunci Perangkat",
        iconEmoji = "🔐",
        descEN = "Screen pinning & exam guard",
        descID = "Screen pinning & penjaga ujian"
    ),
    RuntimeSecurity(
        sectionKey = "checklist_runtime_static_security",
        titleEN = "Runtime Security",
        titleID = "Keamanan Runtime",
        iconEmoji = "⚡",
        descEN = "Screen recorder, display mirror, multi-window",
        descID = "Screen recorder, display mirror, multi-window"
    );

    fun title(lang: UiLanguage): String = localized(lang, titleEN, titleID)
    fun description(lang: UiLanguage): String = localized(lang, descEN, descID)
}

/**
 * Per-step state for the wizard UI.
 */
internal data class WizardStepState(
    val step: WizardStep,
    val isCompleted: Boolean,
    val issueCount: Int
)

internal enum class PreparationWizardPayloadBuildMode {
    FullChecklist,
    ActiveStepOnly
}

internal data class PreparationWizardStepPayload(
    val currentStep: WizardStep,
    val readiness: PreparationChecklistReadiness,
    val sectionHealth: SectionHealth?,
    val sectionText: PreparationChecklistText,
    val quickFixActions: List<PreparationQuickFixAction>,
    val buildMode: PreparationWizardPayloadBuildMode
) {
    val fullChecklistBuilt: Boolean
        get() = buildMode == PreparationWizardPayloadBuildMode.FullChecklist
}

internal data class WizardStepActionCoverage(
    val hasIssue: Boolean,
    val hasActionOrNotice: Boolean
) {
    val showManualFixHint: Boolean
        get() = hasIssue && !hasActionOrNotice
}

internal fun resolvePreparationWizardPayloadBuildMode(
    lowRamProfile: LowRamProfile,
    showChecklistDetails: Boolean
): PreparationWizardPayloadBuildMode {
    return if (!lowRamProfile.enabled || showChecklistDetails) {
        PreparationWizardPayloadBuildMode.FullChecklist
    } else {
        PreparationWizardPayloadBuildMode.ActiveStepOnly
    }
}

internal fun createPreparationWizardStepPayload(
    currentStep: WizardStep,
    readiness: PreparationChecklistReadiness,
    sectionHealthMap: Map<String, SectionHealth>,
    sectionText: PreparationChecklistText,
    quickFixActions: List<PreparationQuickFixAction>,
    buildMode: PreparationWizardPayloadBuildMode
): PreparationWizardStepPayload {
    return PreparationWizardStepPayload(
        currentStep = currentStep,
        readiness = readiness,
        sectionHealth = sectionHealthMap[currentStep.sectionKey],
        sectionText = sectionText,
        quickFixActions = quickFixActions,
        buildMode = buildMode
    )
}

internal fun WizardStep.preparationSection(): PreparationSection {
    return when (this) {
        WizardStep.DeviceSetup -> PreparationSection.DeviceSetup
        WizardStep.Connectivity -> PreparationSection.Connectivity
        WizardStep.DeviceHealth -> PreparationSection.DeviceHealth
        WizardStep.RuntimeInteraction -> PreparationSection.RuntimeInteraction
        WizardStep.DeviceIntegrity -> PreparationSection.DeviceIntegrity
        WizardStep.Clipboard -> PreparationSection.Clipboard
        WizardStep.Location -> PreparationSection.Location
        WizardStep.DeviceLock -> PreparationSection.DeviceLock
        WizardStep.RuntimeSecurity -> PreparationSection.RuntimeSecurity
    }
}

internal fun resolveFirstIssueWizardStepIndex(
    stepStates: List<WizardStepState>
): Int {
    return stepStates.indexOfFirst { it.issueCount > 0 }.takeIf { it >= 0 } ?: 0
}

internal fun resolveWizardStepIndexForAutoFocus(
    currentStepIndex: Int,
    stepStates: List<WizardStepState>,
    userSelectedWizardStep: Boolean,
    autoFocusApplied: Boolean
): Int {
    val boundedCurrentIndex = currentStepIndex.coerceIn(
        0,
        (stepStates.size - 1).coerceAtLeast(0)
    )
    return if (userSelectedWizardStep || autoFocusApplied) {
        boundedCurrentIndex
    } else {
        resolveFirstIssueWizardStepIndex(stepStates)
    }
}

internal fun resolveWizardStepActionCoverage(
    stepState: WizardStepState?,
    quickFixActions: List<PreparationQuickFixAction>
): WizardStepActionCoverage {
    val hasIssue = (stepState?.issueCount ?: 0) > 0
    val hasActionOrNotice = quickFixActions.any {
        it.code != QuickFixRefreshAllSecurityChecksCode
    }
    return WizardStepActionCoverage(
        hasIssue = hasIssue,
        hasActionOrNotice = hasActionOrNotice
    )
}

/**
 * Builds wizard step states from the section health map.
 */
internal fun buildWizardStepStates(
    sectionHealthMap: Map<String, SectionHealth>
): List<WizardStepState> {
    return WizardStep.entries.map { step ->
        val health = sectionHealthMap[step.sectionKey]
        WizardStepState(
            step = step,
            isCompleted = health?.allClear ?: true,
            issueCount = health?.issueCount ?: 0
        )
    }
}

/**
 * Filters quick fix actions relevant to a specific wizard step.
 * Section metadata is the source of truth; target/code-prefix matching stays as
 * compatibility fallback for older tests and future actions that have not been tagged.
 */
internal fun filterQuickFixActionsForStep(
    step: WizardStep,
    allActions: List<PreparationQuickFixAction>
): List<PreparationQuickFixAction> {
    val relevantSection = step.preparationSection()
    val relevantTargets = when (step) {
        WizardStep.DeviceSetup -> emptySet<QuickFixTarget>() // keyboard/bluetooth fixes have null target
        WizardStep.Connectivity -> setOf(QuickFixTarget.Network)
        WizardStep.DeviceHealth -> setOf(QuickFixTarget.DeviceTime, QuickFixTarget.WebView)
        WizardStep.RuntimeInteraction -> emptySet() // accessibility/overlay fixes use QuickFixTarget.All
        WizardStep.DeviceIntegrity -> emptySet() // ADB/root/virtual env use QuickFixTarget.All
        WizardStep.Clipboard -> emptySet()
        WizardStep.Location -> setOf(QuickFixTarget.Location)
        WizardStep.DeviceLock -> setOf(QuickFixTarget.ScreenPinning)
        WizardStep.RuntimeSecurity -> setOf(
            QuickFixTarget.ScreenRecorder,
            QuickFixTarget.DisplayMirror,
            QuickFixTarget.MultiWindow
        )
    }

    // For steps that map to specific targets, include those.
    // For steps with emptySet(), we match by code prefix convention.
    val relevantCodePrefixes = when (step) {
        WizardStep.DeviceSetup -> listOf("quick_fix_60", "quick_fix_65", "quick_fix_200", "quick_fix_205")
        WizardStep.RuntimeInteraction -> listOf("quick_fix_35", "quick_fix_36", "quick_fix_220", "quick_fix_225")
        WizardStep.DeviceIntegrity -> listOf(
            "quick_fix_20", "adb_insecure_property", "root_detected",
            "selinux_permissive", "virtual_env_detected", "quick_fix_15"
        )
        WizardStep.Clipboard -> listOf("clipboard")
        WizardStep.RuntimeSecurity -> listOf("app_switch_violations")
        else -> emptyList()
    }

    return allActions.filter { action ->
        // Don't include the global "refresh all" in per-step view
        if (action.code == QuickFixRefreshAllSecurityChecksCode) return@filter false
        // Match explicit section first
        if (action.section == relevantSection) return@filter true
        // Match by target
        if (action.target != null && action.target in relevantTargets) return@filter true
        // Match by code prefix
        if (relevantCodePrefixes.any { prefix -> action.code.startsWith(prefix) }) return@filter true
        false
    }
}
