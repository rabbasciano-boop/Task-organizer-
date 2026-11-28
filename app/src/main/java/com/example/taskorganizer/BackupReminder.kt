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
private const val HAD_DATA = "backup_reminder_had_data"
private const val RETRY_COUNT = "backup_reminder_retry_count"
private const val PROFILE = "backup_reminder_profile"
private const val ID = 99330
internal const val BACKUP_RETRY_DELAY = 60L * 60 * 1000
internal const val BACKUP_MAX_RETRIES = 3

internal fun shouldSuggestBackup(taskCount: Int, lastBackupAt: Long?, snoozedUntil: Long?, now: Long): Boolean =
    taskCount >= BACKUP_MIN_TASKS && (snoozedUntil == null || snoozedUntil <= now) &&
        (lastBackupAt == null || (lastBackupAt > 0L && now >= lastBackupAt && now - lastBackupAt >= BACKUP_REMINDER_PERIOD))

internal fun nextBackupNoticeAt(lastBackupAt: Long?, baseline: Long, lastNoticeAt: Long?): Long =
    maxOf(lastBackupAt ?: 0L, baseline, lastNoticeAt ?: 0L) + BACKUP_REMINDER_PERIOD

internal fun backupBaselineAfterDataChange(baseline: Long, hadData: Boolean?, hasData: Boolean, now: Long): Long =
    if (baseline <= 0L || baseline > now || (hadData == false && hasData)) now else baseline

internal fun nextBackupRetryAt(retries: Int, now: Long): Long? =
    if (retries in 0 until BACKUP_MAX_RETRIES) now + BACKUP_RETRY_DELAY else null

private fun WorkShiftPreset.backupRelevantFields() = listOf(name, startTime, endTime, breakMinutes.toString())
private val defaultShiftFields = WorkLogSettings().presets.map { it.backupRelevantFields() }

internal fun hasDataToBackUp(archive: TaskArchive): Boolean =
    archive.tasks.isNotEmpty() || archive.deletedTasks.isNotEmpty() || archive.listItems.isNotEmpty() ||
        archive.workLogEntries.isNotEmpty() || archive.activeWorkTimerStartedAt != null ||
        archive.workLogSettings.hourlyRateCents != 0L || archive.workLogSettings.presets.map { it.backupRelevantFields() } != defaultShiftFields ||
        archive.customCategories.isNotEmpty() || archive.customCategoryIcons.isNotEmpty()

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

    @Synchronized fun schedule(context: Context, profile: String, now: Long = System.currentTimeMillis(), notBefore: Long = now + 60_000L) {
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        if (!TaskReminderScheduler.notificationsAllowed(context)) {
            cancel(context, profile)
            return
        }
        val prefs = prefs(context, profile)
        val hasData = hasDataToBackUp(EncryptedTaskStore(context, profileId = profile).load())
        val hadData = if (prefs.contains(HAD_DATA)) prefs.getBoolean(HAD_DATA, false) else null
        val baseline = backupBaselineAfterDataChange(prefs.getLong(BASELINE, 0L), hadData, hasData, now)
        check(prefs.edit().putLong(BASELINE, baseline).putBoolean(HAD_DATA, hasData).remove(RETRY_COUNT).commit())
        val lastBackup = prefs.getLong(LAST_BACKUP_AT, 0L).takeIf { it > 0 }
        val lastNotice = prefs.getLong(LAST_NOTICE, 0L).takeIf { it > 0 }
        val due = maxOf(nextBackupNoticeAt(lastBackup, baseline, lastNotice), notBefore)
        val pi = pending(context, profile, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi)
    }

    @Synchronized fun markBackup(context: Context, profile: String, now: Long = System.currentTimeMillis()) {
        val prefs = prefs(context, profile)
        check(prefs.edit().putLong(LAST_BACKUP_AT, now).putLong(BASELINE, now).remove(LAST_NOTICE).remove(BACKUP_CARD_SNOOZED_UNTIL).commit())
        runCatching { cancel(context, profile); schedule(context, profile, now) }
            .onFailure { Log.e("BackupReminder", "Could not schedule next backup reminder", it) }
    }

    @Synchronized fun retryAfterFailure(context: Context, profile: String, now: Long = System.currentTimeMillis()) {
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        if (!TaskReminderScheduler.notificationsAllowed(context)) return
        val preferences = prefs(context, profile)
        val count = preferences.getInt(RETRY_COUNT, 0)
        val retryAt = nextBackupRetryAt(count, now) ?: return
        val pi = pending(context, profile, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        check(preferences.edit().putInt(RETRY_COUNT, count + 1).commit())
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, retryAt, pi)
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
        val archive = EncryptedTaskStore(context, profileId = profile).load()
        if (prefs.contains(RETRY_COUNT)) prefs.edit().remove(RETRY_COUNT).apply()
        if (!hasDataToBackUp(archive)) {
            schedule(context, profile, now, now + BACKUP_REMINDER_PERIOD)
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
            catch (error: Exception) {
                Log.e("BackupReminder", "Could not process backup reminder", error)
                runCatching { BackupReminder.retryAfterFailure(context.applicationContext, intent.getStringExtra(PROFILE).orEmpty()) }
            }
            finally { pending.finish() }
        }.start()
    }
}
