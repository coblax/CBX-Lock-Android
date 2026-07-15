package com.coblax.examlock.nativebridge

import android.util.Log

private const val NativeLoaderTag = "NativeLibraryLoader"

/**
 * Centralized native library loader. All bridges share this single loading point
 * to avoid multiple independent `System.loadLibrary` calls with separate state tracking.
 */
internal object NativeLibraryLoader {
    val loadFailure: Throwable?
    val isAvailable: Boolean

    init {
        var failure: Throwable? = null
        val available = try {
            System.loadLibrary("examlock_native")
            true
        } catch (throwable: Throwable) {
            failure = throwable
            false
        }
        loadFailure = failure
        isAvailable = available
        failure?.let {
            Log.w(NativeLoaderTag, "Native library 'examlock_native' unavailable; Kotlin fallback will be used.", it)
        }
    }
}
