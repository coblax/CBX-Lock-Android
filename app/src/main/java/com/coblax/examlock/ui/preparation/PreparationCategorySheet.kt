package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coblax.examlock.AccessibilityInspectionResult
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.LocalTelegramDiagnosticsEnabled
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.InScreenSheetHost

/**
 * Renders the existing technical checklist rows for one category.
 */
@Composable
internal fun PreparationCategorySectionContent(
    category: PreparationCategory,
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    text: PreparationChecklistText,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: AccessibilityInspectionResult,
    accessibilityGuardAvailable: Boolean,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean
) {
    when (category) {
        PreparationCategory.DeviceSetup -> PreparationDeviceSetupSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            needsBluetoothPermission = needsBluetoothPermission,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.Connectivity -> PreparationConnectivitySection(
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.DeviceHealth -> PreparationDeviceHealthSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.RuntimeInteraction -> PreparationRuntimeInteractionSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityInspection = accessibilityInspection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.DeviceIntegrity -> PreparationDeviceIntegritySection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.Clipboard -> PreparationRuntimeClipboardSection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.Location -> PreparationLocationSection(
            location = state.location,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.DeviceLock -> PreparationDeviceLockSection(
            device = state.device,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
        PreparationCategory.RuntimeSecurity -> PreparationRuntimeStaticSecuritySection(
            runtimeSecurity = state.runtimeSecurity,
            bypass = state.bypass,
            text = text,
            sendingSection = state.session.sendingSection,
            onRequestSectionReport = actions.session.onRequestSectionReport
        )
    }
}

/** Category details open in the shared in-screen sheet (see [InScreenSheetHost]). */
@Composable
internal fun PreparationCategorySheetHost(
    openCategory: PreparationCategory?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PreparationCategory) -> Unit
) {
    InScreenSheetHost(
        value = openCategory,
        onDismiss = onDismiss,
        closeLabel = tr("Close details", "Tutup detail"),
        modifier = modifier,
        maxWidth = PreparationContentMaxWidth,
        sheetTag = PreparationUiTestTags.DetailSheet,
        scrimTag = PreparationUiTestTags.DetailSheetScrim,
        content = content
    )
}

@Composable
internal fun PreparationCategorySheetContent(
    status: PreparationCategoryStatus,
    hasGlobalBlockingIssues: Boolean,
    showGeofenceMapAction: Boolean,
    onOpenGeofenceMap: () -> Unit,
    onDismiss: () -> Unit,
    technicalDetails: @Composable () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val uiLanguage = LocalUiLanguage.current
    val telegramEnabled = LocalTelegramDiagnosticsEnabled.current
    val colors = preparationStatusColors(status.tone.uiStatusTone())
    val title = status.category.title(uiLanguage)

    Column(
        modifier = Modifier.semantics { paneTitle = title }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spaceLarge, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreparationCategoryBadge(category = status.category, colors = colors, size = 40.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { heading() }
            ) {
                Text(
                    text = title,
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.sectionTitle
                )
                Text(
                    text = status.category.description(uiLanguage),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(tokens.touchTarget)
                    .testTag(PreparationUiTestTags.DetailSheetClose)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = tr("Close details", "Tutup detail"),
                    tint = AppColors.current.textSecondary
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = tokens.spaceLarge,
                    end = tokens.spaceLarge,
                    top = tokens.spaceSmall,
                    bottom = tokens.spaceLarge
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.spaceMedium)
        ) {
            StatusBadge(
                label = preparationToneLabel(status),
                tone = status.tone.uiStatusTone()
            )
            if (status.issues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spaceSmall)) {
                    status.issues.forEach { issue ->
                        PreparationIssueRow(issue = issue)
                    }
                }
            }
            PreparationCategoryActions(
                actions = status.actions,
                hasGlobalBlockingIssues = hasGlobalBlockingIssues,
                emphasizeFirst = status.tone == PreparationTone.Blocking
            )
            if (showGeofenceMapAction) {
                OutlinedButton(
                    onClick = onOpenGeofenceMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.touchTarget),
                    shape = RoundedCornerShape(tokens.radiusMedium),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.current.brandText
                    ),
                    border = BorderStroke(1.dp, AppColors.current.outline)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("View exam area map", "Lihat peta area ujian"),
                        style = AppTextStyles.button
                    )
                }
            }
            PreparationSectionHeader(text = tr("Technical details", "Detail teknis"))
            if (telegramEnabled) {
                Text(
                    text = tr(
                        "Tap the Telegram icon on an item to send diagnostics for it.",
                        "Ketuk ikon Telegram pada item untuk mengirim diagnostiknya."
                    ),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic.copy(fontWeight = FontWeight.Normal)
                )
            }
            technicalDetails()
        }
    }
}
