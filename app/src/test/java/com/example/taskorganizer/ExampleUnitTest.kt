package com.example.taskorganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun createsTheCorrectNextOccurrence() {
        assertEquals("2026-12-01", RecurrenceTiming.nextDate("2026-11-30", "daily"))
        assertEquals("2026-12-07", RecurrenceTiming.nextDate("2026-11-30", "weekly"))
        assertEquals("2026-12-30", RecurrenceTiming.nextDate("2026-11-30", "monthly"))
    }

    @Test
    fun doesNotCreateAnOccurrenceForOneOffActivities() {
        assertEquals(null, RecurrenceTiming.nextDate("2026-11-30", "none"))
        assertEquals(null, RecurrenceTiming.nextDate("not-a-date", "daily"))
    }
}
