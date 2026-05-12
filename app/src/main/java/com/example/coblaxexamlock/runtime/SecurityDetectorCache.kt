package com.example.coblaxexamlock.runtime

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.readWebViewCompatibilityStatus as readWebViewCompatibilityStatusFresh

internal const val SecurityDetectorCacheTtlMillis = 2_500L

internal class CachedDetectorValue<T>(
    private val ttlMillis: Long,
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val lock = Any()
    private var loadedAtMillis: Long = Long.MIN_VALUE
    private var loaded = false
    private var cachedValue: T? = null

    fun read(forceRefresh: Boolean = false, loader: () -> T): T {
        synchronized(lock) {
            val now = nowMillis()
            if (!forceRefresh && loaded && now - loadedAtMillis < ttlMillis) {
                @Suppress("UNCHECKED_CAST")
                return cachedValue as T
            }
            return loader().also { value ->
                cachedValue = value
                loadedAtMillis = nowMillis()
                loaded = true
            }
        }
    }

    fun invalidate() {
        synchronized(lock) {
            loaded = false
            cachedValue = null
            loadedAtMillis = Long.MIN_VALUE
        }
    }
}

internal data class ExternalDisplaySnapshot(
    val count: Int,
    val infoList: List<ExternalDisplayInfo>
) {
    val detected: Boolean
        get() = count > 0
}

private data class SignatureIntegrityCacheEntry(
    val expectedKey: String,
    val result: SignatureIntegrityResult
)

internal object SecurityDetectorCache {
    private val screenRecorderPackages = CachedDetectorValue<List<String>>(SecurityDetectorCacheTtlMillis)
    private val screenRecorderReports = CachedDetectorValue<List<ScreenRecorderAppReport>>(SecurityDetectorCacheTtlMillis)
    private val externalDisplaySnapshot = CachedDetectorValue<ExternalDisplaySnapshot>(SecurityDetectorCacheTtlMillis)
    private val webViewCompatibility = CachedDetectorValue<WebViewCompatibilityStatus>(SecurityDetectorCacheTtlMillis)
    private val rootDetectionDetails = CachedDetectorValue<RootDetectionDetails>(SecurityDetectorCacheTtlMillis)
    private val signatureIntegrity = CachedDetectorValue<SignatureIntegrityCacheEntry>(SecurityDetectorCacheTtlMillis)

    fun readScreenRecorderPackages(context: Context, forceRefresh: Boolean = false): List<String> {
        return screenRecorderPackages.read(forceRefresh) {
            detectScreenRecorderPackages(context.applicationContext)
        }
    }

    fun inspectScreenRecorderAppsCached(context: Context, forceRefresh: Boolean = false): List<ScreenRecorderAppReport> {
        return screenRecorderReports.read(forceRefresh) {
            inspectScreenRecorderApps(context.applicationContext)
        }
    }

    fun readExternalDisplaySnapshot(context: Context, forceRefresh: Boolean = false): ExternalDisplaySnapshot {
        return externalDisplaySnapshot.read(forceRefresh) {
            val infoList = getExternalDisplayInfoList(context.applicationContext)
            ExternalDisplaySnapshot(
                count = infoList.size,
                infoList = infoList
            )
        }
    }

    fun readWebViewCompatibilityStatus(context: Context, forceRefresh: Boolean = false): WebViewCompatibilityStatus {
        return webViewCompatibility.read(forceRefresh) {
            readWebViewCompatibilityStatusFresh(context.applicationContext)
        }
    }

    fun readRootDetectionDetails(context: Context, forceRefresh: Boolean = false): RootDetectionDetails {
        return rootDetectionDetails.read(forceRefresh) {
            getRootDetectionDetails(context.applicationContext)
        }
    }

    fun checkSignatureIntegrity(
        context: Context,
        expectedFingerprints: List<String>,
        forceRefresh: Boolean = false
    ): SignatureIntegrityResult {
        val expectedKey = expectedFingerprints.joinToString("|")
        val entry = signatureIntegrity.read(forceRefresh) {
            SignatureIntegrityCacheEntry(
                expectedKey = expectedKey,
                result = SignatureIntegrity.check(context.applicationContext, expectedFingerprints)
            )
        }
        if (entry.expectedKey == expectedKey) {
            return entry.result
        }
        return signatureIntegrity.read(forceRefresh = true) {
            SignatureIntegrityCacheEntry(
                expectedKey = expectedKey,
                result = SignatureIntegrity.check(context.applicationContext, expectedFingerprints)
            )
        }.result
    }

    fun invalidateStaticSecurity() {
        screenRecorderPackages.invalidate()
        screenRecorderReports.invalidate()
        externalDisplaySnapshot.invalidate()
    }
}
