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
    AURORA_NORDIC("aurora_nordic", false),
    SUNSET_GLOW("sunset_glow", false),
    SUNSET_GLOW_DARK("sunset_glow_dark", true),
    AGENDA_REALE("agenda_reale", true);

    companion object {
        fun fromStorage(value: String?, fallbackDark: Boolean): OrganizerTheme =
            entries.firstOrNull { it.storageKey == value } ?: if (fallbackDark) ORIGINAL_DARK else CARTA_DORATA
    }
}

val OrganizerTheme.isSunsetGlow: Boolean
    get() = this == OrganizerTheme.SUNSET_GLOW || this == OrganizerTheme.SUNSET_GLOW_DARK

val OrganizerTheme.isAgendaReale: Boolean
    get() = this == OrganizerTheme.AGENDA_REALE

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
    val sunsetGlow: Boolean get() = theme.isSunsetGlow
    val agendaReale: Boolean get() = theme.isAgendaReale
    val gradientTaskCards: Boolean get() = sunsetGlow || agendaReale
}

private fun paletteFor(theme: OrganizerTheme): OrganizerColorPalette = when (theme) {
    OrganizerTheme.ORIGINAL_DARK -> OrganizerColorPalette(theme, Color(0xFF0D1B2F), Color(0xFF14243B), Color(0xFF1B2E48), Color(0xFFD7B46A), Color(0xFFAEB9C9), Color(0xFFF4F6FA), Color(0xFFD7B46A), Color(0xFF42B883), Color(0xFFE48C8C))
    OrganizerTheme.CARTA_DORATA -> OrganizerColorPalette(theme, Color(0xFFF6F0E5), Color(0xFFFFFDF8), Color(0xFFF0E4D0), Color(0xFF805C1D), Color(0xFF4C5864), Color(0xFF14243B), Color(0xFF805C1D), Color(0xFF1E7F57), Color(0xFFB3261E))
    OrganizerTheme.AURORA_MIDNIGHT -> OrganizerColorPalette(theme, Color(0xFF070A18), Color(0xE6171830), Color(0xB3161E3A), Color(0xFF38BDF8), Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFFA855F7), Color(0xFF4ADE80), Color(0xFFFCA5A5))
    OrganizerTheme.AURORA_NORDIC -> OrganizerColorPalette(theme, Color(0xFFEEF6F8), Color(0xE6FFFFFF), Color(0xCCFFFFFF), Color(0xFF08766D), Color(0xFF334155), Color(0xFF0F172A), Color(0xFF4F46E5), Color(0xFF176B4D), Color(0xFFB42318))
    OrganizerTheme.SUNSET_GLOW -> OrganizerColorPalette(theme, Color(0xFFFFF6EE), Color(0xFFFFFCF8), Color(0xFFFFF5EC), Color(0xFFEA6A47), Color(0xFF6E4752), Color(0xFF2B1620), Color(0xFFD94E7A), Color(0xFF188B7C), Color(0xFFD94E7A))
    OrganizerTheme.SUNSET_GLOW_DARK -> OrganizerColorPalette(theme, Color(0xFF140D1C), Color(0xFF211530), Color(0xFF28193B), Color(0xFFFF7B47), Color(0xFFE3B8B4), Color(0xFFFFF4EC), Color(0xFFE85D8A), Color(0xFF71D4B0), Color(0xFFFF91AE))
    OrganizerTheme.AGENDA_REALE -> OrganizerColorPalette(theme, Color(0xFF230B2E), Color(0xFF46175A), Color(0xFF341143), Color(0xFFFF69B4), Color(0xFFC8D3E0), Color(0xFFF5F8FC), Color(0xFFFFD6EB), Color(0xFF5EEAD4), Color(0xFFFB7185))
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
    val sunsetGlow get() = active.sunsetGlow
    val agendaReale get() = active.agendaReale
    val gradientTaskCards get() = active.gradientTaskCards

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
