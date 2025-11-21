package cl.duocuc.aulaviva

import android.app.Application
import android.util.Log
import cl.duocuc.aulaviva.data.supabase.SupabaseClientProvider

/**
 * Clase Application personalizada.
 * Se ejecuta ANTES de cualquier Activity.
 *
 * Inicializa Supabase una sola vez al arrancar la app.
 */
class AulaVivaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Supabase
        try {
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

            // ✅ CAMBIO: Permitir arrancar app sin Supabase (modo offline)
            if (supabaseUrl.isEmpty() || supabaseAnonKey.isEmpty()) {
                Log.w("AulaVivaApp", "⚠️ Credenciales Supabase no configuradas - Modo OFFLINE ONLY")
                Log.w("AulaVivaApp", "⚠️ Configura SUPABASE_URL y SUPABASE_ANON_KEY en local.properties")
                // No lanzar excepción, permitir que la app inicie
            } else {
                SupabaseClientProvider.initialize(supabaseUrl, supabaseAnonKey)
                Log.i("AulaVivaApp", "✅ Aplicación inicializada con Supabase correctamente")
            }

        } catch (e: Exception) {
            Log.e("AulaVivaApp", "❌ Error inicializando aplicación", e)
            // No lanzar excepción, permitir que la app continúe
        }
    }
}
