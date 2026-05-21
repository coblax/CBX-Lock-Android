package com.example.coblaxexamlock.ui.admin

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.LowRamProfileOverride
import com.example.coblaxexamlock.isLowRamProfileOverrideRisky
import com.example.coblaxexamlock.lowRamProfileBadgeLabel
import com.example.coblaxexamlock.lowRamProfileOverrideOptions
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockBlueSoft
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

@Composable
internal fun PerformanceProfileGearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutline.copy(alpha = 0.72f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = tr(
                "Open performance profile settings",
                "Buka pengaturan profil performa"
            ),
            tint = LockBlueDeep,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun PublicPerformanceProfileDialog(
    selectedOverride: LowRamProfileOverride,
    detectedProfile: LowRamProfile,
    effectiveProfile: LowRamProfile,
    onOverrideChange: (LowRamProfileOverride) -> Unit,
    onDismiss: () -> Unit
) {
    val overrideOptions = remember { lowRamProfileOverrideOptions() }
    val riskyOverride = isLowRamProfileOverrideRisky(detectedProfile, selectedOverride)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockBackground,
        title = {
            Text(
                text = tr("Performance Profile", "Profil Performa"),
                color = LockTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr(
                        "Mode ini hanya mengatur beban UI dan interval polling. Proteksi ujian tetap aktif.",
                        "Mode ini hanya mengatur beban UI dan interval polling. Proteksi ujian tetap aktif."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    overrideOptions.chunked(2).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { option ->
                                PerformanceProfileOptionPill(
                                    override = option,
                                    selected = selectedOverride == option,
                                    onClick = { onOverrideChange(option) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowOptions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                when {
                    riskyOverride -> StatusBanner(
                        message = tr(
                            "Mode terpilih lebih ringan dari hasil deteksi. Di HP RAM kecil, pilihan ini bisa membuat UI atau WebView lebih lag.",
                            "Mode terpilih lebih ringan dari hasil deteksi. Di HP RAM kecil, pilihan ini bisa membuat UI atau WebView lebih lag."
                        ),
                        isError = true
                    )
                    selectedOverride == LowRamProfileOverride.Ultra -> StatusBanner(
                        message = tr(
                            "Ultra membuat UI lebih hemat dan polling berkala lebih jarang. Alarm dan proteksi event-based tetap aktif.",
                            "Ultra membuat UI lebih hemat dan polling berkala lebih jarang. Alarm dan proteksi event-based tetap aktif."
                        ),
                        isError = false
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, LockOutline.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PerformanceProfileSummaryRow(
                        label = tr("Detected", "Terdeteksi"),
                        value = lowRamProfileBadgeLabel(detectedProfile)
                    )
                    PerformanceProfileSummaryRow(
                        label = tr("Active", "Aktif"),
                        value = lowRamProfileBadgeLabel(effectiveProfile)
                    )
                    PerformanceProfileSummaryRow(
                        label = "RAM",
                        value = "avail=${effectiveProfile.availableMemoryMb ?: "-"}MB total=${effectiveProfile.totalMemoryMb ?: "-"}MB"
                    )
                    PerformanceProfileSummaryRow(
                        label = tr("Polling", "Polling"),
                        value = "${effectiveProfile.slowPollingMultiplier}x"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Close", "Tutup"), color = LockBlue)
            }
        }
    )
}

@Composable
private fun PerformanceProfileOptionPill(
    override: LowRamProfileOverride,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val containerColor = if (selected) {
        performanceProfileSelectedContainer(override)
    } else {
        Color.White
    }
    val borderColor = if (selected) {
        performanceProfileDotColor(override).copy(alpha = 0.65f)
    } else {
        LockOutline.copy(alpha = 0.65f)
    }
    val contentColor = if (selected && override == LowRamProfileOverride.Ultra) {
        LockOnDark
    } else {
        LockTextPrimary
    }
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                role = Role.RadioButton,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(performanceProfileDotColor(override))
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = performanceProfileTitle(override),
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = performanceProfileSubtitle(override),
                color = if (selected && override == LowRamProfileOverride.Ultra) {
                    LockOnDark.copy(alpha = 0.74f)
                } else {
                    LockTextSecondary
                },
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PerformanceProfileSummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LockTextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = LockTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

private fun performanceProfileTitle(override: LowRamProfileOverride): String =
    when (override) {
        LowRamProfileOverride.Auto -> "Auto"
        LowRamProfileOverride.Normal -> "Normal"
        LowRamProfileOverride.Low -> "Low"
        LowRamProfileOverride.Ultra -> "Ultra"
    }

private fun performanceProfileSubtitle(override: LowRamProfileOverride): String =
    when (override) {
        LowRamProfileOverride.Auto -> "Deteksi"
        LowRamProfileOverride.Normal -> "Penuh"
        LowRamProfileOverride.Low -> "Ringan"
        LowRamProfileOverride.Ultra -> "Paling ringan"
    }

private fun performanceProfileDotColor(override: LowRamProfileOverride): Color =
    when (override) {
        LowRamProfileOverride.Auto -> LockBlue
        LowRamProfileOverride.Normal -> Color(0xFF2E7D32)
        LowRamProfileOverride.Low -> LockGoldDark
        LowRamProfileOverride.Ultra -> LockGold
    }

private fun performanceProfileSelectedContainer(override: LowRamProfileOverride): Color =
    when (override) {
        LowRamProfileOverride.Auto -> LockBlueSoft.copy(alpha = 0.56f)
        LowRamProfileOverride.Normal -> Color(0xFFEFFAF1)
        LowRamProfileOverride.Low -> Color(0xFFFFF8E6)
        LowRamProfileOverride.Ultra -> LockBlueDeep
    }
