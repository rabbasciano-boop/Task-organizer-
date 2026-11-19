package com.example.taskorganizer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date pickers exchange dates as UTC midnight, never as local time. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toPickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
