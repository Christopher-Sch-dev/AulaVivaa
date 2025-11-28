package cl.duocuc.aulaviva.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StorageUploadResponseDto(
    val url: String,
    val nombre: String
)

