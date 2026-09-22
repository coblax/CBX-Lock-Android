package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiTokens

// ──────────────────────────────────────────────────────────────
// Step Section Content (renders the actual checklist section)
// ──────────────────────────────────────────────────────────────

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

@Composable
internal fun WizardStepDetailsCard(
    step: WizardStep,
    issueCount: Int,
    content: @Composable () -> Unit
) {
    val tokens = ScopedUiTokens.current
    var expanded by rememberSaveable(step.sectionKey) {
        mutableStateOf(shouldExpandWizardStepDetails(issueCount))
    }

    LaunchedEffect(step, issueCount) {
        if (issueCount > 0) {
            expanded = true
        }
    }
    val detailsStateDescription = if (expanded) {
        tr("Checklist details expanded", "Detail checklist terbuka")
    } else {
        tr("Checklist details collapsed", "Detail checklist tertutup")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PreparationWizardUiTestTags.DetailsCard)
            .clip(RoundedCornerShape(tokens.radiusLarge))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                AppColors.current.outlineMedium,
                RoundedCornerShape(tokens.radiusLarge)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PreparationWizardUiTestTags.DetailsToggle)
                .heightIn(min = tokens.touchTarget)
                .semantics {
                    role = Role.Button
                    stateDescription = detailsStateDescription
                }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = tokens.spaceMedium, vertical = tokens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.FactCheck,
                contentDescription = null,
                tint = AppColors.current.blue,
                modifier = Modifier.size(21.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = tr("Checklist details", "Detail checklist"),
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.cardTitle
                )
                Text(
                    text = if (expanded) {
                        tr("Select to hide technical checks", "Pilih untuk menyembunyikan pemeriksaan teknis")
                    } else {
                        tr("Select to review technical checks", "Pilih untuk meninjau pemeriksaan teknis")
                    },
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) {
                    tr("Hide checklist details", "Sembunyikan detail checklist")
                } else {
                    tr("Show checklist details", "Tampilkan detail checklist")
                },
                tint = AppColors.current.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PreparationWizardUiTestTags.DetailsContent)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                content()
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Manual Fix Hint Card
// ──────────────────────────────────────────────────────────────

@Composable
internal fun WizardManualFixHintCard() {
    PreparationNoticeCard(
        title = tr("Manual Fix Needed", "Perbaikan Manual Dibutuhkan"),
        message = tr(
            "No automatic button is available for this section yet. Open Technical Details or press Refresh after the manual fix.",
            "Belum ada tombol otomatis untuk bagian ini. Buka Detail Teknis atau tekan Refresh setelah perbaikan manual."
        ),
        accentColor = AppColors.current.goldDark,
        backgroundColor = AppColors.current.warnBgSoft
    )
}

// ──────────────────────────────────────────────────────────────
// Per-Step Quick Fix Card
// ──────────────────────────────────────────────────────────────

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
                else AppColors.current.goldDark.copy(alpha = 0.30f),
                RoundedCornerShape(UiTokens.RadiusLg)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = tr("Recommended actions", "Tindakan yang disarankan"),
            color = AppColors.current.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        notices.forEach { notice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(UiTokens.RadiusSm))
                    .background(AppColors.current.warnBgSoft)
                    .border(1.dp, AppColors.current.goldDark.copy(alpha = 0.35f), RoundedCornerShape(UiTokens.RadiusSm))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = notice.text,
                    color = AppColors.current.goldDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        var stepIndex = 0

        blockingActions.forEach { action ->
            val stepLabel = "${stepIndex + 1}."
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
                    color = AppColors.current.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        warningActions.forEach { action ->
            val stepLabel = "${stepIndex + 1}."
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
                    color = AppColors.current.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
