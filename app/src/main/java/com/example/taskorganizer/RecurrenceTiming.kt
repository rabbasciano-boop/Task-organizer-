package com.example.taskorganizer

import java.time.LocalDate

/** Calculates the following occurrence without relying on UI state. */
internal object RecurrenceTiming {
    fun nextDate(isoDate: String, recurrence: String): String? = runCatching {
        val date = LocalDate.parse(isoDate)
        when (recurrence) {
            "daily" -> date.plusDays(1)
            "weekly" -> date.plusWeeks(1)
            "monthly" -> date.plusMonths(1)
            else -> return null
        }.toString()
    }.getOrNull()
}
