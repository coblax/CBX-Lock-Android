package com.coblax.examlock.ui.exam

import android.os.SystemClock
import com.coblax.examlock.IntegrityCheckResult
import com.coblax.examlock.ReverseEngineeringResult

private const val RuntimeSecurityRefreshCacheTtlMillis = 1_500L

internal data class RuntimeReverseEngineeringRefreshCache(
    val result: ReverseEngineeringResult,
    val capturedAtElapsedMs: Long
)

internal data class RuntimeIntegrityRefreshCache(
    val result: IntegrityCheckResult,
    val baselineFingerprint: String?,
    val capturedAtElapsedMs: Long
)

internal fun RuntimeReverseEngineeringRefreshCache.isFresh(
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return (nowElapsedMs - capturedAtElapsedMs).coerceAtLeast(0L) <= RuntimeSecurityRefreshCacheTtlMillis
}

internal fun RuntimeIntegrityRefreshCache.isFreshFor(
    baselineFingerprint: String?,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return baselineFingerprint == this.baselineFingerprint &&
        (nowElapsedMs - capturedAtElapsedMs).coerceAtLeast(0L) <= RuntimeSecurityRefreshCacheTtlMillis
}
