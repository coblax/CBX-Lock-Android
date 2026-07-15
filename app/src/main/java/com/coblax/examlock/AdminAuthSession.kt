package com.coblax.examlock

import android.os.SystemClock
import android.util.Log
import java.util.UUID

internal const val AdminAuthTokenValidDurationMillis = 30 * 60 * 1000L

private const val AdminAuthSessionTag = "AdminAuthSession"

internal data class AdminAuthCapabilityToken(
    val value: String,
    val issuedAtElapsedRealtime: Long
)

internal object AdminAuthSession {
    @Volatile
    private var activeToken: AdminAuthCapabilityToken? = null

    @Synchronized
    fun issue(): AdminAuthCapabilityToken {
        return issueAt(SystemClock.elapsedRealtime())
    }

    @Synchronized
    fun isTokenValid(): Boolean = currentTokenLocked() != null

    @Synchronized
    fun hasActiveToken(): Boolean = currentTokenLocked() != null

    @Synchronized
    fun currentToken(): AdminAuthCapabilityToken? = currentTokenLocked()

    @Synchronized
    fun clear() {
        if (activeToken != null) {
            Log.i(AdminAuthSessionTag, "Admin auth session token cleared explicitly.")
        }
        activeToken = null
    }

    @Synchronized
    fun issueForTests(issuedAtElapsedRealtime: Long = SystemClock.elapsedRealtime()) {
        issueAt(issuedAtElapsedRealtime)
    }

    private fun issueAt(issuedAtElapsedRealtime: Long): AdminAuthCapabilityToken {
        return AdminAuthCapabilityToken(
            value = UUID.randomUUID().toString(),
            issuedAtElapsedRealtime = issuedAtElapsedRealtime
        ).also { token ->
            activeToken = token
        }
    }

    private fun currentTokenLocked(
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): AdminAuthCapabilityToken? {
        val token = activeToken ?: return null
        val ageMillis = nowElapsedRealtime - token.issuedAtElapsedRealtime
        if (ageMillis > AdminAuthTokenValidDurationMillis) {
            Log.w(AdminAuthSessionTag, "Admin auth session token expired (age=${ageMillis}ms, limit=${AdminAuthTokenValidDurationMillis}ms). Admin must re-authenticate.")
            activeToken = null
            return null
        }
        return token
    }
}
