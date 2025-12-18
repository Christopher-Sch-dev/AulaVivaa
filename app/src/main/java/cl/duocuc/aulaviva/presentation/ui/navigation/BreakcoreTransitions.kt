package cl.duocuc.aulaviva.presentation.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset

/**
 * Transiciones breakcore (inspiradas en tu web)
 * - Glitch al entrar
 * - Slide + fade agresivo
 */
@OptIn(ExperimentalAnimationApi::class)
fun breakcoreEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun breakcoreExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(300)
    ) + fadeOut(
        animationSpec = tween(300)
    )
}

/**
 * Transición "glitch wipe" (pantalla se rompe al cambiar)
 */
@OptIn(ExperimentalAnimationApi::class)
fun glitchWipeTransition(): EnterTransition {
    return fadeIn(
        animationSpec = keyframes {
            durationMillis = 400
            0f at 0
            0.3f at 100 // Parpadeo inicial
            0f at 150
            1f at 400
        }
    ) + slideInVertically(
        initialOffsetY = { it / 20 }, // Shake vertical
        animationSpec = tween(400, easing = LinearOutSlowInEasing)
    )
}
