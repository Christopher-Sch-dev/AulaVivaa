package cl.duocuc.aulaviva.backend.presentation.controller

import cl.duocuc.aulaviva.backend.application.dto.request.ActualizarClaseRequest
import cl.duocuc.aulaviva.backend.application.dto.request.CrearClaseRequest
import cl.duocuc.aulaviva.backend.application.dto.response.ApiResponse
import cl.duocuc.aulaviva.backend.application.dto.response.ClaseResponse
import cl.duocuc.aulaviva.backend.application.service.ClaseService
import cl.duocuc.aulaviva.backend.infrastructure.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/clases")
class ClaseController(
    private val claseService: ClaseService
) {

    @PostMapping
    fun crearClase(@Valid @RequestBody request: CrearClaseRequest): ResponseEntity<ApiResponse<ClaseResponse>> {
        return try {
            val creadorId = CurrentUser.getUserId()
            val clase = claseService.crearClase(creadorId, request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(clase, "Clase creada exitosamente"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al crear clase", e.message))
        }
    }

    @GetMapping
    fun obtenerClases(
        @RequestParam(required = false) asignaturaId: UUID?
    ): ResponseEntity<ApiResponse<List<ClaseResponse>>> {
        return try {
            val docenteId = CurrentUser.getUserId()
            val clases = if (asignaturaId != null) {
                claseService.obtenerClasesPorAsignatura(asignaturaId)
            } else {
                claseService.obtenerClasesPorDocente(docenteId)
            }
            ResponseEntity.ok(ApiResponse.success(clases))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener clases", e.message))
        }
    }

    @GetMapping("/{id}")
    fun obtenerClase(@PathVariable id: UUID): ResponseEntity<ApiResponse<ClaseResponse>> {
        return try {
            val clase = claseService.obtenerClasePorId(id)
            ResponseEntity.ok(ApiResponse.success(clase))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Clase no encontrada", e.message))
        }
    }

    @PutMapping("/{id}")
    fun actualizarClase(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ActualizarClaseRequest
    ): ResponseEntity<ApiResponse<ClaseResponse>> {
        return try {
            val creadorId = CurrentUser.getUserId()
            val clase = claseService.actualizarClase(id, creadorId, request)
            ResponseEntity.ok(ApiResponse.success(clase, "Clase actualizada exitosamente"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para actualizar esta clase", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al actualizar clase", e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun eliminarClase(@PathVariable id: UUID): ResponseEntity<ApiResponse<Unit?>> {
        return try {
            val creadorId = CurrentUser.getUserId()
            claseService.eliminarClase(id, creadorId)
            ResponseEntity.ok(ApiResponse.success(null, "Clase eliminada exitosamente"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para eliminar esta clase", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al eliminar clase", e.message))
        }
    }
}

