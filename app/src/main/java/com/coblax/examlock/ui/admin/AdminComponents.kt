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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
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
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.QrExportBitmapSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
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
import com.coblax.examlock.ExamQrSecurityBypass
import com.coblax.examlock.formatExamScheduleDateTime
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.R
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
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
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
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
                .background(AppColors.current.blue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = tr("Main menu", "Menu utama"),
                tint = AppColors.current.onDark,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = "MENU",
            color = AppColors.current.textPrimary,
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
            color = AppColors.current.textPrimary,
            fontSize = 16.sp
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = AppColors.current.textMuted,
                fontSize = 16.sp
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.current.surfaceSoft,
            unfocusedContainerColor = AppColors.current.surfaceSoft,
            focusedBorderColor = AppColors.current.blue,
            unfocusedBorderColor = AppColors.current.outline,
            focusedTextColor = AppColors.current.textPrimary,
            unfocusedTextColor = AppColors.current.textPrimary,
            cursorColor = AppColors.current.blue,
            focusedPlaceholderColor = AppColors.current.textMuted,
            unfocusedPlaceholderColor = AppColors.current.textMuted
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
            .background(AppColors.current.surfaceSoft)
            .border(
                width = 1.dp,
                color = if (isActive) AppColors.current.blue else AppColors.current.outline,
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
            color = if (isBlank) AppColors.current.textMuted else AppColors.current.textPrimary,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = tr("Pick", "Pilih"),
            color = AppColors.current.blue,
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
            .border(1.dp, AppColors.current.outlineMedium, RoundedCornerShape(UiTokens.RadiusSm))
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
                color = AppColors.current.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = AppColors.current.textSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.current.blue,
                uncheckedThumbColor = AppColors.current.blue.copy(alpha = 0.6f),
                uncheckedTrackColor = AppColors.current.outlineMedium
            )
        )
    }
}


@Composable
internal fun StatusBanner(
    message: String,
    isError: Boolean
) {
    val colors = AppColors.current
    val backgroundColor = when {
        isError -> colors.dangerBgSoft
        else -> colors.blueTint
    }
    val borderColor = when {
        isError -> colors.statusDanger.copy(alpha = if (colors.isDark) 0.40f else 0.35f)
        else -> colors.blue.copy(alpha = if (colors.isDark) 0.40f else 0.30f)
    }
    val textColor = if (isError) colors.issueText else colors.brandText

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

private sealed interface QrPreview {
    data object Loading : QrPreview
    data object TooLarge : QrPreview
    data class Ready(val bitmap: Bitmap, val exportSpec: QrExportBitmapSpec) : QrPreview
}

@Composable
internal fun GeneratedQrCard(
    encryptedPayload: String,
    examName: String,
    startTime: String,
    endTime: String,
    locationPolicy: ExamQrLocationPolicy,
    securityBypasses: Set<ExamQrSecurityBypass> = emptySet(),
    minAppVersionName: String? = null
) {
    val uiLanguage = LocalUiLanguage.current
    val bypassTitles = remember(securityBypasses, uiLanguage) {
        examQrBypassTitles(uiLanguage, securityBypasses)
    }
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val coroutineScope = rememberCoroutineScope()
    val shareFailedMessage = tr("Failed to open the share menu.", "Gagal membuka menu bagikan.")
    val saveSuccessPrefix = tr("Saved to Pictures/COBLAX EXAM LOCK:", "Tersimpan di Pictures/COBLAX EXAM LOCK:")
    val saveFailedMessage = tr("Failed to save the QR.", "Gagal menyimpan QR.")
    val savedToDocumentMessage = tr("QR saved.", "QR tersimpan.")
    var preview by remember(encryptedPayload) { mutableStateOf<QrPreview>(QrPreview.Loading) }
    var exportBusy by remember { mutableStateOf(false) }

    // Picking a mask the scanner reads takes several trial decodes; on a slow phone that
    // froze the screen when it ran during composition.
    LaunchedEffect(encryptedPayload, lowRamProfile) {
        preview = withContext(Dispatchers.Default) { buildQrPreview(encryptedPayload, lowRamProfile) }
    }
    val ready = preview as? QrPreview.Ready
    DisposableEffect(ready) {
        onDispose {
            ready?.bitmap?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        }
    }
    if (preview == QrPreview.TooLarge) {
        StatusBanner(
            message = tr(
                "This QR holds too much to show as one code. Use fewer location points or a shorter link, then generate again.",
                "Isi QR ini terlalu banyak untuk satu kode. Kurangi titik lokasi atau persingkat link, lalu generate lagi."
            ),
            isError = true
        )
        return
    }

    fun renderPoster(spec: QrExportBitmapSpec): Bitmap = ExamQrExportHelper.createShareBitmap(
        encryptedPayload = encryptedPayload,
        examName = examName,
        startTime = startTime,
        endTime = endTime,
        locationPolicy = locationPolicy,
        exportSpec = spec
    )

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val spec = ready?.exportSpec
        if (uri == null || spec == null) {
            return@rememberLauncherForActivityResult
        }
        exportBusy = true
        coroutineScope.launch {
            val outcome = withContext(Dispatchers.Default) {
                runCatching {
                    val poster = renderPoster(spec)
                    try {
                        ExamQrExportHelper.writePng(context, poster, uri)
                    } finally {
                        if (!poster.isRecycled) poster.recycle()
                    }
                }
            }
            exportBusy = false
            Toast.makeText(
                context,
                if (outcome.isSuccess) savedToDocumentMessage else saveFailedMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun export(share: Boolean) {
        val spec = ready?.exportSpec ?: return
        if (exportBusy) {
            return
        }
        if (!share && !ExamQrExportHelper.canSaveToGalleryDirectly) {
            runCatching { saveDocumentLauncher.launch(ExamQrExportHelper.suggestedFileName(examName)) }
                .onFailure { Toast.makeText(context, saveFailedMessage, Toast.LENGTH_SHORT).show() }
            return
        }
        exportBusy = true
        coroutineScope.launch {
            // Drawing and compressing the poster takes a few hundred milliseconds; keep it
            // off the main thread and only hand the result to the share sheet there.
            val outcome = withContext(Dispatchers.Default) {
                runCatching {
                    val exportBitmap = renderPoster(spec)
                    try {
                        if (share) {
                            ExamQrExportHelper.writeShareFile(context, exportBitmap, examName).toString()
                        } else {
                            ExamQrExportHelper.saveToGallery(context, exportBitmap, examName)
                        }
                    } finally {
                        if (!exportBitmap.isRecycled) {
                            exportBitmap.recycle()
                        }
                    }
                }
            }
            exportBusy = false
            outcome.onSuccess { result ->
                if (share) {
                    runCatching {
                        ExamQrExportHelper.launchShare(context, Uri.parse(result), examName)
                    }.onFailure {
                        Toast.makeText(context, shareFailedMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "$saveSuccessPrefix $result", Toast.LENGTH_LONG).show()
                }
            }.onFailure {
                Toast.makeText(context, if (share) shareFailedMessage else saveFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outline, RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("Encrypted Exam QR", "QR Ujian Terenkripsi"),
            color = AppColors.current.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                "The CBX Lock app can read and decrypt this data when scanned.",
                "Aplikasi CBX Lock dapat membaca dan mendekripsi data ini saat dipindai."
            ),
            color = AppColors.current.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 340.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, AppColors.current.outline, RoundedCornerShape(18.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ready != null) {
                Image(
                    bitmap = ready.bitmap.asImageBitmap(),
                    contentDescription = tr("Encrypted exam QR", "QR ujian terenkripsi"),
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = AppColors.current.blue)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        ExamDetailLine(label = tr("Exam Name", "Nama Ujian"), value = examName)
        ExamDetailLine(label = tr("Start", "Mulai"), value = startTime)
        ExamDetailLine(label = tr("End", "Selesai"), value = endTime)
        ExamDetailLine(
            label = tr("Location", "Lokasi"),
            value = when (locationPolicy.shapeType) {
                GeofenceShapeType.Circle -> tr(
                    "Circle · ${locationPolicy.effectiveCircleCenters.size} ${if (locationPolicy.effectiveCircleCenters.size == 1) "center" else "centers"} · radius ${locationPolicy.radiusMeters} m",
                    "Lingkaran · ${locationPolicy.effectiveCircleCenters.size} titik pusat · radius ${locationPolicy.radiusMeters} m"
                )
                GeofenceShapeType.Polygon -> tr(
                    "Polygon · ${locationPolicy.vertices.size} corners",
                    "Polygon · ${locationPolicy.vertices.size} sudut"
                )
                GeofenceShapeType.Disabled -> tr("Anywhere (no location check)", "Bebas (tanpa cek lokasi)")
            }
        )
        if (bypassTitles.isNotEmpty()) {
            ExamDetailLine(
                label = tr("Checks turned off", "Pengamanan dilonggarkan"),
                value = bypassTitles.joinToString()
            )
        }
        if (!minAppVersionName.isNullOrBlank()) {
            ExamDetailLine(
                label = tr("Minimum CBX Lock", "CBX Lock minimal"),
                value = "v$minAppVersionName"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        val actionsEnabled = ready != null && !exportBusy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { export(share = true) },
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(UiTokens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.current.blue,
                    contentColor = AppColors.current.onDark
                )
            ) {
                Text(tr("Share", "Bagikan"), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { export(share = false) },
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(UiTokens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.current.background,
                    contentColor = AppColors.current.brandText
                ),
                border = BorderStroke(1.dp, AppColors.current.blue.copy(alpha = 0.45f))
            ) {
                Text(tr("Save", "Simpan"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The preview bitmap and the export size for [encryptedPayload]; runs off the main thread. */
private fun buildQrPreview(encryptedPayload: String, lowRamProfile: LowRamProfile): QrPreview {
    val qrModules = QrCodeGenerator.moduleCount(encryptedPayload)
    if (qrModules == 0) {
        return QrPreview.TooLarge
    }
    // Students often scan this straight off the admin's screen, so a dense QR gets
    // enough pixels (and screen width) for a phone camera to resolve each module.
    val previewBitmapSize = when {
        lowRamProfile.severe -> 384
        lowRamProfile.enabled -> 512
        else -> 640
    }.coerceAtLeast(qrModules * 4).coerceAtMost(1024)
    val bitmap = runCatching { QrCodeGenerator.generateBitmap(encryptedPayload, size = previewBitmapSize) }.getOrNull()
        ?: return QrPreview.TooLarge
    return QrPreview.Ready(bitmap, calculateQrExportBitmapSpec(lowRamProfile, qrModules))
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
            color = AppColors.current.textSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = AppColors.current.textPrimary,
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
                    Text(tr("Next", "Lanjut"), color = AppColors.current.blueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = AppColors.current.textMuted)
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
            containerColor = AppColors.current.surface,
            titleContentColor = AppColors.current.onDark,
            textContentColor = AppColors.current.onDark,
            title = {
                Text(
                    text = tr("Select Time", "Pilih Jam"),
                    color = AppColors.current.onDark,
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
                    Text(tr("Save", "Simpan"), color = AppColors.current.blueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = AppColors.current.textMuted)
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
            .heightIn(min = if (subtitle.isNullOrBlank()) 64.dp else 80.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = 0.70f), RoundedCornerShape(UiTokens.RadiusLg))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .flatPill(containerColor = 
                            if (contentColor == AppColors.current.onDark) {
                                Color.White.copy(alpha = 0.14f)
                            } else {
                                AppColors.current.blueTint
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
                    color = if (contentColor == AppColors.current.onDark) {
                        Color.White.copy(alpha = 0.80f)
                    } else {
                        AppColors.current.textSecondary
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

