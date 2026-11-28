package com.example.taskorganizer

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReminderActionsTest {
    @Test fun snoozeCountAndTimeAreBounded() {
        assertTrue(canSnooze(0))
        assertTrue(canSnooze(2))
        assertFalse(canSnooze(3))
        assertFalse(canSnooze(-1))
        assertEquals(601_000L, snoozeTriggerAt(1_000L))
    }

    @Test fun completingRecurringTaskTwiceCreatesOnlyOneNextOccurrence() {
        val today = LocalDate.now()
        val task = OrganizerTask(id = "one", title = "Studio", date = today.toString(), recurrence = "daily")
        val completed = requireNotNull(completeFromNotification(TaskArchive(tasks = listOf(task)), task.id))
        assertTrue(completed.tasks.first { it.id == task.id }.completed)
        assertEquals(1, completed.tasks.count { it.date == today.plusDays(1).toString() && !it.completed })
        assertNull(completeFromNotification(completed, task.id))
    }

    @Test fun missingDeletedAndCompletedTasksAreNotChanged() {
        val task = OrganizerTask(id = "one", title = "Studio", date = "2026-11-28")
        assertNull(completeFromNotification(TaskArchive(), task.id))
        assertNull(completeFromNotification(TaskArchive(deletedTasks = listOf(task.copy(deletedAt = 1))), task.id))
        assertNull(completeFromNotification(TaskArchive(tasks = listOf(task.copy(completed = true))), task.id))
        assertNull(completeFromNotification(TaskArchive(tasks = listOf(task.copy(deletedAt = 1))), task.id))
    }
}
