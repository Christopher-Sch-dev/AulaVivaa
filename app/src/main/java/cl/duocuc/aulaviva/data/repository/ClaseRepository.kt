package cl.duocuc.aulaviva.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.data.supabase.SupabaseClaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository que maneja toda la lógica de clases.
 * Utiliza arquitectura offline-first con Supabase.
 *
 * Usa DOS fuentes de datos:
 * 1. Room (BD local) - Funciona sin internet
 * 2. Supabase Postgres + Storage - Se sincroniza cuando hay conexión
 *
 * ESTRATEGIA:
 * - Siempre leo primero de Room (rápido, offline)
 * - Intento sincronizar con Supabase en segundo plano
 * - Si no hay internet, guardo en Room con sincronizado=false
 * - Cuando vuelve internet, subo lo pendiente
 */
class ClaseRepository(context: Context) {

    private val uid: String get() = SupabaseAuthManager.getCurrentUserId() ?: ""

    // Referencia al DAO de Room (BD local)
    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val claseDao: ClaseDao = db.claseDao()

    // Referencia al repository de Supabase
    private val supabaseRepo = SupabaseClaseRepository(claseDao)

    private val appContext = context.applicationContext

    /**
     * Mapeo local Clase -> ClaseEntity controlando el flag de sincronización.
     */
    private fun Clase.toEntityLocal(sincronizado: Boolean): ClaseEntity = ClaseEntity(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        archivoPdfUrl = this.archivoPdfUrl,
        archivoPdfNombre = this.archivoPdfNombre,
        creador = this.creador,
        asignaturaId = this.asignaturaId,
        sincronizado = sincronizado
    )

    /**
     * Obtiene las clases del usuario desde Room (local).
     * Flow emite automáticamente cuando los datos cambian.
     */
    fun obtenerClasesLocal(): Flow<List<Clase>> {
        return claseDao.obtenerClasesPorUsuario(uid).map { entities ->
            entities.map { entity ->
                Clase(
                    id = entity.id,
                    nombre = entity.nombre,
                    descripcion = entity.descripcion,
                    fecha = entity.fecha,
                    archivoPdfUrl = entity.archivoPdfUrl,
                    archivoPdfNombre = entity.archivoPdfNombre,
                    creador = entity.creador,
                    asignaturaId = entity.asignaturaId
                )
            }
        }
    }

    /**
     * Obtiene clases de una asignatura específica (Flow para LiveData).
     */
    fun obtenerClasesPorAsignatura(asignaturaId: String): Flow<List<Clase>> {
        return claseDao.obtenerClasesPorAsignatura(asignaturaId).map { entities ->
            entities.map { entity ->
                Clase(
                    id = entity.id,
                    nombre = entity.nombre,
                    descripcion = entity.descripcion,
                    fecha = entity.fecha,
                    archivoPdfUrl = entity.archivoPdfUrl,
                    archivoPdfNombre = entity.archivoPdfNombre,
                    creador = entity.creador,
                    asignaturaId = entity.asignaturaId
                )
            }
        }
    }

    /**
     * Obtiene clases de múltiples asignaturas (para alumnos inscritos).
     */
    fun obtenerClasesPorAsignaturas(asignaturasIds: List<String>): Flow<List<Clase>> {
        if (asignaturasIds.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return claseDao.obtenerClasesPorAsignaturas(asignaturasIds).map { entities ->
            entities.map { entity ->
                Clase(
                    id = entity.id,
                    nombre = entity.nombre,
                    descripcion = entity.descripcion,
                    fecha = entity.fecha,
                    archivoPdfUrl = entity.archivoPdfUrl,
                    archivoPdfNombre = entity.archivoPdfNombre,
                    creador = entity.creador,
                    asignaturaId = entity.asignaturaId
                )
            }
        }
    }

    /**
     * Obtiene una clase específica por ID desde Room.
     */
    suspend fun obtenerClasePorId(claseId: String): Clase? {
        return try {
            val entity = claseDao.obtenerClasePorId(claseId)
            if (entity != null) {
                Clase(
                    id = entity.id,
                    nombre = entity.nombre,
                    descripcion = entity.descripcion,
                    fecha = entity.fecha,
                    archivoPdfUrl = entity.archivoPdfUrl,
                    archivoPdfNombre = entity.archivoPdfNombre,
                    creador = entity.creador,
                    asignaturaId = entity.asignaturaId
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sincroniza clases desde Supabase a Room.
     */
    suspend fun sincronizarDesdeSupabase() {
        try {
            // PASO 1: Subir clases pendientes desde Room a Supabase
            val clasesNoSincronizadas = claseDao.obtenerNoSincronizadas()
            Log.d(
                "ClaseRepository",
                "🔄 Intentando subir ${clasesNoSincronizadas.size} clases pendientes..."
            )
            clasesNoSincronizadas.forEach { claseEntity ->
                try {
                    val clase = Clase(
                        id = claseEntity.id,
                        nombre = claseEntity.nombre,
                        descripcion = claseEntity.descripcion,
                        fecha = claseEntity.fecha,
                        archivoPdfUrl = claseEntity.archivoPdfUrl,
                        archivoPdfNombre = claseEntity.archivoPdfNombre,
                        creador = claseEntity.creador
                    )
                    // Intentar crear/actualizar en Supabase
                    val result = if (supabaseRepo.obtenerClasePorId(clase.id).isSuccess) {
                        supabaseRepo.actualizarClase(clase)
                    } else {
                        supabaseRepo.crearClase(clase)
                    }
                    result.fold(
                        onSuccess = {
                            // Si se subió, marcar como sincronizada en Room
                            claseDao.insertarClase(claseEntity.copy(sincronizado = true))
                            Log.d("ClaseRepository", "✅ Clase ${clase.id} sincronizada a Supabase")
                        },
                        onFailure = { error ->
                            Log.e(
                                "ClaseRepository",
                                "❌ Error subiendo clase ${clase.id}: ${error.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e(
                        "ClaseRepository",
                        "❌ Excepción al procesar clase pendiente ${claseEntity.id}",
                        e
                    )
                }
            }

            // PASO 2: Descargar clases desde Supabase y actualizar Room
            val result = supabaseRepo.obtenerClases()
            result.fold(
                onSuccess = {
                    Log.d(
                        "ClaseRepository",
                        "✅ Sincronización exitosa: ${it.size} clases descargadas"
                    )
                    // El supabaseRepo.obtenerClases ya actualiza Room.
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error en sincronización de descarga", error)
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general sincronizando", e)
        }
    }

    /**
     * Sincroniza (descarga) clases para una asignatura específica.
     * Esto evita tocar otras clases locales y es seguro para vistas de alumnos.
     */
    suspend fun sincronizarClasesPorAsignatura(asignaturaId: String) {
        try {
            val result = supabaseRepo.obtenerClasesPorAsignatura(asignaturaId)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Sincronizadas ${it.size} clases para asignatura $asignaturaId")
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error sincronizando clases por asignatura", error)
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Excepción sincronizando por asignatura", e)
        }
    }

    /**
     * Crea una nueva clase.
     */
    suspend fun crearClase(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // El ID ya debe venir generado desde el ViewModel
            if (clase.id.isEmpty()) {
                onError("Error: La clase debe tener un ID válido")
                return
            }

            val result = supabaseRepo.crearClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase creada exitosamente en Supabase")
                    // Ya se guarda en Room dentro de supabaseRepo.crearClase
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error creando clase en Supabase: $error")
                    // Guardar localmente con sincronizado = false
                    claseDao.insertarClase(clase.toEntityLocal(false))
                    Log.d(
                        "ClaseRepository",
                        "💾 Clase guardada localmente por error en Supabase: ${clase.id}"
                    )
                    onSuccess() // Consideramos éxito local para la UI
                    // No llamamos onError aquí ya que la guardamos localmente
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general en crearClase: $e")
            // Guardar localmente con sincronizado = false si tiene ID válido
            if (clase.id.isNotEmpty()) {
                claseDao.insertarClase(clase.toEntityLocal(false))
                Log.d(
                    "ClaseRepository",
                    "💾 Clase guardada localmente por excepción: ${clase.id}"
                )
                onSuccess() // Consideramos éxito local para la UI
            } else {
                onError("Error: No se pudo crear la clase - ID inválido")
            }
        }
    }

    /**
     * Wrapper no-suspend para crearClase
     */
    fun crearClaseAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO) // Preferir pasar viewModelScope desde UI
    ) {
        scope.launch {
            crearClase(clase, onSuccess, onError)
        }
    }

    /**
     * Actualiza una clase existente.
     */
    suspend fun actualizarClase(
        claseId: String,
        nombre: String,
        descripcion: String,
        fecha: String,
        archivoPdfUrl: String,
        archivoPdfNombre: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val clase = Clase(
                id = claseId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = fecha,
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
                creador = uid
            )

            val result = supabaseRepo.actualizarClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase actualizada exitosamente en Supabase")
                    // Ya se guarda en Room dentro de supabaseRepo.actualizarClase
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error actualizando clase en Supabase: $error")
                    // Actualizar localmente con sincronizado = false
                    claseDao.actualizarClase(clase.toEntityLocal(false))
                    Log.d(
                        "ClaseRepository",
                        "💾 Clase actualizada localmente por error en Supabase: ${'$'}{clase.id}"
                    )
                    onSuccess() // Consideramos éxito local para la UI
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general en actualizarClase: $e")
            // Actualizar localmente con sincronizado = false
            val claseLocal = Clase(
                id = claseId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = fecha,
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
                creador = uid
            )
            claseDao.actualizarClase(claseLocal.toEntityLocal(false))
            Log.d(
                "ClaseRepository",
                "💾 Clase actualizada localmente por excepción: ${'$'}{claseLocal.id}"
            )
            onSuccess() // Consideramos éxito local para la UI
        }
    }

    /**
     * Método sobrecargado para actualizar clase usando objeto Clase
     */
    suspend fun actualizarClase(clase: Clase) {
        try {
            val result = supabaseRepo.actualizarClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase actualizada")
                    // Ya se guarda en Room dentro de supabaseRepo.actualizarClase
                },
                onFailure = { error ->
                    Log.e(
                        "ClaseRepository",
                        "❌ Error actualizando clase en Supabase (sobrecarga): $error"
                    )
                    // Actualizar localmente con sincronizado = false
                    claseDao.actualizarClase(clase.toEntityLocal(false))
                    Log.d(
                        "ClaseRepository",
                        "💾 Clase actualizada localmente por error en Supabase (sobrecarga): ${'$'}{clase.id}"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general en actualizarClase (sobrecarga): $e")
            val claseLocal = clase
            claseDao.actualizarClase(claseLocal.toEntityLocal(false))
            Log.d(
                "ClaseRepository",
                "💾 Clase actualizada localmente por excepción (sobrecarga): ${'$'}{claseLocal.id}"
            )
        }
    }

    /**
     * Elimina una clase.
     */
    suspend fun eliminarClase(
        claseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val result = supabaseRepo.eliminarClase(claseId)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase eliminada exitosamente de Supabase")
                    // Ya se elimina de Room dentro de supabaseRepo.eliminarClase
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error eliminando clase de Supabase: $error")
                    // Si falla la eliminación en Supabase, simplemente no la eliminamos localmente
                    // El usuario puede intentar de nuevo más tarde o manejarlo manualmente.
                    onError(error.message ?: "Error al eliminar clase en Supabase")
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general en eliminarClase: $e")
            onError(e.message ?: "Error al eliminar clase")
        }
    }

    /**
     * Método sobrecargado para eliminar clase usando solo el ID
     */
    suspend fun eliminarClase(claseId: String) {
        try {
            val result = supabaseRepo.eliminarClase(claseId)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase eliminada")
                    // Ya se elimina de Room dentro de supabaseRepo.eliminarClase
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error eliminando clase (sobrecarga): $error")
                    // Si falla la eliminación en Supabase, simplemente no la eliminamos localmente
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error general en eliminarClase (sobrecarga): $e")
        }
    }

    /**
     * Limpia todas las clases locales (útil al cerrar sesión)
     */
    suspend fun limpiarLocal() {
        claseDao.eliminarTodas()
    }

    /**
     * Crea una CLASE DE PRUEBA automáticamente
     */
    suspend fun crearClaseDePrueba(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val claseDemoId = "clase_demo_${'$'}{System.currentTimeMillis()}"
            val nombreArchivoDemo = "clase_demo.pdf"
            // TODO: Replace with actual Uri from assets, e.g., Uri.parse("android.resource://" + appContext.packageName + "/raw/clase_demo")
            val dummyAssetUri: Uri = Uri.EMPTY // Placeholder for asset Uri

            var pdfUrl: String = "https://www.bluebooksoft.com/DISENO_PROGRAMACION_WEB/1366.pdf"

            if (dummyAssetUri != Uri.EMPTY) {
                try {
                    pdfUrl = subirPdfASupabaseStorage(dummyAssetUri, nombreArchivoDemo)
                    Log.d(
                        "ClaseRepository",
                        "✅ PDF de demostración subido a Supabase Storage: $pdfUrl"
                    )
                } catch (e: Exception) {
                    Log.e(
                        "ClaseRepository",
                        "❌ Error subiendo PDF de demostración desde assets: ${'$'}{e.message}",
                        e
                    )
                    // Fallback to external URL if asset upload fails
                    pdfUrl = "https://www.bluebooksoft.com/DISENO_PROGRAMACION_WEB/1366.pdf"
                }
            }

            val claseDemo = Clase(
                id = claseDemoId,
                nombre = "Introducción a Desarrollo Android con Kotlin",
                descripcion = """
                    En esta clase exploraremos los fundamentos del desarrollo móvil Android utilizando Kotlin.

                    Actividades:
                    • Configuración del entorno de desarrollo
                    • Sintaxis básica de Kotlin
                    • Creación de la primera aplicación
                    • Arquitectura MVVM
                    • Integración con Supabase

                    Material incluido: Guía completa en PDF con ejemplos prácticos.
                """.trimIndent(),
                fecha = "Lunes 4 de Noviembre, 14:00hrs",
                archivoPdfUrl = pdfUrl,
                archivoPdfNombre = nombreArchivoDemo,
                creador = uid
            )

            val result = supabaseRepo.crearClase(claseDemo)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase de prueba creada")
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error creando clase de prueba", error)
                    onError(error.message ?: "Error al crear clase de prueba")
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error en crearClaseDePrueba", e)
            onError(e.message ?: "Error al crear clase de prueba")
        }
    }

    /**
     * Comprueba si una asignatura tiene clases asociadas.
     * Retorna `true` si existe al menos una clase para la asignatura.
     */
    suspend fun tieneClases(asignaturaId: String): Boolean {
        return try {
            val clases = claseDao.obtenerClasesPorAsignaturaDirecto(asignaturaId)
            clases.isNotEmpty()
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error verificando clases para $asignaturaId", e)
            // En caso de error, conservador: asumimos que sí tiene clases para evitar borrados accidentales
            true
        }
    }

    /**
     * Sube un PDF a Supabase Storage y retorna la URL pública.
     */
    suspend fun subirPdfASupabaseStorage(
        pdfUri: Uri,
        nombreArchivo: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d("ClaseRepository", "📤 Iniciando subida de PDF: $nombreArchivo")

            val result = supabaseRepo.subirPdf(appContext, pdfUri, nombreArchivo)

            result.fold(
                onSuccess = { url ->
                    Log.d("ClaseRepository", "✅ PDF subido exitosamente")
                    return@withContext url
                },
                onFailure = { error ->
                    Log.e("ClaseRepository", "❌ Error subiendo PDF", error)
                    throw Exception(error.message ?: "Error subiendo PDF")
                }
            )
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error en subirPdfASupabaseStorage", e)
            throw Exception("Error subiendo PDF: ${'$'}{e.message}")
        }
    }
}
