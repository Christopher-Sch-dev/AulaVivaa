package cl.duocuc.aulaviva.presentation.ui.auth.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelAlumnoActivityCompose
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelPrincipalActivityCompose
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import kotlinx.coroutines.launch

/**
 * Activity de bienvenida de la aplicación AulaViva.
 *
 * Implementa persistencia de sesión:
 * - Verifica si existe una sesión activa al iniciar la actividad
 * - Si hay sesión activa, redirige automáticamente al panel correspondiente según el rol del usuario
 * - Previene navegación hacia atrás si el usuario está autenticado
 */
class WelcomeActivityCompose : ComponentActivity() {

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si existe una sesión persistente al iniciar la actividad
        // Si el usuario ya está autenticado, redirigir automáticamente al panel correspondiente
        lifecycleScope.launch {
            if (authRepository.isLoggedIn()) {
                try {
                    // Obtener el rol del usuario desde el repositorio de autenticación
                    val resultRol = authRepository.obtenerRolUsuario()
                    val rol = resultRol.getOrNull() ?: "docente"

                    // Crear Intent según el rol del usuario (docente o alumno)
                    val intent = when (rol.lowercase()) {
                        "docente" -> Intent(
                            this@WelcomeActivityCompose,
                            PanelPrincipalActivityCompose::class.java
                        )
                        "alumno" -> Intent(
                            this@WelcomeActivityCompose,
                            PanelAlumnoActivityCompose::class.java
                        )
                        else -> Intent(
                            this@WelcomeActivityCompose,
                            PanelPrincipalActivityCompose::class.java
                        )
                    }

                    // Limpiar el stack de actividades y navegar al panel correspondiente
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@launch
                } catch (e: Exception) {
                    // Si falla la verificación, continuar mostrando la pantalla de bienvenida
                    android.util.Log.e("WelcomeActivity", "Error verificando sesión", e)
                }
            }
        }

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

    /**
     * Previene la navegación hacia atrás si el usuario está autenticado.
     * Si el usuario intenta retroceder estando logueado, se mantiene en la pantalla de bienvenida.
     * Para salir, el usuario debe cerrar sesión explícitamente.
     */
    override fun onBackPressed() {
        if (authRepository.isLoggedIn()) {
            // Usuario autenticado: no permitir retroceder, debe cerrar sesión primero
            return
        }
        super.onBackPressed()
    }
}

