package cl.duocuc.aulaviva.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Cliente Supabase centralizado.
 * Inicializar SOLO UNA VEZ en el Application onCreate.
 *
 * Proporciona acceso a:
 * - Auth (login, registro, sesión)
 * - Postgrest (CRUD en tablas Postgres)
 * - Storage (subir/descargar archivos)
 */
object SupabaseClientProvider {

    private var supabaseClient: SupabaseClient? = null

    /**
     * Inicializa cliente Supabase con credenciales desde BuildConfig.
     * Llamar una sola vez en Application.onCreate() o MainActivity.onCreate().
     */
    fun initialize(supabaseUrl: String, anonKey: String) {
        if (supabaseClient != null) {
            Log.w("Supabase", "⚠️ Cliente ya inicializado, ignorando...")
            return
        }

        try {
            supabaseClient = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = anonKey
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
            Log.i("Supabase", "✅ Cliente inicializado correctamente")
        } catch (e: Exception) {
            Log.e("Supabase", "❌ Error inicializando cliente", e)
            throw IllegalStateException("No se pudo inicializar Supabase: ${e.message}", e)
        }
    }

    /**
     * Obtiene instancia del cliente (asegura que esté inicializado).
     */
    fun getClient(): SupabaseClient {
        return supabaseClient ?: throw IllegalStateException(
            "SupabaseClient no inicializado. Llama a initialize() primero en Application.onCreate()."
        )
    }

    /**
     * Verifica si el cliente está inicializado.
     */
    fun isInitialized(): Boolean = supabaseClient != null
}
