package cl.duocuc.aulaviva.data.supabase

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.local.AsignaturaEntity
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.data.supabase.AlumnoAsignaturaSupabaseDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository Supabase para asignaturas.
 * Maneja CRUD en PostgreSQL y sincronización con Room.
 *
 * Implementa arquitectura offline-first.
 */
class SupabaseAsignaturaRepository(
    private val asignaturaDao: AsignaturaDao,
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao
) {

    /**
     * Crear asignatura en Supabase.
     */
    suspend fun crearAsignatura(asignatura: Asignatura): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "📚 Creando asignatura: ${asignatura.nombre}")

            val dto = AsignaturaSupabaseDto(
                nombre = asignatura.nombre,
                codigoAcceso = asignatura.codigoAcceso,
                docenteId = asignatura.docenteId,
                descripcion = asignatura.descripcion
            )

            val result = supabase.from("asignaturas")
                .insert(dto) {
                    select(Columns.ALL)
                }
                .decodeSingle<AsignaturaSupabaseDto>()

            val asignaturaCreada = Asignatura(
                id = result.id,
                nombre = result.nombre,
                codigoAcceso = result.codigoAcceso,
                docenteId = result.docenteId,
                descripcion = result.descripcion,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt
            )

            // Guardar en Room
            asignaturaDao.insertarAsignatura(asignaturaCreada.toEntity(sincronizado = true))

            Log.d("SupabaseAsignatura", "✅ Asignatura creada: ${asignaturaCreada.id}")
            Result.success(asignaturaCreada)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error creando asignatura", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener asignaturas de un docente desde Supabase.
     */
    suspend fun obtenerAsignaturasDocente(docenteId: String): Result<List<Asignatura>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "📚 Obteniendo asignaturas del docente: $docenteId")

            val results = supabase.from("asignaturas")
                .select {
                    filter {
                        eq("docente_id", docenteId)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<AsignaturaSupabaseDto>()

            val asignaturas = results.map { dto ->
                Asignatura(
                    id = dto.id,
                    nombre = dto.nombre,
                    codigoAcceso = dto.codigoAcceso,
                    docenteId = dto.docenteId,
                    descripcion = dto.descripcion,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
            }

            // Sincronizar con Room
            asignaturaDao.insertarVarias(asignaturas.map { it.toEntity(sincronizado = true) })

            Log.d("SupabaseAsignatura", "✅ ${asignaturas.size} asignaturas obtenidas")
            Result.success(asignaturas)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error obteniendo asignaturas", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los alumnos inscritos en una asignatura desde Supabase.
     */
    suspend fun obtenerInscritosPorAsignatura(asignaturaId: String): Result<List<AlumnoAsignaturaEntity>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "👥 Obteniendo inscritos para: $asignaturaId")

            val results = supabase.from("alumno_asignaturas")
                .select {
                    filter {
                        eq("asignatura_id", asignaturaId)
                    }
                }
                .decodeList<AlumnoAsignaturaSupabaseDto>()

            val inscripciones = results.map { dto ->
                dto.toEntity(sincronizado = true)
            }

            // Guardar en Room
            if (inscripciones.isNotEmpty()) {
                alumnoAsignaturaDao.insertarVarias(inscripciones)
            }

            Log.d("SupabaseAsignatura", "✅ ${inscripciones.size} inscritos obtenidos")
            Result.success(inscripciones)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error obteniendo inscritos", e)
            Result.failure(e)
        }
    }

    /**
     * Actualizar asignatura en Supabase.
     */
    suspend fun actualizarAsignatura(asignatura: Asignatura): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "✏️ Actualizando asignatura: ${asignatura.id}")

            val dto = AsignaturaSupabaseDto(
                id = asignatura.id,
                nombre = asignatura.nombre,
                codigoAcceso = asignatura.codigoAcceso,
                docenteId = asignatura.docenteId,
                descripcion = asignatura.descripcion
            )

            supabase.from("asignaturas")
                .update(dto) {
                    filter {
                        eq("id", asignatura.id)
                    }
                }

            // Actualizar en Room
            asignaturaDao.actualizarAsignatura(asignatura.toEntity(sincronizado = true))

            Log.d("SupabaseAsignatura", "✅ Asignatura actualizada")
            Result.success(asignatura)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error actualizando asignatura", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar asignatura en Supabase.
     */
    suspend fun eliminarAsignatura(asignaturaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "🗑️ Eliminando asignatura: $asignaturaId")

            supabase.from("asignaturas")
                .delete {
                    filter {
                        eq("id", asignaturaId)
                    }
                }

            // Eliminar de Room
            val entity = asignaturaDao.obtenerAsignaturaPorId(asignaturaId)
            entity?.let { asignaturaDao.eliminarAsignatura(it) }

            Log.d("SupabaseAsignatura", "✅ Asignatura eliminada")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error eliminando asignatura", e)
            Result.failure(e)
        }
    }

    /**
     * Generar código único para asignatura.
     * Genera localmente y actualiza en Supabase.
     */
    suspend fun generarCodigo(asignaturaId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "🔑 Generando código para: $asignaturaId")

            val request = GenerarCodigoRequest(asignaturaId)
            val codigo = supabase.postgrest.rpc("generar_codigo_asignatura", request).decodeAs<String>()

            Log.d("SupabaseAsignatura", "✅ Código generado: $codigo")
            Result.success(codigo)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error generando código", e)
            Result.failure(e)
        }
    }
}

/**
 * Extensiones para mapeo.
 */
private fun Asignatura.toEntity(sincronizado: Boolean = false) = AsignaturaEntity(
    id = id,
    nombre = nombre,
    codigoAcceso = codigoAcceso,
    docenteId = docenteId,
    descripcion = descripcion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sincronizado = sincronizado
)

private fun AlumnoAsignaturaSupabaseDto.toEntity(sincronizado: Boolean = false) = AlumnoAsignaturaEntity(
    id = id,
    alumnoId = alumnoId,
    asignaturaId = asignaturaId,
    fechaInscripcion = fechaInscripcion,
    estado = estado,
    sincronizado = sincronizado
)
