package cl.duocuc.aulaviva.data.supabase

import android.util.Log
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaDao
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import cl.duocuc.aulaviva.data.local.AsignaturaDao
import cl.duocuc.aulaviva.data.model.AlumnoAsignatura
import cl.duocuc.aulaviva.data.model.Asignatura
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository Supabase para inscripciones de alumnos.
 * Maneja inscripción con código y gestión de asignaturas inscritas.
 */
class SupabaseAlumnoRepository(
    private val alumnoAsignaturaDao: AlumnoAsignaturaDao,
    private val asignaturaDao: AsignaturaDao
) {

    /**
     * Inscribirse en asignatura con código usando RPC.
     */
    suspend fun inscribirConCodigo(codigo: String): Result<Asignatura> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAlumno", "🎓 Inscribiendo con código: $codigo")

            val response = supabase.postgrest["rpc"].rpc(
                "rpc_inscribir_con_codigo",
                InscribirConCodigoRequest(codigo.uppercase().trim())
            ).decodeAs<InscripcionResponse>()

            if (!response.success) {
                Log.e("SupabaseAlumno", "❌ Error: ${response.error}")
                return@withContext Result.failure(Exception(response.error ?: "Error desconocido"))
            }

            val asignaturaDto = response.asignatura ?: run {
                return@withContext Result.failure(Exception("Asignatura no encontrada"))
            }

            // Crear modelo de asignatura
            val asignatura = Asignatura(
                id = asignaturaDto.id,
                nombre = asignaturaDto.nombre,
                descripcion = asignaturaDto.descripcion,
                docenteId = asignaturaDto.docenteId,
                codigoAcceso = codigo
            )

            // Guardar asignatura en Room
            asignaturaDao.insertarAsignatura(asignatura.toEntity(sincronizado = true))

            Log.d("SupabaseAlumno", "✅ Inscrito en: ${asignatura.nombre}")
            Result.success(asignatura)

        } catch (e: Exception) {
            Log.e("SupabaseAlumno", "❌ Error en inscripción", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener asignaturas inscritas del alumno desde Supabase.
     */
    suspend fun obtenerAsignaturasInscritas(alumnoId: String): Result<List<Asignatura>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAlumno", "📚 Obteniendo asignaturas inscritas de: $alumnoId")

            // Obtener inscripciones
            val inscripciones = supabase.from("alumno_asignaturas")
                .select {
                    filter {
                        eq("alumno_id", alumnoId)
                        eq("estado", "activo")
                    }
                    order(column = "fecha_inscripcion", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<AlumnoAsignaturaSupabaseDto>()

            // Guardar inscripciones en Room
            alumnoAsignaturaDao.insertarVarias(inscripciones.map { it.toEntity(sincronizado = true) })

            // Obtener asignaturas correspondientes
            val asignaturasIds = inscripciones.map { it.asignaturaId }

            if (asignaturasIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val asignaturas = supabase.from("asignaturas")
                .select {
                    filter {
                        isIn("id", asignaturasIds)
                    }
                }
                .decodeList<AsignaturaSupabaseDto>()
                .map { dto ->
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

            // Guardar asignaturas en Room
            asignaturaDao.insertarVarias(asignaturas.map { it.toEntity(sincronizado = true) })

            Log.d("SupabaseAlumno", "✅ ${asignaturas.size} asignaturas inscritas")
            Result.success(asignaturas)

        } catch (e: Exception) {
            Log.e("SupabaseAlumno", "❌ Error obteniendo asignaturas inscritas", e)
            Result.failure(e)
        }
    }

    /**
     * Darse de baja de una asignatura.
     */
    suspend fun darDeBaja(alumnoId: String, asignaturaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAlumno", "🚪 Dando de baja: $asignaturaId")

            supabase.from("alumno_asignaturas")
                .delete {
                    filter {
                        eq("alumno_id", alumnoId)
                        eq("asignatura_id", asignaturaId)
                    }
                }

            // Eliminar de Room
            val inscripcion = alumnoAsignaturaDao.obtenerInscripcion(alumnoId, asignaturaId)
            inscripcion?.let { alumnoAsignaturaDao.eliminarInscripcion(it) }

            Log.d("SupabaseAlumno", "✅ Baja exitosa")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SupabaseAlumno", "❌ Error dando de baja", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener inscripciones de una asignatura (para docentes).
     */
    suspend fun obtenerInscripcionesAsignatura(asignaturaId: String): Result<List<AlumnoAsignatura>> = withContext(Dispatchers.IO) {
        try {
            val supabase = SupabaseClientProvider.getClient()

            Log.d("SupabaseAlumno", "👥 Obteniendo inscripciones de: $asignaturaId")

            val inscripciones = supabase.from("alumno_asignaturas")
                .select {
                    filter {
                        eq("asignatura_id", asignaturaId)
                    }
                    order(column = "fecha_inscripcion", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<AlumnoAsignaturaSupabaseDto>()
                .map { dto ->
                    AlumnoAsignatura(
                        id = dto.id,
                        alumnoId = dto.alumnoId,
                        asignaturaId = dto.asignaturaId,
                        fechaInscripcion = dto.fechaInscripcion,
                        estado = dto.estado
                    )
                }

            Log.d("SupabaseAlumno", "✅ ${inscripciones.size} inscripciones")
            Result.success(inscripciones)

        } catch (e: Exception) {
            Log.e("SupabaseAlumno", "❌ Error obteniendo inscripciones", e)
            Result.failure(e)
        }
    }
}

/**
 * Extensiones para mapeo.
 */
private fun AlumnoAsignaturaSupabaseDto.toEntity(sincronizado: Boolean = false) = AlumnoAsignaturaEntity(
    id = id,
    alumnoId = alumnoId,
    asignaturaId = asignaturaId,
    fechaInscripcion = fechaInscripcion,
    estado = estado,
    sincronizado = sincronizado
)

private fun Asignatura.toEntity(sincronizado: Boolean = false) = cl.duocuc.aulaviva.data.local.AsignaturaEntity(
    id = id,
    nombre = nombre,
    codigoAcceso = codigoAcceso,
    docenteId = docenteId,
    descripcion = descripcion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sincronizado = sincronizado
)
