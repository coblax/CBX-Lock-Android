package com.example.coblaxexamlock.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class CachedDetectorValueTest {
    @Test
    fun readWithinTtlUsesCachedValue() {
        var now = 1_000L
        var calls = 0
        val cache = CachedDetectorValue<Int>(
            ttlMillis = 500L,
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
            ttlMillis = 500L,
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
            ttlMillis = 500L,
            nowMillis = { now }
        )

        assertEquals(1, cache.read { ++calls })
        now += 501L
        assertEquals(2, cache.read { ++calls })
        assertEquals(2, calls)
    }
}
