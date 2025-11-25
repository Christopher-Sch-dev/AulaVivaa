package cl.duocuc.aulaviva.data.model

/**
 * Modelo de datos para la inscripción de un alumno en una asignatura.
 * Representa la relación N:M entre alumnos y asignaturas.
 *
 * Este modelo se sincroniza con la tabla "alumno_asignaturas" en Supabase.
 */
data class AlumnoAsignatura(
    val id: String = "",
    val alumnoId: String = "",
    val asignaturaId: String = "",
    val fechaInscripcion: String = "",
    val estado: String = "activo" // activo, inactivo, completado
)
