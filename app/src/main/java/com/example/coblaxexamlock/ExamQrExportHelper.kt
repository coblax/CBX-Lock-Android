package com.example.coblaxexamlock

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import java.io.File
import java.io.FileOutputStream


internal object ExamQrExportHelper {
    fun createShareBitmap(
        encryptedPayload: String,
        examName: String,
        startTime: String,
        endTime: String,
        locationPolicy: ExamQrLocationPolicy,
        exportSpec: QrExportBitmapSpec = calculateQrExportBitmapSpec()
    ): Bitmap {
        val width = exportSpec.widthPx
        val height = exportSpec.heightPx
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = exportSpec.scale

        val backgroundColor = Color.WHITE
        val cardColor = "#F5F7FA".toColorInt()
        val outlineColor = "#D0D7E2".toColorInt()
        val titleColor = "#1F2937".toColorInt()
        val subtitleColor = "#5B6472".toColorInt()
        val accentColor = "#4481F3".toColorInt()

        canvas.drawColor(backgroundColor)
        canvas.scale(scale, scale)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 52f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = 68f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subtitleColor
            textSize = 38f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = 46f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        canvas.drawText("COBLAX EXAM LOCK", width / 2f, 120f, titlePaint)
        canvas.drawText("QR Ujian Terenkripsi", width / 2f, 220f, headingPaint)

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subtitleColor
            textSize = 36f
        }
        drawIntroParagraph(canvas = canvas, paint = subtitlePaint)

        val cardRect = RectF(90f, 430f, width - 90f, height - 90f)
        canvas.drawRoundRect(cardRect, 34f, 34f, cardPaint)
        canvas.drawRoundRect(cardRect, 34f, 34f, borderPaint)

        val qrPadding = 40f
        val qrSize = 760f
        val qrContainerSize = qrSize + (qrPadding * 2f)
        val qrContainerLeft = (width - qrContainerSize) / 2f
        val qrContainerTop = 560f
        val qrContainer = RectF(
            qrContainerLeft,
            qrContainerTop,
            qrContainerLeft + qrContainerSize,
            qrContainerTop + qrContainerSize
        )
        canvas.drawRoundRect(qrContainer, 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(qrContainer, 30f, 30f, borderPaint)

        val qrBitmap = QrCodeGenerator.generateBitmap(encryptedPayload, size = exportSpec.qrSizePx)
        try {
            val qrRect = RectF(
                qrContainer.left + qrPadding,
                qrContainer.top + qrPadding,
                qrContainer.right - qrPadding,
                qrContainer.bottom - qrPadding
            )
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        } finally {
            if (!qrBitmap.isRecycled) {
                qrBitmap.recycle()
            }
        }

        var currentY = qrContainer.bottom + 110f
        drawDetailLine(canvas, "Nama Ujian", examName, currentY, labelPaint, valuePaint)
        currentY += 145f
        drawDetailLine(canvas, "Mulai", startTime, currentY, labelPaint, valuePaint)
        currentY += 145f
        drawDetailLine(canvas, "Selesai", endTime, currentY, labelPaint, valuePaint)
        currentY += 145f
        val geofenceValue = when (locationPolicy.shapeType) {
            GeofenceShapeType.Circle -> {
                val primaryCenter = locationPolicy.effectiveCircleCenters.firstOrNull()
                "Circle | ${locationPolicy.effectiveCircleCenters.size} centers | ${locationPolicy.radiusMeters} m | ${
                    primaryCenter?.let { "${it.latitude}, ${it.longitude}" } ?: "-"
                }"
            }
            GeofenceShapeType.Polygon ->
                "Polygon | ${locationPolicy.vertices.size} points"
            GeofenceShapeType.Disabled -> "Disabled"
        }
        drawDetailLine(canvas, "Geofence", geofenceValue, currentY, labelPaint, valuePaint)

        return bitmap
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, examName: String): String {
        val displayName = buildFileName(examName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/COBLAX EXAM LOCK"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("Tidak bisa membuat file galeri.")

            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } ?: error("Tidak bisa menulis file gambar.")

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            displayName
        } else {
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: error("Folder gambar tidak tersedia.")
            val exportDir = File(picturesDir, "COBLAX EXAM LOCK").apply { mkdirs() }
            val file = File(exportDir, displayName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
            file.name
        }
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, examName: String) {
        val shareDir = File(context.cacheDir, "shared_qr").apply {
            mkdirs()
            cleanupOldExportFiles(this)
        }
        val shareFile = File(shareDir, buildFileName(examName))
        FileOutputStream(shareFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "QR Ujian $examName")
            putExtra(Intent.EXTRA_TEXT, "QR ujian terenkripsi: $examName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Bagikan QR Ujian")
        if (!launchPlatformIntentSafely(context, chooserIntent)) {
            throw IllegalStateException("Tidak ada aplikasi yang bisa membagikan QR.")
        }
    }

    private fun buildFileName(examName: String): String {
        val safeName = examName
            .trim()
            .ifBlank { "ujian" }
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "ujian" }
        return "COBLAX_QR_${safeName}_${System.currentTimeMillis()}.png"
    }

    private fun cleanupOldExportFiles(directory: File) {
        val now = System.currentTimeMillis()
        directory.listFiles()?.forEach { file ->
            val tooOld = now - file.lastModified() > 24L * 60L * 60L * 1000L
            if (tooOld) {
                runCatching { file.delete() }
            }
        }
        val files = directory.listFiles()
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        files.drop(4).forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun drawDetailLine(
        canvas: Canvas,
        label: String,
        value: String,
        startY: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        val startX = 180f
        canvas.drawText(label, startX, startY, labelPaint)
        canvas.drawText(value.ifBlank { "-" }, startX, startY + 62f, valuePaint)
    }

    private fun drawIntroParagraph(
        canvas: Canvas,
        paint: TextPaint
    ) {
        val text =
            "Bagikan atau simpan QR ini. Aplikasi COBLAX EXAM LOCK akan membaca dan mendekripsi data ini saat dipindai."
        val width = 1240
        val startY = 280f
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()

        canvas.withTranslation(((canvas.width - width) / 2f), startY) {
            layout.draw(this)
        }
    }
}
