package cl.duocuc.aulaviva.data.model

/**
 * Modelo de datos para Asignatura.
 * Representa un curso/asignatura creada por un docente.
 *
 * Este modelo se sincroniza con la tabla "asignaturas" en Supabase.
 * Los alumnos se inscriben mediante código único.
 */
data class Asignatura(
    val id: String = "",
    val nombre: String = "",
    val codigoAcceso: String = "",
    val docenteId: String = "",
    val descripcion: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
