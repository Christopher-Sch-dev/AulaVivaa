package cl.duocuc.aulaviva

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth // Importa la clase Firebase Auth (ojo aquí)

class LoginActivity : AppCompatActivity() {

    // Instanciamos Firebase Auth



    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa Firebase Auth aquí
        auth = FirebaseAuth.getInstance()

        // Captura los campos y el botón del layout
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val emailLayout = findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Acción al presionar el botón de login
        loginButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            // Validación básica y sencilla
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

            // Si los datos están bien, intenta login real con Firebase
            if (valid) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Bienvenido!", Toast.LENGTH_SHORT).show()
                            // Aquí puedes pasar al Panel principal
                        } else {
                            Toast.makeText(
                                this,
                                "Error de login: " + (task.exception?.message ?: ""),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
            val goToRegisterButton = findViewById<Button>(R.id.goToRegisterButton)
            goToRegisterButton.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }

        }
    }
}
