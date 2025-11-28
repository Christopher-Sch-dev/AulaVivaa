package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CrearAsignaturaRequestDto(
    val nombre: String,
    val descripcion: String = ""
)

@Serializable
data class ActualizarAsignaturaRequestDto(
    val nombre: String,
    val descripcion: String = ""
)

@Serializable
data class AsignaturaResponseDto(
    val id: String,
    val nombre: String,
    val codigoAcceso: String,
    val docenteId: String,
    val descripcion: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class GenerarCodigoResponseDto(
    val codigo: String
)

