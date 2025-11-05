package cl.duocuc.aulaviva.presentation.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * 📄 Utilidad centralizada para abrir PDFs con Intent Externo.
 *
 * Solución profesional usada por apps como Google Drive, Dropbox, OneDrive.
 * Abre PDFs con el visor nativo del sistema (Google PDF Viewer, Adobe, etc.)
 * con fallback automático a navegador si no hay visor instalado.
 */
object PdfUtils {

    private const val TAG = "PdfUtils"

    /**
     * Abre PDF con Intent Externo (visor nativo del sistema).
     *
     * @param context Contexto actual (Activity)
     * @param pdfUrl URL del PDF (http/https)
     * @return true si se abrió correctamente, false si falló
     */
    fun abrirPdfExterno(context: Context, pdfUrl: String?): Boolean {
        // Validar URL
        if (pdfUrl.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ URL del PDF está vacía")
            Toast.makeText(context, "Esta clase no tiene PDF", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            Log.d(TAG, "🔍 Intentando abrir PDF: $pdfUrl")

            // OPCIÓN 1: Intent para visor PDF nativo (RECOMENDADO)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Verificar que hay una app que pueda abrir PDFs
            if (intent.resolveActivity(context.packageManager) != null) {
                Log.d(TAG, "✅ Visor PDF nativo encontrado. Abriendo...")
                context.startActivity(intent)
                return true
            } else {
                // OPCIÓN 2: Fallback a navegador (si no hay visor)
                Log.w(TAG, "⚠️ No hay visor PDF nativo. Intentando navegador...")
                return abrirEnNavegador(context, pdfUrl)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error abriendo PDF: ${e.message}", e)
            // OPCIÓN 3: Último fallback (si Intent falla)
            return abrirEnNavegador(context, pdfUrl)
        }
    }

    /**
     * Fallback: Abre PDF en navegador (Chrome, Firefox, etc).
     */
    private fun abrirEnNavegador(context: Context, pdfUrl: String): Boolean {
        return try {
            Log.d(TAG, "🌐 Abriendo en navegador: $pdfUrl")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(pdfUrl)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "✅ Navegador abierto correctamente")
                true
            } else {
                Log.e(TAG, "❌ No hay navegador ni visor PDF instalado")
                Toast.makeText(
                    context,
                    "No hay navegador ni visor PDF instalado",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en fallback navegador: ${e.message}", e)
            Toast.makeText(
                context,
                "Error al abrir PDF: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}
