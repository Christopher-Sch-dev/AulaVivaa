package cl.duocuc.aulaviva

import android.app.Application
import android.util.Log
import cl.duocuc.aulaviva.data.remote.TokenManager

/**
 * Clase Application personalizada.
 * Se ejecuta ANTES de cualquier Activity.
 *
 * Inicializa Spring Boot backend (migrado de Supabase directo).
 */
class AulaVivaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Inicializar TokenManager para persistencia de sesión
        TokenManager.init(this)
        // Verificar configuración de Spring Boot
        try {
            val springBootUrl = BuildConfig.SPRING_BOOT_URL

            if (springBootUrl.isEmpty() || springBootUrl == "http://localhost:8080/") {
                Log.w("AulaVivaApp", "⚠️ URL de Spring Boot no configurada - Usando localhost")
                Log.w("AulaVivaApp", "Configura SPRING_BOOT_URL en local.properties")
            } else {
                Log.i("AulaVivaApp", "✅ Backend Spring Boot configurado: $springBootUrl")
            }

            // El cliente Retrofit se inicializa automáticamente al acceder a SpringBootClient.apiService
            // No requiere inicialización explícita como Supabase

        } catch (e: Exception) {
            Log.e("AulaVivaApp", "Error inicializando aplicación", e)
            // No lanzar excepción, permitir que la app continúe funcionando
        }
    }
}
