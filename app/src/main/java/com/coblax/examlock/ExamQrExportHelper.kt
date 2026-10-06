package com.coblax.examlock

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import java.io.File
import java.io.FileOutputStream


private const val DetailStartX = 180f
private const val DetailMinTextSize = 32f

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
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
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
        // Everything below is laid out in the unscaled poster's units. Using the scaled
        // bitmap size here pushed the whole poster left and cut the card short on
        // low-RAM phones.
        val logicalWidth = width / scale
        val logicalHeight = height / scale

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

        canvas.drawText("COBLAX EXAM LOCK", logicalWidth / 2f, 120f, titlePaint)
        canvas.drawText("QR Ujian Terenkripsi", logicalWidth / 2f, 220f, headingPaint)

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subtitleColor
            textSize = 36f
        }
        drawIntroParagraph(canvas = canvas, paint = subtitlePaint, logicalWidth = logicalWidth)

        val cardRect = RectF(90f, 430f, logicalWidth - 90f, logicalHeight - 90f)
        canvas.drawRoundRect(cardRect, 34f, 34f, cardPaint)
        canvas.drawRoundRect(cardRect, 34f, 34f, borderPaint)

        val qrPadding = 40f
        val qrSize = 760f
        val qrContainerSize = qrSize + (qrPadding * 2f)
        val qrContainerLeft = (logicalWidth - qrContainerSize) / 2f
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
            // No filtering: the bitmap is already at the drawn size, and smoothing would
            // blur module edges the scanner needs.
            canvas.drawBitmap(qrBitmap, null, qrRect, Paint().apply { isFilterBitmap = false })
        } finally {
            if (!qrBitmap.isRecycled) {
                qrBitmap.recycle()
            }
        }

        val valueMaxWidth = logicalWidth - 2 * DetailStartX
        var currentY = qrContainer.bottom + 110f
        drawDetailLine(canvas, "Nama Ujian", examName, currentY, labelPaint, valuePaint, valueMaxWidth)
        currentY += 145f
        drawDetailLine(canvas, "Mulai", startTime, currentY, labelPaint, valuePaint, valueMaxWidth)
        currentY += 145f
        drawDetailLine(canvas, "Selesai", endTime, currentY, labelPaint, valuePaint, valueMaxWidth)
        currentY += 145f
        val geofenceValue = when (locationPolicy.shapeType) {
            GeofenceShapeType.Circle ->
                "Lingkaran · ${locationPolicy.effectiveCircleCenters.size} titik pusat · radius ${locationPolicy.radiusMeters} m"
            GeofenceShapeType.Polygon ->
                "Polygon · ${locationPolicy.vertices.size} sudut"
            GeofenceShapeType.Disabled -> "Bebas (tanpa cek lokasi)"
        }
        drawDetailLine(canvas, "Lokasi", geofenceValue, currentY, labelPaint, valuePaint, valueMaxWidth)

        return bitmap
    }

    /**
     * Saves straight into Pictures/COBLAX EXAM LOCK on Android 10 and later. Older
     * versions need a storage permission for that folder, so there the admin picks the
     * place in the system save dialog ([writePng]); the app folder used before was
     * hidden from the gallery and wiped on uninstall.
     */
    val canSaveToGalleryDirectly: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun saveToGallery(context: Context, bitmap: Bitmap, examName: String): String {
        check(canSaveToGalleryDirectly) { "Direct gallery save needs Android 10." }
        val displayName = buildFileName(examName)
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
        return displayName
    }

    /** Writes the poster to a document the admin picked in the system save dialog. */
    fun writePng(context: Context, bitmap: Bitmap, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } ?: error("Tidak bisa menulis file gambar.")
    }

    fun suggestedFileName(examName: String): String = buildFileName(examName)

    /** Writes the poster where the share sheet can read it; safe off the main thread. */
    fun writeShareFile(context: Context, bitmap: Bitmap, examName: String): Uri {
        val shareDir = File(context.cacheDir, "shared_qr").apply {
            mkdirs()
            cleanupOldExportFiles(this)
        }
        val shareFile = File(shareDir, buildFileName(examName))
        FileOutputStream(shareFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
    }

    fun launchShare(context: Context, uri: Uri, examName: String) {
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

    /** One label/value pair; a value too long for the card is shrunk, then shortened. */
    private fun drawDetailLine(
        canvas: Canvas,
        label: String,
        value: String,
        startY: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        maxWidth: Float
    ) {
        canvas.drawText(label, DetailStartX, startY, labelPaint)
        val fitted = Paint(valuePaint)
        val text = value.ifBlank { "-" }
        while (fitted.measureText(text) > maxWidth && fitted.textSize > DetailMinTextSize) {
            fitted.textSize -= 2f
        }
        val shown = if (fitted.measureText(text) <= maxWidth) {
            text
        } else {
            TextUtils.ellipsize(text, TextPaint(fitted), maxWidth, TextUtils.TruncateAt.END).toString()
        }
        canvas.drawText(shown, DetailStartX, startY + 62f, fitted)
    }

    private fun drawIntroParagraph(
        canvas: Canvas,
        paint: TextPaint,
        logicalWidth: Float
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

        canvas.withTranslation(((logicalWidth - width) / 2f), startY) {
            layout.draw(this)
        }
    }
}
