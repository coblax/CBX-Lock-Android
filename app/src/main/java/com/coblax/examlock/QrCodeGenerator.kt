package com.coblax.examlock

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.QRCode


object QrCodeGenerator {
    internal val DefaultBitmapConfig: Bitmap.Config = Bitmap.Config.RGB_565

    fun generateBitmap(content: String, size: Int = 960): Bitmap {
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            QrMaskSelector.encodeHints(QrMaskSelector.readableMaskFor(content))
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
    private const val CheckModulePx = 4

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
        // If no mask reads (never seen), the default is still the best-scored layout.
        val choice = candidates.firstOrNull { mask -> isReadableByScanner(content, mask) }
        cachedChoice = content to choice
        return choice
    }

    /** Decodes like the camera scanner does: no PURE_BARCODE, no TRY_HARDER. */
    fun isReadableByScanner(content: String, mask: Int?): Boolean = runCatching {
        // 0x0 asks for the natural size: one pixel per module, quiet zone included.
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, encodeHints(mask))
        val side = matrix.width * CheckModulePx
        val luminance = ByteArray(side * side) { index ->
            val dark = matrix[(index % side) / CheckModulePx, (index / side) / CheckModulePx]
            if (dark) 0 else -1 // -1 is 0xFF: white
        }
        val source = PlanarYUVLuminanceSource(luminance, side, side, 0, 0, side, side, false)
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text == content
    }.getOrDefault(false)

    fun encodeHints(mask: Int?): Map<EncodeHintType, Any> = buildMap {
        put(EncodeHintType.MARGIN, QuietZoneModules)
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        if (mask != null) put(EncodeHintType.QR_MASK_PATTERN, mask)
    }
}
