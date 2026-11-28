package com.example.taskorganizer

import java.time.LocalDate
import java.time.YearMonth

/** Shared recurrence logic used by both the app screens and the Home-screen widgets. */
internal object RecurrenceTiming {
    fun toggleTask(archive: TaskArchive, taskId: String, today: LocalDate = LocalDate.now()): TaskArchive {
        val task = archive.tasks.firstOrNull { it.id == taskId } ?: return archive
        return updateTask(archive, task.copy(completed = !task.completed), today)
    }

    fun updateTask(archive: TaskArchive, requestedTask: OrganizerTask, today: LocalDate = LocalDate.now()): TaskArchive {
        val index = archive.tasks.indexOfFirst { it.id == requestedTask.id }
        if (index < 0) return archive
        val previous = archive.tasks[index]
        val recurrenceEnabled = requestedTask.recurrence != "none"
        val completesRecurrence = !previous.completed && requestedTask.completed && recurrenceEnabled
        val reopensRecurrence = previous.completed && !requestedTask.completed && recurrenceEnabled
        val seriesId = when {
            !recurrenceEnabled -> null
            completesRecurrence -> previous.seriesId ?: previous.id
            else -> requestedTask.seriesId
        }
        val anchorDate = when {
            !recurrenceEnabled -> null
            completesRecurrence -> requestedTask.recurrenceAnchorDate ?: requestedTask.date
            else -> requestedTask.recurrenceAnchorDate
        }
        val updatedTask = requestedTask.copy(seriesId = seriesId, recurrenceAnchorDate = anchorDate)
        val nextDate = if (completesRecurrence) firstFutureDate(updatedTask.date, anchorDate ?: updatedTask.date, updatedTask.recurrence, today) else null
        val activeFutureOccurrences = if (nextDate != null) archive.tasks.filter { candidate ->
            candidate.id != previous.id && candidate.seriesId == seriesId && candidate.date == nextDate
        } else emptyList()
        // An occurrence completed in advance must remain completed: reopening an earlier item
        // must never turn a future completion back into something to do.
        val retainedActiveOccurrence = activeFutureOccurrences.firstOrNull()
        val duplicateActiveOccurrences = activeFutureOccurrences.drop(1)
        val restoredOccurrence = if (nextDate != null && retainedActiveOccurrence == null) {
            archive.deletedTasks.firstOrNull { candidate ->
                candidate.seriesId == seriesId && candidate.date == nextDate
            }?.copy(completed = false, deletedAt = 0L)
        } else null
        val nextTask = if (nextDate != null && retainedActiveOccurrence == null && restoredOccurrence == null) {
            updatedTask.copy(
                id = java.util.UUID.randomUUID().toString(),
                date = nextDate,
                completed = false,
                createdAt = System.currentTimeMillis(),
                deletedAt = 0L,
                seriesId = seriesId,
                recurrenceAnchorDate = anchorDate,
                subtasks = updatedTask.subtasks.map { it.copy(id = java.util.UUID.randomUUID().toString(), done = false) }
            )
        } else null
        val generatedOccurrence = if (
            reopensRecurrence &&
            previous.seriesId != null &&
            archive.tasks.none { candidate ->
                candidate.id != previous.id && candidate.seriesId == previous.seriesId && candidate.completed && candidate.date > previous.date
            }
        ) {
            // Remove the actual first open future occurrence of this series. Do not derive a
            // date from today: undoing tomorrow must still find the occurrence made yesterday.
            archive.tasks
                .asSequence()
                .filter { candidate ->
                    candidate.id != previous.id &&
                        candidate.seriesId == previous.seriesId &&
                        !candidate.completed &&
                        candidate.deletedAt == 0L &&
                        candidate.date > previous.date
                }
                .minByOrNull { it.date }
        } else null
        val duplicateIds = duplicateActiveOccurrences.mapTo(mutableSetOf()) { it.id }
        val activeTasks = archive.tasks.toMutableList().also { it[index] = updatedTask }
            .filterNot { it.id == generatedOccurrence?.id || it.id in duplicateIds }
            .map { candidate -> if (candidate.id == retainedActiveOccurrence?.id) retainedActiveOccurrence else candidate } +
            listOfNotNull(restoredOccurrence, nextTask)
        return archive.copy(
            tasks = activeTasks,
            deletedTasks = archive.deletedTasks.filterNot { it.id == generatedOccurrence?.id || it.id == restoredOccurrence?.id } +
                listOfNotNull(generatedOccurrence?.copy(deletedAt = System.currentTimeMillis())) +
                duplicateActiveOccurrences.map { it.copy(deletedAt = System.currentTimeMillis()) }
        )
    }

    fun reminderTasks(archive: TaskArchive, taskId: String): List<OrganizerTask> {
        val task = archive.tasks.firstOrNull { it.id == taskId } ?: return emptyList()
        val seriesId = task.seriesId ?: return listOf(task)
        return archive.tasks.filter { candidate ->
            candidate.id == task.id || (candidate.seriesId == seriesId && !candidate.completed && candidate.deletedAt == 0L)
        } + archive.deletedTasks.filter { it.seriesId == seriesId && it.deletedAt > 0L }
    }

    private fun firstFutureDate(
        currentIsoDate: String,
        anchorIsoDate: String,
        recurrence: String,
        today: LocalDate
    ): String? = runCatching {
        val current = LocalDate.parse(currentIsoDate)
        val anchor = LocalDate.parse(anchorIsoDate)
        when (recurrence) {
            "daily" -> generateSequence(current.plusDays(1)) { it.plusDays(1) }.first { it.isAfter(today) }
            "weekly" -> generateSequence(current.plusWeeks(1)) { it.plusWeeks(1) }.first { it.isAfter(today) }
            "monthly" -> generateSequence(YearMonth.from(current).plusMonths(1)) { it.plusMonths(1) }
                .map { month -> month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth())) }
                .first { it.isAfter(today) }
            else -> return null
        }.toString()
    }.getOrNull()
}
