package cl.duocuc.aulaviva.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 🤖 Interfaz Retrofit para Gemini API
 * Modelo: gemini-2.5-flash (vigente octubre 2025)
 */
interface GeminiApiService {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
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
