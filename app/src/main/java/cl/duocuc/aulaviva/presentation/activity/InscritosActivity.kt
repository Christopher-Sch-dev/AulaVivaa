package cl.duocuc.aulaviva.presentation.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityInscritosBinding
// applyEdgeToEdge aplicado desde BaseActivity
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import cl.duocuc.aulaviva.presentation.viewmodel.InscritosViewModel

/**
 * Activity para ver los alumnos inscritos en una asignatura.
 * Solo accesible para docentes.
 */
class InscritosActivity : BaseActivity() {
    private lateinit var binding: ActivityInscritosBinding
    private val viewModel: InscritosViewModel by viewModels()
    private lateinit var adapter: InscritosAdapter
    private var asignaturaId: String = ""
    private var asignaturaNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInscritosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge aplicado automáticamente por BaseActivity

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
        setupSwipeRefresh()
        cargarInscritos()

        // Sincronizar al abrir
        viewModel.sincronizarInscritos(asignaturaId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Alumnos Inscritos"
            subtitle = asignaturaNombre
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        adapter = InscritosAdapter()
        binding.recyclerViewInscritos.apply {
            layoutManager = LinearLayoutManager(this@InscritosActivity)
            adapter = this@InscritosActivity.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sincronizarInscritos(asignaturaId)
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // Observar errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cargarInscritos() {
        val database = AppDatabase.getDatabase(this@InscritosActivity)

        // Observar Flow de inscritos
        database.alumnoAsignaturaDao()
            .obtenerInscripcionesPorAsignatura(asignaturaId)
            .asLiveData()
            .observe(this) { inscritos: List<AlumnoAsignaturaEntity> ->
                if (inscritos.isEmpty()) {
                    binding.textViewEmpty.visibility = View.VISIBLE
                    binding.recyclerViewInscritos.visibility = View.GONE
                    binding.textViewTotal.text = "Total: 0 alumnos"
                } else {
                    binding.textViewEmpty.visibility = View.GONE
                    binding.recyclerViewInscritos.visibility = View.VISIBLE
                    binding.textViewTotal.text = "Total: ${inscritos.size} alumnos"
                    adapter.submitList(inscritos)
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
