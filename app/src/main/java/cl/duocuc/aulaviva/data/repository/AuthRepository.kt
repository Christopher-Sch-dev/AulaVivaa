package cl.duocuc.aulaviva.data.repository

import android.util.Log
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository que maneja toda la lógica de autenticación.
 * AHORA USA SUPABASE 100% (NO Firebase).
 *
 * Adaptador entre Activities y SupabaseAuthManager.
 */
class AuthRepository {

    private val usuarioRepository = UsuarioRepository()

    /**
     * Obtener UID del usuario actual.
     */
    fun getCurrentUserId(): String? = SupabaseAuthManager.getCurrentUserId()

    /**
     * Obtener email del usuario actual.
     */
    fun getCurrentUserEmail(): String? = SupabaseAuthManager.getCurrentUserEmail()

    /**
     * Verificar si hay sesión activa.
     */
    fun isLoggedIn(): Boolean = SupabaseAuthManager.isLoggedIn()

    /**
     * Login con email y password.
     * Usa callbacks para mantener compatibilidad con Activities existentes.
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            // Timeout de seguridad de 15 segundos
            var completed = false
            val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!completed) {
                    completed = true
                    onError("La operación tardó demasiado. Verifica tu conexión a internet.")
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 15000)

            try {
                val result = withContext(Dispatchers.IO) {
                    SupabaseAuthManager.login(email, password)
                }

                if (!completed) {
                    completed = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    result.fold(
                        onSuccess = {
                            onSuccess()
                        },
                        onFailure = { e ->
                            onError(e.message ?: "Error desconocido al iniciar sesión")
                        }
                    )
                }
            } catch (e: Exception) {
                if (!completed) {
                    completed = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    /**
     * Registro de nuevo usuario.
     * Guarda el usuario en Auth y sus datos en tabla usuarios.
     */
    fun register(
        email: String,
        password: String,
        rol: String = "docente",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            // Timeout de seguridad de 15 segundos
            var completed = false
            val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!completed) {
                    completed = true
                    onError("La operación tardó demasiado. Verifica tu conexión a internet.")
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 15000)

            try {
                // Paso 1: Registrar en Supabase Auth
                val resultAuth = withContext(Dispatchers.IO) {
                    SupabaseAuthManager.register(email, password)
                }

                if (!completed) {
                    resultAuth.fold(
                        onSuccess = { uid ->
                            // Paso 2: Guardar datos adicionales en tabla usuarios
                            CoroutineScope(Dispatchers.IO).launch {
                                val resultUsuario = usuarioRepository.guardarUsuario(
                                    uid = uid,
                                    email = email,
                                    rol = rol
                                )

                                withContext(Dispatchers.Main) {
                                    if (!completed) {
                                        completed = true
                                        timeoutHandler.removeCallbacks(timeoutRunnable)

                                        resultUsuario.fold(
                                            onSuccess = {
                                                Log.d(
                                                    "AuthRepo",
                                                    "✅ Usuario registrado completamente"
                                                )
                                                onSuccess()
                                            },
                                            onFailure = { e ->
                                                Log.e(
                                                    "AuthRepo",
                                                    "❌ Error guardando datos usuario",
                                                    e
                                                )
                                                onError("Usuario creado pero error al guardar datos: ${e.message}")
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        onFailure = { e ->
                            completed = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            onError(e.message ?: "Error desconocido al registrar usuario")
                        }
                    )
                }
            } catch (e: Exception) {
                if (!completed) {
                    completed = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    /**
     * Obtener rol del usuario actual.
     */
    suspend fun obtenerRolUsuario(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uid = SupabaseAuthManager.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

            val result = usuarioRepository.obtenerUsuario(uid)
            result.fold(
                onSuccess = { usuario ->
                    Result.success(usuario.rol)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception("Error obteniendo rol: ${e.message}"))
        }
    }

    /**
     * Cerrar sesión.
     */
    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseAuthManager.logout()
        }
    }
}
