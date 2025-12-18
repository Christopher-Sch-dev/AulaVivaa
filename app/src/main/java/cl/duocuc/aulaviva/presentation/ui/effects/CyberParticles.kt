package cl.duocuc.aulaviva.presentation.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Particle System Optimizado
 * Max 30 partículas, ciclo de vida simple.
 */
@Composable
fun CyberParticleBackground(
    maxParticles: Int = 30,
    modifier: Modifier = Modifier,
    particleColor: Color = Color(0xFF00FF41) // Default Matrix Green
) {
    val particles = remember {
        List(maxParticles) {
            CyberParticle()
        }
    }
    
    var time by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            time = System.currentTimeMillis()
            particles.forEach { it.update(time) }
            delay(33) // ~30 FPS
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            if (particle.alpha > 0f) {
                drawCircle(
                    color = particleColor.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(particle.x * size.width, particle.y * size.height)
                )
            }
        }
    }
}

private class CyberParticle {
    var x by mutableFloatStateOf(Random.nextFloat())
    var y by mutableFloatStateOf(Random.nextFloat())
    var size by mutableFloatStateOf(Random.nextFloat() * 3f + 1f)
    var alpha by mutableFloatStateOf(Random.nextFloat() * 0.5f)
    var speedY by mutableFloatStateOf(Random.nextFloat() * 0.005f + 0.001f)
    
    fun update(time: Long) {
        y += speedY
        if (y > 1f) {
            y = 0f
            x = Random.nextFloat()
        }
        // Flicker effect
        alpha = (Math.sin(time / 200.0 + x * 10) * 0.3 + 0.4).toFloat().coerceIn(0.1f, 0.7f)
    }
}
