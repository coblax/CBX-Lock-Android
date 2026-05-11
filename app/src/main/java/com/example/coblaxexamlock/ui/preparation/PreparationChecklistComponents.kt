package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
import java.util.Locale

@Composable
internal fun PreparationAssistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
    labelPrefix: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 38.dp else 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) LockBlue else Color.White,
            contentColor = if (filled) LockOnDark else LockBlueDeep
        ),
        border = if (filled) null else BorderStroke(1.dp, LockOutline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = if (filled) LockOnDark else LockBlueDeep
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            labelPrefix?.let { prefix ->
                Text(
                    text = prefix,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun SecurityChecklistItem(
    title: String,
    value: String,
    meta: String? = null,
    metaColor: Color? = null,
    detail: String? = null,
    status: String,
    onSendTelegram: () -> Unit,
    isSending: Boolean,
    sendEnabled: Boolean,
    showSendButton: Boolean = true
) {
    val normalizedStatus = status.trim().lowercase(Locale.US)
    val accentColor = when (normalizedStatus) {
        "aman", "safe", "siap", "ready", "aktif", "active", "diizinkan", "allowed",
        "stable", "stabil",
        "inside area", "di dalam area", "clean", "bersih" ->
            Color(0xFF2F8F63)
        "dipantau", "monitored", "fallback", "warning", "peringatan",
        "package warning", "peringatan paket", "needs fix", "perlu perbaikan",
        "available", "tersedia", "check", "cek",
        "stale fix", "fix kedaluwarsa", "low accuracy", "akurasi rendah",
        "missing accuracy", "akurasi tidak ada", "no fix", "belum ada fix",
        "needs location permission", "butuh izin lokasi", "location services off", "layanan lokasi off",
        "waiting for location", "menunggu lokasi",
        "offline", "captive portal", "unvalidated", "belum tervalidasi",
        "unstable", "tidak stabil", "airplane mode", "mode pesawat" ->
            LockGoldDark
        "bypassed", "bypass", "policy off", "policy nonaktif", "disabled", "nonaktif" ->
            Color(0xFF5C6B7A)
        else -> Color(0xFFB34A4A)
    }
    val badgeBackground = when (normalizedStatus) {
        "aman", "safe", "siap", "ready", "aktif", "active", "diizinkan", "allowed",
        "stable", "stabil",
        "inside area", "di dalam area", "clean", "bersih" ->
            Color(0xFFE8F6EE)
        "dipantau", "monitored", "fallback", "warning", "peringatan",
        "package warning", "peringatan paket", "needs fix", "perlu perbaikan",
        "available", "tersedia", "check", "cek",
        "stale fix", "fix kedaluwarsa", "low accuracy", "akurasi rendah",
        "missing accuracy", "akurasi tidak ada", "no fix", "belum ada fix",
        "needs location permission", "butuh izin lokasi", "location services off", "layanan lokasi off",
        "waiting for location", "menunggu lokasi",
        "offline", "captive portal", "unvalidated", "belum tervalidasi",
        "unstable", "tidak stabil", "airplane mode", "mode pesawat" ->
            LockGold.copy(alpha = 0.18f)
        "bypassed", "bypass", "policy off", "policy nonaktif", "disabled", "nonaktif" ->
            Color(0xFFE9EEF3)
        else -> Color(0xFFFFEAEA)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSendButton) {
                val sendButtonColor =
                    if (sendEnabled || isSending) Color(0xFF2AABEE) else Color(0xFFB5DDF3)
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            enabled = sendEnabled && !isSending,
                            onClick = onSendTelegram
                        ),
                    shape = CircleShape,
                    color = sendButtonColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = tr(
                                    "Send diagnostics to Telegram",
                                    "Kirim diagnostik ke Telegram"
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.90f))
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = LockTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = value,
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 3
                )
                if (!meta.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = meta,
                        color = metaColor ?: accentColor,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        maxLines = 4
                    )
                }
                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail,
                        color = LockTextMuted,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = badgeBackground
            ) {
                Text(
                    text = status,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun PreparationSummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.90f))
                )
                Text(
                    text = label.uppercase(Locale.US),
                    color = LockTextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
            Text(
                text = value,
                color = LockTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun CompactPrepActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(76.dp)
            .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.20f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun PreparationFloatingActionBar(
    startButtonColor: Color,
    startButtonContentColor: Color,
    canStartExam: Boolean,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    onRefreshStatus: () -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = LockBlueDeep,
        border = BorderStroke(1.dp, LockBlueDeep),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactPrepActionButton(
                icon = Icons.Rounded.Refresh,
                label = "Refresh",
                onClick = onRefreshStatus
            )
            val lowRam = LocalLowRamProfile.current
            val startEnabled = canStartExam && !(isStartingExam || webViewSessionResetInFlight)
            val buttonBg = if (!lowRam.enabled && startEnabled) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(startButtonColor.copy(alpha = 0.88f), startButtonColor)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            } else {
                Modifier
            }
            Button(
                onClick = onStartExam,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .then(buttonBg),
                shape = RoundedCornerShape(20.dp),
                enabled = startEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = startButtonColor,
                    contentColor = startButtonContentColor,
                    disabledContainerColor = startButtonColor.copy(alpha = 0.85f),
                    disabledContentColor = startButtonContentColor
                )
            ) {
                Text(
                    text = if (webViewSessionResetInFlight) {
                        tr("PREPARING CLEAN SESSION...", "MENYIAPKAN SESI BERSIH...")
                    } else if (isStartingExam) {
                        tr("STARTING / VALIDATING...", "MEMULAI / VALIDASI...")
                    } else {
                        tr("START EXAM MODE", "MULAI UJIAN")
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            CompactPrepActionButton(
                icon = Icons.Rounded.Home,
                label = "Menu",
                onClick = onBackHome
            )
        }
    }
}
