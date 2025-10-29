package cl.duocuc.aulaviva.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository que maneja toda la lógica de autenticación
 * Separa la lógica de Firebase de las Activities
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Obtener usuario actual
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Login con email y password
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Error desconocido")
                }
            }
    }

    // Registro de nuevo usuario
    fun register(
        email: String,
        password: String,
        rol: String = "alumno",  // Nuevo parámetro: rol del usuario
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Guardamos datos extra en Firestore incluyendo el rol
                    val uid = auth.currentUser?.uid ?: ""
                    val usuario = hashMapOf(
                        "email" to email,
                        "rol" to rol,  // "docente" o "alumno"
                        "fechaRegistro" to System.currentTimeMillis()
                    )
                    firestore.collection("usuarios").document(uid)
                        .set(usuario)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.message ?: "Error al guardar datos") }
                } else {
                    val errorMsg = task.exception?.message ?: ""
                    if (errorMsg.contains("The email address is already in use")) {
                        onError("Ese correo ya está registrado. Inicia sesión o usa otro.")
                    } else {
                        onError(errorMsg)
                    }
                }
            }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }
}
