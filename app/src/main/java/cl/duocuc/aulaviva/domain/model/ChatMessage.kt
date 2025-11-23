package cl.duocuc.aulaviva.domain.model

data class ChatMessage(
    val id: Long,
    val sessionId: Long,
    val sender: String,
    val message: String,
    val createdAt: Long
)
