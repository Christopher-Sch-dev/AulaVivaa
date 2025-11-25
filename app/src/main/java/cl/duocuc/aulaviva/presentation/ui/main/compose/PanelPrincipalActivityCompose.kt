package cl.duocuc.aulaviva.presentation.ui.main.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.presentation.ui.auth.compose.LoginActivityCompose
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity del panel principal (docente).
 *
 * Verifica la autenticación antes de mostrar la pantalla.
 * Si el usuario no está autenticado, redirige al login.
 */
class PanelPrincipalActivityCompose : ComponentActivity() {

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar autenticación antes de mostrar la pantalla
        lifecycleScope.launch {
            // Pequeño delay para asegurar que la sesión esté completamente establecida
            delay(100)

            if (!authRepository.isLoggedIn()) {
                // Usuario no autenticado: redirigir al login
                android.util.Log.w("PanelPrincipal", "⚠️ Usuario no autenticado, redirigiendo al login")
                val intent = Intent(this@PanelPrincipalActivityCompose, LoginActivityCompose::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return@launch
            }

            // Usuario autenticado: mostrar la pantalla
            setContent {
                AulaVivaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PanelPrincipalScreen()
                    }
                }
            }
        }
    }
}

