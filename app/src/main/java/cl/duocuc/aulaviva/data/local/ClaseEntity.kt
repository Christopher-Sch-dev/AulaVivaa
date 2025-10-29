package cl.duocuc.aulaviva.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para guardar clases en la base de datos local SQLite.
 * Esta tabla permite que la app funcione sin internet (modo offline).
 * 
 * @Entity indica que esta clase representa una tabla en la BD local
 * @PrimaryKey es el identificador único de cada registro
 * 
 * Pensamiento: Necesito guardar las clases localmente por si pierdo conexión.
 * Room me permite hacer eso de forma simple y Android ya lo trae incluido.
 */
@Entity(tableName = "clases")
data class ClaseEntity(
    @PrimaryKey 
    val id: String,              // ID único de Firestore
    val nombre: String,          // Nombre de la clase
    val fecha: String,           // Fecha en formato texto (dd/MM/yyyy)
    val creador: String,         // UID del docente que la creó
    val sincronizado: Boolean = false  // ¿Ya está en Firestore? (para sincronizar después)
)
