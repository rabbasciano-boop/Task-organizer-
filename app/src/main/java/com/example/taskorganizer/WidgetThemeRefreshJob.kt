package com.example.taskorganizer

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/**
 * Android does not deliver configuration-change broadcasts to apps that are closed.
 * This light periodic refresh keeps a Home-screen widget aligned with the system theme
 * without collecting data or starting a foreground service.
 */
class WidgetThemeRefreshJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            TaskWidgetUpdater.updateAll(applicationContext)
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 7_041
        private const val MIN_REFRESH_INTERVAL_MILLIS = 15 * 60 * 1_000L
        private const val SETTINGS_PREFERENCES = "task_organizer_settings"

        fun schedule(context: Context, widgetsInstalled: Boolean) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            val followsSystemTheme = context
                .getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean("follow_system_theme", false)
            if (!widgetsInstalled || !followsSystemTheme) {
                scheduler.cancel(JOB_ID)
                return
            }
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, WidgetThemeRefreshJob::class.java))
                .setPeriodic(MIN_REFRESH_INTERVAL_MILLIS)
                .setRequiresBatteryNotLow(false)
                .build()
            scheduler.schedule(job)
        }
    }
}
