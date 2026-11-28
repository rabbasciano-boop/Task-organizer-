package com.example.taskorganizer

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskRecurrenceTest {
    @Test fun nextDailyAndMonthlyOccurrenceHasFreshUncheckedSteps() {
        for (frequency in listOf("daily", "monthly")) {
            val date = LocalDate.of(2026, 11, 28)
            val steps = listOf(Subtask("a", "Primo", true), Subtask("b", "Secondo", false))
            val task = OrganizerTask(id = "task", title = "Studio", date = date.toString(), recurrence = frequency, subtasks = steps)
            val updated = RecurrenceTiming.updateTask(TaskArchive(tasks = listOf(task)), task.copy(completed = true), date)
            val original = updated.tasks.first { it.id == task.id }
            val next = updated.tasks.first { it.id != task.id }
            assertEquals(steps, original.subtasks)
            assertEquals(steps.map { it.title }, next.subtasks.map { it.title })
            assertTrue(next.subtasks.none { it.done })
            assertTrue(next.subtasks.map { it.id }.intersect(steps.map { it.id }.toSet()).isEmpty())
            assertNotEquals(original.id, next.id)
            val reopened = RecurrenceTiming.updateTask(updated, original.copy(completed = false), date)
            assertEquals(steps, reopened.tasks.first { it.id == task.id }.subtasks)
            assertFalse(reopened.tasks.first { it.id == task.id }.completed)
        }
    }
}
