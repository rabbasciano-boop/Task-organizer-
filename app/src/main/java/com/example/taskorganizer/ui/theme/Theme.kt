package com.example.taskorganizer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class OrganizerTheme(val storageKey: String, val isDark: Boolean) {
    ORIGINAL_DARK("original_dark", true),
    CARTA_DORATA("carta_dorata", false),
    AURORA_MIDNIGHT("aurora_midnight", true),
    AURORA_NORDIC("aurora_nordic", false);

    companion object {
        fun fromStorage(value: String?, fallbackDark: Boolean): OrganizerTheme =
            entries.firstOrNull { it.storageKey == value } ?: if (fallbackDark) ORIGINAL_DARK else CARTA_DORATA
    }
}

object OrganizerPalette {
    var currentTheme = OrganizerTheme.ORIGINAL_DARK
    var background = Color(0xFF0D1B2F)
    var card = Color(0xFF14243B)
    var cardLight = Color(0xFF1B2E48)
    var accent = Color(0xFFD7B46A)
    var muted = Color(0xFFAEB9C9)
    var text = Color(0xFFF4F6FA)
    var accentSecondary = Color(0xFFD7B46A)
    var success = Color(0xFF42B883)
    var danger = Color(0xFFE48C8C)
    var aurora = false

    fun apply(theme: OrganizerTheme) {
        currentTheme = theme
        aurora = theme == OrganizerTheme.AURORA_MIDNIGHT || theme == OrganizerTheme.AURORA_NORDIC
        when (theme) {
            OrganizerTheme.ORIGINAL_DARK -> {
                background = Color(0xFF0D1B2F); card = Color(0xFF14243B); cardLight = Color(0xFF1B2E48)
                accent = Color(0xFFD7B46A); accentSecondary = Color(0xFFD7B46A); muted = Color(0xFFAEB9C9); text = Color(0xFFF4F6FA); success = Color(0xFF42B883); danger = Color(0xFFE48C8C)
            }
            OrganizerTheme.CARTA_DORATA -> {
                background = Color(0xFFF6F0E5); card = Color(0xFFFFFDF8); cardLight = Color(0xFFF0E4D0)
                accent = Color(0xFF805C1D); accentSecondary = Color(0xFF805C1D); muted = Color(0xFF4C5864); text = Color(0xFF14243B); success = Color(0xFF1E7F57); danger = Color(0xFFB3261E)
            }
            OrganizerTheme.AURORA_MIDNIGHT -> {
                background = Color(0xFF070A18); card = Color(0xE6171830); cardLight = Color(0xB3161E3A)
                accent = Color(0xFF38BDF8); accentSecondary = Color(0xFFA855F7); muted = Color(0xFFCBD5E1); text = Color(0xFFF8FAFC); success = Color(0xFF4ADE80); danger = Color(0xFFFCA5A5)
            }
            OrganizerTheme.AURORA_NORDIC -> {
                background = Color(0xFFEEF6F8); card = Color(0xE6FFFFFF); cardLight = Color(0xCCFFFFFF)
                accent = Color(0xFF08766D); accentSecondary = Color(0xFF4F46E5); muted = Color(0xFF334155); text = Color(0xFF0F172A); success = Color(0xFF176B4D); danger = Color(0xFFB42318)
            }
        }
    }
}

@Composable
fun TaskOrganizerTheme(theme: OrganizerTheme = OrganizerTheme.ORIGINAL_DARK, content: @Composable () -> Unit) {
    OrganizerPalette.apply(theme)
    val colors = if (theme.isDark) {
        darkColorScheme(
            primary = OrganizerPalette.accent, onPrimary = OrganizerPalette.background,
            secondary = OrganizerPalette.accentSecondary, background = OrganizerPalette.background,
            onBackground = OrganizerPalette.text, surface = OrganizerPalette.card, onSurface = OrganizerPalette.text
        )
    } else {
        lightColorScheme(
            primary = OrganizerPalette.accent, onPrimary = Color.White,
            secondary = OrganizerPalette.accentSecondary, background = OrganizerPalette.background,
            onBackground = OrganizerPalette.text, surface = OrganizerPalette.card, onSurface = OrganizerPalette.text
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
