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
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.databinding.ActivityPanelPrincipalBinding
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.utils.NotificationHelper
import kotlinx.coroutines.launch

class PanelPrincipalActivity : BaseActivity() {

    private lateinit var binding: ActivityPanelPrincipalBinding
    private lateinit var notificationHelper: NotificationHelper
    private var rolActual: String = ""

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
        cargarDatosUsuario()  // Cargo info del usuario desde Supabase
    }

    /**
     * Carga los datos del usuario desde Supabase y personaliza la UI.
     * Muestra el rol (docente/alumno) y adapta las opciones disponibles.
     */
    private fun cargarDatosUsuario() {
        val uid = SupabaseAuthManager.getCurrentUserId()
        val email = SupabaseAuthManager.getCurrentUserEmail()

        if (uid == null || email == null) {
            Log.e("PanelPrincipal", "❌ Usuario no autenticado")
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            // Redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("PanelPrincipal", "✅ Usuario cargado: $email")

        // Por defecto, todos los usuarios registrados son docentes
        rolActual = "docente"

        val emoji = "👨‍🏫"
        val nombreCorto = email.substringBefore("@")
        val mensaje = "Bienvenido Profesor $nombreCorto"

        binding.bienvenidaTextView.text = "$emoji $mensaje"
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
        val email = SupabaseAuthManager.getCurrentUserEmail() ?: "Usuario"
        val nombre = email.substringBefore("@")  // Extraigo nombre del email
        notificationHelper.enviarNotificacionBienvenida(nombre)
    }

    private fun setupListeners() {
        // 📚 Botón: Mis Asignaturas (Docente)
        binding.misAsignaturasButton.setOnClickListener {
            try {
                if (rolActual.isEmpty()) {
                    Toast.makeText(
                        this,
                        "⏳ Cargando tu perfil... Intenta de nuevo en un momento",
                        Toast.LENGTH_SHORT
                    ).show()
                    cargarDatosUsuario()
                    return@setOnClickListener
                }

                Log.d("PanelPrincipal", "✅ Abriendo Mis Asignaturas (Docente)")
                startActivity(
                    Intent(
                        this,
                        cl.duocuc.aulaviva.presentation.activity.DocenteAsignaturasActivity::class.java
                    )
                )
            } catch (ex: Exception) {
                Log.e("PanelPrincipal", "❌ Error abriendo asignaturas: ${ex.message}", ex)
                Toast.makeText(this, "No se pudo abrir asignaturas", Toast.LENGTH_SHORT).show()
            }
        }

        // 🧪 Botón: Modo Demostración
        binding.crearClasePruebaButton.setOnClickListener {
            crearAsignaturaYClaseDemo()
        }

        // Botón para cerrar sesión con confirmación
        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que quieres salir de Aula Viva?")
                .setPositiveButton("Sí, salir") { _, _ ->
                    lifecycleScope.launch {
                        SupabaseAuthManager.logout()
                        val intent = Intent(this@PanelPrincipalActivity, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    /**
     * 🎓 Crea una asignatura DEMO y luego una clase de demostración dentro de ella
     * Flujo: Asignatura → Clase con contenido rico
     *
     * ✅ ACTUALIZADO: Ahora crea primero asignatura, luego clase
     */
    private fun crearAsignaturaYClaseDemo() {
        if (rolActual.isEmpty()) {
            Toast.makeText(
                this,
                "⏳ Espera a que se cargue tu perfil...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🎓 Crear Asignatura y Clase Demo")
            .setMessage("Crea una asignatura completa con:\n\n✅ Asignatura: 'Programación Móvil DEMO'\n✅ Código de acceso único\n✅ Clase: 'Introducción a Kotlin'\n✅ Contenido educativo completo\n\nPerfecto para probar todas las funcionalidades.")
            .setPositiveButton("Crear Demo") { _, _ ->
                Log.d("PanelPrincipal", "🔄 Creando asignatura y clase demo...")
                Toast.makeText(this, "⏳ Creando demostración...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    try {
                        val db = cl.duocuc.aulaviva.data.local.AppDatabase.getDatabase(this@PanelPrincipalActivity)
                        val asignaturasRepo = cl.duocuc.aulaviva.data.repository.AsignaturasRepository(
                            db.asignaturaDao(),
                            db.alumnoAsignaturaDao(),
                            cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository(
                                db.asignaturaDao(),
                                db.alumnoAsignaturaDao()
                            )
                        )
                        val clasesRepo = cl.duocuc.aulaviva.data.repository.ClaseRepository(this@PanelPrincipalActivity)

                        // PASO 1: Crear asignatura DEMO usando la firma correcta
                        val resultAsignatura = asignaturasRepo.crearAsignatura(
                            nombre = "Programación Móvil DEMO",
                            descripcion = "Asignatura de demostración con clase de prueba incluida"
                        )

                        if (resultAsignatura.isFailure) {
                            throw resultAsignatura.exceptionOrNull() ?: Exception("Error desconocido al crear asignatura")
                        }

                        val asignaturaCreada = resultAsignatura.getOrNull()!!
                        val codigoFinal = asignaturaCreada.codigoAcceso.uppercase() // MAYÚSCULAS
                        Log.d("PanelPrincipal", "✅ Asignatura demo creada con código: $codigoFinal")

                        // PASO 2: Crear clase DEMO dentro de la asignatura
                        val claseDemo = cl.duocuc.aulaviva.data.model.Clase(
                            id = java.util.UUID.randomUUID().toString(),
                            nombre = "Introducción a Kotlin para Android",
                            descripcion = """
                                📱 Clase demostrativa sobre Kotlin y desarrollo Android

                                📚 Contenido:
                                • Sintaxis básica de Kotlin
                                • Null safety y tipos de datos
                                • Funciones y lambdas
                                • Coroutines para programación asíncrona
                                • Jetpack Compose UI moderna

                                🎯 Objetivos:
                                1. Comprender conceptos fundamentales
                                2. Aplicar patrones de diseño MVVM
                                3. Integrar APIs REST con Retrofit

                                🔗 Recursos:
                                • Documentación oficial: kotlinlang.org
                                • Android Developers: developer.android.com
                            """.trimIndent(),
                            fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                            asignaturaId = asignaturaCreada.id,
                            archivoPdfUrl = "https://kotlinlang.org/docs/kotlin-reference.pdf",
                            archivoPdfNombre = "Kotlin_Reference_Demo.pdf",
                            creador = cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager.getCurrentUserId() ?: ""
                        )

                        clasesRepo.crearClaseAsync(
                            clase = claseDemo,
                            onSuccess = {
                                Log.d("PanelPrincipal", "✅ Demo completa: Asignatura + Clase con PDF creadas")
                                runOnUiThread {
                                    Toast.makeText(
                                        this@PanelPrincipalActivity,
                                        "✅ ¡Demo creada!\n\nCódigo: $codigoFinal\n\nVe a 'Mis Asignaturas' → ${asignaturaCreada.nombre}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onError = { error ->
                                Log.e("PanelPrincipal", "❌ Error creando clase demo: $error")
                                runOnUiThread {
                                    Toast.makeText(
                                        this@PanelPrincipalActivity,
                                        "⚠️ Asignatura creada, pero error en clase: $error",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )

                    } catch (e: Exception) {
                        Log.e("PanelPrincipal", "❌ Error en modo demostración", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@PanelPrincipalActivity,
                                "❌ Error al crear demo: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
