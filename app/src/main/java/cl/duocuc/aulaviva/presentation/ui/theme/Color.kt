package cl.duocuc.aulaviva.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =================================================================
// SYSTEM DESIGN V2 - PROFESSIONAL (NEW)
// =================================================================

// Colores primarios (degradados sofisticados)
val PrimaryDark = Color(0xFF0F172A)      // Azul oscuro profundo
val PrimaryMedium = Color(0xFF1E293B)    // Gris-azul suave
val PrimaryAccent = Color(0xFF06B6D4)    // Cian (no verde brillante)

// Colores secundarios (para énfasis, NO predominante)
val SecondaryAccent = Color(0xFF8B5CF6)  // Violeta sutil
val TertiaryAccent = Color(0xFF0EA5E9)   // Azul claro

// Neutros
val SurfaceLight = Color(0xFF1A202C)     // Gris oscuro para tarjetas
// Note: Overriding legacy TextPrimary with new standard
val TextPrimary = Color(0xFFE2E8F0)      // Gris claro (no blanco puro)
val TextSecondary = Color(0xFF94A3B8)    // Gris medio

// Estados
val SuccessGreen = Color(0xFF10B981)     // Verde profesional (no fluo)
val ErrorRed = Color(0xFFF87171)         // Rojo suave
val WarningOrange = Color(0xFFFB923C)    // Naranja sutil

// =================================================================
// SYSTEM DESIGN V1 - LEGACY (MATRIX/GLITCH)
// PRESERVED FOR COMPATIBILITY
// =================================================================

// Primary - Terminal Green (Action/Focus)
val MatrixGreen = Color(0xFF00FF41)
val MatrixDarkGreen = Color(0xFF003B00)
val MatrixGreenAlpha = Color(0x3300FF41)

// Secondary - Glitch Red/Blue (Effects)
val GlitchRed = Color(0xFFFF003C)
val GlitchBlue = Color(0xFF04D9FF)

// Backgrounds - OLED Optimized
val CyberBlack = Color(0xFF000000) // True Black
val BackgroundDark = Color(0xFF050505) // Deepest Grey
val SurfaceDark = Color(0xFF0A0A0A) // Card Surface
val SurfaceHighlight = Color(0xFF111111) // Hover state

// Functional
val CyberRed = Color(0xFFFF2A6D)
val NeonGreen = Color(0xFF00FF41)
val TechGold = Color(0xFFFFD700)

// Text (Legacy)
// val TextPrimary = Color(0xFFE0E0E0) // REPLACED by V2
// val TextSecondary = Color(0xFF008F11) // REPLACED by V2 (Name collision) -> Renaming legacy if needed or assuming V2 is acceptable.
// Renaming legacy TextSecondary to avoid collision if it was used specifically for green text.
val TextSecondaryLegacy = Color(0xFF008F11) // Matrix Dark Text
val TextTertiary = Color(0xFF003B00) // Faint Code

// Gradients
val GradientMatrix = listOf(MatrixGreen, MatrixDarkGreen)
val GradientDarkSurface = listOf(SurfaceDark, BackgroundDark)
