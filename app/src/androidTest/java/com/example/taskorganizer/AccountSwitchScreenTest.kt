package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountSwitchScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun offersGoogleOrGuestAfterSignOut() {
        var selected = ""
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                AccountSwitchScreen(working = false, message = null, onGoogle = { selected = "google" }, onGuest = { selected = "guest" })
            }
        }
        compose.onNodeWithText("Accedi con Google").performClick()
        compose.runOnIdle { assertEquals("google", selected) }
        compose.onNodeWithText("Continua come ospite").performClick()
        compose.runOnIdle { assertEquals("guest", selected) }
    }
}
