package cl.duocuc.aulaviva.data.supabase

import android.app.Application
import android.net.Uri
import android.util.Log
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import io.github.jan.supabase.postgrest.from
import cl.duocuc.aulaviva.data.repository.StorageRepository
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Repository Supabase para clases.
 * Maneja CRUD en Postgres + Storage para PDFs.
 * Sincroniza con Room (caché local).
 *
 * Implementa arquitectura offline-first con Supabase.
 */
class SupabaseClaseRepository(private val claseDao: ClaseDao) {

    // ============ CLASES CRUD ============

    /**
     * Crear clase en Supabase Postgres.
     */
    suspend fun crearClase(clase: Clase): Result<Clase> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()
            val uidDocente = SupabaseAuthManager.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

            val claseConCreador = clase.copy(
                creador = uidDocente,
                fecha = clase.fecha.ifEmpty { System.currentTimeMillis().toString() }
            )

            Log.d("SupabaseRepo", "➕ Creando clase: ${clase.nombre}")

            // Convertir a DTO para Supabase
            val dto = ClaseDTO(
                id = claseConCreador.id,
                nombre = claseConCreador.nombre,
                descripcion = claseConCreador.descripcion,
                fecha = claseConCreador.fecha,
                archivo_pdf_url = claseConCreador.archivoPdfUrl,
                archivo_pdf_nombre = claseConCreador.archivoPdfNombre,
                creador = claseConCreador.creador,
                asignatura_id = claseConCreador.asignaturaId
            )

            // Insertar en Postgres
            supabase.from("clases").insert(dto)

            // Sincronizar a Room
            claseDao.insertarClase(claseConCreador.toEntity())

            Log.d("SupabaseRepo", "✅ Clase creada: ${claseConCreador.id}")
            Result.success(claseConCreador)

        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error creando clase", e)
            Result.failure(Exception("Error al crear clase: ${e.message}"))
        }
    }

    /**
     * Obtener todas las clases del docente actual.
     */
    suspend fun obtenerClases(): Result<List<Clase>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()
            val uidDocente = SupabaseAuthManager.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Usuario no autenticado"))

            Log.d("SupabaseRepo", "🔍 Obteniendo clases de usuario: ${uidDocente.take(8)}...")

            val response = supabase.from("clases")
                .select()
                .decodeList<ClaseDTO>()

            // Filtrar por creador (por si RLS no está configurado)
            val clasesFiltradas = response.filter { it.creador == uidDocente }

            val clases = clasesFiltradas.map { it.toClase() }

            // Sincronizar a Room: insertar/actualizar (onConflict=REPLACE via DAO)
            // Evitamos eliminar todas las clases localmente para no provocar vacíos
            // en caso de que la descarga falle o el alcance sea distinto (alumno vs docente).
            if (clases.isNotEmpty()) {
                claseDao.insertarVarias(clases.map { it.toEntity() })
            } else {
                // Si la respuesta filtrada quedó vacía, no borramos la cache local.
                Log.w("SupabaseRepo", "⚠️ Respuesta vacía tras filtrar por creador: no se modifica cache local")
            }

            Log.d("SupabaseRepo", "✅ ${clases.size} clases obtenidas")
            Result.success(clases)

        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error obteniendo clases", e)
            // Fallback seguro: devolver todas las clases locales para no quedar en vacío
            return@withContext try {
                val entidades = claseDao.obtenerTodasLasClases().first()
                val todas = entidades.map { it.toClase() }
                Log.w("SupabaseRepo", "⚠️ Usando cache local: ${todas.size} clases")
                Result.success(todas)
            } catch (localError: Exception) {
                Log.e("SupabaseRepo", "❌ Error fallback obteniendo cache local", localError)
                Result.failure(Exception("Error al obtener clases: ${e.message}"))
            }
        }
    }

    /**
     * Obtener clases filtradas por `asignatura_id` (útil para alumnos).
     * Descarga solo las clases de la asignatura y las persiste en Room sin tocar otras entradas.
     */
    suspend fun obtenerClasesPorAsignatura(asignaturaId: String): Result<List<Clase>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseRepo", "🔍 Obteniendo clases de asignatura: $asignaturaId")

            val response = supabase.from("clases")
                .select {
                    filter {
                        eq("asignatura_id", asignaturaId)
                    }
                }
                .decodeList<ClaseDTO>()

            val clases = response.map { it.toClase() }

            // Insertar/actualizar sólo las clases de esta asignatura
            if (clases.isNotEmpty()) {
                claseDao.insertarVarias(clases.map { it.toEntity() })
            } else {
                Log.w("SupabaseRepo", "⚠️ No se encontraron clases para asignatura: $asignaturaId")
            }

            Result.success(clases)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error obteniendo clases por asignatura", e)
            // Fallback: devolver las clases locales de esa asignatura
            return@withContext try {
                val locales = claseDao.obtenerClasesPorAsignaturaDirecto(asignaturaId).map { it.toClase() }
                Log.w("SupabaseRepo", "⚠️ Usando cache local para asignatura $asignaturaId: ${locales.size} clases")
                Result.success(locales)
            } catch (localError: Exception) {
                Log.e("SupabaseRepo", "❌ Error fallback locales por asignatura", localError)
                Result.failure(Exception("Error al obtener clases por asignatura: ${e.message}"))
            }
        }
    }

    /**
     * Obtener clase por ID.
     */
    suspend fun obtenerClasePorId(claseId: String): Result<Clase> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseRepo", "🔍 Obteniendo clase: $claseId")

            val response = supabase.from("clases")
                .select {
                    filter {
                        eq("id", claseId)
                    }
                }
                .decodeSingle<ClaseDTO>()

            val clase = response.toClase()

            // Actualizar en Room
            claseDao.insertarClase(clase.toEntity())

            Log.d("SupabaseRepo", "✅ Clase obtenida: ${clase.nombre}")
            Result.success(clase)

        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error obteniendo clase", e)
            // Fallback a Room
            try {
                val claseLocal = claseDao.obtenerClasePorId(claseId)?.toClase()
                if (claseLocal != null) {
                    Result.success(claseLocal)
                } else {
                    Result.failure(Exception("Clase no encontrada"))
                }
            } catch (localError: Exception) {
                Result.failure(Exception("Error al obtener clase: ${e.message}"))
            }
        }
    }

    /**
     * Actualizar clase.
     */
    suspend fun actualizarClase(clase: Clase): Result<Clase> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseRepo", "✏️ Actualizando clase: ${clase.id}")

            val dto = ClaseDTO(
                id = clase.id,
                nombre = clase.nombre,
                descripcion = clase.descripcion,
                fecha = clase.fecha,
                archivo_pdf_url = clase.archivoPdfUrl,
                archivo_pdf_nombre = clase.archivoPdfNombre,
                creador = clase.creador
            )

            supabase.from("clases").update(dto) {
                filter {
                    eq("id", clase.id)
                }
            }

            claseDao.actualizarClase(clase.toEntity())

            Log.d("SupabaseRepo", "✅ Clase actualizada")
            Result.success(clase)

        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error actualizando clase", e)
            Result.failure(Exception("Error al actualizar clase: ${e.message}"))
        }
    }

    /**
     * Eliminar clase.
     */
    suspend fun eliminarClase(claseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseRepo", "🗑️ Eliminando clase: $claseId")

            supabase.from("clases").delete {
                filter {
                    eq("id", claseId)
                }
            }

            // Eliminar de Room
            val entity = claseDao.obtenerClasePorId(claseId)
            if (entity != null) {
                claseDao.eliminarClase(entity)
            }

            Log.d("SupabaseRepo", "✅ Clase eliminada")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error eliminando clase", e)
            Result.failure(Exception("Error al eliminar clase: ${e.message}"))
        }
    }

    // ============ STORAGE (PDFs) ============

    /**
     * Subir PDF a Supabase Storage.
     * Retorna URL pública del archivo.
     */
    suspend fun subirPdf(
        application: Application,
        pdfUri: Uri,
        nombreArchivo: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Delegar la subida a `StorageRepository` para centralizar la lógica de Storage
        try {
            val storageRepo = RepositoryProvider.provideStorageRepository(application)
            return@withContext storageRepo.subirPdf(pdfUri, nombreArchivo)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "❌ Error delegando subida a StorageRepository", e)
            Result.failure(Exception("Error al subir PDF: ${e.message}"))
        }
    }
}

// ============ DTOs para Supabase ============

/**
 * DTO para serialización JSON con Supabase.
 * Los nombres de campos coinciden con la tabla "clases" en Postgres.
 */
@Serializable
data class ClaseDTO(
    val id: String,
    val nombre: String,
    val descripcion: String = "",
    val fecha: String,
    val archivo_pdf_url: String = "",
    val archivo_pdf_nombre: String = "",
    val creador: String,
    val asignatura_id: String? = null
)

// ============ Extensiones de mapeo ============

fun ClaseDTO.toClase(): Clase {
    return Clase(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        archivoPdfUrl = this.archivo_pdf_url,
        archivoPdfNombre = this.archivo_pdf_nombre,
        creador = this.creador,
        asignaturaId = this.asignatura_id ?: ""  // Convertir null a vacío
    )
}

fun Clase.toEntity(): ClaseEntity {
    return ClaseEntity(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        archivoPdfUrl = this.archivoPdfUrl,
        archivoPdfNombre = this.archivoPdfNombre,
        creador = this.creador,
        asignaturaId = this.asignaturaId,
        sincronizado = true
    )
}

fun ClaseEntity.toClase(): Clase {
    return Clase(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        archivoPdfUrl = this.archivoPdfUrl,
        archivoPdfNombre = this.archivoPdfNombre,
        creador = this.creador
    )
}
