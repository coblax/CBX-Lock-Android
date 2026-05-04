package com.example.coblaxexamlock.nativebridge

import com.example.coblaxexamlock.BuildConfig

internal enum class NativeBridgeBackendMode {
    Auto,
    ForceNative,
    ForceKotlinFallback
}

internal object NativeBridgeTestControl {
    @Volatile
    private var backendMode: NativeBridgeBackendMode = NativeBridgeBackendMode.Auto

    val currentMode: NativeBridgeBackendMode
        get() = if (BuildConfig.DEBUG) backendMode else NativeBridgeBackendMode.Auto

    fun setBackendModeForTests(mode: NativeBridgeBackendMode) {
        if (BuildConfig.DEBUG) {
            backendMode = mode
        }
    }

    fun resetBackendModeForTests() {
        backendMode = NativeBridgeBackendMode.Auto
    }

    inline fun <T> withBackendMode(
        mode: NativeBridgeBackendMode,
        block: () -> T
    ): T {
        val previousMode = currentMode
        setBackendModeForTests(mode)
        return try {
            block()
        } finally {
            setBackendModeForTests(previousMode)
        }
    }
}
