package com.example.taskorganizer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.LocalDate

enum class WidgetForm { SMALL, MEDIUM, LARGE }

/** Common behavior for the three Home-screen widgets. They use the encrypted local archive only. */
abstract class TaskOrganizerWidgetProvider : AppWidgetProvider() {
    protected abstract val form: WidgetForm

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> TaskWidgetUpdater.update(context, appWidgetManager, id, form) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) = TaskWidgetUpdater.update(context, appWidgetManager, appWidgetId, form)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SET_FILTER) {
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val filter = intent.getStringExtra(EXTRA_FILTER).orEmpty()
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && filter in setOf("all", "today", "todo", "done")) {
                context.getSharedPreferences(WIDGET_PREFERENCES, Context.MODE_PRIVATE)
                    .edit().putString("filter_$widgetId", filter).apply()
                TaskWidgetUpdater.update(context, AppWidgetManager.getInstance(context), widgetId, WidgetForm.LARGE)
            }
            return
        }
        if (intent.action != ACTION_TOGGLE_TASK) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        if (taskId.isBlank()) return

        val pendingResult = goAsync()
        Thread {
            try {
                val appContext = context.applicationContext
                val store = EncryptedTaskStore(appContext)
                var changedTask: OrganizerTask? = null
                store.update { archive ->
                    val task = archive.tasks.firstOrNull { it.id == taskId } ?: return@update archive
                    changedTask = task.copy(completed = !task.completed)
                    archive.copy(tasks = archive.tasks.map { if (it.id == taskId) changedTask!! else it })
                }
                changedTask?.let { changed ->
                    if (changed.completed) TaskReminderScheduler.cancel(appContext, changed)
                    else TaskReminderScheduler.schedule(appContext, changed)
                }
                TaskWidgetUpdater.updateAll(appContext)
            } catch (_: Exception) {
                // The app retains the encrypted archive unchanged. It can offer recovery on next open.
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.taskorganizer.widget.TOGGLE_TASK"
        const val ACTION_SET_FILTER = "com.example.taskorganizer.widget.SET_FILTER"
        const val EXTRA_TASK_ID = "widget_task_id"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_FILTER = "widget_filter"
        const val WIDGET_PREFERENCES = "task_organizer_widget"
    }
}

class TaskOrganizerProgressWidgetProvider : TaskOrganizerWidgetProvider() {
    override val form = WidgetForm.SMALL
}

class TaskOrganizerOverviewWidgetProvider : TaskOrganizerWidgetProvider() {
    override val form = WidgetForm.MEDIUM
}

class TaskOrganizerAgendaWidgetProvider : TaskOrganizerWidgetProvider() {
    override val form = WidgetForm.LARGE
}

/** Refreshes date-sensitive widget content even when the main app has not been opened. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            try {
                TaskWidgetUpdater.updateAll(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

object TaskWidgetUpdater {
    private const val SETTINGS_PREFERENCES = "task_organizer_settings"
    private const val HIDE_TITLES_KEY = "hide_widget_titles"

    private data class WidgetPalette(
        val background: Int,
        val card: Int,
        val accent: Int,
        val text: Int,
        val muted: Int,
        val success: Int
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            TaskOrganizerProgressWidgetProvider::class.java to WidgetForm.SMALL,
            TaskOrganizerOverviewWidgetProvider::class.java to WidgetForm.MEDIUM,
            TaskOrganizerAgendaWidgetProvider::class.java to WidgetForm.LARGE
        ).forEach { (provider, form) ->
            val component = ComponentName(context, provider)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it, form) }
        }
    }

    fun areTaskTitlesHidden(context: Context): Boolean =
        context.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE).getBoolean(HIDE_TITLES_KEY, false)

    fun setTaskTitlesHidden(context: Context, hidden: Boolean) {
        context.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(HIDE_TITLES_KEY, hidden)
            .apply()
    }

    fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int, form: WidgetForm) {
        val layout = when (form) {
            WidgetForm.SMALL -> R.layout.widget_small
            WidgetForm.MEDIUM -> R.layout.widget_medium
            WidgetForm.LARGE -> R.layout.widget_large
        }
        val palette = palette(context)
        val today = LocalDate.now()
        val tasks = runCatching { EncryptedTaskStore(context).load().tasks }.getOrElse { emptyList() }
        val todayTasks = tasks.filter { it.date == today.toString() }.sortedWith(compareBy<OrganizerTask> { it.time == null }.thenBy { it.time ?: "" })
        val completed = todayTasks.count { it.completed }
        val progress = if (todayTasks.isEmpty()) 0 else (completed * 100 / todayTasks.size)
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val weekTasks = tasks.filter { task ->
            runCatching {
                val date = LocalDate.parse(task.date)
                !date.isBefore(weekStart) && !date.isAfter(weekEnd)
            }.getOrDefault(false)
        }
        val nextTask = todayTasks.firstOrNull { !it.completed }
        val hideTaskTitles = areTaskTitlesHidden(context)
        val activeFilter = context.getSharedPreferences(TaskOrganizerWidgetProvider.WIDGET_PREFERENCES, Context.MODE_PRIVATE)
            .getString("filter_$appWidgetId", "all") ?: "all"
        val visibleTasks = when (activeFilter) {
            "today" -> todayTasks
            "todo" -> tasks.filter { !it.completed }
            "done" -> tasks.filter { it.completed }
            else -> tasks
        }.sortedWith(compareBy<OrganizerTask> { it.date }.thenBy { it.time ?: "" })
        val views = RemoteViews(context.packageName, layout)
        applyColors(views, layout, palette)
        views.setImageViewBitmap(R.id.widget_ring, progressRing(progress, palette))
        views.setTextViewText(R.id.widget_progress, "$progress%")
        views.setTextViewText(R.id.widget_summary, if (todayTasks.isEmpty()) "Nessuna attività per oggi" else "$completed di ${todayTasks.size} completate oggi")
        views.setOnClickPendingIntent(R.id.widget_add, openAppIntent(context, addNew = true))
        views.setOnClickPendingIntent(R.id.widget_title, openAppIntent(context, addNew = false))

        when (form) {
            WidgetForm.SMALL -> {
                views.setTextViewText(R.id.widget_motto, if (todayTasks.isEmpty()) "Inizia quando vuoi: aggiungi la prima attività." else "Un passo alla volta, stai andando bene.")
            }
            WidgetForm.MEDIUM -> {
                views.setTextViewText(R.id.widget_summary, "$completed di ${todayTasks.size} fatte")
                views.setTextViewText(R.id.widget_week_total, weekTasks.size.toString())
                views.setTextViewText(R.id.widget_week_done, weekTasks.count { it.completed }.toString())
                views.setTextViewText(R.id.widget_week_todo, weekTasks.count { !it.completed }.toString())
                bindNextTask(context, views, nextTask, palette, hideTaskTitles)
            }
            WidgetForm.LARGE -> {
                views.setTextViewText(R.id.widget_day, "${formatDate(today)} · $completed di ${todayTasks.size} oggi completate")
                views.setTextViewText(R.id.widget_total_counts, "${tasks.count { it.completed }}/${tasks.size} totali")
                bindFilters(context, views, appWidgetId, activeFilter, palette)
                val rows = R.id.widget_task_rows
                views.removeAllViews(rows)
                visibleTasks.take(4).forEach { task ->
                    val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                    row.setInt(R.id.widget_row_root, "setBackgroundColor", palette.card)
                    row.setTextColor(R.id.widget_row_title, palette.text)
                    row.setTextColor(R.id.widget_row_meta, palette.muted)
                    row.setTextViewText(R.id.widget_row_title, taskTitleForWidget(task, hideTaskTitles))
                    row.setTextViewText(R.id.widget_row_meta, "${task.category} · ${task.time ?: "senza orario"}")
                    row.setTextViewText(R.id.widget_row_toggle, if (task.completed) "✓" else "○")
                    row.setTextColor(R.id.widget_row_toggle, if (task.completed) palette.success else palette.accent)
                    row.setOnClickPendingIntent(R.id.widget_row_toggle, toggleIntent(context, task))
                    row.setOnClickPendingIntent(R.id.widget_row_root, openAppIntent(context, addNew = false))
                    views.addView(rows, row)
                }
                if (visibleTasks.isEmpty()) {
                    val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                    row.setInt(R.id.widget_row_root, "setBackgroundColor", palette.card)
                    row.setTextColor(R.id.widget_row_title, palette.text)
                    row.setTextColor(R.id.widget_row_meta, palette.muted)
                    row.setTextViewText(R.id.widget_row_title, if (activeFilter == "today") "Giornata libera" else "Nessuna attività qui")
                    row.setTextViewText(R.id.widget_row_meta, "Tocca + per pianificare qualcosa")
                    row.setTextViewText(R.id.widget_row_toggle, "+")
                    row.setTextColor(R.id.widget_row_toggle, palette.accent)
                    row.setOnClickPendingIntent(R.id.widget_row_root, openAppIntent(context, addNew = true))
                    views.addView(rows, row)
                }
            }
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun bindNextTask(
        context: Context,
        views: RemoteViews,
        task: OrganizerTask?,
        palette: WidgetPalette,
        hideTaskTitles: Boolean
    ) {
        if (task == null) {
            views.setTextViewText(R.id.widget_next_title, "Nessuna attività in programma")
            views.setTextViewText(R.id.widget_next_meta, "Tocca + per aggiungerne una")
            views.setTextViewText(R.id.widget_next_toggle, "+")
            views.setTextColor(R.id.widget_next_toggle, palette.accent)
            views.setOnClickPendingIntent(R.id.widget_next_toggle, openAppIntent(context, addNew = true))
        } else {
            views.setTextViewText(R.id.widget_next_title, taskTitleForWidget(task, hideTaskTitles))
            views.setTextViewText(R.id.widget_next_meta, "Prossima · ${task.time ?: "senza orario"} · ${task.category}")
            views.setTextViewText(R.id.widget_next_toggle, "○")
            views.setTextColor(R.id.widget_next_toggle, palette.accent)
            views.setOnClickPendingIntent(R.id.widget_next_toggle, toggleIntent(context, task))
        }
    }

    private fun taskTitleForWidget(task: OrganizerTask, hidden: Boolean): String =
        if (hidden) "Attività pianificata" else task.title

    private fun bindFilters(context: Context, views: RemoteViews, appWidgetId: Int, activeFilter: String, palette: WidgetPalette) {
        listOf(
            R.id.widget_filter_all to "all",
            R.id.widget_filter_today to "today",
            R.id.widget_filter_todo to "todo",
            R.id.widget_filter_done to "done"
        ).forEach { (viewId, filter) ->
            val selected = filter == activeFilter
            views.setInt(viewId, "setBackgroundColor", if (selected) palette.accent else palette.card)
            views.setTextColor(viewId, if (selected) palette.background else palette.muted)
            views.setOnClickPendingIntent(viewId, filterIntent(context, appWidgetId, filter))
        }
    }

    private fun applyColors(views: RemoteViews, layout: Int, palette: WidgetPalette) {
        views.setColorStateList(R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(palette.background))
        views.setTextColor(R.id.widget_title, palette.text)
        views.setTextColor(R.id.widget_add, palette.accent)
        views.setTextColor(R.id.widget_progress, palette.text)
        views.setTextColor(R.id.widget_progress_label, palette.muted)
        views.setTextColor(R.id.widget_summary, palette.muted)
        when (layout) {
            R.layout.widget_small -> views.setTextColor(R.id.widget_motto, palette.muted)
            R.layout.widget_medium -> {
                views.setTextColor(R.id.widget_medium_heading, palette.text)
                views.setTextColor(R.id.widget_week_heading, palette.muted)
                views.setTextColor(R.id.widget_week_total, palette.accent)
                views.setTextColor(R.id.widget_week_done, palette.success)
                views.setTextColor(R.id.widget_week_todo, palette.accent)
                views.setTextColor(R.id.widget_week_total_label, palette.muted)
                views.setTextColor(R.id.widget_week_done_label, palette.muted)
                views.setTextColor(R.id.widget_week_todo_label, palette.muted)
                views.setTextColor(R.id.widget_next_title, palette.text)
                views.setTextColor(R.id.widget_next_meta, palette.muted)
            }
            else -> {
                views.setTextColor(R.id.widget_day, palette.muted)
                views.setTextColor(R.id.widget_total_counts, palette.muted)
            }
        }
    }

    private fun palette(context: Context): WidgetPalette {
        val mode = context.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE).getString("theme_mode", "original_dark")
        return when (mode) {
            "carta_dorata" -> WidgetPalette(Color.rgb(246, 240, 229), Color.rgb(255, 253, 248), Color.rgb(164, 117, 40), Color.rgb(20, 36, 59), Color.rgb(95, 104, 114), Color.rgb(45, 140, 105))
            "aurora_midnight" -> WidgetPalette(Color.rgb(23, 30, 58), Color.rgb(31, 41, 77), Color.rgb(56, 189, 248), Color.rgb(248, 250, 252), Color.rgb(203, 213, 225), Color.rgb(52, 211, 153))
            "aurora_nordic" -> WidgetPalette(Color.rgb(225, 246, 241), Color.rgb(248, 255, 255), Color.rgb(13, 148, 136), Color.rgb(15, 23, 42), Color.rgb(51, 65, 85), Color.rgb(5, 150, 105))
            else -> WidgetPalette(Color.rgb(16, 25, 43), Color.rgb(21, 32, 54), Color.rgb(215, 180, 106), Color.rgb(244, 246, 250), Color.rgb(174, 185, 201), Color.rgb(66, 184, 131))
        }
    }

    private fun progressRing(progress: Int, palette: WidgetPalette): Bitmap {
        val size = 180
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val bounds = RectF(16f, 16f, (size - 16).toFloat(), (size - 16).toFloat())
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 14f; strokeCap = Paint.Cap.ROUND }
            stroke.color = palette.card
            canvas.drawArc(bounds, 0f, 360f, false, stroke)
            stroke.color = if (progress == 100) palette.success else palette.accent
            canvas.drawArc(bounds, -90f, progress * 3.6f, false, stroke)
        }
    }

    private fun openAppIntent(context: Context, addNew: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NEW_TASK, addNew)
        }
        return PendingIntent.getActivity(context, if (addNew) 601 else 600, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun toggleIntent(context: Context, task: OrganizerTask): PendingIntent {
        val intent = Intent(context, TaskOrganizerProgressWidgetProvider::class.java).apply {
            action = TaskOrganizerWidgetProvider.ACTION_TOGGLE_TASK
            data = Uri.parse("taskorganizer://toggle/${task.id}")
            putExtra(TaskOrganizerWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        return PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun filterIntent(context: Context, appWidgetId: Int, filter: String): PendingIntent {
        val intent = Intent(context, TaskOrganizerProgressWidgetProvider::class.java).apply {
            action = TaskOrganizerWidgetProvider.ACTION_SET_FILTER
            data = Uri.parse("taskorganizer://filter/$appWidgetId/$filter")
            putExtra(TaskOrganizerWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
            putExtra(TaskOrganizerWidgetProvider.EXTRA_FILTER, filter)
        }
        return PendingIntent.getBroadcast(context, "$appWidgetId-$filter".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun formatDate(date: LocalDate): String = "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthValue.toString().padStart(2, '0')}"
}
