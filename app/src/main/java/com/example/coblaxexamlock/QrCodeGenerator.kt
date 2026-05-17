package com.example.coblaxexamlock

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel


object QrCodeGenerator {
    fun generateBitmap(content: String, size: Int = 960): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        return bitmap
    }
}
