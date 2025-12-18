package cl.duocuc.aulaviva.data.repository

import android.app.Application
import android.util.Log
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.remote.SpringBootClaseRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * REPOSITORIO OPTIMIZADO (Phase 3)
 * Implementa el patrón Offline-First estricto con Single Source of Truth (SSOT).
 * 
 * Estrategia:
 * 1. UI observa SOLO la Base de Datos (Room).
 * 2. Repositorio refresca la BD en segundo plano.
 * 3. Escrituras van a la BD primero (optimistic UI) y luego se sincronizan.
 */
class OfflineClasesRepository(private val application: Application) : IClaseRepository {

    private val uid: String get() = cl.duocuc.aulaviva.data.remote.JwtDecoder.getUserIdFromToken(
        cl.duocuc.aulaviva.data.remote.TokenManager.getToken() ?: ""
    ) ?: ""

    private val db = AppDatabase.getDatabase(application)
    private val claseDao: ClaseDao = db.claseDao()
    private val remoteRepo = SpringBootClaseRepository(claseDao, SpringBootClient.apiService)

    // ============================================================================================
    // LECTURA (READS) - Reactive Source of Truth
    // ============================================================================================

    /**
     * Devuelve un Flow que:
     * 1. Emite INMEDIATAMENTE lo que hay en cache (rápido).
     * 2. Intenta sincronizar con la nube en segundo plano.
     * 3. Si la nube trae datos nuevos, Room los guarda y el Flow emite de nuevo automáticamente.
     */
    override fun obtenerClasesLocal(): Flow<List<Clase>> = flow {
        // Emitir cache local (Source of Truth principal)
        emitAll(
            claseDao.obtenerClasesPorUsuario(uid).map { entities -> 
                entities.map { it.toClase() } 
            }
        )
    }.onStart {
        // Trigger de sincronización al iniciar la observación (Side Effect controlado)
        CoroutineScope(Dispatchers.IO).launch {
            sincronizarDesdeSupabase()
        }
    }

    override fun obtenerClasesPorAsignatura(asignaturaId: String): Flow<List<Clase>> = flow {
         emitAll(
            claseDao.obtenerClasesPorAsignatura(asignaturaId).map { entities ->
                entities.map { it.toClase() }
            }
         )
    }.onStart {
        CoroutineScope(Dispatchers.IO).launch {
            sincronizarClasesPorAsignatura(asignaturaId)
        }
    }

    override fun obtenerClasesPorAsignaturas(asignaturasIds: List<String>): Flow<List<Clase>> {
        if (asignaturasIds.isEmpty()) return flowOf(emptyList())
        return claseDao.obtenerClasesPorAsignaturas(asignaturasIds).map { entities ->
            entities.map { it.toClase() }
        }
    }

    override suspend fun obtenerClasePorId(claseId: String): Clase? {
        // Lectura directa (one-shot), intentamos refrescar si es null
        val cached = claseDao.obtenerClasePorId(claseId)?.toClase()
        if (cached != null) return cached
        
        // Si no está en cache, intentamos traerlo (fallback)
        return try {
            remoteRepo.obtenerClasePorId(claseId).getOrNull()
        } catch(e: Exception) {
            null
        }
    }
    
    // ============================================================================================
    // ESCRITURA (WRITES) - Write-Through / Write-Back
    // ============================================================================================

    override suspend fun crearClase(clase: Clase, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            // OPTIMISTIC UPDATE: Guardar en local inmediatamente declarándolo NO sincronizado
            // Esto hace que la UI responda instantáneamente
            val entityLocal = clase.toEntityLocal(sincronizado = false)
            claseDao.insertarClase(entityLocal)
            onSuccess() // Confirmar a la UI "ya está guardado"

            // Intentar subir en background
            val result = remoteRepo.crearClase(clase)
            result.onSuccess { 
                // Si sube bien, actualizamos a sincronizado = true
                claseDao.insertarClase(clase.toEntityLocal(sincronizado = true))
                Log.d("OfflineRepo", "✅ Sync upload éxito: ${clase.id}")
            }.onFailure {
                Log.w("OfflineRepo", "⏳ Sync upload falló (guardado para después): ${it.message}")
            }
        } catch (e: Exception) {
             Log.e("OfflineRepo", "❌ Error crítico creando clase", e)
             onError(e.message ?: "Error desconocido")
        }
    }

    override fun crearClaseAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        scope.launch { crearClase(clase, onSuccess, onError) }
    }

    // ============================================================================================
    // SINCRONIZACIÓN (SYNC)
    // ============================================================================================

    override suspend fun sincronizarDesdeSupabase() {
        try {
            // 1. PUSH: Subir pendientes
            val pendientes = claseDao.obtenerNoSincronizadas()
            if (pendientes.isNotEmpty()) {
                Log.d("OfflineRepo", "🔄 Subiendo ${pendientes.size} pendientes...")
                pendientes.forEach { entity ->
                    val clase = entity.toClase()
                    // Lógica de "upsert" remoto
                    val remoteResult = remoteRepo.crearClase(clase) // SpringBootRepo maneja la lógica interna
                    if (remoteResult.isSuccess) {
                        claseDao.insertarClase(entity.copy(sincronizado = true))
                    }
                }
            }
            
            // 2. PULL: Descargar nuevos
            remoteRepo.obtenerClases() // Este método en SpringBootRepo ya inserta en Room
        } catch (e: Exception) {
            Log.e("OfflineRepo", "Sync error", e)
        }
    }

    override suspend fun sincronizarClasesPorAsignatura(asignaturaId: String) {
        remoteRepo.obtenerClasesPorAsignatura(asignaturaId)
    }

    // ============================================================================================
    // UTILS & MAPPERS
    // ============================================================================================

    override suspend fun actualizarClase(
        claseId: String, nombre: String, descripcion: String, 
        fecha: String, archivoPdfUrl: String, archivoPdfNombre: String, 
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
         // Implementación simplificada reutilizando lógica de entidad
         val original = obtenerClasePorId(claseId) ?: return onError("Clase no encontrada")
         val updated = original.copy(
             nombre = nombre, descripcion = descripcion, fecha = fecha,
             archivoPdfUrl = archivoPdfUrl, archivoPdfNombre = archivoPdfNombre
         )
         actualizarClase(updated)
         onSuccess()
    }

    override suspend fun actualizarClase(clase: Clase) {
        // Optimistic Update
        claseDao.actualizarClase(clase.toEntityLocal(sincronizado = false))
        
        // Sync
        try {
            val result = remoteRepo.actualizarClase(clase)
            if (result.isSuccess) {
                claseDao.actualizarClase(clase.toEntityLocal(sincronizado = true))
            }
        } catch (e: Exception) {
            Log.w("OfflineRepo", "Update offline", e)
        }
    }

    override suspend fun eliminarClase(claseId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val entity = claseDao.obtenerClasePorId(claseId) ?: return onError("No existe local")
            claseDao.eliminarClase(entity) // Borrado local optimista
            
            // Sync
            val result = remoteRepo.eliminarClase(claseId)
            if (result.isFailure) {
                Log.w("OfflineRepo", "Delete failed sync", result.exceptionOrNull())
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error")
        }
    }

    override suspend fun eliminarClase(claseId: String) {
        eliminarClase(claseId, {}, {})
    }

    override suspend fun limpiarLocal() {
        claseDao.eliminarTodas()
    }

    override suspend fun tieneClases(asignaturaId: String): Boolean {
        val countLocal = claseDao.obtenerClasesPorAsignaturaDirecto(asignaturaId).size
        if (countLocal > 0) return true
        
        // Try sync
        sincronizarClasesPorAsignatura(asignaturaId)
        return claseDao.obtenerClasesPorAsignaturaDirecto(asignaturaId).isNotEmpty()
    }

    private fun Clase.toEntityLocal(sincronizado: Boolean) = ClaseEntity(
        id = id, nombre = nombre, descripcion = descripcion, fecha = fecha,
        archivoPdfUrl = archivoPdfUrl, archivoPdfNombre = archivoPdfNombre,
        creador = creador, asignaturaId = asignaturaId, sincronizado = sincronizado
    )

    private fun ClaseEntity.toClase() = Clase(
        id = id, nombre = nombre, descripcion = descripcion, fecha = fecha,
        archivoPdfUrl = archivoPdfUrl, archivoPdfNombre = archivoPdfNombre,
        creador = creador, asignaturaId = asignaturaId
    )
}
