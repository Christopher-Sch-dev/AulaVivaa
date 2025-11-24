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
 * WelcomeActivity migrada a Jetpack Compose
 *
 * Esta es la versión Compose de WelcomeActivity.
 * Mantiene la misma funcionalidad pero usando Material Design 3 completo.
 */
class WelcomeActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomeScreen()
                }
            }
        }
    }
}

