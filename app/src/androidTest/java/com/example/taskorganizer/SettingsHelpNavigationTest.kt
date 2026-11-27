package com.example.taskorganizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsHelpNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exposesTermsPrivacyAndSupportFromSettings() {
        var opened = ""
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                SettingsHubScreen(signedInAccount = null, onOpen = { opened = it }, onOpenPrivacy = { opened = "privacy" })
            }
        }
        compose.onNodeWithText("Termini d’uso").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("terms", opened) }
        compose.onNodeWithText("Informativa sulla privacy").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("privacy", opened) }
        compose.onNodeWithText("Supporto").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("support", opened) }
    }
}
