package cl.duocuc.aulaviva.presentation.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sistema de venas orgánicas (como tu portafolio)
 * Pulsación simulando flujo sanguíneo digital
 * Se PAUSA cuando la app va a background para ahorrar batería.
 */
@Composable
fun BiomechanicalVeins(
    modifier: Modifier = Modifier,
    veinColor: Color = Color(0xFF06B6D4).copy(alpha = 0.15f),
    veinCount: Int = 8
) {
    // CRITICAL: Obtener lifecycle para pausar en background
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var pulsePhase by remember { mutableFloatStateOf(0f) }
    
    // CRITICAL OPTIMIZATION: Loop con lifecycle pause
    // Solo corre cuando la Activity está RESUMED
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                pulsePhase += 0.05f
                delay(33)
            }
        }
    }
    
    // SAFETY LIMIT: Clamp veinCount to avoid excessive object allocation
    val safeVeinCount = veinCount.coerceIn(1, 20)

    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(safeVeinCount) { i ->
            val startX = size.width * (i / safeVeinCount.toFloat())
            val path = Path().apply {
                moveTo(startX, 0f)
                
                // Curva orgánica (bezier)
                val segments = 10
                for (j in 0..segments) {
                    val t = j / segments.toFloat()
                    val x = startX + sin(t * 3.14f + pulsePhase + i) * 50f
                    val y = size.height * t
                    
                    if (j == 0) {
                        lineTo(x, y)
                    } else {
                        cubicTo(
                            x - 20f, y - 50f,
                            x + 20f, y + 50f,
                            x, y
                        )
                    }
                }
            }
            
            // Pulso (grosor variable)
            val thickness = 2f + sin(pulsePhase * 2f + i) * 1f
            
            drawPath(
                path = path,
                color = veinColor,
                style = Stroke(
                    width = thickness,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
