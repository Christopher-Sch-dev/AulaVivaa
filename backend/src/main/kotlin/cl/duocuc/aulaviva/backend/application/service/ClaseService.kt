package cl.duocuc.aulaviva.backend.application.service

import cl.duocuc.aulaviva.backend.application.dto.request.ActualizarClaseRequest
import cl.duocuc.aulaviva.backend.application.dto.request.CrearClaseRequest
import cl.duocuc.aulaviva.backend.application.dto.response.ClaseResponse
import cl.duocuc.aulaviva.backend.domain.entity.Clase
import cl.duocuc.aulaviva.backend.infrastructure.repository.ClaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ClaseService(
    private val claseRepository: ClaseRepository
) {

    @Transactional
    fun crearClase(creadorId: UUID, request: CrearClaseRequest): ClaseResponse {
        val clase = Clase(
            id = UUID.randomUUID().toString(), // Generar UUID como String
            nombre = request.nombre,
            descripcion = request.descripcion,
            fecha = request.fecha.ifEmpty { System.currentTimeMillis().toString() },
            archivoPdfUrl = request.archivoPdfUrl,
            archivoPdfNombre = request.archivoPdfNombre,
            creador = creadorId,
            asignaturaId = request.asignaturaId
        )

        return claseRepository.save(clase).toResponse()
    }

    fun obtenerClasesPorDocente(docenteId: UUID): List<ClaseResponse> {
        return claseRepository.findClasesByDocente(docenteId)
            .map { it.toResponse() }
    }

    fun obtenerClasesPorAsignatura(asignaturaId: UUID): List<ClaseResponse> {
        return claseRepository.findByAsignaturaId(asignaturaId)
            .map { it.toResponse() }
    }

    fun obtenerClasesPorAsignaturas(asignaturasIds: List<UUID>): List<ClaseResponse> {
        return claseRepository.findByAsignaturasIds(asignaturasIds)
            .map { it.toResponse() }
    }

    fun obtenerClasePorId(claseId: String): ClaseResponse {
        val clase = claseRepository.findById(claseId)
            .orElseThrow { RuntimeException("Clase no encontrada") }
        return clase.toResponse()
    }

    @Transactional
    fun actualizarClase(
        claseId: String,
        creadorId: UUID,
        request: ActualizarClaseRequest
    ): ClaseResponse {
        val clase = claseRepository.findById(claseId)
            .orElseThrow { RuntimeException("Clase no encontrada") }

        if (clase.creador != creadorId) {
            throw SecurityException("No tienes permiso para actualizar esta clase")
        }

        val claseActualizada = clase.copy(
            nombre = request.nombre,
            descripcion = request.descripcion,
            fecha = request.fecha,
            archivoPdfUrl = request.archivoPdfUrl,
            archivoPdfNombre = request.archivoPdfNombre
        )

        return claseRepository.save(claseActualizada).toResponse()
    }

    @Transactional
    fun eliminarClase(claseId: String, creadorId: UUID) {
        val clase = claseRepository.findById(claseId)
            .orElseThrow { RuntimeException("Clase no encontrada") }

        if (clase.creador != creadorId) {
            throw SecurityException("No tienes permiso para eliminar esta clase")
        }

        claseRepository.delete(clase)
    }

    private fun Clase.toResponse() = ClaseResponse(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        fecha = fecha,
        archivoPdfUrl = archivoPdfUrl,
        archivoPdfNombre = archivoPdfNombre,
        creador = creador,
        asignaturaId = asignaturaId,
        createdAt = createdAt
    )
}

