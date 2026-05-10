package com.example.coblaxexamlock.ui.preparation

import androidx.compose.runtime.Composable
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import java.util.Locale

internal data class PreparationChecklistNetworkText(
    val networkStatusLabel: String,
    val networkValue: String,
    val networkMeta: String?,
    val networkDetail: String?,
    val webViewProviderStatusLabel: String,
    val webViewProviderValue: String,
    val webViewProviderDetail: String?
)

@Composable
internal fun buildPreparationChecklistNetworkText(
    state: PreparationScreenState
): PreparationChecklistNetworkText = with(state) {
    val networkStatusLabel = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> tr("Stable", "Stabil")
        NetworkReadinessUserVerdict.Offline -> tr("Offline", "Offline")
        NetworkReadinessUserVerdict.Unvalidated -> tr("Unvalidated", "Belum Tervalidasi")
        NetworkReadinessUserVerdict.CaptivePortal -> tr("Captive Portal", "Captive Portal")
        NetworkReadinessUserVerdict.DnsFailed -> tr("DNS Failed", "DNS Gagal")
        NetworkReadinessUserVerdict.Slow -> tr("Slow", "Lambat")
        NetworkReadinessUserVerdict.VpnActive -> tr("VPN Active", "VPN Aktif")
        NetworkReadinessUserVerdict.AirplaneMode -> tr("Airplane Mode", "Mode Pesawat")
        NetworkReadinessUserVerdict.Unstable -> tr("Unstable", "Tidak Stabil")
    }
    val networkValue = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> tr(
            "Connected and ready on ${networkReadinessStatus.transportLabel}.",
            "Terhubung dan siap di ${networkReadinessStatus.transportLabel}."
        )
        NetworkReadinessUserVerdict.Offline -> tr(
            "No active internet connection is available right now.",
            "Saat ini belum ada koneksi internet aktif."
        )
        NetworkReadinessUserVerdict.Unvalidated -> tr(
            "A network is connected, but Android has not validated internet access yet.",
            "Jaringan sudah terhubung, tetapi Android belum memvalidasi akses internet."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> tr(
            "This network may still require a portal or login step before internet works.",
            "Jaringan ini mungkin masih membutuhkan portal atau langkah login sebelum internet bisa dipakai."
        )
        NetworkReadinessUserVerdict.DnsFailed -> tr(
            "Internet is connected, but DNS did not answer the quick probe.",
            "Internet terhubung, tetapi DNS tidak menjawab probe cepat."
        )
        NetworkReadinessUserVerdict.Slow -> tr(
            "Internet works, but the quick probe is slow. A steadier network is recommended.",
            "Internet bisa dipakai, tetapi probe cepat lambat. Jaringan yang lebih stabil disarankan."
        )
        NetworkReadinessUserVerdict.VpnActive -> if (bypassVpn) {
            tr(
                "VPN is active, but the approved VPN bypass is currently enabled.",
                "VPN aktif, tetapi bypass VPN resmi sedang aktif."
            )
        } else {
            tr(
                "VPN is active and Start Exam is blocked until it is turned off.",
                "VPN aktif dan Mulai Ujian diblokir sampai VPN dimatikan."
            )
        }
        NetworkReadinessUserVerdict.AirplaneMode -> tr(
            "Airplane mode is on and no active connection is available.",
            "Mode pesawat aktif dan belum ada koneksi aktif."
        )
        NetworkReadinessUserVerdict.Unstable -> tr(
            "The connection has changed several times recently. A stable network is recommended before and during the exam.",
            "Koneksi berubah beberapa kali belakangan ini. Jaringan yang stabil disarankan sebelum dan selama ujian."
        )
    }
    val networkLastChangeSummary = lastNetworkChangeAt?.ifBlank { "-" } ?: "-"
    val networkFlapMeta = when {
        networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable ||
            networkUnstableRuntimeStatus.flapCount > 0 ->
            tr(
                "Last change: $networkLastChangeSummary | Changes: ${networkUnstableRuntimeStatus.flapCount}",
                "Perubahan terakhir: $networkLastChangeSummary | Perubahan: ${networkUnstableRuntimeStatus.flapCount}"
            )
        else -> null
    }
    val networkProbeMeta = networkReadinessStatus.dnsProbeStatus
        .takeIf { it.verdict.name !in setOf("NotRun", "Skipped") }
        ?.let { probe ->
            tr(
                "DNS probe: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}",
                "Probe DNS: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}"
            )
        }
    val networkMeta = listOfNotNull(networkFlapMeta, networkProbeMeta)
        .joinToString("\n")
        .ifBlank { null }
    val networkDetail = networkReadinessStatus.userFacingQuickFixText ?: when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> null
        NetworkReadinessUserVerdict.Offline -> tr(
            "Check Wi-Fi or mobile data, then tap Refresh.",
            "Periksa Wi-Fi atau data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unvalidated -> tr(
            "Wait a moment or switch to a network with working internet, then tap Refresh.",
            "Tunggu sebentar atau pindah ke jaringan yang internetnya aktif, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> tr(
            "Complete the network login page first, then return here and tap Refresh.",
            "Selesaikan halaman login jaringan dahulu, lalu kembali dan tekan Refresh."
        )
        NetworkReadinessUserVerdict.DnsFailed -> tr(
            "Try another network or DNS, disable VPN if needed, then tap Refresh.",
            "Coba jaringan atau DNS lain, matikan VPN bila perlu, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Slow -> tr(
            "Move closer to Wi-Fi or switch network before starting.",
            "Dekatkan ke Wi-Fi atau pindah jaringan sebelum mulai."
        )
        NetworkReadinessUserVerdict.VpnActive -> if (bypassVpn) {
            tr(
                "VPN bypass active. Use only for approved troubleshooting and send a Network report if requested.",
                "Bypass VPN aktif. Gunakan hanya untuk troubleshooting resmi dan kirim report Network bila diminta."
            )
        } else {
            tr(
                "Turn off VPN from Android VPN settings, return here, then tap Refresh.",
                "Matikan VPN dari setelan VPN Android, kembali ke sini, lalu tekan Refresh."
            )
        }
        NetworkReadinessUserVerdict.AirplaneMode -> tr(
            "Turn off airplane mode or enable Wi-Fi/mobile data, then tap Refresh.",
            "Matikan mode pesawat atau aktifkan Wi-Fi/data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unstable -> tr(
            "Use the most stable available network before starting the exam.",
            "Gunakan jaringan yang paling stabil sebelum mulai ujian."
        )
    }
    val webViewHealthItem = preExamHealthCheckSnapshot.items.firstOrNull {
        it.category == PreExamHealthCategory.WebView
    }
    val webViewProviderStatusLabel = when (webViewHealthItem?.verdict) {
        PreExamHealthVerdict.Blocking -> tr("Unavailable", "Tidak Tersedia")
        PreExamHealthVerdict.Warning -> tr("Needs Update", "Perlu Update")
        PreExamHealthVerdict.Stable -> tr("Ready", "Siap")
        null -> tr("Unknown", "Tidak Diketahui")
    }
    val webViewProviderValue = webViewHealthItem?.detail ?: tr(
        "WebView provider status is not available yet.",
        "Status WebView provider belum tersedia."
    )
    val webViewProviderDetail = webViewHealthItem?.quickFix

    PreparationChecklistNetworkText(
        networkStatusLabel = networkStatusLabel,
        networkValue = networkValue,
        networkMeta = networkMeta,
        networkDetail = networkDetail,
        webViewProviderStatusLabel = webViewProviderStatusLabel,
        webViewProviderValue = webViewProviderValue,
        webViewProviderDetail = webViewProviderDetail
    )
}
