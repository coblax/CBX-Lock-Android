package com.example.coblaxexamlock.ui.exam

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeStateUpdateTest {
    @Test
    fun mutableStateSetIfChangedSkipsEqualValue() {
        val state = mutableStateOf("ready")

        assertFalse(state.setIfChanged("ready"))
        assertEquals("ready", state.value)
        assertTrue(state.setIfChanged("blocking"))
        assertEquals("blocking", state.value)
    }

    @Test
    fun mutableIntStateSetIfChangedSkipsEqualValue() {
        val state = mutableIntStateOf(1)

        assertFalse(state.setIfChanged(1))
        assertEquals(1, state.intValue)
        assertTrue(state.setIfChanged(2))
        assertEquals(2, state.intValue)
    }
}
