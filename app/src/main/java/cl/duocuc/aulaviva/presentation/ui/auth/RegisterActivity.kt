package cl.duocuc.aulaviva.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import cl.duocuc.aulaviva.R
import cl.duocuc.aulaviva.databinding.ActivityRegisterBinding
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel

class RegisterActivity : BaseActivity() {

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
        viewModel.isLoading.observe(this) { isLoading ->
            updateUIState(isLoading)
        }

        // Observer para errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
                // Asegurar que la UI vuelva al estado normal después de un error
                updateUIState(false)
            }
        }

        // Observer para registro exitoso
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "✓ Cuenta creada exitosamente", Toast.LENGTH_LONG).show()

                // Pequeño delay para que el usuario vea el mensaje de éxito
                binding.root.postDelayed({
                    finish()
                    // Animación de transición suave
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }, 600)
            }
        }
    }

    private fun updateUIState(isLoading: Boolean) {
        // Actualizar botones
        binding.registerButton.isEnabled = !isLoading
        binding.backToLoginButton.isEnabled = !isLoading

        // Actualizar texto del botón
        binding.registerButton.text = if (isLoading) "Registrando..." else "Registrarse"

        // Actualizar campos de entrada
        binding.registerEmailInput.isEnabled = !isLoading
        binding.registerPasswordInput.isEnabled = !isLoading
        binding.radioDocente.isEnabled = !isLoading
        binding.radioAlumno.isEnabled = !isLoading
    }

    private fun setupListeners() {
        // ✅ FIX: Botón para volver a la pantalla de bienvenida
        // Abre WelcomeActivity explícitamente en lugar de hacer finish()
        // Esto evita que la app se cierre si no hay activity en el stack
        binding.backToLoginButton.setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Botón de registro
        binding.registerButton.setOnClickListener {
            // Animación de feedback al tocar el botón
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_scale))

            // Obtener datos ingresados por el usuario
            val email = binding.registerEmailInput.text?.toString()?.trim() ?: ""
            val password = binding.registerPasswordInput.text?.toString() ?: ""

            // Detectar qué rol seleccionó el usuario (Docente o Alumno)
            // El RadioGroup solo permite tener un RadioButton seleccionado a la vez
            val rol = when (binding.radioGroupRol.checkedRadioButtonId) {
                R.id.radioDocente -> "docente"  // Si seleccionó el botón de Docente
                R.id.radioAlumno -> "alumno"    // Si seleccionó el botón de Alumno
                else -> "alumno"                 // Por defecto será alumno
            }

            // Validación de datos antes de registrar
            var valid = true

            // Validar email
            if (!viewModel.isValidEmail(email)) {
                binding.registerEmailLayout.error = "Correo inválido"
                valid = false
            } else {
                binding.registerEmailLayout.error = null // Limpiar error si está correcto
            }

            // Validar contraseña (mínimo 6 caracteres)
            if (!viewModel.isValidPassword(password)) {
                binding.registerPasswordLayout.error =
                    "La contraseña debe tener al menos 6 caracteres"
                valid = false
            } else {
                binding.registerPasswordLayout.error = null // Limpiar error si está correcto
            }

            // Si todo es válido, proceder con el registro
            if (valid) {
                // Mostrar mensaje de confirmación con el rol seleccionado
                Toast.makeText(this, "Registrando como $rol...", Toast.LENGTH_SHORT).show()
                // Enviar datos al ViewModel para crear cuenta en Supabase
                viewModel.register(email, password, rol)
            }
        }
    }
}
