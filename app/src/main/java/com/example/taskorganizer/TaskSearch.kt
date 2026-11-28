package com.example.taskorganizer

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{M}+")
private val searchWhitespace = Regex("\\s+")

private fun normalizeSearchText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ITALY)
        .trim()

internal fun taskMatchesQuery(task: OrganizerTask, query: String): Boolean {
    val words = normalizeSearchText(query).split(searchWhitespace).filter(String::isNotEmpty)
    if (words.isEmpty()) return true
    val fields = listOf(task.title, task.note, task.category).map(::normalizeSearchText)
    return words.all { word -> fields.any { it.contains(word) } }
}
