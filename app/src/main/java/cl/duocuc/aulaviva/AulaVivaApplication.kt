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

            if (supabaseUrl.isEmpty() || supabaseAnonKey.isEmpty()) {
                Log.e("AulaVivaApp", "❌ Credenciales Supabase no configuradas en local.properties")
                throw IllegalStateException("Falta configurar SUPABASE_URL y SUPABASE_ANON_KEY")
            }

            SupabaseClientProvider.initialize(supabaseUrl, supabaseAnonKey)
            Log.i("AulaVivaApp", "✅ Aplicación inicializada correctamente")

        } catch (e: Exception) {
            Log.e("AulaVivaApp", "❌ Error inicializando aplicación", e)
            throw e
        }
    }
}
