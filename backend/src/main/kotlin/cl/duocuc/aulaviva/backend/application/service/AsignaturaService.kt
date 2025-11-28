package cl.duocuc.aulaviva.backend.application.service

import cl.duocuc.aulaviva.backend.application.dto.request.ActualizarAsignaturaRequest
import cl.duocuc.aulaviva.backend.application.dto.request.CrearAsignaturaRequest
import cl.duocuc.aulaviva.backend.application.dto.response.AsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.GenerarCodigoResponse
import cl.duocuc.aulaviva.backend.domain.entity.Asignatura
import cl.duocuc.aulaviva.backend.infrastructure.repository.AsignaturaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AsignaturaService(
    private val asignaturaRepository: AsignaturaRepository
) {

    @Transactional
    fun crearAsignatura(docenteId: UUID, request: CrearAsignaturaRequest): AsignaturaResponse {
        // Generar código temporal (se actualizará después)
        val codigoTemporal = "TEMP-${System.currentTimeMillis().toString().takeLast(4)}"

        val asignatura = Asignatura(
            nombre = request.nombre,
            codigoAcceso = codigoTemporal,
            docenteId = docenteId,
            descripcion = request.descripcion
        )

        // Generar código real antes de guardar
        val codigoReal = generarCodigoUnico(request.nombre)
        val asignaturaConCodigo = asignatura.copy(codigoAcceso = codigoReal)
        val asignaturaFinal = asignaturaRepository.save(asignaturaConCodigo)

        return asignaturaFinal.toResponse()
    }

    fun obtenerAsignaturasPorDocente(docenteId: UUID): List<AsignaturaResponse> {
        return asignaturaRepository.findByDocenteId(docenteId)
            .map { it.toResponse() }
    }

    fun obtenerAsignaturaPorId(asignaturaId: UUID): AsignaturaResponse {
        val asignatura = asignaturaRepository.findById(asignaturaId)
            .orElseThrow { RuntimeException("Asignatura no encontrada") }
        return asignatura.toResponse()
    }

    @Transactional
    fun actualizarAsignatura(
        asignaturaId: UUID,
        docenteId: UUID,
        request: ActualizarAsignaturaRequest
    ): AsignaturaResponse {
        val asignatura = asignaturaRepository.findById(asignaturaId)
            .orElseThrow { RuntimeException("Asignatura no encontrada") }

        if (asignatura.docenteId != docenteId) {
            throw SecurityException("No tienes permiso para actualizar esta asignatura")
        }

        val asignaturaActualizada = asignatura.copy(
            nombre = request.nombre,
            descripcion = request.descripcion
        )

        return asignaturaRepository.save(asignaturaActualizada).toResponse()
    }

    @Transactional
    fun eliminarAsignatura(asignaturaId: UUID, docenteId: UUID) {
        val asignatura = asignaturaRepository.findById(asignaturaId)
            .orElseThrow { RuntimeException("Asignatura no encontrada") }

        if (asignatura.docenteId != docenteId) {
            throw SecurityException("No tienes permiso para eliminar esta asignatura")
        }

        asignaturaRepository.delete(asignatura)
    }

    @Transactional
    fun generarCodigo(asignaturaId: UUID, docenteId: UUID): GenerarCodigoResponse {
        val asignatura = asignaturaRepository.findById(asignaturaId)
            .orElseThrow { RuntimeException("Asignatura no encontrada") }

        if (asignatura.docenteId != docenteId) {
            throw SecurityException("No tienes permiso para generar código de esta asignatura")
        }

        val codigo = generarCodigoUnico(asignatura.nombre)
        val asignaturaActualizada = asignatura.copy(codigoAcceso = codigo)
        asignaturaRepository.save(asignaturaActualizada)

        return GenerarCodigoResponse(codigo)
    }

    fun buscarPorCodigo(codigo: String): AsignaturaResponse? {
        val asignatura = asignaturaRepository.findByCodigoAcceso(codigo.uppercase().trim())
        return asignatura?.toResponse()
    }

    private fun generarCodigoUnico(nombreAsignatura: String): String {
        val caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefijo = nombreAsignatura
            .replace(Regex("[^a-zA-Z]"), "")
            .uppercase()
            .take(3)
            .ifEmpty { "ASG" }
        val año = java.time.Year.now().value
        val prefijoCompleto = "$prefijo$año"

        var intentos = 0
        while (intentos < 100) {
            val codigoAleatorio = (1..4).map {
                caracteres.random()
            }.joinToString("")
            val codigoCompleto = "$prefijoCompleto-$codigoAleatorio"

            if (!asignaturaRepository.existsByCodigoAcceso(codigoCompleto)) {
                return codigoCompleto
            }
            intentos++
        }

        throw RuntimeException("No se pudo generar código único después de 100 intentos")
    }

    private fun Asignatura.toResponse() = AsignaturaResponse(
        id = id,
        nombre = nombre,
        codigoAcceso = codigoAcceso,
        docenteId = docenteId,
        descripcion = descripcion,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

