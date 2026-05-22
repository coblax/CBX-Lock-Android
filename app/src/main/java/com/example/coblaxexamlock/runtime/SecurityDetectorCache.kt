package com.example.coblaxexamlock.runtime

import android.content.Context
import android.os.SystemClock
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.readWebViewCompatibilityStatus as readWebViewCompatibilityStatusFresh
import java.util.LinkedHashMap

internal const val SecurityDetectorCacheTtlMillis = 2_500L

internal class CachedDetectorValue<T>(
    private val baseTtlMillis: Long,
    private val ttlMultiplier: () -> Int = { 1 },
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val lock = Any()
    @Volatile private var loadedAtMillis: Long = Long.MIN_VALUE
    @Volatile private var loaded = false
    @Volatile private var cachedValue: T? = null

    fun read(forceRefresh: Boolean = false, loader: () -> T): T {
        if (!forceRefresh) {
            synchronized(lock) {
                val now = nowMillis()
                val effectiveTtl = baseTtlMillis * ttlMultiplier().coerceAtLeast(1)
                if (loaded && now - loadedAtMillis < effectiveTtl) {
                    @Suppress("UNCHECKED_CAST")
                    return cachedValue as T
                }
            }
        }
        val value = loader()
        synchronized(lock) {
            cachedValue = value
            loadedAtMillis = nowMillis()
            loaded = true
        }
        return value
    }

    fun invalidate() {
        synchronized(lock) {
            loaded = false
            cachedValue = null
            loadedAtMillis = Long.MIN_VALUE
        }
    }
}

internal class CachedDetectorMap<K, V>(
    private val baseTtlMillis: Long,
    private val ttlMultiplier: () -> Int = { 1 },
    private val maxEntries: () -> Int = { Int.MAX_VALUE },
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private data class Entry<V>(
        val loadedAtMillis: Long,
        val value: V
    )

    private val lock = Any()
    private val cachedValues = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    fun read(key: K, forceRefresh: Boolean = false, loader: () -> V): V {
        if (!forceRefresh) {
            synchronized(lock) {
                val now = nowMillis()
                val effectiveTtl = baseTtlMillis * ttlMultiplier().coerceAtLeast(1)
                val cached = cachedValues[key]
                if (cached != null && now - cached.loadedAtMillis < effectiveTtl) {
                    return cached.value
                }
            }
        }
        val value = loader()
        synchronized(lock) {
            cachedValues[key] = Entry(
                loadedAtMillis = nowMillis(),
                value = value
            )
            trimToMaxEntriesLocked()
        }
        return value
    }

    fun invalidate() {
        synchronized(lock) {
            cachedValues.clear()
        }
    }

    private fun trimToMaxEntriesLocked() {
        val limit = maxEntries().coerceAtLeast(1)
        while (cachedValues.size > limit) {
            val eldestKey = cachedValues.entries.iterator().next().key
            cachedValues.remove(eldestKey)
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
    @Volatile
    var cacheTtlMultiplier: Int = 1

    @Volatile
    var metadataCacheMaxEntries: Int = 64

    private val ttlMultiplier: () -> Int = { cacheTtlMultiplier }
    private val metadataMaxEntries: () -> Int = { metadataCacheMaxEntries }

    private val packageInventory = CachedDetectorValue<InstalledPackageInventory>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val packageMetadata =
        CachedDetectorMap<String, InstalledPackageMetadata?>(
            SecurityDetectorCacheTtlMillis,
            ttlMultiplier,
            metadataMaxEntries
        )
    private val screenRecorderPackages = CachedDetectorValue<List<String>>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val screenRecorderReports = CachedDetectorValue<List<ScreenRecorderAppReport>>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val fakeLocationPackages = CachedDetectorValue<List<String>>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val externalDisplaySnapshot = CachedDetectorValue<ExternalDisplaySnapshot>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val webViewCompatibility = CachedDetectorValue<WebViewCompatibilityStatus>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val rootDetectionDetails = CachedDetectorValue<RootDetectionDetails>(SecurityDetectorCacheTtlMillis, ttlMultiplier)
    private val signatureIntegrity = CachedDetectorValue<SignatureIntegrityCacheEntry>(SecurityDetectorCacheTtlMillis, ttlMultiplier)

    fun readPackageInventory(context: Context, forceRefresh: Boolean = false): InstalledPackageInventory {
        return packageInventory.read(forceRefresh) {
            readInstalledPackageInventory(context.applicationContext)
        }
    }

    fun readPackageMetadata(
        context: Context,
        packageName: String,
        forceRefresh: Boolean = false,
        packageInventory: InstalledPackageInventory? = null,
        includeDisplayMetadata: Boolean = true
    ): InstalledPackageMetadata? {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) {
            return null
        }
        val cacheKey = "$normalizedPackageName|display=$includeDisplayMetadata"
        return packageMetadata.read(cacheKey, forceRefresh) {
            val appContext = context.applicationContext
            resolveInstalledPackageMetadata(
                context = appContext,
                packageName = normalizedPackageName,
                packageInventory = packageInventory ?: readPackageInventory(appContext, forceRefresh),
                includeDisplayMetadata = includeDisplayMetadata
            )
        }
    }

    fun readScreenRecorderPackages(context: Context, forceRefresh: Boolean = false): List<String> {
        return screenRecorderPackages.read(forceRefresh) {
            val appContext = context.applicationContext
            val inventory = readPackageInventory(appContext, forceRefresh)
            detectScreenRecorderPackagesFromInventory(
                context = appContext,
                inventory = inventory,
                metadataResolver = { packageName ->
                    readPackageMetadata(
                        context = appContext,
                        packageName = packageName,
                        forceRefresh = forceRefresh,
                        packageInventory = inventory,
                        includeDisplayMetadata = false
                    )
                }
            )
        }
    }

    fun inspectScreenRecorderAppsCached(context: Context, forceRefresh: Boolean = false): List<ScreenRecorderAppReport> {
        return screenRecorderReports.read(forceRefresh) {
            val appContext = context.applicationContext
            val inventory = readPackageInventory(appContext, forceRefresh)
            inspectScreenRecorderAppsFromInventory(
                context = appContext,
                inventory = inventory,
                metadataResolver = { packageName ->
                    readPackageMetadata(
                        context = appContext,
                        packageName = packageName,
                        forceRefresh = forceRefresh,
                        packageInventory = inventory,
                        includeDisplayMetadata = true
                    )
                }
            )
        }
    }

    fun readSuspiciousFakeLocationPackages(context: Context, forceRefresh: Boolean = false): List<String> {
        return fakeLocationPackages.read(forceRefresh) {
            val appContext = context.applicationContext
            val inventory = readPackageInventory(appContext, forceRefresh)
            detectSuspiciousFakeLocationPackagesFromInventory(
                context = appContext,
                inventory = inventory,
                metadataResolver = { packageName ->
                    readPackageMetadata(
                        context = appContext,
                        packageName = packageName,
                        forceRefresh = forceRefresh,
                        packageInventory = inventory,
                        includeDisplayMetadata = false
                    )
                }
            )
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
            val appContext = context.applicationContext
            getRootDetectionDetails(
                context = appContext,
                packageInventory = readPackageInventory(appContext, forceRefresh)
            )
        }
    }

    fun readVirtualEnvironmentDiagnostics(
        context: Context,
        forceRefresh: Boolean = false
    ) = getVirtualEnvironmentDiagnostics(
        context = context.applicationContext,
        forceRefresh = forceRefresh
    )

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
        packageInventory.invalidate()
        packageMetadata.invalidate()
        screenRecorderPackages.invalidate()
        screenRecorderReports.invalidate()
        fakeLocationPackages.invalidate()
        externalDisplaySnapshot.invalidate()
        rootDetectionDetails.invalidate()
        invalidateVirtualEnvironmentDiagnosticsCache()
    }

    fun invalidateAll() {
        invalidateStaticSecurity()
        webViewCompatibility.invalidate()
        signatureIntegrity.invalidate()
    }
}
