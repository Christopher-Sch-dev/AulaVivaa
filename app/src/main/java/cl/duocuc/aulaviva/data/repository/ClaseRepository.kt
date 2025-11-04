package cl.duocuc.aulaviva.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import cl.duocuc.aulaviva.data.model.Clase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
                    creador = entity.creador
                )
            } else null
        } catch (_: Exception) {
            null
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
                    descripcion = doc.getString("descripcion") ?: "",
                    fecha = doc.getString("fecha") ?: "",
                    archivoPdfUrl = doc.getString("archivoPdfUrl") ?: "",
                    archivoPdfNombre = doc.getString("archivoPdfNombre") ?: "",
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
                descripcion = clase.descripcion,
                fecha = clase.fecha,
                archivoPdfUrl = clase.archivoPdfUrl,
                archivoPdfNombre = clase.archivoPdfNombre,
                creador = uid,
                sincronizado = false  // Aún no está en Firestore
            )
            claseDao.insertarClase(entity)

            // Intento subir a Firestore
            try {
                firestore.collection("clases")
                    .document(entity.id)
                    .set(
                        hashMapOf(
                            "nombre" to entity.nombre,
                            "descripcion" to entity.descripcion,
                            "fecha" to entity.fecha,
                            "archivoPdfUrl" to entity.archivoPdfUrl,
                            "archivoPdfNombre" to entity.archivoPdfNombre,
                            "creador" to entity.creador
                        )
                    )
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
     * Wrapper no-suspend para crearClase y evitar errores de corrutina desde UI
     */
    fun crearClaseAsync(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            crearClase(clase, onSuccess, onError)
        }
    }

    /**
     * Actualiza una clase existente.
     * Mismo flujo: primero Room, luego Firestore.
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
            val entity = ClaseEntity(
                id = claseId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = fecha,
                archivoPdfUrl = archivoPdfUrl,
                archivoPdfNombre = archivoPdfNombre,
                creador = uid,
                sincronizado = false
            )
            claseDao.actualizarClase(entity)

            // Intento actualizar en Firestore
            try {
                firestore.collection("clases")
                    .document(claseId)
                    .update(
                        hashMapOf<String, Any>(
                            "nombre" to nombre,
                            "descripcion" to descripcion,
                            "fecha" to fecha,
                            "archivoPdfUrl" to archivoPdfUrl,
                            "archivoPdfNombre" to archivoPdfNombre
                        )
                    )
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
     * ✅ TAREA 3: Método sobrecargado para actualizar clase usando objeto Clase
     */
    suspend fun actualizarClase(clase: Clase) {
        val entity = ClaseEntity(
            id = clase.id,
            nombre = clase.nombre,
            descripcion = clase.descripcion,
            fecha = clase.fecha,
            archivoPdfUrl = clase.archivoPdfUrl,
            archivoPdfNombre = clase.archivoPdfNombre,
            creador = clase.creador,
            sincronizado = false
        )
        claseDao.actualizarClase(entity)

        // Intento actualizar en Firestore
        try {
            firestore.collection("clases")
                .document(clase.id)
                .update(
                    hashMapOf<String, Any>(
                        "nombre" to clase.nombre,
                        "descripcion" to clase.descripcion,
                        "fecha" to clase.fecha,
                        "archivoPdfUrl" to clase.archivoPdfUrl,
                        "archivoPdfNombre" to clase.archivoPdfNombre
                    )
                )
                .await()

            claseDao.actualizarClase(entity.copy(sincronizado = true))
        } catch (_: Exception) {
            // No importa si falla en Firestore, ya se actualizó en Room
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
     * ✅ TAREA 3: Método sobrecargado para eliminar clase usando solo el ID
     */
    suspend fun eliminarClase(claseId: String) {
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

            // Primero creo la clase con todos los campos
            val entity = ClaseEntity(
                id = claseDemoId,
                nombre = "Introducción a Desarrollo Android con Kotlin",
                descripcion = """
                    En esta clase exploraremos los fundamentos del desarrollo móvil Android utilizando Kotlin.
                    
                    Actividades:
                    • Configuración del entorno de desarrollo
                    • Sintaxis básica de Kotlin
                    • Creación de la primera aplicación
                    • Arquitectura MVVM
                    • Integración con Firebase
                    
                    Material incluido: Guía completa en PDF con ejemplos prácticos.
                """.trimIndent(),
                fecha = "Lunes 4 de Noviembre, 14:00hrs",
                archivoPdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                archivoPdfNombre = "Demo_Material_Kotlin.pdf",
                creador = uid,
                sincronizado = false
            )
            claseDao.insertarClase(entity)

            // Subo a Firestore
            firestore.collection("clases")
                .document(claseDemoId)
                .set(
                    hashMapOf(
                        "nombre" to entity.nombre,
                        "descripcion" to entity.descripcion,
                        "fecha" to entity.fecha,
                        "archivoPdfUrl" to entity.archivoPdfUrl,
                        "archivoPdfNombre" to entity.archivoPdfNombre,
                        "creador" to uid
                    )
                )
                .await()

            // Actualizo como sincronizado
            claseDao.actualizarClase(entity.copy(sincronizado = true))
            onSuccess()

        } catch (e: Exception) {
            onError(e.message ?: "Error al crear clase de prueba")
        }
    }

    /**
     * 📤 TAREA 2: Sube un PDF a Firebase Storage y retorna la URL pública
     *
     * Esta función permite que los PDFs funcionen en múltiples dispositivos.
     * En lugar de guardar content:// (que solo funciona local), subimos el archivo
     * a Firebase Storage y obtenemos una URL pública que funciona en cualquier lugar.
     *
     * Ruta de almacenamiento: clases/{UID_DOCENTE}/{TIMESTAMP}_{NOMBRE_ARCHIVO}
     * - UID_DOCENTE: identifica al docente propietario
     * - TIMESTAMP: evita colisiones de nombres
     *
     * @param pdfUri URI local del PDF seleccionado (content://...)
     * @param nombreArchivo Nombre original del archivo
     * @return URL pública del PDF en Firebase Storage (https://firebasestorage.googleapis.com/...)
     * @throws Exception si falla la autenticación o la subida
     */
    suspend fun subirPdfAFirebaseStorage(
        pdfUri: Uri,
        nombreArchivo: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Obtener UID del docente actual (necesario para seguridad)
            val uidDocente = auth.currentUser?.uid
                ?: throw Exception("Docente no autenticado")

            // Referencia a Firebase Storage
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference

            // Crear ruta segura: clases/{UID}/{TIMESTAMP}_{NOMBRE}
            // Ejemplo: clases/abc123/1699123456789_Material_Kotlin.pdf
            val timestamp = System.currentTimeMillis()
            val rutaPdf = "clases/$uidDocente/${timestamp}_$nombreArchivo"

            Log.d("ClaseRepository", "📤 Iniciando subida de PDF a: $rutaPdf")

            // Subir archivo de forma síncrona en corrutina
            val fileRef = storageRef.child(rutaPdf)
            fileRef.putFile(pdfUri).await()

            Log.d("ClaseRepository", "✅ PDF subido correctamente")

            // Obtener URL pública del PDF (esta URL funciona en cualquier dispositivo)
            val downloadUrl = fileRef.downloadUrl.await()
            val urlPublica = downloadUrl.toString()

            Log.d("ClaseRepository", "🔗 URL pública obtenida: $urlPublica")

            return@withContext urlPublica

        } catch (e: Exception) {
            Log.e("ClaseRepository", "❌ Error al subir PDF a Storage", e)
            throw Exception("Error subiendo PDF: ${e.message}")
        }
    }
}
