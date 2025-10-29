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
    
    /**
     * 🎓 Crea una CLASE DE PRUEBA automáticamente
     * Para demostrar funcionalidades de la app
     */
    suspend fun crearClaseDePrueba(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val claseDemoId = "clase_demo_${System.currentTimeMillis()}"
            
            val claseDemo = Clase(
                id = claseDemoId,
                nombre = "Introducción a Desarrollo Android con Kotlin",
                fecha = "Lunes 29 de Octubre, 14:00hrs",
                creador = uid
            )
            
            // Primero creo la clase
            val entity = ClaseEntity(
                id = claseDemoId,
                nombre = claseDemo.nombre,
                fecha = claseDemo.fecha,
                creador = uid,
                sincronizado = false
            )
            claseDao.insertarClase(entity)
            
            // Subo a Firestore
            firestore.collection("clases")
                .document(claseDemoId)
                .set(hashMapOf(
                    "nombre" to claseDemo.nombre,
                    "fecha" to claseDemo.fecha,
                    "creador" to uid
                ))
                .await()
            
            // Ahora agrego los materiales
            agregarMaterialDePrueba(claseDemoId)
            
            claseDao.actualizarClase(entity.copy(sincronizado = true))
            onSuccess()
            
        } catch (e: Exception) {
            onError(e.message ?: "Error al crear clase de prueba")
        }
    }
    
    /**
     * Agrega materiales educativos de prueba a la clase demo
     */
    private suspend fun agregarMaterialDePrueba(claseId: String) {
        try {
            // Material 1: PDF simulado
            firestore.collection("clases").document(claseId)
                .collection("materiales")
                .document("material_1")
                .set(hashMapOf(
                    "tipo" to "pdf",
                    "nombre" to "Guía de Kotlin para Android.pdf",
                    "descripcion" to """
                        📚 GUÍA COMPLETA DE KOTLIN PARA ANDROID
                        
                        Este material cubre los fundamentos de programación Android moderna usando Kotlin:
                        
                        📖 Contenido:
                        • Variables y tipos de datos (val, var, tipos básicos)
                        • Funciones y lambdas (sintaxis moderna)
                        • Clases y objetos (POO en Kotlin)
                        • Null Safety (evitar NullPointerException)
                        • Coroutines (programación asíncrona)
                        • Android Jetpack (ViewModel, LiveData, Room)
                        • Material Design 3 (UI moderna)
                        • Firebase (Auth y Firestore)
                        
                        🎯 Objetivos de aprendizaje:
                        1. Entender sintaxis básica de Kotlin
                        2. Aplicar programación orientada a objetos
                        3. Usar coroutines para operaciones asíncronas
                        4. Implementar arquitectura MVVM
                        5. Integrar Firebase en apps Android
                        
                        💡 Conceptos clave:
                        • Kotlin es interoperable con Java
                        • Menos código, más seguro (null safety)
                        • Coroutines simplifican async/await
                        • Jetpack moderniza desarrollo Android
                        
                        📝 Ejercicios prácticos incluidos:
                        - Crear tu primera Activity
                        - Implementar RecyclerView
                        - Conectar Room Database
                        - Integrar Firebase Auth
                        - Usar ViewModel y LiveData
                        
                        ⚡ Nivel: Intermedio-Avanzado
                        ⏱️ Tiempo estimado: 90 minutos
                        
                        Este material fue diseñado para estudiantes de 4to semestre de Ingeniería en Informática, pero es útil para cualquiera que quiera aprender desarrollo Android moderno.
                    """.trimIndent(),
                    "fechaSubida" to System.currentTimeMillis()
                ))
                .await()
            
            // Material 2: Link útil
            firestore.collection("clases").document(claseId)
                .collection("materiales")
                .document("material_2")
                .set(hashMapOf(
                    "tipo" to "link",
                    "nombre" to "Documentación Oficial de Kotlin",
                    "url" to "https://kotlinlang.org/docs/android-overview.html",
                    "descripcion" to "Guía oficial de JetBrains para desarrollo Android con Kotlin. Incluye tutoriales, ejemplos de código y mejores prácticas.",
                    "fechaSubida" to System.currentTimeMillis()
                ))
                .await()
                
        } catch (e: Exception) {
            println("Error al agregar materiales de prueba: ${e.message}")
        }
    }
}