package cl.duocuc.aulaviva.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityPanelAlumnoBinding
import cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager
import cl.duocuc.aulaviva.presentation.activity.AlumnoClasesActivity
import cl.duocuc.aulaviva.presentation.ui.auth.LoginActivity
import cl.duocuc.aulaviva.presentation.adapter.AsignaturaAlumnoAdapter
import cl.duocuc.aulaviva.presentation.viewmodel.AlumnoViewModel
import cl.duocuc.aulaviva.data.model.Asignatura
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * Panel para alumnos con funcionalidad completa:
 * - Inscribirse con código
 * - Ver asignaturas inscritas
 * - Acceder a clases de cada asignatura
 */
class PanelAlumnoActivity : BaseActivity() {

    private lateinit var binding: ActivityPanelAlumnoBinding
    private val viewModel: AlumnoViewModel by viewModels()
    private lateinit var adapter: AsignaturaAlumnoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelAlumnoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Sincronizar asignaturas al inicio
        viewModel.sincronizarAsignaturasInscritas()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        adapter = AsignaturaAlumnoAdapter { asignatura ->
            abrirClasesDeAsignatura(asignatura)
        }

        binding.recyclerViewAsignaturas.apply {
            layoutManager = LinearLayoutManager(this@PanelAlumnoActivity)
            adapter = this@PanelAlumnoActivity.adapter
        }
    }

    private fun setupButtons() {
        binding.btnInscribirseCodigo.setOnClickListener {
            mostrarDialogoInscripcion()
        }

        binding.fabLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun observeViewModel() {
        // Obtener asignaturas donde el alumno está inscrito
        viewModel.asignaturasInscritas.observe(this) { asignaturas ->
            if (asignaturas.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewAsignaturas.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewAsignaturas.visibility = View.VISIBLE
                adapter.updateList(asignaturas)
            }
        }

        viewModel.inscripcionExitosa.observe(this) { asignatura ->
            asignatura?.let {
                Toast.makeText(this, "✅ Inscrito en: ${it.nombre}", Toast.LENGTH_SHORT).show()
                viewModel.limpiarInscripcionExitosa()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.limpiarError()
            }
        }
    }

    private fun mostrarDialogoInscripcion() {
        val dialogView = layoutInflater.inflate(
            cl.duocuc.aulaviva.R.layout.dialog_inscribirse_codigo,
            null
        )

        val inputLayout = dialogView.findViewById<TextInputLayout>(cl.duocuc.aulaviva.R.id.layoutCodigoAcceso)
        val inputCodigo = dialogView.findViewById<TextInputEditText>(cl.duocuc.aulaviva.R.id.inputCodigoAcceso)

        val dialog = AlertDialog.Builder(this)
            .setTitle("📝 Inscribirse a Asignatura")
            .setMessage("Ingresa el código de acceso proporcionado por tu docente\n\nEjemplos:\n• TEMP-7656\n• TG025-PAEK")
            .setView(dialogView)
            .setPositiveButton("Inscribirse", null) // Lo manejamos manualmente
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val codigo = inputCodigo.text.toString().trim()
                if (codigo.isEmpty()) {
                    inputLayout.error = "Debes ingresar un código"
                } else {
                    inputLayout.error = null
                    dialog.dismiss()
                    inscribirseConCodigo(codigo)
                }
            }
        }

        dialog.show()
    }

    private fun inscribirseConCodigo(codigo: String) {
        android.util.Log.d("PanelAlumno", "🎯 Intentando inscribir con código: '$codigo'")
        viewModel.inscribirConCodigo(codigo)
    }

    private fun abrirClasesDeAsignatura(asignatura: Asignatura) {
        val intent = Intent(this, AlumnoClasesActivity::class.java)
        intent.putExtra("ASIGNATURA_ID", asignatura.id)
        intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
        startActivity(intent)
    }

    private fun cerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que quieres salir de Aula Viva?")
            .setPositiveButton("Sí, salir") { _, _ ->
                lifecycleScope.launch {
                    SupabaseAuthManager.logout()
                    val intent = Intent(this@PanelAlumnoActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refrescar al volver
        viewModel.sincronizarAsignaturasInscritas()
    }
}
