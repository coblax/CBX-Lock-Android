package com.coblax.examlock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockIssueText
import com.google.android.libraries.places.api.model.Place

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@Composable
internal fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockBackground,
        title = {
            Text(
                text = title,
                color = LockTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = LockTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Close", "Tutup"), color = LockBlue)
            }
        }
    )
}

@Composable
internal fun ScanSourceDialog(
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockBackground,
        title = {
            Text(
                text = tr("Choose Scan Source", "Pilih Sumber Scan"),
                color = LockTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr(
                        "Scan the exam QR with the camera or pick a QR image from storage.",
                        "Scan QR ujian dengan kamera atau pilih gambar QR dari penyimpanan."
                    ),
                    color = LockTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LockBlue,
                        contentColor = LockOnDark
                    )
                ) {
                    Text(tr("Camera", "Kamera"), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onFileClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LockBlue
                    ),
                    border = BorderStroke(1.dp, LockOutline)
                ) {
                    Text(tr("File QR", "File QR"), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Close", "Tutup"), color = LockBlue)
            }
        }
    )
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
        containerColor = LockBackground,
        title = {
            Text(
                text = tr("Admin Access", "Akses Admin"),
                color = LockTextPrimary,
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
                        focusedContainerColor = LockSurfaceSoft,
                        unfocusedContainerColor = LockSurfaceSoft,
                        focusedBorderColor = LockBlue,
                        unfocusedBorderColor = LockOutline,
                        focusedTextColor = LockTextPrimary,
                        unfocusedTextColor = LockTextPrimary,
                        cursorColor = LockBlue,
                        focusedPlaceholderColor = LockTextMuted,
                        unfocusedPlaceholderColor = LockTextMuted
                    )
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = LockIssueText,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(tr("Unlock", "Buka"), color = LockBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "Batal"), color = LockTextMuted)
            }
        }
    )
}
