package com.coblax.examlock.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialogContent
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.UiTokens

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

/**
 * Page-load failure card, drawn over the WebView area. Same look as the exam popups,
 * but inline so it never takes window focus.
 */
@Composable
internal fun ExamWebErrorOverlay(
    examDisplayName: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppAlertDialogContent(
        tone = UiStatusTone.Danger,
        icon = Icons.Rounded.CloudOff,
        title = tr("Exam page could not load", "Halaman ujian belum berhasil dimuat"),
        badge = examDisplayName.ifBlank { null },
        message = errorMessage?.takeIf { it.isNotBlank() } ?: tr(
            "Check the connection, then try again.",
            "Periksa koneksi, lalu coba lagi."
        ),
        primaryAction = AppAlertAction(tr("Reload", "Muat ulang"), onRetry),
        modifier = modifier
    )
}




/**
 * Full-screen overlay shown while screen pinning is being activated.
 * Blocks all touches — prevents accidental Home/Recent presses.
 * No animations: safe for API 24 / 768MB RAM.
 */
@Composable
internal fun PinningActivationOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6101827)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1A2332))
                .border(1.dp, AppColors.current.goldDark.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AppColors.current.goldDark.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = AppColors.current.goldDark,
                        modifier = Modifier.padding(14.dp).size(36.dp)
                    )
                }
                Text(
                    text = tr("Activating Exam Lock...", "Mengaktifkan Kunci Ujian..."),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = tr(
                        "Android is setting up secure screen pinning. Follow the steps below carefully.",
                        "Android sedang mengatur screen pinning aman. Ikuti langkah berikut dengan seksama."
                    ),
                    color = Color(0xFFB0BED0),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
                PinningStep(
                    step = "1",
                    icon = Icons.Rounded.TouchApp,
                    title = tr("Tap \"Got it\" or \"Pin\"", "Ketuk \"Got it\" atau \"Pin\""),
                    desc = tr(
                        "When Android shows a pinning dialog, tap Got it or Pin.",
                        "Saat Android menampilkan dialog pinning, ketuk Got it atau Pin."
                    )
                )
                PinningStep(
                    step = "2",
                    icon = Icons.Rounded.PanTool,
                    title = tr("DO NOT press Home or Recent", "JANGAN tekan Home atau Recent"),
                    desc = tr(
                        "Pressing Home or Recent cancels the process — you must try again.",
                        "Menekan Home atau Recent membatalkan proses — Anda harus mencoba lagi."
                    ),
                    accentColor = AppColors.current.statusDanger
                )
                PinningStep(
                    step = "3",
                    icon = Icons.Rounded.Lock,
                    title = tr("Wait until Screen Pinning is active", "Tunggu sampai Screen Pinning aktif"),
                    desc = tr(
                        "Stay here. Start Exam becomes available after pinning is confirmed.",
                        "Tetap di sini. Mulai Ujian tersedia setelah pinning terkonfirmasi."
                    )
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(UiTokens.RadiusSm))
                        .background(AppColors.current.statusDanger.copy(alpha = 0.16f))
                        .border(1.dp, AppColors.current.statusDanger.copy(alpha = 0.25f), RoundedCornerShape(UiTokens.RadiusSm))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PanTool,
                        contentDescription = null,
                        tint = AppColors.current.statusDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tr(
                            "Do not touch Home or Recent during this process",
                            "Jangan sentuh Home atau Recent selama proses ini"
                        ),
                        color = AppColors.current.statusDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PinningStep(
    step: String,
    icon: ImageVector,
    title: String,
    desc: String,
    accentColor: Color = AppColors.current.goldDark
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = desc, color = Color(0xFF8DA5BE), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
