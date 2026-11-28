package com.example.taskorganizer

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun activityListDoesNotOverlapNewTaskButton() {
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                TodayScreen(
                    tasks = emptyList(), nickname = "Utente", signedInAccount = null, onAdd = {},
                    onQuickAdd = { _, _ -> }, onToggleTask = {}, onCompleteTask = {}, onDeleteTask = {},
                    onRestoreTask = {}, onEdit = {}, onOpenActivities = {}, onOpenLists = {}, onOpenCalendar = {},
                    onOpenTimeLog = {}, onManageAccount = {}, onOpenPrivacy = {}
                )
            }
        }
        val listBounds = compose.onNodeWithTag("home_activity_list").getUnclippedBoundsInRoot()
        val buttonBounds = compose.onNodeWithContentDescription("Crea una nuova attività").getUnclippedBoundsInRoot()
        assertTrue("La lista deve finire sopra il pulsante", listBounds.bottom <= buttonBounds.top)
        compose.onNodeWithTag("home_activity_list").performScrollToNode(hasText("Nessuna attività in questa data"))
        val emptyBounds = compose.onNodeWithText("Nessuna attività in questa data").getUnclippedBoundsInRoot()
        assertTrue("La card vuota deve restare sopra il pulsante", emptyBounds.bottom <= buttonBounds.top)
    }
}
