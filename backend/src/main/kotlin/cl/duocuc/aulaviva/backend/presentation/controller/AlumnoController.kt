package cl.duocuc.aulaviva.backend.presentation.controller

import cl.duocuc.aulaviva.backend.application.dto.request.InscribirConCodigoRequest
import cl.duocuc.aulaviva.backend.application.dto.response.AlumnoAsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.ApiResponse
import cl.duocuc.aulaviva.backend.application.dto.response.AsignaturaResponse
import cl.duocuc.aulaviva.backend.application.dto.response.InscripcionResponse
import cl.duocuc.aulaviva.backend.application.service.AlumnoService
import cl.duocuc.aulaviva.backend.infrastructure.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/alumnos")
class AlumnoController(
    private val alumnoService: AlumnoService
) {

    @PostMapping("/inscribir")
    fun inscribirConCodigo(@Valid @RequestBody request: InscribirConCodigoRequest): ResponseEntity<ApiResponse<InscripcionResponse>> {
        return try {
            val alumnoId = CurrentUser.getUserId()
            val resultado = alumnoService.inscribirConCodigo(alumnoId, request)

            if (resultado.success) {
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(resultado, resultado.message))
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(resultado.message, null))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al inscribirse", e.message))
        }
    }

    @GetMapping("/asignaturas")
    fun obtenerAsignaturasInscritas(): ResponseEntity<ApiResponse<List<AsignaturaResponse>>> {
        return try {
            val alumnoId = CurrentUser.getUserId()
            val asignaturas = alumnoService.obtenerAsignaturasInscritas(alumnoId)
            ResponseEntity.ok(ApiResponse.success(asignaturas))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener asignaturas inscritas", e.message))
        }
    }

    @DeleteMapping("/asignaturas/{asignaturaId}")
    fun darDeBaja(@PathVariable asignaturaId: UUID): ResponseEntity<ApiResponse<Unit?>> {
        return try {
            val alumnoId = CurrentUser.getUserId()
            alumnoService.darDeBaja(alumnoId, asignaturaId)
            ResponseEntity.ok(ApiResponse.success(null, "Baja exitosa"))
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para dar de baja", e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al dar de baja", e.message))
        }
    }

    @GetMapping("/asignaturas/{asignaturaId}/inscripciones")
    fun obtenerInscripciones(@PathVariable asignaturaId: UUID): ResponseEntity<ApiResponse<List<AlumnoAsignaturaResponse>>> {
        return try {
            val inscripciones = alumnoService.obtenerInscripcionesPorAsignatura(asignaturaId)
            ResponseEntity.ok(ApiResponse.success(inscripciones))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener inscripciones", e.message))
        }
    }
}

