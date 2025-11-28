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
     * este método es un no-op para mantener compatibilidad con la interfaz.
     */
    override suspend fun guardarUsuario(
        uid: String,
        email: String,
        rol: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Spring Boot crea el usuario automáticamente en el registro
        // Este método es un no-op para mantener compatibilidad con la interfaz
        Log.d("UsuarioRepo", "💾 Usuario ya creado por Spring Boot en registro (no-op)")
        Result.success(Unit)
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
     * Nota: Spring Boot no tiene endpoint específico para actualizar rol actualmente.
     * Este método es un no-op para mantener compatibilidad con la interfaz.
     *
     * Para implementar en el futuro:
     * - Agregar endpoint PUT /api/usuarios/{id}/rol en Spring Boot
     * - Actualizar este método para llamar al endpoint
     */
    override suspend fun actualizarRol(uid: String, nuevoRol: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            Log.d("UsuarioRepo", "✏️ Actualización de rol no soportada en Spring Boot (no-op)")
            // TODO: Implementar endpoint en Spring Boot cuando sea necesario
            Result.success(Unit)
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
