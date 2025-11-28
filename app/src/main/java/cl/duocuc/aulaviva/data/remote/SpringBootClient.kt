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
        val token = TokenManager.getToken()

        val requestBuilder = original.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
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
 * Gestor de tokens JWT.
 * Guarda y recupera el token de autenticación.
 */
object TokenManager {
    private var token: String? = null

    fun saveToken(newToken: String) {
        token = newToken
        Log.d("TokenManager", "Token guardado")
    }

    fun getToken(): String? = token

    fun clearToken() {
        token = null
        Log.d("TokenManager", "Token eliminado")
    }

    fun hasToken(): Boolean = token != null
}

