package com.coblax.examlock.ui.exam

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StartExamPreflightCancelTest {
    private fun state() = StartExamPreflightUiState(
        visible = mutableStateOf(false),
        step = mutableStateOf(StartExamPreflightStep.Idle),
        detail = mutableStateOf(null),
        startedAtElapsedMs = mutableStateOf(null),
        slowHintVisible = mutableStateOf(false)
    )

    /** Cancel used to only hide the dialog; the start went on and could still open the exam. */
    @Test
    fun cancelStopsTheRunningStartAtItsNextStep() {
        val preflight = state()
        showStartExamPreflight(preflight, startedAtElapsedMs = 1L)
        preflight.throwIfCancelledByStudent()

        cancelStartExamPreflight(preflight)

        assertFalse(preflight.visible.value)
        val error = assertThrows(CancellationException::class.java) {
            preflight.throwIfCancelledByStudent()
        }
        assertEquals(StartExamCancelledByStudent, error.message)
    }

    @Test
    fun pressingStartAgainIsANewAttempt() {
        val preflight = state()
        showStartExamPreflight(preflight, startedAtElapsedMs = 1L)
        cancelStartExamPreflight(preflight)

        showStartExamPreflight(preflight, startedAtElapsedMs = 2L)

        assertTrue(preflight.visible.value)
        assertFalse(preflight.cancelRequested.value)
        preflight.throwIfCancelledByStudent()
    }

    /** A block or a finished start hides the dialog too, but that is not a cancel. */
    @Test
    fun hidingTheDialogIsNotACancel() {
        val preflight = state()
        showStartExamPreflight(preflight, startedAtElapsedMs = 1L)

        hideStartExamPreflight(preflight)

        assertFalse(preflight.cancelRequested.value)
        preflight.throwIfCancelledByStudent()
    }
}
