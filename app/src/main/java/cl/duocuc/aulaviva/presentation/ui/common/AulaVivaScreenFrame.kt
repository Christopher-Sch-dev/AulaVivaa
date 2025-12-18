package cl.duocuc.aulaviva.presentation.ui.common

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.effects.CyberParticleBackground
import cl.duocuc.aulaviva.presentation.ui.effects.aggressiveScanLines
import cl.duocuc.aulaviva.presentation.ui.effects.cyberGrid
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaColors

/**
 * Modos de efectos visuales por tipo de pantalla.
 * 
 * Cada modo define qué efectos se aplican para mantener consistencia visual
 * y optimizar performance según el uso típico de cada tipo de pantalla.
 * 
 * REGLA DE ORO: Los efectos NO afectan funcionalidad, solo estética.
 * Todos los efectos se degradan gracefully en Android < 13.
 */
enum class ScreenEffectMode {
    /**
     * AUTH: Login, Register, Welcome
     * Efectos: cyberGrid + scanLines + particles (30)
     * NO glitch: evita distracción en inputs de formulario
     */
    AUTH,
    
    /**
     * PANEL: PanelPrincipal, PanelAlumno
     * Efectos: cyberGrid + scanLines + particles (20) + glitch opcional
     * Experiencia visual completa para dashboard principal
     */
    PANEL,
    
    /**
     * LIST: Asignaturas, Clases, Inscritos (listas con scroll)
     * Efectos: SOLO scanLines
     * Performance primero: scroll pesado con LazyColumn
     */
    LIST,
    
    /**
     * DETAIL: DetalleClase, CrearEditarClase
     * Efectos: scanLines + fondo estático
     * La atención es el contenido, no los efectos
     */
    DETAIL,
    
    /**
     * IA: ResultadoIA, Chat
     * Efectos: scanLines leve
     * Sin Matrix hasta estabilizar - legibilidad de Markdown primero
     */
    IA,
    
    /**
     * SPLASH: Pantalla de inicio/carga
     * Efectos: Matrix + particles + scanLines
     * Experiencia visual impactante al iniciar la app
     */
    SPLASH
}

/**
 * AulaVivaScreenFrame: Wrapper común para todas las pantallas.
 * 
 * Aplica efectos visuales de forma CONSISTENTE según el modo seleccionado.
 * Este componente garantiza que todas las pantallas de la app tengan
 * la misma identidad visual Cyberpunk/Breakcore.
 * 
 * Los efectos que requieren Android 13+ (shaders AGSL) se desactivan
 * automáticamente en versiones anteriores sin crashear.
 * 
 * ARQUITECTURA:
 * - Este wrapper NO modifica la navegación ni el ViewModel
 * - Solo aplica efectos visuales de fondo
 * - El contenido de la pantalla se renderiza ENCIMA de los efectos
 * 
 * @param mode Modo de efectos a aplicar (determina qué efectos se renderizan)
 * @param showParticles Override para mostrar/ocultar partículas independiente del modo
 * @param content Contenido composable de la pantalla
 */
@Composable
fun AulaVivaScreenFrame(
    mode: ScreenEffectMode,
    showParticles: Boolean? = null, // null = usar default del modo
    content: @Composable () -> Unit
) {
    // Determinar qué efectos aplicar según el modo
    val useGrid = when (mode) {
        ScreenEffectMode.AUTH, ScreenEffectMode.PANEL, ScreenEffectMode.SPLASH -> true
        else -> false
    }
    
    val useScanLines = when (mode) {
        ScreenEffectMode.SPLASH -> false // Splash usa Matrix, no scanlines
        else -> true // Todas las demás pantallas usan scanlines
    }
    
    val useParticles = showParticles ?: when (mode) {
        ScreenEffectMode.AUTH -> true
        ScreenEffectMode.PANEL -> true
        ScreenEffectMode.SPLASH -> true
        else -> false
    }
    
    val particlesCount = when (mode) {
        ScreenEffectMode.AUTH -> 30
        ScreenEffectMode.PANEL -> 20
        ScreenEffectMode.SPLASH -> 40
        else -> 0
    }
    
    val useMatrix = when (mode) {
        ScreenEffectMode.SPLASH -> true
        else -> false
    }
    
    // Verificar soporte de shaders AGSL (Android 13+)
    val supportsShaders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    
    // Construir modifier base
    var baseModifier = Modifier
        .fillMaxSize()
        .background(AulaVivaColors.BackgroundDark)
    
    // Aplicar efectos condicionalmente usando composición de modifiers
    // Los modifiers de shader tienen sus propias verificaciones de versión internas
    // pero las agregamos aquí también por claridad y seguridad
    
    // Orden de capas (de fondo a frente):
    // 1. Background color (siempre)
    // 2. cyberGrid shader (si aplica)
    // 3. scanLines shader (si aplica)
    // 4. MatrixBackground (composable, si aplica)
    // 5. Particles (composable, si aplica)
    // 6. Content (siempre, encima de todo)
    
    Box(
        modifier = if (useGrid && useScanLines && supportsShaders) {
            baseModifier.cyberGrid().aggressiveScanLines()
        } else if (useGrid && supportsShaders) {
            baseModifier.cyberGrid()
        } else if (useScanLines && supportsShaders) {
            baseModifier.aggressiveScanLines()
        } else {
            baseModifier
        }
    ) {
        // Capa Matrix (solo para SPLASH mode)
        if (useMatrix) {
            MatrixBackground(modifier = Modifier.fillMaxSize())
        }
        
        // Capa de partículas (si aplica)
        if (useParticles && particlesCount > 0) {
            CyberParticleBackground(maxParticles = particlesCount)
        }
        
        // Contenido de la pantalla (siempre visible, encima de efectos)
        content()
    }
}
