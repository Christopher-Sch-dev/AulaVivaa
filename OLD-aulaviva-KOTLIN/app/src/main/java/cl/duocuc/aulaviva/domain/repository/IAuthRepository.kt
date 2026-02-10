package cl.duocuc.aulaviva.domain.repository

import kotlin.Result

interface IAuthRepository {
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun isLoggedIn(): Boolean
    suspend fun login(email: String, password: String): Result<String>
    suspend fun register(email: String, password: String, rol: String = "docente"): Result<Unit>
    suspend fun obtenerRolUsuario(): Result<String>
    suspend fun logout()
}
