package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IAuthRepository

import android.util.Log
import cl.duocuc.aulaviva.data.remote.JwtDecoder
import cl.duocuc.aulaviva.data.remote.SpringBootAuthRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository que maneja toda la lógica de autenticación.
 * Utiliza Spring Boot backend para autenticación.
 *
 * Migrado de Supabase directo a Spring Boot.
 */
class AuthRepository : IAuthRepository {

    private val springBootAuth = SpringBootAuthRepository(SpringBootClient.apiService)
    private val usuarioRepository = UsuarioRepository()

    /**
     * Obtener UID del usuario actual.
     */
    override fun getCurrentUserId(): String? = springBootAuth.getCurrentUserId()

    /**
     * Obtener email del usuario actual.
     */
    override fun getCurrentUserEmail(): String? = springBootAuth.getCurrentUserEmail()

    /**
     * Verificar si hay sesión activa.
     */
    override fun isLoggedIn(): Boolean = springBootAuth.isLoggedIn()

    /**
     * Login con email y password.
     */
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = springBootAuth.login(email, password)
            result.fold(
                onSuccess = { userId ->
                    // Actualizar cache del usuario
                    springBootAuth.setCurrentUser(userId, email)
                    Result.success(userId)
                },
                onFailure = { e ->
                    Log.e("AuthRepo", "❌ Error en login", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e("AuthRepo", "❌ Excepción en login suspend", e)
            Result.failure(Exception(e.message ?: "Error desconocido al iniciar sesión"))
        }
    }

    /**
     * Registro de nuevo usuario.
     * Spring Boot maneja todo el registro (Auth + tabla usuarios).
     */
    override suspend fun register(email: String, password: String, rol: String): Result<Unit> {
        return try {
            val result = springBootAuth.register(email, password, rol)
            result.fold(
                onSuccess = { userId ->
                    // Actualizar cache del usuario
                    springBootAuth.setCurrentUser(userId, email)
                    Log.d("AuthRepo", "✅ Usuario registrado completamente")
                    Result.success(Unit)
                },
                onFailure = { e ->
                    Log.e("AuthRepo", "❌ Error en registro: ${e.message}", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e("AuthRepo", "❌ Excepción en register suspend", e)
            Result.failure(Exception(e.message ?: "Error desconocido al registrar usuario"))
        }
    }

    /**
     * Obtener rol del usuario actual.
     * PRIORIDAD: 1) JWT decode (sin red), 2) API call (con red)
     * Esto evita que alumno vaya a panel docente si la API falla.
     */
    override suspend fun obtenerRolUsuario(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // PRIMERO: Intentar obtener rol directamente del JWT (sin llamada de red)
            val token = TokenManager.getToken()
            if (token != null) {
                val rolFromToken = JwtDecoder.getRolFromToken(token)
                if (!rolFromToken.isNullOrBlank()) {
                    android.util.Log.d("AuthRepo", "✅ Rol obtenido del JWT: $rolFromToken")
                    return@withContext Result.success(rolFromToken)
                }
            }
            
            // FALLBACK: Si no se puede obtener del JWT, llamar a la API
            android.util.Log.d("AuthRepo", "⚠️ Rol no encontrado en JWT, consultando API...")
            val result = springBootAuth.getCurrentUser()
            result.fold(
                onSuccess = { usuario ->
                    Result.success(usuario.rol)
                },
                onFailure = { error ->
                    android.util.Log.e("AuthRepo", "❌ Error obteniendo rol de API: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "❌ Excepción obteniendo rol", e)
            Result.failure(Exception("Error obteniendo rol: ${e.message}"))
        }
    }

    /**
     * Cerrar sesión.
     */
    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            springBootAuth.logout()
        }
    }
}
