package com.coblax.examlock.ui.exam

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.ExamParticipantContext
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.NetworkReadinessVerdict
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialogContent
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.resolveUiStatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * "Info" in the exam footer: what a proctor needs to look at when a student raises a
 * hand, without the student leaving the exam. The status bar is hidden in lock mode, so
 * it also carries the time.
 *
 * Drawn inline over the exam page, like the page-error card, and not as a dialog window:
 * a dialog takes window focus, which the exam guard has to be told about or it counts
 * as leaving the exam. The exam server's address is left out, as everywhere students see.
 */
internal data class ExamProctorInfoRow(
    val label: String,
    val value: String,
    val tone: UiStatusTone = UiStatusTone.Neutral
)

/** "Siti Aminah (12345)", or whichever of the two the exam site exposes. */
internal fun proctorParticipantName(context: ExamParticipantContext): String? {
    val name = context.displayName.orEmpty().trim()
    val account = context.username.orEmpty().trim().ifEmpty { context.userId.orEmpty().trim() }
    return when {
        name.isNotEmpty() && account.isNotEmpty() && !name.equals(account, ignoreCase = true) ->
            "$name ($account)"
        name.isNotEmpty() -> name
        account.isNotEmpty() -> account
        else -> null
    }
}

/** Class and room codes as the exam site names them, e.g. "XII IPA 1 · R07". */
internal fun proctorParticipantGroup(context: ExamParticipantContext): String? =
    listOfNotNull(
        context.kodeKelas?.trim()?.takeIf { it.isNotEmpty() },
        context.kodeRuang?.trim()?.takeIf { it.isNotEmpty() }
    ).joinToString(" · ").ifEmpty { null }

internal fun buildExamProctorInfoRows(
    state: ExamRuntimeChromeState,
    language: UiLanguage,
    clockText: String,
    deviceText: String,
    appVersionText: String
): List<ExamProctorInfoRow> {
    fun t(english: String, indonesian: String) = localized(language, english, indonesian)

    val connectivity = resolveExamFooterConnectivityVisual(state.networkStatus, state.serverStatus)
    val transport = when (connectivity.transport) {
        ExamFooterConnectivityTransport.Wifi -> "Wi-Fi"
        ExamFooterConnectivityTransport.Cellular ->
            listOfNotNull(t("Mobile data", "Data seluler"), connectivity.cellularLabel).joinToString(" ")
        ExamFooterConnectivityTransport.Unknown -> null
    }
    val network = when (state.networkStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> t("Connected", "Terhubung")
        NetworkReadinessVerdict.Offline -> t("Offline", "Offline")
        NetworkReadinessVerdict.AirplaneMode -> t("Airplane mode", "Mode pesawat")
        NetworkReadinessVerdict.Unvalidated -> t("Limited", "Terbatas")
        NetworkReadinessVerdict.CaptivePortal -> t("Network sign-in needed", "Perlu login jaringan")
        NetworkReadinessVerdict.VpnActive -> t("VPN active", "VPN aktif")
        NetworkReadinessVerdict.Unstable -> t("Unstable", "Tidak stabil")
    }
    val networkTone = when (connectivity.severity) {
        ExamFooterConnectivitySeverity.Stable -> UiStatusTone.Success
        ExamFooterConnectivitySeverity.Info -> UiStatusTone.Info
        ExamFooterConnectivitySeverity.Warning -> UiStatusTone.Warning
    }
    val (server, serverTone) = when (state.serverStatus) {
        ExamServerFooterStatus.Online -> t("Online", "Online") to UiStatusTone.Success
        ExamServerFooterStatus.Checking -> t("Checking", "Memeriksa") to UiStatusTone.Info
        ExamServerFooterStatus.Warning -> t("Responding with a warning", "Menjawab dengan peringatan") to UiStatusTone.Warning
        ExamServerFooterStatus.Unstable -> t("Unstable", "Tidak stabil") to UiStatusTone.Warning
        ExamServerFooterStatus.Offline -> t("Not responding", "Tidak menjawab") to UiStatusTone.Warning
        ExamServerFooterStatus.Insecure -> t("Online, not secure", "Online, tidak aman") to UiStatusTone.Warning
    }
    val (security, securityTone) = when (state.shieldStatus) {
        ExamFooterShieldStatus.Safe -> t("Protected", "Aman") to UiStatusTone.Success
        ExamFooterShieldStatus.Warning -> t("Needs checking", "Perlu diperiksa") to UiStatusTone.Warning
        ExamFooterShieldStatus.Danger -> t("Blocked", "Diblokir") to UiStatusTone.Danger
    }
    val percent = state.batteryStatus.levelPercent.coerceIn(0, 100)
    val battery = if (state.batteryStatus.isCharging) {
        t("$percent%, charging", "$percent%, sedang diisi")
    } else {
        "$percent%"
    }
    val batteryTone = when {
        state.batteryStatus.isCharging -> UiStatusTone.Success
        percent <= 20 -> UiStatusTone.Danger
        percent <= 40 -> UiStatusTone.Warning
        else -> UiStatusTone.Neutral
    }

    return buildList {
        add(ExamProctorInfoRow(t("Time", "Jam"), clockText))
        add(
            ExamProctorInfoRow(
                t("Participant", "Peserta"),
                state.participantName ?: t("Not read from the exam site yet", "Belum terbaca dari situs ujian")
            )
        )
        state.participantGroup?.let { add(ExamProctorInfoRow(t("Class · Room", "Kelas · Ruang"), it)) }
        add(ExamProctorInfoRow(t("Exam", "Ujian"), state.examDisplayName.ifBlank { "-" }))
        add(
            ExamProctorInfoRow(
                t("Network", "Jaringan"),
                listOfNotNull(transport, network).joinToString(" · "),
                networkTone
            )
        )
        add(ExamProctorInfoRow(t("Exam server", "Server ujian"), server, serverTone))
        add(ExamProctorInfoRow(t("Security", "Keamanan"), security, securityTone))
        add(ExamProctorInfoRow(t("Battery", "Baterai"), battery, batteryTone))
        add(ExamProctorInfoRow(t("Phone", "HP"), deviceText))
        add(ExamProctorInfoRow("CBX Lock", appVersionText))
    }
}

/** "Samsung SM-A105F · Android 11", without repeating a maker the model already names. */
internal fun proctorDeviceText(manufacturer: String, model: String, androidRelease: String): String {
    val maker = manufacturer.trim().replaceFirstChar { it.titlecase(Locale.ROOT) }
    val name = model.trim().let { trimmedModel ->
        if (maker.isEmpty() || trimmedModel.startsWith(maker, ignoreCase = true)) {
            trimmedModel
        } else {
            "$maker $trimmedModel"
        }
    }.ifEmpty { "-" }
    return "$name · Android ${androidRelease.trim().ifEmpty { "-" }}"
}

@Composable
internal fun ExamProctorInfoOverlay(
    state: ExamRuntimeChromeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onRestartApp: () -> Unit = {}
) {
    val language = LocalUiLanguage.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L - nowMillis % 1_000L)
        }
    }
    // Time of day only: it fits one line on a small phone, and it is what a proctor notes.
    val clockFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.ROOT) }
    val deviceText = remember {
        proctorDeviceText(Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty(), Build.VERSION.RELEASE.orEmpty())
    }
    val rows = buildExamProctorInfoRows(
        state = state,
        language = language,
        clockText = clockFormat.format(Date(nowMillis)),
        deviceText = deviceText,
        appVersionText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    )

    // The scrim takes every touch, so nothing reaches the exam page underneath, and a
    // tap outside the card closes it like any sheet.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        AppAlertDialogContent(
            tone = UiStatusTone.Info,
            icon = Icons.Outlined.Info,
            title = tr("Info for the proctor", "Info untuk pengawas"),
            message = tr(
                "Show this screen to your proctor if something is wrong. The exam keeps running.",
                "Tunjukkan layar ini ke pengawas jika ada masalah. Ujian tetap berjalan."
            ),
            primaryAction = AppAlertAction(label = tr("Close", "Tutup"), onClick = onClose),
            // For a white page that shows no error card: what a phone restart would fix,
            // in seconds. It ends the exam session, hence behind Info and not in the footer.
            secondaryActions = listOf(
                AppAlertAction(
                    label = tr("Restart app (leaves the exam)", "Mulai ulang aplikasi (keluar ujian)"),
                    onClick = onRestartApp,
                    style = com.coblax.examlock.ui.dialog.AppAlertStyle.Destructive
                )
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(ExamRuntimeUiTestTags.ProctorInfoSheet)
                .pointerInput(Unit) { detectTapGestures { } },
            extraContent = { ProctorInfoRows(rows) }
        )
    }
}

@Composable
private fun ProctorInfoRows(rows: List<ExamProctorInfoRow>) {
    val colors = AppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            val valueColor = when (row.tone) {
                UiStatusTone.Neutral -> colors.textPrimary
                else -> resolveUiStatusColors(colors, row.tone).contentColor
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = row.label,
                    modifier = Modifier.weight(0.38f),
                    color = colors.textSecondary,
                    style = AppTextStyles.label
                )
                Text(
                    text = row.value,
                    modifier = Modifier.weight(0.62f),
                    color = valueColor,
                    style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
