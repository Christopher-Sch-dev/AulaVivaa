package cl.duocuc.aulaviva.backend.application.service

import cl.duocuc.aulaviva.backend.application.dto.request.InscribirConCodigoRequest
import cl.duocuc.aulaviva.backend.application.dto.response.AlumnoAsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.AsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.InscripcionResponse
import cl.duocuc.aulaviva.backend.domain.entity.AlumnoAsignatura
import cl.duocuc.aulaviva.backend.domain.entity.EstadoInscripcion
import cl.duocuc.aulaviva.backend.infrastructure.repository.AlumnoAsignaturaRepository
import cl.duocuc.aulaviva.backend.infrastructure.repository.AsignaturaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AlumnoService(
    private val alumnoAsignaturaRepository: AlumnoAsignaturaRepository,
    private val asignaturaRepository: AsignaturaRepository
) {

    @Transactional
    fun inscribirConCodigo(alumnoId: UUID, request: InscribirConCodigoRequest): InscripcionResponse {
        val codigoLimpio = request.codigo.uppercase().trim()

        // Buscar asignatura por código
        val asignatura = asignaturaRepository.findByCodigoAcceso(codigoLimpio)
            ?: return InscripcionResponse(
                success = false,
                message = "Código inválido",
                asignatura = null
            )

        // Verificar si ya está inscrito
        if (alumnoAsignaturaRepository.existsByAlumnoIdAndAsignaturaId(alumnoId, asignatura.id)) {
            return InscripcionResponse(
                success = false,
                message = "Ya estás inscrito en esta asignatura",
                asignatura = null
            )
        }

        // Crear inscripción
        val inscripcion = AlumnoAsignatura(
            alumnoId = alumnoId,
            asignaturaId = asignatura.id,
            estado = EstadoInscripcion.activo
        )

        alumnoAsignaturaRepository.save(inscripcion)

        return InscripcionResponse(
            success = true,
            message = "Inscripción exitosa",
            asignatura = asignatura.toResponse()
        )
    }

    fun obtenerAsignaturasInscritas(alumnoId: UUID): List<AsignaturaResponse> {
        val inscripciones = alumnoAsignaturaRepository.findByAlumnoIdAndEstado(
            alumnoId,
            EstadoInscripcion.activo
        )

        val asignaturasIds = inscripciones.map { it.asignaturaId }

        if (asignaturasIds.isEmpty()) {
            return emptyList()
        }

        return asignaturaRepository.findByIds(asignaturasIds)
            .map { it.toResponse() }
    }

    @Transactional
    fun darDeBaja(alumnoId: UUID, asignaturaId: UUID) {
        val inscripcion = alumnoAsignaturaRepository.findByAlumnoIdAndAsignaturaId(alumnoId, asignaturaId)
            ?: throw RuntimeException("Inscripción no encontrada")

        if (inscripcion.alumnoId != alumnoId) {
            throw SecurityException("No tienes permiso para dar de baja esta inscripción")
        }

        alumnoAsignaturaRepository.delete(inscripcion)
    }

    fun obtenerInscripcionesPorAsignatura(asignaturaId: UUID): List<AlumnoAsignaturaResponse> {
        return alumnoAsignaturaRepository.findByAsignaturaId(asignaturaId)
            .map { it.toResponse() }
    }

    private fun cl.duocuc.aulaviva.backend.domain.entity.Asignatura.toResponse(): AsignaturaResponse = AsignaturaResponse(
        id = id,
        nombre = nombre,
        codigoAcceso = codigoAcceso,
        docenteId = docenteId,
        descripcion = descripcion,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun AlumnoAsignatura.toResponse() = AlumnoAsignaturaResponse(
        id = id,
        alumnoId = alumnoId,
        asignaturaId = asignaturaId,
        fechaInscripcion = fechaInscripcion,
        estado = estado.name
    )
}

