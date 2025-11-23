package cl.duocuc.aulaviva.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import cl.duocuc.aulaviva.presentation.base.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cl.duocuc.aulaviva.databinding.ActivityAlumnoAsignaturasBinding
// applyEdgeToEdge aplicado desde BaseActivity
import cl.duocuc.aulaviva.data.model.Asignatura
import cl.duocuc.aulaviva.presentation.adapter.AlumnoAsignaturaAdapter
import cl.duocuc.aulaviva.presentation.dialog.IngresarCodigoDialog
import cl.duocuc.aulaviva.presentation.viewmodel.AlumnoViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Activity para alumnos: ver asignaturas inscritas y agregar nuevas con código.
 */
class AlumnoAsignaturasActivity : BaseActivity() {

    private lateinit var binding: ActivityAlumnoAsignaturasBinding
    private val viewModel: AlumnoViewModel by viewModels()
    private lateinit var adapter: AlumnoAsignaturaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlumnoAsignaturasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge aplicado automáticamente por BaseActivity

        setupToolbar()
        setupRecyclerView()
        setupFAB()
        setupSwipeRefresh()
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
        adapter = AlumnoAsignaturaAdapter(
            onVerClasesClick = { asignatura ->
                abrirClasesAsignatura(asignatura)
            },
            onDarDeBajaClick = { asignatura ->
                confirmarDarDeBaja(asignatura)
            }
        )

        binding.recyclerViewAsignaturas.apply {
            layoutManager = LinearLayoutManager(this@AlumnoAsignaturasActivity)
            adapter = this@AlumnoAsignaturasActivity.adapter
        }
    }

    private fun setupFAB() {
        binding.fabAgregarCodigo.setOnClickListener {
            mostrarDialogCodigo()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sincronizarAsignaturasInscritas()
        }
    }

    private fun observeViewModel() {
        // Observar asignaturas inscritas
        viewModel.asignaturasInscritas.observe(this) { asignaturas ->
            adapter.submitList(asignaturas)

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
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // Observar errores
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Observar inscripción exitosa
        viewModel.inscripcionExitosa.observe(this) { asignatura ->
            asignatura?.let {
                Toast.makeText(this, "✅ Inscrito en: ${it.nombre}", Toast.LENGTH_LONG).show()
                viewModel.limpiarInscripcionExitosa()
            }
        }
    }

    private fun mostrarDialogCodigo() {
        val dialog = IngresarCodigoDialog { codigo ->
            viewModel.inscribirConCodigo(codigo)
        }
        dialog.show(supportFragmentManager, "IngresarCodigoDialog")
    }

    private fun abrirClasesAsignatura(asignatura: Asignatura) {
        val intent = Intent(this, AlumnoClasesActivity::class.java)
        intent.putExtra("ASIGNATURA_ID", asignatura.id)
        intent.putExtra("ASIGNATURA_NOMBRE", asignatura.nombre)
        startActivity(intent)
    }

    private fun confirmarDarDeBaja(asignatura: Asignatura) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Darse de Baja")
            .setMessage("¿Estás seguro de que deseas darte de baja de '${asignatura.nombre}'?\n\nDejarás de ver las clases de esta asignatura.")
            .setPositiveButton("Dar de Baja") { _, _ ->
                viewModel.darDeBaja(asignatura.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
