package cl.duocuc.aulaviva.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object PdfUtils {

    private const val TAG = "PdfUtils"

    /**
     * Abre un PDF con el visor nativo o con el navegador como fallback.
     * Si se llama con un `Context` que no es Activity añade `FLAG_ACTIVITY_NEW_TASK`.
     */
    fun abrirPdfExterno(context: Context, pdfUrl: String?): Boolean {
        if (pdfUrl.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ URL del PDF está vacía")
            Toast.makeText(context, "Esta clase no tiene PDF", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            Log.d(TAG, "🔍 Intentando abrir PDF: $pdfUrl")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(pdfUrl), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Si el context no es Activity, necesitamos NEW_TASK
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            return if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                abrirEnNavegador(context, pdfUrl)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error abriendo PDF: ${e.message}", e)
            return abrirEnNavegador(context, pdfUrl)
        }
    }

    private fun abrirEnNavegador(context: Context, pdfUrl: String): Boolean {
        return try {
            Log.d(TAG, "🌐 Abriendo en navegador: $pdfUrl")
            val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(pdfUrl) }
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "✅ Navegador abierto correctamente")
                true
            } else {
                Log.e(TAG, "❌ No hay navegador ni visor PDF instalado")
                Toast.makeText(context, "No hay navegador ni visor PDF instalado", Toast.LENGTH_LONG).show()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en fallback navegador: ${e.message}", e)
            Toast.makeText(context, "Error al abrir PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
