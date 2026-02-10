package cl.duocuc.aulaviva.backend.presentation.controller

import cl.duocuc.aulaviva.backend.application.dto.request.ActualizarAsignaturaRequest
import cl.duocuc.aulaviva.backend.application.dto.request.CrearAsignaturaRequest
import cl.duocuc.aulaviva.backend.application.dto.response.ApiResponse
import cl.duocuc.aulaviva.backend.application.dto.response.AsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.GenerarCodigoResponse
import cl.duocuc.aulaviva.backend.application.service.AsignaturaService
import cl.duocuc.aulaviva.backend.infrastructure.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/asignaturas")
class AsignaturaController(
    private val asignaturaService: AsignaturaService
) {

    @PostMapping
    fun crearAsignatura(@Valid @RequestBody request: CrearAsignaturaRequest): ResponseEntity<ApiResponse<AsignaturaResponse>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            val asignatura = asignaturaService.crearAsignatura(docenteId, request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(asignatura, "Asignatura creada exitosamente"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al crear asignatura", e.message))
        }
    }

    @GetMapping
    fun obtenerAsignaturas(): ResponseEntity<ApiResponse<List<AsignaturaResponse>>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            val asignaturas = asignaturaService.obtenerAsignaturasPorDocente(docenteId)
            ResponseEntity.ok(ApiResponse.success(asignaturas))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener asignaturas", e.message))
        }
    }

    @GetMapping("/{id}")
    fun obtenerAsignatura(@PathVariable id: UUID): ResponseEntity<ApiResponse<AsignaturaResponse>> {
        return try {
            val asignatura = asignaturaService.obtenerAsignaturaPorId(id)
            ResponseEntity.ok(ApiResponse.success(asignatura))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Asignatura no encontrada", e.message))
        }
    }

    @PutMapping("/{id}")
    fun actualizarAsignatura(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ActualizarAsignaturaRequest
    ): ResponseEntity<ApiResponse<AsignaturaResponse>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            val asignatura = asignaturaService.actualizarAsignatura(id, docenteId, request)
            ResponseEntity.ok(ApiResponse.success(asignatura, "Asignatura actualizada exitosamente"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para actualizar esta asignatura", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al actualizar asignatura", e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun eliminarAsignatura(@PathVariable id: UUID): ResponseEntity<ApiResponse<Unit?>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            asignaturaService.eliminarAsignatura(id, docenteId)
            ResponseEntity.ok(ApiResponse.success(null, "Asignatura eliminada exitosamente"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para eliminar esta asignatura", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al eliminar asignatura", e.message))
        }
    }

    @PostMapping("/{id}/generar-codigo")
    fun generarCodigo(@PathVariable id: UUID): ResponseEntity<ApiResponse<GenerarCodigoResponse>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            val codigo = asignaturaService.generarCodigo(id, docenteId)
            ResponseEntity.ok(ApiResponse.success(codigo, "Código generado exitosamente"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para generar código", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al generar código", e.message))
        }
    }
}

