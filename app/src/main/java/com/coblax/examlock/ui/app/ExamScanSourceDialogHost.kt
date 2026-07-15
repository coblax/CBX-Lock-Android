package com.coblax.examlock.ui.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.QrPortraitCaptureActivity
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.config.QrImageReadErrorDecode
import com.coblax.examlock.config.QrImageReadErrorOpen
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.decodeQrPayloadFromImageUri
import com.coblax.examlock.ui.admin.ScanSourceDialog
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
    var showSourceDialog by remember { mutableStateOf(true) }
    fun dismissHost() {
        showSourceDialog = false
        onDismiss()
    }

    val scanLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result: ScanIntentResult ->
        dismissHost()
        val rawPayload = result.contents ?: return@rememberLauncherForActivityResult
        onRawPayload(rawPayload)
    }
    val fileScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            dismissHost()
            return@rememberLauncherForActivityResult
        }
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
                dismissHost()
                return@launch
            }

            if (rawPayload.isNullOrBlank()) {
                onScanError(imageNoQrMessage)
                dismissHost()
                return@launch
            }

            dismissHost()
            onRawPayload(rawPayload)
        }
    }

    if (showSourceDialog) {
        ScanSourceDialog(
            onCameraClick = {
                showSourceDialog = false
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
                showSourceDialog = false
                fileScanLauncher.launch("image/*")
            },
            onDismiss = ::dismissHost
        )
    }
}
