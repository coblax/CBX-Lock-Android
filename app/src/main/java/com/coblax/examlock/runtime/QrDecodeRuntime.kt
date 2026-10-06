package com.coblax.examlock.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.resolveLowRamProfile
import com.coblax.examlock.config.QrImageReadErrorOpen
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.InputStream
import java.nio.charset.StandardCharsets
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

/**
 * The second pass reads likely QR regions of the original picture. Each region is read
 * on its own at up to this many pixels a side, so even an Ultra phone sees a dense QR
 * at about three pixels per module instead of the one it got from the shrunken whole.
 * Pixels are held as one luminance byte each, so this costs about 1.6 MB plus the
 * region's bitmap.
 */
private const val NormalRegionMaxEdgePx = 2560
private const val LowRegionMaxEdgePx = 1600
private const val UltraRegionMaxEdgePx = 1280

/** A region read smaller than this cannot hold a readable QR. */
private const val MinRegionSidePx = 360

/** Fractions of the shorter side for the centered squares tried in the second pass. */
private val regionSquareFractions = listOf(0.8f, 0.55f, 0.38f)

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

internal fun qrRegionMaxEdgePx(lowRamProfile: LowRamProfile): Int = when {
    lowRamProfile.ultra -> UltraRegionMaxEdgePx
    lowRamProfile.enabled -> LowRegionMaxEdgePx
    else -> NormalRegionMaxEdgePx
}

private fun newQrDecodeReader(): MultiFormatReader {
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
    )
    return MultiFormatReader().apply { setHints(hints) }
}

/**
 * Grey levels of the bitmap, one byte a pixel and read a row at a time: a quarter of
 * the IntArray the decoder used to copy the whole picture into.
 */
private fun Bitmap.toLuminanceSource(): LuminanceSource {
    val bitmapWidth = width
    val bitmapHeight = height
    val luminance = ByteArray(bitmapWidth * bitmapHeight)
    val row = IntArray(bitmapWidth)
    for (y in 0 until bitmapHeight) {
        getPixels(row, 0, bitmapWidth, 0, y, bitmapWidth, 1)
        val offset = y * bitmapWidth
        for (x in 0 until bitmapWidth) {
            val pixel = row[x]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            // Same weighting as ZXing's RGBLuminanceSource.
            luminance[offset + x] = ((red + 2 * green + blue) / 4).toByte()
        }
    }
    return PlanarYUVLuminanceSource(luminance, bitmapWidth, bitmapHeight, 0, 0, bitmapWidth, bitmapHeight, false)
}

/**
 * [MultiFormatReader.decode] clears the reader's hints and tries every barcode format;
 * a poster's text then read as an 8-digit EAN code now and then, which the student saw
 * as "not a CBX Lock QR". decodeWithState keeps QR-only and TRY_HARDER.
 */
private fun decodeQrPayloadFromSource(source: LuminanceSource, reader: MultiFormatReader): String? {
    reader.reset()
    val normalResult = runCatching {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull()
    if (normalResult != null) {
        reader.reset()
        return normalResult
    }

    // The local-threshold binarizer loses module edges a chat app's JPEG blurred; one
    // threshold for the whole crop often still separates them.
    reader.reset()
    val globalResult = runCatching {
        reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source))).text
    }.getOrNull()
    if (globalResult != null) {
        reader.reset()
        return globalResult
    }

    reader.reset()
    val invertedResult = runCatching {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert()))).text
    }.getOrNull()
    reader.reset()
    return invertedResult
}

/**
 * Reads [source] as nothing but the QR and its white margin. It skips the finder-pattern
 * search, which is what a chat app's JPEG blur most often defeats, so it only suits a
 * crop known to hold just the code: the QR box of a CBX poster.
 */
private fun decodePureQrPayload(source: LuminanceSource): String? {
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.PURE_BARCODE to true,
        DecodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
    )
    val reader = QRCodeReader()
    listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source)).forEach { binarizer ->
        runCatching { reader.decode(BinaryBitmap(binarizer), hints).text }.getOrNull()?.let { return it }
        reader.reset()
    }
    return null
}

/** The poster's QR box holds only the code, so read it both ways before anything else. */
private fun decodePosterQrPayload(source: LuminanceSource, reader: MultiFormatReader): String? =
    decodePureQrPayload(source) ?: decodeQrPayloadFromSource(source, reader)

private fun buildQrDecodeFallbackRects(width: Int, height: Int): List<Rect> {
    val rects = linkedSetOf<Rect>()
    val minDimension = minOf(width, height)
    posterQrRect(width, height)?.let { rects += it }

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

/**
 * Regions of the original picture where a QR usually is: the QR band of an exported
 * poster (tall pictures), then ever tighter squares around the middle, the way people
 * photograph a printed QR.
 */
internal fun buildQrDecodeRegionRects(width: Int, height: Int): List<Rect> {
    val rects = linkedSetOf<Rect>()
    posterQrRect(width, height)?.let { rects += it }
    if (height > width * 1.3) {
        rects += Rect((width * 0.08).toInt(), (height * 0.18).toInt(), (width * 0.92).toInt(), (height * 0.72).toInt())
    }
    val minDimension = minOf(width, height)
    regionSquareFractions.forEach { fraction ->
        val size = (minDimension * fraction).toInt()
        if (size >= 96) {
            val left = (width - size) / 2
            val top = (height - size) / 2
            rects += Rect(left, top, left + size, top + size)
        }
    }
    return rects.toList()
}

/**
 * Where the QR's white box sits on a CBX poster (ExamQrExportHelper: 1440×2120 with the
 * 840 px box at 300,560), whatever size the poster was saved or forwarded at. Reading
 * only that box keeps the card border and the text around it from distracting the
 * finder-pattern search.
 */
internal fun posterQrRect(width: Int, height: Int): Rect? {
    val aspect = height.toFloat() / width
    if (aspect !in PosterAspectRange) {
        return null
    }
    val scale = width / PosterLogicalWidth
    val left = (PosterQrBoxLeft * scale).toInt()
    val top = (PosterQrBoxTop * scale).toInt()
    val size = (PosterQrBoxSize * scale).toInt()
    if (size < MinRegionSidePx / 2) {
        return null
    }
    return Rect(left, top, (left + size).coerceAtMost(width), (top + size).coerceAtMost(height))
}

/** The inside of the poster's QR box: the code and its white margin, no box border. */
internal fun posterQrInnerRect(width: Int, height: Int): Rect? {
    val box = posterQrRect(width, height) ?: return null
    val inset = (PosterQrBoxInset * width / PosterLogicalWidth).toInt()
    return Rect(box.left + inset, box.top + inset, box.right - inset, box.bottom - inset)
}

private const val PosterLogicalWidth = 1440f
private const val PosterQrBoxInset = 20f
private const val PosterQrBoxLeft = 300f
private const val PosterQrBoxTop = 560f
private const val PosterQrBoxSize = 840f
private val PosterAspectRange = 1.42f..1.52f

internal fun decodeQrPayloadFromBitmap(bitmap: Bitmap): String? {
    return decodeQrPayloadFromBitmap(
        bitmap = bitmap,
        preferFallbackRegionsFirst = false,
        skipFullBitmapScanAfterFallback = false
    )
}

internal fun shouldSkipQrFullBitmapScanAfterFallback(
    lowRamProfile: LowRamProfile,
    width: Int,
    height: Int
): Boolean = lowRamProfile.ultra && height > width * 1.3f

internal fun decodeQrPayloadFromBitmap(
    bitmap: Bitmap,
    lowRamProfile: LowRamProfile
): String? {
    return decodeQrPayloadFromBitmap(
        bitmap = bitmap,
        preferFallbackRegionsFirst = lowRamProfile.severe,
        skipFullBitmapScanAfterFallback = shouldSkipQrFullBitmapScanAfterFallback(
            lowRamProfile = lowRamProfile,
            width = bitmap.width,
            height = bitmap.height
        )
    )
}

private fun decodeQrPayloadFromBitmap(
    bitmap: Bitmap,
    preferFallbackRegionsFirst: Boolean,
    skipFullBitmapScanAfterFallback: Boolean
): String? {
    val width = bitmap.width
    val height = bitmap.height
    val reader = newQrDecodeReader()
    val source = bitmap.toLuminanceSource()
    posterQrInnerRect(width, height)?.let { inner ->
        runCatching { source.crop(inner.left, inner.top, inner.width(), inner.height()) }.getOrNull()
            ?.let { crop -> decodePosterQrPayload(crop, reader) }
            ?.let { return it }
    }

    // Heuristic: if the image is tall (like an export card), try crop regions first
    // because the QR is embedded in a decorative layout and full-image decode often fails
    val isTallImage = height > width * 1.3
    val shouldPreferFallback = preferFallbackRegionsFirst || isTallImage

    if (shouldPreferFallback) {
        decodeQrPayloadFromFallbackRegions(source, width, height, reader)?.let { return it }
        if (skipFullBitmapScanAfterFallback) {
            return null
        }
    }

    decodeQrPayloadFromSource(source, reader)?.let { return it }

    if (!shouldPreferFallback) {
        decodeQrPayloadFromFallbackRegions(source, width, height, reader)?.let { return it }
    }

    return null
}

private fun decodeQrPayloadFromFallbackRegions(
    source: LuminanceSource,
    width: Int,
    height: Int,
    reader: MultiFormatReader
): String? {
    buildQrDecodeFallbackRects(width, height).forEach { cropRect ->
        val crop = runCatching {
            source.crop(cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        }.getOrNull() ?: return@forEach
        decodeQrPayloadFromSource(crop, reader)?.let { return it }
    }
    return null
}

internal suspend fun decodeQrPayloadFromImageUri(
    context: Context,
    uri: Uri,
    lowRamProfile: LowRamProfile = resolveLowRamProfile(context)
): String? = withContext(LowRamDispatchers.detectorIo) {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsDecoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
        true
    } ?: false

    if (!boundsDecoded || boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        throw IllegalStateException(QrImageReadErrorOpen)
    }
    val imageWidth = boundsOptions.outWidth
    val imageHeight = boundsOptions.outHeight
    val bitmapConfig = qrDecodePreferredBitmapConfig(lowRamProfile)

    // Pass 1: the whole picture, shrunk to the profile's size.
    val bitmapOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateBitmapSampleSize(
            width = imageWidth,
            height = imageHeight,
            maxWidth = lowRamProfile.qrMaxEdgePx,
            maxHeight = lowRamProfile.qrMaxEdgePx
        )
        inPreferredConfig = bitmapConfig
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bitmapOptions)
    } ?: throw IllegalStateException(QrImageReadErrorOpen)

    val wholePicture = try {
        decodeQrPayloadFromBitmap(bitmap = bitmap, lowRamProfile = lowRamProfile)
    } finally {
        bitmap.recycle()
    }
    if (wholePicture != null) {
        return@withContext wholePicture
    }

    // Pass 2: a dense QR shrunk with the whole picture has too few pixels per module
    // (a Low phone could not read its own saved poster, nobody could read a photo), so
    // read the likely QR regions closer to their original size, one at a time.
    decodeQrPayloadFromImageRegions(
        context = context,
        uri = uri,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        regionMaxEdgePx = qrRegionMaxEdgePx(lowRamProfile),
        bitmapConfig = bitmapConfig
    )
}

private fun decodeQrPayloadFromImageRegions(
    context: Context,
    uri: Uri,
    imageWidth: Int,
    imageHeight: Int,
    regionMaxEdgePx: Int,
    bitmapConfig: Bitmap.Config
): String? {
    val stream = context.contentResolver.openInputStream(uri) ?: return null
    val regionDecoder = stream.use { input ->
        runCatching { newRegionDecoder(input) }.getOrNull()
    } ?: return null
    val reader = newQrDecodeReader()
    try {
        posterQrInnerRect(imageWidth, imageHeight)?.let { inner ->
            val sample = calculateBitmapSampleSize(inner.width(), inner.height(), regionMaxEdgePx, regionMaxEdgePx)
            // Full colour depth for this small box: RGB_565 bands the grey of a dense QR a
            // chat app has blurred, and that was enough to lose it on Low and Ultra phones.
            decodeQrPayloadFromRegion(regionDecoder, inner, sample, Bitmap.Config.ARGB_8888) {
                decodePosterQrPayload(it, reader)
            }?.let { return it }
        }
        buildQrDecodeRegionRects(imageWidth, imageHeight).forEach { region ->
            val sampleSize = calculateBitmapSampleSize(
                width = region.width(),
                height = region.height(),
                maxWidth = regionMaxEdgePx,
                maxHeight = regionMaxEdgePx
            )
            // A picture a chat app blurred and recompressed often reads better at half
            // size, where the blur shrinks to under a pixel; so try that too.
            listOf(sampleSize, sampleSize * 2)
                .filter { sample -> minOf(region.width(), region.height()) / sample >= MinRegionSidePx }
                .forEach { sample ->
                    decodeQrPayloadFromRegion(regionDecoder, region, sample, bitmapConfig) { source ->
                        decodeQrPayloadFromSource(source, reader)
                    }?.let { return it }
                }
        }
    } finally {
        regionDecoder.recycle()
    }
    return null
}

private fun decodeQrPayloadFromRegion(
    regionDecoder: BitmapRegionDecoder,
    region: Rect,
    sampleSize: Int,
    bitmapConfig: Bitmap.Config,
    decode: (LuminanceSource) -> String?
): String? {
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = bitmapConfig
    }
    val regionBitmap = runCatching { regionDecoder.decodeRegion(region, options) }.getOrNull() ?: return null
    return try {
        decode(regionBitmap.toLuminanceSource())
    } finally {
        regionBitmap.recycle()
    }
}

@Suppress("DEPRECATION")
private fun newRegionDecoder(input: InputStream): BitmapRegionDecoder? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(input)
    } else {
        BitmapRegionDecoder.newInstance(input, false)
    }
