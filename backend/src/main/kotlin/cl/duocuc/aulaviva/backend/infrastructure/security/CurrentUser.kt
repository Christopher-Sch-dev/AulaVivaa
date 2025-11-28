package cl.duocuc.aulaviva.backend.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

object CurrentUser {
    fun getUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UUID
    }
}

