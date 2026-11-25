package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.taskorganizer.ui.theme.OrganizerPalette

internal object SunsetGlowPreference {
    var intensity by mutableFloatStateOf(1f)
        private set
    private var initialized = false

    fun load(context: android.content.Context) {
        if (!initialized) {
            intensity = context.getSharedPreferences("task_organizer_settings", android.content.Context.MODE_PRIVATE)
                .getFloat("sunset_glow_intensity", 1f).coerceIn(.3f, 1.6f)
            initialized = true
        }
    }

    fun update(context: android.content.Context, value: Float) {
        intensity = value.coerceIn(.3f, 1.6f)
        context.getSharedPreferences("task_organizer_settings", android.content.Context.MODE_PRIVATE)
            .edit().putFloat("sunset_glow_intensity", intensity).apply()
    }
}

@Composable
internal fun rememberSunsetGlowIntensity(): Float {
    val context = LocalContext.current
    remember(context) { SunsetGlowPreference.load(context.applicationContext) }
    return SunsetGlowPreference.intensity
}

/** Accent treatment shared by the gradient themes. */
@Composable
internal fun sunsetPrimaryBrush(): Brush = when {
    OrganizerPalette.agendaReale -> Brush.linearGradient(listOf(Color(0xFFF5F8FC), Color(0xFFFFD6EB), Color(0xFFFF85C8), Color(0xFFFF69B4)))
    OrganizerPalette.currentTheme.isDark -> {
    Brush.linearGradient(listOf(Color(0xFFFFB347), Color(0xFFFF7B47), Color(0xFFE85D8A)))
    }
    else -> {
    Brush.linearGradient(listOf(Color(0xFFF59E42), Color(0xFFEA6A47), Color(0xFFD94E7A)))
    }
}

@Composable
internal fun sunsetTaskBrush(cardColor: String): Brush {
    if (OrganizerPalette.agendaReale && cardColor == DefaultCardColor) {
        return Brush.horizontalGradient(listOf(Color(0xFFFF69B4), Color(0xFFFF85C8), Color(0xFFFFD6EB), Color(0xFFE8EEF5)))
    }
    val colors = when (cardColor) {
        "oceano" -> if (OrganizerPalette.agendaReale) listOf(Color(0xFF4B9CC7), Color(0xFF88C7E8), Color(0xFFE8EEF5)) else listOf(Color(0xFF155E8D), Color(0xFF147FB5), Color(0xFF4F63B8))
        "salvia" -> if (OrganizerPalette.agendaReale) listOf(Color(0xFF4AB49B), Color(0xFF8BE0D0), Color(0xFFE8EEF5)) else listOf(Color(0xFF1E634E), Color(0xFF318368), Color(0xFF6AB692))
        "prugna" -> if (OrganizerPalette.agendaReale) listOf(Color(0xFF7C4B9D), Color(0xFFB17AC8), Color(0xFFFFD6EB)) else listOf(Color(0xFF4A3169), Color(0xFF7250A3), Color(0xFFB06CCF))
        "corallo" -> if (OrganizerPalette.agendaReale) listOf(Color(0xFFDA617A), Color(0xFFFB9BAE), Color(0xFFFFD6EB)) else listOf(Color(0xFFC0444C), Color(0xFFE06464), Color(0xFFF08AB0))
        "miele" -> if (OrganizerPalette.agendaReale) listOf(Color(0xFFC59243), Color(0xFFE8C77B), Color(0xFFF5E6BD)) else listOf(Color(0xFF9B650D), Color(0xFFD68B1A), Color(0xFFF0BF57))
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
    if (OrganizerPalette.gradientTaskCards) {
        clip(shape).background(sunsetPrimaryBrush())
    } else {
        background(Gold, shape)
    }

@Composable
internal fun Modifier.sunsetGlow(shape: Shape = RoundedCornerShape(20.dp), strong: Boolean = false): Modifier {
    if (!OrganizerPalette.gradientTaskCards) return this
    val intensity = if (OrganizerPalette.sunsetGlow) rememberSunsetGlowIntensity() else 1f
    val elevation = with(LocalDensity.current) { (if (strong) 14.dp else 8.dp).toPx() } * intensity
    val silver = Color(0xFFF5F8FC)
    val glowColor = if (OrganizerPalette.currentTheme.isDark) Color(0xFFFF7B47) else Color(0xFFEA6A47)
    val glowSecondary = if (OrganizerPalette.currentTheme.isDark) Color(0xFFE85D8A) else Color(0xFFF59E42)
    return graphicsLayer {
        this.shape = shape
        clip = false
        shadowElevation = elevation * if (strong) 1.55f else 1.35f
        ambientShadowColor = if (OrganizerPalette.agendaReale) Gold.copy(alpha = if (strong) 0.56f else 0.36f) else Gold.copy(alpha = if (strong) 0.68f else 0.44f)
        spotShadowColor = if (OrganizerPalette.agendaReale) silver.copy(alpha = if (strong) 0.46f else 0.26f) else AccentSecondary.copy(alpha = if (strong) 0.62f else 0.38f)
    }.then(if (OrganizerPalette.sunsetGlow) Modifier.drawWithCache {
        val strokeWidth = (if (strong) 4.dp else 3.dp).toPx() * intensity
        val rounded = shape.createOutline(size, layoutDirection, this)
        val outlinePath = Path().apply {
            when (rounded) {
                is Outline.Rectangle -> addRect(rounded.rect)
                is Outline.Rounded -> addRoundRect(rounded.roundRect)
                is Outline.Generic -> addPath(rounded.path)
            }
        }
        val glowRadius = (if (strong) 24.dp else 16.dp).toPx() * intensity
        onDrawWithContent {
            // Layered translucent outlines make a visible halo, with the centre
            // excluded so no unwanted shapes appear inside transparent cards.
            clipPath(outlinePath, clipOp = ClipOp.Difference) {
                for (layer in 8 downTo 1) {
                    val fraction = layer / 8f
                    drawOutline(
                        rounded,
                        color = (if (layer % 2 == 0) glowSecondary else glowColor)
                            .copy(alpha = ((if (strong) .105f else .075f) * intensity * (1f - fraction * .65f)).coerceIn(0f, 1f)),
                        style = Stroke(width = glowRadius * fraction * 2f)
                    )
                }
            }
            drawContent()
            // A subtle inner highlight follows the existing outline and never paints a rectangle.
            drawOutline(rounded, color = Color.White.copy(alpha = (.10f * intensity).coerceAtMost(.2f)), style = Stroke(width = strokeWidth))
        }
    } else Modifier).border(
        width = if (strong) 1.5.dp else 1.dp,
        brush = sunsetPrimaryBrush(),
        shape = shape
    )
}

@Composable
internal fun Modifier.sunsetTaskSurface(cardColor: String, shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    if (OrganizerPalette.gradientTaskCards) {
        clip(shape).background(sunsetTaskBrush(cardColor))
    } else {
        background(taskCardBackground(cardColor), shape)
    }
