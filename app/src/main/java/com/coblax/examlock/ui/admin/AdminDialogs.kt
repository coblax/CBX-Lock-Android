package com.coblax.examlock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.R
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialog
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiStatusTone
import com.google.android.libraries.places.api.model.Place

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@Composable
internal fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    tone: UiStatusTone = UiStatusTone.Info
) {
    AppAlertDialog(
        tone = tone,
        icon = if (tone == UiStatusTone.Info) Icons.Rounded.Info else Icons.Rounded.Warning,
        title = title,
        message = message,
        dismissible = true,
        onDismissRequest = onDismiss,
        primaryAction = AppAlertAction(tr("Close", "Tutup"), onDismiss)
    )
}

@Composable
internal fun ScanSourceDialog(
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.current.background,
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.current.outlineMedium,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = tr("Choose Scan Source", "Pilih Sumber Scan"),
                color = AppColors.current.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // Description
            Text(
                text = tr(
                    "Scan the exam QR with the camera or pick a QR image from storage.",
                    "Scan QR ujian dengan kamera atau pilih gambar QR dari penyimpanan."
                ),
                color = AppColors.current.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Camera action card — primary
            Button(
                onClick = onCameraClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.current.blue,
                    contentColor = AppColors.current.onDark
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr("Camera", "Kamera"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // File action card — secondary/outline style
            Button(
                onClick = onFileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.current.surfaceSoft,
                    contentColor = AppColors.current.blue
                ),
                border = BorderStroke(1.dp, AppColors.current.outline)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr("File QR", "File QR"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Cancel link
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr("Cancel", "Batal"),
                    color = AppColors.current.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun AdminPasswordDialog(
    password: String,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.current.background,
        title = {
            Text(
                text = tr("Admin Access", "Akses Admin"),
                color = AppColors.current.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = { Text(tr("Enter password", "Masukkan password")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.current.surfaceSoft,
                        unfocusedContainerColor = AppColors.current.surfaceSoft,
                        focusedBorderColor = AppColors.current.blue,
                        unfocusedBorderColor = AppColors.current.outline,
                        focusedTextColor = AppColors.current.textPrimary,
                        unfocusedTextColor = AppColors.current.textPrimary,
                        cursorColor = AppColors.current.blue,
                        focusedPlaceholderColor = AppColors.current.textMuted,
                        unfocusedPlaceholderColor = AppColors.current.textMuted
                    )
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = AppColors.current.issueText,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(tr("Unlock", "Buka"), color = AppColors.current.blue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "Batal"), color = AppColors.current.textMuted)
            }
        }
    )
}
