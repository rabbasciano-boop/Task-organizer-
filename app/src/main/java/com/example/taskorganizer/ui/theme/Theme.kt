package com.example.taskorganizer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
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

data class OrganizerColorPalette(
    val theme: OrganizerTheme,
    val background: Color,
    val card: Color,
    val cardLight: Color,
    val accent: Color,
    val muted: Color,
    val text: Color,
    val accentSecondary: Color,
    val success: Color,
    val danger: Color
) {
    val aurora: Boolean get() = theme == OrganizerTheme.AURORA_MIDNIGHT || theme == OrganizerTheme.AURORA_NORDIC
}

private fun paletteFor(theme: OrganizerTheme): OrganizerColorPalette = when (theme) {
    OrganizerTheme.ORIGINAL_DARK -> OrganizerColorPalette(theme, Color(0xFF0D1B2F), Color(0xFF14243B), Color(0xFF1B2E48), Color(0xFFD7B46A), Color(0xFFAEB9C9), Color(0xFFF4F6FA), Color(0xFFD7B46A), Color(0xFF42B883), Color(0xFFE48C8C))
    OrganizerTheme.CARTA_DORATA -> OrganizerColorPalette(theme, Color(0xFFF6F0E5), Color(0xFFFFFDF8), Color(0xFFF0E4D0), Color(0xFF805C1D), Color(0xFF4C5864), Color(0xFF14243B), Color(0xFF805C1D), Color(0xFF1E7F57), Color(0xFFB3261E))
    OrganizerTheme.AURORA_MIDNIGHT -> OrganizerColorPalette(theme, Color(0xFF070A18), Color(0xE6171830), Color(0xB3161E3A), Color(0xFF38BDF8), Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFFA855F7), Color(0xFF4ADE80), Color(0xFFFCA5A5))
    OrganizerTheme.AURORA_NORDIC -> OrganizerColorPalette(theme, Color(0xFFEEF6F8), Color(0xE6FFFFFF), Color(0xCCFFFFFF), Color(0xFF08766D), Color(0xFF334155), Color(0xFF0F172A), Color(0xFF4F46E5), Color(0xFF176B4D), Color(0xFFB42318))
}

val LocalOrganizerPalette = staticCompositionLocalOf { paletteFor(OrganizerTheme.ORIGINAL_DARK) }

/** State-backed compatibility layer used by existing color helpers across the app. */
object OrganizerPalette {
    private var active by mutableStateOf(paletteFor(OrganizerTheme.ORIGINAL_DARK))
    val currentTheme get() = active.theme
    val background get() = active.background
    val card get() = active.card
    val cardLight get() = active.cardLight
    val accent get() = active.accent
    val muted get() = active.muted
    val text get() = active.text
    val accentSecondary get() = active.accentSecondary
    val success get() = active.success
    val danger get() = active.danger
    val aurora get() = active.aurora

    fun apply(theme: OrganizerTheme): OrganizerColorPalette = paletteFor(theme).also { active = it }
}

@Composable
fun TaskOrganizerTheme(theme: OrganizerTheme = OrganizerTheme.ORIGINAL_DARK, content: @Composable () -> Unit) {
    val palette = OrganizerPalette.apply(theme)
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
    // Do not key the entire composition by theme: changing appearance must preserve the
    // current tab, an open editor and any draft already being typed.
    CompositionLocalProvider(LocalOrganizerPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}
