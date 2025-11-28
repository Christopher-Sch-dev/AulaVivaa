package cl.duocuc.aulaviva.backend.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class CrearClaseRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    val nombre: String,

    @field:Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
    val descripcion: String = "",

    @field:NotBlank(message = "La fecha es obligatoria")
    val fecha: String,

    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = "",
    val asignaturaId: UUID? = null
)

data class ActualizarClaseRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    val nombre: String,

    @field:Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
    val descripcion: String = "",

    @field:NotBlank(message = "La fecha es obligatoria")
    val fecha: String,

    val archivoPdfUrl: String = "",
    val archivoPdfNombre: String = ""
)

data class InscribirConCodigoRequest(
    @field:NotBlank(message = "El código es obligatorio")
    val codigo: String
)

