package com.example.taskorganizer

import android.app.Notification
import android.content.Intent
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationActionsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun actions(count: Int): Array<Notification.Action> {
        val source = Intent().putExtra("task_id", "test-task")
            .putExtra("profile_id", "guest").putExtra("reminder_index", 0).putExtra("snooze_count", count)
        val builder = Notification.Builder(context, TaskReminderScheduler.REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_task_organizer)
        ReminderNotificationActions.add(context, builder, source)
        return builder.build().actions
    }

    @Test fun actionsAreSeparateAndRequireAuthenticationOnSupportedDevices() {
        val actions = actions(0)
        assertEquals(listOf("Completa", "Posticipa 10 min"), actions.map { it.title.toString() })
        assertNotEquals(actions[0].actionIntent, actions[1].actionIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) assertTrue(actions.all { it.isAuthenticationRequired })
    }

    @Test fun thirdPostponementHasNoSnoozeButton() {
        assertEquals(listOf("Completa"), actions(3).map { it.title.toString() })
    }
}
