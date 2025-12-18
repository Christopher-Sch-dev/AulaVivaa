package cl.duocuc.aulaviva.presentation.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * Particle System Optimizado con Lifecycle Pause
 * Max 30 partículas, ciclo de vida simple.
 * Se PAUSA cuando la app va a background para ahorrar batería.
 */
@Composable
fun CyberParticleBackground(
    maxParticles: Int = 80, // Aumentado de 30 para más densidad
    modifier: Modifier = Modifier,
    particleColor: Color = Color(0xFF00FF41) // Default Matrix Green
) {
    // CRITICAL: Obtener lifecycle para pausar en background
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val particles = remember {
        List(maxParticles) {
            CyberParticle()
        }
    }
    
    var time by remember { mutableLongStateOf(0L) }
    
    // CRITICAL OPTIMIZATION: Loop con lifecycle pause
    // Solo corre cuando la Activity está RESUMED
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                time = System.currentTimeMillis()
                particles.forEach { it.update(time) }
                delay(33) // ~30 FPS
            }
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
    var size by mutableFloatStateOf(Random.nextFloat() * 5f + 2f) // Aumentado: 2..7 radius
    var alpha by mutableFloatStateOf(Random.nextFloat() * 0.8f + 0.2f) // Más visibles
    var speedY by mutableFloatStateOf(Random.nextFloat() * 0.008f + 0.002f) // Más rápidas
    
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
