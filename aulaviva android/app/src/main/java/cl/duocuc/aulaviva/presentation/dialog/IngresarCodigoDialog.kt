package cl.duocuc.aulaviva.presentation.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import cl.duocuc.aulaviva.databinding.DialogIngresarCodigoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Diálogo para ingresar código de asignatura.
 * Valida formato básico antes de enviar.
 */
class IngresarCodigoDialog(
    private val onIngresar: (codigo: String) -> Unit
) : DialogFragment() {

    private var _binding: DialogIngresarCodigoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogIngresarCodigoBinding.inflate(LayoutInflater.from(context))

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Asignatura")
            .setView(binding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val codigo = binding.editTextCodigo.text.toString().trim().uppercase()

                if (validarCodigo(codigo)) {
                    onIngresar(codigo)
                } else {
                    binding.layoutCodigo.error = "Código inválido. Formato: XXX####-XXXX"
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Valida formato código: 3 letras + 4 dígitos + guion + 4 caracteres.
     * Ejemplo válido: POO2025-A3F9
     */
    private fun validarCodigo(codigo: String): Boolean {
        // Regex: 3 letras + 4 dígitos + guion + 4 alfanuméricos
        val regex = Regex("^[A-Z]{3}\\d{4}-[A-Z0-9]{4}$")
        return regex.matches(codigo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
