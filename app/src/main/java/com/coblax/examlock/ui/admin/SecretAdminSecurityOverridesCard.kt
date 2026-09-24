package com.coblax.examlock.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.dialog.AppAlertAction
import com.coblax.examlock.ui.dialog.AppAlertDialog
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiStatusTone

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

/** Every bypass off. Turning bypasses off only makes the exam stricter, so no confirmation. */
internal fun AdminSettings.withAllSecurityOverridesOff(): AdminSettings = copy(
    bypassScreenPinning = false,
    bypassBluetooth = false,
    bypassAccessibility = false,
    bypassAdb = false,
    bypassRoot = false,
    bypassReverseEngineering = false,
    bypassApkIntegrity = false,
    bypassVirtualEnvironment = false,
    bypassVpn = false,
    bypassKeyboardPolicy = false,
    bypassClipboard = false,
    bypassOverlay = false,
    bypassGeofence = false,
    bypassFakeLocation = false,
    bypassDeviceTime = false,
    bypassAppSwitch = false,
    bypassScreenRecorder = false,
    bypassDisplayMirror = false,
    bypassMultiWindow = false
)

/** One bypass switch: how it reads, and how it maps onto [AdminSettings]. */
internal data class SecurityOverrideItem(
    val key: String,
    val title: String,
    val description: String,
    val isOn: (AdminSettings) -> Boolean,
    val set: (AdminSettings, Boolean) -> AdminSettings
)

internal data class SecurityOverrideGroup(
    val title: String,
    val items: List<SecurityOverrideItem>
)

/**
 * The 19 bypasses, grouped by what they relax so the admin can find one without reading
 * the whole list. Every switch appears exactly once (see SecretAdminOverridesTest).
 */
internal fun securityOverrideGroups(uiLanguage: UiLanguage, settings: AdminSettings): List<SecurityOverrideGroup> {
    fun t(english: String, indonesian: String) = localized(uiLanguage, english, indonesian)
    val tamperedNote = t(
        "Bypass storage was tampered. Enforcement stays on until this is saved again.",
        "Storage bypass dimanipulasi. Enforcement tetap aktif sampai ini disimpan ulang."
    )
    return listOf(
        SecurityOverrideGroup(
            title = t("Screen lock & apps", "Kunci layar & aplikasi"),
            items = listOf(
                SecurityOverrideItem(
                    key = "screen_pinning",
                    title = t("Screen pinning", "Screen pinning"),
                    description = t("Skip lock task and the pin confirmation.", "Lewati lock task dan konfirmasi pin."),
                    isOn = { it.bypassScreenPinning },
                    set = { s, on -> s.copy(bypassScreenPinning = on) }
                ),
                SecurityOverrideItem(
                    key = "app_switch",
                    title = t("App switch alerts", "Peringatan pindah aplikasi"),
                    description = t("No forced-exit alarm when switching apps.", "Tanpa alarm keluar paksa saat pindah aplikasi."),
                    isOn = { it.bypassAppSwitch },
                    set = { s, on -> s.copy(bypassAppSwitch = on) }
                ),
                SecurityOverrideItem(
                    key = "multi_window",
                    title = t("Split screen detection", "Deteksi split screen"),
                    description = t("Allow split screen and picture-in-picture.", "Izinkan split screen dan picture-in-picture."),
                    isOn = { it.bypassMultiWindow },
                    set = { s, on -> s.copy(bypassMultiWindow = on) }
                ),
                SecurityOverrideItem(
                    key = "overlay",
                    title = t("Floating app detection", "Deteksi aplikasi melayang"),
                    description = t("Ignore touches covered by other apps.", "Abaikan sentuhan yang tertutup aplikasi lain."),
                    isOn = { it.bypassOverlay },
                    set = { s, on -> s.copy(bypassOverlay = on) }
                )
            )
        ),
        SecurityOverrideGroup(
            title = t("Device", "Perangkat"),
            items = listOf(
                SecurityOverrideItem(
                    key = "bluetooth",
                    title = t("Bluetooth check", "Cek Bluetooth"),
                    description = t("Ignore Bluetooth permission and state.", "Abaikan izin dan status Bluetooth."),
                    isOn = { it.bypassBluetooth },
                    set = { s, on -> s.copy(bypassBluetooth = on) }
                ),
                SecurityOverrideItem(
                    key = "keyboard",
                    title = t("Keyboard policy", "Kebijakan keyboard"),
                    description = t("Allow any system keyboard, no fallback.", "Izinkan keyboard apa pun tanpa fallback."),
                    isOn = { it.bypassKeyboardPolicy },
                    set = { s, on -> s.copy(bypassKeyboardPolicy = on) }
                ),
                SecurityOverrideItem(
                    key = "accessibility",
                    title = t("Accessibility check", "Cek aksesibilitas"),
                    description = t("Ignore third-party accessibility services.", "Abaikan layanan aksesibilitas pihak ketiga."),
                    isOn = { it.bypassAccessibility },
                    set = { s, on -> s.copy(bypassAccessibility = on) }
                ),
                SecurityOverrideItem(
                    key = "clipboard",
                    title = t("Clipboard monitoring", "Pemantauan clipboard"),
                    description = t("No alarm on clipboard changes.", "Tanpa alarm saat clipboard berubah."),
                    isOn = { it.bypassClipboard },
                    set = { s, on -> s.copy(bypassClipboard = on) }
                ),
                SecurityOverrideItem(
                    key = "device_time",
                    title = t("Device time", "Waktu perangkat"),
                    description = t("Skip automatic date, time zone, and clock checks.", "Lewati cek tanggal, zona waktu, dan jam otomatis."),
                    isOn = { it.bypassDeviceTime },
                    set = { s, on -> s.copy(bypassDeviceTime = on) }
                )
            )
        ),
        SecurityOverrideGroup(
            title = t("Device integrity", "Integritas perangkat"),
            items = listOf(
                SecurityOverrideItem(
                    key = "adb",
                    title = t("USB debugging (ADB)", "USB debugging (ADB)"),
                    description = t("Ignore USB debugging checks.", "Abaikan pemeriksaan USB debugging."),
                    isOn = { it.bypassAdb },
                    set = { s, on -> s.copy(bypassAdb = on) }
                ),
                SecurityOverrideItem(
                    key = "root",
                    title = t("Root check", "Cek root"),
                    description = t("Ignore rooted-device detection.", "Abaikan deteksi HP yang di-root."),
                    isOn = { it.bypassRoot },
                    set = { s, on -> s.copy(bypassRoot = on) }
                ),
                SecurityOverrideItem(
                    key = "reverse_engineering",
                    title = t("Reverse engineering", "Reverse engineering"),
                    description = if (settings.reverseEngineeringBypassTampered) {
                        tamperedNote
                    } else {
                        t(
                            "Skip debugger and hooking enforcement. Still logged.",
                            "Lewati enforcement debugger dan hooking. Tetap dicatat."
                        )
                    },
                    isOn = { it.bypassReverseEngineering && !it.reverseEngineeringBypassTampered },
                    set = { s, on -> s.copy(bypassReverseEngineering = on) }
                ),
                SecurityOverrideItem(
                    key = "apk_integrity",
                    title = t("APK integrity", "Integritas APK"),
                    description = if (settings.apkIntegrityBypassTampered) {
                        tamperedNote
                    } else {
                        t(
                            "Skip signature and hash enforcement. Still logged.",
                            "Lewati enforcement signature dan hash. Tetap dicatat."
                        )
                    },
                    isOn = { it.bypassApkIntegrity && !it.apkIntegrityBypassTampered },
                    set = { s, on -> s.copy(bypassApkIntegrity = on) }
                ),
                SecurityOverrideItem(
                    key = "virtual_environment",
                    title = t("Emulator detection", "Deteksi emulator"),
                    description = t("Allow emulators and virtual machines.", "Izinkan emulator dan mesin virtual."),
                    isOn = { it.bypassVirtualEnvironment },
                    set = { s, on -> s.copy(bypassVirtualEnvironment = on) }
                )
            )
        ),
        SecurityOverrideGroup(
            title = t("Network & location", "Jaringan & lokasi"),
            items = listOf(
                SecurityOverrideItem(
                    key = "vpn",
                    title = t("VPN detection", "Deteksi VPN"),
                    description = t("Allow starting with a VPN on.", "Izinkan mulai ujian saat VPN aktif."),
                    isOn = { it.bypassVpn },
                    set = { s, on -> s.copy(bypassVpn = on) }
                ),
                SecurityOverrideItem(
                    key = "geofence",
                    title = t("Geofence", "Geofence"),
                    description = t("Skip the exam-area position check.", "Lewati cek posisi area ujian."),
                    isOn = { it.bypassGeofence },
                    set = { s, on -> s.copy(bypassGeofence = on) }
                ),
                SecurityOverrideItem(
                    key = "fake_location",
                    title = t("Anti fake location", "Anti lokasi palsu"),
                    description = t("Skip mock location and fake GPS checks.", "Lewati cek mock location dan GPS palsu."),
                    isOn = { it.bypassFakeLocation },
                    set = { s, on -> s.copy(bypassFakeLocation = on) }
                )
            )
        ),
        SecurityOverrideGroup(
            title = t("Screen capture", "Rekam layar"),
            items = listOf(
                SecurityOverrideItem(
                    key = "screen_recorder",
                    title = t("Screen recorder detection", "Deteksi perekam layar"),
                    description = t("Allow screen recorder apps.", "Izinkan aplikasi perekam layar."),
                    isOn = { it.bypassScreenRecorder },
                    set = { s, on -> s.copy(bypassScreenRecorder = on) }
                ),
                SecurityOverrideItem(
                    key = "display_mirror",
                    title = t("Cast / mirror detection", "Deteksi cast / mirror"),
                    description = t("Allow external displays and casting.", "Izinkan layar eksternal dan casting."),
                    isOn = { it.bypassDisplayMirror },
                    set = { s, on -> s.copy(bypassDisplayMirror = on) }
                )
            )
        )
    )
}

@Composable
internal fun SecretAdminSecurityOverridesCard(
    settings: AdminSettings,
    overridesActive: Boolean,
    onSettingsChange: (AdminSettings) -> Unit
) {
    val uiLanguage = LocalUiLanguage.current
    val tokens = ScopedUiTokens.current
    var pendingOverride by remember { mutableStateOf<Pair<String, AdminSettings>?>(null) }
    val activeCount = remember(settings) { activeSecurityOverrideCount(settings) }
    val groups = remember(settings, uiLanguage) { securityOverrideGroups(uiLanguage, settings) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SecretAdminSection(
            title = tr("Status", "Status")
        ) {
            if (activeCount > 0 || overridesActive) {
                SecretAdminNote(
                    text = tr(
                        "$activeCount bypass active. The exam is weaker while they are on; every detection is still logged.",
                        "$activeCount bypass aktif. Ujian lebih lemah selama aktif; semua deteksi tetap dicatat."
                    ),
                    tone = UiStatusTone.Warning
                )
                OutlinedButton(
                    onClick = { onSettingsChange(settings.withAllSecurityOverridesOff()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.touchTarget)
                        .testTag(SecretAdminUiTestTags.DisableAllOverrides),
                    shape = RoundedCornerShape(tokens.radiusMedium),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.current.brandText),
                    border = BorderStroke(1.dp, AppColors.current.outline)
                ) {
                    Text(text = tr("Turn off all bypasses", "Matikan semua bypass"), style = AppTextStyles.button)
                }
            } else {
                SecretAdminNote(
                    text = tr(
                        "No bypass is active. Every exam check is enforced.",
                        "Tidak ada bypass aktif. Semua pemeriksaan ujian berlaku."
                    ),
                    tone = UiStatusTone.Success
                )
            }
        }

        groups.forEach { group ->
            val groupActive = group.items.count { it.isOn(settings) }
            SecretAdminSection(
                title = group.title,
                padded = false,
                trailing = if (groupActive > 0) {
                    {
                        Text(
                            text = tr("$groupActive on", "$groupActive aktif"),
                            color = AppColors.current.goldDark,
                            style = AppTextStyles.label.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    null
                }
            ) {
                group.items.forEachIndexed { index, item ->
                    if (index > 0) SecretAdminRowDivider()
                    val on = item.isOn(settings)
                    SecretAdminSwitchRow(
                        title = item.title,
                        description = item.description,
                        checked = on,
                        highlighted = on,
                        testTag = SecretAdminUiTestTags.OverrideRowPrefix + item.key,
                        onCheckedChange = { enable ->
                            val proposed = item.set(settings, enable)
                            if (enable) {
                                pendingOverride = item.title to proposed
                            } else {
                                onSettingsChange(proposed)
                            }
                        }
                    )
                }
            }
        }
    }

    pendingOverride?.let { (title, proposed) ->
        AppAlertDialog(
            tone = UiStatusTone.Warning,
            icon = Icons.Rounded.GppMaybe,
            title = tr("Turn on this bypass?", "Aktifkan bypass ini?"),
            message = tr(
                "\"$title\" weakens exam enforcement. Use it only for approved troubleshooting; detection stays logged.",
                "\"$title\" melemahkan pengamanan ujian. Pakai hanya untuk troubleshooting resmi; deteksi tetap dicatat."
            ),
            primaryAction = AppAlertAction(
                label = tr("Turn on", "Aktifkan"),
                onClick = {
                    onSettingsChange(proposed)
                    pendingOverride = null
                }
            ),
            secondaryActions = listOf(
                AppAlertAction(
                    label = tr("Cancel", "Batal"),
                    onClick = { pendingOverride = null }
                )
            ),
            dismissible = true,
            onDismissRequest = { pendingOverride = null }
        )
    }
}
