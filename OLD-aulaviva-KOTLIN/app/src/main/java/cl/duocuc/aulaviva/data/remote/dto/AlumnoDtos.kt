package cl.duocuc.aulaviva.data.remote.dto

data class InscribirConCodigoRequestDto(
    val codigo: String
)

data class InscripcionResponseDto(
    val success: Boolean,
    val message: String,
    val asignatura: AsignaturaResponseDto?
)

data class AlumnoAsignaturaResponseDto(
    val id: String,
    val alumnoId: String,
    val asignaturaId: String,
    val fechaInscripcion: String,
    val estado: String
)

