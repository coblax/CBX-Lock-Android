package com.coblax.examlock.runtime

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedDetectorValueTest {
    @Test
    fun readWithinTtlUsesCachedValue() {
        var now = 1_000L
        var calls = 0
        val cache = CachedDetectorValue<Int>(
            baseTtlMillis = 500L,
            nowMillis = { now }
        )

        assertEquals(1, cache.read { ++calls })
        now += 100L
        assertEquals(1, cache.read { ++calls })
        assertEquals(1, calls)
    }

    @Test
    fun forceRefreshAlwaysReloadsValue() {
        var calls = 0
        val cache = CachedDetectorValue<Int>(
            baseTtlMillis = 500L,
            nowMillis = { 1_000L }
        )

        assertEquals(1, cache.read { ++calls })
        assertEquals(2, cache.read(forceRefresh = true) { ++calls })
        assertEquals(2, calls)
    }

    @Test
    fun readAfterTtlExpiresReloadsValue() {
        var now = 1_000L
        var calls = 0
        val cache = CachedDetectorValue<Int>(
            baseTtlMillis = 500L,
            nowMillis = { now }
        )

        assertEquals(1, cache.read { ++calls })
        now += 501L
        assertEquals(2, cache.read { ++calls })
        assertEquals(2, calls)
    }

    @Test
    fun invalidateClearsCachedValue() {
        var calls = 0
        val cache = CachedDetectorValue<Int>(
            baseTtlMillis = 500L,
            nowMillis = { 1_000L }
        )

        assertEquals(1, cache.read { ++calls })
        cache.invalidate()
        assertEquals(2, cache.read { ++calls })
        assertEquals(2, calls)
    }

    @Test
    fun cachedDetectorMapCachesByKeyAndReloadsOnForceRefresh() {
        var now = 1_000L
        var calls = 0
        val cache = CachedDetectorMap<String, Int>(
            baseTtlMillis = 500L,
            nowMillis = { now }
        )

        assertEquals(1, cache.read("a") { ++calls })
        assertEquals(1, cache.read("a") { ++calls })
        assertEquals(2, cache.read("b") { ++calls })
        assertEquals(3, cache.read("a", forceRefresh = true) { ++calls })
        now += 501L
        assertEquals(4, cache.read("a") { ++calls })
        assertEquals(4, calls)
    }

    @Test
    fun cachedDetectorMapInvalidateClearsAllKeys() {
        var calls = 0
        val cache = CachedDetectorMap<String, Int>(
            baseTtlMillis = 500L,
            nowMillis = { 1_000L }
        )

        assertEquals(1, cache.read("a") { ++calls })
        assertEquals(2, cache.read("b") { ++calls })
        cache.invalidate()
        assertEquals(3, cache.read("a") { ++calls })
        assertEquals(4, cache.read("b") { ++calls })
        assertEquals(4, calls)
    }

    @Test
    fun cachedDetectorMapEvictsLeastRecentlyUsedEntryOverBudget() {
        var calls = 0
        val cache = CachedDetectorMap<String, Int>(
            baseTtlMillis = 500L,
            maxEntries = { 2 },
            nowMillis = { 1_000L }
        )

        assertEquals(1, cache.read("a") { ++calls })
        assertEquals(2, cache.read("b") { ++calls })
        assertEquals(1, cache.read("a") { ++calls })
        assertEquals(3, cache.read("c") { ++calls })
        assertEquals(4, cache.read("b") { ++calls })
        assertEquals(4, calls)
    }

    @Test
    fun cachedDetectorMapCachesMissingMetadataNull() {
        var calls = 0
        val cache = CachedDetectorMap<String, InstalledPackageMetadata?>(
            baseTtlMillis = 500L,
            nowMillis = { 1_000L }
        )

        assertNull(cache.read("missing") { calls++; null })
        assertNull(cache.read("missing") { calls++; null })
        assertEquals(1, calls)
    }

    @Test
    fun packageMetadataDisplayFlagHonorsLowRamDefaultOnlyWhenCallerDoesNotDecide() {
        assertFalse(
            resolvePackageMetadataDisplayFlag(
                includeDisplayMetadata = null,
                skipDisplayMetadataDefault = true
            )
        )
        assertTrue(
            resolvePackageMetadataDisplayFlag(
                includeDisplayMetadata = null,
                skipDisplayMetadataDefault = false
            )
        )
        assertTrue(
            resolvePackageMetadataDisplayFlag(
                includeDisplayMetadata = true,
                skipDisplayMetadataDefault = true
            )
        )
        assertFalse(
            resolvePackageMetadataDisplayFlag(
                includeDisplayMetadata = false,
                skipDisplayMetadataDefault = false
            )
        )
    }

    @Test
    fun packageInventoryChangeHandlerInvalidatesOnlyForPackageActions() {
        var invalidations = 0

        assertTrue(
            handlePackageInventoryChange(Intent.ACTION_PACKAGE_ADDED) {
                invalidations++
            }
        )
        assertTrue(
            handlePackageInventoryChange(Intent.ACTION_PACKAGE_REMOVED) {
                invalidations++
            }
        )
        assertTrue(
            handlePackageInventoryChange(Intent.ACTION_PACKAGE_CHANGED) {
                invalidations++
            }
        )
        assertTrue(
            handlePackageInventoryChange(Intent.ACTION_PACKAGE_REPLACED) {
                invalidations++
            }
        )
        assertFalse(
            handlePackageInventoryChange(Intent.ACTION_BATTERY_CHANGED) {
                invalidations++
            }
        )

        assertEquals(4, invalidations)
    }
}
