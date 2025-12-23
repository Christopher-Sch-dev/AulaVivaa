package cl.duocuc.aulaviva.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.databinding.ItemAlumnoAsignaturaBinding
import cl.duocuc.aulaviva.data.model.Asignatura

/**
 * Adapter para mostrar asignaturas inscritas del alumno.
 * Permite ver clases y darse de baja.
 */
class AlumnoAsignaturaAdapter(
    private val onVerClasesClick: (Asignatura) -> Unit,
    private val onDarDeBajaClick: (Asignatura) -> Unit
) : ListAdapter<Asignatura, AlumnoAsignaturaAdapter.AsignaturaViewHolder>(AsignaturaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsignaturaViewHolder {
        val binding = ItemAlumnoAsignaturaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AsignaturaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AsignaturaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AsignaturaViewHolder(
        private val binding: ItemAlumnoAsignaturaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asignatura: Asignatura) {
            binding.apply {
                textViewNombre.text = asignatura.nombre
                textViewDescripcion.text = asignatura.descripcion.ifEmpty { "Sin descripción" }

                // Botones
                buttonVerClases.setOnClickListener {
                    onVerClasesClick(asignatura)
                }

                buttonDarDeBaja.setOnClickListener {
                    onDarDeBajaClick(asignatura)
                }
            }
        }
    }

    private class AsignaturaDiffCallback : DiffUtil.ItemCallback<Asignatura>() {
        override fun areItemsTheSame(oldItem: Asignatura, newItem: Asignatura): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Asignatura, newItem: Asignatura): Boolean {
            return oldItem == newItem
        }
    }
}
