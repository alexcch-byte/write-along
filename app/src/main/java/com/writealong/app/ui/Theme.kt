package com.writealong.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.writealong.app.glyph.Script

object Wa {
    val Paper = Color(0xFFFFF8EC)
    val Panel = Color(0xFFFFEFD6)
    val Card = Color(0xFFFFFFFF)
    val Text = Color(0xFF2B2D42)
    val SubText = Color(0xFF6B6F80)

    val Primary = Color(0xFF5B6CF0)
    val PrintColor = Color(0xFFFF8A3D)
    val CursiveColor = Color(0xFF9B5DE5)
    val FreeColor = Color(0xFF14B8A6)
    val Good = Color(0xFF22A861)
    val Warn = Color(0xFFE9A23B)
    val Bad = Color(0xFFE5484D)
    val Star = Color(0xFFFFC233)
    val StarEmpty = Color(0xFFE3DED3)

    // Writing lines
    val TopLine = Color(0xFF5B6CF0)
    val MidLine = Color(0xFF8C98F5)
    val BaseLine = Color(0xFFE5484D)
    val TailLine = Color(0xFFC9C3B8)
    val Zone = Color(0x0F5B6CF0)

    // Guides
    val Road = Color(0xFFE4E1DA)
    val RoadActive = Color(0xFFFFE3A3)
    val RoadDone = Color(0xFFC8EED6)
    val Dotted = Color(0xFF9A97A0)
    val Faint = Color(0x33858290)
    val Arrow = Color(0xFF55525C)
    val StartDot = Color(0xFF22A861)
    val StopDot = Color(0xFFE5484D)

    val Ink = Color(0xFF2B2D42)
    val PenColors = listOf(
        Color(0xFF2B2D42),
        Color(0xFF2F6BFF),
        Color(0xFFE63946),
        Color(0xFF16A34A),
        Color(0xFF8E44AD),
    )

    fun scriptColor(script: Script) = if (script == Script.PRINT) PrintColor else CursiveColor
}

private val Rounded = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
    headlineMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    labelLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 13.sp),
)

@Composable
fun WriteAlongTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Wa.Primary,
            onPrimary = Color.White,
            background = Wa.Paper,
            surface = Wa.Card,
            onSurface = Wa.Text,
            onBackground = Wa.Text,
        ),
        typography = AppTypography,
        content = content,
    )
}
