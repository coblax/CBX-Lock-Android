package com.coblax.examlock.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.ExamQrCodec
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.QrCodeGenerator
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.model.DateTimeField
import com.coblax.examlock.parseStoredDateTime
import com.coblax.examlock.ui.geofence.GeofenceAreaEditorScreen
import com.coblax.examlock.ui.geofence.geofenceIssueMessage
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.validateExamUrl
import com.coblax.examlock.viewmodel.CustomQrDraftState
import java.util.Calendar
import java.util.TimeZone

// Narrower than the Material default so "Kembali" with its arrow fits on a small phone at a
// large font size instead of breaking mid-word.
private val StepButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** The first CBX Lock that reads a QR asking for a minimum version (payload v9). */
private const val MinVersionQrFirstReader = "3.3.4"

private const val DefaultExamDurationMillis = 2 * 60 * 60 * 1000L

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
    onGenerationIsErrorChange: (Boolean) -> Unit = {},
    draftResumed: Boolean = false,
    onStartNewDraft: () -> Unit = {}
) {
    val uiLanguage = LocalUiLanguage.current
    val missingFieldsMessage = tr(
        "Complete the URL, exam name, start time, and end time.",
        "Lengkapi URL, nama ujian, waktu mulai, dan waktu selesai."
    )
    val qrCreatedMessage = tr(
        "Encrypted QR created successfully. Scan this QR from the scan menu.",
        "QR terenkripsi berhasil dibuat. Pindai QR ini lewat menu scan."
    )
    val invalidExamUrlMessage = tr(
        "Exam URL must start with http:// or https:// and include a domain.",
        "URL ujian harus diawali http:// atau https:// dan memiliki domain."
    )
    val endNotAfterStartMessage = tr(
        "The end time must be after the start time.",
        "Waktu selesai harus setelah waktu mulai."
    )
    val alreadyEndedMessage = tr(
        "The end time has already passed, so no student could use this QR. Pick a later end time.",
        "Waktu selesai sudah lewat, jadi QR ini tidak bisa dipakai siswa. Pilih waktu selesai yang lebih akhir."
    )
    val invalidUpdateUrlMessage = tr(
        "The installer link must be an https:// address.",
        "Link installer harus berupa alamat https://."
    )
    val qrTooLargeMessage = tr(
        "This QR holds too much to print as one code. Use fewer location points or a shorter URL or installer link.",
        "Isi QR ini terlalu banyak untuk satu kode. Kurangi titik lokasi atau persingkat URL / link installer."
    )
    var activePickerField by remember { mutableStateOf<DateTimeField?>(null) }
    var isTimePickerVisible by remember { mutableStateOf(false) }
    var draftDateTime by remember { mutableStateOf<Calendar?>(null) }
    var stepNavigationError by remember { mutableStateOf<String?>(null) }
    val selectedCustomQrAdminTab = runCatching {
        CustomQrAdminTab.valueOf(selectedTabName)
    }.getOrDefault(CustomQrAdminTab.Exam)
    val locationIssue = draft.locationIssue()
    val scheduleProblem = customQrScheduleProblem(draft.startTime, draft.endTime)

    fun updateDraft(transform: (CustomQrDraftState) -> CustomQrDraftState) {
        onDraftChange(transform(draft))
    }

    val clearGeneratedQr = {
        onGeneratedQrPayloadChange(null)
        onGenerationStatusChange(null)
        onGenerationIsErrorChange(false)
    }

    fun examStepMessage(): String? = when {
        draft.examUrl.isBlank() || draft.examName.isBlank() ||
            draft.startTime.isBlank() || draft.endTime.isBlank() -> missingFieldsMessage
        normalizedCustomQrExamUrl(draft) == null -> invalidExamUrlMessage
        scheduleProblem == CustomQrScheduleProblem.EndNotAfterStart -> endNotAfterStartMessage
        scheduleProblem != null -> missingFieldsMessage
        else -> null
    }

    fun navigateToStep(target: CustomQrAdminTab) {
        val canMoveBack = target.ordinal <= selectedCustomQrAdminTab.ordinal
        if (canMoveBack || canOpenCustomQrStep(target, draft, locationIssue == null)) {
            stepNavigationError = null
            onSelectedTabNameChange(target.name)
            return
        }
        stepNavigationError = examStepMessage()
            ?: locationIssue?.let { geofenceIssueMessage(uiLanguage, draft.locationShape, it) }
    }

    fun failGeneration(message: String) {
        onGenerationStatusChange(message)
        onGenerationIsErrorChange(true)
        onGeneratedQrPayloadChange(null)
    }

    fun generateQr() {
        examStepMessage()?.let { return failGeneration(it) }
        val normalizedExamUrl = normalizedCustomQrExamUrl(draft) ?: return failGeneration(invalidExamUrlMessage)
        if (customQrScheduleProblem(draft.startTime, draft.endTime, System.currentTimeMillis()) ==
            CustomQrScheduleProblem.AlreadyEnded
        ) {
            return failGeneration(alreadyEndedMessage)
        }
        locationIssue?.let { return failGeneration(geofenceIssueMessage(uiLanguage, draft.locationShape, it)) }
        val updateUrl = if (draft.requireCurrentAppVersion && draft.appUpdateUrl.isNotBlank()) {
            validateExamUrl(normalizeAdminUrl(draft.appUpdateUrl), allowCleartext = false).normalizedUrl
                ?: return failGeneration(invalidUpdateUrlMessage)
        } else {
            ""
        }
        val payload = ExamQrPayload(
            examUrl = normalizedExamUrl,
            examName = draft.examName.trim(),
            startDateTime = draft.startTime,
            endDateTime = draft.endTime,
            saveToDirectLink = showSaveToDirectLinkOption && draft.saveToDirectLink,
            locationPolicy = buildCustomQrLocationPolicy(draft),
            locationPolicySource = LocationPolicySource.CustomQr,
            securityBypasses = draft.securityBypasses,
            minAppVersionCode = if (draft.requireCurrentAppVersion) BuildConfig.VERSION_CODE else 0,
            minAppVersionName = if (draft.requireCurrentAppVersion) BuildConfig.VERSION_NAME else "",
            appUpdateUrl = updateUrl
        )
        val encrypted = ExamQrCodec.encrypt(payload)
        if (!QrCodeGenerator.canEncode(encrypted)) {
            return failGeneration(qrTooLargeMessage)
        }
        onGeneratedQrPayloadChange(encrypted)
        onGenerationStatusChange(qrCreatedMessage)
        onGenerationIsErrorChange(false)
    }

    if (showCircleMapEditor || showPolygonMapEditor) {
        val polygon = showPolygonMapEditor
        val closeEditor = {
            onShowCircleMapEditorChange(false)
            onShowPolygonMapEditorChange(false)
        }
        GeofenceAreaEditorScreen(
            shape = if (polygon) GeofenceShapeType.Polygon else GeofenceShapeType.Circle,
            initialPoints = if (polygon) draft.polygonVertices else draft.circlePoints,
            initialRadiusMeters = draft.geofenceRadiusMeters,
            onDismiss = closeEditor,
            onSave = { points, radiusMeters ->
                updateDraft {
                    if (polygon) {
                        it.copy(polygonVertices = points)
                    } else {
                        it.copy(
                            geofenceCircleCenters = points,
                            geofenceCenterLat = points.firstOrNull()?.latitude.orEmpty(),
                            geofenceCenterLng = points.firstOrNull()?.longitude.orEmpty(),
                            geofenceRadiusMeters = radiusMeters
                        )
                    }
                }
                clearGeneratedQr()
                closeEditor()
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
                shape = RoundedCornerShape(UiTokens.RadiusXs),
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

        if (draftResumed) {
            Spacer(modifier = Modifier.height(10.dp))
            CustomQrResumedDraftBanner(onStartNewDraft = onStartNewDraft)
        }

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
                            value = draft.examUrl,
                            onValueChange = {
                                updateDraft { current -> current.copy(examUrl = it) }
                                clearGeneratedQr()
                            },
                            placeholder = tr("Exam URL (Required)", "URL Ujian (Wajib)"),
                            keyboardType = KeyboardType.Uri
                        )
                        AdminInputField(
                            value = draft.examName,
                            onValueChange = {
                                updateDraft { current -> current.copy(examName = it) }
                                clearGeneratedQr()
                            },
                            placeholder = tr("Exam Name (Required)", "Nama Ujian (Wajib)")
                        )
                        AdminPickerField(
                            value = draft.startTime,
                            placeholder = tr("Exam Date & Time", "Tanggal & Waktu Ujian"),
                            isActive = activePickerField == DateTimeField.Start,
                            onClick = {
                                activePickerField = DateTimeField.Start
                                draftDateTime = parseStoredDateTime(draft.startTime)
                                isTimePickerVisible = false
                            }
                        )
                        AdminPickerField(
                            value = draft.endTime,
                            placeholder = tr("End Date & Time", "Tanggal & Waktu Selesai"),
                            isActive = activePickerField == DateTimeField.End,
                            onClick = {
                                activePickerField = DateTimeField.End
                                draftDateTime = if (draft.endTime.isBlank()) {
                                    defaultEndFor(draft.startTime)
                                } else {
                                    parseStoredDateTime(draft.endTime)
                                }
                                isTimePickerVisible = false
                            }
                        )
                        if (scheduleProblem == CustomQrScheduleProblem.EndNotAfterStart) {
                            Text(
                                text = endNotAfterStartMessage,
                                color = AppColors.current.issueText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                CustomQrAdminTab.Location -> CustomQrLocationStep(
                    draft = draft,
                    onModeChange = { mode ->
                        updateDraft { it.withLocationMode(mode) }
                        clearGeneratedQr()
                        stepNavigationError = null
                    },
                    onOpenEditor = {
                        if (draft.locationShape == GeofenceShapeType.Polygon) {
                            onShowPolygonMapEditorChange(true)
                        } else {
                            onShowCircleMapEditorChange(true)
                        }
                    },
                    onClearPoints = {
                        updateDraft {
                            if (it.locationShape == GeofenceShapeType.Polygon) {
                                it.copy(polygonVertices = emptyList())
                            } else {
                                it.copy(geofenceCircleCenters = emptyList(), geofenceCenterLat = "", geofenceCenterLng = "")
                            }
                        }
                        clearGeneratedQr()
                    }
                )

                CustomQrAdminTab.Generate -> {
                    CustomQrBypassSection(
                        selected = draft.securityBypasses,
                        onSelectedChange = { bypasses ->
                            updateDraft { current -> current.copy(securityBypasses = bypasses) }
                            clearGeneratedQr()
                        }
                    )

                    CustomQrToggleCard(
                        title = tr(
                            "Require CBX Lock v${BuildConfig.VERSION_NAME} or newer",
                            "Wajibkan CBX Lock v${BuildConfig.VERSION_NAME} atau lebih baru"
                        ),
                        description = tr(
                            "Students on an older build are asked to update before they can start. Builds older than $MinVersionQrFirstReader cannot read this QR at all.",
                            "Siswa dengan versi lama diminta memperbarui dulu sebelum bisa mulai. Versi di bawah $MinVersionQrFirstReader tidak bisa membaca QR ini sama sekali."
                        ),
                        checked = draft.requireCurrentAppVersion,
                        onCheckedChange = {
                            updateDraft { current -> current.copy(requireCurrentAppVersion = it) }
                            clearGeneratedQr()
                        }
                    )
                    if (draft.requireCurrentAppVersion) {
                        AdminInputField(
                            value = draft.appUpdateUrl,
                            onValueChange = {
                                updateDraft { current -> current.copy(appUpdateUrl = it) }
                                clearGeneratedQr()
                            },
                            placeholder = tr(
                                "Installer download link (optional, e.g. Google Drive)",
                                "Link unduh installer (opsional, mis. Google Drive)"
                            ),
                            keyboardType = KeyboardType.Uri
                        )
                    }

                    if (showSaveToDirectLinkOption) {
                        CustomQrToggleCard(
                            title = tr(
                                "Save to Direct Link after scan",
                                "Setelah scan, simpan juga sebagai Direct Link"
                            ),
                            description = tr(
                                "When this QR is scanned, it will update the Direct Link config.",
                                "Saat QR ini dipindai, konfigurasi Direct Link akan diperbarui."
                            ),
                            checked = draft.saveToDirectLink,
                            onCheckedChange = {
                                updateDraft { current -> current.copy(saveToDirectLink = it) }
                                clearGeneratedQr()
                            }
                        )
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
                            examName = draft.examName,
                            startTime = draft.startTime,
                            endTime = draft.endTime,
                            locationPolicy = buildCustomQrLocationPolicy(draft),
                            securityBypasses = draft.securityBypasses,
                            minAppVersionName = BuildConfig.VERSION_NAME.takeIf { draft.requireCurrentAppVersion }
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
            // With large text the arrows push "Kembali" onto two lines on a narrow phone;
            // the words alone are clear enough.
            val showStepIcons = LocalDensity.current.fontScale <= 1.2f
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
                        contentPadding = StepButtonPadding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.current.surfaceSoft,
                            contentColor = AppColors.current.textPrimary
                        ),
                        border = BorderStroke(1.dp, AppColors.current.outlineStrong)
                    ) {
                        if (showStepIcons) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = tr("Previous", "Kembali"),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    contentPadding = StepButtonPadding,
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showStepIcons) {
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
    }

    val currentDraft = draftDateTime
    if (activePickerField != null && currentDraft != null && !isTimePickerVisible) {
        ComposeDatePickerDialog(
            initialDateMillis = currentDraft.toDatePickerUtcMillis(),
            onDismiss = {
                activePickerField = null
                draftDateTime = null
            },
            onConfirm = { selectedDateMillis ->
                draftDateTime = currentDraft.withDatePickerDay(selectedDateMillis)
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
                        // Moving the start past the end would leave an impossible window;
                        // carry the end along so the exam keeps a sensible length.
                        val end = parseCustomQrDateTimeMillis(current.endTime)
                        val newEnd = if (end == null || end <= completedCalendar.timeInMillis) {
                            formatDateTime(
                                (completedCalendar.clone() as Calendar).apply {
                                    timeInMillis += DefaultExamDurationMillis
                                }
                            )
                        } else {
                            current.endTime
                        }
                        current.copy(startTime = formattedValue, endTime = newEnd)
                    }
                    DateTimeField.End -> updateDraft { current ->
                        current.copy(endTime = formattedValue)
                    }
                    null -> Unit
                }
                clearGeneratedQr()
                stepNavigationError = null

                activePickerField = null
                draftDateTime = null
                isTimePickerVisible = false
            }
        )
    }
    }
}

/** An end time two hours after [startTime], for an End picker opened on an empty field. */
private fun defaultEndFor(startTime: String): Calendar {
    val start = parseCustomQrDateTimeMillis(startTime) ?: return Calendar.getInstance()
    return Calendar.getInstance().apply { timeInMillis = start + DefaultExamDurationMillis }
}

/**
 * The Material date picker works in UTC days. Handing it local time showed the day
 * before for any time earlier than the UTC offset (before 07:00 in WIB).
 */
internal fun Calendar.toDatePickerUtcMillis(): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(this@toDatePickerUtcMillis.get(Calendar.YEAR), this@toDatePickerUtcMillis.get(Calendar.MONTH), this@toDatePickerUtcMillis.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

/** Keeps this calendar's time of day and takes the day the picker returned (UTC). */
internal fun Calendar.withDatePickerDay(selectedUtcMillis: Long?): Calendar {
    val updated = clone() as Calendar
    if (selectedUtcMillis == null) {
        return updated
    }
    val day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedUtcMillis }
    updated.set(Calendar.YEAR, day.get(Calendar.YEAR))
    updated.set(Calendar.MONTH, day.get(Calendar.MONTH))
    updated.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH))
    return updated
}

internal const val CustomQrStartNewDraftTestTag = "custom_qr_start_new_draft"

/** Leaving Custom QR keeps the draft; this says so and is the one way to start over. */
@Composable
private fun CustomQrResumedDraftBanner(onStartNewDraft: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusMd),
        color = AppColors.current.blueTint,
        border = BorderStroke(1.dp, AppColors.current.blue.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Continuing your last draft.", "Melanjutkan draf terakhir."),
                modifier = Modifier.weight(1f),
                color = AppColors.current.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp
            )
            TextButton(
                onClick = onStartNewDraft,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(CustomQrStartNewDraftTestTag)
            ) {
                Text(
                    text = tr("Start new", "Mulai baru"),
                    color = AppColors.current.brandText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomQrToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusMd),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppColors.current.outlineStrong),
        onClick = { onCheckedChange(!checked) }
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
                    text = title,
                    color = AppColors.current.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = AppColors.current.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.current.blue,
                    uncheckedColor = AppColors.current.outlineStrong,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
