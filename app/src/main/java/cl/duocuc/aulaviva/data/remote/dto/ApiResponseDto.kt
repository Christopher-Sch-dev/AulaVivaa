package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

