package cl.duocuc.aulaviva.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityAlumnoClasesBinding
// applyEdgeToEdge ahora gestionado por BaseActivity
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.presentation.adapter.ClaseAdapter
import cl.duocuc.aulaviva.presentation.ui.clases.DetalleClaseActivity
import cl.duocuc.aulaviva.presentation.viewmodel.ClaseViewModel

/**
 * Activity para alumnos: ver clases de una asignatura (READ-ONLY).
 * No permite editar/eliminar, solo visualizar y usar funciones IA.
 */
class AlumnoClasesActivity : BaseActivity() {

    private lateinit var binding: ActivityAlumnoClasesBinding
    private val viewModel: ClaseViewModel by viewModels()
    private lateinit var adapter: ClaseAdapter
    private var asignaturaId: String = ""
    private var asignaturaNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlumnoClasesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener asignatura del intent
        asignaturaId = intent.getStringExtra("ASIGNATURA_ID") ?: ""
        asignaturaNombre = intent.getStringExtra("ASIGNATURA_NOMBRE") ?: "Asignatura"

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        // Sincronizar solo las clases de esta asignatura (más seguro y rápido)
        if (asignaturaId.isNotEmpty()) {
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = asignaturaNombre
            subtitle = "Clases"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        // Adapter READ-ONLY para alumno (oculta botones de edición)
        adapter = ClaseAdapter(
            onClaseClick = { clase ->
                abrirDetalleClase(clase)
            },
            onEditarClick = null, // Alumno no puede editar
            onEliminarClick = null, // Alumno no puede eliminar
            esAlumno = true // ✅ FLAG para ocultar botones
        )

        binding.recyclerViewClases.apply {
            layoutManager = LinearLayoutManager(this@AlumnoClasesActivity)
            adapter = this@AlumnoClasesActivity.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Recargar solo las clases de la asignatura para no afectar otras entradas locales
            viewModel.sincronizarClasesPorAsignatura(asignaturaId)
        }
    }

    private fun observeViewModel() {
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

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

    private fun abrirDetalleClase(clase: Clase) {
        val intent = Intent(this, DetalleClaseActivity::class.java)
        intent.putExtra("CLASE_ID", clase.id)
        intent.putExtra("ES_ALUMNO", true) // Flag para ocultar botones editar
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
