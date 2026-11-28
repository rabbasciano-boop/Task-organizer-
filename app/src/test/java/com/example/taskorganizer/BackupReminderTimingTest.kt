package com.example.taskorganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class BackupReminderTimingTest {
    private val now = 2_000_000_000_000L

    @Test fun cardDependsOnTaskCountBackupAgeAndSnooze() {
        assertFalse(shouldSuggestBackup(9, null, null, now))
        assertTrue(shouldSuggestBackup(10, null, null, now))
        assertFalse(shouldSuggestBackup(10, now - BACKUP_REMINDER_PERIOD + 1, null, now))
        assertTrue(shouldSuggestBackup(10, now - BACKUP_REMINDER_PERIOD, null, now))
        assertFalse(shouldSuggestBackup(10, null, now + BACKUP_CARD_SNOOZE, now))
        assertTrue(shouldSuggestBackup(10, null, now, now))
        assertFalse(shouldSuggestBackup(10, now + 60_000, null, now))
    }

    @Test fun notificationCountdownRestartsAfterBackupOrLastNotice() {
        assertEquals(now + BACKUP_REMINDER_PERIOD, nextBackupNoticeAt(null, now, null))
        assertEquals(now + BACKUP_REMINDER_PERIOD, nextBackupNoticeAt(now, now - BACKUP_REMINDER_PERIOD, null))
        assertEquals(now + BACKUP_REMINDER_PERIOD, nextBackupNoticeAt(now - BACKUP_REMINDER_PERIOD, now - BACKUP_REMINDER_PERIOD, now))
        assertEquals(now + BACKUP_REMINDER_PERIOD, nextBackupNoticeAt(now - 3 * BACKUP_REMINDER_PERIOD, now, null))
    }

    @Test fun firstDataAfterAnEmptyProfileStartsAFreshTwoWeekCountdown() {
        val old = now - 20L * 24 * 60 * 60 * 1000
        val updated = backupBaselineAfterDataChange(old, false, true, now)
        assertEquals(now, updated)
        assertEquals(now + BACKUP_REMINDER_PERIOD, nextBackupNoticeAt(null, updated, null))
        assertEquals(old, backupBaselineAfterDataChange(old, true, true, now))
        assertEquals(old, backupBaselineAfterDataChange(old, null, true, now))
        assertEquals(old, backupBaselineAfterDataChange(old, false, false, now))
    }

    @Test fun failedReadRetriesAreLimited() {
        assertEquals(now + BACKUP_RETRY_DELAY, nextBackupRetryAt(0, now))
        assertEquals(now + BACKUP_RETRY_DELAY, nextBackupRetryAt(2, now))
        assertNull(nextBackupRetryAt(3, now))
        assertNull(nextBackupRetryAt(-1, now))
    }

    @Test fun emptyProfileDoesNotNeedBackupNotification() {
        assertFalse(hasDataToBackUp(TaskArchive()))
        assertTrue(hasDataToBackUp(TaskArchive(tasks = listOf(OrganizerTask(title = "Prova", date = "2026-11-28")))))
        assertTrue(hasDataToBackUp(TaskArchive(listItems = listOf(OrganizerListItem(title = "Latte")))))
        assertTrue(hasDataToBackUp(TaskArchive(workLogEntries = listOf(WorkLogEntry(date = "2026-11-28", startTime = "09:00", endTime = "10:00")))))
        assertTrue(hasDataToBackUp(TaskArchive(customCategories = listOf("Salute"))))
        assertTrue(hasDataToBackUp(TaskArchive(activeWorkTimerStartedAt = now)))
        assertTrue(hasDataToBackUp(TaskArchive(workLogSettings = WorkLogSettings(hourlyRateCents = 100))))
        assertTrue(hasDataToBackUp(TaskArchive(workLogSettings = WorkLogSettings(presets = emptyList()))))
    }
}
