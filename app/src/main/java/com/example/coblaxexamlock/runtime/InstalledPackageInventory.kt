package com.example.coblaxexamlock.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo

internal data class InstalledPackageRecord(
    val packageName: String,
    val flags: Int,
    val enabled: Boolean
) {
    val systemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM != 0

    val updatedSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

    val systemOrUpdatedSystemApp: Boolean
        get() = systemApp || updatedSystemApp
}

internal class InstalledPackageInventory(
    val records: List<InstalledPackageRecord>
) {
    val byPackageName: Map<String, InstalledPackageRecord> by lazy {
        records.associateBy { record -> record.packageName }
    }

    fun get(packageName: String): InstalledPackageRecord? = byPackageName[packageName]

    fun hasPackage(packageName: String): Boolean = get(packageName) != null

    fun isSystemOrUpdatedSystemPackage(packageName: String): Boolean {
        return get(packageName)?.systemOrUpdatedSystemApp == true
    }

    fun findPackages(packageNames: Iterable<String>): List<InstalledPackageRecord> {
        return packageNames.mapNotNull { packageName -> get(packageName) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstalledPackageInventory) return false
        return records == other.records
    }

    override fun hashCode(): Int = records.hashCode()
}

internal data class InstalledPackageMetadata(
    val packageName: String,
    val enabled: Boolean,
    val systemApp: Boolean,
    val updatedSystemApp: Boolean,
    val label: String = packageName,
    val versionName: String = "-"
) {
    val systemOrUpdatedSystemApp: Boolean
        get() = systemApp || updatedSystemApp
}

internal fun ApplicationInfo.toInstalledPackageRecord(): InstalledPackageRecord =
    InstalledPackageRecord(
        packageName = packageName,
        flags = flags,
        enabled = enabled
    )

internal fun InstalledPackageRecord.toInstalledPackageMetadata(): InstalledPackageMetadata =
    InstalledPackageMetadata(
        packageName = packageName,
        enabled = enabled,
        systemApp = systemApp,
        updatedSystemApp = updatedSystemApp
    )

internal fun resolveInstalledPackageMetadata(
    context: Context,
    packageName: String,
    packageInventory: InstalledPackageInventory,
    includeDisplayMetadata: Boolean = false
): InstalledPackageMetadata? {
    if (packageName.isBlank()) {
        return null
    }

    val inventoryRecord = packageInventory.get(packageName)
    if (inventoryRecord != null && !includeDisplayMetadata) {
        return inventoryRecord.toInstalledPackageMetadata()
    }
    val appInfo = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(packageName, 0)
    }.getOrNull()
    val record = inventoryRecord ?: appInfo?.toInstalledPackageRecord() ?: return null
    if (!includeDisplayMetadata) {
        return record.toInstalledPackageMetadata()
    }
    val label = runCatching {
        appInfo?.let { context.packageManager.getApplicationLabel(it).toString().trim() }
    }.getOrNull().orEmpty()
    val versionName = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull().orEmpty()

    return InstalledPackageMetadata(
        packageName = record.packageName,
        enabled = record.enabled,
        systemApp = record.systemApp,
        updatedSystemApp = record.updatedSystemApp,
        label = label.ifBlank { record.packageName },
        versionName = versionName.ifBlank { "-" }
    )
}

@SuppressLint("QueryPermissionsNeeded")
internal fun readInstalledPackageInventory(context: Context): InstalledPackageInventory {
    val records = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getInstalledApplications(0)
    }.getOrDefault(emptyList())
        .map(ApplicationInfo::toInstalledPackageRecord)
    return InstalledPackageInventory(records = records)
}
