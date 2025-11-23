package cl.duocuc.aulaviva.presentation.legacy.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Legacy placeholder. Utilities were consolidated to `cl.duocuc.aulaviva.utils`.
 * Mantengo este archivo en una package `presentation.legacy` para evitar referencias rotas
 * durante la migración. No usar directamente.
 */
@Deprecated("Moved to cl.duocuc.aulaviva.utils.EdgeToEdge - do not use legacy version")
fun applyEdgeToEdge_legacy(activity: Activity, root: View) {
    // No-op legacy shim
}
