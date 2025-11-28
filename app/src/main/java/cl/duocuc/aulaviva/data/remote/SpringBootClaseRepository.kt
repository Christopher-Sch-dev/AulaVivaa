package cl.duocuc.aulaviva.data.remote

import android.util.Log
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository remoto para clases usando Spring Boot.
 * Reemplaza a SupabaseClaseRepository.
 */
class SpringBootClaseRepository(
    private val claseDao: ClaseDao,
    private val apiService: SpringBootApiService
) {

    suspend fun crearClase(clase: Clase): Result<Clase> = withContext(Dispatchers.IO) {
        try {
            val request = CrearClaseRequestDto(
                nombre = clase.nombre,
                descripcion = clase.descripcion,
                fecha = clase.fecha.ifEmpty { System.currentTimeMillis().toString() },
                archivoPdfUrl = clase.archivoPdfUrl,
                archivoPdfNombre = clase.archivoPdfNombre,
                asignaturaId = clase.asignaturaId
            )

            val response = apiService.crearClase("Bearer ${TokenManager.getToken()}", request)

            if (response.isSuccessful && response.body()?.success == true) {
                val claseDto = response.body()!!.data!!
                val claseCreada = claseDto.toClase()

                // Sincronizar a Room
                claseDao.insertarClase(claseCreada.toEntity(sincronizado = true))

                Log.d("SpringBootClase", "✅ Clase creada: ${claseCreada.id}")
                Result.success(claseCreada)
            } else {
                val error = response.body()?.error ?: response.message()
                Log.e("SpringBootClase", "❌ Error creando clase: $error")
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootClase", "❌ Excepción creando clase", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerClases(): Result<List<Clase>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerClases("Bearer ${TokenManager.getToken()}", null)

            if (response.isSuccessful && response.body()?.success == true) {
                val clasesDto = response.body()!!.data!!
                val clases = clasesDto.map { it.toClase() }

                // Sincronizar a Room
                if (clases.isNotEmpty()) {
                    claseDao.insertarVarias(clases.map { it.toEntity(sincronizado = true) })
                }

                Log.d("SpringBootClase", "✅ ${clases.size} clases obtenidas")
                Result.success(clases)
            } else {
                val error = response.body()?.error ?: response.message()
                Log.e("SpringBootClase", "❌ Error obteniendo clases: $error")
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("SpringBootClase", "❌ Excepción obteniendo clases", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerClasesPorAsignatura(asignaturaId: String): Result<List<Clase>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerClases("Bearer ${TokenManager.getToken()}", asignaturaId)

            if (response.isSuccessful && response.body()?.success == true) {
                val clasesDto = response.body()!!.data!!
                val clases = clasesDto.map { it.toClase() }

                // Sincronizar a Room
                if (clases.isNotEmpty()) {
                    claseDao.insertarVarias(clases.map { it.toEntity(sincronizado = true) })
                }

                Result.success(clases)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarClase(clase: Clase): Result<Clase> = withContext(Dispatchers.IO) {
        try {
            val request = ActualizarClaseRequestDto(
                nombre = clase.nombre,
                descripcion = clase.descripcion,
                fecha = clase.fecha,
                archivoPdfUrl = clase.archivoPdfUrl,
                archivoPdfNombre = clase.archivoPdfNombre
            )

            val response = apiService.actualizarClase("Bearer ${TokenManager.getToken()}", clase.id, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val claseDto = response.body()!!.data!!
                val claseActualizada = claseDto.toClase()

                // Sincronizar a Room
                claseDao.insertarClase(claseActualizada.toEntity(sincronizado = true))

                Result.success(claseActualizada)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarClase(claseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarClase("Bearer ${TokenManager.getToken()}", claseId)

            if (response.isSuccessful && response.body()?.success == true) {
                // Eliminar de Room
                claseDao.eliminarClase(claseId)
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: response.message()
                Result.failure(Exception(error ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extensiones para mapeo
    private fun ClaseResponseDto.toClase() = Clase(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        fecha = fecha,
        archivoPdfUrl = archivoPdfUrl,
        archivoPdfNombre = archivoPdfNombre,
        creador = creador,
        asignaturaId = asignaturaId
    )

    private fun Clase.toEntity(sincronizado: Boolean) = cl.duocuc.aulaviva.data.local.ClaseEntity(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        fecha = fecha,
        archivoPdfUrl = archivoPdfUrl,
        archivoPdfNombre = archivoPdfNombre,
        creador = creador,
        asignaturaId = asignaturaId,
        sincronizado = sincronizado
    )
}

