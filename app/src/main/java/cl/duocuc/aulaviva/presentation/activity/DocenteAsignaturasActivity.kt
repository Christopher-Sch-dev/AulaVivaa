package cl.duocuc.aulaviva.presentation.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityDocenteAsignaturasBinding
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.adapter.AsignaturaAdapter
import cl.duocuc.aulaviva.presentation.dialog.CrearAsignaturaDialog
import cl.duocuc.aulaviva.presentation.dialog.MostrarCodigoDialog
import cl.duocuc.aulaviva.presentation.viewmodel.AsignaturasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Activity para gestionar las asignaturas del docente.
 * Permite crear, ver clases, visualizar códigos de acceso y eliminar asignaturas.
 */
class DocenteAsignaturasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocenteAsignaturasBinding
    private val viewModel: AsignaturasViewModel by viewModels()
    private lateinit var adapter: AsignaturaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocenteAsignaturasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFAB()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Mis Asignaturas"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        adapter = AsignaturaAdapter(
            onVerClasesClick = { asignatura ->
                abrirClasesAsignatura(asignatura)
            },
            onVerInscritosClick = { asignatura ->
                abrirInscritosAsignatura(asignatura)
            },
            onCopiarCodigoClick = { asignatura ->
                copiarCodigoAlPortapapeles(asignatura)
            },
            onEliminarClick = { asignatura ->
                confirmarEliminar(asignatura)
            }
        )

        binding.recyclerViewAsignaturas.apply {
            layoutManager = LinearLayoutManager(this@DocenteAsignaturasActivity)
            adapter = this@DocenteAsignaturasActivity.adapter
        }
    }

    private fun setupFAB() {
        binding.fabCrearAsignatura.setOnClickListener {
            mostrarDialogCrear()
        }
    }

    private fun observeViewModel() {
        // Observar lista de asignaturas
        viewModel.asignaturas.observe(this) { asignaturas ->
            adapter.submitList(asignaturas)

            // Mostrar mensaje si no hay asignaturas
            if (asignaturas.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewAsignaturas.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewAsignaturas.visibility = View.VISIBLE
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Observar éxito en operaciones
        viewModel.operationSuccess.observe(this) { mensaje ->
            mensaje?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Observar código generado
        viewModel.codigoGenerado.observe(this) { codigo ->
            codigo?.let {
                mostrarCodigoGenerado(it)
            }
        }
    }

    private fun mostrarDialogCrear() {
        val dialog = CrearAsignaturaDialog { nombre, descripcion ->
            viewModel.crearAsignatura(nombre, descripcion)
        }
        dialog.show(supportFragmentManager, "CrearAsignaturaDialog")
    }

    /**
     * Abre la pantalla de clases de la asignatura.
     */
    private fun abrirClasesAsignatura(asignatura: Asignatura) {
        val intent = Intent(this, DocenteClasesActivity::class.java)
        intent.putExtra("ASIGNATURA_ID", asignatura.id)
        intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
        startActivity(intent)
    }

    /**
     * Abre la pantalla de alumnos inscritos en la asignatura.
     */
    private fun abrirInscritosAsignatura(asignatura: Asignatura) {
        val intent = Intent(this, InscritosActivity::class.java)
        intent.putExtra("ASIGNATURA_ID", asignatura.id)
        intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
        startActivity(intent)
    }

    /**
     * Copia el código de acceso al portapapeles.
     */
    private fun copiarCodigoAlPortapapeles(asignatura: Asignatura) {
        if (asignatura.codigoAcceso.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ Esta asignatura no tiene código generado", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código Asignatura", asignatura.codigoAcceso)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            this,
            "✅ Código copiado: ${asignatura.codigoAcceso}\n\nCompártelo con tus alumnos",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun mostrarCodigoAcceso(asignatura: Asignatura) {
        if (asignatura.codigoAcceso.isNullOrEmpty()) {
            // Generar código si no existe
            MaterialAlertDialogBuilder(this)
                .setTitle("Generar Código")
                .setMessage("Esta asignatura no tiene código de acceso. ¿Deseas generar uno?")
                .setPositiveButton("Generar") { _, _ ->
                    viewModel.generarCodigo(asignatura.id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            // Mostrar código existente
            val dialog = MostrarCodigoDialog.newInstance(
                asignatura.nombre,
                asignatura.codigoAcceso
            )
            dialog.show(supportFragmentManager, "MostrarCodigoDialog")
        }
    }

    private fun mostrarCodigoGenerado(codigo: String) {
        // Este método se llama cuando se genera un código nuevo
        Toast.makeText(this, "Código generado: $codigo", Toast.LENGTH_LONG).show()
    }

    private fun confirmarEliminar(asignatura: Asignatura) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Asignatura")
            .setMessage("¿Estás seguro de eliminar '${asignatura.nombre}'?\n\nEsta acción no se puede deshacer y eliminará todas las clases asociadas.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarAsignatura(asignatura.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
