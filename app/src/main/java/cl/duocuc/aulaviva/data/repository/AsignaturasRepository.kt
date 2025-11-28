package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.remote.SpringBootAsignaturaRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.TokenManager
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
    private val springBootRepository: SpringBootAsignaturaRepository
) : IAsignaturasRepository {

    /**
     * Obtiene asignaturas del docente actual (Flow para LiveData).
     */
    override fun obtenerAsignaturasDocente(): Flow<List<Asignatura>> {
        val docenteId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: ""

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
    override suspend fun sincronizarAsignaturas(): Result<Unit> {
        val docenteId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: return Result.failure(Exception("Usuario no autenticado"))

        Log.d("AsignaturasRepo", "🔄 Sincronizando asignaturas...")

        return try {
            springBootRepository.obtenerAsignaturasDocente(docenteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AsignaturasRepo", "❌ Error sincronizando", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza los alumnos inscritos en una asignatura.
     */
    override suspend fun sincronizarInscritos(asignaturaId: String): Result<Unit> {
        Log.d("AsignaturasRepo", "🔄 Sincronizando inscritos para: $asignaturaId")

        return try {
            // Spring Boot no tiene endpoint específico, usar el de alumnos
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AsignaturasRepo", "❌ Error sincronizando inscritos", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva asignatura.
     */
    override suspend fun crearAsignatura(
        nombre: String,
        descripcion: String
    ): Result<Asignatura> {
        Log.d("AsignaturasRepo", "➕ Creando asignatura: $nombre")

        // Crear asignatura temporal (Spring Boot generará el ID y código)
        val asignatura = Asignatura(
            id = cl.duocuc.aulaviva.utils.IdUtils.generateId(),
            nombre = nombre,
            codigoAcceso = "", // Se generará en el backend
            docenteId = "", // Se obtendrá del token
            descripcion = descripcion,
            createdAt = "",
            updatedAt = ""
        )

        return springBootRepository.crearAsignatura(asignatura)
    }

    /**
     * Genera código único para una asignatura.
     */
    override suspend fun generarCodigo(asignaturaId: String): Result<String> {
        Log.d("AsignaturasRepo", "🔑 Generando código para: $asignaturaId")
        return springBootRepository.generarCodigo(asignaturaId)
    }

    /**
     * Actualiza una asignatura existente.
     */
    override suspend fun actualizarAsignatura(asignatura: Asignatura): Result<Asignatura> {
        Log.d("AsignaturasRepo", "✏️ Actualizando asignatura: ${asignatura.id}")
        return springBootRepository.actualizarAsignatura(asignatura)
    }

    /**
     * Elimina una asignatura.
     */
    override suspend fun eliminarAsignatura(asignaturaId: String): Result<Unit> {
        Log.d("AsignaturasRepo", "🗑️ Eliminando asignatura: $asignaturaId")
        return springBootRepository.eliminarAsignatura(asignaturaId)
    }

    /**
     * Obtiene una asignatura específica por ID.
     */
    override suspend fun obtenerAsignaturaPorId(asignaturaId: String): Asignatura? {
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

    /**
     * Expone las inscripciones (AlumnoAsignaturaEntity) como Flow para la UI (docente).
     */
    override fun obtenerInscritosFlow(asignaturaId: String): Flow<List<AlumnoAsignaturaEntity>> {
        return alumnoAsignaturaDao.obtenerInscripcionesPorAsignatura(asignaturaId)
    }
}
