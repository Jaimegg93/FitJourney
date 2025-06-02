package com.example.fitjourney.ui.theme

import ColorPrimario
import ErrorRojo
import FondoClaro
import FondoPantalla
import VerdeSecundario
import Superficie
import TextoOscuro
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography


private val LightColors = lightColorScheme(
    primary = ColorPrimario,
    secondary = VerdeSecundario,
    background = FondoClaro,
    surface = Superficie,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextoOscuro,
    onSurface = TextoOscuro,
    error = ErrorRojo ,
    primaryContainer = Color(0xFFE0F2FF),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFD7FFE0),
    onSecondaryContainer = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = ColorPrimario,
    secondary = VerdeSecundario,
    background = FondoPantalla,
    surface =  Color(0xFF1C1B1B),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRojo,
    primaryContainer = Color(0xFF1C1B1B),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2D2D2D),
    onSecondaryContainer = Color.White
)

@Composable
fun FitJourneyTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
