package com.example.taskorganizer

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Main destinations used by the app's bottom navigation. */
sealed interface OrganizerScreen {
    val labelResource: Int
    val icon: Int

    data object Today : OrganizerScreen { override val labelResource = R.string.nav_today; override val icon = R.drawable.ic_nav_today }
    data object Activities : OrganizerScreen { override val labelResource = R.string.nav_activities; override val icon = R.drawable.ic_nav_activities }
    data object Calendar : OrganizerScreen { override val labelResource = R.string.nav_calendar; override val icon = R.drawable.ic_nav_calendar }
    data object Account : OrganizerScreen { override val labelResource = R.string.nav_account; override val icon = R.drawable.ic_nav_account }
    data object Options : OrganizerScreen { override val labelResource = R.string.nav_options; override val icon = R.drawable.ic_nav_settings }

    companion object { val all = listOf(Today, Activities, Calendar, Account, Options) }
}

@Composable
fun OrganizerNavigationBar(
    selected: OrganizerScreen,
    onSelected: (OrganizerScreen) -> Unit,
    barColor: Color,
    mutedColor: Color,
    selectedColor: Color,
    indicatorColor: Color
) {
    NavigationBar(containerColor = barColor, contentColor = mutedColor) {
        OrganizerScreen.all.forEach { screen ->
            val label = stringResource(screen.labelResource)
            NavigationBarItem(
                selected = selected == screen,
                onClick = { onSelected(screen) },
                icon = {
                    Icon(
                        painter = painterResource(screen.icon),
                        contentDescription = null,
                        modifier = Modifier.semantics { contentDescription = label }
                    )
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = mutedColor,
                    unselectedTextColor = mutedColor
                )
            )
        }
    }
}
