package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    runQuickFix: (QuickFixTarget?, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickFixActions = buildPreparationQuickFixActions(
        state = state,
        actions = actions,
        accessibilityGuardRequired = accessibilityGuardRequired,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        geofenceReady = geofenceReady,
        fakeLocationReady = fakeLocationReady,
        needsBluetoothPermission = needsBluetoothPermission,
        runQuickFix = runQuickFix
    )
    val primaryQuickFixAction = quickFixActions.firstOrNull()
    val remainingQuickFixActions = quickFixActions.drop(1)
    val blockingQuickFixActions = remainingQuickFixActions.filter { it.severity == QuickFixSeverity.Blocking }
    val warningQuickFixActions = remainingQuickFixActions.filter { it.severity == QuickFixSeverity.Warning }
    val blockingQuickFixCount = quickFixActions.count { it.severity == QuickFixSeverity.Blocking }
    val warningQuickFixCount = quickFixActions.count { it.severity == QuickFixSeverity.Warning && it.priority != 900 }
    val quickFixBorderColor = if (blockingQuickFixCount > 0)
        Color(0xFFB34A4A).copy(alpha = 0.30f)
    else
        LockGoldDark.copy(alpha = 0.30f)
    val quickFixAccentColor = if (blockingQuickFixCount > 0) Color(0xFFB34A4A) else LockGoldDark
    val showQuickFixesCard = quickFixActions.isNotEmpty()

    if (showQuickFixesCard) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, quickFixBorderColor),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Left accent stripe; no animation, safe for low-RAM / API 24.
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
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

                    val primaryAction = primaryQuickFixAction
                    if (primaryAction != null) {
                        PreparationAssistButton(
                            text = primaryAction.text,
                            labelPrefix = tr("Fix First", "Perbaiki Dulu"),
                            filled = true,
                            loading = primaryAction.loading,
                            enabled = primaryAction.enabled,
                            onClick = primaryAction.onClick
                        )
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
                        blockingQuickFixActions.forEach { action ->
                            PreparationAssistButton(
                                text = action.text,
                                compact = true,
                                filled = action.filled,
                                loading = action.loading,
                                enabled = action.enabled,
                                onClick = action.onClick
                            )
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
                        warningQuickFixActions.forEach { action ->
                            PreparationAssistButton(
                                text = action.text,
                                compact = true,
                                filled = action.filled,
                                loading = action.loading,
                                enabled = action.enabled,
                                onClick = action.onClick
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}
