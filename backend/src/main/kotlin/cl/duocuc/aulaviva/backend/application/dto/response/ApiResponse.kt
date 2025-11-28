package cl.duocuc.aulaviva.backend.application.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null) = ApiResponse(true, data, message)
        fun <T> error(error: String, message: String? = null) = ApiResponse<T>(false, null, message, error)
    }
}

