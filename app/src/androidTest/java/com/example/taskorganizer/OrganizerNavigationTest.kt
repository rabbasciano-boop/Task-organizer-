package com.example.taskorganizer

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test

class OrganizerNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectingAnotherTabChangesTheSelectedDestination() {
        var selected by mutableStateOf<OrganizerScreen>(OrganizerScreen.Today)
        compose.setContent {
            OrganizerNavigationBar(
                selected = selected,
                onSelected = { selected = it },
                barColor = Color.DarkGray,
                mutedColor = Color.LightGray,
                selectedColor = Color.Yellow,
                indicatorColor = Color.Gray
            )
        }

        compose.onNodeWithText("Oggi").assertIsSelected()
        compose.onNodeWithText("Calendario").performClick().assertIsSelected()
    }
}
