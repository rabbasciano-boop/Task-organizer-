package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Rule
import org.junit.Test

class SettingsNotificationsRemovalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun notificationSectionIsNotInSettings() {
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                SettingsHubScreen(signedInAccount = null, onOpen = {}, onOpenPrivacy = {})
            }
        }
        compose.onNodeWithText("Notifiche").assertDoesNotExist()
    }
}
