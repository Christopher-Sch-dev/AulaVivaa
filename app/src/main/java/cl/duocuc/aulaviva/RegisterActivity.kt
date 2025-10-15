package cl.duocuc.aulaviva

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

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

            // Si pasa validaciones, intento registrar con Firebase
            if (valid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Si resulta, aviso que está todo bien y cierro pantalla
                            Toast.makeText(this, "Registro exitoso, ahora puedes iniciar sesión.", Toast.LENGTH_SHORT).show()
                            finish() // Vuelve atrás, al login
                        } else {
                            // Si hay error (correo ya usado, mal conexión, etc.)
                            Toast.makeText(
                                this,
                                "Error de registro: " + (task.exception?.message ?: ""),
                                Toast.LENGTH_LONG
                            ).show()
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
