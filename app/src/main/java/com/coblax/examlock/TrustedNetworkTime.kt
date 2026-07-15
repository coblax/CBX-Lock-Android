package com.coblax.examlock

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TrustedNetworkTimeFreshMillis = 10 * 60 * 1000L
private const val TrustedNetworkTimeSocketTimeoutMillis = 2_000
private const val NtpPacketSize = 48
private const val NtpTransmitTimeOffset = 40
private const val NtpUnixEpochOffsetMillis = 2_208_988_800_000L
private const val NtpFractionScale = 4_294_967_296.0

internal data class TrustedNetworkTimeSnapshot(
    val unixTimeMillis: Long,
    val fetchedAtElapsedRealtimeMillis: Long,
    val roundTripMillis: Long,
    val host: String
) {
    fun currentUnixTimeMillis(nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()): Long {
        return unixTimeMillis + (nowElapsedRealtimeMillis - fetchedAtElapsedRealtimeMillis)
    }

    fun isFresh(nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()): Boolean {
        return nowElapsedRealtimeMillis - fetchedAtElapsedRealtimeMillis <= TrustedNetworkTimeFreshMillis
    }
}

internal object TrustedNetworkTimeCoordinator {
    private val hosts = listOf(
        "time.google.com",
        "time.cloudflare.com",
        "pool.ntp.org"
    )

    @Volatile
    private var cachedSnapshot: TrustedNetworkTimeSnapshot? = null

    suspend fun currentNetworkNowMillis(
        context: Context,
        forceRefresh: Boolean = false
    ): Long? {
        val cached = cachedSnapshot
        if (!forceRefresh && cached != null && cached.isFresh()) {
            return cached.currentUnixTimeMillis()
        }
        if (!canAttemptNetworkTime(context)) {
            return null
        }
        val refreshed = withContext(Dispatchers.IO) {
            fetchFirstAvailableSnapshot()
        }
        if (refreshed != null) {
            cachedSnapshot = refreshed
            return refreshed.currentUnixTimeMillis()
        }
        return null
    }

    fun clear() {
        cachedSnapshot = null
    }

    private fun canAttemptNetworkTime(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
    }

    private fun fetchFirstAvailableSnapshot(): TrustedNetworkTimeSnapshot? {
        for (host in hosts) {
            val snapshot = runCatching { fetchSnapshot(host) }.getOrNull()
            if (snapshot != null) {
                return snapshot
            }
        }
        return null
    }

    private fun fetchSnapshot(host: String): TrustedNetworkTimeSnapshot {
        val request = ByteArray(NtpPacketSize)
        request[0] = 0x1B
        writeTimestamp(request, NtpTransmitTimeOffset, System.currentTimeMillis())

        DatagramSocket().use { socket ->
            socket.soTimeout = TrustedNetworkTimeSocketTimeoutMillis
            val address = InetAddress.getByName(host)
            val sendElapsed = SystemClock.elapsedRealtime()
            socket.send(DatagramPacket(request, request.size, address, 123))

            val response = ByteArray(NtpPacketSize)
            socket.receive(DatagramPacket(response, response.size))
            val receiveElapsed = SystemClock.elapsedRealtime()

            val transmitTimeMillis = readTimestamp(response, NtpTransmitTimeOffset)
                ?: error("Invalid NTP transmit timestamp")
            val roundTripMillis = receiveElapsed - sendElapsed
            return TrustedNetworkTimeSnapshot(
                unixTimeMillis = transmitTimeMillis + (roundTripMillis / 2L),
                fetchedAtElapsedRealtimeMillis = receiveElapsed,
                roundTripMillis = roundTripMillis,
                host = host
            )
        }
    }
}

private fun readTimestamp(buffer: ByteArray, offset: Int): Long? {
    if (offset + 8 > buffer.size) {
        return null
    }
    val seconds = readUnsignedInt(buffer, offset)
    val fraction = readUnsignedInt(buffer, offset + 4)
    if (seconds == 0L && fraction == 0L) {
        return null
    }
    return ((seconds * 1000L) + ((fraction * 1000.0) / NtpFractionScale).toLong()) -
        NtpUnixEpochOffsetMillis
}

private fun writeTimestamp(buffer: ByteArray, offset: Int, unixTimeMillis: Long) {
    if (offset + 8 > buffer.size) {
        return
    }
    val ntpTimeMillis = unixTimeMillis + NtpUnixEpochOffsetMillis
    val seconds = ntpTimeMillis / 1000L
    val fraction = ((ntpTimeMillis % 1000L) * NtpFractionScale / 1000.0).toLong()
    writeUnsignedInt(buffer, offset, seconds)
    writeUnsignedInt(buffer, offset + 4, fraction)
}

private fun readUnsignedInt(buffer: ByteArray, offset: Int): Long {
    return ((buffer[offset].toLong() and 0xFFL) shl 24) or
        ((buffer[offset + 1].toLong() and 0xFFL) shl 16) or
        ((buffer[offset + 2].toLong() and 0xFFL) shl 8) or
        (buffer[offset + 3].toLong() and 0xFFL)
}

private fun writeUnsignedInt(buffer: ByteArray, offset: Int, value: Long) {
    buffer[offset] = ((value shr 24) and 0xFFL).toByte()
    buffer[offset + 1] = ((value shr 16) and 0xFFL).toByte()
    buffer[offset + 2] = ((value shr 8) and 0xFFL).toByte()
    buffer[offset + 3] = (value and 0xFFL).toByte()
}
