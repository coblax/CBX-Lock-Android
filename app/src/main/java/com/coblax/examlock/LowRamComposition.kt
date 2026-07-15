package com.coblax.examlock

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalLowRamProfile = staticCompositionLocalOf { LowRamProfile() }
internal val LocalDeviceCompatibilityProfile = staticCompositionLocalOf { DeviceCompatibilityProfile() }
