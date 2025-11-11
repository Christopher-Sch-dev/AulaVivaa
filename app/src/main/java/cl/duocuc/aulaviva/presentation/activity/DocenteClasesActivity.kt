package cl.duocuc.aulaviva.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityDocenteClasesBinding
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.adapter.ClaseAdapter
import cl.duocuc.aulaviva.presentation.ui.clases.DetalleClaseActivity
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel

/**
 * Activity para docentes: ver y gestionar clases de una asignatura específica.
 * Permite crear, editar y eliminar clases dentro de una asignatura.
 */
class DocenteClasesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocenteClasesBinding
    private val viewModel: ClaseViewModel by viewModels()
    private lateinit var adapter: ClaseAdapter
    private var asignaturaId: String = ""
    private var asignaturaNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocenteClasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener asignatura del intent
        asignaturaId = intent.getStringExtra("ASIGNATURA_ID") ?: ""
        asignaturaNombre = intent.getStringExtra("ASIGNATURA_NOMBRE") ?: "Asignatura"

        if (asignaturaId.isEmpty()) {
            Toast.makeText(this, "Error: No se especificó asignatura", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupFAB()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = asignaturaNombre
            subtitle = "Gestionar Clases"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        // Adapter con callbacks completos (docente puede editar/eliminar)
        adapter = ClaseAdapter(
            onClaseClick = { clase ->
                abrirDetalleClase(clase)
            },
            onEditarClick = { clase ->
                editarClase(clase)
            },
            onEliminarClick = { clase ->
                confirmarEliminarClase(clase)
            }
        )

        binding.recyclerViewClases.apply {
            layoutManager = LinearLayoutManager(this@DocenteClasesActivity)
            adapter = this@DocenteClasesActivity.adapter
        }
    }

    private fun setupFAB() {
        binding.fabCrearClase.setOnClickListener {
            crearNuevaClase()
        }
    }

    private fun observeViewModel() {
        // Obtener clases de esta asignatura
        viewModel.obtenerClasesPorAsignatura(asignaturaId).observe(this) { clases ->
            adapter.updateList(clases)

            if (clases.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewClases.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewClases.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Crea una nueva clase para esta asignatura.
     * Pasa asignaturaId pre-asignado a la siguiente activity.
     */
    private fun crearNuevaClase() {
        val intent = Intent(this, cl.duocuc.aulaviva.presentation.ui.clases.CrearEditarClaseActivity::class.java)
        intent.putExtra("ASIGNATURA_ID", asignaturaId)
        intent.putExtra("ASIGNATURA_NOMBRE", asignaturaNombre)
        startActivity(intent)
    }

    /**
     * Edita una clase existente.
     */
    private fun editarClase(clase: Clase) {
        val intent = Intent(this, cl.duocuc.aulaviva.presentation.ui.clases.CrearEditarClaseActivity::class.java)
        intent.putExtra("CLASE_ID", clase.id)
        intent.putExtra("ASIGNATURA_ID", asignaturaId)
        intent.putExtra("ASIGNATURA_NOMBRE", asignaturaNombre)
        startActivity(intent)
    }

    /**
     * Abre el detalle de la clase.
     */
    private fun abrirDetalleClase(clase: Clase) {
        val intent = Intent(this, DetalleClaseActivity::class.java)
        intent.putExtra("CLASE_ID", clase.id)
        intent.putExtra("ES_ALUMNO", false) // Docente puede ver botones editar
        startActivity(intent)
    }

    /**
     * Confirma y elimina una clase.
     */
    private fun confirmarEliminarClase(clase: Clase) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Clase")
            .setMessage("¿Estás seguro de eliminar '${clase.nombre}'?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarClase(
                    claseId = clase.id,
                    onSuccess = {
                        Toast.makeText(this, "✅ Clase eliminada", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refrescar lista al volver de crear/editar
        viewModel.sincronizarConSupabase()
    }
}
