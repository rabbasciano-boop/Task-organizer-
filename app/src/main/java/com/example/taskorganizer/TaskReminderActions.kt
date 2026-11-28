package com.example.taskorganizer

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

internal const val ACTION_ARCHIVE_CHANGED = "com.example.taskorganizer.ARCHIVE_CHANGED"
internal const val EXTRA_CHANGED_PROFILE = "changed_profile"
private const val COMPLETE = "com.example.taskorganizer.REMINDER_COMPLETE"
private const val SNOOZE = "com.example.taskorganizer.REMINDER_SNOOZE"
private const val MAX_SNOOZES = 3

internal fun canSnooze(count: Int): Boolean = count in 0 until MAX_SNOOZES
internal fun snoozeTriggerAt(now: Long): Long = now + 10 * 60_000L

internal fun completeFromNotification(archive: TaskArchive, taskId: String): TaskArchive? {
    val current = archive.tasks.firstOrNull { it.id == taskId && it.deletedAt == 0L && !it.completed } ?: return null
    return RecurrenceTiming.updateTask(archive, current.copy(completed = true))
}

internal object ReminderNotificationActions {
    fun add(context: Context, builder: Notification.Builder, source: Intent) {
        val taskId = TaskReminderScheduler.taskId(source)
        val profile = TaskReminderScheduler.profileId(source)
        if (taskId.isBlank() || profile.isBlank()) return
        fun action(kind: String, label: Int): Notification.Action {
            val target = Intent(context, TaskReminderActionReceiver::class.java)
                .setAction(kind)
                .setData(Uri.Builder().scheme("taskorganizer").authority("reminder-action")
                    .appendPath(profile).appendPath(taskId)
                    .appendPath(TaskReminderScheduler.reminderIndex(source).toString())
                    .appendPath(TaskReminderScheduler.snoozeCount(source).toString())
                    .appendPath(kind).build())
                .putExtras(source)
            val pending = PendingIntent.getBroadcast(context, 0, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val actionBuilder = Notification.Action.Builder(0, context.getString(label), pending)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) actionBuilder.setAuthenticationRequired(true)
            return actionBuilder.build()
        }
        builder.addAction(action(COMPLETE, R.string.notification_action_complete))
        if (canSnooze(TaskReminderScheduler.snoozeCount(source))) {
            builder.addAction(action(SNOOZE, R.string.notification_action_snooze))
        }
    }
}

class TaskReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != COMPLETE && intent.action != SNOOZE) return
        // On older Android versions actions cannot request device authentication themselves.
        if (context.getSystemService(KeyguardManager::class.java).isDeviceLocked) return
        val profile = TaskReminderScheduler.profileId(intent)
        val taskId = TaskReminderScheduler.taskId(intent)
        if (taskId.isBlank() || !LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        val result = goAsync()
        Thread {
            try {
                val app = context.applicationContext
                val manager = app.getSystemService(NotificationManager::class.java)
                val store = EncryptedTaskStore(app, profileId = profile)
                if (intent.action == COMPLETE) {
                    val updated = store.updateIf { archive ->
                        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) null
                        else completeFromNotification(archive, taskId)
                    }
                    if (updated != null) {
                        app.sendBroadcast(Intent(ACTION_ARCHIVE_CHANGED).setPackage(app.packageName).putExtra(EXTRA_CHANGED_PROFILE, profile))
                        TaskReminderScheduler.cancel(app, taskId, profile)
                        RecurrenceTiming.reminderTasks(updated, taskId).forEach { TaskReminderScheduler.schedule(app, it, profile) }
                        TaskWidgetUpdater.updateAll(app)
                    }
                    // Dismiss every displayed reminder for this task after a successful or stale action.
                    for (index in 0..TaskReminderScheduler.SNOOZE_INDEX) manager.cancel(taskId, index)
                } else {
                    val count = TaskReminderScheduler.snoozeCount(intent)
                    val task = if (store.hasArchive()) store.load().tasks.firstOrNull { it.id == taskId && it.deletedAt == 0L && !it.completed && it.reminderEnabled && it.time != null } else null
                    if (task != null && canSnooze(count)) TaskReminderScheduler.scheduleSnooze(app, task, profile, count + 1, System.currentTimeMillis())
                    manager.cancel(taskId, TaskReminderScheduler.reminderIndex(intent))
                }
            } catch (error: Exception) {
                // Keep the notification available for retry when storage or scheduling fails.
                Log.e("ReminderActions", "Could not process reminder action", error)
            } finally {
                result.finish()
            }
        }.start()
    }
}
