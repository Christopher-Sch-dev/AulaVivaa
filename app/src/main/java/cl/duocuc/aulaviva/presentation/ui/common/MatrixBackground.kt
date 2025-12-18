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

    // Parameters - Optimized for high density visual impact
    val fontSize = 16.sp // Reduced from 20sp for higher density (more columns)
    val fontSizePx = with(density) { fontSize.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.toDp().toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.toDp().toPx() }
    
    val columns = (screenWidth / fontSizePx).toInt()
    
    // State for drops: y-position for each column
    // Initialize with random start positions scattered throughout the screen
    // State for drops and speeds
    val drops = remember { mutableStateListOf<Float>() }
    val speeds = remember { mutableStateListOf<Float>() }
    
    // Initialize
    LaunchedEffect(Unit) {
        if (drops.isEmpty()) {
            repeat(columns) {
                drops.add(Random.nextFloat() * screenHeight)
                speeds.add(Random.nextFloat() * 1.5f + 0.5f) // Speed multiplier 0.5x to 2.0x
            }
        }
    }

    // CRITICAL OPTIMIZATION: Animation Loop CON LIFECYCLE PAUSE
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(33L) // ~30 FPS for smoother glitch movement
                for (i in drops.indices) {
                    if (i < drops.size && i < speeds.size) {
                        // Reset when reaching bottom
                        if (drops[i] * fontSizePx > screenHeight && Random.nextFloat() > 0.95f) {
                            drops[i] = 0f
                            speeds[i] = Random.nextFloat() * 1.5f + 0.5f // New random speed
                        }
                        drops[i] += 1f * speeds[i] // Move by speed
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val letterSpacing = fontSizePx
            
            drops.forEachIndexed { i, dropY ->
                // Safety check for index out of bounds if resized
                if (i >= drops.size) return@forEachIndexed

                val text = characters.random().toString()
                
                // GLITCH JITTER: Random horizontal offset
                val jitterX = if (Random.nextFloat() > 0.98f) (Random.nextFloat() - 0.5f) * letterSpacing else 0f
                
                val x = i * letterSpacing + jitterX
                val y = dropY * letterSpacing

                // Only draw if visible and valid dimensions
                if (size.height > 0 && y > -letterSpacing && y < size.height + letterSpacing) {
                    
                    // GLITCH COLOR: Occasional white/cyan flash instead of green
                    val isGlitch = Random.nextFloat() > 0.97f
                    val color = if (isGlitch) {
                        if (Random.nextBoolean()) Color.White else Color(0xFF00FFFF) // Cyan
                    } else {
                        Color(0xFF00FF41) // Matrix Green
                    }

                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        topLeft = Offset(x, y),
                        style = TextStyle(
                            color = color,
                            fontSize = if (isGlitch) fontSize * 1.2f else fontSize, // Glitch size bump
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isGlitch) FontWeight.Black else FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
