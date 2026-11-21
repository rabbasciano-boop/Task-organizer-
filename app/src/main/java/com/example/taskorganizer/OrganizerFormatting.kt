package com.example.taskorganizer

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

val ItalianDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ITALY)
    .withResolverStyle(ResolverStyle.STRICT)
val ItalianSlashDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ITALY)
    .withResolverStyle(ResolverStyle.STRICT)

fun parseItalianDate(value: String): LocalDate? = sequenceOf(ItalianDateFormatter, ItalianSlashDateFormatter)
    .mapNotNull { formatter -> runCatching { LocalDate.parse(value.trim(), formatter) }.getOrNull() }
    .firstOrNull()

fun formatItalianDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(ItalianDateFormatter)
}.getOrDefault(isoDate)

fun formatMonthKey(monthKey: String): String = runCatching {
    YearMonth.parse(monthKey).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY))
}.getOrDefault(monthKey)

fun formatTimeRange(task: OrganizerTask): String? = task.time?.let { start ->
    task.endTime?.let { end -> "$start – $end" } ?: start
}

fun parseAmountCents(value: String): Long? {
    val raw = value.trim().replace("€", "").trim()
    val withoutThousands = if (raw.matches(Regex("\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?"))) raw.replace(".", "") else raw
    val entered = withoutThousands.replace(',', '.')
    if (entered.isBlank() || !entered.matches(Regex("\\d+(\\.\\d{1,2})?"))) return null
    val parts = entered.split('.')
    val euros = parts[0].toLongOrNull() ?: return null
    if (euros > 9_999_999L) return null
    val cents = parts.getOrNull(1)?.padEnd(2, '0')?.toLongOrNull() ?: 0L
    return euros * 100 + cents
}

fun formatCurrency(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(cents / 100.0)

fun normalizeTime(value: String): String? {
    val enteredTime = value.trim()
    if (enteredTime.isBlank()) return null
    val completeTime = if (enteredTime.matches(Regex("\\d{1,2}"))) "$enteredTime:00" else enteredTime
    return runCatching {
        LocalTime.parse(completeTime, DateTimeFormatter.ofPattern("H:mm")).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()
}

fun greetingFor(nickname: String): String {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour in 5..13 -> "Buongiorno"
        hour in 14..19 -> "Buon pomeriggio"
        else -> "Buonasera"
    }
    return "$greeting, ${nickname.ifBlank { "Utente" }}"
}
