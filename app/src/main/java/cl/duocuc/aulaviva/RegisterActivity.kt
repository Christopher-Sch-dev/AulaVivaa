package cl.duocuc.aulaviva

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Variable para usar Firebase Auth (login/registro seguro en la nube)
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Carga la pantalla diseñada en activity_register.xml
        setContentView(R.layout.activity_register)

        // Inicializo el objeto de autenticación de Firebase
        auth = FirebaseAuth.getInstance()

        // Referencias a los inputs y botones del formulario
        val emailInput = findViewById<TextInputEditText>(R.id.registerEmailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.registerPasswordInput)
        val emailLayout = findViewById<TextInputLayout>(R.id.registerEmailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.registerPasswordLayout)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val backToLoginButton = findViewById<Button>(R.id.backToLoginButton)

        // Acción al presionar "Registrar"
        registerButton.setOnClickListener {
            // Traigo texto del usuario y limpio espacios
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            // Bandera de validación, parte "en true"
            var valid = true

            // Validación simple de email
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Correo inválido"
                valid = false
            } else {
                emailLayout.error = null // Borra mensaje de error si está ok
            }

            // Validación simple de contraseña (mínimo 6 chars)
            if (password.length < 6) {
                passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                passwordLayout.error = null
            }

            // Si todas las validaciones están OK, registrar el usuario en Firebase
            if (valid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Obtengo el UID único que Firebase le asignó al nuevo usuario
                            val uid = auth.currentUser?.uid

                            // Creo un objeto con los datos del perfil que quiero guardar en Firestore
                            val nuevoUsuario = hashMapOf(
                                "email" to email,
                                "rol" to "alumno" // Por defecto todos los registros nuevos son alumnos
                                // Más adelante puedo agregar campos como nombre, carrera, etc.
                            )

                            // Verifico que el UID exista antes de guardar en la base de datos
                            if (uid != null) {
                                // Guardo el perfil del usuario en Firestore, colección "usuarios"
                                FirebaseFirestore.getInstance()
                                    .collection("usuarios")
                                    .document(uid) // Uso el UID como ID del documento para relacionarlo fácil
                                    .set(nuevoUsuario)
                                    .addOnSuccessListener {
                                        // Todo salió bien: usuario creado y perfil guardado
                                        Toast.makeText(this, "Registro y guardado exitoso, inicia sesión.", Toast.LENGTH_SHORT).show()
                                        finish() // Cierro la pantalla y vuelvo al login
                                    }
                                    .addOnFailureListener { e ->
                                        // El usuario se creó en Auth, pero no se guardó el perfil en Firestore
                                        Toast.makeText(this, "Se registró, pero no se guardó el perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                        finish() // Igual cierro, aunque haya fallado el guardado del perfil
                                    }
                            }
                        }
                    }
            }
        }


        // Opción para volver al login SIN crear usuario
        backToLoginButton.setOnClickListener {
            finish() // Simplemente cierra y vuelve al login
        }

    }
}
