package com.coblax.examlock.ui.admin

import android.location.Location
import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit

import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.formatCoordinates
import com.coblax.examlock.GeofencePoint
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.model.DateTimeField
import com.coblax.examlock.parseGeofenceConfig
import com.coblax.examlock.parseStoredDateTime
import com.coblax.examlock.R
import com.coblax.examlock.ui.geofence.CircleGeofenceEditorScreen
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.geofence.PolygonGeofenceEditor
import com.coblax.examlock.ui.geofence.summarizeCircleVertexList
import com.coblax.examlock.ui.geofence.summarizePolygonVertexList
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.validateExamUrl
import com.coblax.examlock.viewmodel.CustomQrDraftState
import com.coblax.examlock.ui.theme.UiTokens
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

internal fun isCustomQrExamStepComplete(draft: CustomQrDraftState): Boolean {
    return draft.examName.isNotBlank() &&
        draft.startTime.isNotBlank() &&
        draft.endTime.isNotBlank() &&
        validateExamUrl(draft.examUrl).normalizedUrl != null
}

internal fun canOpenCustomQrStep(
    target: CustomQrAdminTab,
    draft: CustomQrDraftState,
    locationConfigurationValid: Boolean
): Boolean {
    return when (target) {
        CustomQrAdminTab.Exam -> true
        CustomQrAdminTab.Location -> isCustomQrExamStepComplete(draft)
        CustomQrAdminTab.Generate ->
            isCustomQrExamStepComplete(draft) &&
                (!draft.geofenceEnabled || locationConfigurationValid)
    }
}

@Composable
@Suppress("AssignedValueIsNeverRead")
internal fun CustomQrAdminScreen(
    showSaveToDirectLinkOption: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTabName: String = CustomQrAdminTab.Exam.name,
    onSelectedTabNameChange: (String) -> Unit = {},
    draft: CustomQrDraftState = CustomQrDraftState(),
    onDraftChange: (CustomQrDraftState) -> Unit = {},
    showCircleMapEditor: Boolean = false,
    onShowCircleMapEditorChange: (Boolean) -> Unit = {},
    showPolygonMapEditor: Boolean = false,
    onShowPolygonMapEditorChange: (Boolean) -> Unit = {},
    generatedQrPayload: String? = null,
    onGeneratedQrPayloadChange: (String?) -> Unit = {},
    generationStatus: String? = null,
    onGenerationStatusChange: (String?) -> Unit = {},
    generationIsError: Boolean = false,
    onGenerationIsErrorChange: (Boolean) -> Unit = {}
) {
    val missingFieldsMessage = tr(
        "Complete the URL, exam name, start time, and end time.",
        "Lengkapi URL, nama ujian, waktu mulai, dan waktu selesai."
    )
    val qrCreatedMessage = tr(
        "Encrypted QR created successfully. Scan this QR from the scan menu.",
        "QR terenkripsi berhasil dibuat. Pindai QR ini lewat menu scan."
    )
    val invalidGeofenceMessage = tr(
        "Geofence configuration is invalid. Latitude must be -90..90, longitude -180..180, and radius must be greater than 0.",
        "Konfigurasi geofence tidak valid. Latitude harus -90..90, longitude -180..180, dan radius harus lebih dari 0."
    )
    val invalidExamUrlMessage = tr(
        "Exam URL must start with https:// and include a domain.",
        "URL ujian harus diawali https:// dan memiliki domain."
    )
    var activePickerField by remember { mutableStateOf<DateTimeField?>(null) }
    var isTimePickerVisible by remember { mutableStateOf(false) }
    var draftDateTime by remember { mutableStateOf<Calendar?>(null) }
    var stepNavigationError by remember { mutableStateOf<String?>(null) }
    val examUrl = draft.examUrl
    val examName = draft.examName
    val startTime = draft.startTime
    val endTime = draft.endTime
    val geofenceEnabled = draft.geofenceEnabled
    val geofenceShapeTypeName = draft.geofenceShapeTypeName
    val geofenceCenterLat = draft.geofenceCenterLat
    val geofenceCenterLng = draft.geofenceCenterLng
    val geofenceRadiusMeters = draft.geofenceRadiusMeters
    val polygonVertices = draft.polygonVertices
    val geofenceCircleCenters = draft.geofenceCircleCenters
    val saveToDirectLink = draft.saveToDirectLink
    fun updateDraft(transform: (CustomQrDraftState) -> CustomQrDraftState) {
        onDraftChange(transform(draft))
    }
    val selectedGeofenceShapeType = runCatching {
        GeofenceShapeType.valueOf(geofenceShapeTypeName)
    }.getOrDefault(GeofenceShapeType.Circle)
    val selectedCustomQrAdminTab = runCatching {
        CustomQrAdminTab.valueOf(selectedTabName)
    }.getOrDefault(CustomQrAdminTab.Exam)
    val geofenceConfigResult = remember(
        geofenceEnabled,
        selectedGeofenceShapeType,
        geofenceCenterLat,
        geofenceCenterLng,
        geofenceRadiusMeters,
        polygonVertices,
        geofenceCircleCenters
    ) {
        parseGeofenceConfig(
            enabled = geofenceEnabled,
            centerLatRaw = geofenceCenterLat,
            centerLngRaw = geofenceCenterLng,
            radiusMetersRaw = geofenceRadiusMeters,
            shapeType = selectedGeofenceShapeType,
            polygonVertices = polygonVertices,
            circleCenters = geofenceCircleCenters
        )
    }
    val effectiveCircleCenters = if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
        geofenceCircleCenters
    } else {
        emptyList()
    }
    val effectiveCircleCenter = effectiveCircleCenters.firstOrNull()
    val currentLocationPolicy = ExamQrLocationPolicy(
        shapeType = when {
            !geofenceEnabled -> GeofenceShapeType.Disabled
            else -> selectedGeofenceShapeType
        },
        centerLat = if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
            effectiveCircleCenter?.latitude?.trim().orEmpty()
        } else {
            geofenceCenterLat.trim()
        },
        centerLng = if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
            effectiveCircleCenter?.longitude?.trim().orEmpty()
        } else {
            geofenceCenterLng.trim()
        },
        radiusMeters = geofenceRadiusMeters.trim(),
        vertices = if (selectedGeofenceShapeType == GeofenceShapeType.Polygon) {
            polygonVertices
        } else {
            emptyList()
        },
        circleCenters = effectiveCircleCenters
    )
    val clearGeneratedQr = {
        onGeneratedQrPayloadChange(null)
        onGenerationStatusChange(null)
        onGenerationIsErrorChange(false)
    }

    fun navigateToStep(target: CustomQrAdminTab) {
        val canMoveBack = target.ordinal <= selectedCustomQrAdminTab.ordinal
        val locationValid = !geofenceEnabled || geofenceConfigResult.config != null
        if (canMoveBack || canOpenCustomQrStep(target, draft, locationValid)) {
            stepNavigationError = null
            onSelectedTabNameChange(target.name)
            return
        }

        stepNavigationError = when {
            !isCustomQrExamStepComplete(draft) -> {
                if (
                    examUrl.isBlank() ||
                    examName.isBlank() ||
                    startTime.isBlank() ||
                    endTime.isBlank()
                ) {
                    missingFieldsMessage
                } else {
                    invalidExamUrlMessage
                }
            }
            else -> invalidGeofenceMessage
        }
    }

    fun generateQr() {
        when {
            examUrl.isBlank() ||
                examName.isBlank() ||
                startTime.isBlank() ||
                endTime.isBlank() -> {
                onGenerationStatusChange(missingFieldsMessage)
                onGenerationIsErrorChange(true)
                onGeneratedQrPayloadChange(null)
            }

            geofenceEnabled && geofenceConfigResult.config == null -> {
                onGenerationStatusChange(invalidGeofenceMessage)
                onGenerationIsErrorChange(true)
                onGeneratedQrPayloadChange(null)
            }

            else -> {
                val examUrlValidation = validateExamUrl(examUrl)
                val normalizedExamUrl = examUrlValidation.normalizedUrl
                if (normalizedExamUrl == null) {
                    onGenerationStatusChange(invalidExamUrlMessage)
                    onGenerationIsErrorChange(true)
                    onGeneratedQrPayloadChange(null)
                } else {
                    val payload = ExamQrPayload(
                        examUrl = normalizedExamUrl,
                        examName = examName.trim(),
                        startDateTime = startTime,
                        endDateTime = endTime,
                        saveToDirectLink = showSaveToDirectLinkOption && saveToDirectLink,
                        locationPolicy = currentLocationPolicy,
                        locationPolicySource = LocationPolicySource.CustomQr
                    )
                    onGeneratedQrPayloadChange(ExamQrCodec.encrypt(payload))
                    onGenerationStatusChange(qrCreatedMessage)
                    onGenerationIsErrorChange(false)
                }
            }
        }
    }

    if (showCircleMapEditor) {
        CircleGeofenceEditorScreen(
            initialCenters = geofenceCircleCenters,
            initialRadiusMeters = geofenceRadiusMeters,
            onDismiss = { onShowCircleMapEditorChange(false) },
            onSave = { centers, radiusMeters ->
                updateDraft {
                    it.copy(
                        geofenceCircleCenters = centers,
                        geofenceCenterLat = centers.firstOrNull()?.latitude.orEmpty(),
                        geofenceCenterLng = centers.firstOrNull()?.longitude.orEmpty(),
                        geofenceRadiusMeters = radiusMeters
                    )
                }
                clearGeneratedQr()
                onShowCircleMapEditorChange(false)
            }
        )
        return
    }

    if (showPolygonMapEditor) {
        PolygonGeofenceEditor(
            initialVertices = polygonVertices,
            onDismiss = { onShowPolygonMapEditorChange(false) },
            onSave = { vertices ->
                updateDraft { it.copy(polygonVertices = vertices) }
                clearGeneratedQr()
                onShowPolygonMapEditorChange(false)
            }
        )
        return
    }

    UpgradeUiScope {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.current.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackPillButton(onClick = onBack)

            Surface(
                shape = RoundedCornerShape(UiTokens.RadiusPill),
                color = AppColors.current.blueFill
            ) {
                Text(
                    text = "CUSTOM QR",
                    color = AppColors.current.brandText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tr("Create Exam QR", "Buat QR Ujian"),
            color = AppColors.current.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tr(
                "Fill exam data, set location, then generate.",
                "Isi data ujian, atur lokasi, lalu generate."
            ),
            color = AppColors.current.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        CustomQrAdminTabSelector(
            selectedTab = selectedCustomQrAdminTab,
            onTabSelected = { target -> navigateToStep(target) }
        )

        stepNavigationError?.let { message ->
            Spacer(modifier = Modifier.height(10.dp))
            StatusBanner(
                message = message,
                isError = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedCustomQrAdminTab) {
                CustomQrAdminTab.Exam -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, AppColors.current.outlineStrong, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                            Text(
                                text = tr("Exam Data", "Data Ujian"),
                                color = AppColors.current.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            AdminInputField(
                                value = examUrl,
                                onValueChange = {
                                    updateDraft { current -> current.copy(examUrl = it) }
                                    clearGeneratedQr()
                                },
                                placeholder = tr("Exam URL (Required)", "URL Ujian (Wajib)"),
                                keyboardType = KeyboardType.Uri
                            )
                            AdminInputField(
                                value = examName,
                                onValueChange = {
                                    updateDraft { current -> current.copy(examName = it) }
                                    clearGeneratedQr()
                                },
                                placeholder = tr("Exam Name (Required)", "Nama Ujian (Wajib)")
                            )
                            AdminPickerField(
                                value = startTime,
                                placeholder = tr("Exam Date & Time", "Tanggal & Waktu Ujian"),
                                isActive = activePickerField == DateTimeField.Start,
                                onClick = {
                                    clearGeneratedQr()
                                    activePickerField = DateTimeField.Start
                                    draftDateTime = parseStoredDateTime(startTime)
                                    isTimePickerVisible = false
                                }
                            )
                            AdminPickerField(
                                value = endTime,
                                placeholder = tr("End Date & Time", "Tanggal & Waktu Selesai"),
                                isActive = activePickerField == DateTimeField.End,
                                onClick = {
                                    clearGeneratedQr()
                                    activePickerField = DateTimeField.End
                                    draftDateTime = parseStoredDateTime(endTime)
                                    isTimePickerVisible = false
                                }
                            )
                        }
                }

                CustomQrAdminTab.Location -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, AppColors.current.outlineStrong, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("Location / Geofence", "Lokasi / Geofence"),
                                    color = AppColors.current.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (geofenceEnabled) {
                                    val locationReady = geofenceConfigResult.config != null
                                    Icon(
                                        imageVector = if (locationReady) {
                                            Icons.Rounded.CheckCircle
                                        } else {
                                            Icons.Rounded.Warning
                                        },
                                        contentDescription = if (locationReady) {
                                            tr(
                                                "Location configuration valid",
                                                "Konfigurasi lokasi valid"
                                            )
                                        } else {
                                            tr(
                                                "Location configuration needs attention",
                                                "Konfigurasi lokasi perlu diperiksa"
                                            )
                                        },
                                        tint = if (locationReady) {
                                            AppColors.current.statusSafe
                                        } else {
                                            AppColors.current.statusWarn
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            AdminToggleRow(
                                title = tr("Enable Strict Geofence", "Aktifkan Geofence Ketat"),
                                description = tr(
                                    "Store this exam's allowed location inside the QR.",
                                    "Simpan lokasi yang diizinkan untuk ujian ini di dalam QR."
                                ),
                                checked = geofenceEnabled,
                                onCheckedChange = {
                                    updateDraft { current -> current.copy(geofenceEnabled = it) }
                                    clearGeneratedQr()
                                }
                            )
                            if (geofenceEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            updateDraft { current ->
                                                current.copy(geofenceShapeTypeName = GeofenceShapeType.Circle.name)
                                            }
                                            clearGeneratedQr()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
                                                AppColors.current.blue
                                            } else {
                                                AppColors.current.surfaceSoft
                                            },
                                            contentColor = if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
                                                AppColors.current.onDark
                                            } else {
                                                AppColors.current.textPrimary
                                            }
                                        )
                                    ) {
                                        Text(tr("Circle", "Lingkaran"), fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            updateDraft { current ->
                                                current.copy(geofenceShapeTypeName = GeofenceShapeType.Polygon.name)
                                            }
                                            clearGeneratedQr()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedGeofenceShapeType == GeofenceShapeType.Polygon) {
                                                AppColors.current.blue
                                            } else {
                                                AppColors.current.surfaceSoft
                                            },
                                            contentColor = if (selectedGeofenceShapeType == GeofenceShapeType.Polygon) {
                                                AppColors.current.onDark
                                            } else {
                                                AppColors.current.textPrimary
                                            }
                                        )
                                    ) {
                                        Text(tr("Polygon", "Polygon"), fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (selectedGeofenceShapeType == GeofenceShapeType.Circle) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onShowCircleMapEditorChange(true) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.current.blue,
                                                contentColor = AppColors.current.onDark
                                            )
                                        ) {
                                            Text(
                                                tr(
                                                    "Open Map (${geofenceCircleCenters.size}/5)",
                                                    "Buka Map (${geofenceCircleCenters.size}/5)"
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (geofenceCircleCenters.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    updateDraft { current ->
                                                        current.copy(
                                                            geofenceCircleCenters = emptyList(),
                                                            geofenceCenterLat = "",
                                                            geofenceCenterLng = ""
                                                        )
                                                    }
                                                    clearGeneratedQr()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AppColors.current.dangerBgSubtle,
                                                    contentColor = AppColors.current.dialogDangerIcon
                                                ),
                                                border = BorderStroke(1.dp, AppColors.current.dialogDangerIcon.copy(alpha = 0.3f))
                                            ) {
                                                Text(tr("Clear", "Hapus"), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    val radiusValue = geofenceRadiusMeters.toFloatOrNull() ?: 100f
                                    Text(
                                        text = tr(
                                            "Radius: ${geofenceRadiusMeters.ifBlank { "-" }} m",
                                            "Radius: ${geofenceRadiusMeters.ifBlank { "-" }} m"
                                        ),
                                        color = AppColors.current.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    androidx.compose.material3.Slider(
                                        value = radiusValue.coerceIn(50f, 5000f),
                                        onValueChange = { value ->
                                            updateDraft { current ->
                                                current.copy(geofenceRadiusMeters = value.toInt().toString())
                                            }
                                            clearGeneratedQr()
                                        },
                                        valueRange = 50f..5000f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(UiTokens.RadiusSm),
                                        color = AppColors.current.surfaceSoft,
                                        border = BorderStroke(1.dp, AppColors.current.outlineMedium)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tr(
                                                    "Circle centers: ${geofenceCircleCenters.size}/5",
                                                    "Titik center circle: ${geofenceCircleCenters.size}/5"
                                                ),
                                                color = AppColors.current.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr(
                                                    "Shared radius: ${geofenceRadiusMeters.ifBlank { "-" }} m",
                                                    "Radius bersama: ${geofenceRadiusMeters.ifBlank { "-" }} m"
                                                ),
                                                color = AppColors.current.textSecondary,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = tr(
                                                    "Primary center: ${
                                                        geofenceCircleCenters.firstOrNull()?.let { center ->
                                                            "${center.latitude}, ${center.longitude}"
                                                        } ?: "-"
                                                    }",
                                                    "Center utama: ${
                                                        geofenceCircleCenters.firstOrNull()?.let { center ->
                                                            "${center.latitude}, ${center.longitude}"
                                                        } ?: "-"
                                                    }"
                                                ),
                                                color = AppColors.current.textSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                            if (geofenceCircleCenters.size > 1) {
                                                Text(
                                                    text = tr(
                                                        "Centers preview: ${summarizeCircleVertexList(geofenceCircleCenters)}",
                                                        "Preview center: ${summarizeCircleVertexList(geofenceCircleCenters)}"
                                                    ),
                                                    color = AppColors.current.textSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = tr(
                                            "Use the full map editor to place up to 5 center points with one shared radius.",
                                            "Gunakan editor map penuh untuk menaruh sampai 5 titik center dengan satu radius bersama."
                                        ),
                                        color = AppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onShowPolygonMapEditorChange(true) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.current.blue,
                                                contentColor = AppColors.current.onDark
                                            )
                                        ) {
                                            Text(
                                                tr(
                                                    "Open Map (${polygonVertices.size}/50)",
                                                    "Buka Map (${polygonVertices.size}/50)"
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (polygonVertices.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    updateDraft { current ->
                                                        current.copy(polygonVertices = emptyList())
                                                    }
                                                    clearGeneratedQr()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AppColors.current.dangerBgSubtle,
                                                    contentColor = AppColors.current.dialogDangerIcon
                                                ),
                                                border = BorderStroke(1.dp, AppColors.current.dialogDangerIcon.copy(alpha = 0.3f))
                                            ) {
                                                Text(tr("Clear", "Hapus"), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(UiTokens.RadiusSm),
                                        color = AppColors.current.surfaceSoft,
                                        border = BorderStroke(1.dp, AppColors.current.outlineMedium)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tr(
                                                    "Polygon points: ${polygonVertices.size}/50",
                                                    "Titik polygon: ${polygonVertices.size}/50"
                                                ),
                                                color = AppColors.current.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr(
                                                    "Last point: ${
                                                        polygonVertices.lastOrNull()?.let { vertex ->
                                                            "${vertex.latitude}, ${vertex.longitude}"
                                                        } ?: "-"
                                                    }",
                                                    "Titik terakhir: ${
                                                        polygonVertices.lastOrNull()?.let { vertex ->
                                                            "${vertex.latitude}, ${vertex.longitude}"
                                                        } ?: "-"
                                                    }"
                                                ),
                                                color = AppColors.current.textSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                            if (polygonVertices.isNotEmpty()) {
                                                Text(
                                                    text = tr(
                                                        "Preview: ${summarizePolygonVertexList(polygonVertices)}",
                                                        "Preview: ${summarizePolygonVertexList(polygonVertices)}"
                                                    ),
                                                    color = AppColors.current.textSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = tr(
                                            "Use the full map editor to add up to 50 polygon boundary points.",
                                            "Gunakan editor map penuh untuk menambah sampai 50 titik batas polygon."
                                        ),
                                        color = AppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            val geofenceSummary = when {
                                !geofenceEnabled -> tr(
                                    "Geofence will be disabled for this QR.",
                                    "Geofence akan nonaktif untuk QR ini."
                                )
                                geofenceConfigResult.config != null &&
                                    geofenceConfigResult.config.shapeType == GeofenceShapeType.Polygon -> tr(
                                    "QR polygon area with ${geofenceConfigResult.config.vertices.size} points.",
                                    "Area polygon QR dengan ${geofenceConfigResult.config.vertices.size} titik."
                                )
                                geofenceConfigResult.config != null -> {
                                    val centers = geofenceConfigResult.config.circleCenters.ifEmpty {
                                        listOf(
                                            GeofencePoint(
                                                latitude = geofenceConfigResult.config.centerLat,
                                                longitude = geofenceConfigResult.config.centerLng
                                            )
                                        )
                                    }
                                    tr(
                                        "QR circle area with ${centers.size} centers | radius ${
                                            String.format(Locale.US, "%.1f m", geofenceConfigResult.config.radiusMeters)
                                        } | primary ${
                                            formatCoordinates(centers.first().latitude, centers.first().longitude)
                                        }",
                                        "Area circle QR dengan ${centers.size} center | radius ${
                                            String.format(Locale.US, "%.1f m", geofenceConfigResult.config.radiusMeters)
                                        } | utama ${
                                            formatCoordinates(centers.first().latitude, centers.first().longitude)
                                        }"
                                    )
                                }
                                else -> invalidGeofenceMessage
                            }
                            Text(
                                text = geofenceSummary,
                                color = if (geofenceEnabled && geofenceConfigResult.config == null) {
                                    AppColors.current.dialogDangerIcon
                                } else {
                                    AppColors.current.textSecondary
                                },
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            if (geofenceEnabled && geofenceConfigResult.error != null) {
                                val validationMsg = when (geofenceConfigResult.error) {
                                    "invalid_latitude" -> tr("Latitude must be between -90 and 90.", "Latitude harus antara -90 dan 90.")
                                    "invalid_longitude" -> tr("Longitude must be between -180 and 180.", "Longitude harus antara -180 dan 180.")
                                    "invalid_radius" -> tr("Radius must be greater than 0.", "Radius harus lebih dari 0.")
                                    "polygon_min_3_vertices" -> tr("Polygon requires at least 3 points.", "Polygon membutuhkan minimal 3 titik.")
                                    "polygon_degenerate" -> tr("Polygon area is too small or degenerate.", "Area polygon terlalu kecil atau degenerate.")
                                    "polygon_self_intersecting" -> tr("Polygon lines must not cross each other.", "Garis polygon tidak boleh saling bersilangan.")
                                    else -> tr("Configuration error: ${geofenceConfigResult.error}", "Error konfigurasi: ${geofenceConfigResult.error}")
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppColors.current.dangerBgSubtle,
                                    border = BorderStroke(1.dp, AppColors.current.dialogDangerIcon.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = validationMsg,
                                        color = AppColors.current.dialogDangerIcon,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                CustomQrAdminTab.Generate -> {
                    if (showSaveToDirectLinkOption) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(UiTokens.RadiusMd),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, AppColors.current.outlineStrong)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            "Save to Direct Link after scan",
                                            "Setelah scan, simpan juga sebagai Direct Link"
                                        ),
                                        color = AppColors.current.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tr(
                                            "When this QR is scanned, it will update the Direct Link config.",
                                            "Saat QR ini dipindai, konfigurasi Direct Link akan diperbarui."
                                        ),
                                        color = AppColors.current.textSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                Checkbox(
                                    checked = saveToDirectLink,
                                    onCheckedChange = {
                                        updateDraft { current -> current.copy(saveToDirectLink = it) }
                                        clearGeneratedQr()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AppColors.current.blue,
                                        uncheckedColor = AppColors.current.outlineStrong,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    generationStatus?.let { status ->
                        StatusBanner(
                            message = status,
                            isError = generationIsError
                        )
                    }

                    generatedQrPayload?.let { qrPayload ->
                        GeneratedQrCard(
                            encryptedPayload = qrPayload,
                            examName = examName,
                            startTime = startTime,
                            endTime = endTime,
                            locationPolicy = currentLocationPolicy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(UiTokens.RadiusMd),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AppColors.current.outlineStrong)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCustomQrAdminTab != CustomQrAdminTab.Exam) {
                    Button(
                        onClick = {
                            navigateToStep(
                                CustomQrAdminTab.entries[
                                    selectedCustomQrAdminTab.ordinal - 1
                                ]
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(UiTokens.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.current.surfaceSoft,
                            contentColor = AppColors.current.textPrimary
                        ),
                        border = BorderStroke(1.dp, AppColors.current.outlineStrong)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Previous", "Kembali"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (selectedCustomQrAdminTab == CustomQrAdminTab.Generate) {
                            generateQr()
                        } else {
                            navigateToStep(
                                CustomQrAdminTab.entries[
                                    selectedCustomQrAdminTab.ordinal + 1
                                ]
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(UiTokens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.current.blue,
                        contentColor = AppColors.current.onDark
                    )
                ) {
                    Text(
                        text = if (selectedCustomQrAdminTab == CustomQrAdminTab.Generate) {
                            tr("Generate QR", "Generate QR")
                        } else {
                            tr("Next", "Lanjut")
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (
                            selectedCustomQrAdminTab == CustomQrAdminTab.Generate
                        ) {
                            Icons.Rounded.QrCodeScanner
                        } else {
                            Icons.AutoMirrored.Rounded.ArrowForward
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    val currentDraft = draftDateTime
    if (activePickerField != null && currentDraft != null && !isTimePickerVisible) {
        ComposeDatePickerDialog(
            initialDateMillis = currentDraft.timeInMillis,
            onDismiss = {
                activePickerField = null
                draftDateTime = null
            },
            onConfirm = { selectedDateMillis ->
                val updatedCalendar = (currentDraft.clone() as Calendar)
                val selectedCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis ?: currentDraft.timeInMillis
                }
                updatedCalendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                updatedCalendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                updatedCalendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                draftDateTime = updatedCalendar
                isTimePickerVisible = true
            }
        )
    }

    if (activePickerField != null && currentDraft != null && isTimePickerVisible) {
        ComposeTimePickerDialog(
            initialHour = currentDraft.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentDraft.get(Calendar.MINUTE),
            onDismiss = {
                activePickerField = null
                draftDateTime = null
                isTimePickerVisible = false
            },
            onConfirm = { hour, minute ->
                val completedCalendar = (currentDraft.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val formattedValue = formatDateTime(completedCalendar)

                when (activePickerField) {
                    DateTimeField.Start -> updateDraft { current ->
                        current.copy(startTime = formattedValue)
                    }
                    DateTimeField.End -> updateDraft { current ->
                        current.copy(endTime = formattedValue)
                    }
                    null -> Unit
                }

                activePickerField = null
                draftDateTime = null
                isTimePickerVisible = false
            }
        )
    }
    }
}
