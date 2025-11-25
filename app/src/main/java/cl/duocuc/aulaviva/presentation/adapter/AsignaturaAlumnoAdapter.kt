package cl.duocuc.aulaviva.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.databinding.ItemAsignaturaAlumnoBinding
import cl.duocuc.aulaviva.data.model.Asignatura

/**
 * ListAdapter para mostrar asignaturas en el panel del alumno de forma eficiente.
 */
class AsignaturaAlumnoAdapter(
    private val onAsignaturaClick: (Asignatura) -> Unit
) : ListAdapter<Asignatura, AsignaturaAlumnoAdapter.ViewHolder>(AsignaturaDiff()) {

    inner class ViewHolder(private val binding: ItemAsignaturaAlumnoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(asignatura: Asignatura) {
            binding.textNombreAsignatura.text = asignatura.nombre
            binding.textDescripcionAsignatura.text = asignatura.descripcion
            binding.textCodigoAcceso.text = "Código: ${asignatura.codigoAcceso}"

            binding.root.setOnClickListener {
                onAsignaturaClick(asignatura)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAsignaturaAlumnoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateList(nuevasAsignaturas: List<Asignatura>) {
        submitList(nuevasAsignaturas)
    }

    class AsignaturaDiff : DiffUtil.ItemCallback<Asignatura>() {
        override fun areItemsTheSame(oldItem: Asignatura, newItem: Asignatura): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asignatura, newItem: Asignatura): Boolean = oldItem == newItem
    }
}
