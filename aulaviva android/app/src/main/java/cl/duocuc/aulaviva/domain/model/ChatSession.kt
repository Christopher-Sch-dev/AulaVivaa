package cl.duocuc.aulaviva.domain.model

data class ChatSession(
    val id: Long,
    val nombreClase: String,
    val descripcion: String,
    val pdfUrl: String?,
    val analysisText: String?,
    val startedAt: Long,
    val lastActivityAt: Long
)
