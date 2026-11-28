package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupReminderCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buttonsOpenBackupAndPostpone() {
        var opened = 0
        var postponed = 0
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                BackupReminderCard(onBackup = { opened++ }, onLater = { postponed++ })
            }
        }
        compose.onNodeWithText("Apri backup").performClick()
        compose.onNodeWithText("Più tardi").performClick()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, postponed) }
    }
}
