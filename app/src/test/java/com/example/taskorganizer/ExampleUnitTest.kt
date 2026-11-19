package com.example.taskorganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderTimingTest {
    @Test
    fun keepsTheFinalReminderWhenTheRepeatPlanExceedsTheLimit() {
        val startAt = 24L * 60 * 60 * 1000
        val triggers = ReminderTiming.plannedTriggerTimes(startAt, 24 * 60, 5)

        assertEquals(48, triggers.size)
        assertEquals(startAt, triggers.last())
        assertTrue(triggers.distinct().size == triggers.size)
    }

    @Test
    fun createsOneReminderWhenRepeatIsDisabled() {
        assertEquals(listOf(900_000L), ReminderTiming.plannedTriggerTimes(1_800_000L, 15, 0))
    }

    @Test
    fun formatsTheActualTimeRemaining() {
        assertEquals("Ora", ReminderTiming.remainingLabel(startAt = 1_000L, now = 1_000L))
        assertEquals("Tra 10 min", ReminderTiming.remainingLabel(startAt = 600_000L, now = 0L))
        assertEquals("Tra 1 ora", ReminderTiming.remainingLabel(startAt = 3_600_000L, now = 0L))
        assertEquals("Tra 1 h e 5 min", ReminderTiming.remainingLabel(startAt = 3_900_000L, now = 0L))
    }
}

class PinAttemptPolicyTest {
    @Test
    fun graduallyIncreasesTheDelayAfterFailedAttempts() {
        assertEquals(0L, PinAttemptPolicy.delayFor(1))
        assertEquals(0L, PinAttemptPolicy.delayFor(2))
        assertEquals(30_000L, PinAttemptPolicy.delayFor(3))
        assertEquals(60_000L, PinAttemptPolicy.delayFor(4))
        assertEquals(5 * 60_000L, PinAttemptPolicy.delayFor(5))
        assertEquals(15 * 60_000L, PinAttemptPolicy.delayFor(6))
        assertEquals(15 * 60_000L, PinAttemptPolicy.delayFor(20))
    }
}

class RecurrenceTimingTest {
    @Test
    fun createsTheFirstFutureOccurrenceForOverdueActivities() {
        val task = OrganizerTask(id = "daily", title = "Acqua", date = "2026-11-01", recurrence = "daily")
        val updated = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(task)), task.id, LocalDate.of(2026, 11, 19))

        assertEquals(listOf("2026-11-01", "2026-11-20"), updated.tasks.map { it.date }.sorted())
    }

    @Test
    fun monthlyRecurrenceKeepsTheOriginalDayAndDoesNotDuplicate() {
        val task = OrganizerTask(id = "monthly", title = "Bolletta", date = "2027-01-31", recurrence = "monthly")
        val first = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(task)), task.id, LocalDate.of(2027, 1, 31))
        val afterUndo = RecurrenceTiming.toggleTask(first, task.id, LocalDate.of(2027, 1, 31))
        val afterSecondCompletion = RecurrenceTiming.toggleTask(afterUndo, task.id, LocalDate.of(2027, 1, 31))

        assertEquals(2, afterSecondCompletion.tasks.size)
        assertTrue(afterSecondCompletion.tasks.any { it.date == "2027-02-28" })

        val february = afterSecondCompletion.tasks.first { it.date == "2027-02-28" }
        val march = RecurrenceTiming.toggleTask(afterSecondCompletion, february.id, LocalDate.of(2027, 2, 28))
        assertTrue(march.tasks.any { it.date == "2027-03-31" })
    }

    @Test
    fun editedMonthlyDateBecomesTheNewAnchor() {
        val previous = OrganizerTask(
            id = "edited-monthly",
            title = "Bolletta",
            date = "2027-01-31",
            recurrence = "monthly",
            seriesId = "series",
            recurrenceAnchorDate = "2027-01-31"
        )
        val editedAndCompleted = previous.copy(date = "2027-01-15", completed = true, recurrenceAnchorDate = null)
        val updated = RecurrenceTiming.updateTask(
            TaskArchive(tasks = listOf(previous)),
            editedAndCompleted,
            LocalDate.of(2027, 1, 15)
        )

        assertTrue(updated.tasks.any { it.date == "2027-02-15" })
    }
}

class DatePickerConversionsTest {
    @Test
    fun keepsTheSameCalendarDayAtUtcAndItalianDates() {
        val date = LocalDate.of(2026, 11, 20)

        assertEquals(date, date.toPickerMillis().toPickerDate())
    }

    @Test
    fun pickerMillisecondUsesUtcMidnight() {
        val date = LocalDate.of(2027, 3, 28)

        assertEquals(0L, Math.floorMod(date.toPickerMillis(), 24L * 60L * 60L * 1000L))
    }
}

class UiSessionViewModelTest {
    @Test
    fun startsUnlockedOnlyWhenThereIsNoPinAndCanBeLockedAgain() {
        val withPin = UiSessionViewModel(hasPin = true)
        val withoutPin = UiSessionViewModel(hasPin = false)

        assertTrue(!withPin.isUnlocked)
        assertTrue(withoutPin.isUnlocked)

        withPin.unlock()
        assertTrue(withPin.isUnlocked)
        withPin.lock()
        assertTrue(!withPin.isUnlocked)
    }
}

class ArchiveIdNormalizationTest {
    @Test
    fun keepsNewestActiveTaskAndRemovesTheSameIdFromTrash() {
        val older = OrganizerTask(id = "same", title = "Vecchia", date = "2026-11-20", createdAt = 1)
        val newer = OrganizerTask(id = "same", title = "Nuova", date = "2026-11-21", createdAt = 2)
        val trashCopy = newer.copy(deletedAt = 3)

        val normalized = normalizeArchiveIds(TaskArchive(tasks = listOf(older, newer), deletedTasks = listOf(trashCopy)))

        assertEquals(listOf("Nuova"), normalized.tasks.map { it.title })
        assertTrue(normalized.deletedTasks.isEmpty())
    }
}
