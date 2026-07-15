package com.coblax.examlock.ui.geofence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineStrong

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@Composable
internal fun CompactBackIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = tr("Back", "Kembali"),
            tint = LockBlueDeep,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun CompactInfoMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = label,
                color = LockTextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = LockTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
internal fun CompactCoordinateMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = label,
                color = LockTextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    color = LockTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = "-",
                                color = LockTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 11.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
internal fun CompactRadiusMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = tr("Radius (m)", "Radius (m)"),
                color = LockTextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    color = LockTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "100",
                                color = LockTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 11.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
internal fun MapTypeSelectorOverlay(
    selectedType: GeofenceMapType,
    onTypeSelected: (GeofenceMapType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeofenceMapType.entries.forEach { mapType ->
                val selected = mapType == selectedType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) LockBlue else LockSurfaceSoft)
                        .border(1.dp, if (selected) LockBlue else LockOutlineStrong, RoundedCornerShape(10.dp))
                        .clickable { onTypeSelected(mapType) }
                ) {
                    Text(
                        text = when (mapType) {
                            GeofenceMapType.Default -> tr("Default", "Default")
                            GeofenceMapType.Satellite -> tr("Satellite", "Satellite")
                            GeofenceMapType.Terrain -> tr("Terrain", "Terrain")
                        },
                        color = if (selected) LockOnDark else LockTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
