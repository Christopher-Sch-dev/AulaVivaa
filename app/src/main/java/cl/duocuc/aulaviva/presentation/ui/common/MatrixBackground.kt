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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
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
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val characters = remember { "0123456789ABCDEFアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン@#$%&*" }
    val textMeasurer = rememberTextMeasurer()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Parameters - Optimized for high density visual impact
    val fontSize = 16.sp 
    val fontSizePx = with(density) { fontSize.toPx() }.coerceAtLeast(1f) // Prevent division by zero
    val screenWidth = with(density) { configuration.screenWidthDp.toDp().toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.toDp().toPx() }
    
    // SAFETY: Clamp columns to avoid OOM on huge screens or bugs
    val columns = (screenWidth / fontSizePx).toInt().coerceIn(1, 150) 
    
    // Debug log
    LaunchedEffect(columns) {
        android.util.Log.d("MatrixBackground", "Init: columns=$columns, w=$screenWidth, h=$screenHeight, font=$fontSizePx")
    }
    
    // State for drops and speeds
    val drops = remember { mutableStateListOf<Float>() }
    val speeds = remember { mutableStateListOf<Float>() }
    // OPTIMIZATION: Track which char to show per column to avoid new String() every frame
    // Styles for optimization
    val normalStyle = remember(fontSize) {
        TextStyle(
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
    val glitchStyle = remember(fontSize) {
        TextStyle(
            fontSize = fontSize * 1.2f,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )
    }

    // Pre-measure characters to optimize performance and avoid Constraints crash
    val characterCache = remember(textMeasurer, characters, fontSize) {
        characters.associate { char ->
            val charStr = char.toString()
            val constraints = Constraints() // Open constraints (0 to Inf)

            val normal = textMeasurer.measure(
                text = charStr,
                style = normalStyle,
                softWrap = false,
                constraints = constraints
            )
            val glitch = textMeasurer.measure(
                text = charStr,
                style = glitchStyle,
                softWrap = false,
                constraints = constraints
            )
            char to (normal to glitch)
        }
    }

    val charIndices = remember { mutableStateListOf<Int>() }
    
    // Initialize
    LaunchedEffect(Unit) {
        if (drops.isEmpty()) {
            repeat(columns) {
                drops.add(Random.nextFloat() * screenHeight)
                speeds.add(Random.nextFloat() * 1.5f + 0.5f) 
                charIndices.add(Random.nextInt(characters.length))
            }
        }
    }

    // Animation Loop
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(33L) 
                
                // Safety check to avoid index OOB if state isn't sync'd yet
                val size = minOf(drops.size, speeds.size, charIndices.size)
                
                for (i in 0 until size) {
                    if (drops[i] * fontSizePx > screenHeight && Random.nextFloat() > 0.95f) {
                        drops[i] = 0f
                        speeds[i] = Random.nextFloat() * 1.5f + 0.5f
                    }
                    drops[i] += 1f * speeds[i]
                    
                    // Change character occasionally
                    if (Random.nextFloat() > 0.90f) {
                       charIndices[i] = Random.nextInt(characters.length)
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
        if (columns > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val letterSpacing = fontSizePx
                
                // Draw loop - highly optimized
                val count = minOf(drops.size, charIndices.size)
                
                for (i in 0 until count) {
                    val dropY = drops[i]
                    
                    val charIndex = charIndices[i]
                    // Safety check index again
                    if (charIndex >= 0 && charIndex < characters.length) {
                        val text = characters[charIndex].toString() 
                        
                        val jitterX = if (Random.nextFloat() > 0.98f) (Random.nextFloat() - 0.5f) * letterSpacing else 0f
                        val x = i * letterSpacing + jitterX
                        val y = dropY * letterSpacing
    
                        if (size.height > 0 && y > -letterSpacing && y < size.height + letterSpacing) {
                            val isGlitch = Random.nextFloat() > 0.97f
                            val color = if (isGlitch) {
                                if (Random.nextBoolean()) Color.White else Color(0xFF00FFFF)
                            } else {
                                Color(0xFF00FF41)
                            }
    
                            // Use cached layout to avoid measurement during draw text (fixes crash & optimizes)
                            val cache = characterCache[characters[charIndex]]
                            if (cache != null) {
                                val (normalLayout, glitchLayout) = cache
                                val layoutToDraw = if (isGlitch) glitchLayout else normalLayout

                                drawText(
                                    textLayoutResult = layoutToDraw,
                                    color = color,
                                    topLeft = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
