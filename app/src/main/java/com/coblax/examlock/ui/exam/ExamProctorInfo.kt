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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * What the CBX Exam System page on screen says about the student and the exam, read from
 * its markup when the Info sheet opens (the site keeps no participant data in storage
 * CBX Lock could read). [page] is "ujian" (/ujian/<id>), "siswa" (student home) or "lain".
 */
internal data class ExamSiteInfo(
    val page: String = "lain",
    val nama: String? = null,
    val nis: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    val ruang: String? = null,
    val ruangNama: String? = null,
    val paket: String? = null,
    /** "Tersimpan", "Menyimpan", "Offline" or "Mencoba lagi", as the exam page shows it. */
    val simpan: String? = null,
    /** Answers not yet sent to the server, shown with Offline / Mencoba lagi. */
    val tertunda: Int? = null,
    val dijawab: Int? = null,
    val total: Int? = null,
    val sisaWaktu: String? = null
)

/**
 * Reads the CBX Exam System page: the exam screen's top bar (status, answered count,
 * timer, and a span with name · NIS · room that phones hide with CSS but keep in the
 * DOM), class and room name from the page's embedded data, or the student home header.
 * Element by element, since textContent runs neighbouring blocks together. ES5.
 */
internal const val ExamSiteInfoProbeScript = """
(function() {
  try {
    var out = { page: 'lain' };
    function txt(el) { return el ? (el.textContent || '').replace(/\s+/g, ' ').trim() : ''; }
    var path = location.pathname || '';
    var header = document.querySelector('header');
    if (/^\/ujian\/\d+/.test(path) && header) {
      out.page = 'ujian';
      var title = header.querySelector('p[title]');
      if (title) out.paket = title.getAttribute('title');
      var spans = header.querySelectorAll('span');
      for (var i = 0; i < spans.length; i++) {
        var t = txt(spans[i]);
        if (!out.nis) {
          var m = t.match(/^\u00b7?\s*(.+?)\s*\u00b7\s*NIS\s+(\S+)\s*\u00b7\s*(.+)$/);
          if (m) { out.nama = m[1]; out.nis = m[2]; out.ruang = m[3]; continue; }
        }
        if (out.total == null) {
          var d = t.match(/^(\d+)\s*\/\s*(\d+)(\s*dijawab)?$/);
          if (d) { out.dijawab = +d[1]; out.total = +d[2]; continue; }
        }
        if (!out.simpan) {
          var s = t.match(/^(Tersimpan|Menyimpan|Offline|Mencoba lagi)\u2026?(?:\s*\u00b7\s*(\d+))?$/);
          if (s) { out.simpan = s[1]; if (s[2]) out.tertunda = +s[2]; }
        }
      }
      var timer = document.querySelector('[role="timer"]');
      if (timer) out.sisaWaktu = txt(timer);
      // Class and room names are only in the page's embedded data when /ujian/<id> was
      // loaded in full (a reload or restart); the site's own link there navigates in
      // the page. Scanned once per address.
      if (window.__cbxInfoPath !== path) {
        var html = document.documentElement.innerHTML;
        var k = html.match(/\\?"kelasNama\\?":\\?"([^"\\]{1,60})/);
        var r = html.match(/\\?"ruangNama\\?":\\?"([^"\\]{1,60})/);
        window.__cbxInfoPath = path;
        window.__cbxInfoKelas = k && k[1] !== '-' ? k[1] : null;
        window.__cbxInfoRuang = r ? r[1] : null;
      }
      if (window.__cbxInfoKelas) out.kelas = window.__cbxInfoKelas;
      if (window.__cbxInfoRuang) out.ruangNama = window.__cbxInfoRuang;
      if (!out.kelas && out.nis) {
        try {
          var seen = JSON.parse(sessionStorage.getItem('cbx_lock_info_siswa') || 'null');
          if (seen && seen.nis === out.nis && seen.kelas) out.kelas = seen.kelas;
        } catch (ignored) {}
      }
    } else if (/^\/siswa/.test(path)) {
      out.page = 'siswa';
      var h1 = document.querySelector('h1');
      if (h1) out.nama = txt(h1);
      var all = document.querySelectorAll('span');
      for (var j = 0; j < all.length; j++) {
        var n = txt(all[j]).match(/^NIS\s+(\S+)$/);
        if (!n) continue;
        out.nis = n[1];
        var sib = all[j].parentNode ? all[j].parentNode.children : [];
        var extra = [];
        for (var x = 0; x < sib.length; x++) { if (sib[x] !== all[j] && txt(sib[x])) extra.push(txt(sib[x])); }
        if (extra[0]) out.kelas = extra[0];
        if (extra[1]) out.jurusan = extra[1];
        break;
      }
      if (out.nis && out.kelas) {
        try {
          sessionStorage.setItem('cbx_lock_info_siswa', JSON.stringify({ nis: out.nis, kelas: out.kelas }));
        } catch (ignored) {}
      }
    }
    return JSON.stringify(out);
  } catch (e) {
    return JSON.stringify({ page: 'error' });
  }
})()
"""

internal const val ExamSiteInfoRefreshMillis = 5_000L

/** [raw] is evaluateJavascript's answer: the probe's JSON string, quoted again. */
internal fun parseExamSiteInfo(raw: String?): ExamSiteInfo? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return runCatching {
        val json = when (val value = org.json.JSONTokener(raw).nextValue()) {
            is String -> org.json.JSONObject(value)
            is org.json.JSONObject -> value
            else -> return null
        }
        fun str(key: String): String? =
            if (json.isNull(key) || !json.has(key)) null else json.optString(key).trim().take(80).ifEmpty { null }
        fun int(key: String): Int? =
            if (json.isNull(key) || !json.has(key)) null else json.optInt(key, -1).takeIf { it >= 0 }
        ExamSiteInfo(
            page = str("page") ?: "lain",
            nama = str("nama"),
            nis = str("nis"),
            kelas = str("kelas"),
            jurusan = str("jurusan"),
            ruang = str("ruang"),
            ruangNama = str("ruangNama"),
            paket = str("paket"),
            simpan = str("simpan"),
            tertunda = int("tertunda"),
            dijawab = int("dijawab"),
            total = int("total"),
            sisaWaktu = str("sisaWaktu")
        )
    }.getOrNull()
}

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

/** "Siti Aminah (12345)" from the exam site's own page. */
internal fun proctorSiteParticipant(site: ExamSiteInfo?): String? {
    if (site == null) return null
    val name = site.nama.orEmpty()
    val nis = site.nis.orEmpty()
    return when {
        name.isNotEmpty() && nis.isNotEmpty() -> "$name ($nis)"
        name.isNotEmpty() -> name
        nis.isNotEmpty() -> "NIS $nis"
        else -> null
    }
}

/** "XII IPA 1 · Ruang 7" (room name when the page carries it, else its code). */
internal fun proctorSiteGroup(site: ExamSiteInfo?): String? {
    if (site == null) return null
    // Class only: the home page's department name is long and the class name implies it.
    return listOfNotNull(site.kelas, site.ruangNama ?: site.ruang).joinToString(" · ").ifEmpty { null }
}

/** Save status and progress, e.g. "Saved · 12/40 answered" or "Offline · 3 not sent". */
internal fun proctorSiteAnswers(site: ExamSiteInfo?, language: UiLanguage): ExamProctorInfoRow? {
    if (site == null || site.page != "ujian") return null
    fun t(english: String, indonesian: String) = localized(language, english, indonesian)
    val pending = site.tertunda?.takeIf { it > 0 }
    val (status, tone) = when (site.simpan) {
        "Tersimpan" -> t("Saved", "Tersimpan") to UiStatusTone.Success
        "Menyimpan" -> t("Saving", "Menyimpan") to UiStatusTone.Info
        "Offline" -> t("Offline", "Offline") to UiStatusTone.Warning
        "Mencoba lagi" -> t("Retrying", "Mencoba lagi") to UiStatusTone.Warning
        else -> null to UiStatusTone.Neutral
    }
    val progress = if (site.dijawab != null && site.total != null) {
        t("${site.dijawab}/${site.total} answered", "${site.dijawab}/${site.total} dijawab")
    } else {
        null
    }
    val unsent = pending?.let { t("$it not sent yet", "$it belum terkirim") }
    val value = listOfNotNull(status, unsent, progress).joinToString(" · ").ifEmpty { return null }
    return ExamProctorInfoRow(t("Answers", "Jawaban"), value, if (pending != null) UiStatusTone.Warning else tone)
}

internal fun buildExamProctorInfoRows(
    state: ExamRuntimeChromeState,
    language: UiLanguage,
    clockText: String,
    deviceText: String,
    appVersionText: String,
    site: ExamSiteInfo? = null
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
                proctorSiteParticipant(site)
                    ?: state.participantName
                    ?: t("Not read from the exam site yet", "Belum terbaca dari situs ujian")
            )
        )
        (proctorSiteGroup(site) ?: state.participantGroup)?.let {
            add(ExamProctorInfoRow(t("Class · Room", "Kelas · Ruang"), it))
        }
        add(ExamProctorInfoRow(t("Exam", "Ujian"), site?.paket ?: state.examDisplayName.ifBlank { "-" }))
        site?.sisaWaktu?.takeIf { site.page == "ujian" }?.let {
            add(ExamProctorInfoRow(t("Time left", "Sisa waktu"), it))
        }
        proctorSiteAnswers(site, language)?.let { add(it) }
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
    onRestartApp: () -> Unit = {},
    probeSite: ((String?) -> Unit) -> Unit = { it(null) }
) {
    val language = LocalUiLanguage.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L - nowMillis % 1_000L)
        }
    }
    // Re-read the page while the sheet is open: the timer and save status move. A failed
    // read keeps the last good one rather than blanking rows a proctor is looking at.
    // The session rebuilds its action lambdas every composition, and the clock above
    // recomposes every second, so key the loop on nothing and call the latest probe.
    var siteInfo by remember { mutableStateOf<ExamSiteInfo?>(null) }
    val currentProbeSite by rememberUpdatedState(probeSite)
    LaunchedEffect(Unit) {
        while (true) {
            currentProbeSite { raw -> parseExamSiteInfo(raw)?.takeIf { it.page != "error" }?.let { siteInfo = it } }
            delay(ExamSiteInfoRefreshMillis)
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
        appVersionText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        site = siteInfo
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
