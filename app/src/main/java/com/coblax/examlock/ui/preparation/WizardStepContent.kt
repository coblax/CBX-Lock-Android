package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockWarnBgSoft
import com.coblax.examlock.ui.theme.UiTokens

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Step Section Content (renders the actual checklist section)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun WizardStepSectionContent(
    step: WizardStep,
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    text: PreparationChecklistText,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: com.coblax.examlock.AccessibilityInspectionResult,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean
) {
    when (step) {
        WizardStep.DeviceSetup -> PreparationDeviceSetupSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            needsBluetoothPermission = needsBluetoothPermission,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Connectivity -> PreparationConnectivitySection(
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceHealth -> PreparationDeviceHealthSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.RuntimeInteraction -> PreparationRuntimeInteractionSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityInspection = accessibilityInspection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceIntegrity -> PreparationDeviceIntegritySection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Clipboard -> PreparationRuntimeClipboardSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.Location -> PreparationLocationSection(
            location = state.location,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.DeviceLock -> PreparationDeviceLockSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        WizardStep.RuntimeSecurity -> PreparationRuntimeStaticSecuritySection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Manual Fix Hint Card
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun WizardManualFixHintCard() {
    PreparationNoticeCard(
        title = tr("Manual Fix Needed", "Perbaikan Manual Dibutuhkan"),
        message = tr(
            "No automatic button is available for this section yet. Open Technical Details or press Refresh after the manual fix.",
            "Belum ada tombol otomatis untuk bagian ini. Buka Detail Teknis atau tekan Refresh setelah perbaikan manual."
        ),
        accentColor = LockGoldDark,
        backgroundColor = LockWarnBgSoft
    )
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Per-Step Quick Fix Card
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun WizardStepQuickFixCard(
    actions: List<PreparationQuickFixAction>,
    hasGlobalBlockingIssues: Boolean
) {
    val lowRamProfile = LocalLowRamProfile.current
    val displayActions = remember(actions, lowRamProfile.enabled, lowRamProfile.ultra, hasGlobalBlockingIssues) {
        selectPreparationQuickFixActionsForDisplay(
            actions = actions,
            lowRamProfile = lowRamProfile,
            hasGlobalBlockingIssues = hasGlobalBlockingIssues
        )
    }
    val visibleActions = remember(displayActions) {
        buildList {
            displayActions.primary
                ?.takeIf { !it.isNotice }
                ?.let(::add)
            addAll(displayActions.blocking)
            addAll(displayActions.warnings)
            displayActions.refresh?.let(::add)
        }.distinctBy { it.code }
    }
    val blockingActions = visibleActions.filter { it.severity == QuickFixSeverity.Blocking && !it.isNotice }
    val warningActions = visibleActions.filter { it.severity == QuickFixSeverity.Warning && !it.isNotice }
    val notices = displayActions.notices

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (blockingActions.isNotEmpty()) WizardRed.copy(alpha = 0.30f)
                else LockGoldDark.copy(alpha = 0.30f),
                RoundedCornerShape(UiTokens.RadiusLg)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = tr("Quick Fix", "Perbaikan Cepat"),
            color = LockTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        notices.forEach { notice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(UiTokens.RadiusSm))
                    .background(LockWarnBgSoft)
                    .border(1.dp, LockGoldDark.copy(alpha = 0.35f), RoundedCornerShape(UiTokens.RadiusSm))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = notice.text,
                    color = LockGoldDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val stepNumbers = listOf("â‘ ", "â‘¡", "â‘¢", "â‘£", "â‘¤", "â‘¥", "â‘¦", "â‘§", "â‘¨", "â‘©")
        var stepIndex = 0

        blockingActions.forEach { action ->
            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
            stepIndex++
            PreparationAssistButton(
                text = action.displayTextForProfile(lowRamProfile),
                labelPrefix = stepLabel,
                compact = true,
                filled = action.filled,
                loading = action.loading,
                enabled = action.enabled,
                onClick = action.onClick
            )
            if (!action.reason.isNullOrBlank()) {
                Text(
                    text = action.reason,
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        warningActions.forEach { action ->
            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
            stepIndex++
            PreparationAssistButton(
                text = action.displayTextForProfile(lowRamProfile),
                labelPrefix = stepLabel,
                compact = true,
                filled = false,
                loading = action.loading,
                enabled = action.enabled,
                onClick = action.onClick
            )
            if (!action.reason.isNullOrBlank()) {
                Text(
                    text = action.reason,
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
