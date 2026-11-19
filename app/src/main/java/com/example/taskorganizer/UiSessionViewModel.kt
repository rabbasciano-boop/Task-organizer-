package com.example.taskorganizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Short-lived UI state: it survives a configuration change, but is deliberately not persisted
 * when Android terminates the process. This keeps the PIN protection effective after a real app
 * restart while avoiding an unnecessary lock screen after rotating the phone.
 */
class UiSessionViewModel(hasPin: Boolean) : ViewModel() {
    var isUnlocked by mutableStateOf(!hasPin)
    var showOpening by mutableStateOf(true)

    fun unlock() {
        isUnlocked = true
    }

    fun lock() {
        isUnlocked = false
    }
}

class UiSessionViewModelFactory(private val hasPin: Boolean) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(UiSessionViewModel::class.java))
        return UiSessionViewModel(hasPin) as T
    }
}
