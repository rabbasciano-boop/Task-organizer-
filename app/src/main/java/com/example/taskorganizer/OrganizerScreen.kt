package com.example.taskorganizer

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

/** Main destinations used by the app's bottom navigation. */
sealed interface OrganizerScreen {
    val labelResource: Int
    val icon: Int

    data object Today : OrganizerScreen { override val labelResource = R.string.nav_home; override val icon = R.drawable.ic_nav_home }
    data object Activities : OrganizerScreen { override val labelResource = R.string.nav_activities; override val icon = R.drawable.ic_nav_activities }
    data object Lists : OrganizerScreen { override val labelResource = R.string.nav_lists; override val icon = R.drawable.ic_nav_lists }
    data object Calendar : OrganizerScreen { override val labelResource = R.string.nav_calendar; override val icon = R.drawable.ic_nav_calendar }
    data object Options : OrganizerScreen { override val labelResource = R.string.nav_options; override val icon = R.drawable.ic_nav_settings }

    companion object { val all = listOf(Today, Activities, Lists, Calendar, Options) }
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
        Spacer(Modifier.width(7.dp))
        OrganizerScreen.all.forEach { screen ->
            val label = stringResource(screen.labelResource)
            val isSelected = selected == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelected(screen) },
                icon = {
                    Icon(
                        painter = painterResource(screen.icon),
                        contentDescription = if (isSelected) null else label
                    )
                },
                alwaysShowLabel = false,
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = mutedColor,
                    unselectedTextColor = mutedColor
                )
            )
        }
        Spacer(Modifier.width(7.dp))
    }
}
