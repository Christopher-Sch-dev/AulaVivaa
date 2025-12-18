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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelAlumnoActivityCompose
import cl.duocuc.aulaviva.presentation.ui.main.compose.PanelPrincipalActivityCompose
import cl.duocuc.aulaviva.presentation.ui.theme.AulaVivaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity de bienvenida de la aplicación AulaViva.
 *
 * Implementa persistencia de sesión:
 * - Verifica si existe una sesión activa al iniciar la actividad
 * - Si hay sesión activa, redirige automáticamente al panel correspondiente según el rol del usuario
 * - Previene navegación hacia atrás si el usuario está autenticado
 * 
 * SplashScreen API:
 * - Muestra splash nativo durante cold start
 * - Mantiene splash mientras verifica sesión (sin esperar red)
 */
class WelcomeActivityCompose : ComponentActivity() {

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()
    
    // Variable para controlar cuándo liberar el splash screen
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ CRÍTICO: installSplashScreen() DEBE llamarse ANTES de super.onCreate()
        val splashScreen = installSplashScreen()
        
        // Mantener el splash visible mientras verificamos la sesión (sin esperar red)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)

        // Verificar si existe una sesión persistente al iniciar la actividad
        // Si el usuario ya está autenticado, redirigir automáticamente al panel correspondiente
        lifecycleScope.launch {
            // Esperar un poco más para dar tiempo a que Supabase restaure la sesión desde el almacenamiento
            delay(300)

            // Verificar sesión múltiples veces para asegurar que se restaure correctamente
            var isAuthenticated = false
            var attempts = 0
            val maxAttempts = 3

            while (attempts < maxAttempts && !isAuthenticated) {
                isAuthenticated = authRepository.isLoggedIn()
                if (!isAuthenticated) {
                    attempts++
                    if (attempts < maxAttempts) {
                        delay(200) // Esperar un poco más antes de reintentar
                    }
                }
            }

            if (isAuthenticated) {
                try {
                    android.util.Log.d("WelcomeActivity", "✅ Sesión encontrada, obteniendo rol del usuario...")

                    // Obtener el rol del usuario desde el repositorio de autenticación
                    val resultRol = authRepository.obtenerRolUsuario()
                    val rol = resultRol.getOrNull() ?: "docente"

                    android.util.Log.d("WelcomeActivity", "✅ Rol obtenido: $rol, redirigiendo al panel correspondiente")

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
                    keepSplashScreen = false // Liberar splash antes de navegar
                    startActivity(intent)
                    finish()
                    return@launch
                } catch (e: Exception) {
                    // Si falla la verificación, continuar mostrando la pantalla de bienvenida
                    android.util.Log.e("WelcomeActivity", "❌ Error verificando sesión o obteniendo rol", e)
                    keepSplashScreen = false // Liberar splash para mostrar welcome
                }
            } else {
                android.util.Log.d("WelcomeActivity", "⚠️ No hay sesión activa, mostrando pantalla de bienvenida")
                keepSplashScreen = false // Liberar splash para mostrar welcome
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

