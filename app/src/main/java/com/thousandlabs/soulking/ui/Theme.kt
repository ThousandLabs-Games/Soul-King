package com.thousandlabs.soulking.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Sk {
    val Bg = Color(0xFF0A0A0E)
    val Surface = Color(0xFF15151C)
    val SurfaceHigh = Color(0xFF1F1F29)
    val Gold = Color(0xFFE9B949)
    val GoldDim = Color(0x33E9B949)
    val OnGold = Color(0xFF1C1405)
    val Text = Color(0xFFF1F1F5)
    val TextDim = Color(0xFF9B9BAA)
    val Divider = Color(0xFF26262F)
    val Good = Color(0xFF2FD07A)
    val Bad = Color(0xFFFF4D5E)

    /** Une couleur par joueur, bien distinctes en pleine partie. */
    val Players = listOf(
        Color(0xFFFF4D5E),
        Color(0xFF3D8BFF),
        Color(0xFF2FD07A),
        Color(0xFFB36BFF),
    )

    fun player(i: Int): Color = Players[i.mod(Players.size)]
}

private val scheme = darkColorScheme(
    primary = Sk.Gold,
    onPrimary = Sk.OnGold,
    primaryContainer = Sk.GoldDim,
    onPrimaryContainer = Sk.Gold,
    secondary = Sk.Gold,
    onSecondary = Sk.OnGold,
    background = Sk.Bg,
    onBackground = Sk.Text,
    surface = Sk.Bg,
    onSurface = Sk.Text,
    surfaceVariant = Sk.Surface,
    onSurfaceVariant = Sk.TextDim,
    surfaceContainer = Sk.Surface,
    surfaceContainerHigh = Sk.SurfaceHigh,
    surfaceContainerHighest = Sk.SurfaceHigh,
    surfaceContainerLow = Sk.Surface,
    outline = Sk.Divider,
    outlineVariant = Sk.Divider,
    error = Sk.Bad,
)

private val typography = Typography(
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Black),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun SoulKingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

/** Couronne au trait, l'emblème de l'appli. */
val CrownIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Crown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // socle
            moveTo(4f, 18.5f)
            lineTo(20f, 18.5f)
            lineTo(20f, 20.5f)
            lineTo(4f, 20.5f)
            close()
            // pointes
            moveTo(4.2f, 16.8f)
            lineTo(2.6f, 7.2f)
            lineTo(8.2f, 11.2f)
            lineTo(12f, 4.2f)
            lineTo(15.8f, 11.2f)
            lineTo(21.4f, 7.2f)
            lineTo(19.8f, 16.8f)
            close()
        }
    }.build()
}
