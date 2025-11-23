package cl.duocuc.aulaviva.presentation.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Aplica Edge-to-edge de forma segura al root de una Activity.
 * Llamar después de `setContentView(binding.root)`.
 */
fun applyEdgeToEdge(activity: Activity, root: View) {
    // Permitimos que la app dibuje por detrás de las system bars.
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)

    // Aplicar insets: mantenemos padding existente y agregamos los insets del sistema.
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = v.paddingLeft + sysBars.left,
            top = v.paddingTop + sysBars.top,
            right = v.paddingRight + sysBars.right,
            bottom = v.paddingBottom + sysBars.bottom
        )

        // Devolver los insets sin consumirlos para que hijos puedan seguir recibiéndolos.
        insets
    }

    // Forzar la aplicación inmediata de insets
    ViewCompat.requestApplyInsets(root)
}
