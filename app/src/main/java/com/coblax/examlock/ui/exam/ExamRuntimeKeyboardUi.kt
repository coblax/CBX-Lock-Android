package com.coblax.examlock.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockOutlineStrong
import java.util.Locale

@Composable
internal fun ExamBuiltInKeyboardPanel(
    isShiftEnabled: Boolean,
    onTextKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xFFF2F5FA))
            .border(1.dp, LockOutline.copy(alpha = 0.70f), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(4.dp)
                    .flatPill(containerColor = LockOutline)
            )

            Text(
                text = tr("Internal Keyboard", "Keyboard Internal"),
                color = LockTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            keyboardRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { key ->
                        ExamBuiltInKeyboardKey(
                            label = if (isShiftEnabled) key.uppercase(Locale.US) else key,
                            onClick = { onTextKey(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ExamBuiltInKeyboardKey(
                    icon = Icons.Rounded.ArrowUpward,
                    label = "",
                    onClick = onShiftToggle,
                    modifier = Modifier.weight(1.05f),
                    isAccent = isShiftEnabled
                )
                listOf("z", "x", "c", "v", "b", "n", "m").forEach { key ->
                    ExamBuiltInKeyboardKey(
                        label = if (isShiftEnabled) key.uppercase(Locale.US) else key,
                        onClick = { onTextKey(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExamBuiltInKeyboardKey(
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    label = "",
                    onClick = onBackspace,
                    modifier = Modifier.weight(1.05f),
                    isSecondary = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("@", ".", "-", "/").forEach { key ->
                    ExamBuiltInKeyboardKey(
                        label = key,
                        onClick = { onTextKey(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExamBuiltInKeyboardKey(
                    icon = Icons.Rounded.SpaceBar,
                    label = "",
                    onClick = onSpace,
                    modifier = Modifier.weight(3.1f),
                    isSecondary = true
                )
                ExamBuiltInKeyboardKey(
                    icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                    label = "",
                    onClick = onEnter,
                    modifier = Modifier.weight(1.05f),
                    isAccent = true
                )
            }
        }
    }
}

@Composable
internal fun ExamBuiltInKeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isAccent: Boolean = false,
    isSecondary: Boolean = false
) {
    val backgroundColor = when {
        isAccent -> LockBlue
        isSecondary -> Color(0xFFE6EBF3)
        else -> Color.White
    }
    val contentColor = if (isAccent) LockOnDark else LockTextPrimary

    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(backgroundColor)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null && label.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
