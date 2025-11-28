package cl.duocuc.aulaviva.data.remote

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Utilidad para decodificar tokens JWT y extraer información.
 */
object JwtDecoder {

    /**
     * Decodifica un token JWT y extrae el userId (subject).
     */
    fun getUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e("JwtDecoder", "Token JWT inválido: no tiene 3 partes")
                return null
            }

            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            val json = JSONObject(decodedString)

            json.getString("sub") // "sub" es el subject (userId)
        } catch (e: Exception) {
            Log.e("JwtDecoder", "Error decodificando token", e)
            null
        }
    }

    /**
     * Decodifica un token JWT y extrae el email.
     */
    fun getEmailFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            val json = JSONObject(decodedString)

            json.optString("email", "")
        } catch (e: Exception) {
            Log.e("JwtDecoder", "Error decodificando email del token", e)
            null
        }
    }

    /**
     * Decodifica un token JWT y extrae el rol.
     */
    fun getRolFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes)
            val json = JSONObject(decodedString)

            json.optString("rol", "")
        } catch (e: Exception) {
            Log.e("JwtDecoder", "Error decodificando rol del token", e)
            null
        }
    }
}

