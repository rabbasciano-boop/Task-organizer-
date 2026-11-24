package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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

/** A real blurred light layer, used where the Sunset theme needs a visible neon halo. */
@Composable
internal fun SunsetGlowContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = OrganizerPalette.sunsetGlow,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.padding(if (enabled) if (strong) 11.dp else 7.dp else 0.dp)) {
        if (enabled) {
            Box(
                Modifier.matchParentSize()
                    .background(sunsetPrimaryBrush(), shape)
                    .blur(if (strong) 18.dp else 11.dp, BlurredEdgeTreatment.Unbounded)
            )
        }
        content()
    }
}
