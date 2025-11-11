package cl.duocuc.aulaviva.presentation.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import cl.duocuc.aulaviva.databinding.DialogMostrarCodigoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Diálogo para mostrar el código de acceso de una asignatura.
 * Permite copiar el código al portapapeles.
 */
class MostrarCodigoDialog : DialogFragment() {

    private var _binding: DialogMostrarCodigoBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_NOMBRE = "nombre"
        private const val ARG_CODIGO = "codigo"

        fun newInstance(nombre: String, codigo: String): MostrarCodigoDialog {
            return MostrarCodigoDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOMBRE, nombre)
                    putString(ARG_CODIGO, codigo)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMostrarCodigoBinding.inflate(LayoutInflater.from(context))

        val nombre = arguments?.getString(ARG_NOMBRE) ?: ""
        val codigo = arguments?.getString(ARG_CODIGO) ?: ""

        binding.apply {
            textViewNombreAsignatura.text = nombre
            textViewCodigo.text = codigo

            buttonCopiar.setOnClickListener {
                copiarAlPortapapeles(codigo)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Código de Acceso")
            .setView(binding.root)
            .setPositiveButton("Cerrar", null)
            .create()
    }

    private fun copiarAlPortapapeles(codigo: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código de Acceso", codigo)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Código copiado al portapapeles",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
