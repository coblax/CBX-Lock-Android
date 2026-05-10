package com.example.coblaxexamlock

internal enum class VpnBypassState {
    Active,
    Inactive,
    Tampered
}

internal object VpnBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): VpnBypassState {
        return when {
            tampered -> VpnBypassState.Tampered
            enabled -> VpnBypassState.Active
            else -> VpnBypassState.Inactive
        }
    }
}
