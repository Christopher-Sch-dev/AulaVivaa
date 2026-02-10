package cl.duocuc.aulaviva.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,              // Cian para actions
    onPrimary = PrimaryDark,
    primaryContainer = SecondaryAccent,   // Violeta suave
    secondary = TertiaryAccent,           // Azul claro
    onSecondary = Color.White,
    tertiary = WarningOrange,
    background = PrimaryDark,             // Negro azulado
    onBackground = TextPrimary,
    surface = SurfaceLight,               // Tarjetas
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF2D3748),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF475569)
)

@Composable
fun AulaVivaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Forzamos dark scheme por diseño profesional
    val colorScheme = DarkColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AulaVivaTypography,
        shapes = AulaVivaShapes,
        content = content
    )
}
