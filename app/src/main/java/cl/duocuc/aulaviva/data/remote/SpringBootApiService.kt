package cl.duocuc.aulaviva.data.remote

import cl.duocuc.aulaviva.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz Retrofit para comunicarse con el backend Spring Boot.
 * Define todos los endpoints disponibles en el backend.
 */
interface SpringBootApiService {

    // ============ AUTH ============

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<ApiResponseDto<AuthResponseDto>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<ApiResponseDto<UsuarioResponseDto>>

    // ============ ASIGNATURAS ============

    @POST("api/asignaturas")
    suspend fun crearAsignatura(
        @Header("Authorization") token: String,
        @Body request: CrearAsignaturaRequestDto
    ): Response<ApiResponseDto<AsignaturaResponseDto>>

    @GET("api/asignaturas")
    suspend fun obtenerAsignaturas(
        @Header("Authorization") token: String
    ): Response<ApiResponseDto<List<AsignaturaResponseDto>>>

    @GET("api/asignaturas/{id}")
    suspend fun obtenerAsignatura(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<AsignaturaResponseDto>>

    @PUT("api/asignaturas/{id}")
    suspend fun actualizarAsignatura(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: ActualizarAsignaturaRequestDto
    ): Response<ApiResponseDto<AsignaturaResponseDto>>

    @DELETE("api/asignaturas/{id}")
    suspend fun eliminarAsignatura(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<Unit?>>

    @POST("api/asignaturas/{id}/generar-codigo")
    suspend fun generarCodigo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<GenerarCodigoResponseDto>>

    // ============ CLASES ============

    @POST("api/clases")
    suspend fun crearClase(
        @Header("Authorization") token: String,
        @Body request: CrearClaseRequestDto
    ): Response<ApiResponseDto<ClaseResponseDto>>

    @GET("api/clases")
    suspend fun obtenerClases(
        @Header("Authorization") token: String,
        @Query("asignaturaId") asignaturaId: String?
    ): Response<ApiResponseDto<List<ClaseResponseDto>>>

    @GET("api/clases/{id}")
    suspend fun obtenerClase(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<ClaseResponseDto>>

    @PUT("api/clases/{id}")
    suspend fun actualizarClase(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: ActualizarClaseRequestDto
    ): Response<ApiResponseDto<ClaseResponseDto>>

    @DELETE("api/clases/{id}")
    suspend fun eliminarClase(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<Unit?>>

    // ============ ALUMNOS ============

    @POST("api/alumnos/inscribir")
    suspend fun inscribirConCodigo(
        @Header("Authorization") token: String,
        @Body request: InscribirConCodigoRequestDto
    ): Response<ApiResponseDto<InscripcionResponseDto>>

    @GET("api/alumnos/asignaturas")
    suspend fun obtenerAsignaturasInscritas(
        @Header("Authorization") token: String
    ): Response<ApiResponseDto<List<AsignaturaResponseDto>>>

    @DELETE("api/alumnos/asignaturas/{asignaturaId}")
    suspend fun darDeBaja(
        @Header("Authorization") token: String,
        @Path("asignaturaId") asignaturaId: String
    ): Response<ApiResponseDto<Unit?>>

    @GET("api/alumnos/asignaturas/{asignaturaId}/inscripciones")
    suspend fun obtenerInscripciones(
        @Header("Authorization") token: String,
        @Path("asignaturaId") asignaturaId: String
    ): Response<ApiResponseDto<List<AlumnoAsignaturaResponseDto>>>

    // ============ STORAGE ============

    @Multipart
    @POST("api/storage/upload")
    suspend fun subirPdf(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("nombre") nombre: RequestBody
    ): Response<ApiResponseDto<StorageUploadResponseDto>>
}

