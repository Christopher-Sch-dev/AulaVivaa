package cl.duocuc.aulaviva.data.remote

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.AlumnoAsignatura
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository remoto para alumnos usando Spring Boot.
 * Reemplaza a SupabaseAlumnoRepository.
 */
class SpringBootAlumnoRepository(
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao,
    private val asignaturaDao: AsignaturaDao,
    private val apiService: SpringBootApiService
) {

    suspend fun inscribirConCodigo(codigo: String): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val request = InscribirConCodigoRequestDto(codigo = codigo)
            val response = apiService.inscribirConCodigo("Bearer ${TokenManager.getToken()}", request)

            if (response.isSuccessful && response.body()?.success == true) {
                val inscripcion = response.body()!!.data!!

                if (inscripcion.success && inscripcion.asignatura != null) {
                    val asignatura = inscripcion.asignatura!!.toAsignatura()

                    // Guardar asignatura en Room
                    asignaturaDao.insertarAsignatura(asignatura.toEntity(sincronizado = true))

                    // Obtener ID del alumno del token JWT
                    val alumnoId = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
                        TokenManager.getToken() ?: ""
                    ) ?: ""

                    if (alumnoId.isNotEmpty()) {
                        // Crear inscripción en Room
                        val inscripcionEntity = AlumnoAsignaturaEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            alumnoId = alumnoId,
                            asignaturaId = asignatura.id,
                            fechaInscripcion = java.time.OffsetDateTime.now().toString(),
                            estado = "activo",
                            sincronizado = true
                        )
                        alumnoAsignaturaDao.insertarInscripcion(inscripcionEntity)
                        Log.d("SpringBootAlumno", "✅ Inscripción guardada en Room: ${asignatura.nombre}")
                    } else {
                        Log.w("SpringBootAlumno", "⚠️ No se pudo obtener ID del alumno del token")
                    }

                    Log.d("SpringBootAlumno", "✅ Inscrito en: ${asignatura.nombre}")
                    Result.success(asignatura)
                } else {
                    Result.failure(Exception(inscripcion.message))
                }
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAlumno", "❌ Error inscribiendo", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerAsignaturasInscritas(alumnoId: String): Result<List<Asignatura>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerAsignaturasInscritas("Bearer ${TokenManager.getToken()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val asignaturasDto = response.body()!!.data!!
                val asignaturas = asignaturasDto.map { it.toAsignatura() }

                // Guardar asignaturas en Room
                if (asignaturas.isNotEmpty()) {
                    asignaturaDao.insertarVarias(asignaturas.map { it.toEntity(sincronizado = true) })

                    // Crear inscripciones en Room para cada asignatura
                    val inscripciones = asignaturas.map { asignatura ->
                        AlumnoAsignaturaEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            alumnoId = alumnoId,
                            asignaturaId = asignatura.id,
                            fechaInscripcion = java.time.OffsetDateTime.now().toString(),
                            estado = "activo",
                            sincronizado = true
                        )
                    }
                    alumnoAsignaturaDao.insertarVarias(inscripciones)
                    Log.d("SpringBootAlumno", "✅ ${inscripciones.size} inscripciones guardadas en Room")
                }

                Result.success(asignaturas)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAlumno", "❌ Error obteniendo asignaturas inscritas", e)
            Result.failure(e)
        }
    }

    suspend fun darDeBaja(alumnoId: String, asignaturaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.darDeBaja("Bearer ${TokenManager.getToken()}", asignaturaId)

            if (response.isSuccessful && response.body()?.success == true) {
                // Eliminar inscripción de Room
                val inscripcion = alumnoAsignaturaDao.obtenerInscripcion(alumnoId, asignaturaId)
                inscripcion?.let { alumnoAsignaturaDao.eliminarInscripcion(it) }

                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerInscripcionesAsignatura(asignaturaId: String): Result<List<AlumnoAsignatura>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerInscripciones("Bearer ${TokenManager.getToken()}", asignaturaId)

            if (response.isSuccessful && response.body()?.success == true) {
                val inscripcionesDto = response.body()!!.data!!
                val inscripciones = inscripcionesDto.map { it.toAlumnoAsignatura() }

                // Guardar inscripciones en Room
                if (inscripciones.isNotEmpty()) {
                    val inscripcionesEntity = inscripciones.map { inscripcion ->
                        AlumnoAsignaturaEntity(
                            id = inscripcion.id,
                            alumnoId = inscripcion.alumnoId,
                            asignaturaId = inscripcion.asignaturaId,
                            fechaInscripcion = inscripcion.fechaInscripcion,
                            estado = inscripcion.estado,
                            sincronizado = true
                        )
                    }
                    alumnoAsignaturaDao.insertarVarias(inscripcionesEntity)
                    Log.d("SpringBootAlumno", "✅ ${inscripcionesEntity.size} inscripciones guardadas en Room")
                }

                Result.success(inscripciones)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAlumno", "❌ Error obteniendo inscripciones", e)
            Result.failure(e)
        }
    }

    // Extensiones para mapeo
    private fun AsignaturaResponseDto.toAsignatura() = Asignatura(
        id = id,
        nombre = nombre,
        codigoAcceso = codigoAcceso,
        docenteId = docenteId,
        descripcion = descripcion,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Asignatura.toEntity(sincronizado: Boolean) = cl.duocuc.aulaviva.data.local.AsignaturaEntity(
        id = id,
        nombre = nombre,
        codigoAcceso = codigoAcceso,
        docenteId = docenteId,
        descripcion = descripcion,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sincronizado = sincronizado
    )

    private fun AlumnoAsignaturaResponseDto.toAlumnoAsignatura() = AlumnoAsignatura(
        id = id,
        alumnoId = alumnoId,
        asignaturaId = asignaturaId,
        fechaInscripcion = fechaInscripcion,
        estado = estado
    )
}

