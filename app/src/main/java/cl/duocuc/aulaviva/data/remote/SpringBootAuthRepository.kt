package cl.duocuc.aulaviva.data.remote

import android.util.Log
import cl.duocuc.aulaviva.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository remoto para autenticación usando Spring Boot.
 * Reemplaza a SupabaseAuthManager.
 */
class SpringBootAuthRepository(
    private val apiService: SpringBootApiService
) {

    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequestDto(email = email, password = password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!.data!!
                TokenManager.saveToken(authResponse.token)
                Log.d("SpringBootAuth", "✅ Login exitoso: ${authResponse.userId}")
                Result.success(authResponse.userId)
            } else {
                val error = response.body()?.error ?: response.message()
                Log.e("SpringBootAuth", "❌ Error en login: $error")
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAuth", "❌ Excepción en login", e)
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, rol: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequestDto(email = email, password = password, rol = rol)
            val response = apiService.register(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!.data!!
                TokenManager.saveToken(authResponse.token)
                Log.d("SpringBootAuth", "✅ Registro exitoso: ${authResponse.userId}")
                Result.success(authResponse.userId)
            } else {
                val error = response.body()?.error ?: response.message()
                Log.e("SpringBootAuth", "❌ Error en registro: $error")
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAuth", "❌ Excepción en registro", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<UsuarioResponseDto> = withContext(Dispatchers.IO) {
        try {
            val token = TokenManager.getToken()
                ?: return@withContext Result.failure(Exception("No hay token de autenticación"))

            val response = apiService.getCurrentUser("Bearer $token")

            if (response.isSuccessful && response.body()?.success == true) {
                val usuario = response.body()!!.data!!
                Result.success(usuario)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error obteniendo usuario"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAuth", "❌ Excepción obteniendo usuario", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        val token = TokenManager.getToken() ?: return null
        return JwtDecoder.getUserIdFromToken(token) ?: currentUserId
    }

    fun getCurrentUserEmail(): String? {
        val token = TokenManager.getToken() ?: return null
        return JwtDecoder.getEmailFromToken(token) ?: currentUserEmail
    }

    fun isLoggedIn(): Boolean {
        return TokenManager.hasToken()
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            TokenManager.clearToken()
            currentUserId = null
            currentUserEmail = null
            Log.d("SpringBootAuth", "✅ Sesión cerrada")
        }
    }

    // Cache local del usuario actual (fallback si el token no se puede decodificar)
    private var currentUserId: String? = null
    private var currentUserEmail: String? = null

    fun setCurrentUser(userId: String, email: String) {
        currentUserId = userId
        currentUserEmail = email
    }
}

