package com.example.coblaxexamlock

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

internal fun calculateQrExportBitmapSpec(
    lowRamProfile: LowRamProfile = LowRamProfile()
): QrExportBitmapSpec {
    val scale = when {
        lowRamProfile.severe -> 0.67f
        lowRamProfile.enabled -> 0.78f
        else -> 1f
    }
    val width = (BaseQrExportWidthPx * scale).toInt().coerceAtLeast(900)
    val height = (BaseQrExportHeightPx * scale).toInt().coerceAtLeast(1320)
    val qrSize = (BaseQrExportQrSizePx * scale).toInt().coerceAtLeast(500)
    return QrExportBitmapSpec(
        widthPx = width,
        heightPx = height,
        qrSizePx = qrSize,
        scale = scale,
        estimatedBitmapBytes = width * height * 4
    )
}
