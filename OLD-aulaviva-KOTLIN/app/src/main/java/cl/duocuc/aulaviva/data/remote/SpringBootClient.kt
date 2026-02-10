package cl.duocuc.aulaviva.data.remote

import android.util.Log
import cl.duocuc.aulaviva.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para comunicarse con el backend Spring Boot.
 * Singleton que proporciona acceso a la API.
 */
object SpringBootClient {

    private const val BASE_URL = BuildConfig.SPRING_BOOT_URL

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("SpringBootAPI", message)
    }.apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Solo agregar el token si no existe ya un header Authorization
        if (original.header("Authorization") == null) {
            val token = TokenManager.getToken()
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: SpringBootApiService = retrofit.create(SpringBootApiService::class.java)
}

/**
 * Gestor de tokens JWT con persistencia.
 * Guarda el token en SharedPreferences para que persista al cerrar la app.
 * La sesión se mantiene hasta que el usuario cierre sesión manualmente.
 */
object TokenManager {
    private const val PREFS_NAME = "aulaviva_session"
    private const val KEY_TOKEN = "jwt_token"
    
    private var cachedToken: String? = null
    private var prefs: android.content.SharedPreferences? = null
    
    /**
     * Inicializa el TokenManager con el contexto de la aplicación.
     * Debe llamarse en Application.onCreate()
     */
    fun init(context: android.content.Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        // Cargar token desde SharedPreferences al iniciar
        cachedToken = prefs?.getString(KEY_TOKEN, null)
        if (cachedToken != null) {
            Log.d("TokenManager", "Token restaurado desde almacenamiento")
        }
    }

    fun saveToken(newToken: String) {
        cachedToken = newToken
        prefs?.edit()?.putString(KEY_TOKEN, newToken)?.apply()
        Log.d("TokenManager", "Token guardado (persistente)")
    }

    fun getToken(): String? = cachedToken

    fun clearToken() {
        cachedToken = null
        prefs?.edit()?.remove(KEY_TOKEN)?.apply()
        Log.d("TokenManager", "Token eliminado")
    }

    fun hasToken(): Boolean = cachedToken != null
}

