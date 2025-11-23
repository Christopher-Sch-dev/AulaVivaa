package cl.duocuc.aulaviva.utils

import java.util.UUID

object IdUtils {
    fun generateId(): String = UUID.randomUUID().toString()

    fun generateUniqueName(prefix: String = ""): String {
        return if (prefix.isBlank()) UUID.randomUUID().toString() else "${UUID.randomUUID()}_$prefix"
    }
}
