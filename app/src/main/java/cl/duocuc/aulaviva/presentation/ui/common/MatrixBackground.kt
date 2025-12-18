package cl.duocuc.aulaviva.presentation.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cl.duocuc.aulaviva.presentation.ui.theme.MatrixGreen
import cl.duocuc.aulaviva.presentation.ui.theme.MatrixDarkGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun MatrixBackground(
    modifier: Modifier = Modifier
) {
    // CRITICAL: Obtener lifecycle para pausar animación en background
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val characters = remember { "0123456789ABCDEFアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン@#$%&*" }
    val textMeasurer = rememberTextMeasurer()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Parameters - Optimized for performance
    val fontSize = 20.sp // Increased from 14.sp to reduce column count
    val fontSizePx = with(density) { fontSize.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.toDp().toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.toDp().toPx() }
    
    val columns = (screenWidth / fontSizePx).toInt()
    
    // State for drops: y-position for each column
    // Initialize with random start positions above screen or on screen
    val drops = remember { 
        mutableStateListOf<Float>().apply {
            repeat(columns) { 
                add(Random.nextFloat() * screenHeight * -1f) // Start above screen
            }
        }
    }

    // CRITICAL OPTIMIZATION: Animation Loop CON LIFECYCLE PAUSE
    // El loop SOLO corre cuando la Activity está en RESUMED state
    // Cuando la app va a background, el loop se PAUSA automáticamente
    // Esto ahorra batería y CPU significativamente
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Este bloque solo se ejecuta mientras la pantalla está visible
            while (isActive) {
                delay(50L) // 20 FPS - optimizado para balance visual/performance
                for (i in drops.indices) {
                    // Random reset logic - cuando la gota llega al fondo
                    if (drops[i] * fontSizePx > screenHeight && Random.nextFloat() > 0.975f) {
                        drops[i] = 0f // Reset to top
                    }
                    drops[i] += 1f // Move down by 1 row unit
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Matrix black base
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val letterSpacing = fontSizePx
            
            drops.forEachIndexed { i, dropY ->
                // Draw the "Head" (Bright)
                val text = characters.random().toString()
                val x = i * letterSpacing
                val y = dropY * letterSpacing

                // Only draw if visible
                if (y > -letterSpacing && y < size.height + letterSpacing) {
                    
                    // Removed heavy "Trail" text rendering for optimization.
                    // Only drawing the head character significantly reduces GPU/CPU load.
                    
                    // Head
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        topLeft = Offset(x, y),
                        style = TextStyle(
                            color = Color(0xFF00FF41), // Hardcoded Matrix Green for safety
                            fontSize = fontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
