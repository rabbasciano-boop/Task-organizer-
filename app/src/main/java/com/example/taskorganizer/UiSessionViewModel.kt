package com.example.taskorganizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Short-lived UI state: it survives a configuration change, but is deliberately not persisted
 * when Android terminates the process. This keeps the PIN protection effective after a real app
 * restart while avoiding an unnecessary lock screen after rotating the phone.
 */
class UiSessionViewModel : ViewModel() {
    var initialized by mutableStateOf(false)
        private set
    var isUnlocked by mutableStateOf(false)
    var showOpening by mutableStateOf(true)

    fun initialize(hasPin: Boolean) {
        if (initialized) return
        initialized = true
        isUnlocked = !hasPin
    }

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
    }
}
