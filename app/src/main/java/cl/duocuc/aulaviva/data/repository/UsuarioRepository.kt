package cl.duocuc.aulaviva.data.repository

import android.util.Log
import cl.duocuc.aulaviva.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Repository para manejar la tabla `usuarios` en Supabase.
 * Gestiona datos adicionales del usuario (rol, nombre, etc.)
 */
class UsuarioRepository {

    /**
     * Guardar datos del usuario en tabla usuarios.
     */
    suspend fun guardarUsuario(
        uid: String,
        email: String,
        rol: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("UsuarioRepo", "💾 Guardando usuario: $email con rol: $rol")

            val usuarioDTO = UsuarioDTO(
                id = uid,
                email = email,
                rol = rol,
                nombre = email.substringBefore("@") // Nombre por defecto desde email
            )

            supabase.from("usuarios").insert(usuarioDTO)

            Log.d("UsuarioRepo", "✅ Usuario guardado exitosamente")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("UsuarioRepo", "❌ Error guardando usuario", e)
            Result.failure(Exception("Error al guardar datos del usuario: ${e.message}"))
        }
    }

    /**
     * Obtener datos del usuario desde tabla usuarios.
     */
    suspend fun obtenerUsuario(uid: String): Result<UsuarioDTO> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("UsuarioRepo", "🔍 Buscando usuario: ${uid.take(8)}...")

            val response = supabase.from("usuarios")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingle<UsuarioDTO>()

            Log.d("UsuarioRepo", "✅ Usuario encontrado: ${response.email}, rol: ${response.rol}")
            Result.success(response)

        } catch (e: Exception) {
            Log.e("UsuarioRepo", "❌ Error obteniendo usuario", e)
            Result.failure(Exception("Usuario no encontrado: ${e.message}"))
        }
    }

    /**
     * Actualizar rol del usuario.
     */
    suspend fun actualizarRol(uid: String, nuevoRol: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val supabase = SupabaseClientProvider.getClient()

                Log.d("UsuarioRepo", "✏️ Actualizando rol a: $nuevoRol")

                supabase.from("usuarios")
                    .update(mapOf("rol" to nuevoRol)) {
                        filter {
                            eq("id", uid)
                        }
                    }

                Log.d("UsuarioRepo", "✅ Rol actualizado")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e("UsuarioRepo", "❌ Error actualizando rol", e)
                Result.failure(Exception("Error al actualizar rol: ${e.message}"))
            }
        }
}

/**
 * DTO para serialización de usuarios en Supabase.
 */
@Serializable
data class UsuarioDTO(
    val id: String,
    val email: String,
    val rol: String,
    val nombre: String = ""
)
