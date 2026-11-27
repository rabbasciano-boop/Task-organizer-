package com.example.taskorganizer

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

/** Undo is retained during a rotation; work entries remain stored in the encrypted archive. */
class TimeLogUndoViewModel : ViewModel() {
    internal val pendingDeletions = mutableStateListOf<PendingWorkDeletion>()
    internal val pendingRestores = mutableStateListOf<String>()

    internal fun discardPending() {
        pendingDeletions.clear()
        pendingRestores.clear()
    }
}

internal data class PendingWorkDeletion(val entry: WorkLogEntry, val expiresAtElapsed: Long)
