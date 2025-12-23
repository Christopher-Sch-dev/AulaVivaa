package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IAlumnoRepository

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.remote.SpringBootAlumnoRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository MVVM para alumnos (inscripciones).
 * Orquesta la lógica de inscripción y gestión de asignaturas inscritas.
 */
class AlumnoRepository(
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao,
    private val asignaturaDao: AsignaturaDao,
    private val springBootRepository: SpringBootAlumnoRepository
) : IAlumnoRepository {

    /**
     * Obtiene asignaturas inscritas del alumno actual (Flow para LiveData).
     */
    override fun obtenerAsignaturasInscritas(): Flow<List<Asignatura>> {
        val alumnoId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: ""

        // Obtener IDs de inscripciones desde Room
        return alumnoAsignaturaDao.obtenerInscripcionesActivas(alumnoId).map { inscripciones ->
            val asignaturasIds = inscripciones.map { it.asignaturaId }

            if (asignaturasIds.isEmpty()) {
                emptyList()
            } else {
                // Obtener asignaturas correspondientes
                val entities = asignaturasIds.mapNotNull { id ->
                    asignaturaDao.obtenerAsignaturaPorId(id)
                }

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
                }.distinctBy { it.id } // Ensure unique keys for LazyColumn
            }
        }
    }

    /**
     * Sincroniza asignaturas inscritas desde Spring Boot.
     */
    override suspend fun sincronizarAsignaturasInscritas(): Result<Unit> {
        val alumnoId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: return Result.failure(Exception("Usuario no autenticado"))

        Log.d("AlumnoRepo", "🔄 Sincronizando asignaturas inscritas...")

        return try {
            springBootRepository.obtenerAsignaturasInscritas(alumnoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AlumnoRepo", "❌ Error sincronizando", e)
            Result.failure(e)
        }
    }

    /**
     * Inscribe al alumno en una asignatura usando código.
     */
    override suspend fun inscribirConCodigo(codigo: String): Result<Asignatura> {
        if (codigo.isBlank()) {
            return Result.failure(Exception("El código no puede estar vacío"))
        }

        Log.d("AlumnoRepo", "🎓 Inscribiendo con código: $codigo")
        return springBootRepository.inscribirConCodigo(codigo)
    }

    /**
     * Darse de baja de una asignatura.
     */
    override suspend fun darDeBaja(asignaturaId: String): Result<Unit> {
        val alumnoId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: return Result.failure(Exception("Usuario no autenticado"))

        Log.d("AlumnoRepo", "🚪 Dando de baja de: $asignaturaId")
        return springBootRepository.darDeBaja(alumnoId, asignaturaId)
    }

    /**
     * Verifica si el alumno está inscrito en una asignatura.
     */
    override suspend fun estaInscrito(asignaturaId: String): Boolean {
        val alumnoId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
            cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
        ) ?: return false
        val inscripcion = alumnoAsignaturaDao.obtenerInscripcion(alumnoId, asignaturaId)
        return inscripcion != null && inscripcion.estado == "activo"
    }

    /**
     * Obtiene una asignatura específica por ID (desde Room).
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
}
