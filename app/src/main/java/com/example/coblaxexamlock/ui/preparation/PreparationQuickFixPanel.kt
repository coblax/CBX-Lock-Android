package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

@Composable
internal fun PreparationQuickFixPanel(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    accessibilityGuardRequired: Boolean,
    accessibilityGuardEnabled: Boolean,
    geofenceReady: Boolean,
    fakeLocationReady: Boolean,
    needsBluetoothPermission: Boolean,
    accessibilityInspection: AccessibilityInspectionResult,
    runQuickFix: (QuickFixTarget?, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiLanguage = LocalUiLanguage.current
    val lowRamProfile = LocalLowRamProfile.current
    val quickFixActions = remember(
        state.network,
        state.device,
        state.location,
        state.runtimeSecurity,
        state.bypass,
        state.diagnostics,
        actions.session,
        actions.network,
        actions.device,
        actions.location,
        actions.runtimeSecurity,
        uiLanguage,
        accessibilityGuardRequired,
        accessibilityGuardEnabled,
        geofenceReady,
        fakeLocationReady,
        needsBluetoothPermission,
        accessibilityInspection,
        runQuickFix
    ) {
        buildPreparationQuickFixActions(
            state = state,
            actions = actions,
            uiLanguage = uiLanguage,
            accessibilityGuardRequired = accessibilityGuardRequired,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            geofenceReady = geofenceReady,
            fakeLocationReady = fakeLocationReady,
            needsBluetoothPermission = needsBluetoothPermission,
            accessibilityInspection = accessibilityInspection,
            runQuickFix = runQuickFix
        )
    }
    val displayActions = remember(quickFixActions, lowRamProfile) {
        selectPreparationQuickFixActionsForDisplay(
            actions = quickFixActions,
            lowRamProfile = lowRamProfile
        )
    }
    val noticeQuickFixActions = displayActions.notices
    val pinningDeferredNotice = noticeQuickFixActions.firstOrNull {
        it.code == QuickFixScreenPinningDeferredCode
    }
    LaunchedEffect(pinningDeferredNotice?.diagnosticDetails) {
        val details = pinningDeferredNotice?.diagnosticDetails ?: return@LaunchedEffect
        actions.onScreenPinningDeferred(details)
    }
    val primaryQuickFixAction = displayActions.primary
    val blockingQuickFixActions = displayActions.blocking
    val warningQuickFixActions = displayActions.warnings
    val refreshQuickFixAction = displayActions.refresh
    val blockingQuickFixCount = displayActions.blockingCount
    val warningQuickFixCount = displayActions.warningCount
    val quickFixBorderColor = if (blockingQuickFixCount > 0)
        Color(0xFFB34A4A).copy(alpha = 0.30f)
    else
        LockGoldDark.copy(alpha = 0.30f)
    val quickFixAccentColor = if (blockingQuickFixCount > 0) Color(0xFFB34A4A) else LockGoldDark
    val showQuickFixesCard = quickFixActions.isNotEmpty()

    if (showQuickFixesCard) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, quickFixBorderColor, RoundedCornerShape(20.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Left accent stripe; no animation, safe for low-RAM / API 24.
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                        .background(quickFixAccentColor)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = tr("Auto-Fix Assistant", "Asisten Auto-Fix"),
                        color = LockTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (blockingQuickFixCount > 0) {
                            tr(
                                "$blockingQuickFixCount blocker(s) must be resolved before START EXAM MODE.",
                                "$blockingQuickFixCount penghambat harus dibereskan sebelum START EXAM MODE."
                            )
                        } else {
                            tr(
                                "$warningQuickFixCount warning(s) need review before starting the exam.",
                                "$warningQuickFixCount peringatan perlu ditinjau sebelum mulai ujian."
                            )
                        } + " " + tr(
                            "Fix the first item, then return here.",
                            "Perbaiki item pertama, lalu kembali ke sini."
                        ),
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    noticeQuickFixActions.forEach { notice ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFF8E6))
                                .border(1.dp, LockGoldDark.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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

                    val stepNumbers = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩")
                    var stepIndex = 0

                    val primaryAction = primaryQuickFixAction
                    if (primaryAction != null) {
                        val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
                        stepIndex++
                        PreparationAssistButton(
                            text = primaryAction.text,
                            labelPrefix = "$stepLabel ${tr("Fix First", "Perbaiki Dulu")}",
                            filled = true,
                            loading = primaryAction.loading,
                            enabled = primaryAction.enabled,
                            onClick = primaryAction.onClick
                        )
                        if (!primaryAction.reason.isNullOrBlank()) {
                            Text(
                                text = primaryAction.reason,
                                color = LockTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    if (blockingQuickFixActions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB34A4A))
                            )
                            Text(
                                text = tr("Blocking Fixes", "Perbaikan Wajib"),
                                color = LockTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        blockingQuickFixActions.forEachIndexed { _, action ->
                            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
                            stepIndex++
                            PreparationAssistButton(
                                text = action.text,
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
                    }

                    if (warningQuickFixActions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LockGoldDark)
                            )
                            Text(
                                text = tr("Optional Checks", "Cek Opsional"),
                                color = LockTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        warningQuickFixActions.forEachIndexed { _, action ->
                            val stepLabel = stepNumbers.getOrElse(stepIndex) { "${stepIndex + 1}." }
                            stepIndex++
                            PreparationAssistButton(
                                text = action.text,
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
                    }

                    if (refreshQuickFixAction != null) {
                        PreparationAssistButton(
                            text = refreshQuickFixAction.text,
                            compact = true,
                            filled = false,
                            loading = refreshQuickFixAction.loading,
                            enabled = refreshQuickFixAction.enabled,
                            onClick = refreshQuickFixAction.onClick
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}
