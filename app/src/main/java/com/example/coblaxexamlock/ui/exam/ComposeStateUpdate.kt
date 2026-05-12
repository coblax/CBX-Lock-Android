package com.example.coblaxexamlock.ui.exam

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState

internal fun <T> MutableState<T>.setIfChanged(nextValue: T): Boolean {
    if (value == nextValue) return false
    value = nextValue
    return true
}

internal fun MutableIntState.setIfChanged(nextValue: Int): Boolean {
    if (intValue == nextValue) return false
    intValue = nextValue
    return true
}
