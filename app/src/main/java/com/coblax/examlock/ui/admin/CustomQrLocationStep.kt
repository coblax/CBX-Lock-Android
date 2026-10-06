package com.coblax.examlock.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pentagon
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.geofence.MaxCircleCenters
import com.coblax.examlock.ui.geofence.MaxPolygonPoints
import com.coblax.examlock.ui.geofence.formatCoordinateText
import com.coblax.examlock.ui.geofence.geofenceDraftIssue
import com.coblax.examlock.ui.geofence.geofenceIssueMessage
import com.coblax.examlock.ui.geofence.parseRadiusMeters
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.primaryActionColors
import com.coblax.examlock.viewmodel.CustomQrDraftState

internal enum class CustomQrLocationMode { Anywhere, Circle, Polygon }

internal const val CustomQrLocationEditTestTag = "custom_qr_location_edit"

internal val CustomQrDraftState.locationMode: CustomQrLocationMode
    get() = when {
        !geofenceEnabled -> CustomQrLocationMode.Anywhere
        geofenceShapeTypeName == GeofenceShapeType.Polygon.name -> CustomQrLocationMode.Polygon
        else -> CustomQrLocationMode.Circle
    }

internal val CustomQrDraftState.locationShape: GeofenceShapeType
    get() = if (geofenceShapeTypeName == GeofenceShapeType.Polygon.name) {
        GeofenceShapeType.Polygon
    } else {
        GeofenceShapeType.Circle
    }

/** Circle centers, including a single center kept only in the older lat/lng fields. */
internal val CustomQrDraftState.circlePoints: List<GeofenceVertex>
    get() = geofenceCircleCenters.ifEmpty {
        if (geofenceCenterLat.isNotBlank() && geofenceCenterLng.isNotBlank()) {
            listOf(GeofenceVertex(geofenceCenterLat.trim(), geofenceCenterLng.trim()))
        } else {
            emptyList()
        }
    }

internal val CustomQrDraftState.locationPoints: List<GeofenceVertex>
    get() = if (locationShape == GeofenceShapeType.Polygon) polygonVertices else circlePoints

internal fun CustomQrDraftState.withLocationMode(mode: CustomQrLocationMode): CustomQrDraftState = when (mode) {
    CustomQrLocationMode.Anywhere -> copy(geofenceEnabled = false)
    CustomQrLocationMode.Circle -> copy(geofenceEnabled = true, geofenceShapeTypeName = GeofenceShapeType.Circle.name)
    CustomQrLocationMode.Polygon -> copy(geofenceEnabled = true, geofenceShapeTypeName = GeofenceShapeType.Polygon.name)
}

/** What still stops this draft's location from going into a QR, or null when it is fine. */
internal fun CustomQrDraftState.locationIssue() =
    if (!geofenceEnabled) null else geofenceDraftIssue(locationShape, locationPoints, geofenceRadiusMeters)

@Composable
internal fun CustomQrLocationStep(
    draft: CustomQrDraftState,
    onModeChange: (CustomQrLocationMode) -> Unit,
    onOpenEditor: () -> Unit,
    onClearPoints: () -> Unit
) {
    val colors = AppColors.current
    val tokens = ScopedUiTokens.current
    val mode = draft.locationMode
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radiusLarge))
            .background(colors.cardBg)
            .border(1.dp, colors.outlineStrong, RoundedCornerShape(tokens.radiusLarge))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = tr("Exam location", "Lokasi ujian"), color = colors.textPrimary, style = AppTextStyles.cardTitle)
            Text(
                text = tr(
                    "Choose where students may take this exam.",
                    "Pilih di mana siswa boleh mengerjakan ujian ini."
                ),
                color = colors.textSecondary,
                style = AppTextStyles.bodyCompact
            )
        }

        LocationModeSelector(selected = mode, onSelect = onModeChange)

        when (mode) {
            CustomQrLocationMode.Anywhere -> LocationNote(
                icon = Icons.Rounded.Info,
                tint = colors.brandText,
                text = tr(
                    "Students can sit the exam anywhere. CBX Lock will not ask for their location.",
                    "Siswa bisa ujian dari mana saja. CBX Lock tidak akan meminta lokasi mereka."
                )
            )

            CustomQrLocationMode.Circle,
            CustomQrLocationMode.Polygon -> {
                val shape = draft.locationShape
                val points = draft.locationPoints
                Text(
                    text = if (shape == GeofenceShapeType.Polygon) {
                        tr(
                            "Students must be inside the area drawn by its corners (3 to $MaxPolygonPoints).",
                            "Siswa harus berada di dalam area yang dibentuk sudut-sudutnya (3 sampai $MaxPolygonPoints)."
                        )
                    } else {
                        tr(
                            "Students must be inside one of the circles (up to $MaxCircleCenters centers, one radius).",
                            "Siswa harus berada di dalam salah satu lingkaran (maks. $MaxCircleCenters titik pusat, satu radius)."
                        )
                    },
                    color = colors.textSecondary,
                    style = AppTextStyles.bodyCompact
                )
                LocationSummary(draft = draft, shape = shape, points = points)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val action = primaryActionColors()
                    Button(
                        onClick = onOpenEditor,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(CustomQrLocationEditTestTag),
                        shape = RoundedCornerShape(tokens.radiusSmall),
                        colors = ButtonDefaults.buttonColors(containerColor = action.container, contentColor = action.content)
                    ) {
                        Icon(Icons.Rounded.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (points.isEmpty()) tr("Set the area", "Atur area") else tr("Edit the area", "Ubah area"),
                            style = AppTextStyles.button,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (points.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(tokens.radiusSmall),
                            border = BorderStroke(1.dp, colors.issueText.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = tr("Remove all points", "Hapus semua titik"),
                                tint = colors.issueText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.cardBg,
            title = { Text(tr("Remove all points?", "Hapus semua titik?"), color = colors.textPrimary, style = AppTextStyles.cardTitle) },
            text = {
                Text(
                    tr("The area for this QR will be empty.", "Area untuk QR ini akan kosong."),
                    color = colors.textSecondary,
                    style = AppTextStyles.bodyCompact
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearPoints()
                }) {
                    Text(tr("Remove all", "Hapus semua"), color = colors.issueText, style = AppTextStyles.button)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(tr("Cancel", "Batal"), color = colors.brandText, style = AppTextStyles.button)
                }
            }
        )
    }
}

@Composable
private fun LocationModeSelector(selected: CustomQrLocationMode, onSelect: (CustomQrLocationMode) -> Unit) {
    val colors = AppColors.current
    val tokens = ScopedUiTokens.current
    val action = primaryActionColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(tokens.radiusMedium))
            .background(colors.surfaceSoft)
            .border(1.dp, colors.outlineSubtle, RoundedCornerShape(tokens.radiusMedium))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Triple(CustomQrLocationMode.Anywhere, Icons.Rounded.Public, tr("Anywhere", "Bebas")),
            Triple(CustomQrLocationMode.Circle, Icons.Rounded.Adjust, tr("Circle", "Lingkaran")),
            Triple(CustomQrLocationMode.Polygon, Icons.Rounded.Pentagon, tr("Polygon", "Polygon"))
        ).forEach { (mode, icon, label) ->
            val isSelected = mode == selected
            val content = if (isSelected) action.content else colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(tokens.radiusSmall))
                    .background(if (isSelected) action.container else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mode) })
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Text(
                    text = label,
                    color = content,
                    style = AppTextStyles.label,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LocationSummary(draft: CustomQrDraftState, shape: GeofenceShapeType, points: List<GeofenceVertex>) {
    val colors = AppColors.current
    val tokens = ScopedUiTokens.current
    val uiLanguage = LocalUiLanguage.current
    val issue = geofenceDraftIssue(shape, points, draft.geofenceRadiusMeters)
    val radius = parseRadiusMeters(draft.geofenceRadiusMeters)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radiusMedium))
            .background(colors.surfaceSoft)
            .border(1.dp, colors.outlineMedium, RoundedCornerShape(tokens.radiusMedium))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LocationNote(
            icon = if (issue == null) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            tint = if (issue == null) colors.statusSafe else colors.statusWarn,
            text = when {
                issue != null -> geofenceIssueMessage(uiLanguage, shape, issue)
                shape == GeofenceShapeType.Polygon -> tr(
                    "Ready: area with ${points.size} corners.",
                    "Siap: area dengan ${points.size} sudut."
                )
                else -> tr(
                    "Ready: ${points.size} ${if (points.size == 1) "center" else "centers"}, radius $radius m.",
                    "Siap: ${points.size} titik pusat, radius $radius m."
                )
            }
        )
        val shown = points.take(SummaryPointLimit)
        shown.forEachIndexed { index, point ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${index + 1}",
                    color = colors.textSecondary,
                    style = AppTextStyles.label,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 20.dp)
                )
                Text(
                    text = formatCoordinateText(point).ifBlank { "–" },
                    color = colors.textPrimary,
                    style = AppTextStyles.diagnostic.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (points.size > shown.size) {
            Text(
                text = tr("+${points.size - shown.size} more", "+${points.size - shown.size} titik lagi"),
                color = colors.textSecondary,
                style = AppTextStyles.label
            )
        }
    }
}

@Composable
private fun LocationNote(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = text, color = AppColors.current.textPrimary, style = AppTextStyles.bodyCompact, modifier = Modifier.weight(1f))
    }
}

private const val SummaryPointLimit = 4
