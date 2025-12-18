package cl.duocuc.aulaviva.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme // Deprecated for this theme but kept for compat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Cyber-Academic is a Dark-First theme. 
// We intentionally override light mode to maintain the "Cyber" aesthetic in all conditions.

private val MatrixColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = Color.Black,
    primaryContainer = MatrixGreenAlpha,
    onPrimaryContainer = MatrixGreen,

    secondary = MatrixDarkGreen,
    onSecondary = MatrixGreen,
    secondaryContainer = MatrixDarkGreen.copy(alpha = 0.5f),
    onSecondaryContainer = MatrixGreen,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondary,

    error = CyberRed,
    onError = Color.Black,
    
    outline = TextTertiary,
    outlineVariant = SurfaceHighlight
)

// We map Light Scheme to Dark Scheme because this app strictly enforces the Cyber aesthetic
// This ensures consistency even if user has Light Mode on system.
private val ForceDarkScheme = MatrixColorScheme

/**
 * Tema principal "AulaViva Cyber-Academic"
 *
 * Enforced Dark Mode for immersion and style consistency.
 */
@Composable
fun AulaVivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color by default to preserve our Brand Identity
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Optional: You could enable dynamic colors here if you really wanted to, 
            // but for "Branded" cyber aesthetic, we usually prefer our defined palette.
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicDarkColorScheme(context)
        }
        else -> MatrixColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Always treat status bar as if we are in dark mode (light icons)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AulaVivaTypography, // We will update Typography next step if needed
        content = content
    )
}

