package com.coblax.examlock.ui.admin

import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.R
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

internal fun activeSecurityOverrideCount(settings: AdminSettings): Int {
    return listOf(
        settings.bypassScreenPinning,
        settings.bypassBluetooth,
        settings.bypassAccessibility,
        settings.bypassAdb,
        settings.bypassRoot,
        settings.bypassReverseEngineering,
        settings.bypassApkIntegrity,
        settings.bypassVirtualEnvironment,
        settings.bypassVpn,
        settings.bypassKeyboardPolicy,
        settings.bypassClipboard,
        settings.bypassOverlay,
        settings.bypassGeofence,
        settings.bypassFakeLocation,
        settings.bypassDeviceTime,
        settings.bypassAppSwitch,
        settings.bypassScreenRecorder,
        settings.bypassDisplayMirror,
        settings.bypassMultiWindow
    ).count { it }
}

@Composable
internal fun SecretAdminSecurityOverridesCard(
    settings: AdminSettings,
    overridesActive: Boolean,
    onSettingsChange: (AdminSettings) -> Unit
) {
        var pendingOverrideTitle by remember { mutableStateOf<String?>(null) }
        var pendingOverrideSettings by remember { mutableStateOf<AdminSettings?>(null) }
        val activeOverrideCount = remember(settings) {
            activeSecurityOverrideCount(settings)
        }
        val screenPinningTitle = tr("Bypass Screen Pinning", "Bypass Screen Pinning")
        val bluetoothTitle = tr("Bypass Bluetooth Checks", "Bypass Cek Bluetooth")
        val accessibilityTitle = tr(
            "Bypass Accessibility Checks",
            "Bypass Cek Aksesibilitas"
        )
        val adbTitle = tr("Bypass ADB Checks", "Bypass Cek ADB")
        val rootTitle = tr("Bypass Root Checks", "Bypass Cek Root")
        val reverseEngineeringTitle = tr(
            "Bypass Reverse Engineering Checks",
            "Bypass Cek Reverse Engineering"
        )
        val apkIntegrityTitle = tr(
            "Bypass APK Integrity Checks",
            "Bypass Cek Integritas APK"
        )
        val virtualEnvironmentTitle = tr(
            "Bypass Virtual Environment",
            "Bypass Virtual Environment"
        )
        val vpnTitle = tr("Bypass VPN Detection", "Bypass Deteksi VPN")
        val keyboardTitle = tr("Bypass Keyboard Policy", "Bypass Kebijakan Keyboard")
        val clipboardTitle = tr(
            "Bypass Clipboard Monitoring",
            "Bypass Monitoring Clipboard"
        )
        val overlayTitle = tr("Bypass Overlay Detection", "Bypass Deteksi Overlay")
        val geofenceTitle = tr("Bypass Geofence", "Bypass Geofence")
        val fakeLocationTitle = tr(
            "Bypass Anti-Fake-Location",
            "Bypass Anti-Fake-Location"
        )
        val deviceTimeTitle = tr("Bypass Device Time", "Bypass Waktu Perangkat")
        val appSwitchTitle = tr(
            "Bypass App Switch Alerts",
            "Bypass Peringatan App Switch"
        )
        val screenRecorderTitle = tr(
            "Bypass Screen Recorder Detection",
            "Bypass Deteksi Screen Recorder"
        )
        val displayMirrorTitle = tr(
            "Bypass Display Mirror Detection",
            "Bypass Deteksi Display Mirror"
        )
        val multiWindowTitle = tr(
            "Bypass Multi-Window Detection",
            "Bypass Deteksi Multi-Window"
        )

        fun requestOverrideChange(
            title: String,
            enabled: Boolean,
            proposedSettings: AdminSettings
        ) {
            if (enabled) {
                pendingOverrideTitle = title
                pendingOverrideSettings = proposedSettings
            } else {
                onSettingsChange(proposedSettings)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = AppColors.current.surfaceSoft,
            border = BorderStroke(1.dp, AppColors.current.outline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Security Overrides", "Override Keamanan"),
                        color = AppColors.current.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (overridesActive || activeOverrideCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(UiTokens.RadiusPill),
                            color = AppColors.current.gold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, AppColors.current.gold.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = tr(
                                    "$activeOverrideCount ACTIVE",
                                    "$activeOverrideCount AKTIF"
                                ),
                                color = AppColors.current.goldDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                AdminToggleRow(
                    title = tr("Bypass Screen Pinning", "Bypass Screen Pinning"),
                    description = tr(
                        "Skip lock-task and pin confirmation.",
                        "Lewati lock-task dan konfirmasi pin."
                    ),
                    checked = settings.bypassScreenPinning,
                    onCheckedChange = {
                        requestOverrideChange(
                            screenPinningTitle,
                            it,
                            settings.copy(bypassScreenPinning = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Bluetooth Checks", "Bypass Cek Bluetooth"),
                    description = tr(
                        "Ignore Bluetooth permission and status checks.",
                        "Abaikan izin dan status Bluetooth."
                    ),
                    checked = settings.bypassBluetooth,
                    onCheckedChange = {
                        requestOverrideChange(
                            bluetoothTitle,
                            it,
                            settings.copy(bypassBluetooth = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Accessibility Checks", "Bypass Cek Aksesibilitas"),
                    description = tr(
                        "Ignore accessibility service warnings and blocks.",
                        "Abaikan peringatan dan blokir aksesibilitas."
                    ),
                    checked = settings.bypassAccessibility,
                    onCheckedChange = {
                        requestOverrideChange(
                            accessibilityTitle,
                            it,
                            settings.copy(bypassAccessibility = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass ADB Checks", "Bypass Cek ADB"),
                    description = tr(
                        "Ignore USB debugging checks.",
                        "Abaikan pemeriksaan USB debugging."
                    ),
                    checked = settings.bypassAdb,
                    onCheckedChange = {
                        requestOverrideChange(
                            adbTitle,
                            it,
                            settings.copy(bypassAdb = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Root Checks", "Bypass Cek Root"),
                    description = tr(
                        "Ignore root device detection.",
                        "Abaikan deteksi perangkat root."
                    ),
                    checked = settings.bypassRoot,
                    onCheckedChange = {
                        requestOverrideChange(
                            rootTitle,
                            it,
                            settings.copy(bypassRoot = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr(
                        "Bypass Reverse Engineering Checks",
                        "Bypass Cek Reverse Engineering"
                    ),
                    description = if (settings.reverseEngineeringBypassTampered) {
                        tr(
                            "Bypass storage was tampered. Enforcement stays active until the admin saves this setting again.",
                            "Storage bypass terdeteksi tampered. Enforcement tetap aktif sampai admin menyimpan ulang pengaturan ini."
                        )
                    } else {
                        tr(
                            "Skip debugger, tracer, hooking memory, class, and package enforcement for official troubleshooting only. Detection remains logged.",
                            "Lewati enforcement debugger, tracer, memory hooking, class, dan package hanya untuk troubleshooting resmi. Deteksi tetap dicatat."
                        )
                    },
                    checked = settings.bypassReverseEngineering && !settings.reverseEngineeringBypassTampered,
                    onCheckedChange = {
                        requestOverrideChange(
                            reverseEngineeringTitle,
                            it,
                            settings.copy(bypassReverseEngineering = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass APK Integrity Checks", "Bypass Cek Integritas APK"),
                    description = if (settings.apkIntegrityBypassTampered) {
                        tr(
                            "Bypass storage was tampered. Enforcement stays active until the admin saves this setting again.",
                            "Storage bypass terdeteksi tampered. Enforcement tetap aktif sampai admin menyimpan ulang pengaturan ini."
                        )
                    } else {
                        tr(
                            "Skip signature/hash integrity enforcement for official troubleshooting only. Detection remains logged.",
                            "Lewati enforcement signature/hash integrity hanya untuk troubleshooting resmi. Deteksi tetap dicatat."
                        )
                    },
                    checked = settings.bypassApkIntegrity && !settings.apkIntegrityBypassTampered,
                    onCheckedChange = {
                        requestOverrideChange(
                            apkIntegrityTitle,
                            it,
                            settings.copy(bypassApkIntegrity = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Virtual Environment", "Bypass Virtual Environment"),
                    description = tr(
                        "Ignore emulator/VM detection.",
                        "Abaikan deteksi emulator/VM."
                    ),
                    checked = settings.bypassVirtualEnvironment,
                    onCheckedChange = {
                        requestOverrideChange(
                            virtualEnvironmentTitle,
                            it,
                            settings.copy(bypassVirtualEnvironment = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass VPN Detection", "Bypass Deteksi VPN"),
                    description = tr(
                        "Allow exam start while VPN is active for approved troubleshooting only.",
                        "Izinkan mulai ujian saat VPN aktif hanya untuk troubleshooting resmi."
                    ),
                    checked = settings.bypassVpn,
                    onCheckedChange = {
                        requestOverrideChange(
                            vpnTitle,
                            it,
                            settings.copy(bypassVpn = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Keyboard Policy", "Bypass Kebijakan Keyboard"),
                    description = tr(
                        "Allow any system keyboard without fallback.",
                        "Izinkan keyboard sistem apa pun tanpa fallback."
                    ),
                    checked = settings.bypassKeyboardPolicy,
                    onCheckedChange = {
                        requestOverrideChange(
                            keyboardTitle,
                            it,
                            settings.copy(bypassKeyboardPolicy = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Clipboard Monitoring", "Bypass Monitoring Clipboard"),
                    description = tr(
                        "Disable clipboard change alarms.",
                        "Matikan alarm perubahan clipboard."
                    ),
                    checked = settings.bypassClipboard,
                    onCheckedChange = {
                        requestOverrideChange(
                            clipboardTitle,
                            it,
                            settings.copy(bypassClipboard = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Overlay Detection", "Bypass Deteksi Overlay"),
                    description = tr(
                        "Ignore obscured touch alerts.",
                        "Abaikan peringatan sentuhan tertutup."
                    ),
                    checked = settings.bypassOverlay,
                    onCheckedChange = {
                        requestOverrideChange(
                            overlayTitle,
                            it,
                            settings.copy(bypassOverlay = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Geofence", "Bypass Geofence"),
                    description = tr(
                        "Skip exam-area position enforcement.",
                        "Lewati enforcement posisi area ujian."
                    ),
                    checked = settings.bypassGeofence,
                    onCheckedChange = {
                        requestOverrideChange(
                            geofenceTitle,
                            it,
                            settings.copy(bypassGeofence = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Anti-Fake-Location", "Bypass Anti-Fake-Location"),
                    description = tr(
                        "Skip mock-location and fake GPS enforcement.",
                        "Lewati enforcement mock-location dan fake GPS."
                    ),
                    checked = settings.bypassFakeLocation,
                    onCheckedChange = {
                        requestOverrideChange(
                            fakeLocationTitle,
                            it,
                            settings.copy(bypassFakeLocation = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Device Time", "Bypass Waktu Perangkat"),
                    description = tr(
                        "Skip automatic date & time, automatic time zone, and clock-change checks.",
                        "Lewati cek tanggal & waktu otomatis, zona waktu otomatis, dan perubahan jam."
                    ),
                    checked = settings.bypassDeviceTime,
                    onCheckedChange = {
                        requestOverrideChange(
                            deviceTimeTitle,
                            it,
                            settings.copy(bypassDeviceTime = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass App Switch Alerts", "Bypass Peringatan App Switch"),
                    description = tr(
                        "Disable forced-exit alarms on app switching.",
                        "Matikan alarm keluar paksa saat pindah aplikasi."
                    ),
                    checked = settings.bypassAppSwitch,
                    onCheckedChange = {
                        requestOverrideChange(
                            appSwitchTitle,
                            it,
                            settings.copy(bypassAppSwitch = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Screen Recorder Detection", "Bypass Deteksi Screen Recorder"),
                    description = tr(
                        "Skip screen recorder app detection.",
                        "Lewati deteksi aplikasi screen recorder."
                    ),
                    checked = settings.bypassScreenRecorder,
                    onCheckedChange = {
                        requestOverrideChange(
                            screenRecorderTitle,
                            it,
                            settings.copy(bypassScreenRecorder = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Display Mirror Detection", "Bypass Deteksi Display Mirror"),
                    description = tr(
                        "Skip external display / screen casting detection.",
                        "Lewati deteksi display eksternal / screen casting."
                    ),
                    checked = settings.bypassDisplayMirror,
                    onCheckedChange = {
                        requestOverrideChange(
                            displayMirrorTitle,
                            it,
                            settings.copy(bypassDisplayMirror = it)
                        )
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass Multi-Window Detection", "Bypass Deteksi Multi-Window"),
                    description = tr(
                        "Skip split-screen and picture-in-picture detection.",
                        "Lewati deteksi split-screen dan picture-in-picture."
                    ),
                    checked = settings.bypassMultiWindow,
                    onCheckedChange = {
                        requestOverrideChange(
                            multiWindowTitle,
                            it,
                            settings.copy(bypassMultiWindow = it)
                        )
                    }
                )
            }
        }

        val proposedSettings = pendingOverrideSettings
        if (proposedSettings != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingOverrideTitle = null
                    pendingOverrideSettings = null
                },
                title = {
                    Text(tr("Enable security override?", "Aktifkan override keamanan?"))
                },
                text = {
                    Text(
                        text = tr(
                            "${pendingOverrideTitle.orEmpty()} weakens exam enforcement. Enable it only for approved troubleshooting. Detection remains logged.",
                            "${pendingOverrideTitle.orEmpty()} melemahkan enforcement ujian. Aktifkan hanya untuk troubleshooting resmi. Deteksi tetap dicatat."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onSettingsChange(proposedSettings)
                            pendingOverrideTitle = null
                            pendingOverrideSettings = null
                        }
                    ) {
                        Text(tr("Enable override", "Aktifkan override"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingOverrideTitle = null
                            pendingOverrideSettings = null
                        }
                    ) {
                        Text(tr("Cancel", "Batal"))
                    }
                }
            )
        }
}
