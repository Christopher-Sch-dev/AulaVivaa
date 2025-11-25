package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IAuthRepository

import android.util.Log
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository que maneja toda la lógica de autenticación.
 * Utiliza Supabase Auth y gestión de usuarios.
 *
 * Adaptador entre Activities y SupabaseAuthManager.
 */
class AuthRepository : IAuthRepository {

    private val usuarioRepository = UsuarioRepository()

    /**
     * Obtener UID del usuario actual.
     */
    override fun getCurrentUserId(): String? = SupabaseAuthManager.getCurrentUserId()

    /**
     * Obtener email del usuario actual.
     */
    override fun getCurrentUserEmail(): String? = SupabaseAuthManager.getCurrentUserEmail()

    /**
     * Verificar si hay sesión activa.
     */
    override fun isLoggedIn(): Boolean = SupabaseAuthManager.isLoggedIn()

    /**
     * Login con email y password.
     * Usa callbacks para mantener compatibilidad con Activities existentes.
     */
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                SupabaseAuthManager.login(email, password)
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "❌ Excepción en login suspend", e)
            Result.failure(Exception(e.message ?: "Error desconocido al iniciar sesión"))
        }
    }

    /**
     * Registro de nuevo usuario.
     * Guarda el usuario en Auth y sus datos en tabla usuarios.
     */
    override suspend fun register(email: String, password: String, rol: String): Result<Unit> {
        return try {
            val resultAuth = withContext(Dispatchers.IO) {
                SupabaseAuthManager.register(email, password)
            }

            resultAuth.fold(
                onSuccess = { uid ->
                    if (uid == "pending_confirmation") {
                        Log.d("AuthRepo", "⚠️ Usuario creado - Requiere confirmación de email")
                        return Result.failure(Exception("pending_confirmation"))
                    }

                    // Guardar datos adicionales en tabla usuarios
                    val resultUsuario = usuarioRepository.guardarUsuario(uid = uid, email = email, rol = rol)
                    resultUsuario.fold(
                        onSuccess = {
                            Log.d("AuthRepo", "✅ Usuario registrado completamente")
                            Result.success(Unit)
                        },
                        onFailure = { e ->
                            Log.w("AuthRepo", "⚠️ Usuario creado en Auth pero falló guardar datos adicionales", e)
                            // Devolver éxito parcial (usuario existe en Auth)
                            Result.success(Unit)
                        }
                    )
                },
                onFailure = { e ->
                    Log.e("AuthRepo", "❌ Error en registro Auth: ${e.message}", e)
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
     */
    override suspend fun obtenerRolUsuario(): Result<String> = withContext(Dispatchers.IO) {
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
    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            SupabaseAuthManager.logout()
        }
    }
}
