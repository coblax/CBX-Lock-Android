package com.example.coblaxexamlock.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides coroutine dispatchers tuned by the low-RAM profile.
 *
 * On Ultra tier devices, limiting parallelism to 1 prevents burst CPU/memory
 * allocations from concurrent detector scans.
 */
internal object LowRamDispatchers {
    @Volatile
    var detectorParallelism: Int = 4

    val detectorIo: CoroutineDispatcher
        get() {
            val limit = detectorParallelism.coerceAtLeast(1)
            return if (limit >= 4) {
                Dispatchers.IO
            } else {
                Dispatchers.IO.limitedParallelism(limit)
            }
        }
}
