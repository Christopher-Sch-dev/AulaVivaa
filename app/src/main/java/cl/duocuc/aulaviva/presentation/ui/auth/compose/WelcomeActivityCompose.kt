package cl.duocuc.aulaviva.presentation.ui.auth.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.presentation.ui.common.AulaVivaBootScreen
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
    
    // Estado reactivo para controlar splash y mostrar BootScreen con efectos cyberpunk
    // Usa mutableStateOf para que Compose recomponga la UI cuando cambie
    // Estado para controlar la animación de carga personalizada (Boot Screen)
    private var showBootAnimation by mutableStateOf(true)
    
    // Estado para resultados de validación básica
    private var sessionCheckComplete by mutableStateOf(false)
    private var targetIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Nativo: Lo mostramos solo lo mínimo necesario para inicializar
        val splashScreen = installSplashScreen()
        // Inmediatamente permitimos que se oculte para mostrar nuestra animación personalizada
        splashScreen.setKeepOnScreenCondition { false } 
        
        super.onCreate(savedInstanceState)

        // Iniciamos verificación de sesión en paralelo a la animación
        lifecycleScope.launch {
            // No delay artificial aquí, dejemos que la animación mande
            
            var isAuthenticated = false
            try {
                // Check rápido inicial
                isAuthenticated = authRepository.isLoggedIn()
                
                // Si falla o false, reintentar brevemente por si Supabase está despertando
                if (!isAuthenticated) {
                    delay(200)
                    isAuthenticated = authRepository.isLoggedIn()
                }

                if (isAuthenticated) {
                    val resultRol = authRepository.obtenerRolUsuario()
                    val rol = resultRol.getOrNull() ?: "docente"
                    
                    // Preparamos el intent pero NO lo lanzamos aún
                    targetIntent = when (rol.lowercase()) {
                        "docente" -> Intent(this@WelcomeActivityCompose, PanelPrincipalActivityCompose::class.java)
                        "alumno" -> Intent(this@WelcomeActivityCompose, PanelAlumnoActivityCompose::class.java)
                        else -> Intent(this@WelcomeActivityCompose, PanelPrincipalActivityCompose::class.java)
                    }.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WelcomeActivity", "Error check session", e)
            } finally {
                sessionCheckComplete = true
                checkNavigation()
            }
        }

        setContent {
            AulaVivaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Control de flujo visual
                    if (showBootAnimation) {
                        AulaVivaBootScreen(
                            onLoadComplete = {
                                // La animación terminó.
                                // Intentamos navegar si la sesión ya se verificó.
                                // Si la sesión aún verifica, la navegación se disparará en checkNavigation()
                                showBootAnimation = false
                                checkNavigation()
                            }
                        )
                    } else {
                        // Si no hay animación, mostramos WelcomeScreen
                        // (Si hubiera sesión, ya habríamos navegado en checkNavigation)
                        WelcomeScreen()
                    }
                }
            }
        }
    }

    private fun checkNavigation() {
        // Solo navegamos si la animación terminó Y la verificación de sesión terminó
        if (!showBootAnimation && sessionCheckComplete) {
            targetIntent?.let { intent ->
                startActivity(intent)
                finish()
            }
            // Si targetIntent es null, significa que no hay sesión -> Se queda en WelcomeScreen
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

