package cl.duocuc.aulaviva.backend.application.dto.response

import java.util.*

data class AuthResponse(
    val token: String,
    val userId: UUID,
    val email: String,
    val rol: String
)

data class UsuarioResponse(
    val id: UUID,
    val email: String,
    val rol: String,
    val nombre: String?
)

