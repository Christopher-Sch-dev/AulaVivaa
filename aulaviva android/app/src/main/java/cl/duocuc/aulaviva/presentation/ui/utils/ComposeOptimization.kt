package cl.duocuc.aulaviva.presentation.ui.utils

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * REGLA 1: Usar @Stable para data classes que se pasan a composables
 * Fuente: https://developer.android.com/jetpack/compose/performance/stability
 */
@Stable
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * REGLA 2: Lambdas deben ser remembered si contienen estado
 * Fuente: https://www.droidcon.com/2025/01/10/best-practices-for-composition-patterns-in-jetpack-compose/
 */
@Composable
fun OptimizedButton(
    text: String,
    onClick: () -> Unit, // Pasar directamente desde ViewModel
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // BIEN: Lambda viene desde fuera, ya optimizada
    Button(
        onClick = onClick, 
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text)
    }
}
