package com.example.taskorganizer

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateListOf
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Rule
import org.junit.Test

class ActivitySearchTest {
    @get:Rule val compose = createComposeRule()

    private val tasks = listOf(
        OrganizerTask(id = "one", title = "Visita in Città", date = "2026-11-28", note = "Portare gli esami", category = "Salute"),
        OrganizerTask(id = "two", title = "Comprare pane", date = "2026-11-29", category = "Personale")
    )

    @Test fun searchFiltersResultsClearsAndSurvivesRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                ActivitiesScreen(
                    tasks = tasks, deletedTasks = emptyList(), onAdd = {}, onToggle = {}, onEdit = {}, onDelete = {},
                    onRestore = {}, onPermanentlyDelete = {}, onRestoreMany = {}, onPermanentlyDeleteMany = {}
                )
            }
        }
        compose.onNodeWithText("Cerca attività").performTextInput("CITTA esami")
        compose.onNodeWithText("Visita in Città").assertExists()
        compose.onNodeWithText("Comprare pane").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("CITTA esami").assertExists()
        compose.onNodeWithText("Visita in Città").assertExists()
        compose.onNodeWithContentDescription("Cancella ricerca").performClick()
        compose.onNodeWithText("CITTA esami").assertDoesNotExist()
        compose.onNodeWithText("Cerca attività").performTextInput("inesistente")
        compose.onNodeWithText("Nessuna attività trovata per «inesistente»").performScrollTo().assertExists()
    }

    @Test fun updatingTheTaskListRefreshesSearchResults() {
        val items = mutableStateListOf(tasks.first())
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                ActivitiesScreen(items, emptyList(), {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Cerca attività").performTextInput("visita")
        compose.onNodeWithText("Visita in Città").assertExists()
        compose.runOnIdle { items[0] = items[0].copy(title = "Appuntamento") }
        compose.onNodeWithText("Visita in Città").assertDoesNotExist()
        compose.onNodeWithText("Nessuna attività trovata per «visita»").performScrollTo().assertExists()
    }
}
