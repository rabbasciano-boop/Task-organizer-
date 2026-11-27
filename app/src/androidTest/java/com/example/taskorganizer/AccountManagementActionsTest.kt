package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.Column
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountManagementActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelDoesNothingAndSignOutKeepsDataByDefault() {
        var choice: Boolean? = null
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                Column { AccountManagementActions(working = false, onSignOut = { choice = it }, onDeleteAccount = {}) }
            }
        }
        compose.onNodeWithText("Esci dall’account").performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertNull(choice) }
        compose.onNodeWithText("Esci dall’account").performClick()
        compose.onNodeWithText("Esci", substring = false).performClick()
        compose.runOnIdle { assertEquals(false, choice) }
    }

    @Test fun deleteAccountIsNotTriggeredByCancel() {
        var deleted: Boolean? = null
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                Column { AccountManagementActions(working = false, onSignOut = {}, onDeleteAccount = { deleted = it }) }
            }
        }
        compose.onNodeWithText("Elimina account").performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertNull(deleted) }
    }

    @Test fun localDataIsOnlyDeletedWhenExplicitlySelected() {
        var choice: Boolean? = null
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                Column { AccountManagementActions(working = false, onSignOut = { choice = it }, onDeleteAccount = {}) }
            }
        }
        compose.onNodeWithText("Esci dall’account").performClick()
        compose.onNodeWithText("Elimina anche i dati di questo profilo dal telefono").performClick()
        compose.onNodeWithText("Esci", substring = false).performClick()
        compose.runOnIdle { assertEquals(true, choice) }
    }
}
