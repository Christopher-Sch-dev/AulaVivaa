package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CrearClaseRequestDto(
    val nombre: String,
    val descripcion: String = "",
    val fecha: String,
    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = "",
    val asignaturaId: String? = null
)

@Serializable
data class ActualizarClaseRequestDto(
    val nombre: String,
    val descripcion: String = "",
    val fecha: String,
    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = ""
)

@Serializable
data class ClaseResponseDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val archivoPdfUrl: String,
    val archivoPdfNombre: String,
    val creador: String,
    val asignaturaId: String?,
    val createdAt: String,
    val updatedAt: String
)

