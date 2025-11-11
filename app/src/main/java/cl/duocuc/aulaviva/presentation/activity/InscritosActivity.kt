package cl.duocuc.aulaviva.presentation.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityInscritosBinding
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import kotlinx.coroutines.launch

/**
 * Activity para ver los alumnos inscritos en una asignatura.
 * Solo accesible para docentes.
 */
class InscritosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInscritosBinding
    private lateinit var adapter: InscritosAdapter
    private var asignaturaId: String = ""
    private var asignaturaNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInscritosBinding.inflate(layoutInflater)
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
        cargarInscritos()
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

    private fun cargarInscritos() {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@InscritosActivity)

                // Observar Flow de inscritos
                database.alumnoAsignaturaDao()
                    .obtenerInscripcionesPorAsignatura(asignaturaId)
                    .collect { inscritos ->
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
            } catch (e: Exception) {
                Toast.makeText(
                    this@InscritosActivity,
                    "Error al cargar inscritos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
