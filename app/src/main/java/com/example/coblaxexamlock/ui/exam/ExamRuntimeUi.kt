package com.example.coblaxexamlock.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockStatusDanger
import com.example.coblaxexamlock.ui.theme.LockStatusDangerFill
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

internal data class ExamRuntimeChromeState(
    val examSessionStarted: Boolean,
    val examDisplayName: String,
    val loadingProgress: Float,
    val webViewErrorMessage: String?,
    val hasFullscreenCustomView: Boolean,
    val useBuiltInExamKeyboard: Boolean,
    val showBuiltInExamKeyboard: Boolean,
    val showSideArrowControls: Boolean,
    @Suppress("unused")
    val hasEditableFocus: Boolean,
    val builtInKeyboardShiftEnabled: Boolean,
    val networkStatus: NetworkReadinessStatus,
    val serverStatus: ExamServerFooterStatus,
    val batteryStatus: ExamBatteryStatus,
    val shieldStatus: ExamFooterShieldStatus
)

internal data class ExamRuntimeChromeActions(
    val onRetryLoading: () -> Unit,
    val onRefreshPage: () -> Unit,
    val onGoHome: () -> Unit,
    val onTextKey: (String) -> Unit,
    val onBackspace: () -> Unit,
    val onArrowLeft: () -> Unit,
    val onArrowRight: () -> Unit,
    val onToggleSideArrowControls: () -> Unit,
    val onEnter: () -> Unit,
    val onSpace: () -> Unit,
    val onShiftToggle: () -> Unit
)

@Composable
internal fun ExamWebErrorOverlay(
    examDisplayName: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, LockStatusDanger.copy(alpha = 0.25f))
    ) {
        // Subtle gradient background tint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(LockStatusDangerFill.copy(alpha = 0.55f), Color.White)
                    )
                )
        ) {
            // Left accent border stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(LockStatusDanger)
            )

            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big error icon
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = LockStatusDanger.copy(alpha = 0.80f),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = tr("Exam page is not available yet", "Halaman ujian belum berhasil dimuat"),
                    color = LockTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                )
                Text(
                    text = examDisplayName,
                    color = LockBlueDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = LockTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LockBlue,
                        contentColor = LockOnDark
                    )
                ) {
                    Text(tr("Retry Loading", "Muat Ulang"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


