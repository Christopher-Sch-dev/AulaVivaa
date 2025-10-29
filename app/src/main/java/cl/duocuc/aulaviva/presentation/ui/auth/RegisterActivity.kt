package cl.duocuc.aulaviva.presentation.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import cl.duocuc.aulaviva.databinding.ActivityRegisterBinding
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // Observer para loading
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.registerButton.isEnabled = !isLoading
            binding.backToLoginButton.isEnabled = !isLoading
        })

        // Observer para errores
        viewModel.error.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })

        // Observer para registro exitoso
        viewModel.registerSuccess.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Registro exitoso! Ahora inicia sesión", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupListeners() {
        // Botón para volver a login
        binding.backToLoginButton.setOnClickListener {
            finish()
        }

        // Botón de registro
        binding.registerButton.setOnClickListener {
            val email = binding.registerEmailInput.text?.toString()?.trim() ?: ""
            val password = binding.registerPasswordInput.text?.toString() ?: ""

            // Validación
            var valid = true
            if (!viewModel.isValidEmail(email)) {
                binding.registerEmailLayout.error = "Correo inválido"
                valid = false
            } else {
                binding.registerEmailLayout.error = null
            }

            if (!viewModel.isValidPassword(password)) {
                binding.registerPasswordLayout.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                binding.registerPasswordLayout.error = null
            }

            if (valid) {
                viewModel.register(email, password)
            }
        }
    }
}
