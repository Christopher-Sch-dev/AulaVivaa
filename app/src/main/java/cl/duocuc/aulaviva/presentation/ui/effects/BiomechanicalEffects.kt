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
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sistema de venas orgánicas (como tu portafolio)
 * Pulsación simulando flujo sanguíneo digital
 */
@Composable
fun BiomechanicalVeins(
    modifier: Modifier = Modifier,
    veinColor: Color = Color(0xFF06B6D4).copy(alpha = 0.15f),
    veinCount: Int = 8
) {
    var pulsePhase by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            pulsePhase += 0.05f
            delay(33)
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(veinCount) { i ->
            val startX = size.width * (i / veinCount.toFloat())
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
