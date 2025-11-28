package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.domain.repository.IClaseRepository

import android.app.Application
import android.net.Uri
import android.util.Log
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.remote.SpringBootAuthRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClaseRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.data.remote.TokenManager
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
class ClaseRepository(private val application: Application) : IClaseRepository {

    private val uid: String get() = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
        cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
    ) ?: ""

    // Referencia al DAO de Room (BD local)
    private val db = AppDatabase.getDatabase(application)
    private val claseDao: ClaseDao = db.claseDao()

    // Referencia al repository de Spring Boot (migrado de Supabase)
    private val springBootRepo = SpringBootClaseRepository(claseDao, SpringBootClient.apiService)

    private val appContext = application.applicationContext

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
    override fun obtenerClasesLocal(): Flow<List<Clase>> {
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
    override fun obtenerClasesPorAsignatura(asignaturaId: String): Flow<List<Clase>> {
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
    override fun obtenerClasesPorAsignaturas(asignaturasIds: List<String>): Flow<List<Clase>> {
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
    override suspend fun obtenerClasePorId(claseId: String): Clase? {
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
     * Sincroniza clases desde Spring Boot a Room.
     */
    override suspend fun sincronizarDesdeSupabase() {
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
                        creador = claseEntity.creador,
                        asignaturaId = claseEntity.asignaturaId
                    )
                    // Intentar crear/actualizar en Spring Boot
                    val existe = springBootRepo.obtenerClasePorId(clase.id).getOrNull()
                    val result = if (existe != null) {
                        springBootRepo.actualizarClase(clase)
                    } else {
                        springBootRepo.crearClase(clase)
                    }
                    result.fold(
                        onSuccess = {
                            // Si se subió, marcar como sincronizada en Room
                            claseDao.insertarClase(claseEntity.copy(sincronizado = true))
                            Log.d("ClaseRepository", "✅ Clase ${clase.id} sincronizada a Spring Boot")
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

            // PASO 2: Descargar clases desde Spring Boot y actualizar Room
            val result = springBootRepo.obtenerClases()
            result.fold(
                onSuccess = {
                    Log.d(
                        "ClaseRepository",
                        "✅ Sincronización exitosa: ${it.size} clases descargadas"
                    )
                    // El springBootRepo.obtenerClases ya actualiza Room.
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
    override suspend fun sincronizarClasesPorAsignatura(asignaturaId: String) {
        try {
            val result = springBootRepo.obtenerClasesPorAsignatura(asignaturaId)
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
    override suspend fun crearClase(
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

            val result = springBootRepo.crearClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase creada exitosamente en Spring Boot")
                    // Ya se guarda en Room dentro de springBootRepo.crearClase
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
    override fun crearClaseAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        scope.launch {
            crearClase(clase, onSuccess, onError)
        }
    }

    /**
     * Actualiza una clase existente.
     */
    override suspend fun actualizarClase(
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
            // Obtener asignaturaId desde la clase existente si existe
            val claseExistente = claseDao.obtenerClasePorId(claseId)
            val asignaturaId = claseExistente?.asignaturaId ?: ""

            val clase = Clase(
                id = claseId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = fecha,
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
                creador = uid,
                asignaturaId = asignaturaId
            )

            val result = springBootRepo.actualizarClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase actualizada exitosamente en Supabase")
                    // Ya se guarda en Room dentro de springBootRepo.actualizarClase
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
            val claseExistente = claseDao.obtenerClasePorId(claseId)
            val asignaturaId = claseExistente?.asignaturaId ?: ""

            val claseLocal = Clase(
                id = claseId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = fecha,
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
                creador = uid,
                asignaturaId = asignaturaId
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
    override suspend fun actualizarClase(clase: Clase) {
        try {
            val result = springBootRepo.actualizarClase(clase)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase actualizada")
                    // Ya se guarda en Room dentro de springBootRepo.actualizarClase
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
    override suspend fun eliminarClase(
        claseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val result = springBootRepo.eliminarClase(claseId)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase eliminada exitosamente de Supabase")
                    // Ya se elimina de Room dentro de springBootRepo.eliminarClase
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
    override suspend fun eliminarClase(claseId: String) {
        try {
            val result = springBootRepo.eliminarClase(claseId)
            result.fold(
                onSuccess = {
                    Log.d("ClaseRepository", "✅ Clase eliminada")
                    // Ya se elimina de Room dentro de springBootRepo.eliminarClase
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
    override suspend fun limpiarLocal() {
        claseDao.eliminarTodas()
    }

    /**
     * Crea una CLASE DE PRUEBA automáticamente.
     * NOTA: Este método está deprecado y solo se usa para demos.
     * En producción, las clases deben crearse con PDFs reales subidos por el usuario.
     */
    @Deprecated("Solo para uso en demos. Usar crearClase con PDF real en producción.")
    suspend fun crearClaseDePrueba(
        asignaturaId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val claseDemoId = "clase_demo_${System.currentTimeMillis()}"
            val nombreArchivoDemo = "clase_demo.pdf"

            // Para demos, usar un PDF de ejemplo desde Supabase Storage o dejar vacío
            // En producción, el usuario debe subir su propio PDF
            val pdfUrl: String = "" // Vacío para demos sin PDF

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
                creador = uid,
                asignaturaId = asignaturaId
            )

            val result = springBootRepo.crearClase(claseDemo)
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
    override suspend fun tieneClases(asignaturaId: String): Boolean {
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
            // Use centralized StorageRepository to perform uploads
            val storageRepo = RepositoryProvider.provideStorageRepository(application)
            val uploadResult = storageRepo.subirPdf(pdfUri, nombreArchivo)

            uploadResult.fold(onSuccess = { url ->
                Log.d("ClaseRepository", "✅ PDF subido exitosamente")
                return@withContext url
            }, onFailure = { error ->
                Log.e("ClaseRepository", "❌ Error subiendo PDF", error)
                throw Exception(error.message ?: "Error subiendo PDF")
            })
        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error en subirPdfASupabaseStorage", e)
            throw Exception("Error subiendo PDF: ${'$'}{e.message}")
        }
    }
}
