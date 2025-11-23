package cl.duocuc.aulaviva.presentation.legacy.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Legacy placeholder. Utilities were consolidated to `cl.duocuc.aulaviva.utils.PdfUtils`.
 * Mantengo este archivo en `presentation.legacy` para evitar referencias rotas.
 */
@Deprecated("Moved to cl.duocuc.aulaviva.utils.PdfUtils - do not use legacy version")
object PdfUtils_Legacy {
    fun abrirPdfExterno(context: Context, pdfUrl: String?): Boolean {
        // No-op legacy shim; use cl.duocuc.aulaviva.utils.PdfUtils
        return cl.duocuc.aulaviva.utils.PdfUtils.abrirPdfExterno(context, pdfUrl)
    }
}
