package com.example.taskorganizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

internal const val BACKUP_REMINDER_PERIOD = 14L * 24 * 60 * 60 * 1000
internal const val BACKUP_CARD_SNOOZE = 7L * 24 * 60 * 60 * 1000
internal const val BACKUP_MIN_TASKS = 10
internal const val LAST_BACKUP_AT = "backup_last_export_at"
internal const val BACKUP_CARD_SNOOZED_UNTIL = "backup_card_snoozed_until"
private const val BASELINE = "backup_reminder_baseline"
private const val LAST_NOTICE = "backup_reminder_last_notice"
private const val PROFILE = "backup_reminder_profile"
private const val ID = 99330

internal fun shouldSuggestBackup(taskCount: Int, lastBackupAt: Long?, snoozedUntil: Long?, now: Long): Boolean =
    taskCount >= BACKUP_MIN_TASKS && (snoozedUntil == null || snoozedUntil <= now) &&
        (lastBackupAt == null || (lastBackupAt > 0L && now >= lastBackupAt && now - lastBackupAt >= BACKUP_REMINDER_PERIOD))

internal fun nextBackupNoticeAt(lastBackupAt: Long?, baseline: Long, lastNoticeAt: Long?): Long =
    maxOf(lastBackupAt ?: baseline, lastNoticeAt ?: 0L) + BACKUP_REMINDER_PERIOD

internal object BackupReminder {
    private fun prefs(context: Context, profile: String) = context.getSharedPreferences("task_organizer_profile_$profile", Context.MODE_PRIVATE)

    private fun pending(context: Context, profile: String, flags: Int): PendingIntent? {
        val intent = Intent(context, BackupReminderReceiver::class.java)
            .setAction("com.example.taskorganizer.BACKUP_REMINDER")
            .setData(Uri.Builder().scheme("taskorganizer").authority("backup-reminder").appendPath(profile).build())
            .putExtra(PROFILE, profile)
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    @Synchronized fun cancel(context: Context, profile: String) {
        pending(context, profile, PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
        context.getSystemService(NotificationManager::class.java).cancel(profile, ID)
    }

    @Synchronized fun schedule(context: Context, profile: String, now: Long = System.currentTimeMillis()) {
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        if (!TaskReminderScheduler.notificationsAllowed(context)) {
            cancel(context, profile)
            return
        }
        val prefs = prefs(context, profile)
        var baseline = prefs.getLong(BASELINE, 0L)
        if (baseline <= 0L || baseline > now) {
            baseline = now
            check(prefs.edit().putLong(BASELINE, baseline).commit())
        }
        val lastBackup = prefs.getLong(LAST_BACKUP_AT, 0L).takeIf { it > 0 }
        val lastNotice = prefs.getLong(LAST_NOTICE, 0L).takeIf { it > 0 }
        val due = maxOf(nextBackupNoticeAt(lastBackup, baseline, lastNotice), now + 60_000L)
        val pi = pending(context, profile, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi)
    }

    @Synchronized fun markBackup(context: Context, profile: String, now: Long = System.currentTimeMillis()) {
        val prefs = prefs(context, profile)
        check(prefs.edit().putLong(LAST_BACKUP_AT, now).putLong(BASELINE, now).remove(LAST_NOTICE).remove(BACKUP_CARD_SNOOZED_UNTIL).commit())
        runCatching { cancel(context, profile); schedule(context, profile, now) }
            .onFailure { Log.e("BackupReminder", "Could not schedule next backup reminder", it) }
    }

    @Synchronized fun receive(context: Context, intent: Intent) {
        if (intent.action != "com.example.taskorganizer.BACKUP_REMINDER") return
        val profile = intent.getStringExtra(PROFILE).orEmpty()
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        val now = System.currentTimeMillis()
        val prefs = prefs(context, profile)
        val baseline = prefs.getLong(BASELINE, 0L)
        val lastBackup = prefs.getLong(LAST_BACKUP_AT, 0L).takeIf { it > 0 }
        val lastNotice = prefs.getLong(LAST_NOTICE, 0L).takeIf { it > 0 }
        if (baseline <= 0L || now < nextBackupNoticeAt(lastBackup, baseline, lastNotice) || !TaskReminderScheduler.notificationsAllowed(context)) {
            schedule(context, profile, now)
            return
        }
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        TaskReminderScheduler.ensureNotificationChannels(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, TaskReminderScheduler.REMINDER_CHANNEL) else Notification.Builder(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setDefaults(Notification.DEFAULT_SOUND)
        val notification = builder.setSmallIcon(R.drawable.ic_stat_task_organizer)
            .setContentTitle(context.getString(R.string.backup_reminder_title))
            .setContentText(context.getString(R.string.backup_reminder_message))
            .setContentIntent(open).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(profile, ID, notification)
        check(prefs.edit().putLong(LAST_NOTICE, now).commit())
        schedule(context, profile, now)
    }
}

class BackupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try { BackupReminder.receive(context.applicationContext, intent) }
            catch (error: Exception) { Log.e("BackupReminder", "Could not process backup reminder", error) }
            finally { pending.finish() }
        }.start()
    }
}
