package cl.duocuc.aulaviva.backend.application.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: UUID, email: String, rol: String): String {
        val now = Date()
        val expiration = Date(now.time + 86400000) // 24 horas

        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("email", email)
            .claim("rol", rol)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = extractClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun getEmailFromToken(token: String): String {
        val claims = extractClaims(token)
        return claims["email"] as String
    }

    fun getRolFromToken(token: String): String {
        val claims = extractClaims(token)
        return claims["rol"] as String
    }

    private fun extractClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}

