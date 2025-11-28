package cl.duocuc.aulaviva.data.remote.dto

data class CrearAsignaturaRequestDto(
    val nombre: String,
    val descripcion: String = ""
)

data class ActualizarAsignaturaRequestDto(
    val nombre: String,
    val descripcion: String = ""
)

data class AsignaturaResponseDto(
    val id: String,
    val nombre: String,
    val codigoAcceso: String,
    val docenteId: String,
    val descripcion: String,
    val createdAt: String,
    val updatedAt: String
)

data class GenerarCodigoResponseDto(
    val codigo: String
)

