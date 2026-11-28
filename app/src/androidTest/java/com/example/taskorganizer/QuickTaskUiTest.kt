package com.example.taskorganizer

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickTaskUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draftSurvivesRestorationAndSavedTaskCanBeEdited() {
        var saved: OrganizerTask? = null
        var edited: OrganizerTask? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                TodayScreen(
                    tasks = emptyList(), nickname = "Utente", signedInAccount = null, onAdd = {},
                    onQuickAdd = { task, done -> saved = task; done(QuickTaskSaveResult.SAVED) },
                    onToggleTask = {}, onCompleteTask = {}, onDeleteTask = {}, onRestoreTask = {}, onEdit = { edited = it },
                    onOpenActivities = {}, onOpenLists = {}, onOpenCalendar = {}, onOpenTimeLog = {}, onManageAccount = {}, onOpenPrivacy = {}
                )
            }
        }
        compose.onNodeWithText("Aggiungi velocemente").performTextInput("Dentista domani alle 18")
        compose.onNodeWithText("Domani · 18:00").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Dentista domani alle 18").assertExists()
        compose.onNodeWithContentDescription("Crea attività").performClick()
        compose.runOnIdle {
            assertEquals("Dentista", saved?.title)
            assertEquals(LocalDate.now().plusDays(1).toString(), saved?.date)
            assertEquals("18:00", saved?.time)
            assertTrue(saved?.reminderEnabled == true)
        }
        compose.onNodeWithText("Modifica", substring = false).performClick()
        compose.runOnIdle { assertEquals(saved?.id, edited?.id) }
    }

    @Test fun failedSaveKeepsTextAndDoesNotShowSuccess() {
        var called = false
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                TodayScreen(
                    tasks = emptyList(), nickname = "Utente", signedInAccount = null, onAdd = {},
                    onQuickAdd = { _, done -> called = true; done(QuickTaskSaveResult.FAILED) },
                    onToggleTask = {}, onCompleteTask = {}, onDeleteTask = {}, onRestoreTask = {}, onEdit = {},
                    onOpenActivities = {}, onOpenLists = {}, onOpenCalendar = {}, onOpenTimeLog = {}, onManageAccount = {}, onOpenPrivacy = {}
                )
            }
        }
        compose.onNodeWithText("Aggiungi velocemente").performTextInput("Promemoria prova")
        compose.onNodeWithContentDescription("Crea attività").performClick()
        compose.runOnIdle { assertTrue(called) }
        compose.onNodeWithText("Promemoria prova").assertExists()
        compose.onNodeWithText("Attività creata").assertDoesNotExist()
    }
}
