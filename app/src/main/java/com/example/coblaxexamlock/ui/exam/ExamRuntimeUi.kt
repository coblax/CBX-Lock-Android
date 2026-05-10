package com.example.coblaxexamlock.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

internal enum class ExamFooterShieldStatus {
    Safe,
    Warning,
    Danger
}

internal enum class ExamServerFooterStatus {
    Checking,
    Online,
    Warning,
    Offline
}

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
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = tr("Exam page is not available yet", "Halaman ujian belum berhasil dimuat"),
                color = LockTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 25.sp
            )
            Text(
                text = examDisplayName,
                color = LockBlueDeep,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = errorMessage.orEmpty(),
                color = LockTextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
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

@Composable
internal fun ExamStatusPill(
    dotColor: Color,
    text: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 6.dp else 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 7.dp else 8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                color = LockTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

