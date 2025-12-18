package cl.duocuc.aulaviva.presentation.ui.effects

import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Shader que se PAUSA cuando la app está en background
 * CRÍTICO para ahorro de batería y performance
 * Fuente: https://developer.android.com/jetpack/compose/side-effects
 */
@Composable
fun Modifier.lifecycleAwareShader(
    shaderModifier: @Composable Modifier.() -> Modifier
): Modifier {
    // Si es versión antigua, devolvemos sin efecto o manejamos fallback.
    // Asumimos que shaderModifier maneja sus propios checks de versión si es necesario,
    // pero el control de ciclo de vida es general.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isActive by remember { mutableStateOf(true) }
    
    // OPTIMIZACIÓN: Detecta cuando app va a background
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isActive = true
            // Mantenemos isActive en true mientras estemos RESUMED
            try {
                awaitCancellation()
            } finally {
                isActive = false
            }
        }
        isActive = false // Pausa cuando sale de RESUMED
    }
    
    return if (isActive) {
        this.shaderModifier() // Solo aplica shader si está activa
    } else {
        this // Sin shader en background
    }
}

/**
 * DisposableEffect para limpiar recursos
 */
@Composable
fun AutoCleanupEffect(
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    DisposableEffect(Unit) {
        onStart()
        onDispose {
            onStop() // CRÍTICO: Limpia listeners, jobs, etc.
        }
    }
}
