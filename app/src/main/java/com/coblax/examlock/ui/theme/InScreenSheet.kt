package com.coblax.examlock.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modal bottom sheet drawn inside the current composition, not in a separate window.
 * A dialog window would take window focus from the activity (which the exam guard
 * watches), would not inherit FLAG_SECURE on every OEM, and costs a surface on
 * low-RAM phones. Back, scrim tap, and the caller's close button all dismiss it.
 *
 * The sheet is open while [value] is non-null; [content] receives that value.
 */
@Composable
internal fun <T : Any> InScreenSheetHost(
    value: T?,
    onDismiss: () -> Unit,
    closeLabel: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 640.dp,
    sheetTag: String = "in_screen_sheet",
    scrimTag: String = "in_screen_sheet_scrim",
    content: @Composable (T) -> Unit
) {
    val motionPolicy = currentUiMotionPolicy()
    // Keep the last value while the exit animation runs. A plain holder, not state:
    // it is a render cache and must not trigger another composition pass.
    val lastOpened = remember { LastOpenedValue<T>() }
    if (value != null) {
        lastOpened.value = value
    }
    val shownValue = value ?: lastOpened.value
    val visible = value != null

    if (visible) {
        // Composed only while open, so it registers after (and wins over) the screen's
        // own back handling.
        BackHandler(onBack = onDismiss)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sheetMaxHeight = maxHeight * 0.9f
        val scrim: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(scrimTag)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = closeLabel,
                        onClick = onDismiss
                    )
            )
        }
        val sheet: @Composable () -> Unit = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                shownValue?.let { shown ->
                    InScreenSheetSurface(
                        maxHeight = sheetMaxHeight,
                        maxWidth = maxWidth,
                        sheetTag = sheetTag,
                        content = { content(shown) }
                    )
                }
            }
        }
        if (motionPolicy.enabled) {
            val duration = motionPolicy.screenDurationMillis.coerceAtLeast(1)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(duration)),
                exit = fadeOut(tween(duration))
            ) {
                scrim()
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(duration)) { it } + fadeIn(tween(duration)),
                exit = slideOutVertically(tween(duration)) { it } + fadeOut(tween(duration))
            ) {
                sheet()
            }
        } else if (visible) {
            scrim()
            sheet()
        }
    }
}

@Composable
private fun InScreenSheetSurface(
    maxHeight: Dp,
    maxWidth: Dp,
    sheetTag: String,
    content: @Composable () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(topStart = tokens.radiusLarge, topEnd = tokens.radiusLarge)
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .testTag(sheetTag)
            .clip(shape)
            .background(AppColors.current.cardBg)
            .border(BorderStroke(1.dp, AppColors.current.outlineMedium), shape)
            // Swallow taps so they never fall through to the scrim behind the sheet.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.current.outline)
        )
        content()
    }
}

private class LastOpenedValue<T> {
    var value: T? = null
}

internal data class ActionColors(val container: Color, val content: Color)

/**
 * Filled primary-action colors that keep label contrast at or above 4.5:1 in both
 * themes. The brand blue is too light for white text, so light mode uses the darker
 * blue and dark mode puts dark text on the lighter blue.
 */
@Composable
internal fun primaryActionColors(): ActionColors {
    val colors = AppColors.current
    return if (colors.isDark) {
        ActionColors(container = colors.blue, content = colors.background)
    } else {
        ActionColors(container = colors.blueDark, content = colors.onDark)
    }
}
