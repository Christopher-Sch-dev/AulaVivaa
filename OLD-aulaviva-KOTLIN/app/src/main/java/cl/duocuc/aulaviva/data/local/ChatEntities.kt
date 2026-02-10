package cl.duocuc.aulaviva.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nombre_clase") val nombreClase: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "pdf_url") val pdfUrl: String?,
    @ColumnInfo(name = "analysis_text") var analysisText: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_activity_at") var lastActivityAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("session_id")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "sender") val sender: String, // "user" or "ai"
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
