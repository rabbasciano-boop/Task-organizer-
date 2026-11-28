package com.example.taskorganizer

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskEditorSaveTest {
    @get:Rule val compose = createComposeRule()

    @Test fun topSaveKeepsEditedTaskInSunsetTheme() {
        var saved: OrganizerTask? = null
        val original = OrganizerTask(title = "Titolo iniziale", date = "2026-11-27", time = "10:00", endTime = "11:00")
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.SUNSET_GLOW) {
                TaskEditorScreen(
                    task = original, customCategories = emptyList(), customCategoryIcons = emptyMap(),
                    onAddCustomCategory = { _, _ -> }, onDeleteCustomCategory = {}, onCancel = {},
                    onSave = { saved = it }, notificationPermissionAllowed = true, onRequestNotificationPermission = {}
                )
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Titolo modificato")
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.runOnIdle {
            assertEquals(original.id, saved?.id)
            assertEquals("Titolo modificato", saved?.title)
        }
    }

    @Test fun changingTimeWarnsWhenTheOnlyReminderHasAlreadyPassed() {
        var saved: OrganizerTask? = null
        val soon = LocalDateTime.now().plusMinutes(6)
        val original = OrganizerTask(title = "Prova", date = soon.toLocalDate().toString(), time = soon.format(DateTimeFormatter.ofPattern("HH:mm")), reminderEnabled = true, reminderMinutesBefore = 15)
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                TaskEditorScreen(
                    task = original, customCategories = emptyList(), customCategoryIcons = emptyMap(),
                    onAddCustomCategory = { _, _ -> }, onDeleteCustomCategory = {}, onCancel = {},
                    onSave = { saved = it }, notificationPermissionAllowed = true, onRequestNotificationPermission = {}
                )
            }
        }
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.onNodeWithText("Anticipo già passato").assertExists()
        compose.onNodeWithText("Modifica anticipo").performClick()
        compose.runOnIdle { assertEquals(null, saved) }
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.onNodeWithText("Avvisami all’orario").performClick()
        compose.runOnIdle { assertEquals(0, saved?.reminderMinutesBefore) }
    }
}
