package cl.duocuc.aulaviva.backend.application.dto.response

import java.time.OffsetDateTime
import java.util.*

data class AlumnoAsignaturaResponse(
    val id: UUID,
    val alumnoId: UUID,
    val asignaturaId: UUID,
    val fechaInscripcion: OffsetDateTime,
    val estado: String
)

data class InscripcionResponse(
    val success: Boolean,
    val message: String,
    val asignatura: AsignaturaResponse?
)

