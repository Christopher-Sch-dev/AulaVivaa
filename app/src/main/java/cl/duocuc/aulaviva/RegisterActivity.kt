package cl.duocuc.aulaviva

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<TextInputEditText>(R.id.registerEmailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.registerPasswordInput)
        val emailLayout = findViewById<TextInputLayout>(R.id.registerEmailInput)
        val passwordLayout = findViewById<TextInputLayout>(R.id.registerPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            var valid = true
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Correo inválido"
                valid = false
            } else {
                emailLayout.error = null
            }

            if (password.length < 6) {
                passwordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                passwordLayout.error = null
            }

            if (valid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registro exitoso, ahora puedes iniciar sesión.", Toast.LENGTH_SHORT).show()
                            finish() // Vuelve al login para que el usuario se autentique
                        } else {
                            Toast.makeText(
                                this,
                                "Error de registro: " + (task.exception?.message ?: ""),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }

            val backToLoginButton = findViewById<Button>(R.id.backToLoginButton)
            backToLoginButton.setOnClickListener {
                finish() // Termina Activity y vuelve atrás al Login
            }

        }
    }
}
