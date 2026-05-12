package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.UiLanguage
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

internal fun buildPreparationChecklistNetworkText(
    state: PreparationScreenState,
    uiLanguage: UiLanguage
): PreparationChecklistNetworkText = with(state) {
    fun t(english: String, indonesian: String): String = localized(uiLanguage, english, indonesian)
    val networkStatusLabel = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> t("Stable", "Stabil")
        NetworkReadinessUserVerdict.Offline -> t("Offline", "Offline")
        NetworkReadinessUserVerdict.Unvalidated -> t("Unvalidated", "Belum Tervalidasi")
        NetworkReadinessUserVerdict.CaptivePortal -> t("Captive Portal", "Captive Portal")
        NetworkReadinessUserVerdict.DnsFailed -> t("DNS Failed", "DNS Gagal")
        NetworkReadinessUserVerdict.Slow -> t("Slow", "Lambat")
        NetworkReadinessUserVerdict.VpnActive -> t("VPN Active", "VPN Aktif")
        NetworkReadinessUserVerdict.AirplaneMode -> t("Airplane Mode", "Mode Pesawat")
        NetworkReadinessUserVerdict.Unstable -> t("Unstable", "Tidak Stabil")
    }
    val networkValue = when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> t(
            "Connected and ready on ${networkReadinessStatus.transportLabel}.",
            "Terhubung dan siap di ${networkReadinessStatus.transportLabel}."
        )
        NetworkReadinessUserVerdict.Offline -> t(
            "No active internet connection is available right now.",
            "Saat ini belum ada koneksi internet aktif."
        )
        NetworkReadinessUserVerdict.Unvalidated -> t(
            "A network is connected, but Android has not validated internet access yet.",
            "Jaringan sudah terhubung, tetapi Android belum memvalidasi akses internet."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> t(
            "This network may still require a portal or login step before internet works.",
            "Jaringan ini mungkin masih membutuhkan portal atau langkah login sebelum internet bisa dipakai."
        )
        NetworkReadinessUserVerdict.DnsFailed -> t(
            "Internet is connected, but DNS did not answer the quick probe.",
            "Internet terhubung, tetapi DNS tidak menjawab probe cepat."
        )
        NetworkReadinessUserVerdict.Slow -> t(
            "Internet works, but the quick probe is slow. A steadier network is recommended.",
            "Internet bisa dipakai, tetapi probe cepat lambat. Jaringan yang lebih stabil disarankan."
        )
        NetworkReadinessUserVerdict.VpnActive -> if (bypassVpn) {
            t(
                "VPN is active, but the approved VPN bypass is currently enabled.",
                "VPN aktif, tetapi bypass VPN resmi sedang aktif."
            )
        } else {
            t(
                "VPN is active and Start Exam is blocked until it is turned off.",
                "VPN aktif dan Mulai Ujian diblokir sampai VPN dimatikan."
            )
        }
        NetworkReadinessUserVerdict.AirplaneMode -> t(
            "Airplane mode is on and no active connection is available.",
            "Mode pesawat aktif dan belum ada koneksi aktif."
        )
        NetworkReadinessUserVerdict.Unstable -> t(
            "The connection has changed several times recently. A stable network is recommended before and during the exam.",
            "Koneksi berubah beberapa kali belakangan ini. Jaringan yang stabil disarankan sebelum dan selama ujian."
        )
    }
    val networkLastChangeSummary = lastNetworkChangeAt?.ifBlank { "-" } ?: "-"
    val networkFlapMeta = when {
        networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable ||
            networkUnstableRuntimeStatus.flapCount > 0 ->
            t(
                "Last change: $networkLastChangeSummary | Changes: ${networkUnstableRuntimeStatus.flapCount}",
                "Perubahan terakhir: $networkLastChangeSummary | Perubahan: ${networkUnstableRuntimeStatus.flapCount}"
            )
        else -> null
    }
    val networkProbeMeta = networkReadinessStatus.dnsProbeStatus
        .takeIf { it.verdict.name !in setOf("NotRun", "Skipped") }
        ?.let { probe ->
            t(
                "DNS probe: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}",
                "Probe DNS: ${probe.verdict.name} | ${probe.latencyBucket.name.lowercase(Locale.US)}"
            )
        }
    val networkMeta = listOfNotNull(networkFlapMeta, networkProbeMeta)
        .joinToString("\n")
        .ifBlank { null }
    val networkActionDetail = networkReadinessStatus.userFacingQuickFixText ?: when (networkReadinessStatus.userFacingVerdict) {
        NetworkReadinessUserVerdict.Stable -> null
        NetworkReadinessUserVerdict.Offline -> t(
            "Check Wi-Fi or mobile data, then tap Refresh.",
            "Periksa Wi-Fi atau data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unvalidated -> t(
            "Wait a moment or switch to a network with working internet, then tap Refresh.",
            "Tunggu sebentar atau pindah ke jaringan yang internetnya aktif, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.CaptivePortal -> t(
            "Complete the network login page first, then return here and tap Refresh.",
            "Selesaikan halaman login jaringan dahulu, lalu kembali dan tekan Refresh."
        )
        NetworkReadinessUserVerdict.DnsFailed -> t(
            "Try another network or DNS, disable VPN if needed, then tap Refresh.",
            "Coba jaringan atau DNS lain, matikan VPN bila perlu, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Slow -> t(
            "Move closer to Wi-Fi or switch network before starting.",
            "Dekatkan ke Wi-Fi atau pindah jaringan sebelum mulai."
        )
        NetworkReadinessUserVerdict.VpnActive -> if (bypassVpn) {
            t(
                "VPN bypass active. Use only for approved troubleshooting and send a Network report if requested.",
                "Bypass VPN aktif. Gunakan hanya untuk troubleshooting resmi dan kirim report Network bila diminta."
            )
        } else {
            t(
                "Turn off VPN from Android VPN settings, return here, then tap Refresh.",
                "Matikan VPN dari setelan VPN Android, kembali ke sini, lalu tekan Refresh."
            )
        }
        NetworkReadinessUserVerdict.AirplaneMode -> t(
            "Turn off airplane mode or enable Wi-Fi/mobile data, then tap Refresh.",
            "Matikan mode pesawat atau aktifkan Wi-Fi/data seluler, lalu tekan Refresh."
        )
        NetworkReadinessUserVerdict.Unstable -> t(
            "Use the most stable available network before starting the exam.",
            "Gunakan jaringan yang paling stabil sebelum mulai ujian."
        )
    }
    val networkAuditDetail = if (showChecklistDetails) {
        buildPreparationNetworkAuditDetail(
            status = networkReadinessStatus,
            unstableStatus = networkUnstableRuntimeStatus,
            lastNetworkChangeAt = lastNetworkChangeAt,
            lastNetworkChangeSource = lastNetworkChangeSource,
            lastConnectedNetworkLabel = lastConnectedNetworkLabel,
            bypassVpn = bypassVpn,
            vpnBypassState = vpnBypassState,
            isRefreshingNetwork = isRefreshingNetwork,
            uiLanguage = uiLanguage
        )
    } else {
        null
    }
    val networkDetail = appendPreparationAuditDetail(networkActionDetail, networkAuditDetail)
    val webViewHealthItem = preExamHealthCheckSnapshot.items.firstOrNull {
        it.category == PreExamHealthCategory.WebView
    }
    val webViewProviderStatusLabel = when (webViewHealthItem?.verdict) {
        PreExamHealthVerdict.Blocking -> t("Unavailable", "Tidak Tersedia")
        PreExamHealthVerdict.Warning -> t("Needs Update", "Perlu Update")
        PreExamHealthVerdict.Stable -> t("Ready", "Siap")
        null -> t("Unknown", "Tidak Diketahui")
    }
    val webViewProviderValue = webViewHealthItem?.detail ?: t(
        "WebView provider status is not available yet.",
        "Status WebView provider belum tersedia."
    )
    val webViewProviderAuditDetail = if (showChecklistDetails) {
        buildPreparationWebViewAuditDetail(
            status = webViewCompatibilityStatus,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            webViewSessionResetError = webViewSessionResetError,
            uiLanguage = uiLanguage
        )
    } else {
        null
    }
    val webViewProviderDetail = appendPreparationAuditDetail(
        actionDetail = webViewHealthItem?.quickFix,
        auditDetail = webViewProviderAuditDetail
    )

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
