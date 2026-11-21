package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme
internal fun taskCardBackground(color: String): Color = when (color) {
    "oceano" -> Color(0xFF23456E)
    "salvia" -> Color(0xFF245548)
    "prugna" -> Color(0xFF4A345D)
    "corallo" -> Color(0xFF6B3940)
    "miele" -> Color(0xFF635021)
    else -> CardBlue
}

internal fun taskCardTextColor(task: OrganizerTask): Color = if (task.cardColor == DefaultCardColor) MainText else Color(0xFFF7F9FC)
internal fun taskCardMutedColor(task: OrganizerTask): Color = if (task.cardColor == DefaultCardColor) SoftText else Color(0xFFD8E1EC)
internal fun taskCompletionColor(task: OrganizerTask): Color = when {
    task.cardColor == DefaultCardColor && task.completed -> Success
    task.cardColor == DefaultCardColor -> Gold
    task.completed -> Color(0xFF7EE2B1)
    else -> Color(0xFFE8C877)
}
internal val CustomCategoryIconOptions = listOf("🏷️", "📌", "❤️", "🏃", "💊", "✈️", "🎨", "💰", "🛒", "🐾", "🏡", "🎯")

internal fun categoryIcon(category: String, iconOverride: String? = null): String = iconOverride ?: when (category) {
    "Studio" -> "📚"
    "Personale" -> "🏠"
    "Lavoro" -> "💼"
    "Esami" -> "🎓"
    else -> "🏷️"
}

internal fun categoryDisplay(category: String, iconOverride: String? = null): String = "${categoryIcon(category, iconOverride)}  $category"

private fun auroraBackgroundBrush(): Brush? = when (OrganizerPalette.currentTheme) {
    OrganizerTheme.AURORA_MIDNIGHT -> Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF241344), Color(0xFF123842), Color(0xFF0F766E))
    )
    OrganizerTheme.AURORA_NORDIC -> Brush.linearGradient(
        colors = listOf(Color(0xFFDDF5F0), Color(0xFFDCE8FF), Color(0xFFF4E4F5), Color(0xFFE1F6F1))
    )
    else -> null
}

internal fun Modifier.organizerBackground(): Modifier = auroraBackgroundBrush()?.let { background(it) } ?: background(NightBlue)

internal fun openingHaloBrush(): Brush = when (OrganizerPalette.currentTheme) {
    OrganizerTheme.ORIGINAL_DARK -> Brush.radialGradient(listOf(Gold.copy(alpha = 0.20f), Color.Transparent))
    OrganizerTheme.CARTA_DORATA -> Brush.radialGradient(listOf(Gold.copy(alpha = 0.24f), Color.Transparent))
    OrganizerTheme.AURORA_MIDNIGHT -> Brush.radialGradient(listOf(AccentSecondary.copy(alpha = 0.28f), Gold.copy(alpha = 0.10f), Color.Transparent))
    OrganizerTheme.AURORA_NORDIC -> Brush.radialGradient(listOf(Gold.copy(alpha = 0.18f), AccentSecondary.copy(alpha = 0.10f), Color.Transparent))
}
