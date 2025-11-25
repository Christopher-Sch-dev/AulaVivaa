package cl.duocuc.aulaviva.presentation.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import cl.duocuc.aulaviva.databinding.DialogCrearAsignaturaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Diálogo para crear una nueva asignatura.
 * Solicita nombre y descripción.
 */
class CrearAsignaturaDialog(
    private val onCrear: (nombre: String, descripcion: String) -> Unit
) : DialogFragment() {

    private var _binding: DialogCrearAsignaturaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCrearAsignaturaBinding.inflate(LayoutInflater.from(context))

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Asignatura")
            .setView(binding.root)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = binding.editTextNombre.text.toString().trim()
                val descripcion = binding.editTextDescripcion.text.toString().trim()

                if (nombre.isNotEmpty()) {
                    onCrear(nombre, descripcion)
                } else {
                    // El ViewModel ya maneja la validación
                    onCrear(nombre, descripcion)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
