package cl.duocuc.aulaviva.data.remote

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository remoto para asignaturas usando Spring Boot.
 * Reemplaza a SupabaseAsignaturaRepository.
 */
class SpringBootAsignaturaRepository(
    private val asignaturaDao: AsignaturaDao,
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao,
    private val apiService: SpringBootApiService
) {

    suspend fun crearAsignatura(asignatura: Asignatura): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val request = CrearAsignaturaRequestDto(
                nombre = asignatura.nombre,
                descripcion = asignatura.descripcion
            )

            val response = apiService.crearAsignatura("Bearer ${TokenManager.getToken()}", request)

            if (response.isSuccessful && response.body()?.success == true) {
                val asignaturaDto = response.body()!!.data!!
                val asignaturaCreada = asignaturaDto.toAsignatura()

                // Guardar en Room
                asignaturaDao.insertarAsignatura(asignaturaCreada.toEntity(sincronizado = true))

                Log.d("SpringBootAsignatura", "✅ Asignatura creada: ${asignaturaCreada.id}")
                Result.success(asignaturaCreada)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootAsignatura", "❌ Error creando asignatura", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerAsignaturasDocente(docenteId: String): Result<List<Asignatura>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerAsignaturas("Bearer ${TokenManager.getToken()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val asignaturasDto = response.body()!!.data!!
                val asignaturas = asignaturasDto.map { it.toAsignatura() }

                // Guardar en Room
                if (asignaturas.isNotEmpty()) {
                    asignaturaDao.insertarVarias(asignaturas.map { it.toEntity(sincronizado = true) })
                }

                Result.success(asignaturas)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarAsignatura(asignatura: Asignatura): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val request = ActualizarAsignaturaRequestDto(
                nombre = asignatura.nombre,
                descripcion = asignatura.descripcion
            )

            val response = apiService.actualizarAsignatura("Bearer ${TokenManager.getToken()}", asignatura.id, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val asignaturaDto = response.body()!!.data!!
                val asignaturaActualizada = asignaturaDto.toAsignatura()

                // Actualizar en Room
                asignaturaDao.insertarAsignatura(asignaturaActualizada.toEntity(sincronizado = true))

                Result.success(asignaturaActualizada)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAsignatura(asignaturaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarAsignatura("Bearer ${TokenManager.getToken()}", asignaturaId)

            if (response.isSuccessful && response.body()?.success == true) {
                // Eliminar de Room
                asignaturaDao.eliminarAsignatura(asignaturaId)
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generarCodigo(asignaturaId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.generarCodigo("Bearer ${TokenManager.getToken()}", asignaturaId)

            if (response.isSuccessful && response.body()?.success == true) {
                val codigo = response.body()!!.data!!.codigo
                Result.success(codigo)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
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
}

