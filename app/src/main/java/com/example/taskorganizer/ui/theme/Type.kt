package com.example.taskorganizer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.R

val CartaHeadingFont = FontFamily(
    Font(R.font.cormorant_garamond_600, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_700, FontWeight.Bold)
)
val CartaBodyFont = FontFamily(
    Font(R.font.hanken_grotesk_400, FontWeight.Normal),
    Font(R.font.hanken_grotesk_500, FontWeight.Medium),
    Font(R.font.hanken_grotesk_600, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_700, FontWeight.Bold)
)

val CartaTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        displayMedium = base.displayMedium.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        displaySmall = base.displaySmall.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        headlineLarge = base.headlineLarge.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        headlineMedium = base.headlineMedium.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        headlineSmall = base.headlineSmall.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        titleLarge = base.titleLarge.copy(fontFamily = CartaHeadingFont, fontSynthesis = FontSynthesis.None),
        titleMedium = base.titleMedium.copy(fontFamily = CartaBodyFont),
        titleSmall = base.titleSmall.copy(fontFamily = CartaBodyFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = CartaBodyFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = CartaBodyFont),
        bodySmall = base.bodySmall.copy(fontFamily = CartaBodyFont),
        labelLarge = base.labelLarge.copy(fontFamily = CartaBodyFont),
        labelMedium = base.labelMedium.copy(fontFamily = CartaBodyFont),
        labelSmall = base.labelSmall.copy(fontFamily = CartaBodyFont)
    )
}

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
