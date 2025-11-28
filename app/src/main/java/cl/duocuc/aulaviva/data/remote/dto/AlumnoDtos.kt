package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InscribirConCodigoRequestDto(
    val codigo: String
)

@Serializable
data class InscripcionResponseDto(
    val success: Boolean,
    val message: String,
    val asignatura: AsignaturaResponseDto?
)

@Serializable
data class AlumnoAsignaturaResponseDto(
    val id: String,
    val alumnoId: String,
    val asignaturaId: String,
    val fechaInscripcion: String,
    val estado: String
)

