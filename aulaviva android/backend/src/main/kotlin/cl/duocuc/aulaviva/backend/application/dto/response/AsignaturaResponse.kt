package cl.duocuc.aulaviva.backend.application.dto.response

import java.time.OffsetDateTime
import java.util.*

data class AsignaturaResponse(
    val id: UUID,
    val nombre: String,
    val codigoAcceso: String,
    val docenteId: UUID,
    val descripcion: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

data class GenerarCodigoResponse(
    val codigo: String
)

