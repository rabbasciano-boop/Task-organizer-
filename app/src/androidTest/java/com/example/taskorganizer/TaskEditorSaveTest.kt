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
}
