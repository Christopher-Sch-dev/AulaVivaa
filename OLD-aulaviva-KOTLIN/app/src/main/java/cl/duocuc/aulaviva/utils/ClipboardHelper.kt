package cl.duocuc.aulaviva.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Helper para operaciones de clipboard.
 * Centraliza la lógica de copiar al portapapeles siguiendo principios de Clean Architecture.
 */
object ClipboardHelper {
    /**
     * Copia un texto al portapapeles.
     * @param context Context de la aplicación
     * @param label Etiqueta para el clipboard
     * @param text Texto a copiar
     * @return true si se copió exitosamente, false en caso contrario
     */
    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText(label, text)
                clipboard.setPrimaryClip(clip)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("ClipboardHelper", "Error copiando al clipboard", e)
            false
        }
    }
}

