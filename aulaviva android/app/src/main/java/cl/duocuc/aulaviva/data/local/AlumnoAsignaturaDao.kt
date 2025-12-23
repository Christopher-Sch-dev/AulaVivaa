package cl.duocuc.aulaviva.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para inscripciones de alumnos en asignaturas.
 * Gestiona la relación N:M entre alumnos y asignaturas.
 */
@Dao
interface AlumnoAsignaturaDao {

    /**
     * Obtiene todas las inscripciones de un alumno.
     */
    @Query("SELECT * FROM alumno_asignaturas WHERE alumnoId = :alumnoId ORDER BY fechaInscripcion DESC")
    fun obtenerInscripcionesPorAlumno(alumnoId: String): Flow<List<AlumnoAsignaturaEntity>>

    /**
     * Obtiene todas las inscripciones de una asignatura (para docentes).
     */
    @Query("SELECT * FROM alumno_asignaturas WHERE asignaturaId = :asignaturaId ORDER BY fechaInscripcion DESC")
    fun obtenerInscripcionesPorAsignatura(asignaturaId: String): Flow<List<AlumnoAsignaturaEntity>>

    /**
     * Verifica si un alumno está inscrito en una asignatura.
     */
    @Query("SELECT * FROM alumno_asignaturas WHERE alumnoId = :alumnoId AND asignaturaId = :asignaturaId LIMIT 1")
    suspend fun obtenerInscripcion(alumnoId: String, asignaturaId: String): AlumnoAsignaturaEntity?

    /**
     * Obtiene todas las inscripciones activas de un alumno.
     */
    @Query("SELECT * FROM alumno_asignaturas WHERE alumnoId = :alumnoId AND estado = 'activo' ORDER BY fechaInscripcion DESC")
    fun obtenerInscripcionesActivas(alumnoId: String): Flow<List<AlumnoAsignaturaEntity>>

    /**
     * Inserta o reemplaza una inscripción.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarInscripcion(inscripcion: AlumnoAsignaturaEntity)

    /**
     * Inserta múltiples inscripciones (para sincronización).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVarias(inscripciones: List<AlumnoAsignaturaEntity>)

    /**
     * Actualiza una inscripción (cambiar estado).
     */
    @Update
    suspend fun actualizarInscripcion(inscripcion: AlumnoAsignaturaEntity)

    /**
     * Elimina una inscripción específica.
     */
    @Delete
    suspend fun eliminarInscripcion(inscripcion: AlumnoAsignaturaEntity)

    /**
     * Elimina todas las inscripciones de un alumno.
     */
    @Query("DELETE FROM alumno_asignaturas WHERE alumnoId = :alumnoId")
    suspend fun eliminarInscripcionesDeAlumno(alumnoId: String)

    /**
     * Elimina todas las inscripciones.
     */
    @Query("DELETE FROM alumno_asignaturas")
    suspend fun eliminarTodas()

    /**
     * Obtiene inscripciones no sincronizadas.
     */
    @Query("SELECT * FROM alumno_asignaturas WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<AlumnoAsignaturaEntity>
}
