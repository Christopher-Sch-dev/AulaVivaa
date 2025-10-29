package cl.duocuc.aulaviva.data.repository

import cl.duocuc.aulaviva.data.model.Clase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository que maneja toda la lógica de clases en Firestore
 */
class ClaseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Obtener todas las clases del usuario actual
    fun obtenerClases(
        onSuccess: (List<Clase>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("clases")
            .whereEqualTo("creador", uid)
            .get()
            .addOnSuccessListener { result ->
                val listaClases = mutableListOf<Clase>()
                for (doc in result) {
                    val clase = doc.toObject(Clase::class.java).copy(id = doc.id)
                    listaClases.add(clase)
                }
                onSuccess(listaClases)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al obtener clases")
            }
    }

    // Crear nueva clase
    fun crearClase(
        clase: Clase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("clases").add(clase)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Error al crear clase") }
    }

    // Actualizar clase existente
    fun actualizarClase(
        claseId: String,
        nombre: String,
        fecha: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "nombre" to nombre,
            "fecha" to fecha
        )
        firestore.collection("clases").document(claseId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Error al actualizar") }
    }

    // Eliminar clase
    fun eliminarClase(
        claseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("clases").document(claseId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Error al eliminar") }
    }
}
