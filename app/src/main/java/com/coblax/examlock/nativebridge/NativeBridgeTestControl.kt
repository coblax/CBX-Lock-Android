package com.coblax.examlock.nativebridge

import com.coblax.examlock.BuildConfig

internal enum class NativeBridgeBackendMode {
    Auto,
    ForceNative,
    ForceKotlinFallback
}

internal object NativeBridgeTestControl {
    @Volatile
    private var backendMode: NativeBridgeBackendMode = NativeBridgeBackendMode.Auto

    private val modeLock = Any()

    val currentMode: NativeBridgeBackendMode
        get() = if (BuildConfig.DEBUG) backendMode else NativeBridgeBackendMode.Auto

    fun setBackendModeForTests(mode: NativeBridgeBackendMode) {
        if (BuildConfig.DEBUG) {
            synchronized(modeLock) {
                backendMode = mode
            }
        }
    }

    fun resetBackendModeForTests() {
        synchronized(modeLock) {
            backendMode = NativeBridgeBackendMode.Auto
        }
    }

    fun <T> withBackendMode(
        mode: NativeBridgeBackendMode,
        block: () -> T
    ): T {
        synchronized(modeLock) {
            val previousMode = currentMode
            backendMode = if (BuildConfig.DEBUG) mode else NativeBridgeBackendMode.Auto
            return try {
                block()
            } finally {
                backendMode = if (BuildConfig.DEBUG) previousMode else NativeBridgeBackendMode.Auto
            }
        }
    }
}
