package cl.duocuc.aulaviva.presentation.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.databinding.ItemInscritoBinding
import cl.duocuc.aulaviva.data.local.AlumnoAsignaturaEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter simple para mostrar alumnos inscritos.
 */
class InscritosAdapter : ListAdapter<AlumnoAsignaturaEntity, InscritosAdapter.InscritoViewHolder>(InscritoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InscritoViewHolder {
        val binding = ItemInscritoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InscritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InscritoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InscritoViewHolder(
        private val binding: ItemInscritoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inscrito: AlumnoAsignaturaEntity) {
            binding.apply {
                // Mostrar alumno_id (en producción sería mejor obtener el email/nombre del alumno)
                val alumnoId = inscrito.alumnoId.take(8) // Primeros 8 caracteres del ID
                textViewAlumnoId.text = "Alumno: $alumnoId..."

                // Fecha de inscripción
                val fechaFormateada = try {
                    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val fecha = formato.parse(inscrito.fechaInscripcion)
                    val formatoSalida = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    "Inscrito: ${formatoSalida.format(fecha!!)}"
                } catch (e: Exception) {
                    "Inscrito: ${inscrito.fechaInscripcion}"
                }
                textViewFechaInscripcion.text = fechaFormateada

                // Estado
                val estadoTexto = when (inscrito.estado) {
                    "activo" -> "✅ Activo"
                    "inactivo" -> "❌ Inactivo"
                    else -> inscrito.estado
                }
                textViewEstado.text = estadoTexto
            }
        }
    }

    private class InscritoDiffCallback : DiffUtil.ItemCallback<AlumnoAsignaturaEntity>() {
        override fun areItemsTheSame(oldItem: AlumnoAsignaturaEntity, newItem: AlumnoAsignaturaEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlumnoAsignaturaEntity, newItem: AlumnoAsignaturaEntity): Boolean {
            return oldItem == newItem
        }
    }
}
