package com.example.taskorganizer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUndoIsolationTest {
    @Test fun pendingUndoIsNotSharedBetweenProfiles() {
        val store = ViewModelStore()
        try {
            val provider = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())
            val first = provider["time_log_undo_first", TimeLogUndoViewModel::class.java]
            val second = provider["time_log_undo_second", TimeLogUndoViewModel::class.java]
            first.pendingDeletions.add(PendingWorkDeletion(WorkLogEntry(date = "2026-11-27", startTime = "09:00", endTime = "10:00"), 1L))
            assertEquals(1, first.pendingDeletions.size)
            assertTrue(second.pendingDeletions.isEmpty())
            first.discardPending()
            assertTrue(first.pendingDeletions.isEmpty())
        } finally {
            store.clear()
        }
    }
}
