package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline

@Composable
internal fun PreparationNoticeStack(
    state: PreparationScreenState,
    actions: PreparationScreenActions,
    runQuickFix: (QuickFixTarget?, String, Boolean, () -> Unit) -> Unit
) {
    with(state) {
        with(actions) {
            if (webViewSessionResetInFlight) {
                PreparationNoticeCard(
                    title = tr("Preparing Clean Exam Browser", "Menyiapkan Browser Ujian Bersih"),
                    message = tr(
                        "CBX Lock is clearing cookies, local storage, and cached WebView data before the exam opens.",
                        "CBX Lock sedang membersihkan cookie, local storage, dan cache WebView sebelum ujian dibuka."
                    ),
                    accentColor = LockGoldDark,
                    backgroundColor = Color(0xFFFFF8E8)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            webViewSessionResetError?.let { resetError ->
                val webViewHealthItem = preExamHealthCheckSnapshot.items.firstOrNull {
                    it.category == PreExamHealthCategory.WebView
                }
                PreparationNoticeCard(
                    title = tr("Exam Browser Recovered Safely", "Browser Ujian Dipulihkan Aman"),
                    message = resetError + "\n\n" + tr(
                        "Export diagnostics if an admin needs evidence, then press Start Exam Mode again.",
                        "Export diagnostik bila admin membutuhkan bukti, lalu tekan Mulai Ujian lagi."
                    ) + "\n\n" + (webViewHealthItem?.detail ?: ""),
                    accentColor = Color(0xFFB34A4A),
                    backgroundColor = Color(0xFFFFEFEF)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportDiagnostics,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = LockBlueDeep
                        ),
                        border = BorderStroke(1.dp, LockOutline)
                    ) {
                        Text(
                            text = tr("Export Diagnostics", "Export Diagnostik"),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onStartExam,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !(isStartingExam || webViewSessionResetInFlight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark
                        )
                    ) {
                        Text(
                            text = tr("Start Again", "Mulai Lagi"),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                TextButton(
                    onClick = {
                        runQuickFix(
                            QuickFixTarget.WebView,
                            "webview_provider_recovery_fix",
                            true,
                            onOpenWebViewProviderSettings
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Open WebView Settings", "Buka Setelan WebView"),
                        color = LockBlue,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val pinningPending = pinningActivationState.isPending()
            val pinningRetryReady = pinningActivationState == PinningActivationState.TimeoutRetryReady
            if ((pinningPending || pinningRetryReady) && !bypassScreenPinning) {
                PreparationNoticeCard(
                    title = if (pinningRetryReady) {
                        tr("Screen Pinning Not Active — Try Again", "Screen Pinning Belum Aktif — Coba Lagi")
                    } else {
                        tr("Starting Screen Pinning...", "Menjalankan Screen Pinning...")
                    },
                    message = if (pinningRetryReady) {
                        tr(
                            "① Tap \"Start Screen Pinning\" again  ← you are here\n② When Android shows the dialog → tap \"Got it\" or \"Pin\"\n③ Stay on this screen — do NOT press Home or Recent",
                            "① Ketuk \"Start Screen Pinning\" lagi  ← Anda di sini\n② Saat Android menampilkan dialog → ketuk \"Got it\" atau \"Pin\"\n③ Tetap di layar ini — JANGAN tekan Home atau Recent"
                        )
                    } else {
                        screenPinningMessage ?: tr(
                            "① Android may show a confirmation dialog → tap \"Got it\" or \"Pin\"  ← you are here\n② Stay on this screen — do NOT press Home or Recent\n③ Wait until Screen Pinning is active",
                            "① Android mungkin menampilkan dialog konfirmasi → ketuk \"Got it\" atau \"Pin\"  ← Anda di sini\n② Tetap di layar ini — JANGAN tekan Home atau Recent\n③ Tunggu sampai Screen Pinning aktif"
                        )
                    },
                    accentColor = if (pinningRetryReady) Color(0xFFB34A4A) else LockGoldDark,
                    backgroundColor = if (pinningRetryReady) Color(0xFFFFF1F0) else Color(0xFFFFF8E8)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }


            if (usingBuiltInExamKeyboard && !bypassKeyboardPolicy) {
                PreparationNoticeCard(
                    title = tr("System Keyboard Is Not Compatible", "Keyboard Sistem Tidak Cocok"),
                    message = tr(
                        "CBX Lock will switch to its internal keyboard when the exam starts.",
                        "CBX Lock akan beralih ke keyboard internal saat ujian dimulai."
                    ),
                    accentColor = LockGoldDark,
                    backgroundColor = Color(0xFFFFF8E8)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (rootSecurityStatus.detected && !bypassRoot) {
                PreparationNoticeCard(
                    title = tr("Root Device Detected", "Perangkat Root Terdeteksi"),
                    message = tr(
                        "For security, continue the exam on a non-rooted device.",
                        "Demi keamanan, lanjutkan ujian pada perangkat yang tidak di-root."
                    ),
                    accentColor = Color(0xFFB34A4A),
                    backgroundColor = Color(0xFFFFEFEF)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (rootSecurityStatus.selinuxPermissive && !bypassRoot && !rootSecurityStatus.detected) {
                PreparationNoticeCard(
                    title = tr("SELinux Permissive", "SELinux Permisif"),
                    message = tr(
                        "SELinux is not enforcing. The exam can continue, but security is reduced.",
                        "SELinux tidak enforcing. Ujian bisa lanjut, namun tingkat keamanan berkurang."
                    ),
                    accentColor = LockGoldDark,
                    backgroundColor = Color(0xFFFFF8E8)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable) {
                PreparationNoticeCard(
                    title = if (networkReadinessStatus.verdict == NetworkReadinessVerdict.VpnActive && !bypassVpn) {
                        tr("VPN Blocks Start Exam", "VPN Memblokir Mulai Ujian")
                    } else {
                        tr("Network Warning", "Peringatan Network")
                    },
                    message = when (networkReadinessStatus.verdict) {
                        NetworkReadinessVerdict.Offline -> tr(
                            "The exam can still start, but the device is offline right now. Stabilize Wi-Fi or cellular data first if the exam depends on internet.",
                            "Ujian tetap bisa dimulai, tetapi perangkat sedang offline. Stabilkan Wi-Fi atau data seluler terlebih dahulu jika ujian bergantung pada internet."
                        )
                        NetworkReadinessVerdict.Unstable -> tr(
                            "The connection has changed several times recently. The exam can continue, but a more stable network is recommended.",
                            "Koneksi berubah beberapa kali belakangan ini. Ujian bisa lanjut, tetapi jaringan yang lebih stabil sangat disarankan."
                        )
                        NetworkReadinessVerdict.CaptivePortal -> tr(
                            "This connection may still need a login or captive-portal confirmation before internet access is fully ready.",
                            "Koneksi ini mungkin masih membutuhkan login atau konfirmasi captive portal sebelum internet benar-benar siap."
                        )
                        NetworkReadinessVerdict.VpnActive -> if (bypassVpn) {
                            tr(
                                "VPN bypass active. Continue only when this is approved troubleshooting and keep a Network report ready for admin review.",
                                "Bypass VPN aktif. Lanjutkan hanya untuk troubleshooting resmi dan siapkan report Network untuk admin."
                            )
                        } else {
                            tr(
                                "VPN is active. Start Exam is blocked until the VPN is turned off and Network status is refreshed.",
                                "VPN aktif. Mulai Ujian diblokir sampai VPN dimatikan dan status Network direfresh."
                            )
                        }
                        NetworkReadinessVerdict.Unvalidated -> tr(
                            "Android has not validated this connection yet. The exam can still continue, but internet access may still be limited.",
                            "Android belum memvalidasi koneksi ini. Ujian tetap bisa lanjut, tetapi akses internet mungkin masih terbatas."
                        )
                        NetworkReadinessVerdict.AirplaneMode -> tr(
                            "Airplane mode is active. The exam can still continue, but no internet connection is currently available.",
                            "Mode pesawat sedang aktif. Ujian tetap bisa lanjut, tetapi saat ini tidak ada koneksi internet."
                        )
                        NetworkReadinessVerdict.ConnectedStable -> tr(
                            "Network monitoring found a connectivity warning.",
                            "Monitoring network menemukan peringatan konektivitas."
                        )
                    },
                    accentColor = if (networkReadinessStatus.verdict == NetworkReadinessVerdict.VpnActive && !bypassVpn) {
                        Color(0xFFB34A4A)
                    } else {
                        LockGoldDark
                    },
                    backgroundColor = if (networkReadinessStatus.verdict == NetworkReadinessVerdict.VpnActive && !bypassVpn) {
                        Color(0xFFFFEFEF)
                    } else {
                        Color(0xFFFFF8E8)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
