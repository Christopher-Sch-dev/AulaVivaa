package cl.duocuc.aulaviva.data.repository

import android.content.Context
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository que maneja toda la lógica de clases.
 * Usa DOS fuentes de datos:
 * 1. Room (BD local) - Funciona sin internet
 * 2. Firestore (nube) - Se sincroniza cuando hay conexión
 * 
 * ESTRATEGIA:
 * - Siempre leo primero de Room (rápido, offline)
 * - Intento sincronizar con Firestore en segundo plano
 * - Si no hay internet, guardo en Room con sincronizado=false
 * - Cuando vuelve internet, subo lo pendiente
 * 
 * Pensamiento: Esta es la magia de hacer apps que funcionan sin internet.
 * El usuario no nota si hay wifi o no, todo sigue funcionando.
 */
class ClaseRepository(context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid: String get() = auth.currentUser?.uid ?: ""
    
    // Referencia al DAO de Room (BD local)
    private val claseDao: ClaseDao = AppDatabase.getDatabase(context).claseDao()

    /**
     * Obtiene las clases del usuario desde Room (local).
     * Flow emite automáticamente cuando los datos cambian.
     * Mapeo ClaseEntity (BD) a Clase (modelo de la app).
     */
    fun obtenerClasesLocal(): Flow<List<Clase>> {
        return claseDao.obtenerClasesPorUsuario(uid).map { entities ->
            entities.map { entity ->
                Clase(
                    id = entity.id,
                    nombre = entity.nombre,
                    fecha = entity.fecha,
                    creador = entity.creador
                )
            }
        }
    }
    
    /**
     * Sincroniza clases desde Firestore a Room.
     * Esto se llama al abrir la app o al refrescar.
     */
    suspend fun sincronizarDesdeFirestore() {
        try {
            val snapshot = firestore.collection("clases")
                .whereEqualTo("creador", uid)
                .get()
                .await()
            
            val clasesFirestore = snapshot.documents.map { doc ->
                ClaseEntity(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    fecha = doc.getString("fecha") ?: "",
                    creador = doc.getString("creador") ?: "",
                    sincronizado = true
                )
            }
            
            // Guardo todas las clases de Firestore en Room
            claseDao.insertarVarias(clasesFirestore)
            
        } catch (e: Exception) {
            // Si no hay internet, no pasa nada. Room sigue funcionando.
        }
    }

    /**
     * Crea una nueva clase.
     * 1. La guardo primero en Room (instantáneo)
     * 2. Intento subirla a Firestore
     * 3. Si no hay internet, queda marcada como no sincronizada
     */
    suspend fun crearClase(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Primero guardo en Room (siempre funciona)
            val entity = ClaseEntity(
                id = clase.id.ifEmpty { firestore.collection("clases").document().id },
                nombre = clase.nombre,
                fecha = clase.fecha,
                creador = uid,
                sincronizado = false  // Aún no está en Firestore
            )
            claseDao.insertarClase(entity)
            
            // Intento subir a Firestore
            try {
                firestore.collection("clases")
                    .document(entity.id)
                    .set(hashMapOf(
                        "nombre" to entity.nombre,
                        "fecha" to entity.fecha,
                        "creador" to entity.creador
                    ))
                    .await()
                
                // Actualizo en Room como sincronizado
                claseDao.actualizarClase(entity.copy(sincronizado = true))
                onSuccess()
                
            } catch (e: Exception) {
                // No hay internet, pero ya está en Room
                onSuccess()  // El usuario no nota la diferencia
            }
            
        } catch (e: Exception) {
            onError(e.message ?: "Error al crear clase")
        }
    }

    /**
     * Actualiza una clase existente.
     * Mismo flujo: primero Room, luego Firestore.
     */
    suspend fun actualizarClase(
        claseId: String,
        nombre: String,
        fecha: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val entity = ClaseEntity(
                id = claseId,
                nombre = nombre,
                fecha = fecha,
                creador = uid,
                sincronizado = false
            )
            claseDao.actualizarClase(entity)
            
            // Intento actualizar en Firestore
            try {
                firestore.collection("clases")
                    .document(claseId)
                    .update(hashMapOf<String, Any>(
                        "nombre" to nombre,
                        "fecha" to fecha
                    ))
                    .await()
                
                claseDao.actualizarClase(entity.copy(sincronizado = true))
                onSuccess()
                
            } catch (e: Exception) {
                onSuccess()  // Actualizado en Room al menos
            }
            
        } catch (e: Exception) {
            onError(e.message ?: "Error al actualizar")
        }
    }

    /**
     * Elimina una clase.
     * Primero de Room, luego de Firestore.
     */
    suspend fun eliminarClase(
        claseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val entity = ClaseEntity(id = claseId, nombre = "", fecha = "", creador = uid)
            claseDao.eliminarClase(entity)
            
            try {
                firestore.collection("clases")
                    .document(claseId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // No importa si falla en Firestore, ya se borró de Room
            }
            
            onSuccess()
            
        } catch (e: Exception) {
            onError(e.message ?: "Error al eliminar")
        }
    }
    
    /**
     * Limpia todas las clases locales (útil al cerrar sesión)
     */
    suspend fun limpiarLocal() {
        claseDao.eliminarTodas()
    }
}
