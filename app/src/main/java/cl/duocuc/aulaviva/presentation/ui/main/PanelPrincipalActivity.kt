package cl.duocuc.aulaviva.presentation.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.clases.ListaClasesActivity
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PanelPrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding
    private lateinit var notificationHelper: NotificationHelper
    
    // Launcher para pedir permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, envío notificación de bienvenida
            enviarNotificacionBienvenida()
        }
        // Si no lo concede, la app sigue funcionando normal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializo el helper de notificaciones (RECURSO NATIVO #1)
        notificationHelper = NotificationHelper(this)
        
        setupListeners()
        pedirPermisoNotificaciones()
        cargarDatosUsuario()  // Cargo info del usuario desde Firestore
    }
    
    /**
     * Carga los datos del usuario desde Firestore y personaliza la UI.
     * Muestra el rol (docente/alumno) y adapta las opciones disponibles.
     */
    private fun cargarDatosUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val email = documento.getString("email") ?: "Usuario"
                    val rol = documento.getString("rol") ?: "alumno"
                    
                    // Personalizo el mensaje según el rol y email
                    val emoji = if (rol == "docente") "👨‍🏫" else "🎓"
                    val nombreCorto = email.substringBefore("@")
                    val mensaje = if (rol == "docente") {
                        "Bienvenido Profesor $nombreCorto"
                    } else {
                        "Bienvenido Estudiante $nombreCorto"
                    }
                    
                    binding.bienvenidaTextView.text = "$emoji $mensaje"
                    
                    // Adapto el botón según el rol
                    if (rol == "alumno") {
                        binding.irAClasesButton.text = "📚 Ver mis clases"
                    } else {
                        binding.irAClasesButton.text = "📊 Gestionar clases"
                    }
                }
            }
            .addOnFailureListener {
                // Si falla, uso valores por defecto
                binding.bienvenidaTextView.text = "¡Bienvenido!"
            }
    }
    
    /**
     * Pide permiso para notificaciones solo en Android 13+ (Tiramisu).
     * En versiones anteriores no es necesario pedir permiso.
     */
    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tengo el permiso, envío notificación
                    enviarNotificacionBienvenida()
                }
                else -> {
                    // Pido el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // En Android 12 o inferior no necesito pedir permiso
            enviarNotificacionBienvenida()
        }
    }
    
    /**
     * Envía una notificación de bienvenida personalizada
     */
    private fun enviarNotificacionBienvenida() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "Usuario"
        val nombre = email.substringBefore("@")  // Extraigo nombre del email
        notificationHelper.enviarNotificacionBienvenida(nombre)
    }

    private fun setupListeners() {
        // Botón para ver clases
        binding.irAClasesButton.setOnClickListener {
            val intent = Intent(this, ListaClasesActivity::class.java)
            startActivity(intent)
        }
        
        // 🎓 Botón NUEVO: Crear clase de prueba
        binding.crearClasePruebaButton.setOnClickListener {
            crearClaseDemostracion()
        }

        // Botón para cerrar sesión con confirmación
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
    
    /**
     * 🎓 Crea una clase de demostración con contenido rico
     * Esto sirve para testing y para la defensa EV2
     */
    private fun crearClaseDemostracion() {
        AlertDialog.Builder(this)
            .setTitle("🎓 Clase de Demostración")
            .setMessage("¿Quieres crear una clase de prueba con contenido educativo completo?\n\nIncluye:\n• Material educativo rico\n• PDF simulado de Kotlin\n• Links a documentación\n• Listo para probar IA")
            .setPositiveButton("Crear") { _, _ ->
                lifecycleScope.launch {
                    val repository = cl.duocuc.aulaviva.data.repository.ClaseRepository(this@PanelPrincipalActivity)
                    repository.crearClaseDePrueba(
                        onSuccess = {
                            Toast.makeText(this@PanelPrincipalActivity, "✅ Clase de prueba creada! Ve a 'Ver clases'", Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            Toast.makeText(this@PanelPrincipalActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
