package com.example.taskorganizer

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SnoozeRescheduleTest {
    @Test fun savingAChangedTaskCancelsTheOldSnooze() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = LocalProfiles.currentId()
        val task = OrganizerTask(id = UUID.randomUUID().toString(), title = "Test isolato", date = LocalDate.now().plusDays(1).toString(), time = "12:00")
        fun existing(): PendingIntent? {
            val index = TaskReminderScheduler.SNOOZE_INDEX
            val intent = Intent(context, TaskReminderReceiver::class.java)
                .setAction("com.example.taskorganizer.REMINDER")
                .setData(Uri.parse("taskorganizer://reminder/$profile/${task.id}/$index"))
            val requestCode = (task.id.hashCode() * 31 + index) and Int.MAX_VALUE
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        }
        try {
            TaskReminderScheduler.scheduleSnooze(context, task, profile, 1, System.currentTimeMillis())
            assertNotNull(existing())
            TaskReminderScheduler.schedule(context, task.copy(time = "13:00"), profile)
            assertNull(existing())
        } finally {
            TaskReminderScheduler.cancel(context, task.id, profile)
        }
    }
}
