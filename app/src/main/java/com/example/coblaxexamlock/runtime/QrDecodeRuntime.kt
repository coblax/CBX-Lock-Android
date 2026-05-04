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
    QrDecodeCropSpec(0.14f, 0.18f, 0.72f, 0.60f),
    QrDecodeCropSpec(0.18f, 0.22f, 0.64f, 0.50f),
    QrDecodeCropSpec(0.22f, 0.26f, 0.56f, 0.42f)
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

private fun decodeQrPayloadFromPixels(
    width: Int,
    height: Int,
    pixels: IntArray
): String? {
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
    )

    val source = RGBLuminanceSource(width, height, pixels)
    val reader = MultiFormatReader().apply { setHints(hints) }
    val normalResult = runCatching {
        reader.decode(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull()
    if (normalResult != null) {
        return normalResult
    }

    reader.reset()
    return runCatching {
        reader.decode(BinaryBitmap(HybridBinarizer(source.invert()))).text
    }.getOrNull()
}

private fun buildQrDecodeFallbackRects(width: Int, height: Int): List<Rect> {
    val rects = linkedSetOf<Rect>()
    val minDimension = minOf(width, height)

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
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    decodeQrPayloadFromPixels(width, height, pixels)?.let { return it }

    buildQrDecodeFallbackRects(width, height).forEach { cropRect ->
        val cropBitmap = runCatching {
            Bitmap.createBitmap(
                bitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )
        }.getOrNull() ?: return@forEach

        try {
            val cropPixels = IntArray(cropBitmap.width * cropBitmap.height)
            cropBitmap.getPixels(
                cropPixels,
                0,
                cropBitmap.width,
                0,
                0,
                cropBitmap.width,
                cropBitmap.height
            )
            decodeQrPayloadFromPixels(cropBitmap.width, cropBitmap.height, cropPixels)?.let {
                return it
            }
        } finally {
            if (!cropBitmap.isRecycled && cropBitmap !== bitmap) {
                cropBitmap.recycle()
            }
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
        decodeQrPayloadFromBitmap(bitmap)
    } finally {
        bitmap.recycle()
    }
}
