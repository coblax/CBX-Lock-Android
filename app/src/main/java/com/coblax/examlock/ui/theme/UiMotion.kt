package com.coblax.examlock.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.coblax.examlock.LocalLowRamProfile

@Immutable
internal data class UiMotionPolicy(
    val enabled: Boolean,
    val pressDurationMillis: Int = PressDurationMillis,
    val contentDurationMillis: Int = ContentDurationMillis,
    val screenDurationMillis: Int = ScreenDurationMillis,
    val themeDurationMillis: Int = ThemeDurationMillis
) {
    val animationsEnabled: Boolean
        get() = enabled

    companion object {
        const val PressDurationMillis = 100
        const val ContentDurationMillis = 180
        const val ScreenDurationMillis = 220
        const val ThemeDurationMillis = 180

        val Default = UiMotionPolicy(enabled = true)
        val Reduced = UiMotionPolicy(
            enabled = false,
            pressDurationMillis = 0,
            contentDurationMillis = 0,
            screenDurationMillis = 0,
            themeDurationMillis = 0
        )
    }
}

internal fun resolveUiMotionPolicy(
    disableNonEssentialAnimations: Boolean,
    animatorDurationScale: Float
): UiMotionPolicy {
    val systemAnimationsEnabled =
        animatorDurationScale.isFinite() && animatorDurationScale > 0f
    return if (disableNonEssentialAnimations || !systemAnimationsEnabled) {
        UiMotionPolicy.Reduced
    } else {
        UiMotionPolicy.Default
    }
}

private fun readAnimatorDurationScale(
    resolver: android.content.ContentResolver
): Float = runCatching {
    Settings.Global.getFloat(
        resolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
}.getOrDefault(1f)

@Composable
private fun rememberAnimatorDurationScale(): State<Float> {
    val resolver = LocalContext.current.contentResolver
    val durationScale = remember(resolver) {
        mutableFloatStateOf(readAnimatorDurationScale(resolver))
    }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                durationScale.floatValue = readAnimatorDurationScale(resolver)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val registered = runCatching {
            resolver.registerContentObserver(uri, false, observer)
            true
        }.getOrDefault(false)
        onDispose {
            if (registered) {
                runCatching { resolver.unregisterContentObserver(observer) }
            }
        }
    }
    return durationScale
}

private val LocalUiMotionPolicy =
    staticCompositionLocalOf<UiMotionPolicy?> { null }

@Composable
internal fun rememberResolvedUiMotionPolicy(): UiMotionPolicy {
    val lowRamProfile = LocalLowRamProfile.current
    if (lowRamProfile.disableNonEssentialAnimations) {
        return UiMotionPolicy.Reduced
    }
    val animatorDurationScale by rememberAnimatorDurationScale()
    return remember(animatorDurationScale) {
        resolveUiMotionPolicy(
            disableNonEssentialAnimations = false,
            animatorDurationScale = animatorDurationScale
        )
    }
}

@Composable
internal fun currentUiMotionPolicy(): UiMotionPolicy =
    LocalUiMotionPolicy.current ?: rememberResolvedUiMotionPolicy()

@Composable
internal fun ProvideUiMotionPolicy(
    policy: UiMotionPolicy? = null,
    content: @Composable () -> Unit
) {
    val resolvedPolicy = policy ?: rememberResolvedUiMotionPolicy()
    CompositionLocalProvider(
        LocalUiMotionPolicy provides resolvedPolicy,
        content = content
    )
}
