package com.example.taskorganizer

import kotlin.math.max

/** Pure reminder calculations, kept separate so they can be tested without Android services. */
internal object ReminderTiming {
    private const val MAX_SCHEDULED_REMINDERS = 48
    private const val MINUTE_MILLIS = 60_000L

    fun plannedTriggerTimes(startAt: Long, minutesBefore: Int, repeatMinutes: Int): List<Long> {
        val firstTrigger = startAt - minutesBefore.coerceAtLeast(0) * MINUTE_MILLIS
        if (repeatMinutes <= 0) return listOf(firstTrigger)

        val allTriggers = mutableListOf<Long>()
        var trigger = firstTrigger
        while (trigger <= startAt) {
            allTriggers += trigger
            trigger += repeatMinutes * MINUTE_MILLIS
        }
        if (allTriggers.lastOrNull() != startAt) allTriggers += startAt
        return if (allTriggers.size <= MAX_SCHEDULED_REMINDERS) allTriggers
        else allTriggers.take(MAX_SCHEDULED_REMINDERS - 1) + startAt
    }

    fun remainingLabel(startAt: Long, now: Long): String {
        val remaining = max(0L, startAt - now)
        if (remaining < MINUTE_MILLIS) return "Ora"
        val minutes = (remaining + MINUTE_MILLIS - 1) / MINUTE_MILLIS
        return when {
            minutes < 60 -> "Tra $minutes min"
            minutes % 60L == 0L -> "Tra ${minutes / 60} ${if (minutes == 60L) "ora" else "ore"}"
            else -> "Tra ${minutes / 60} h e ${minutes % 60} min"
        }
    }
}
