package cl.duocuc.aulaviva.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Aplica Edge-to-edge de forma segura al root de una Activity.
 * Llamar después de `setContentView(binding.root)` o usar `BaseActivity`.
 *
 * Nota: evita acumulación de padding guardando el padding inicial y aplicando
 * siempre `initial + insets` en lugar de sumar repetidamente.
 */
fun applyEdgeToEdge(activity: Activity, root: View) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)

    // Guardar padding inicial para evitar acumulación cuando se vuelvan a aplicar insets
    val initialPaddingLeft = root.paddingLeft
    val initialPaddingTop = root.paddingTop
    val initialPaddingRight = root.paddingRight
    val initialPaddingBottom = root.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = initialPaddingLeft + sysBars.left,
            top = initialPaddingTop + sysBars.top,
            right = initialPaddingRight + sysBars.right,
            bottom = initialPaddingBottom + sysBars.bottom
        )

        // Devolver los insets sin consumirlos para que hijos puedan seguir recibiéndolos.
        insets
    }

    ViewCompat.requestApplyInsets(root)
}
