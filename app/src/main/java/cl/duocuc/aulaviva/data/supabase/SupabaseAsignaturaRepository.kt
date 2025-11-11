package cl.duocuc.aulaviva.data.supabase

import android.util.Log
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.local.AsignaturaEntity
import cl.duocuc.aulaviva.data.model.Asignatura
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository Supabase para asignaturas.
 * Maneja CRUD en PostgreSQL y sincronización con Room.
 *
 * Implementa arquitectura offline-first.
 */
class SupabaseAsignaturaRepository(private val asignaturaDao: AsignaturaDao) {

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
     * Generar código único para asignatura usando RPC.
     */
    suspend fun generarCodigo(asignaturaId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAsignatura", "🔑 Generando código para asignatura: $asignaturaId")

            val codigo = supabase.postgrest["rpc"].rpc("rpc_generar_codigo_asignatura", GenerarCodigoRequest(asignaturaId))
                .decodeAs<String>()            // Actualizar código en Room
            val entity = asignaturaDao.obtenerAsignaturaPorId(asignaturaId)
            entity?.let {
                asignaturaDao.actualizarAsignatura(it.copy(codigoAcceso = codigo))
            }

            Log.d("SupabaseAsignatura", "✅ Código generado: $codigo")
            Result.success(codigo)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error generando código", e)
            Result.failure(e)
        }
    }

    /**
     * Sincronizar asignaturas no sincronizadas desde Room a Supabase.
     */
    suspend fun sincronizarNoSincronizadas(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val noSincronizadas = asignaturaDao.obtenerNoSincronizadas()

            if (noSincronizadas.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            Log.d("SupabaseAsignatura", "🔄 Sincronizando ${noSincronizadas.size} asignaturas")

            noSincronizadas.forEach { entity ->
                val asignatura = entity.toModel()
                crearAsignatura(asignatura)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SupabaseAsignatura", "❌ Error sincronizando", e)
            Result.failure(e)
        }
    }
}

/**
 * Extensiones para mapeo entre modelos.
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

private fun AsignaturaEntity.toModel() = Asignatura(
    id = id,
    nombre = nombre,
    codigoAcceso = codigoAcceso,
    docenteId = docenteId,
    descripcion = descripcion,
    createdAt = createdAt,
    updatedAt = updatedAt
)
