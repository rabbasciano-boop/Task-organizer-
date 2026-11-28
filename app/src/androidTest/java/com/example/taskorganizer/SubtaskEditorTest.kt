package com.example.taskorganizer

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubtaskEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stepsSurviveRestorationAndAreSaved() {
        var saved: OrganizerTask? = null
        val original = OrganizerTask(id = "test", title = "Studio", date = "2026-11-28")
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                TaskEditorScreen(
                    task = original, customCategories = emptyList(), customCategoryIcons = emptyMap(),
                    onAddCustomCategory = { _, _ -> }, onDeleteCustomCategory = {}, onCancel = {},
                    onSave = { saved = it }, notificationPermissionAllowed = true, onRequestNotificationPermission = {}
                )
            }
        }
        compose.onNodeWithText("Nuovo passaggio").performScrollTo().performTextInput("Leggere capitolo")
        compose.onNodeWithText("Aggiungi", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Leggere capitolo").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Leggere capitolo").performScrollTo().assertExists()
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.runOnIdle {
            assertEquals("Leggere capitolo", saved?.subtasks?.single()?.title)
            assertTrue(saved?.subtasks?.single()?.done == true)
        }
    }
}
