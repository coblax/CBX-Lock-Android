package com.coblax.examlock.ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.coblax.examlock.calculateQrExportBitmapSpec
import com.coblax.examlock.config.PickerDialogColorScheme
import com.coblax.examlock.ExamQrExportHelper
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.formatExamScheduleDateTime
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.R
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockBlueSoft
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockBlueTint
import com.coblax.examlock.ui.theme.LockOutlineStrong
import com.coblax.examlock.ui.theme.LockOutlineMedium
import com.google.android.libraries.places.api.model.Place

import java.util.Calendar
import java.util.Date

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@Composable
internal fun BackPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LockBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = tr("Main menu", "Menu utama"),
                tint = LockOnDark,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = "MENU",
            color = LockTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
internal fun AdminInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = LockTextPrimary,
            fontSize = 16.sp
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = LockTextMuted,
                fontSize = 16.sp
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LockSurfaceSoft,
            unfocusedContainerColor = LockSurfaceSoft,
            focusedBorderColor = LockBlue,
            unfocusedBorderColor = LockOutline,
            focusedTextColor = LockTextPrimary,
            unfocusedTextColor = LockTextPrimary,
            cursorColor = LockBlue,
            focusedPlaceholderColor = LockTextMuted,
            unfocusedPlaceholderColor = LockTextMuted
        )
    )
}

@Composable
internal fun AdminPickerField(
    value: String,
    placeholder: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LockSurfaceSoft)
            .border(
                width = 1.dp,
                color = if (isActive) LockBlue else LockOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isBlank = value.isBlank()
        Text(
            text = value.ifBlank { placeholder },
            color = if (isBlank) LockTextMuted else LockTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = tr("Pick", "Pilih"),
            color = LockBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun AdminToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusSm))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = LockTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = LockTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LockBlue,
                uncheckedThumbColor = LockBlue.copy(alpha = 0.6f),
                uncheckedTrackColor = LockOutlineMedium
            )
        )
    }
}


@Composable
internal fun StatusBanner(
    message: String,
    isError: Boolean
) {
    val backgroundColor = if (isError) Color(0xFFFFF4F4) else Color(0xFFEFF6FF)
    val borderColor = if (isError) Color(0xFFE9B4B4) else Color(0xFFB8D2FF)
    val textColor = if (isError) Color(0xFF9A3030) else LockBlueDeep

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(backgroundColor)
            .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(UiTokens.RadiusSm))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
internal fun GeneratedQrCard(
    encryptedPayload: String,
    examName: String,
    startTime: String,
    endTime: String,
    locationPolicy: ExamQrLocationPolicy
) {
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val previewBitmapSize = when {
        lowRamProfile.severe -> 384
        lowRamProfile.enabled -> 512
        else -> 640
    }
    val previewDisplaySize = when {
        lowRamProfile.severe -> 180.dp
        lowRamProfile.enabled -> 200.dp
        else -> 220.dp
    }
    val exportBitmapSpec = remember(lowRamProfile) {
        calculateQrExportBitmapSpec(lowRamProfile)
    }
    val shareFailedMessage = tr("Failed to open the share menu.", "Gagal membuka menu bagikan.")
    val saveSuccessPrefix = tr("Saved:", "Tersimpan:")
    val saveFailedMessage = tr("Failed to save the QR.", "Gagal menyimpan QR.")
    val qrBitmap = remember(encryptedPayload, previewBitmapSize) {
        QrCodeGenerator.generateBitmap(encryptedPayload, size = previewBitmapSize)
    }
    DisposableEffect(qrBitmap) {
        onDispose {
            if (!qrBitmap.isRecycled) {
                qrBitmap.recycle()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutline, RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("Encrypted Exam QR", "QR Ujian Terenkripsi"),
            color = LockTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                "The CBX Lock app can read and decrypt this data when scanned.",
                "Aplikasi CBX Lock dapat membaca dan mendekripsi data ini saat dipindai."
            ),
            color = LockTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LockOutline, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = tr("Encrypted exam QR", "QR ujian terenkripsi"),
                modifier = Modifier.size(previewDisplaySize)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ExamDetailLine(label = tr("Exam Name", "Nama Ujian"), value = examName)
        ExamDetailLine(label = tr("Start", "Mulai"), value = startTime)
        ExamDetailLine(label = tr("End", "Selesai"), value = endTime)
        ExamDetailLine(
            label = tr("Geofence", "Geofence"),
            value = when (locationPolicy.shapeType) {
                GeofenceShapeType.Circle -> tr(
                    "Circle | ${locationPolicy.effectiveCircleCenters.size} centers | ${locationPolicy.radiusMeters} m",
                    "Lingkaran | ${locationPolicy.effectiveCircleCenters.size} center | ${locationPolicy.radiusMeters} m"
                )
                GeofenceShapeType.Polygon -> tr(
                    "Polygon | ${locationPolicy.vertices.size} points",
                    "Polygon | ${locationPolicy.vertices.size} titik"
                )
                GeofenceShapeType.Disabled -> tr("Disabled", "Nonaktif")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    runCatching {
                        val exportBitmap = ExamQrExportHelper.createShareBitmap(
                            encryptedPayload = encryptedPayload,
                            examName = examName,
                            startTime = startTime,
                            endTime = endTime,
                            locationPolicy = locationPolicy,
                            exportSpec = exportBitmapSpec
                        )
                        try {
                            ExamQrExportHelper.shareBitmap(
                                context = context,
                                bitmap = exportBitmap,
                                examName = examName
                            )
                        } finally {
                            if (!exportBitmap.isRecycled) {
                                exportBitmap.recycle()
                            }
                        }
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            shareFailedMessage,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(UiTokens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockBlue,
                    contentColor = LockOnDark
                )
            ) {
                Text(tr("Share", "Bagikan"), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    runCatching {
                        val exportBitmap = ExamQrExportHelper.createShareBitmap(
                            encryptedPayload = encryptedPayload,
                            examName = examName,
                            startTime = startTime,
                            endTime = endTime,
                            locationPolicy = locationPolicy,
                            exportSpec = exportBitmapSpec
                        )
                        try {
                            ExamQrExportHelper.saveToGallery(
                                context = context,
                                bitmap = exportBitmap,
                                examName = examName
                            )
                        } finally {
                            if (!exportBitmap.isRecycled) {
                                exportBitmap.recycle()
                            }
                        }
                    }.onSuccess { fileName ->
                        android.widget.Toast.makeText(
                            context,
                            "$saveSuccessPrefix $fileName",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            saveFailedMessage,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(UiTokens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockBackground,
                    contentColor = LockBlueDeep
                ),
                border = BorderStroke(1.dp, LockBlue.copy(alpha = 0.45f))
            ) {
                Text(tr("Download", "Download"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun ExamDetailLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = label,
            color = LockTextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = LockTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start
        )
    }
}

internal fun formatDateTime(calendar: Calendar): String {
    return formatExamScheduleDateTime(calendar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeDatePickerDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    PickerDialogTheme {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                    Text(tr("Next", "Lanjut"), color = LockBlueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = LockTextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    PickerDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = LockSurface,
            titleContentColor = LockOnDark,
            textContentColor = LockOnDark,
            title = {
                Text(
                    text = tr("Select Time", "Pilih Jam"),
                    color = LockOnDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }
                ) {
                    Text(tr("Save", "Simpan"), color = LockBlueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = LockTextMuted)
                }
            }
        )
    }
}

@Composable
internal fun PickerDialogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PickerDialogColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
internal fun ActionButton(
    text: String,
    subtitle: String? = null,
    badgeText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    iconContainerColor: Color = contentColor.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle.isNullOrBlank()) 72.dp else 90.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = 0.70f), RoundedCornerShape(UiTokens.RadiusLg))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .flatPill(containerColor = 
                            if (contentColor == LockOnDark) {
                                Color.White.copy(alpha = 0.14f)
                            } else {
                                LockBlueTint
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                lineHeight = 22.sp
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = if (contentColor == LockOnDark) {
                        Color.White.copy(alpha = 0.80f)
                    } else {
                        LockTextSecondary
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconContent != null -> iconContent()
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

