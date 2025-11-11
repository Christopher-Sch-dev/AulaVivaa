package cl.duocuc.aulaviva.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para asignaturas.
 * Define operaciones CRUD en la base de datos local.
 *
 * Room genera automáticamente el código SQL por mí.
 */
@Dao
interface AsignaturaDao {

    /**
     * Obtiene todas las asignaturas de un docente específico.
     */
    @Query("SELECT * FROM asignaturas WHERE docenteId = :docenteId ORDER BY createdAt DESC")
    fun obtenerAsignaturasPorDocente(docenteId: String): Flow<List<AsignaturaEntity>>

    /**
     * Obtiene una asignatura específica por ID.
     */
    @Query("SELECT * FROM asignaturas WHERE id = :asignaturaId LIMIT 1")
    suspend fun obtenerAsignaturaPorId(asignaturaId: String): AsignaturaEntity?

    /**
     * Obtiene una asignatura por su código de acceso.
     */
    @Query("SELECT * FROM asignaturas WHERE codigoAcceso = :codigo LIMIT 1")
    suspend fun obtenerAsignaturaPorCodigo(codigo: String): AsignaturaEntity?

    /**
     * Obtiene todas las asignaturas (para sincronización).
     */
    @Query("SELECT * FROM asignaturas ORDER BY createdAt DESC")
    fun obtenerTodasLasAsignaturas(): Flow<List<AsignaturaEntity>>

    /**
     * Inserta o reemplaza una asignatura.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAsignatura(asignatura: AsignaturaEntity)

    /**
     * Inserta múltiples asignaturas (para sincronización desde Supabase).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVarias(asignaturas: List<AsignaturaEntity>)

    /**
     * Actualiza una asignatura existente.
     */
    @Update
    suspend fun actualizarAsignatura(asignatura: AsignaturaEntity)

    /**
     * Elimina una asignatura específica.
     */
    @Delete
    suspend fun eliminarAsignatura(asignatura: AsignaturaEntity)

    /**
     * Elimina todas las asignaturas (útil al cerrar sesión).
     */
    @Query("DELETE FROM asignaturas")
    suspend fun eliminarTodas()

    /**
     * Obtiene asignaturas no sincronizadas con Supabase.
     */
    @Query("SELECT * FROM asignaturas WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<AsignaturaEntity>
}
