package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val rol: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val userId: String,
    val email: String,
    val rol: String
)

@Serializable
data class UsuarioResponseDto(
    val id: String,
    val email: String,
    val rol: String,
    val nombre: String?
)

