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
    @Volatile private var loadedAtMillis: Long = Long.MIN_VALUE
    @Volatile private var loaded = false
    @Volatile private var cachedValue: T? = null

    fun read(forceRefresh: Boolean = false, loader: () -> T): T {
        if (!forceRefresh) {
            synchronized(lock) {
                val now = nowMillis()
                if (loaded && now - loadedAtMillis < ttlMillis) {
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
    private val ttlMillis: Long,
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private data class Entry<V>(
        val loadedAtMillis: Long,
        val value: V
    )

    private val lock = Any()
    private val cachedValues = mutableMapOf<K, Entry<V>>()

    fun read(key: K, forceRefresh: Boolean = false, loader: () -> V): V {
        if (!forceRefresh) {
            synchronized(lock) {
                val now = nowMillis()
                val cached = cachedValues[key]
                if (cached != null && now - cached.loadedAtMillis < ttlMillis) {
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
        }
        return value
    }

    fun invalidate() {
        synchronized(lock) {
            cachedValues.clear()
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
    private val packageInventory = CachedDetectorValue<InstalledPackageInventory>(SecurityDetectorCacheTtlMillis)
    private val packageMetadata =
        CachedDetectorMap<String, InstalledPackageMetadata?>(SecurityDetectorCacheTtlMillis)
    private val screenRecorderPackages = CachedDetectorValue<List<String>>(SecurityDetectorCacheTtlMillis)
    private val screenRecorderReports = CachedDetectorValue<List<ScreenRecorderAppReport>>(SecurityDetectorCacheTtlMillis)
    private val fakeLocationPackages = CachedDetectorValue<List<String>>(SecurityDetectorCacheTtlMillis)
    private val externalDisplaySnapshot = CachedDetectorValue<ExternalDisplaySnapshot>(SecurityDetectorCacheTtlMillis)
    private val webViewCompatibility = CachedDetectorValue<WebViewCompatibilityStatus>(SecurityDetectorCacheTtlMillis)
    private val rootDetectionDetails = CachedDetectorValue<RootDetectionDetails>(SecurityDetectorCacheTtlMillis)
    private val signatureIntegrity = CachedDetectorValue<SignatureIntegrityCacheEntry>(SecurityDetectorCacheTtlMillis)

    fun readPackageInventory(context: Context, forceRefresh: Boolean = false): InstalledPackageInventory {
        return packageInventory.read(forceRefresh) {
            readInstalledPackageInventory(context.applicationContext)
        }
    }

    fun readPackageMetadata(
        context: Context,
        packageName: String,
        forceRefresh: Boolean = false,
        packageInventory: InstalledPackageInventory? = null
    ): InstalledPackageMetadata? {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) {
            return null
        }
        return packageMetadata.read(normalizedPackageName, forceRefresh) {
            val appContext = context.applicationContext
            resolveInstalledPackageMetadata(
                context = appContext,
                packageName = normalizedPackageName,
                packageInventory = packageInventory ?: readPackageInventory(appContext, forceRefresh),
                includeDisplayMetadata = true
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
                        packageInventory = inventory
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
                        packageInventory = inventory
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
                        packageInventory = inventory
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
}
