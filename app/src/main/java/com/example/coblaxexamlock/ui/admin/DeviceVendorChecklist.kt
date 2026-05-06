package com.example.coblaxexamlock.ui.admin

import java.util.Locale

internal enum class DeviceVendorFamily {
    Xiaomi,
    Oppo,
    Vivo,
    Samsung,
    Generic
}

internal data class DeviceVendorChecklistItem(
    val title: String,
    val detail: String
)

internal data class DeviceVendorChecklist(
    val family: DeviceVendorFamily,
    val displayName: String,
    val items: List<DeviceVendorChecklistItem>
)

internal fun resolveDeviceVendorChecklist(
    manufacturer: String?,
    brand: String?
): DeviceVendorChecklist {
    val key = listOfNotNull(manufacturer, brand)
        .joinToString(" ")
        .lowercase(Locale.US)
    val family = when {
        key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") ->
            DeviceVendorFamily.Xiaomi
        key.contains("oppo") || key.contains("realme") ->
            DeviceVendorFamily.Oppo
        key.contains("vivo") || key.contains("iqoo") ->
            DeviceVendorFamily.Vivo
        key.contains("samsung") ->
            DeviceVendorFamily.Samsung
        else -> DeviceVendorFamily.Generic
    }
    return DeviceVendorChecklist(
        family = family,
        displayName = when (family) {
            DeviceVendorFamily.Xiaomi -> "Xiaomi / Redmi / POCO"
            DeviceVendorFamily.Oppo -> "Oppo / Realme"
            DeviceVendorFamily.Vivo -> "Vivo / iQOO"
            DeviceVendorFamily.Samsung -> "Samsung"
            DeviceVendorFamily.Generic -> "Android"
        },
        items = checklistItemsFor(family)
    )
}

private fun checklistItemsFor(family: DeviceVendorFamily): List<DeviceVendorChecklistItem> {
    val common = listOf(
        DeviceVendorChecklistItem(
            title = "Battery saver",
            detail = "Matikan battery saver saat ujian agar monitoring lock, lokasi, dan alarm tidak dipaksa tidur."
        ),
        DeviceVendorChecklistItem(
            title = "Location precision",
            detail = "Aktifkan lokasi presisi untuk geofence dan anti-fake-location."
        ),
        DeviceVendorChecklistItem(
            title = "Overlay permission",
            detail = "Tutup floating window, bubble chat, screen filter, dan overlay lain sebelum ujian."
        ),
        DeviceVendorChecklistItem(
            title = "Lock mode",
            detail = "Pastikan screen pinning/lock task tersedia, lalu uji Start Exam Mode sekali sebelum hari ujian."
        )
    )
    val vendorSpecific = when (family) {
        DeviceVendorFamily.Xiaomi -> listOf(
            DeviceVendorChecklistItem(
                title = "Autostart",
                detail = "Di MIUI/HyperOS, izinkan Autostart untuk CBX Exam Lock bila sekolah memakai alarm/monitoring panjang."
            ),
            DeviceVendorChecklistItem(
                title = "Battery restriction",
                detail = "Set Battery saver aplikasi ke No restrictions agar WebView dan lokasi tidak diputus agresif."
            )
        )
        DeviceVendorFamily.Oppo -> listOf(
            DeviceVendorChecklistItem(
                title = "App battery management",
                detail = "Di ColorOS/Realme UI, izinkan background activity untuk sesi ujian panjang."
            ),
            DeviceVendorChecklistItem(
                title = "Floating window",
                detail = "Nonaktifkan floating window dan smart sidebar sebelum ujian."
            )
        )
        DeviceVendorFamily.Vivo -> listOf(
            DeviceVendorChecklistItem(
                title = "Background power consumption",
                detail = "Di Funtouch/iQOO, longgarkan background power untuk CBX Exam Lock."
            ),
            DeviceVendorChecklistItem(
                title = "iManager cleanup",
                detail = "Pastikan iManager tidak memasukkan CBX Exam Lock ke daftar auto-clean saat layar terkunci."
            )
        )
        DeviceVendorFamily.Samsung -> listOf(
            DeviceVendorChecklistItem(
                title = "Sleeping apps",
                detail = "Keluarkan CBX Exam Lock dari Sleeping/Deep sleeping apps."
            ),
            DeviceVendorChecklistItem(
                title = "Appear on top",
                detail = "Matikan aplikasi lain yang bisa appear on top agar overlay warning tidak mengganggu."
            )
        )
        DeviceVendorFamily.Generic -> listOf(
            DeviceVendorChecklistItem(
                title = "Background restriction",
                detail = "Pastikan aplikasi tidak dibatasi background dan tidak masuk daftar aplikasi tidur."
            )
        )
    }
    return vendorSpecific + common
}
