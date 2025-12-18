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
import androidx.activity.addCallback

/**
 * Activity de inicio de sesión de la aplicación AulaViva.
 *
 * Implementada con Jetpack Compose y Material Design 3.
 * Maneja la autenticación de usuarios y redirige según su rol (docente/alumno).
 * También implementa persistencia de sesión para evitar mostrar login si ya hay sesión activa.
 */
class LoginActivityCompose : ComponentActivity() {

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si existe una sesión persistente al iniciar la actividad
        // Si el usuario ya está autenticado, redirigir automáticamente sin mostrar el login
        lifecycleScope.launch {
            if (authRepository.isLoggedIn()) {
                try {
                    // Obtener el rol del usuario desde el repositorio de autenticación
                    val resultRol = authRepository.obtenerRolUsuario()
                    val rol = resultRol.getOrNull() ?: "docente"

                    // Crear Intent según el rol del usuario (docente o alumno)
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

                    // Limpiar el stack de actividades y navegar al panel correspondiente
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@launch
                } catch (e: Exception) {
                    // Si falla la verificación, continuar mostrando la pantalla de login
                    android.util.Log.e("LoginActivity", "Error verificando sesión", e)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (authRepository.isLoggedIn()) {
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
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Callback ejecutado cuando el login es exitoso
                            // Verificar el rol del usuario y redirigir al panel correspondiente
                            lifecycleScope.launch {
                                try {
                                    // Obtener el rol del usuario desde el repositorio
                                    val resultRol = authRepository.obtenerRolUsuario()
                                    val rol = resultRol.getOrNull() ?: "docente"

                                    // Crear Intent según el rol del usuario
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

                                    // Limpiar el stack de actividades y navegar al panel
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    // Si falla la obtención del rol, redirigir al panel docente por defecto
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
                        },
                        onNavigateToRegister = {
                            // Navegar a la pantalla de registro
                            val intent = Intent(this@LoginActivityCompose, RegisterActivityCompose::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }


}

