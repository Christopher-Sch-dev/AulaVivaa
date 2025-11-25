package cl.duocuc.aulaviva.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs para serialización con Supabase.
 * Estos modelos mapean directamente con las tablas de PostgreSQL.
 */

@Serializable
data class AsignaturaSupabaseDto(
    val id: String = "",
    val nombre: String = "",
    @SerialName("codigo_acceso")
    val codigoAcceso: String = "",
    @SerialName("docente_id")
    val docenteId: String = "",
    val descripcion: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class AlumnoAsignaturaSupabaseDto(
    val id: String = "",
    @SerialName("alumno_id")
    val alumnoId: String = "",
    @SerialName("asignatura_id")
    val asignaturaId: String = "",
    @SerialName("fecha_inscripcion")
    val fechaInscripcion: String = "",
    val estado: String = "activo"
)

/**
 * Response de la función RPC rpc_inscribir_con_codigo
 */
@Serializable
data class InscripcionResponse(
    val success: Boolean = false,
    val error: String? = null,
    val asignatura: AsignaturaInscripcionDto? = null
)

@Serializable
data class AsignaturaInscripcionDto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    @SerialName("docente_id")
    val docenteId: String = ""
)

/**
 * Request para generar código de asignatura
 */
@Serializable
data class GenerarCodigoRequest(
    @SerialName("p_asignatura_id")
    val asignaturaId: String
)

/**
 * Request para inscribirse con código
 */
@Serializable
data class InscribirConCodigoRequest(
    @SerialName("p_codigo_acceso")
    val codigoAcceso: String
)
