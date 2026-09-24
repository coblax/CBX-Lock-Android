package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.ActionColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiStatusColors
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.currentUiMotionPolicy
import com.coblax.examlock.ui.theme.primaryActionColors
import com.coblax.examlock.ui.theme.resolveUiStatusColors

internal object PreparationUiTestTags {
    const val TopBar = "preparation_top_bar"
    const val HomeAction = "preparation_home_action"
    const val RefreshAction = "preparation_refresh_action"
    const val StatusCard = "preparation_status_card"
    const val AttentionCardPrefix = "preparation_attention_"
    const val CategoryTilePrefix = "preparation_tile_"
    const val StartBar = "preparation_start_bar"
    const val StartAction = "preparation_start_action"
    const val StartHint = "preparation_start_hint"
    const val DetailSheet = "preparation_detail_sheet"
    const val DetailSheetScrim = "preparation_detail_sheet_scrim"
    const val DetailSheetClose = "preparation_detail_sheet_close"
}

/** Content never stretches past this on tablets; long lines are hard to scan. */
internal val PreparationContentMaxWidth = 640.dp

internal fun PreparationCategory.icon(): ImageVector {
    return when (this) {
        PreparationCategory.DeviceSetup -> Icons.Rounded.Settings
        PreparationCategory.Connectivity -> Icons.Rounded.Wifi
        PreparationCategory.DeviceHealth -> Icons.Rounded.HealthAndSafety
        PreparationCategory.RuntimeInteraction -> Icons.Rounded.AccessibilityNew
        PreparationCategory.DeviceIntegrity -> Icons.Rounded.VerifiedUser
        PreparationCategory.Clipboard -> Icons.Rounded.ContentPaste
        PreparationCategory.Location -> Icons.Rounded.LocationOn
        PreparationCategory.DeviceLock -> Icons.Rounded.PhonelinkLock
        PreparationCategory.RuntimeSecurity -> Icons.Rounded.Security
    }
}

internal fun PreparationTone.uiStatusTone(): UiStatusTone = when (this) {
    PreparationTone.Clear -> UiStatusTone.Success
    PreparationTone.Warning -> UiStatusTone.Warning
    PreparationTone.Blocking -> UiStatusTone.Danger
}

@Composable
internal fun preparationStatusColors(tone: UiStatusTone): UiStatusColors {
    val colors = AppColors.current
    return remember(colors, tone) { resolveUiStatusColors(colors, tone) }
}

@Composable
internal fun preparationToneLabel(status: PreparationCategoryStatus): String = when (status.tone) {
    PreparationTone.Clear -> tr("Passed", "Aman")
    PreparationTone.Blocking -> tr(
        if (status.blockingCount == 1) "1 issue" else "${status.blockingCount} issues",
        "${status.blockingCount} masalah"
    )
    PreparationTone.Warning -> tr("Suggestion", "Saran")
}

// ──────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────

@Composable
internal fun PreparationTopBar(
    examTitle: String,
    isRefreshing: Boolean,
    onBackHome: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag(PreparationUiTestTags.TopBar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackHome,
            modifier = Modifier
                .size(ScopedUiTokens.current.touchTarget)
                .testTag(PreparationUiTestTags.HomeAction)
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = tr("Back to home", "Kembali ke menu utama"),
                tint = AppColors.current.brandText
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .semantics(mergeDescendants = true) { heading() }
        ) {
            Text(
                text = tr("Exam preparation", "Persiapan ujian"),
                color = AppColors.current.textSecondary,
                style = AppTextStyles.label
            )
            Text(
                text = examTitle,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.cardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Stays enabled while refreshing: the cooldown queues extra taps instead of
        // dropping them, and a disabled control reads as broken to screen readers.
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(ScopedUiTokens.current.touchTarget)
                .testTag(PreparationUiTestTags.RefreshAction)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.current.brandText
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = tr("Check all again", "Cek ulang semua"),
                    tint = AppColors.current.brandText
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Overall status
// ──────────────────────────────────────────────────────────────

@Composable
internal fun PreparationStatusCard(
    overview: PreparationOverview,
    hasBypassIndicators: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val motionPolicy = currentUiMotionPolicy()
    val overallState = overview.overallState
    val tone = when (overallState) {
        PreparationOverallState.Scanning -> UiStatusTone.Info
        PreparationOverallState.Blocked -> UiStatusTone.Danger
        PreparationOverallState.ReadyWithWarnings,
        PreparationOverallState.Ready -> UiStatusTone.Success
    }
    val colors = preparationStatusColors(tone)
    val blockingCount = overview.blockingCount
    val warningCount = overview.warningCount
    val title = when (overallState) {
        PreparationOverallState.Scanning -> tr("Checking your device…", "Memeriksa perangkat…")
        PreparationOverallState.Blocked -> if (blockingCount > 0) {
            tr(
                if (blockingCount == 1) "1 item must be fixed" else "$blockingCount items must be fixed",
                "$blockingCount hal wajib diperbaiki"
            )
        } else {
            tr("Not ready yet", "Belum siap")
        }
        PreparationOverallState.ReadyWithWarnings,
        PreparationOverallState.Ready -> tr("Ready to start", "Siap mulai ujian")
    }
    val message = when (overallState) {
        PreparationOverallState.Scanning -> tr(
            "Security checks are running. This takes a few seconds.",
            "Pemeriksaan keamanan sedang berjalan. Tunggu beberapa detik."
        )
        PreparationOverallState.Blocked -> buildString {
            append(
                tr(
                    "Fix them top to bottom; status is re-checked automatically.",
                    "Perbaiki dari atas ke bawah; status dicek ulang otomatis."
                )
            )
            if (overview.scanPending) {
                append(' ')
                append(tr("Security scan is still running.", "Pemindaian keamanan masih berjalan."))
            }
        }
        PreparationOverallState.ReadyWithWarnings -> tr(
            "All required checks passed. The $warningCount suggestion(s) below are optional.",
            "Semua pemeriksaan wajib lulus. $warningCount saran di bawah bersifat opsional."
        )
        PreparationOverallState.Ready -> tr(
            "All checks passed. This device is ready for the exam.",
            "Semua pemeriksaan lulus. Perangkat siap dipakai ujian."
        )
    }
    val shape = RoundedCornerShape(tokens.radiusLarge)
    val total = overview.categories.size

    Column(
        modifier = modifier
            .testTag(PreparationUiTestTags.StatusCard)
            .clip(shape)
            .background(colors.containerColor)
            .border(1.dp, colors.borderColor.copy(alpha = 0.45f), shape)
            .semantics(mergeDescendants = true) {}
            .padding(tokens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(tokens.spaceSmall)
    ) {
        // With an enlarged system font the side-by-side layout squeezes the message into a
        // one-word-per-line column, so the message moves under the ring and title instead.
        val largeFont = LocalDensity.current.fontScale > 1.3f
        val ring: @Composable () -> Unit = {
            Box(
                modifier = Modifier.size(if (largeFont) 40.dp else 56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (overallState == PreparationOverallState.Scanning && motionPolicy.enabled) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.contentColor,
                        strokeWidth = 5.dp
                    )
                } else {
                    // Static ring: no running animation on low-RAM or reduced-motion devices.
                    CircularProgressIndicator(
                        progress = {
                            if (total == 0) 1f else overview.clearCategoryCount.toFloat() / total
                        },
                        modifier = Modifier.fillMaxSize(),
                        color = colors.contentColor,
                        trackColor = colors.contentColor.copy(alpha = 0.18f),
                        strokeWidth = 5.dp
                    )
                }
                if (overallState != PreparationOverallState.Scanning && !largeFont) {
                    Text(
                        text = "${overview.clearCategoryCount}/$total",
                        color = colors.contentColor,
                        style = AppTextStyles.label.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        val titleText: @Composable () -> Unit = {
            Text(
                text = title,
                color = colors.contentColor,
                style = AppTextStyles.cardTitle
            )
        }
        val messageText: @Composable () -> Unit = {
            Text(
                text = message,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.bodyCompact
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spaceMedium)
        ) {
            ring()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                titleText()
                if (!largeFont) {
                    messageText()
                }
            }
        }
        if (largeFont) {
            messageText()
        }
        if (hasBypassIndicators) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = tr(
                        "Admin bypass is active. Everything is still logged.",
                        "Bypass admin aktif. Semua tetap dicatat."
                    ),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
        }
    }
}

@Composable
internal fun PreparationSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .padding(top = 4.dp)
            .semantics { heading() },
        color = AppColors.current.textSecondary,
        style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold)
    )
}

// ──────────────────────────────────────────────────────────────
// Issues and their fixes
// ──────────────────────────────────────────────────────────────

@Composable
internal fun PreparationIssueRow(
    issue: PreparationIssue,
    modifier: Modifier = Modifier
) {
    val colors = preparationStatusColors(
        if (issue.blocking) UiStatusTone.Danger else UiStatusTone.Warning
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (issue.blocking) Icons.Rounded.ErrorOutline else Icons.Rounded.Warning,
            contentDescription = if (issue.blocking) {
                tr("Required", "Wajib")
            } else {
                tr("Suggestion", "Saran")
            },
            tint = colors.contentColor,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.title,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold)
            )
            issue.message?.let { message ->
                Text(
                    text = message,
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
        }
    }
}

@Composable
internal fun PreparationQuickFixButton(
    action: PreparationQuickFixAction,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val shape = RoundedCornerShape(tokens.radiusMedium)
    val enabled = action.enabled && !action.loading
    val content: @Composable RowScope.() -> Unit = {
        if (action.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = action.buttonLabel(),
            style = AppTextStyles.button,
            textAlign = TextAlign.Center
        )
    }
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = tokens.touchTarget)
    val padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    if (primary) {
        val primaryColors = primaryActionColors()
        Button(
            onClick = action.onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColors.container,
                contentColor = primaryColors.content,
                disabledContainerColor = colors.surfaceSoft,
                disabledContentColor = colors.textSecondary
            ),
            contentPadding = padding,
            content = content
        )
    } else {
        OutlinedButton(
            onClick = action.onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.brandText,
                disabledContentColor = colors.textSecondary
            ),
            border = BorderStroke(1.dp, colors.outline),
            contentPadding = padding,
            content = content
        )
    }
}

/**
 * Notices and fix buttons for one category. The low-RAM render budget still applies,
 * per category: the ultra profile shows only the single most important button.
 */
@Composable
internal fun PreparationCategoryActions(
    actions: List<PreparationQuickFixAction>,
    hasGlobalBlockingIssues: Boolean,
    emphasizeFirst: Boolean,
    modifier: Modifier = Modifier
) {
    val lowRamProfile = LocalLowRamProfile.current
    val display = remember(actions, lowRamProfile, hasGlobalBlockingIssues) {
        selectPreparationQuickFixActionsForDisplay(
            actions = actions,
            lowRamProfile = lowRamProfile,
            hasGlobalBlockingIssues = hasGlobalBlockingIssues
        )
    }
    val buttons = remember(display) {
        buildList {
            display.primary?.takeUnless { it.isNotice }?.let(::add)
            addAll(display.blocking)
            addAll(display.warnings)
        }
            .distinctBy { it.code }
            // A button disabled by screen pinning must never be the one that looks tappable.
            .sortedBy { !it.enabled }
    }
    // Settings shortcuts are disabled while screen pinning is on; one note explains that
    // instead of a stack of greyed-out buttons with the same suffix.
    val pinningLocked = buttons.count { !it.enabled && it.opensExternalSettings }
    val visibleButtons = buttons.filterNot { !it.enabled && it.opensExternalSettings }
    if (display.notices.isEmpty() && buttons.isEmpty()) {
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        display.notices.forEach { notice ->
            PreparationInfoNote(text = notice.text)
        }
        visibleButtons.forEachIndexed { index, action ->
            PreparationQuickFixButton(
                action = action,
                primary = emphasizeFirst && index == 0 && action.enabled
            )
        }
        if (pinningLocked > 0) {
            PreparationInfoNote(
                text = tr(
                    "$pinningLocked fix(es) open Android Settings, which is locked while screen pinning is on. Turn off screen pinning first to use them.",
                    "$pinningLocked perbaikan membuka Setelan Android, yang terkunci saat screen pinning aktif. Matikan screen pinning dulu untuk memakainya."
                )
            )
        }
    }
}

@Composable
private fun PreparationInfoNote(text: String) {
    val noticeColors = preparationStatusColors(UiStatusTone.Info)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ScopedUiTokens.current.radiusSmall))
            .background(noticeColors.containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = noticeColors.contentColor,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            color = AppColors.current.textPrimary,
            style = AppTextStyles.diagnostic
        )
    }
}

@Composable
internal fun PreparationCategoryBadge(
    category: PreparationCategory,
    colors: UiStatusColors,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.containerColor)
            .border(1.dp, colors.borderColor.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = colors.contentColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
internal fun PreparationAttentionCard(
    status: PreparationCategoryStatus,
    hasGlobalBlockingIssues: Boolean,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val uiLanguage = LocalUiLanguage.current
    val colors = preparationStatusColors(status.tone.uiStatusTone())
    val shape = RoundedCornerShape(tokens.radiusLarge)
    Column(
        modifier = modifier
            .testTag(PreparationUiTestTags.AttentionCardPrefix + status.category.key)
            .clip(shape)
            .background(AppColors.current.cardBg)
            .border(1.dp, colors.borderColor.copy(alpha = 0.45f), shape)
            .padding(tokens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(tokens.spaceMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreparationCategoryBadge(category = status.category, colors = colors)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { heading() }
            ) {
                Text(
                    text = status.category.title(uiLanguage),
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.cardTitle
                )
                Text(
                    text = if (status.tone == PreparationTone.Blocking) {
                        tr("Must be fixed", "Wajib diperbaiki")
                    } else {
                        tr("Optional", "Opsional")
                    },
                    color = colors.contentColor,
                    style = AppTextStyles.label
                )
            }
            TextButton(
                onClick = onOpenDetails,
                modifier = Modifier.heightIn(min = tokens.touchTarget)
            ) {
                Text(
                    text = tr("Details", "Detail"),
                    color = AppColors.current.brandText,
                    style = AppTextStyles.button
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spaceSmall)) {
            status.issues.forEach { issue ->
                PreparationIssueRow(issue = issue)
            }
        }
        PreparationCategoryActions(
            actions = status.actions,
            hasGlobalBlockingIssues = hasGlobalBlockingIssues,
            emphasizeFirst = status.tone == PreparationTone.Blocking
        )
    }
}

// ──────────────────────────────────────────────────────────────
// All checks grid
// ──────────────────────────────────────────────────────────────

@Composable
private fun PreparationCategoryTile(
    status: PreparationCategoryStatus,
    scanPending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val uiLanguage = LocalUiLanguage.current
    // Until the first scan finishes, "no issue yet" is not the same as "passed".
    val pending = scanPending && status.tone == PreparationTone.Clear
    val colors = preparationStatusColors(
        if (pending) UiStatusTone.Neutral else status.tone.uiStatusTone()
    )
    val shape = RoundedCornerShape(tokens.radiusMedium)
    Column(
        modifier = modifier
            .testTag(PreparationUiTestTags.CategoryTilePrefix + status.category.key)
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(colors.containerColor)
            .border(1.dp, colors.borderColor.copy(alpha = 0.35f), shape)
            .clickable(
                role = Role.Button,
                onClickLabel = tr("Open details", "Buka detail"),
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = status.category.icon(),
            contentDescription = null,
            tint = colors.contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = status.category.title(uiLanguage),
            color = AppColors.current.textPrimary,
            style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = when {
                    pending -> Icons.Rounded.Sync
                    status.tone == PreparationTone.Clear -> Icons.Rounded.CheckCircle
                    status.tone == PreparationTone.Warning -> Icons.Rounded.Warning
                    else -> Icons.Rounded.ErrorOutline
                },
                contentDescription = null,
                tint = colors.contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (pending) tr("Checking…", "Memeriksa…") else preparationToneLabel(status),
                color = colors.contentColor,
                style = AppTextStyles.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tiles in rows of up to three. Columns drop to two or one when the phone is narrow or
 * the system font is enlarged, so labels never get squeezed into unreadable stubs.
 * Plain rows instead of a lazy grid: nine tiles, inside an already-lazy list.
 */
@Composable
internal fun PreparationCategoryGrid(
    categories: List<PreparationCategoryStatus>,
    onOpenCategory: (PreparationCategory) -> Unit,
    modifier: Modifier = Modifier,
    scanPending: Boolean = false
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier) {
        val minTileWidth = 96.dp * fontScale.coerceIn(1f, 2f)
        val columns = (maxWidth / minTileWidth).toInt().coerceIn(1, 3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { status ->
                        PreparationCategoryTile(
                            status = status,
                            scanPending = scanPending,
                            onClick = { onOpenCategory(status.category) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Start bar
// ──────────────────────────────────────────────────────────────

@Composable
internal fun PreparationStartBar(
    overview: PreparationOverview,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    hasBypassIndicators: Boolean,
    onStartExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val canStart = overview.canStartExam
    val busy = isStartingExam || webViewSessionResetInFlight
    val enabled = canStart && !busy
    val blockingCount = overview.blockingCount
    val hint: String? = when {
        webViewSessionResetInFlight -> tr(
            "Preparing a clean exam browser…",
            "Menyiapkan browser ujian yang bersih…"
        )
        busy -> null
        canStart && hasBypassIndicators -> tr("Admin bypass is active.", "Bypass admin aktif.")
        canStart -> null
        blockingCount > 0 -> tr(
            if (blockingCount == 1) "Fix 1 required item first." else "Fix $blockingCount required items first.",
            "Selesaikan $blockingCount hal wajib dulu."
        )
        else -> tr(
            "Waiting for security checks to finish…",
            "Menunggu pemeriksaan keamanan selesai…"
        )
    }
    val buttonColors = when {
        hasBypassIndicators -> ActionColors(container = colors.gold, content = colors.blueDeep)
        colors.isDark -> ActionColors(container = colors.safeEmphasis, content = colors.background)
        else -> ActionColors(container = colors.safeStrong, content = colors.onDark)
    }
    val label = when {
        webViewSessionResetInFlight -> tr("Preparing…", "Menyiapkan…")
        isStartingExam -> tr("Starting…", "Memulai…")
        else -> tr("Start exam", "Mulai ujian")
    }

    // Background first, insets inside: the bar's surface also fills the gesture/nav bar
    // strip instead of leaving the list showing through underneath it.
    Column(
        modifier = modifier
            .testTag(PreparationUiTestTags.StartBar)
            .fillMaxWidth()
            .background(colors.cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outlineMedium)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hint != null) {
                Text(
                    text = hint,
                    modifier = Modifier
                        .widthIn(max = PreparationContentMaxWidth)
                        .fillMaxWidth()
                        .testTag(PreparationUiTestTags.StartHint),
                    color = colors.textSecondary,
                    style = AppTextStyles.diagnostic,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onStartExam,
                enabled = enabled,
                modifier = Modifier
                    .widthIn(max = PreparationContentMaxWidth)
                    .fillMaxWidth()
                    .heightIn(min = tokens.primaryActionHeight)
                    .testTag(PreparationUiTestTags.StartAction),
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColors.container,
                    contentColor = buttonColors.content,
                    // Readable in both themes; a faded green looked tappable but was not.
                    disabledContainerColor = colors.surfaceSoft,
                    disabledContentColor = colors.textSecondary
                ),
                // Outline keeps the locked button visible against the bar in dark mode.
                border = if (enabled) null else BorderStroke(1.dp, colors.outline),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when {
                    busy -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    enabled -> Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = AppTextStyles.button.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
