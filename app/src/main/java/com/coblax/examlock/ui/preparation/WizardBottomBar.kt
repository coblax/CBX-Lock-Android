package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.isExpandedLayout

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Bottom Navigation Bar
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Wizard bottom bar with adaptive max-width. On expanded (tablet) screens
 * the bar is constrained so it doesn't stretch edge-to-edge, and side
 * buttons are slightly wider with visible text labels.
 */
@Composable
internal fun WizardBottomBar(
    currentStepIndex: Int,
    totalSteps: Int,
    currentStepCompleted: Boolean,
    canStartExam: Boolean,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    startButtonColor: Color,
    startButtonContentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLastStep = currentStepIndex == totalSteps - 1
    val isFirstStep = currentStepIndex == 0
    val expanded = isExpandedLayout()
    val sideButtonWidth = if (expanded) 96.dp else 76.dp

    Column(
        modifier = modifier
            .then(
                if (expanded) Modifier.widthIn(max = UiTokens.ContentMaxWidthExpanded)
                else Modifier
            )
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(LockBlueDeep)
            .border(1.dp, LockBlueDeep.copy(alpha = 0.85f), RoundedCornerShape(UiTokens.RadiusLg))
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            Button(
                onClick = if (isFirstStep) onBackHome else onPrevious,
                modifier = Modifier
                    .width(sideButtonWidth)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.28f)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isFirstStep) Icons.Rounded.Home
                        else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFirstStep) "Menu" else tr("Back", "Kembali"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Middle button (Next or Start Exam on last step)
            if (isLastStep) {
                val startEnabled = canStartExam && !(isStartingExam || webViewSessionResetInFlight)
                Button(
                    onClick = onStartExam,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = startEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = startButtonColor,
                        contentColor = startButtonContentColor,
                        disabledContainerColor = startButtonColor.copy(alpha = 0.85f),
                        disabledContentColor = startButtonContentColor
                    )
                ) {
                    Text(
                        text = if (webViewSessionResetInFlight) {
                            tr("PREPARING...", "MENYIAPKAN...")
                        } else if (isStartingExam) {
                            tr("STARTING...", "MEMULAI...")
                        } else {
                            tr("START EXAM", "MULAI UJIAN")
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStepCompleted) WizardGreen else LockBlue,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentStepCompleted) {
                                tr("NEXT STEP", "LANGKAH BERIKUT")
                            } else {
                                tr("SKIP â†’", "LEWATI â†’")
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Step counter
            Button(
                onClick = if (isLastStep) onBackHome else onNext,
                modifier = Modifier
                    .width(sideButtonWidth)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.28f)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLastStep) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Menu",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${currentStepIndex + 1}/$totalSteps",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Blocking reason text
        if (isLastStep && !canStartExam) {
            Text(
                text = "âš  " + tr(
                    "Fix all blocking issues to start the exam",
                    "Perbaiki semua masalah blocking untuk memulai ujian"
                ),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }
}
