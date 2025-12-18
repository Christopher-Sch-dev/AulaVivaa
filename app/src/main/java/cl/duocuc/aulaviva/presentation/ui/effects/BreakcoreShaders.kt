package cl.duocuc.aulaviva.presentation.ui.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * BREAKCORE SHADER: Multi-glitch agresivo
 * Inspirado en tu Portafolio: Pixel Sort + Screen Tear + Chromatic Aberration + Noise
 * Se activa cada 3-5 segundos por 500ms (más frecuente que antes)
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val breakcoreGlitchShader = """
    uniform shader composable;
    uniform float time;
    uniform vec2 resolution;
    uniform float intensity;
    uniform float glitchType; // 0=chromatic, 1=tear, 2=pixelSort, 3=noise
    
    // Función de ruido (para efectos aleatorios)
    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }
    
    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / resolution;
        
        if (intensity < 0.1) {
            return composable.eval(fragCoord);
        }
        
        // CHROMATIC ABERRATION (RGB split agresivo)
        if (glitchType < 1.0) {
            float offset = intensity * 8.0;
            half4 r = composable.eval(fragCoord + vec2(offset, 0));
            half4 g = composable.eval(fragCoord);
            half4 b = composable.eval(fragCoord - vec2(offset, 0));
            return half4(r.r, g.g, b.b, 1.0);
        }
        
        // SCREEN TEAR (desplazamiento horizontal agresivo)
        if (glitchType < 2.0) {
            float tear = step(0.85, hash(vec2(uv.y * 20.0, time))) * intensity;
            vec2 tearOffset = vec2(tear * 50.0, 0);
            return composable.eval(fragCoord + tearOffset);
        }
        
        // PIXEL SORT (simulado con desplazamiento vertical)
        if (glitchType < 3.0) {
            float brightness = dot(composable.eval(fragCoord).rgb, vec3(0.299, 0.587, 0.114));
            float sortOffset = brightness * intensity * 15.0;
            return composable.eval(fragCoord + vec2(0, sortOffset));
        }
        
        // DIGITAL NOISE (corrupción de datos)
        float noise = hash(uv + time) * intensity;
        half4 color = composable.eval(fragCoord);
        return color + half4(noise, noise, noise, 0.0) * 0.5;
    }
""".trimIndent()

/**
 * Grid Cyberpunk Infinito (como tu portafolio)
 * Perspectiva + líneas que se alejan
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val cyberGridShader = """
    uniform shader composable;
    uniform float time;
    uniform vec2 resolution;
    
    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / resolution;
        half4 color = composable.eval(fragCoord);
        
        // Grid infinito con perspectiva
        vec2 gridUV = uv * 20.0;
        gridUV.y += time * 2.0; // Movimiento hacia adelante
        
        // Perspectiva (más cerca = más grande)
        float depth = 1.0 - uv.y;
        gridUV /= depth * 0.5 + 0.5;
        
        // Líneas del grid
        // fwidth causa crash en algunos dispositivos/emuladores (error: no match for fwidth(float2))
        // Usamos un valor fijo ajustado por profundidad para evitar el crash
        vec2 grid = abs(fract(gridUV - 0.5) - 0.5) / (0.02 / depth); 
        float line = min(grid.x, grid.y);
        float gridValue = 1.0 - min(line, 1.0);
        
        // Color cyan con fade por distancia
        vec3 gridColor = vec3(0.02, 0.71, 0.83) * gridValue * depth;
        
        return color + half4(gridColor * 0.3, 0.0);
    }
""".trimIndent()

/**
 * Scan Lines Agresivas (No Sutiles)
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val aggressiveScanLineShader = """
    uniform shader composable;
    uniform float time;
    uniform vec2 resolution;
    
    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / resolution;
        half4 color = composable.eval(fragCoord);
        
        // Scan line MÁS EVIDENTE (cada 2 píxeles, no cada 3)
        float scanLine = sin(uv.y * resolution.y * 0.5 + time * 3.0) * 0.08; // 0.08 en vez de 0.02
        
        // Flicker aleatorio (breakcore style)
        float flicker = step(0.95, fract(sin(time * 10.0) * 1000.0)) * 0.05;
        
        return color + half4(scanLine + flicker, scanLine + flicker, scanLine + flicker, 0.0);
    }
""".trimIndent()

/**
 * MODIFIER BREAKCORE: Glitch agresivo cada 3-5 segundos
 */
@Composable
fun Modifier.breakcoreGlitch(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }
    
    var error by remember { mutableStateOf(false) }
    if (error) return this

    return try {
        val shader = remember { RuntimeShader(breakcoreGlitchShader) }
        var time by remember { mutableFloatStateOf(0f) }
        var intensity by remember { mutableFloatStateOf(0f) }
        var glitchType by remember { mutableFloatStateOf(0f) }
        var nextGlitchTime by remember { mutableFloatStateOf(3f) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    time += 0.016f
                    
                    // Glitch aleatorio cada 2-4 segundos (Más agresivo - Breakcore Style)
                    if (time >= nextGlitchTime && intensity == 0f) {
                        intensity = Random.nextFloat() * 0.5f + 0.5f // Intensidad 0.5 a 1.0
                        glitchType = Random.nextInt(4).toFloat() // 0-3 (4 tipos)
                        delay(Random.nextLong(150, 400)) // Duración variable corta (stuttery)
                        intensity = 0f
                        nextGlitchTime = time + Random.nextFloat() * 2f + 2f // 2-4 segundos
                    }
                    
                    delay(16)
                }
            }
        }
        
        this.graphicsLayer {
            try {
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("intensity", intensity)
                shader.setFloatUniform("glitchType", glitchType)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            } catch (e: Exception) {
                // Fail gracefully if shader uniforms fail
                error = true
            }
        }
    } catch (e: Exception) {
        // Fallback if shader compilation fails
        error = true
        this
    }
}

/**
 * MODIFIER CYBER GRID: Grid infinito como fondo
 */
@Composable
fun Modifier.cyberGrid(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }
    
    var error by remember { mutableStateOf(false) }
    if (error) return this

    return try {
        val shader = remember { RuntimeShader(cyberGridShader) }
        var time by remember { mutableFloatStateOf(0f) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    time += 0.016f
                    delay(16)
                }
            }
        }
        
        this.graphicsLayer {
            try {
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            } catch (e: Exception) {
                error = true
            }
        }
    } catch (e: Exception) {
        error = true
        this
    }
}

@Composable
fun Modifier.aggressiveScanLines(): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    
    var error by remember { mutableStateOf(false) }
    if (error) return this

    return try {
        val shader = remember { RuntimeShader(aggressiveScanLineShader) }
        var time by remember { mutableFloatStateOf(0f) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    time += 0.016f
                    delay(16)
                }
            }
        }
        
        this.graphicsLayer {
            try {
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            } catch (e: Exception) {
                error = true
            }
        }
    } catch (e: Exception) {
        error = true
        this
    }
}
