package com.example.taskorganizer

import org.json.JSONArray
import java.util.UUID

internal const val MAX_SUBTASKS = 20
internal const val MAX_SUBTASK_TITLE = 80

internal fun decodeSubtasks(array: JSONArray?): List<Subtask> {
    if (array == null) return emptyList()
    val seen = mutableSetOf<String>()
    val result = mutableListOf<Subtask>()
    for (index in 0 until array.length()) {
        if (result.size >= MAX_SUBTASKS) break
        val item = array.optJSONObject(index) ?: continue
        val title = item.optString("title", "").trim().take(MAX_SUBTASK_TITLE)
        if (title.isBlank()) continue
        val id = item.optString("id", "").trim().ifBlank { UUID.randomUUID().toString() }
        if (seen.add(id)) result.add(Subtask(id = id, title = title, done = item.optBoolean("done", false)))
    }
    return result
}
