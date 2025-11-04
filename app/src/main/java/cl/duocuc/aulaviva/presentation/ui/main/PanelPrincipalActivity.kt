package cl.duocuc.aulaviva.presentation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding
    private lateinit var notificationHelper: NotificationHelper
    private var rolActual: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enviarNotificacionBienvenida()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationHelper = NotificationHelper(this)

        // 1. Desactivar botones al inicio para evitar condiciones de carrera
        binding.irAClasesButton.isEnabled = false
        binding.crearClasePruebaButton.isEnabled = false

        setupListeners()
        pedirPermisoNotificaciones()
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error de autenticación. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            // Redirigir a Login si no hay usuario
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val email = documento.getString("email") ?: "Usuario"
                    val rolBruto = documento.getString("rol") ?: "alumno"
                    val rolNormalized = rolBruto.trim().lowercase()
                    rolActual = if (rolNormalized.contains("doc")) "docente" else "alumno"

                    val emoji = if (rolActual == "docente") "👨‍🏫" else "🎓"
                    val nombreCorto = email.substringBefore("@")
                    val mensaje = if (rolActual == "docente") {
                        "Bienvenido Profesor $nombreCorto"
                    } else {
                        "Bienvenido Estudiante $nombreCorto"
                    }
                    binding.bienvenidaTextView.text = "$emoji $mensaje"

                    binding.irAClasesButton.text =
                        if (rolActual == "docente") "📊 Gestionar clases" else "📚 Ver clases"
                    
                    // 2. Activar botones una vez que los datos están cargados
                    binding.irAClasesButton.isEnabled = true
                    binding.crearClasePruebaButton.isEnabled = true
                } else {
                    binding.bienvenidaTextView.text = "Perfil no encontrado."
                    Toast.makeText(this, "No se pudo encontrar tu perfil en la base de datos.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w("PanelPrincipal", "Error cargando usuario", e)
                binding.bienvenidaTextView.text = "¡Bienvenido!"
                Toast.makeText(this, "Error al cargar el perfil. Revisa tu conexión.", Toast.LENGTH_LONG).show()
            }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enviarNotificacionBienvenida()
            }
        } else {
            enviarNotificacionBienvenida()
        }
    }

    private fun enviarNotificacionBienvenida() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "Usuario"
        val nombre = email.substringBefore("@")
        notificationHelper.enviarNotificacionBienvenida(nombre)
    }

    private fun setupListeners() {
        binding.irAClasesButton.setOnClickListener {
            // La comprobación de rolActual.isEmpty() ya no es necesaria aquí
            val targetActivity = if (rolActual == "docente") {
                cl.duocuc.aulaviva.presentation.ui.clases.ListaClasesActivity::class.java
            } else {
                PanelAlumnoActivity::class.java // Asegúrate que esta clase exista
            }
            startActivity(Intent(this, targetActivity))
        }

        binding.crearClasePruebaButton.setOnClickListener {
            crearClaseDemostracion()
        }

        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que quieres salir de Aula Viva?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun crearClaseDemostracion() {
         // Se mantiene la misma lógica de antes
        AlertDialog.Builder(this)
            .setTitle("🎓 Clase de Demostración")
            .setMessage("¿Quieres crear una clase de prueba con contenido educativo completo?")
            .setPositiveButton("Crear") { _, _ ->
                lifecycleScope.launch {
                    val repository =
                        cl.duocuc.aulaviva.data.repository.ClaseRepository(this@PanelPrincipalActivity)
                    repository.crearClaseDePrueba(
                        onSuccess = {
                            Toast.makeText(
                                this@PanelPrincipalActivity,
                                "✅ Clase de prueba creada! Ve a 'Ver clases'",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onError = { error ->
                            Toast.makeText(
                                this@PanelPrincipalActivity,
                                "Error: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
