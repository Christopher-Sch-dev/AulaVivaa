package cl.duocuc.aulaviva.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para asignaturas.
 * Representa un curso/asignatura en la base de datos local SQLite.
 *
 * Permite modo offline: los docentes pueden ver sus asignaturas sin conexión.
 */
@Entity(tableName = "asignaturas")
data class AsignaturaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val codigoAcceso: String,
    val docenteId: String,
    val descripcion: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val sincronizado: Boolean = false
)
