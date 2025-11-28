package cl.duocuc.aulaviva.backend.application.dto.response

import java.time.OffsetDateTime
import java.util.*

data class ClaseResponse(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val archivoPdfUrl: String,
    val archivoPdfNombre: String,
    val creador: UUID,
    val asignaturaId: UUID?,
    val createdAt: OffsetDateTime
)

