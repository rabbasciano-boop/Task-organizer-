package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationOnboardingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enableButtonRequestsPermission() {
        var requested = false
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                NotificationOnboardingScreen(onEnable = { requested = true })
            }
        }
        compose.onNodeWithText("Attiva le notifiche").performClick()
        compose.runOnIdle { assertTrue(requested) }
    }
}
