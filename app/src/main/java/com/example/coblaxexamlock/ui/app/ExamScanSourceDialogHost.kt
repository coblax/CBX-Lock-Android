package com.example.coblaxexamlock.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.QrPortraitCaptureActivity
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.config.QrImageReadErrorDecode
import com.example.coblaxexamlock.config.QrImageReadErrorOpen
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.runtime.decodeQrPayloadFromImageUri
import com.example.coblaxexamlock.ui.admin.ScanSourceDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
internal fun ExamScanSourceDialogHost(
    uiLanguage: UiLanguage,
    onRawPayload: (String) -> Unit,
    onScanError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        StartupTrace.mark("scan_dialog_opened")
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lowRamProfile = LocalLowRamProfile.current
    val scanLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result: ScanIntentResult ->
        val rawPayload = result.contents ?: return@rememberLauncherForActivityResult
        onRawPayload(rawPayload)
    }
    val fileScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imageOpenFailedMessage = localized(
            uiLanguage,
            "The selected image could not be opened.",
            "Gambar yang dipilih tidak dapat dibuka."
        )
        val imageDecodeFailedMessage = localized(
            uiLanguage,
            "The selected image could not be processed.",
            "Gambar yang dipilih tidak dapat diproses."
        )
        val imageNoQrMessage = localized(
            uiLanguage,
            "No valid QR code was found in the selected image.",
            "QR yang valid tidak ditemukan di gambar yang dipilih."
        )
        coroutineScope.launch {
            val rawPayload = runCatching {
                decodeQrPayloadFromImageUri(
                    context = context,
                    uri = uri,
                    lowRamProfile = lowRamProfile
                )
            }.getOrElse { throwable ->
                onScanError(
                    when (throwable.message) {
                        QrImageReadErrorOpen -> imageOpenFailedMessage
                        QrImageReadErrorDecode -> imageDecodeFailedMessage
                        else -> imageDecodeFailedMessage
                    }
                )
                return@launch
            }

            if (rawPayload.isNullOrBlank()) {
                onScanError(imageNoQrMessage)
                return@launch
            }

            onRawPayload(rawPayload)
        }
    }

    ScanSourceDialog(
        onCameraClick = {
            onDismiss()
            scanLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt(
                        localized(
                            uiLanguage,
                            "Scan the encrypted exam QR",
                            "Arahkan kamera ke QR ujian terenkripsi"
                        )
                    )
                    setBeepEnabled(false)
                    setCaptureActivity(QrPortraitCaptureActivity::class.java)
                    setOrientationLocked(true)
                }
            )
        },
        onFileClick = {
            onDismiss()
            fileScanLauncher.launch("image/*")
        },
        onDismiss = onDismiss
    )
}
