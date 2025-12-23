package cl.duocuc.aulaviva.utils

/**
 * Constantes de la aplicación.
 * Centraliza valores mágicos y configuración.
 */
object Constants {
    // Delays y timeouts
    const val REFRESH_DELAY_MS = 500L
    const val SNACKBAR_DURATION_SHORT_MS = 2000L

    // Límites de validación
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PDF_SIZE_MB = 50

    // Nombres de servicios del sistema
    const val CLIPBOARD_SERVICE = "clipboard"

    // Etiquetas para clipboard
    const val CLIPBOARD_LABEL_CODIGO = "Código"

    // Mensajes de éxito
    const val MSG_DATOS_ACTUALIZADOS = "✓ Datos actualizados"
    const val MSG_CODIGO_COPIADO = "✓ Código copiado: %s"
    const val MSG_ASIGNATURA_ELIMINADA = "✓ Asignatura eliminada"
    const val MSG_CLASE_ELIMINADA = "✓ Clase eliminada"
    const val MSG_BAJA_EXITOSA = "✓ Te has dado de baja de %s"

    // Mensajes de error
    const val MSG_ERROR_ELIMINAR_CON_CLASES = "⚠️ No se puede eliminar: la asignatura tiene clases asociadas"

    // Padding y spacing estándar
    const val PADDING_STANDARD_DP = 16
    const val PADDING_SMALL_DP = 8
    const val SPACING_ITEMS_DP = 12
}

