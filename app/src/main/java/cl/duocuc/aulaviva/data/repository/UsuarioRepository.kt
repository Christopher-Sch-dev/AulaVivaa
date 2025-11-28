package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IUsuarioRepository

import android.util.Log
import cl.duocuc.aulaviva.data.remote.SpringBootAuthRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * Repository para manejar la tabla `usuarios` usando Spring Boot.
 * Migrado de Supabase directo a Spring Boot backend.
 *
 * Nota: Spring Boot crea el usuario automáticamente en el registro,
 * pero mantenemos estos métodos para compatibilidad.
 */
class UsuarioRepository : IUsuarioRepository {

    private val springBootAuth = SpringBootAuthRepository(SpringBootClient.apiService)

    /**
     * Guardar datos del usuario en tabla usuarios.
     * Nota: Spring Boot crea el usuario automáticamente en el registro,
     * este método se mantiene para compatibilidad pero puede no ser necesario.
     */
    override suspend fun guardarUsuario(
        uid: String,
        email: String,
        rol: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Spring Boot crea el usuario automáticamente en el registro
            // Este método se mantiene para compatibilidad pero ya no es necesario
            Log.d("UsuarioRepo", "💾 Usuario ya creado por Spring Boot en registro")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UsuarioRepo", "❌ Error guardando usuario", e)
            Result.failure(Exception("Error al guardar datos del usuario: ${e.message}"))
        }
    }

    /**
     * Obtener datos del usuario desde Spring Boot.
     */
    override suspend fun obtenerUsuario(uid: String): Result<UsuarioDTO> = withContext(Dispatchers.IO) {
        try {
            val result = springBootAuth.getCurrentUser()
            result.fold(
                onSuccess = { usuario ->
                    val usuarioDTO = UsuarioDTO(
                        id = usuario.id,
                        email = usuario.email,
                        rol = usuario.rol,
                        nombre = usuario.nombre ?: ""
                    )
                    Log.d("UsuarioRepo", "✅ Usuario encontrado: ${usuarioDTO.email}, rol: ${usuarioDTO.rol}")
                    Result.success(usuarioDTO)
                },
                onFailure = { error ->
                    Log.e("UsuarioRepo", "❌ Error obteniendo usuario", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("UsuarioRepo", "❌ Excepción obteniendo usuario", e)
            Result.failure(Exception("Usuario no encontrado: ${e.message}"))
        }
    }

    /**
     * Actualizar rol del usuario.
     * Nota: Spring Boot no tiene endpoint específico para actualizar rol,
     * este método se mantiene para compatibilidad.
     */
    override suspend fun actualizarRol(uid: String, nuevoRol: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("UsuarioRepo", "✏️ Actualización de rol no soportada en Spring Boot actualmente")
                // TODO: Implementar endpoint en Spring Boot si es necesario
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("UsuarioRepo", "❌ Error actualizando rol", e)
                Result.failure(Exception("Error al actualizar rol: ${e.message}"))
            }
        }
}

/**
 * DTO para serialización de usuarios (compatibilidad).
 */
data class UsuarioDTO(
    val id: String,
    val email: String,
    val rol: String,
    val nombre: String = ""
)
