package com.coblax.examlock

import kotlin.math.roundToInt

internal data class QrExportBitmapSpec(
    val widthPx: Int,
    val heightPx: Int,
    val qrSizePx: Int,
    val scale: Float,
    val estimatedBitmapBytes: Int
)

private const val BaseQrExportWidthPx = 1440
private const val BaseQrExportHeightPx = 2120
private const val BaseQrExportQrSizePx = 760
private const val MinExportQrSizePx = 500

/**
 * Below about five pixels a module a chat app's recompression, or a photo of the
 * printout, leaves a dense QR unreadable; the low-RAM shrink must not go past that.
 */
private const val MinExportPixelsPerModule = 5

/**
 * The poster is drawn at [BaseQrExportWidthPx] logical pixels and scaled down for
 * low-RAM phones. [qrModules] is the QR's side in modules (quiet zone included); a dense
 * QR raises the scale so it keeps enough pixels per module.
 */
internal fun calculateQrExportBitmapSpec(
    lowRamProfile: LowRamProfile = LowRamProfile(),
    qrModules: Int = 0
): QrExportBitmapSpec {
    val profileScale = when {
        lowRamProfile.severe -> 0.60f
        lowRamProfile.enabled -> 0.72f
        else -> 1f
    }
    val minimumQrSize = maxOf(MinExportQrSizePx, qrModules * MinExportPixelsPerModule)
    val scale = maxOf(profileScale, minimumQrSize.toFloat() / BaseQrExportQrSizePx).coerceAtMost(1f)
    val width = (BaseQrExportWidthPx * scale).roundToInt()
    val height = (BaseQrExportHeightPx * scale).roundToInt()
    return QrExportBitmapSpec(
        widthPx = width,
        heightPx = height,
        qrSizePx = (BaseQrExportQrSizePx * scale).roundToInt(),
        scale = scale,
        // The poster is RGB_565: two bytes a pixel.
        estimatedBitmapBytes = width * height * 2
    )
}

