package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.taskorganizer.ui.theme.OrganizerPalette

/** Accent treatment used only by the two Sunset Glow themes. */
@Composable
internal fun sunsetPrimaryBrush(): Brush = if (OrganizerPalette.currentTheme.isDark) {
    Brush.linearGradient(listOf(Color(0xFFFFB347), Color(0xFFFF7B47), Color(0xFFE85D8A)))
} else {
    Brush.linearGradient(listOf(Color(0xFFF59E42), Color(0xFFEA6A47), Color(0xFFD94E7A)))
}

@Composable
internal fun sunsetTaskBrush(cardColor: String): Brush {
    val colors = when (cardColor) {
        "oceano" -> listOf(Color(0xFF155E8D), Color(0xFF147FB5), Color(0xFF4F63B8))
        "salvia" -> listOf(Color(0xFF1E634E), Color(0xFF318368), Color(0xFF6AB692))
        "prugna" -> listOf(Color(0xFF4A3169), Color(0xFF7250A3), Color(0xFFB06CCF))
        "corallo" -> listOf(Color(0xFFC0444C), Color(0xFFE06464), Color(0xFFF08AB0))
        "miele" -> listOf(Color(0xFF9B650D), Color(0xFFD68B1A), Color(0xFFF0BF57))
        else -> if (OrganizerPalette.currentTheme.isDark) {
            listOf(Color(0xFFFFB347), Color(0xFFFF7B47), Color(0xFFE85D8A))
        } else {
            listOf(Color(0xFFF59E42), Color(0xFFEA6A47), Color(0xFFD94E7A))
        }
    }
    return Brush.linearGradient(colors.map { it.copy(alpha = 0.88f) })
}

@Composable
internal fun Modifier.sunsetPrimarySurface(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    if (OrganizerPalette.sunsetGlow) {
        clip(shape).background(sunsetPrimaryBrush())
    } else {
        background(Gold, shape)
    }

@Composable
internal fun Modifier.sunsetGlow(shape: Shape = RoundedCornerShape(20.dp), strong: Boolean = false): Modifier {
    if (!OrganizerPalette.sunsetGlow) return this
    val elevation = with(LocalDensity.current) { (if (strong) 14.dp else 8.dp).toPx() }
    return graphicsLayer {
        this.shape = shape
        clip = false
        shadowElevation = elevation * if (strong) 1.55f else 1.35f
        ambientShadowColor = Gold.copy(alpha = if (strong) 0.68f else 0.44f)
        spotShadowColor = AccentSecondary.copy(alpha = if (strong) 0.62f else 0.38f)
    }.border(
        width = if (strong) 1.5.dp else 1.dp,
        brush = sunsetPrimaryBrush(),
        shape = shape
    )
}

@Composable
internal fun Modifier.sunsetTaskSurface(cardColor: String, shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    if (OrganizerPalette.sunsetGlow) {
        clip(shape).background(sunsetTaskBrush(cardColor))
    } else {
        background(taskCardBackground(cardColor), shape)
    }
