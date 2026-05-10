package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import java.util.Locale

internal data class PreparationChecklistLocationDetailText(
    val geofenceDetail: String?,
    val fakeLocationDetail: String?
)

internal fun buildPreparationChecklistLocationDetailText(
    state: PreparationScreenState,
    uiLanguage: UiLanguage
): PreparationChecklistLocationDetailText = with(state) {
    fun preparationDetailOrNull(english: () -> String, indonesian: () -> String): String? =
        preparationDetailOrNull(showChecklistDetails, uiLanguage, english, indonesian)
    val geofenceDetail = preparationDetailOrNull(
        english = {
            "- Location policy source: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence enabled: ${if (geofenceRuntimeStatus.evaluation.enabled) "yes" else "no"}\n" +
                "- Shape: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Polygon points: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Polygon vertices: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Circle centers: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Circle centers summary: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Bypass state: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Closest / primary center: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Shared radius: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Permission granted: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "yes" else "no"}\n" +
                "- Precise granted: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "yes" else "no"}\n" +
                "- Current coordinates: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Accuracy: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Fix quality: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix age: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot used for geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "yes" else "no"}\n" +
                "- Distance from closest center: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Final verdict: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Geofence verdict: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Violations: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
        },
        indonesian = {
            "- Sumber policy lokasi: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence aktif: ${if (geofenceRuntimeStatus.evaluation.enabled) "ya" else "tidak"}\n" +
                "- Bentuk: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Titik polygon: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Vertex polygon: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Jumlah center circle: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Ringkasan center circle: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Status bypass: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Center terdekat / utama: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Radius bersama: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Izin lokasi: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "diberikan" else "belum"}\n" +
                "- Lokasi presisi: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "ya" else "belum"}\n" +
                "- Layanan lokasi: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "aktif" else "nonaktif"}\n" +
                "- Koordinat saat ini: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Akurasi: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Kualitas fix: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Umur fix: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot dipakai untuk geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "ya" else "tidak"}\n" +
                "- Jarak dari center terdekat: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Verdict final: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Verdict geofence: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Jumlah pelanggaran: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
        }
    )
    val fakeLocationDetail = preparationDetailOrNull(
        english = {
            "- Monitoring enabled: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "yes" else "no"}\n" +
                "- Permission granted: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "yes" else "no"}\n" +
                "- Snapshot available: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "yes" else "no"}\n" +
                "- Bypass state: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Mock location flag: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "yes" else "no"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Final verdict: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "enabled" else "disabled"}\n" +
                "- Fix quality: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix-quality eligible: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "yes" else "no"}\n" +
                "- Suspicious packages: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Supporting signals: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Violations: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
        },
        indonesian = {
            "- Monitoring aktif: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "ya" else "tidak"}\n" +
                "- Izin lokasi: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "ya" else "tidak"}\n" +
                "- Layanan lokasi aktif: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "ya" else "tidak"}\n" +
                "- Snapshot tersedia: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "ya" else "tidak"}\n" +
                "- Status bypass: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Flag mock location: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "ya" else "tidak"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Verdict final: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "aktif" else "nonaktif"}\n" +
                "- Kualitas fix: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix layak dinilai: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "ya" else "tidak"}\n" +
                "- Paket mencurigakan: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Sinyal pendukung: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Jumlah pelanggaran: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
        }
    )

    PreparationChecklistLocationDetailText(
        geofenceDetail = geofenceDetail,
        fakeLocationDetail = fakeLocationDetail
    )
}
