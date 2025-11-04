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
        // Timeout de seguridad de 15 segundos
        var completed = false
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                onError("La operación tardó demasiado. Verifica tu conexión a internet.")
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (completed) return@addOnCompleteListener

                completed = true
                timeoutHandler.removeCallbacks(timeoutRunnable)

                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val errorMsg = task.exception?.message ?: ""
                    when {
                        errorMsg.contains("password") -> {
                            onError("Contraseña incorrecta")
                        }

                        errorMsg.contains("no user") || errorMsg.contains("not found") -> {
                            onError("No existe una cuenta con ese correo")
                        }

                        errorMsg.contains("network") || errorMsg.contains("internet") -> {
                            onError("Sin conexión a internet. Verifica tu red.")
                        }

                        else -> {
                            onError(errorMsg.ifEmpty { "Error desconocido al iniciar sesión" })
                        }
                    }
                }
            }
    }

    // Registro de nuevo usuario
    fun register(
        email: String,
        password: String,
        rol: String = "alumno",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Timeout de seguridad de 15 segundos
        var completed = false
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                onError("La operación tardó demasiado. Verifica tu conexión a internet.")
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (completed) return@addOnCompleteListener

                if (task.isSuccessful) {
                    // Guardamos datos extra en Firestore incluyendo el rol
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val usuario = hashMapOf(
                            "email" to email,
                            "rol" to rol,
                            "fechaRegistro" to System.currentTimeMillis()
                        )
                        firestore.collection("usuarios").document(uid)
                            .set(usuario)
                            .addOnSuccessListener {
                                if (!completed) {
                                    completed = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    onSuccess()
                                }
                            }
                            .addOnFailureListener { e ->
                                if (!completed) {
                                    completed = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    onError(e.message ?: "Error al guardar datos")
                                }
                            }
                    } else {
                        if (!completed) {
                            completed = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            onError("Error al obtener usuario")
                        }
                    }
                } else {
                    if (!completed) {
                        completed = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        val errorMsg = task.exception?.message ?: ""
                        when {
                            errorMsg.contains("The email address is already in use") -> {
                                onError("Ese correo ya está registrado. Inicia sesión o usa otro.")
                            }

                            errorMsg.contains("network") || errorMsg.contains("internet") -> {
                                onError("Sin conexión a internet. Verifica tu red.")
                            }

                            else -> {
                                onError(errorMsg.ifEmpty { "Error desconocido al registrar" })
                            }
                        }
                    }
                }
            }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }
}
