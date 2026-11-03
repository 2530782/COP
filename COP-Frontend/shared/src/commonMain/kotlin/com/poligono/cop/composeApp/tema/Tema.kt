package com.poligono.cop.composeApp.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Paleta de COP: verde azulado (asociado a salud y calma) con buen contraste
 * para personas con poca experiencia digital (COP-RC-07 Accesibilidad)
 * y el mismo estilo en todas las pantallas (COP-RC-10 Coherencia).
 */
val VerdeCop = Color(0xFF0F6E6E)
val VerdeCopClaro = Color(0xFFD2EEEB)
val VerdeCopOscuro = Color(0xFF0A4F4F)
val FondoCop = Color(0xFFF5F8F7)
val TextoPrincipal = Color(0xFF1A1C1C)
val TextoSecundario = Color(0xFF4A5555)

private val ColoresCop = lightColorScheme(
    primary = VerdeCop,
    onPrimary = Color.White,
    primaryContainer = VerdeCopClaro,
    onPrimaryContainer = VerdeCopOscuro,
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    background = FondoCop,
    onBackground = TextoPrincipal,
    surface = Color.White,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFADBD8),
    onErrorContainer = Color(0xFF5F1410)
)

/** Envuelve toda la app para que cada pantalla use estos colores. */
@Composable
fun CopTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresCop,
        content = contenido
    )
}
