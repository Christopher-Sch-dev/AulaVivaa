package cl.duocuc.aulaviva.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Cyber-Academic Palette - Aula Viva
// High performance, professional, deep dark mode with electric accents.

// Primary - Electric Blue (Action/Focus)
val ElectricBlue = Color(0xFF00F0FF) 
val ElectricBlueDark = Color(0xFF00B8C4)
val ElectricBlueAlpha = Color(0x3300F0FF) // For glows

// Secondary - Deep Purple (Creative/Depth)
val DeepPurple = Color(0xFF7000FF)
val DeepPurpleLight = Color(0xFF9E47FF)

// Backgrounds - OLED Optimized
val CyberBlack = Color(0xFF050505) // Absolute darkest background
val BackgroundDark = Color(0xFF0A0E14) // Main background (Deep Blue-Grey)
val SurfaceDark = Color(0xFF11161D) // Cards/Surfaces
val SurfaceHighlight = Color(0xFF1A212C) // Hover/Active states

// Functional / State Colors
val CyberRed = Color(0xFFFF2A6D) // Critical Error / Stop
val NeonGreen = Color(0xFF00FF9D) // Success / Online
val TechGold = Color(0xFFFFD600) // Warning / Attention

// Text Colors
val TextPrimary = Color(0xFFF0F0F0) // Almost white for readability
val TextSecondary = Color(0xFFAAAAAA) // De-emphasized text
val TextTertiary = Color(0xFF666666) // Disabled/Hint

// Gradients
val GradientCyberMain = listOf(ElectricBlue, DeepPurple)
val GradientDarkSurface = listOf(SurfaceDark, BackgroundDark)

