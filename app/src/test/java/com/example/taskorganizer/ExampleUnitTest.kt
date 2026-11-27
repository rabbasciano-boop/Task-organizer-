package com.example.taskorganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

class SunsetContrastTest {
    @Test
    fun whiteTextRemainsReadableOnEverySunsetGradientOverLightBackground() {
        val gradientStops = listOf(
            Color(0xFFF59E42), Color(0xFFEA6A47), Color(0xFFD94E7A),
            Color(0xFFFFB347), Color(0xFFFF7B47), Color(0xFFE85D8A),
            Color(0xFF155E8D), Color(0xFF147FB5), Color(0xFF4F63B8),
            Color(0xFF1E634E), Color(0xFF318368), Color(0xFF6AB692),
            Color(0xFF4A3169), Color(0xFF7250A3), Color(0xFFB06CCF),
            Color(0xFFC0444C), Color(0xFFE06464), Color(0xFFF08AB0),
            Color(0xFF9B650D), Color(0xFFD68B1A), Color(0xFFF0BF57)
        )
        gradientStops.forEach { color ->
            val background = readableSunsetColor(color, .88f).compositeOver(Color.White)
            val contrast = 1.05f / (background.luminance() + .05f)
            assertTrue("White contrast $contrast is insufficient for $color", contrast >= 4.5f)
        }
    }
}

class ReminderTimingTest {
    @Test
    fun routineNoticesChooseTheNextLocalOccurrenceAcrossDaylightSaving() {
        val rome = java.time.ZoneId.of("Europe/Rome")
        val saturday = java.time.ZonedDateTime.of(2026, 3, 28, 9, 0, 0, 0, rome)
        val daily = RoutineTiming.next(RoutineNotice.LIST, saturday)
        assertEquals(java.time.LocalDate.of(2026, 3, 29), daily.toLocalDate())
        assertEquals(java.time.LocalTime.of(8, 0), daily.toLocalTime())
        assertEquals("+02:00", daily.offset.id)
        val sunday = RoutineTiming.next(RoutineNotice.WEEKLY, saturday)
        assertEquals(java.time.LocalDate.of(2026, 3, 29), sunday.toLocalDate())
        assertEquals(java.time.LocalTime.of(18, 0), sunday.toLocalTime())
        assertEquals(java.time.DayOfWeek.WEDNESDAY, RoutineTiming.next(RoutineNotice.WORK, sunday).dayOfWeek)
        assertEquals(daily.toLocalDate().plusDays(1), RoutineTiming.next(RoutineNotice.LIST, daily).toLocalDate())
    }
    @Test
    fun notificationTextUsesTheActualRemainingTimeAndAvoidsClaimingLateEventsAreNow() {
        val day = 24L * 60 * 60 * 1000
        assertEquals("Manca un giorno a: Visita", ReminderTiming.notificationText(day, 0, "Visita"))
        assertEquals("Tra 15 min: Visita", ReminderTiming.notificationText(15 * 60_000L, 0, "Visita"))
        assertEquals("È il momento di: Visita", ReminderTiming.notificationText(1_000L, 1_000L, "Visita"))
        assertEquals("Promemoria: Visita", ReminderTiming.notificationText(1_000L, 61_001L, "Visita"))
    }

    @Test
    fun inactivityStagesDontDeliverAnOldThreeDayMessageAfterTwoWeeks() {
        val day = 24L * 60 * 60 * 1000
        assertEquals(null, InactivityTiming.stageToDeliver(3 * day - 1))
        assertEquals(0, InactivityTiming.stageToDeliver(3 * day))
        assertEquals(1, InactivityTiming.stageToDeliver(14 * day))
    }
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

    @Test
    fun changingTheClockForwardDoesNotClearAnActiveMonotonicLock() {
        val timing = resolvePinLockTiming(
            lockDuration = 30_000L,
            wallClockRemaining = 0L,
            monotonicRemaining = 18_000L,
            bootChanged = false
        )

        assertEquals(18_000L, timing.remainingMillis)
        assertTrue(!timing.shouldClearExpiredLock)
    }

    @Test
    fun rebootAfterClockChangeRebasesTheSavedLockInsteadOfClearingIt() {
        val timing = resolvePinLockTiming(
            lockDuration = 30_000L,
            wallClockRemaining = 0L,
            monotonicRemaining = 0L,
            bootChanged = true
        )

        assertEquals(30_000L, timing.remainingMillis)
        assertTrue(timing.shouldRebaseAfterBoot)
    }
}

class RecurrenceTimingTest {
    @Test
    fun reopeningARecurringTaskRemovesItsGeneratedFutureOccurrence() {
        val task = OrganizerTask(id = "rent", title = "Affitto", date = "2026-11-01", recurrence = "monthly")
        val completed = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(task)), task.id, LocalDate.of(2026, 11, 1))
        val reopened = RecurrenceTiming.toggleTask(completed, task.id, LocalDate.of(2026, 11, 1))

        assertEquals(1, reopened.tasks.size)
        assertTrue(!reopened.tasks.single().completed)
        assertEquals(1, reopened.deletedTasks.size)
        assertEquals("2026-12-01", reopened.deletedTasks.single().date)

        val completedAgain = RecurrenceTiming.toggleTask(reopened, task.id, LocalDate.of(2026, 11, 1))
        assertEquals(2, completedAgain.tasks.size)
        assertEquals(1, completedAgain.tasks.count { it.date == "2026-12-01" })
        assertTrue(!completedAgain.tasks.single { it.date == "2026-12-01" }.completed)
    }

    @Test
    fun reopeningTheNextDayStillRemovesTheOccurrenceCreatedEarlier() {
        val original = OrganizerTask(id = "daily", title = "Acqua", date = "2026-11-01", recurrence = "daily")
        val completed = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(original)), original.id, LocalDate.of(2026, 11, 1))

        val reopened = RecurrenceTiming.toggleTask(completed, original.id, LocalDate.of(2026, 11, 2))

        assertEquals(listOf("2026-11-01"), reopened.tasks.map { it.date })
        assertEquals(listOf("2026-11-02"), reopened.deletedTasks.map { it.date })
    }

    @Test
    fun reopeningLegacyRecurrenceDoesNotTrashAnUnrelatedTaskWithoutSeriesId() {
        val legacy = OrganizerTask(id = "legacy", title = "Vecchia", date = "2026-11-01", recurrence = "daily", completed = true)
        val unrelated = OrganizerTask(id = "plain", title = "Normale", date = "2026-11-02")

        val reopened = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(legacy, unrelated)), legacy.id, LocalDate.of(2026, 11, 2))

        assertTrue(reopened.tasks.any { it.id == "plain" })
        assertTrue(reopened.deletedTasks.none { it.id == "plain" })
    }

    @Test
    fun reopeningAnEarlierTaskDoesNotReopenAnAlreadyCompletedFutureOccurrence() {
        val november = OrganizerTask(id = "nov", title = "Affitto", date = "2026-11-01", recurrence = "monthly", completed = true, seriesId = "rent", recurrenceAnchorDate = "2026-11-01")
        val december = november.copy(id = "dec", date = "2026-12-01", completed = true)

        val reopened = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(november, december)), november.id, LocalDate.of(2026, 11, 20))
        val completedAgain = RecurrenceTiming.toggleTask(reopened, november.id, LocalDate.of(2026, 11, 20))

        assertTrue(completedAgain.tasks.any { it.id == "dec" && it.completed })
    }

    @Test
    fun completionKeepsAnAlreadyCompletedFutureOccurrenceCompleted() {
        val today = LocalDate.of(2026, 11, 1)
        val original = OrganizerTask(id = "daily", title = "Acqua", date = "2026-11-01", recurrence = "daily", seriesId = "daily")
        val activeDuplicate = original.copy(id = "tomorrow-active", date = "2026-11-02", completed = true)
        val trashedDuplicate = original.copy(id = "tomorrow-trash", date = "2026-11-02", deletedAt = 1L)

        val result = RecurrenceTiming.updateTask(
            TaskArchive(tasks = listOf(original, activeDuplicate), deletedTasks = listOf(trashedDuplicate)),
            original.copy(completed = true),
            today
        )

        val tomorrow = result.tasks.filter { it.date == "2026-11-02" }
        assertEquals(1, tomorrow.size)
        assertTrue(tomorrow.single().completed)
    }

    @Test
    fun reopeningAnOlderCompletedOccurrenceLeavesTheFutureOccurrenceUntouched() {
        val october = OrganizerTask(id = "october", title = "Affitto", date = "2026-10-01", recurrence = "monthly", completed = true, seriesId = "rent", recurrenceAnchorDate = "2026-10-01")
        val november = october.copy(id = "november", date = "2026-11-01")
        val december = october.copy(id = "december", date = "2026-12-01", completed = false)

        val result = RecurrenceTiming.toggleTask(TaskArchive(tasks = listOf(october, november, december)), october.id, LocalDate.of(2026, 11, 20))

        assertTrue(result.tasks.any { it.id == "december" && !it.completed })
        assertTrue(result.deletedTasks.none { it.id == "december" })
    }

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

class OrganizerFormattingTest {
    @Test
    fun acceptsOnlyRealItalianCalendarDates() {
        assertEquals(null, parseItalianDate("31-02-2026"))
        assertEquals(null, parseItalianDate("31/04/2026"))
        assertEquals(LocalDate.of(2028, 2, 29), parseItalianDate("29-02-2028"))
    }

    @Test
    fun parsesItalianAmountsWithoutOverflow() {
        assertEquals(350L, parseAmountCents("3,5"))
        assertEquals(123_450L, parseAmountCents("1.234,50"))
        assertEquals(null, parseAmountCents("99999999999999999999"))
    }
}

class WidgetOrderingTest {
    @Test
    fun keepsUpcomingTasksBeforeOverdueAndCompletedHistory() {
        val tasks = listOf(
            OrganizerTask(id = "old-done", title = "Fatto", date = "2026-10-01", completed = true),
            OrganizerTask(id = "overdue", title = "Ieri", date = "2026-11-19"),
            OrganizerTask(id = "tomorrow", title = "Domani", date = "2026-11-21"),
            OrganizerTask(id = "today", title = "Oggi", date = "2026-11-20")
        )
        assertEquals(listOf("today", "tomorrow", "overdue", "old-done"), widgetTasksForFilter(tasks, "all", LocalDate.of(2026, 11, 20)).map { it.id })
        assertEquals(listOf("old-done"), widgetTasksForFilter(tasks, "done", LocalDate.of(2026, 11, 20)).map { it.id })
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

class TimeLogCalculationTest {
    @Test
    fun themeSoundsRespectMuteAndKnownDoNotDisturb() {
        val normal = android.media.AudioManager.RINGER_MODE_NORMAL
        val all = android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        assertTrue(completionSoundAllowed(true, normal, all))
        assertTrue(completionSoundAllowed(true, normal, android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN))
        assertTrue(!completionSoundAllowed(false, normal, all))
        assertTrue(!completionSoundAllowed(true, android.media.AudioManager.RINGER_MODE_SILENT, all))
        assertTrue(!completionSoundAllowed(true, normal, android.app.NotificationManager.INTERRUPTION_FILTER_NONE))
    }
    @Test
    fun shortTimerIsNotSavedAndLongTimerKeepsAllMinutes() {
        val start = LocalDateTime.of(2026, 11, 27, 9, 15, 20)
        assertTrue(timerWorkEntries(start, start.plusSeconds(30), 0, "").isEmpty())
        val entries = timerWorkEntries(start, start.plusHours(30), 30, " Progetto ")
        assertEquals(3, entries.size)
        assertEquals(30 * 60 - 30, entries.sumOf { workMinutes(it) })
        assertEquals(30, entries.sumOf { it.breakMinutes })
        assertTrue(entries.all { it.notes == "Progetto" })
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertEquals("2026-11-28", entries.last().date)
    }

    @Test
    fun overnightTimerIsRepresentedWithoutLosingMinutes() {
        val start = LocalDateTime.of(2026, 11, 27, 23, 30)
        val entries = timerWorkEntries(start, start.plusHours(1), 0, "")
        assertEquals(1, entries.size)
        assertEquals(60, workMinutes(entries.single()))
    }
    @Test
    fun stoppedTimerKeepsItsBreakAndNotesInTheRecordedEntry() {
        val entry = timerWorkEntry(LocalDateTime.of(2026, 11, 27, 9, 15), LocalTime.of(12, 45), 30, "  Cliente Rossi  ")

        assertEquals("2026-11-27", entry.date)
        assertEquals("09:15", entry.startTime)
        assertEquals("12:45", entry.endTime)
        assertEquals(30, entry.breakMinutes)
        assertEquals("Cliente Rossi", entry.notes)
        assertEquals(180, workMinutes(entry))
        assertEquals(listOf(entry), filterWorkEntries(listOf(entry), "week", LocalDate.of(2026, 11, 27)))
    }
    @Test
    fun warnsAtAndBeyondTheTwentyFourHourLimit() {
        val start = 1_000L
        val day = 24L * 60 * 60 * 1_000
        assertTrue(!workTimerExceedsOneDay(start, start + day - 1))
        assertTrue(workTimerExceedsOneDay(start, start + day))
        assertTrue(workTimerExceedsOneDay(start, start + 30L * 60 * 60 * 1_000))
        assertTrue(!workTimerExceedsOneDay(start, start - 1))
    }

    @Test
    fun calculatesOvernightShiftsAndNeverReturnsNegativeMinutes() {
        assertTrue(isOvernightWorkShift("22:00", "06:00"))
        assertEquals(480, workMinutes("22:00", "06:00", 0))
        assertEquals(0, workMinutes("09:00", "10:00", 90))
        assertEquals(0, workMinutes("09:00", "09:00", 0))
    }

    @Test
    fun roundsWorkEarningsToTheNearestCent() {
        assertEquals(25L, workEarningsCents(minutes = 1, hourlyRateCents = 1_500L))
        assertEquals(1_500L, workEarningsCents(minutes = 60, hourlyRateCents = 1_500L))
    }

    @Test
    fun filtersOnlyValidEntriesForTheRequestedMonth() {
        val entries = listOf(
            WorkLogEntry(id = "in-month", date = "2026-11-12", startTime = "09:00", endTime = "10:00"),
            WorkLogEntry(id = "other-month", date = "2026-10-12", startTime = "09:00", endTime = "10:00"),
            WorkLogEntry(id = "bad-date", date = "2026-13-40", startTime = "09:00", endTime = "10:00")
        )

        assertEquals(listOf("in-month"), filterWorkEntries(entries, "month", LocalDate.of(2026, 11, 20)).map { it.id })
        assertTrue(!validWorkLogEntry("2026-13-40", "09:00", "10:00"))
    }

    @Test
    fun exportsCsvSafelyForQuotesNewlinesAndSpreadsheetFormulas() {
        val csv = workCsv(
            listOf(WorkLogEntry(date = "2026-11-20", startTime = "09:00", endTime = "10:00", notes = "=SUM(A1:A2) \"test\"\nriga due")),
            rateCents = 1_500L
        )

        assertTrue(csv.contains("\"'=SUM(A1:A2) \"\"test\"\"\nriga due\""))
    }
}
