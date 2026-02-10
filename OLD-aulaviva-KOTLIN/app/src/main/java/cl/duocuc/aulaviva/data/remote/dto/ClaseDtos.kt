package cl.duocuc.aulaviva.data.remote.dto

data class CrearClaseRequestDto(
    val nombre: String,
    val descripcion: String = "",
    val fecha: String,
    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = "",
    val asignaturaId: String? = null
)

data class ActualizarClaseRequestDto(
    val nombre: String,
    val descripcion: String = "",
    val fecha: String,
    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = ""
)

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

