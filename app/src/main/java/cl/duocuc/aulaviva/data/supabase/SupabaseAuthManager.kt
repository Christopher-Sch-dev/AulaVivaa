package cl.duocuc.aulaviva.data.supabase

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de autenticación Supabase.
 * Maneja login, registro, logout y estado de sesión.
 *
 * Utiliza Supabase GoTrue para autenticación completa.
 */
object SupabaseAuthManager {

    /**
     * Iniciar sesión con email y contraseña.
     * @return Result con UID del usuario o error.
     */
    suspend fun login(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val supabase = SupabaseClientProvider.getClient()

                Log.d("SupabaseAuth", "🔐 Intentando login para: $email")

                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val user = supabase.auth.currentUserOrNull()
                    ?: return@withContext Result.failure(Exception("Error obteniendo usuario después del login"))

                Log.d("SupabaseAuth", "✅ Login exitoso: ${user.id}")
                Result.success(user.id)

            } catch (e: Exception) {
                Log.e("SupabaseAuth", "❌ Error en login", e)
                val errorMsg = when {
                    e.message?.contains("Invalid API key", ignoreCase = true) == true ->
                        "Error de configuración: La clave de API de Supabase no es válida. Contacta al administrador."

                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Credenciales incorrectas"

                    e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                        "Debes confirmar tu email primero"

                    e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains(
                                "Unable to resolve host",
                                ignoreCase = true
                            ) == true ->
                        "Sin conexión a internet"

                    else -> e.message ?: "Error desconocido al iniciar sesión"
                }
                Result.failure(Exception(errorMsg))
            }
        }

    /**
     * Registrar nuevo usuario con email y contraseña.
     * Confirma automáticamente el email para evitar bloqueos.
     * @return Result con UID del usuario o error.
     */
    suspend fun register(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val supabase = SupabaseClientProvider.getClient()

                Log.d("SupabaseAuth", "📝 Registrando usuario: $email")

                // Paso 1: Crear usuario en Auth
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Paso 2: Intentar obtener el usuario
                var user = supabase.auth.currentUserOrNull()

                if (user == null) {
                    // Si no está logueado, intentar obtener de la sesión
                    val session = supabase.auth.currentSessionOrNull()
                    user = session?.user
                }

                if (user != null) {
                    Log.d("SupabaseAuth", "✅ Registro exitoso: ${user.id}")
                    Result.success(user.id)
                } else {
                    // Último intento: hacer login inmediatamente después de crear
                    Log.d("SupabaseAuth", "⚠️ Usuario creado pero no logueado. Intentando login...")
                    try {
                        supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                        val loginUser = supabase.auth.currentUserOrNull()
                        if (loginUser != null) {
                            Log.d(
                                "SupabaseAuth",
                                "✅ Login exitoso después de registro: ${loginUser.id}"
                            )
                            Result.success(loginUser.id)
                        } else {
                            Log.w(
                                "SupabaseAuth",
                                "⚠️ Usuario creado pero requiere confirmación de email"
                            )
                            Result.success("pending_confirmation")
                        }
                    } catch (loginError: Exception) {
                        Log.w(
                            "SupabaseAuth",
                            "⚠️ Usuario creado pero requiere confirmación: ${loginError.message}"
                        )
                        Result.success("pending_confirmation")
                    }
                }

            } catch (e: Exception) {
                Log.e("SupabaseAuth", "❌ Error en registro", e)
                val errorMsg = when {
                    e.message?.contains("User already registered", ignoreCase = true) == true ||
                            e.message?.contains("already registered", ignoreCase = true) == true ->
                        "Este email ya está registrado"

                    e.message?.contains("Password should be", ignoreCase = true) == true ->
                        "La contraseña debe tener al menos 6 caracteres"

                    e.message?.contains("invalid email", ignoreCase = true) == true ->
                        "Email inválido"

                    e.message?.contains("Signups not allowed", ignoreCase = true) == true ->
                        "Registro deshabilitado. Contacta al administrador."

                    else -> e.message ?: "Error desconocido al registrar usuario"
                }
                Result.failure(Exception(errorMsg))
            }
        }

    /**
     * Cerrar sesión actual.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()
            supabase.auth.signOut()
            Log.d("SupabaseAuth", "✅ Sesión cerrada correctamente")
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "❌ Error cerrando sesión", e)
        }
    }

    /**
     * Verificar si hay una sesión activa.
     */
    fun isLoggedIn(): Boolean {
        return try {
            val supabase = SupabaseClientProvider.getClient()
            supabase.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error verificando sesión", e)
            false
        }
    }

    /**
     * Obtener UID del usuario actual.
     */
    fun getCurrentUserId(): String? {
        return try {
            val supabase = SupabaseClientProvider.getClient()
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error obteniendo UID", e)
            null
        }
    }

    /**
     * Obtener email del usuario actual.
     */
    fun getCurrentUserEmail(): String? {
        return try {
            val supabase = SupabaseClientProvider.getClient()
            supabase.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error obteniendo email", e)
            null
        }
    }
}
