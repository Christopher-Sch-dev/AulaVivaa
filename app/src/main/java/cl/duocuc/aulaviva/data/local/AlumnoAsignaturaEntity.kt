package cl.duocuc.aulaviva.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para inscripciones de alumnos en asignaturas.
 * Representa la relación N:M en la base de datos local.
 *
 * Permite modo offline: los alumnos pueden ver sus inscripciones sin conexión.
 */
@Entity(tableName = "alumno_asignaturas")
data class AlumnoAsignaturaEntity(
    @PrimaryKey
    val id: String,
    val alumnoId: String,
    val asignaturaId: String,
    val fechaInscripcion: String = "",
    val estado: String = "activo",
    val sincronizado: Boolean = false
)
