package com.coblax.examlock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.SecretAdminTab
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.primaryActionColors

internal object SecretAdminUiTestTags {
    const val TopBar = "secret_admin_top_bar"
    const val BackAction = "secret_admin_back"
    const val TabPrefix = "secret_admin_tab_"
    const val ApplyBar = "secret_admin_apply_bar"
    const val ApplyAction = "secret_admin_apply"
    const val RevertAction = "secret_admin_revert"
    const val OverrideRowPrefix = "secret_admin_override_"
    const val DisableAllOverrides = "secret_admin_disable_all_overrides"
}

/** Content never stretches past this on tablets; form rows are hard to scan when wide. */
internal val SecretAdminContentMaxWidth = 640.dp

/**
 * The tabs shown, in order. Location is folded into Setup: it only held the Direct Link
 * location policy, which belongs next to the Direct Link it applies to.
 */
internal val SecretAdminVisibleTabs: List<SecretAdminTab> = listOf(
    SecretAdminTab.Setup,
    SecretAdminTab.Security,
    SecretAdminTab.Overrides,
    SecretAdminTab.Diagnostics
)

/** A persisted tab name that is no longer shown (or is unknown) opens its new home. */
internal fun resolveSecretAdminTab(name: String): SecretAdminTab {
    val tab = SecretAdminTab.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: SecretAdminTab.Setup
    return if (tab == SecretAdminTab.Location) SecretAdminTab.Setup else tab
}

@Composable
internal fun SecretAdminTopBar(
    subtitle: String,
    activeOverrideCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    BoxWithConstraints(modifier = modifier) {
        // On a narrow screen the count alone keeps the title readable; the tab badge and
        // the semantics still say what it counts.
        val compactBadge = isNarrow(maxWidth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SecretAdminUiTestTags.TopBar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(tokens.touchTarget)
                    .testTag(SecretAdminUiTestTags.BackAction)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = tr("Back to main menu", "Kembali ke menu utama"),
                    tint = colors.brandText
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .semantics(mergeDescendants = true) { heading() }
            ) {
                Text(
                    text = tr("Secret Admin", "Admin Rahasia"),
                    color = colors.textPrimary,
                    style = AppTextStyles.cardTitle.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    style = AppTextStyles.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (activeOverrideCount > 0) {
                StatusBadge(
                    label = if (compactBadge) {
                        activeOverrideCount.toString()
                    } else {
                        tr("$activeOverrideCount bypass", "$activeOverrideCount bypass")
                    },
                    tone = UiStatusTone.Warning,
                    semanticLabel = tr(
                        "$activeOverrideCount security bypass active",
                        "$activeOverrideCount bypass keamanan aktif"
                    )
                )
            }
        }
    }
}

/**
 * Equal-width tabs that always fit: no sideways scrolling to find the last one. All
 * labels share one font size, stepped down until the longest fits, so a narrow phone
 * with a large font gets smaller but matching labels instead of one cut-off tab.
 */
@Composable
internal fun SecretAdminTabs(
    selected: SecretAdminTab,
    activeOverrideCount: Int,
    onSelect: (SecretAdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val primary = primaryActionColors()
    val labels = SecretAdminVisibleTabs.map { tab ->
        when (tab) {
            SecretAdminTab.Setup -> tr("Setup", "Setup")
            SecretAdminTab.Security -> tr("Security", "Keamanan")
            SecretAdminTab.Overrides -> tr("Bypass", "Bypass")
            SecretAdminTab.Diagnostics -> tr("Diagnostics", "Diagnostik")
            SecretAdminTab.Location -> tr("Location", "Lokasi")
        }
    }
    val baseStyle = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabCount = SecretAdminVisibleTabs.size
        // Outer padding 4dp each side, 4dp gaps, 4dp inner padding each side of a tab.
        val labelWidth = (maxWidth - 8.dp - 4.dp * (tabCount - 1)) / tabCount - 8.dp
        // Returns the font size (sp) at which every label fits, or null if none does.
        fun fittingSize(reservedForBadge: Dp): Float? {
            var size = baseStyle.fontSize.value
            while (size >= MinTabFontSp) {
                val fits = labels.withIndex().all { (index, text) ->
                    val reserved = if (SecretAdminVisibleTabs[index] == SecretAdminTab.Overrides) reservedForBadge else 0.dp
                    val width = textMeasurer.measure(text, baseStyle.copy(fontSize = size.sp), maxLines = 1).size.width
                    width <= with(density) { (labelWidth - reserved).toPx() }
                }
                if (fits) return size
                size -= 0.5f
            }
            return null
        }
        // The inline count is kept only while it costs at most a small shrink; otherwise it
        // becomes a corner dot (the top bar still shows the number).
        val (fontSize, dotBadge) = remember(labels, labelWidth, activeOverrideCount, density, baseStyle) {
            val plain = fittingSize(0.dp) ?: MinTabFontSp
            val withBadge = if (activeOverrideCount > 0) fittingSize(InlineBadgeWidth) else null
            when {
                activeOverrideCount == 0 -> plain.sp to false
                withBadge != null && withBadge >= plain - 1f -> withBadge.sp to false
                else -> plain.sp to true
            }
        }
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
            SecretAdminVisibleTabs.forEachIndexed { index, tab ->
                val isSelected = tab == selected
                val contentColor = if (isSelected) primary.content else colors.textSecondary
                val label = labels[index]
                val showDot = dotBadge && tab == SecretAdminTab.Overrides && activeOverrideCount > 0
                val dotColor = colors.gold
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = tokens.touchTarget)
                        .clip(RoundedCornerShape(tokens.radiusSmall))
                        .background(if (isSelected) primary.container else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(tab) }
                        )
                        .testTag(SecretAdminUiTestTags.TabPrefix + tab.name)
                        .then(
                            if (showDot) {
                                Modifier.drawWithContent {
                                    drawContent()
                                    drawCircle(
                                        color = dotColor,
                                        radius = 4.dp.toPx(),
                                        center = Offset(size.width - 7.dp.toPx(), 7.dp.toPx())
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = contentColor,
                        style = baseStyle.copy(fontSize = fontSize),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tab == SecretAdminTab.Overrides && activeOverrideCount > 0 && !dotBadge) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.radiusPill))
                                .background(colors.gold)
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeOverrideCount.toString(),
                                color = colors.blueDeep,
                                style = baseStyle.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MinTabFontSp = 9f

/** Room the inline count pill takes next to "Bypass", including its gap. */
private val InlineBadgeWidth = 24.dp

/** Below this width (in font-scaled dp) rows switch to their narrow arrangement. */
private val SecretAdminNarrowWidth = 340.dp

@Composable
private fun isNarrow(maxWidth: Dp): Boolean =
    maxWidth / LocalDensity.current.fontScale.coerceAtLeast(1f) < SecretAdminNarrowWidth

/**
 * One titled group. [padded] content gets the card padding (forms); unpadded content is
 * a list of full-width rows separated by [SecretAdminRowDivider].
 */
@Composable
internal fun SecretAdminSection(
    title: String,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val shape = RoundedCornerShape(tokens.radiusLarge)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                color = colors.textSecondary,
                style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold)
            )
            trailing?.invoke(this)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.cardBg)
                .border(1.dp, colors.outlineSubtle, shape)
                .then(if (padded) Modifier.padding(14.dp) else Modifier),
            verticalArrangement = if (padded) Arrangement.spacedBy(12.dp) else Arrangement.Top,
            content = content
        )
    }
}

@Composable
internal fun SecretAdminRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(AppColors.current.outlineSubtle)
    )
}

/** A labelled field: the label sits inside the outline, so no extra title line is needed. */
@Composable
internal fun SecretAdminTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = AppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = placeholder?.let { hint ->
            { Text(text = hint, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        supportingText = supportingText?.let { text ->
            { Text(text = text, style = AppTextStyles.diagnostic) }
        },
        textStyle = AppTextStyles.bodyCompact.copy(color = colors.textPrimary),
        singleLine = true,
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSoft,
            unfocusedContainerColor = colors.surfaceSoft,
            focusedBorderColor = colors.blue,
            unfocusedBorderColor = colors.outline,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.brandText,
            unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.blue,
            focusedSupportingTextColor = colors.textSecondary,
            unfocusedSupportingTextColor = colors.textSecondary
        )
    )
}

/**
 * A switch row. The whole row toggles, so the target is the full width, not the 48dp
 * switch. [highlighted] tints an active bypass so it stands out in a long list.
 */
@Composable
internal fun SecretAdminSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    testTag: String? = null
) {
    val colors = AppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (highlighted) colors.gold.copy(alpha = 0.10f) else Color.Transparent)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold)
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    color = colors.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
        }
        Switch(
            checked = checked,
            // The row owns the toggle; a second handler here would double-fire.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (highlighted) colors.goldDark else colors.blue,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surfaceSoft,
                uncheckedBorderColor = colors.outline
            )
        )
    }
}

/** "Label ........ value" with the value as a coloured status badge when [tone] is set. */
@Composable
internal fun SecretAdminInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: UiStatusTone? = null
) {
    val colors = AppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = colors.textSecondary,
            style = AppTextStyles.bodyCompact
        )
        if (tone != null) {
            StatusBadge(label = value.ifBlank { "-" }, tone = tone)
        } else {
            Text(
                text = value.ifBlank { "-" },
                modifier = Modifier.widthIn(max = 220.dp),
                color = colors.textPrimary,
                style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A one-line warning/info strip inside a section. */
@Composable
internal fun SecretAdminNote(
    text: String,
    tone: UiStatusTone = UiStatusTone.Info,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    val accent = when (tone) {
        UiStatusTone.Warning -> colors.goldDark
        UiStatusTone.Danger -> colors.statusDanger
        UiStatusTone.Success -> colors.statusSafe
        else -> colors.brandText
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (tone == UiStatusTone.Warning || tone == UiStatusTone.Danger) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp)
            )
        }
        Text(
            text = text,
            color = colors.textSecondary,
            style = AppTextStyles.diagnostic
        )
    }
}

/**
 * Docked at the bottom only while there is something to apply: the draft is edited
 * freely and nothing reaches the exam until Apply.
 */
@Composable
internal fun SecretAdminApplyBar(
    message: String,
    isError: Boolean,
    canApply: Boolean,
    applying: Boolean,
    onRevert: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val primary = primaryActionColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SecretAdminUiTestTags.ApplyBar)
            .background(colors.cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isError) colors.statusDanger.copy(alpha = 0.5f) else colors.outlineMedium)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            val messageText: @Composable (Modifier) -> Unit = { textModifier ->
                Text(
                    text = message,
                    modifier = textModifier,
                    color = if (isError) colors.statusDanger else colors.textSecondary,
                    style = AppTextStyles.diagnostic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val actions: @Composable () -> Unit = {
                TextButton(
                    onClick = onRevert,
                    enabled = canApply && !applying,
                    modifier = Modifier
                        .heightIn(min = tokens.touchTarget)
                        .testTag(SecretAdminUiTestTags.RevertAction)
                ) {
                    Text(
                        text = tr("Discard", "Batal"),
                        style = AppTextStyles.button,
                        color = if (canApply && !applying) colors.brandText else colors.textMuted
                    )
                }
                Button(
                    onClick = onApply,
                    enabled = canApply && !applying,
                    modifier = Modifier
                        .heightIn(min = tokens.touchTarget)
                        .testTag(SecretAdminUiTestTags.ApplyAction),
                    shape = RoundedCornerShape(tokens.radiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary.container,
                        contentColor = primary.content,
                        disabledContainerColor = colors.surfaceSoft,
                        disabledContentColor = colors.textSecondary
                    )
                ) {
                    if (applying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = primary.content
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = tr("Apply", "Terapkan"), style = AppTextStyles.button)
                }
            }
            // Narrow or large-font: the message gets its own line instead of three cramped ones.
            if (isNarrow(maxWidth)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    messageText(Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    messageText(Modifier.weight(1f))
                    actions()
                }
            }
        }
    }
}
