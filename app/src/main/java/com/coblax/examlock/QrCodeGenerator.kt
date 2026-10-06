package com.coblax.examlock

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.QRCode


object QrCodeGenerator {
    internal val DefaultBitmapConfig: Bitmap.Config = Bitmap.Config.RGB_565

    /**
     * Whether [content] fits in one QR at our error-correction level. Past that ZXing
     * throws, which used to crash Custom QR when a polygon had too many points.
     */
    fun canEncode(content: String): Boolean = moduleCount(content) > 0

    /** The QR's side in modules, quiet zone included, or 0 when [content] does not fit. */
    fun moduleCount(content: String): Int = runCatching {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, QrMaskSelector.encodeHints(content, null)).width
    }.getOrDefault(0)

    fun generateBitmap(content: String, size: Int = 960): Bitmap {
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            QrMaskSelector.encodeHints(content, QrMaskSelector.readableMaskFor(content))
        )

        val bitmap = Bitmap.createBitmap(size, size, DefaultBitmapConfig)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val rowOffset = y * size
            for (x in 0 until size) {
                pixels[rowOffset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}

/**
 * ZXing picks the QR mask with the lowest penalty score, and that score does not model
 * its own finder-pattern detector. With a payload as dense as ours, about one code in
 * thirty then carries a stray finder-like pattern: the data is intact (PURE_BARCODE reads
 * it) but the scanner cannot locate the symbol, even in a perfect bitmap, so students
 * could not scan it. The default mask is kept whenever the scanner reads it; otherwise
 * the first mask it does read.
 */
internal object QrMaskSelector {
    private const val QuietZoneModules = 2

    /** Version 25: the densest code a phone camera or a photo of a poster still reads easily. */
    private const val MaxComfortableModules = 117
    private const val CheckModulePx = 4
    private const val OffGridModulePx = 3.6f
    private const val BlurredModulePx = 5.3f

    @Volatile
    private var cachedChoice: Pair<String, Int?>? = null

    /**
     * Null means ZXing's default mask. The choice depends only on the content, so it is
     * made once on a small rendering and reused for every size (preview, poster).
     */
    fun readableMaskFor(content: String): Int? {
        cachedChoice?.let { (cachedContent, mask) ->
            if (cachedContent == content) return mask
        }
        val candidates = listOf<Int?>(null) + (0 until QRCode.NUM_MASK_PATTERNS)
        val readable = candidates.filter { mask -> isReadableByScanner(content, mask) }
        // Prefer a mask that still reads as a phone camera or a resaved picture sees the
        // code: a little smaller, off the pixel grid and soft. Some masks pass the crisp
        // check but grow a finder-like blob once blurred. If none survives that (never
        // seen), the crisp check decides; if no mask reads at all, the default is still
        // the best-scored layout.
        val choice = readable.firstOrNull { mask -> isReadableWhenSoft(content, mask) }
            ?: readable.firstOrNull()
        cachedChoice = content to choice
        return choice
    }

    /**
     * Decodes the code as a phone usually meets it: drawn at a size where module edges
     * fall between pixels (a camera frame), and larger but blurred (a picture a chat app
     * resized and recompressed). Some masks pass the crisp check yet fail one of these.
     */
    fun isReadableWhenSoft(content: String, mask: Int?): Boolean = runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, encodeHints(content, mask))
        decodesSoftly(matrix, content, OffGridModulePx, blur = false) &&
            decodesSoftly(matrix, content, BlurredModulePx, blur = true)
    }.getOrDefault(false)

    private fun decodesSoftly(matrix: BitMatrix, content: String, modulePx: Float, blur: Boolean): Boolean {
        val modules = matrix.width
        val side = (modules * modulePx).toInt()
        // Each pixel averages a 3x3 grid of samples, as a camera or a resize would.
        val grey = IntArray(side * side)
        for (y in 0 until side) {
            for (x in 0 until side) {
                var dark = 0
                for (sub in 0 until 9) {
                    val moduleX = ((x + (sub % 3 + 0.5f) / 3f) / modulePx).toInt().coerceAtMost(modules - 1)
                    val moduleY = ((y + (sub / 3 + 0.5f) / 3f) / modulePx).toInt().coerceAtMost(modules - 1)
                    if (matrix[moduleX, moduleY]) dark++
                }
                grey[y * side + x] = 255 - dark * 255 / 9
            }
        }
        val luminance = ByteArray(side * side)
        for (y in 0 until side) {
            for (x in 0 until side) {
                if (!blur) {
                    luminance[y * side + x] = grey[y * side + x].toByte()
                    continue
                }
                var sum = 0
                var count = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until side && ny in 0 until side) {
                            sum += grey[ny * side + nx]
                            count++
                        }
                    }
                }
                luminance[y * side + x] = (sum / count).toByte()
            }
        }
        val source = PlanarYUVLuminanceSource(luminance, side, side, 0, 0, side, side, false)
        return runCatching {
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text == content
        }.getOrDefault(false)
    }

    /** Decodes like the camera scanner does: no PURE_BARCODE, no TRY_HARDER. */
    fun isReadableByScanner(content: String, mask: Int?): Boolean = runCatching {
        // 0x0 asks for the natural size: one pixel per module, quiet zone included.
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, encodeHints(content, mask))
        val side = matrix.width * CheckModulePx
        val luminance = ByteArray(side * side) { index ->
            val dark = matrix[(index % side) / CheckModulePx, (index / side) / CheckModulePx]
            if (dark) 0 else -1 // -1 is 0xFF: white
        }
        val source = PlanarYUVLuminanceSource(luminance, side, side, 0, 0, side, side, false)
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text == content
    }.getOrDefault(false)

    fun encodeHints(content: String, mask: Int?): Map<EncodeHintType, Any> = buildMap {
        put(EncodeHintType.MARGIN, QuietZoneModules)
        put(EncodeHintType.ERROR_CORRECTION, errorCorrectionFor(content))
        if (mask != null) put(EncodeHintType.QR_MASK_PATTERN, mask)
    }

    @Volatile
    private var cachedLevel: Pair<String, ErrorCorrectionLevel>? = null

    /**
     * H (up to 30% of the code may be lost) whenever that keeps the code at most
     * [MaxComfortableModules] modules a side, as for everyday exams. A denser payload steps
     * down to Q, then M. At H a 15-corner polygon QR was version 37, 165 modules a side: a
     * printed poster photographed on a desk left the reader under five pixels a module and
     * the photo often failed. M still recovers 15%, the usual level for printed and on-screen
     * codes, and every reader decodes every level, older CBX builds included.
     */
    fun errorCorrectionFor(content: String): ErrorCorrectionLevel {
        cachedLevel?.let { (cachedContent, level) ->
            if (cachedContent == content) return level
        }
        val level = listOf(ErrorCorrectionLevel.H, ErrorCorrectionLevel.Q)
            .firstOrNull { level -> symbolModules(content, level) in 1..MaxComfortableModules }
            ?: ErrorCorrectionLevel.M
        cachedLevel = content to level
        return level
    }

    private fun symbolModules(content: String, level: ErrorCorrectionLevel): Int = runCatching {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.ERROR_CORRECTION to level, EncodeHintType.MARGIN to 0)
        ).width
    }.getOrDefault(0)
}
