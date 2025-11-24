package cl.duocuc.aulaviva.presentation.ui.auth.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelAlumnoActivityCompose
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelPrincipalActivityCompose
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * LoginActivity migrada a Jetpack Compose
 *
 * Versión Compose de LoginActivity con Material Design 3 completo.
 * Mantiene toda la funcionalidad original.
 */
class LoginActivityCompose : ComponentActivity() {

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Verificar rol y redirigir
                            lifecycleScope.launch {
                                try {
                                    val resultRol = authRepository.obtenerRolUsuario()
                                    val rol = resultRol.getOrNull() ?: "docente"

                                    val intent = when (rol.lowercase()) {
                                        "docente" -> Intent(
                                            this@LoginActivityCompose,
                                            PanelPrincipalActivityCompose::class.java
                                        )
                                "alumno" -> Intent(
                                    this@LoginActivityCompose,
                                    PanelAlumnoActivityCompose::class.java
                                )
                                        else -> Intent(
                                            this@LoginActivityCompose,
                                            PanelPrincipalActivityCompose::class.java
                                        )
                                    }

                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    // Default a docente si falla
                                    val intent = Intent(
                                        this@LoginActivityCompose,
                                        PanelPrincipalActivityCompose::class.java
                                    )
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            }
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

