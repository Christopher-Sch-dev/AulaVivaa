package cl.duocuc.aulaviva.backend.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CrearAsignaturaRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    val nombre: String,

    @field:Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    val descripcion: String = ""
)

data class ActualizarAsignaturaRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    val nombre: String,

    @field:Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    val descripcion: String = ""
)

