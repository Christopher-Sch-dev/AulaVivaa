package cl.duocuc.aulaviva.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) - Interface que define las operaciones con la BD local.
 * Room genera automáticamente el código de estas funciones.
 *
 * Uso Flow para que los datos se actualicen automáticamente en la UI cuando cambien.
 * Uso suspend para operaciones que modifican datos (insert, update, delete).
 *
 * Pensamiento: El DAO es como el "repository" pero para la base de datos local.
 * Room hace toda la magia SQL por mí, yo solo defino qué quiero hacer.
 */
@Dao
interface ClaseDao {

    /**
     * Obtiene todas las clases de un usuario específico.
     * Flow emite nuevos valores automáticamente cuando la BD cambia.
     */
    @Query("SELECT * FROM clases WHERE creador = :uid ORDER BY fecha DESC")
    fun obtenerClasesPorUsuario(uid: String): Flow<List<ClaseEntity>>

    /**
     * Obtiene una clase específica por su ID.
     */
    @Query("SELECT * FROM clases WHERE id = :claseId LIMIT 1")
    suspend fun obtenerClasePorId(claseId: String): ClaseEntity?

    /**
     * Obtiene todas las clases (para alumnos que ven clases de todos los profes)
     */
    @Query("SELECT * FROM clases ORDER BY fecha DESC")
    fun obtenerTodasLasClases(): Flow<List<ClaseEntity>>

    /**
     * Inserta o reemplaza una clase en la BD local.
     * onConflict = REPLACE significa que si el ID ya existe, lo actualiza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarClase(clase: ClaseEntity)

    /**
     * Inserta múltiples clases de una vez (útil para sincronizar desde Firestore)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVarias(clases: List<ClaseEntity>)

    /**
     * Actualiza una clase existente
     */
    @Update
    suspend fun actualizarClase(clase: ClaseEntity)

    /**
     * Elimina una clase específica
     */
    @Delete
    suspend fun eliminarClase(clase: ClaseEntity)

    /**
     * Elimina todas las clases (útil para cerrar sesión y limpiar datos)
     */
    @Query("DELETE FROM clases")
    suspend fun eliminarTodas()

    /**
     * Obtiene clases que aún no se han sincronizado con Firestore
     * (útil cuando vuelve la conexión a internet)
     */
    @Query("SELECT * FROM clases WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<ClaseEntity>
}
