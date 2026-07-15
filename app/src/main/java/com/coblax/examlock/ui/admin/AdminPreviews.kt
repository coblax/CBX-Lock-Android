package com.coblax.examlock.ui.admin

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.R
import com.coblax.examlock.ui.theme.COBLAXEXAMLOCKTheme

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@Preview(showBackground = true)
@Composable
fun ExamLockHomeScreenPreview() {
    COBLAXEXAMLOCKTheme {
        ExamLockHomeScreen(
            uiLanguage = UiLanguage.English,
            onUiLanguageChange = {},
            onScanExam = {},
            onOpenAdmin = {},
            onOpenFastExam = {},
            directLinkLabel = "EXAM_SKANSATP",
            onSecretTap = {},
            onOpenPerformanceProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomQrAdminScreenPreview() {
    COBLAXEXAMLOCKTheme {
        CustomQrAdminScreen(showSaveToDirectLinkOption = true, onBack = {})
    }
}
