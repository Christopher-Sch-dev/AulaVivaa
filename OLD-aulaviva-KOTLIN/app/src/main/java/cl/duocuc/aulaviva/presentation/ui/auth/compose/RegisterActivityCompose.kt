package cl.duocuc.aulaviva.presentation.ui.auth.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme

/**
 * RegisterActivity migrada a Jetpack Compose
 *
 * Versión Compose de RegisterActivity con Material Design 3 completo.
 * Mantiene toda la funcionalidad original.
 */
class RegisterActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            // Cerrar la actividad después de un breve delay
                            finish()
                        },
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

