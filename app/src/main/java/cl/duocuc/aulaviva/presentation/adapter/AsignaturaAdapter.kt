package cl.duocuc.aulaviva.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duocuc.aulaviva.databinding.ItemAsignaturaBinding
import cl.duocuc.aulaviva.data.model.Asignatura
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para mostrar las asignaturas del docente.
 * Permite ver clases, inscritos, copiar código y eliminar asignaturas.
 */
class AsignaturaAdapter(
    private val onVerClasesClick: (Asignatura) -> Unit,
    private val onVerInscritosClick: (Asignatura) -> Unit,
    private val onCopiarCodigoClick: (Asignatura) -> Unit,
    private val onEliminarClick: (Asignatura) -> Unit
) : ListAdapter<Asignatura, AsignaturaAdapter.AsignaturaViewHolder>(AsignaturaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsignaturaViewHolder {
        val binding = ItemAsignaturaBinding.inflate(
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
        private val binding: ItemAsignaturaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asignatura: Asignatura) {
            binding.apply {
                // Nombre y descripción
                textViewNombre.text = asignatura.nombre
                textViewDescripcion.text = asignatura.descripcion.ifEmpty { "Sin descripción" }

                // Código de acceso
                val textoCodigo = if (!asignatura.codigoAcceso.isNullOrEmpty()) {
                    "Código: ${asignatura.codigoAcceso}"
                } else {
                    "Sin código generado"
                }
                textViewCodigo.text = textoCodigo

                // Fecha de creación
                val fechaFormateada = try {
                    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val fecha = formato.parse(asignatura.createdAt)
                    val formatoSalida = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    "Creada: ${formatoSalida.format(fecha!!)}"
                } catch (e: Exception) {
                    "Creada: ${asignatura.createdAt}"
                }
                textViewFecha.text = fechaFormateada

                // Botones
                buttonVerClases.setOnClickListener {
                    onVerClasesClick(asignatura)
                }

                buttonVerInscritos.setOnClickListener {
                    onVerInscritosClick(asignatura)
                }

                buttonCopiarCodigo.setOnClickListener {
                    onCopiarCodigoClick(asignatura)
                }

                buttonEliminar.setOnClickListener {
                    onEliminarClick(asignatura)
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
