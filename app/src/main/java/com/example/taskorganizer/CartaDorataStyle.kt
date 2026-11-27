package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal fun goldFoilBrush(alpha: Float = 1f): Brush = Brush.linearGradient(
    listOf(Color(0xFF9C7428), Color(0xFFE9D08F), Color(0xFFB88B35), Color(0xFFF0DDA6), Color(0xFF8F6A22)).map { it.copy(alpha = alpha) }
)

@Composable
internal fun GoldDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFB88B35)))))
        Box(Modifier.size(7.dp).rotate(45f).background(goldFoilBrush(), RoundedCornerShape(1.dp)))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color(0xFFB88B35), Color.Transparent))))
    }
}
