package cl.duocuc.aulaviva.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.databinding.ItemAsignaturaAlumnoBinding
import cl.duocuc.aulaviva.data.model.Asignatura

/**
 * Adapter para mostrar asignaturas en el panel del alumno
 */
class AsignaturaAlumnoAdapter(
    private val onAsignaturaClick: (Asignatura) -> Unit
) : RecyclerView.Adapter<AsignaturaAlumnoAdapter.ViewHolder>() {

    private var asignaturas: List<Asignatura> = emptyList()

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
        holder.bind(asignaturas[position])
    }

    override fun getItemCount() = asignaturas.size

    fun updateList(nuevasAsignaturas: List<Asignatura>) {
        asignaturas = nuevasAsignaturas
        notifyDataSetChanged()
    }
}
