package cl.duocuc.aulaviva.data.repository

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository MVVM para asignaturas.
 * Orquesta la lógica entre Room (local) y Supabase (remoto).
 *
 * Patrón: Offline-first
 * - Siempre lee de Room (rápido)
 * - Sincroniza con Supabase en segundo plano
 */
class AsignaturasRepository(
    private val asignaturaDao: AsignaturaDao,
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao,
    private val supabaseRepository: SupabaseAsignaturaRepository
) {

    /**
     * Obtiene asignaturas del docente actual (Flow para LiveData).
     */
    fun obtenerAsignaturasDocente(): Flow<List<Asignatura>> {
        val docenteId = SupabaseAuthManager.getCurrentUserId() ?: ""

        // Leer de Room (offline-first)
        return asignaturaDao.obtenerAsignaturasPorDocente(docenteId).map { entities ->
            entities.map { entity ->
                Asignatura(
                    id = entity.id,
                    nombre = entity.nombre,
                    codigoAcceso = entity.codigoAcceso,
                    docenteId = entity.docenteId,
                    descripcion = entity.descripcion,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    /**
     * Sincroniza asignaturas desde Supabase.
     */
    suspend fun sincronizarAsignaturas(): Result<Unit> {
        val docenteId = SupabaseAuthManager.getCurrentUserId()
            ?: return Result.failure(Exception("Usuario no autenticado"))

        Log.d("AsignaturasRepo", "🔄 Sincronizando asignaturas...")

        return try {
            supabaseRepository.obtenerAsignaturasDocente(docenteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AsignaturasRepo", "❌ Error sincronizando", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza los alumnos inscritos en una asignatura.
     */
    suspend fun sincronizarInscritos(asignaturaId: String): Result<Unit> {
        Log.d("AsignaturasRepo", "🔄 Sincronizando inscritos para: $asignaturaId")

        return try {
            supabaseRepository.obtenerInscritosPorAsignatura(asignaturaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AsignaturasRepo", "❌ Error sincronizando inscritos", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva asignatura.
     */
    suspend fun crearAsignatura(
        nombre: String,
        descripcion: String
    ): Result<Asignatura> {
        val docenteId = SupabaseAuthManager.getCurrentUserId()
            ?: return Result.failure(Exception("Usuario no autenticado"))

        Log.d("AsignaturasRepo", "➕ Creando asignatura: $nombre")

        // Generar código temporal (se actualizará después)
        val codigoTemporal = "TEMP-${System.currentTimeMillis().toString().takeLast(4)}"

        // Crear timestamp actual
        val now = java.time.Instant.now().toString()

        val asignatura = Asignatura(
            id = java.util.UUID.randomUUID().toString(),
            nombre = nombre,
            codigoAcceso = codigoTemporal,
            docenteId = docenteId,
            descripcion = descripcion,
            createdAt = now,
            updatedAt = now
        )

        return supabaseRepository.crearAsignatura(asignatura)
    }

    /**
     * Genera código único para una asignatura.
     */
    suspend fun generarCodigo(asignaturaId: String): Result<String> {
        Log.d("AsignaturasRepo", "🔑 Generando código para: $asignaturaId")
        return supabaseRepository.generarCodigo(asignaturaId)
    }

    /**
     * Actualiza una asignatura existente.
     */
    suspend fun actualizarAsignatura(asignatura: Asignatura): Result<Asignatura> {
        Log.d("AsignaturasRepo", "✏️ Actualizando asignatura: ${asignatura.id}")
        return supabaseRepository.actualizarAsignatura(asignatura)
    }

    /**
     * Elimina una asignatura.
     */
    suspend fun eliminarAsignatura(asignaturaId: String): Result<Unit> {
        Log.d("AsignaturasRepo", "🗑️ Eliminando asignatura: $asignaturaId")
        return supabaseRepository.eliminarAsignatura(asignaturaId)
    }

    /**
     * Obtiene una asignatura específica por ID.
     */
    suspend fun obtenerAsignaturaPorId(asignaturaId: String): Asignatura? {
        val entity = asignaturaDao.obtenerAsignaturaPorId(asignaturaId)
        return entity?.let {
            Asignatura(
                id = it.id,
                nombre = it.nombre,
                codigoAcceso = it.codigoAcceso,
                docenteId = it.docenteId,
                descripcion = it.descripcion,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }
}
