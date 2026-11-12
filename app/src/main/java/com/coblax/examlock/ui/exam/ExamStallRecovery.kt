package com.coblax.examlock.ui.exam

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.coblax.examlock.runtime.resolveNetworkDnsAddresses
import java.net.Inet6Address
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener

/**
 * An exam page that stops loading with the network up: white screen, progress bar frozen
 * part-way. Seen after a phone's Wi-Fi dropped and came back; reloading and restarting the
 * app did not help, restarting the phone did. That points at state the phone keeps, such
 * as a DNS answer or route cached during the drop, rather than at the page.
 *
 * The first stall is reported as before. A second one in a row tells the student what
 * clears that state without a restart, and the admin's diagnostics record what hung.
 */
internal const val ExamStallDetailMarker = " (STALL: "

internal fun examStallMessage(consecutiveStalls: Int, stallSeconds: Long): String =
    if (consecutiveStalls <= 1) {
        "Halaman ujian berhenti memuat selama $stallSeconds detik. Tekan Muat ulang. " +
            "Jika tetap macet, tekan Mulai ulang aplikasi."
    } else {
        "Halaman ujian macet lagi walaupun internet tersambung. Koneksi aplikasi ke server ujian " +
            "kemungkinan tersangkut setelah sempat putus. Tekan Mulai ulang aplikasi, lalu buka " +
            "ujian lagi. Jika masih macet, nyalakan mode pesawat 10 detik lalu matikan lagi. " +
            "Tidak perlu restart HP."
    }

/** The error text handed to the session: what the student reads, then the stall count. */
internal fun examStallErrorDescription(consecutiveStalls: Int, stallSeconds: Long): String =
    examStallMessage(consecutiveStalls, stallSeconds) + ExamStallDetailMarker + consecutiveStalls + ")"

/**
 * Lists the scripts and stylesheets the page asked for that never finished: the resource
 * timeline only gains an entry once a resource completes or fails. ES5, for Android 7.
 */
internal const val ExamStallProbeScript = """
(function() {
  try {
    var done = {};
    var entries = (window.performance && performance.getEntriesByType)
      ? performance.getEntriesByType('resource') : [];
    for (var i = 0; i < entries.length; i++) { done[entries[i].name] = true; }
    var nodes = document.querySelectorAll('script[src],link[rel~="stylesheet"][href]');
    var pending = [];
    for (var j = 0; j < nodes.length; j++) {
      var node = nodes[j];
      var url = node.src || node.href;
      if (!url || done[url]) continue;
      var a = document.createElement('a');
      a.href = url;
      pending.push(a.host + (node.tagName === 'SCRIPT' ? ' js' : ' css'));
    }
    return JSON.stringify({
      ready: document.readyState,
      resources: nodes.length,
      finished: entries.length,
      pending: pending.slice(0, 8),
      pendingCount: pending.length
    });
  } catch (e) {
    return JSON.stringify({ error: String(e) });
  }
})()
"""

internal data class ExamStallPageEvidence(
    val readyState: String = "-",
    val resources: Int = 0,
    val finished: Int = 0,
    /** "host[:port] js|css" for each page-critical resource that never finished. */
    val pending: List<String> = emptyList(),
    val pendingCount: Int = 0,
    val probeError: String? = null
) {
    val pendingHosts: List<String>
        get() = pending.map { it.substringBefore(' ').substringBefore(':') }
            .filter { it.isNotBlank() }
            .distinct()
}

/** [raw] is what evaluateJavascript hands back: the script's string result, JSON-quoted. */
internal fun parseExamStallProbe(raw: String?): ExamStallPageEvidence {
    if (raw.isNullOrBlank() || raw == "null") return ExamStallPageEvidence(probeError = "no_result")
    return runCatching {
        val json = when (val value = JSONTokener(raw).nextValue()) {
            is String -> JSONObject(value)
            is JSONObject -> value
            else -> return ExamStallPageEvidence(probeError = "unexpected_result")
        }
        if (json.has("error")) return ExamStallPageEvidence(probeError = json.optString("error").take(80))
        val pendingArray = json.optJSONArray("pending")
        ExamStallPageEvidence(
            readyState = json.optString("ready", "-"),
            resources = json.optInt("resources"),
            finished = json.optInt("finished"),
            pending = List(pendingArray?.length() ?: 0) { index -> pendingArray!!.optString(index) }
                .filter { it.isNotBlank() },
            pendingCount = json.optInt("pendingCount")
        )
    }.getOrElse { ExamStallPageEvidence(probeError = "unparseable") }
}

internal data class ExamStallNetworkEvidence(
    val transport: String = "-",
    val validated: Boolean? = null,
    val dnsServers: List<String> = emptyList(),
    val privateDns: String = "-",
    val ipv6: Boolean? = null,
    /** host → resolved addresses, or the reason the lookup failed. */
    val lookups: List<Pair<String, String>> = emptyList()
)

private const val StallDnsLookupTimeoutMillis = 4_000L

/** What the phone's own network says right after the stall. Admin diagnostics only. */
internal suspend fun collectExamStallNetworkEvidence(
    context: Context,
    hosts: List<String>
): ExamStallNetworkEvidence {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val network = runCatching { connectivity?.activeNetwork }.getOrNull()
    val links = network?.let { runCatching { connectivity?.getLinkProperties(it) }.getOrNull() }
    val capabilities = network?.let { runCatching { connectivity?.getNetworkCapabilities(it) }.getOrNull() }
    val transport = when {
        capabilities == null -> "none"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
        else -> "other"
    }
    val privateDns = when {
        links == null -> "-"
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> "unsupported"
        !links.isPrivateDnsActive -> "off"
        links.privateDnsServerName.isNullOrBlank() -> "auto"
        else -> "strict:${links.privateDnsServerName}"
    }
    val lookups = coroutineScope {
        hosts.distinct().take(4).map { host ->
            async {
                val outcome = withTimeoutOrNull(StallDnsLookupTimeoutMillis) {
                    runCatching { resolveNetworkDnsAddresses(host) }
                        .fold(
                            onSuccess = { addresses -> addresses.joinToString(",").ifEmpty { "empty" } },
                            onFailure = { error -> error.javaClass.simpleName.ifBlank { "failed" } }
                        )
                } ?: "timeout"
                host to outcome
            }
        }.awaitAll()
    }
    return ExamStallNetworkEvidence(
        transport = transport,
        validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        dnsServers = links?.dnsServers.orEmpty().mapNotNull { it.hostAddress },
        privateDns = privateDns,
        ipv6 = links?.linkAddresses?.any { link ->
            val address = link.address
            address is Inet6Address && !address.isLinkLocalAddress
        },
        lookups = lookups
    )
}

internal fun formatExamStallEvidence(
    attempt: Int,
    page: ExamStallPageEvidence,
    network: ExamStallNetworkEvidence
): String = buildString {
    fun yesNo(value: Boolean?) = when (value) { true -> "yes"; false -> "no"; null -> "-" }
    append("attempt=").append(attempt)
    if (page.probeError != null) {
        append(" | page_probe=").append(page.probeError)
    } else {
        append(" | ready=").append(page.readyState)
        append(" | resources=").append(page.finished).append('/').append(page.resources).append(" finished")
        append(" | pending=").append(page.pendingCount)
        if (page.pending.isNotEmpty()) append(" [").append(page.pending.joinToString(", ")).append(']')
    }
    append(" | transport=").append(network.transport)
    append(" | validated=").append(yesNo(network.validated))
    append(" | dns=[").append(network.dnsServers.joinToString(",")).append(']')
    append(" | private_dns=").append(network.privateDns)
    append(" | ipv6=").append(yesNo(network.ipv6))
    if (network.lookups.isNotEmpty()) {
        append(" | resolve: ")
        append(network.lookups.joinToString(" ") { (host, outcome) -> "$host=$outcome" })
    }
}.take(900)

/** Lower-case host without port, for the DNS lookups. */
internal fun stallLookupHost(url: String?): String? =
    examHostOf(url)?.lowercase(Locale.US)

/** One stall, as recorded: shown on the stall card, kept in the stall history. */
internal data class ExamStallReport(
    val attempt: Int,
    val wallClockMs: Long,
    /** The exam's host; the student-facing summary says "server ujian" instead. */
    val examHost: String?,
    val page: ExamStallPageEvidence,
    val network: ExamStallNetworkEvidence
) {
    /** The full line for the admin: hosts, addresses and all. */
    val adminDetail: String
        get() = formatExamStallEvidence(attempt, page, network)
}

/**
 * The stall log as a student or proctor may see it: what hung and how the network looks,
 * without the exam server's name or address, which students are never shown.
 */
internal fun buildExamStallSummaryLines(
    report: ExamStallReport,
    language: com.coblax.examlock.model.UiLanguage,
    clockText: String
): List<String> {
    fun t(english: String, indonesian: String) = com.coblax.examlock.i18n.localized(language, english, indonesian)
    val examHost = report.examHost?.lowercase(Locale.US)
    fun hostLabel(host: String): String =
        if (examHost != null && host.substringBefore(':').lowercase(Locale.US) == examHost) {
            t("exam server", "server ujian")
        } else {
            host
        }
    fun yesNo(value: Boolean?) = when (value) {
        true -> t("yes", "ya")
        false -> t("no", "tidak")
        null -> "-"
    }

    val hung = when {
        report.page.probeError != null || report.page.pendingCount == 0 ->
            t("the main page from the exam server never arrived", "halaman utama dari server ujian belum diterima")
        else -> {
            val grouped = report.page.pending
                .map { entry -> hostLabel(entry.substringBefore(' ')) to entry.substringAfter(' ', "").uppercase(Locale.US) }
                .groupBy({ it }, { it })
                .map { (key, items) -> "${items.size} ${key.second.ifBlank { t("file", "file") }} ${t("from", "dari")} ${key.first}" }
            val listed = grouped.joinToString(", ")
            val more = report.page.pendingCount - report.page.pending.size
            if (more > 0) "$listed, +$more" else listed
        }
    }
    val transport = when (report.network.transport) {
        "wifi" -> "Wi-Fi"
        "cellular" -> t("Mobile data", "Data seluler")
        "ethernet" -> "Ethernet"
        "vpn" -> "VPN"
        "none" -> t("No network", "Tidak ada jaringan")
        else -> report.network.transport
    }
    val validated = when (report.network.validated) {
        true -> t("internet checked", "internet terverifikasi")
        false -> t("internet not verified", "internet belum terverifikasi")
        null -> null
    }
    val examLookup = report.network.lookups.firstOrNull { (host, _) -> host.equals(examHost, ignoreCase = true) }?.second
    val examDns = when {
        examLookup == null -> "-"
        examLookup == "timeout" -> t("no answer (timeout)", "tidak dijawab (timeout)")
        examLookup.any { it.isDigit() } && (examLookup.contains('.') || examLookup.contains(':')) -> {
            val addresses = examLookup.split(',')
            val v4 = addresses.any { it.contains('.') }
            val v6 = addresses.any { it.contains(':') }
            t("answered", "terjawab") + " (" + listOfNotNull("IPv4".takeIf { v4 }, "IPv6".takeIf { v6 }).joinToString("+") + ")"
        }
        else -> t("failed", "gagal") + ": $examLookup"
    }
    val privateDns = when {
        report.network.privateDns == "off" -> t("off", "mati")
        report.network.privateDns == "auto" -> t("automatic", "otomatis")
        report.network.privateDns.startsWith("strict:") -> t("custom server", "server khusus")
        else -> report.network.privateDns
    }
    return listOf(
        t("Stall #${report.attempt}", "Macet ke-${report.attempt}") + " · $clockText",
        t("Hung: ", "Menggantung: ") + hung,
        t("Network: ", "Jaringan: ") + listOfNotNull(transport, validated).joinToString(", "),
        t("Exam server DNS: ", "DNS server ujian: ") + examDns,
        "Private DNS: $privateDns · IPv6: ${yesNo(report.network.ipv6)}"
    )
}
