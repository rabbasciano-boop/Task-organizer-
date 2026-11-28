package com.example.taskorganizer

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class WorkTimerAutoStopTest {
    private val start = Instant.parse("2026-01-10T08:00:00Z").toEpochMilli()
    private val timer = WorkTimerAutoStop(start + WORK_TIMER_LIMIT_MILLIS, 60, "Appunti", 1000L, 4)

    @Test fun stopsOnceAndPreservesExistingEntriesAndDraft() {
        val existing = WorkLogEntry(date = "2026-01-09", startTime = "09:00", endTime = "10:00")
        val archive = TaskArchive(workLogEntries = listOf(existing), activeWorkTimerStartedAt = start, activeWorkTimerAutoStop = timer)
        assertNull(stopWorkTimerIfDue(archive, start, start + WORK_TIMER_LIMIT_MILLIS - 1, 1000L + WORK_TIMER_LIMIT_MILLIS - 1, 4, ZoneId.of("UTC")))
        val saved = stopWorkTimerIfDue(archive, start, start + WORK_TIMER_LIMIT_MILLIS, 1000L + WORK_TIMER_LIMIT_MILLIS, 4, ZoneId.of("UTC"))!!
        assertNull(saved.activeWorkTimerStartedAt)
        assertEquals(2, saved.workLogEntries.size)
        assertEquals(existing, saved.workLogEntries.first())
        assertEquals(420, saved.workLogEntries.drop(1).sumOf { workMinutes(it.startTime, it.endTime, it.breakMinutes) })
        assertEquals("Appunti", saved.workLogEntries.last().notes)
        assertTrue(saved.workLogEntries.last().autoStopped)
        assertNull(stopWorkTimerIfDue(saved, start, start + WORK_TIMER_LIMIT_MILLIS, 1000L + WORK_TIMER_LIMIT_MILLIS, 4, ZoneId.of("UTC")))
        val duplicate = archive.copy(workLogEntries = saved.workLogEntries)
        assertEquals(2, stopWorkTimerIfDue(duplicate, start, start + WORK_TIMER_LIMIT_MILLIS, 1000L + WORK_TIMER_LIMIT_MILLIS, 4, ZoneId.of("UTC"))!!.workLogEntries.size)
    }

    @Test fun usesMonotonicClockAndFallsBackAfterReboot() {
        assertEquals(WORK_TIMER_LIMIT_MILLIS - 3_600_000, workTimerRemaining(timer, start + WORK_TIMER_LIMIT_MILLIS * 2, 1000 + 3_600_000, 4))
        assertEquals(0L, workTimerRemaining(timer, start + WORK_TIMER_LIMIT_MILLIS * 2, 0, 5))
    }

    @Test fun savesActualElapsedHoursAcrossDaylightSavingChanges() {
        val zone = ZoneId.of("Europe/Rome")
        for (date in listOf("2026-03-29T00:00:00", "2026-10-25T00:00:00")) {
            val started = LocalDateTime.parse(date).atZone(zone).toInstant().toEpochMilli()
            val parts = autoStoppedWorkEntries(started, WorkTimerAutoStop(started + WORK_TIMER_LIMIT_MILLIS, 60, "Nota"), zone)
            assertEquals(420, parts.sumOf { workMinutes(it.startTime, it.endTime, it.breakMinutes) })
            assertTrue(parts.all { it.autoStopped && it.notes == "Nota" })
        }
    }

    @Test fun queuedDraftIsSavedEvenIfTimerHasJustStopped() {
        val archive = TaskArchive(activeWorkTimerStartedAt = start, activeWorkTimerAutoStop = timer)
        val saved = stopWorkTimerIfDue(archive, start, start + WORK_TIMER_LIMIT_MILLIS, 1000 + WORK_TIMER_LIMIT_MILLIS, 4, ZoneId.of("UTC"))!!
        val updated = applyWorkTimerDraft(saved, start, 20, "Nota aggiornata")!!
        assertEquals(20, updated.workLogEntries.sumOf { it.breakMinutes })
        assertTrue(updated.workLogEntries.all { it.notes == "Nota aggiornata" })
        assertNull(applyWorkTimerDraft(saved, start + 1, 0, "Altro"))
        assertNull(stopWorkTimerIfDue(TaskArchive(activeWorkTimerStartedAt = start), start, start + WORK_TIMER_LIMIT_MILLIS, 0, null, ZoneId.of("UTC")))
    }
}
