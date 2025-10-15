package cl.duocuc.aulaviva

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Manejo de autenticación y base de datos
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Inputs y botones
        val emailInput = findViewById<TextInputEditText>(R.id.registerEmailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.registerPasswordInput)
        val emailLayout = findViewById<TextInputLayout>(R.id.registerEmailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.registerPasswordLayout)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val backToLoginButton = findViewById<Button>(R.id.backToLoginButton)

        // Listener para registro nuevo
        registerButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""
            var valid = true

            // Validaciones simples
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Correo inválido"
                valid = false
            } else emailLayout.error = null

            if (password.length < 6) {
                passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else passwordLayout.error = null

            // Solo sigue si está válido
            if (valid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Guardamos datos extra en Firestore
                            val uid = auth.currentUser?.uid ?: ""
                            val usuario = hashMapOf(
                                "email" to email,
                                "rol" to "alumno"
                            )
                            firestore.collection("usuarios").document(uid)
                                .set(usuario)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registro exitoso. ¡Bienvenido a Aula Viva!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Registro incompleto: ${e.message}", Toast.LENGTH_LONG).show()
                                    finish()
                                }

                        } else {
                            val errorMsg = task.exception?.message ?: ""
                            if (errorMsg.contains("The email address is already in use")) {
                                Toast.makeText(this, "Ese correo ya está registrado. Inicia sesión o usa otro.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }

                    }
            }
        }

        // Listener para volver al login
        backToLoginButton.setOnClickListener {
            finish()
        }
    }
}
