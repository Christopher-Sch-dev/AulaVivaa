package cl.duocuc.aulaviva.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaz Retrofit para Gemini API
 * Modelo primario: gemini-3-flash-preview
 * Modelo fallback: gemini-2.5-flash-lite
 */
interface GeminiApiService {

    // Modelo primario: gemini-3-flash-preview (más nuevo y potente)
    @POST("v1beta/models/gemini-3-flash-preview:generateContent")
    suspend fun generateContentPrimary(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
    
    // Modelo fallback: gemini-2.5-flash-lite (estable y probado)
    @POST("v1beta/models/gemini-2.5-flash-lite:generateContent")
    suspend fun generateContentFallback(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
    
    // Mantener compatibilidad: alias al modelo primario
    @POST("v1beta/models/gemini-3-flash-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

/**
 * Request según documentación oficial de Gemini
 */
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 1024
)

/**
 * Response según documentación oficial de Gemini
 */
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ContentResponse?
)

data class ContentResponse(
    val parts: List<PartResponse>?
)

data class PartResponse(
    val text: String?
)
