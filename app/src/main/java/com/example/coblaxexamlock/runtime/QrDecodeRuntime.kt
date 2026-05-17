package com.example.coblaxexamlock.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.resolveLowRamProfile
import com.example.coblaxexamlock.config.QrImageReadErrorOpen
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class QrDecodeCropSpec(
    val leftFraction: Float,
    val topFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float
)

private val qrDecodeFallbackCropSpecs = listOf(
    // Target the QR position in the standard export card (QR is at ~26%-66% vertically, centered horizontally)
    QrDecodeCropSpec(0.10f, 0.22f, 0.80f, 0.48f),
    QrDecodeCropSpec(0.14f, 0.18f, 0.72f, 0.60f),
    QrDecodeCropSpec(0.18f, 0.22f, 0.64f, 0.50f),
    QrDecodeCropSpec(0.22f, 0.26f, 0.56f, 0.42f),
    // Tighter center crop for screenshots or cropped images
    QrDecodeCropSpec(0.08f, 0.08f, 0.84f, 0.84f)
)

internal fun calculateBitmapSampleSize(
    width: Int,
    height: Int,
    maxWidth: Int,
    maxHeight: Int
): Int {
    var sampleSize = 1
    var currentWidth = width
    var currentHeight = height
    while (currentWidth > maxWidth || currentHeight > maxHeight) {
        sampleSize *= 2
        currentWidth /= 2
        currentHeight /= 2
    }
    return sampleSize.coerceAtLeast(1)
}

internal fun qrDecodePreferredBitmapConfig(lowRamProfile: LowRamProfile): Bitmap.Config =
    if (lowRamProfile.enabled) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888

private fun newQrDecodeReader(): MultiFormatReader {
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
    )
    return MultiFormatReader().apply { setHints(hints) }
}

private fun decodeQrPayloadFromPixels(
    width: Int,
    height: Int,
    pixels: IntArray,
    reader: MultiFormatReader
): String? {
    val source = RGBLuminanceSource(width, height, pixels)
    reader.reset()
    val normalResult = runCatching {
        reader.decode(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull()
    if (normalResult != null) {
        reader.reset()
        return normalResult
    }

    reader.reset()
    val invertedResult = runCatching {
        reader.decode(BinaryBitmap(HybridBinarizer(source.invert()))).text
    }.getOrNull()
    reader.reset()
    return invertedResult
}

private fun buildQrDecodeFallbackRects(width: Int, height: Int): List<Rect> {
    val rects = linkedSetOf<Rect>()
    val minDimension = minOf(width, height)

    // For tall images (export card format), target the known QR position
    // QR is centered horizontally, positioned at ~26%-66% vertically
    if (height > width * 1.3) {
        val qrEstimatedSize = (width * 0.55).toInt()
        val qrLeft = ((width - qrEstimatedSize) / 2).coerceAtLeast(0)
        val qrTop = (height * 0.24).toInt().coerceAtLeast(0)
        val qrBottom = (height * 0.70).toInt().coerceAtMost(height)
        val qrRight = (qrLeft + qrEstimatedSize).coerceAtMost(width)
        if (qrRight - qrLeft >= 96 && qrBottom - qrTop >= 96) {
            rects += Rect(qrLeft, qrTop, qrRight, qrBottom)
        }
        // Wider crop of the same region
        val wideLeft = (width * 0.08).toInt()
        val wideRight = (width * 0.92).toInt()
        if (wideRight - wideLeft >= 96) {
            rects += Rect(wideLeft, qrTop, wideRight, qrBottom)
        }
    }

    // Center-biased square crops
    listOf(0.72f, 0.58f).forEach { sizeFraction ->
        val size = (minDimension * sizeFraction).toInt().coerceAtLeast(96)
        val left = ((width - size) / 2).coerceAtLeast(0)
        val top = ((height - size) / 2).coerceAtLeast(0)
        val boundedWidth = minOf(size, width - left)
        val boundedHeight = minOf(size, height - top)
        if (boundedWidth >= 96 && boundedHeight >= 96) {
            rects += Rect(left, top, left + boundedWidth, top + boundedHeight)
        }
    }

    qrDecodeFallbackCropSpecs.forEach { spec ->
        val left = (width * spec.leftFraction).toInt().coerceIn(0, width - 1)
        val top = (height * spec.topFraction).toInt().coerceIn(0, height - 1)
        val cropWidth = (width * spec.widthFraction).toInt().coerceAtLeast(96)
        val cropHeight = (height * spec.heightFraction).toInt().coerceAtLeast(96)
        val right = minOf(width, left + cropWidth)
        val bottom = minOf(height, top + cropHeight)
        if (right - left >= 96 && bottom - top >= 96) {
            rects += Rect(left, top, right, bottom)
        }
    }

    return rects.toList()
}

internal fun decodeQrPayloadFromBitmap(bitmap: Bitmap): String? {
    return decodeQrPayloadFromBitmap(bitmap, preferFallbackRegionsFirst = false)
}

private fun decodeQrPayloadFromBitmap(
    bitmap: Bitmap,
    preferFallbackRegionsFirst: Boolean
): String? {
    val width = bitmap.width
    val height = bitmap.height
    val reader = newQrDecodeReader()

    // Heuristic: if the image is tall (like an export card), try crop regions first
    // because the QR is embedded in a decorative layout and full-image decode often fails
    val isTallImage = height > width * 1.3
    val shouldPreferFallback = preferFallbackRegionsFirst || isTallImage

    if (shouldPreferFallback) {
        decodeQrPayloadFromFallbackRegions(bitmap, reader)?.let { return it }
    }

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    decodeQrPayloadFromPixels(width, height, pixels, reader)?.let { return it }

    if (!shouldPreferFallback) {
        decodeQrPayloadFromFallbackRegions(bitmap, reader)?.let { return it }
    }

    return null
}

private fun decodeQrPayloadFromFallbackRegions(
    bitmap: Bitmap,
    reader: MultiFormatReader
): String? {
    buildQrDecodeFallbackRects(bitmap.width, bitmap.height).forEach { cropRect ->
        val cropWidth = cropRect.width()
        val cropHeight = cropRect.height()
        val cropPixels = runCatching {
            IntArray(cropWidth * cropHeight).also { regionPixels ->
                bitmap.getPixels(
                    regionPixels,
                    0,
                    cropWidth,
                    cropRect.left,
                    cropRect.top,
                    cropWidth,
                    cropHeight
                )
            }
        }.getOrNull() ?: return@forEach

        decodeQrPayloadFromPixels(cropWidth, cropHeight, cropPixels, reader)?.let {
            return it
        }
    }

    return null
}

internal suspend fun decodeQrPayloadFromImageUri(
    context: Context,
    uri: Uri,
    lowRamProfile: LowRamProfile = resolveLowRamProfile(context)
): String? = withContext(Dispatchers.IO) {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsDecoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
        true
    } ?: false

    if (!boundsDecoded || boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        throw IllegalStateException(QrImageReadErrorOpen)
    }

    val bitmapOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateBitmapSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight,
            maxWidth = lowRamProfile.qrMaxEdgePx,
            maxHeight = lowRamProfile.qrMaxEdgePx
        )
        inPreferredConfig = qrDecodePreferredBitmapConfig(lowRamProfile)
    }

    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bitmapOptions)
    } ?: throw IllegalStateException(QrImageReadErrorOpen)

    try {
        decodeQrPayloadFromBitmap(
            bitmap = bitmap,
            preferFallbackRegionsFirst = lowRamProfile.severe
        )
    } finally {
        bitmap.recycle()
    }
}
