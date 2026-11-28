package com.example.taskorganizer

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal data class QuickTaskDraft(val title: String, val date: LocalDate, val time: LocalTime?, val ambiguousHour: Boolean = false) {
    fun isPast(now: LocalDateTime): Boolean = time?.let { LocalDateTime.of(date, it).isBefore(now) } ?: false
}

private val datePattern = Regex("(?<![\\p{L}\\d/])(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?(?![\\d/])")
private val relativePattern = Regex("(?iu)(?<![\\p{L}\\d])(dopodomani|domani|oggi)(?![\\p{L}\\d])")
private val offsetPattern = Regex("(?iu)(?<![\\p{L}\\d])tra\\s+(\\d{1,4})\\s+giorni?(?![\\p{L}\\d])")
private val weekdayPattern = Regex("(?iu)(?<![\\p{L}\\d])(luned[iì]|lun|marted[iì]|mar|mercoled[iì]|mer|gioved[iì]|gio|venerd[iì]|ven|sabato|sab|domenica|dom)(?![\\p{L}\\d])")
private val dayPattern = Regex("(?iu)(?<![\\p{L}\\d])il\\s+(\\d{1,2})(?![\\d/])")
private val clockPattern = Regex("(?iu)(?<![\\p{L}\\d/])(?:(?:alle|ore|h)\\s*(\\d{1,2})(?:([:.,])(\\d{2}))?|(\\d{1,2}):(\\d{2}))(?!\\d|[/,:.]\\d)")
private val timeOfDayPattern = Regex("(?iu)(?<![\\p{L}\\d])(stasera|stamattina)(?![\\p{L}\\d])")

internal fun parseQuickTask(text: String, now: LocalDateTime): QuickTaskDraft {
    val today = now.toLocalDate()
    val consumed = mutableListOf<IntRange>()
    var date: LocalDate? = null

    datePattern.find(text)?.let { match ->
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val yearText = match.groupValues[3]
        date = if (yearText.isNotEmpty()) runCatching { LocalDate.of(yearText.toInt(), month, day) }.getOrNull()
        else (0..8).asSequence().mapNotNull { offset -> runCatching { LocalDate.of(today.year + offset, month, day) }.getOrNull() }.firstOrNull { !it.isBefore(today) }
        if (date != null) consumed += match.range
    }
    if (date == null) offsetPattern.find(text)?.let { match ->
        val offset = match.groupValues[1].toLongOrNull()
        date = offset?.let { runCatching { today.plusDays(it) }.getOrNull() }
        if (date != null) consumed += match.range
    }
    if (date == null) relativePattern.find(text)?.let { match ->
        date = today.plusDays(when (match.value.lowercase(Locale.ITALY)) { "domani" -> 1; "dopodomani" -> 2; else -> 0 }.toLong())
        consumed += match.range
    }
    if (date == null) weekdayPattern.findAll(text).firstOrNull { match ->
        match.value.length > 3 || match.value.first().isLowerCase()
    }?.let { match ->
        val day = when (match.value.lowercase(Locale.ITALY).take(3)) {
            "lun" -> DayOfWeek.MONDAY; "mar" -> DayOfWeek.TUESDAY; "mer" -> DayOfWeek.WEDNESDAY
            "gio" -> DayOfWeek.THURSDAY; "ven" -> DayOfWeek.FRIDAY; "sab" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
        date = today.with(TemporalAdjusters.next(day))
        consumed += match.range
    }
    if (date == null) dayPattern.find(text)?.let { match ->
        val day = match.groupValues[1].toInt()
        date = (0L..12L).asSequence().mapNotNull { offset ->
            val month = YearMonth.from(today).plusMonths(offset)
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }.firstOrNull { !it.isBefore(today) }
        if (date != null) consumed += match.range
    }

    var time: LocalTime? = null
    var ambiguousHour = false
    clockPattern.find(text)?.let { match ->
        val hour = (match.groupValues[1].ifEmpty { match.groupValues[4] }).toInt()
        val minute = (match.groupValues[3].ifEmpty { match.groupValues[5] }).ifEmpty { "0" }.toInt()
        time = runCatching { LocalTime.of(hour, minute) }.getOrNull()
        if (time != null) {
            consumed += match.range
            ambiguousHour = match.groupValues[1].isNotEmpty() && match.groupValues[2].isEmpty() && hour in 1..11
        }
    }
    if (time == null) timeOfDayPattern.find(text)?.let { match ->
        val candidate = if (match.value.equals("stasera", ignoreCase = true)) LocalTime.of(20, 0) else LocalTime.of(9, 0)
        if (LocalDateTime.of(date ?: today, candidate).isAfter(now)) { time = candidate; consumed += match.range }
    }
    val explicitDate = date != null
    var finalDate = date ?: today
    if (!explicitDate && time != null && !LocalDateTime.of(finalDate, time).isAfter(now)) finalDate = finalDate.plusDays(1)
    val title = buildString {
        text.forEachIndexed { index, char -> append(if (consumed.any { index in it }) ' ' else char) }
    }.trim().replace(Regex("\\s+"), " ").replace(Regex("\\s+([,.!?])"), "$1").ifBlank { text.trim() }
    return QuickTaskDraft(title, finalDate, time, ambiguousHour)
}
