package cl.duocuc.aulaviva.backend.presentation.controller

import cl.duocuc.aulaviva.backend.application.dto.response.ApiResponse
import cl.duocuc.aulaviva.backend.application.service.StorageService
import cl.duocuc.aulaviva.backend.infrastructure.security.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/storage")
class StorageController(
    private val storageService: StorageService
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun subirPdf(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("nombre") nombre: String
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        return try {
            val userId = CurrentUser.getUserId()
            val url = storageService.subirPdf(file, nombre, userId)
            val response = mapOf(
                "url" to url,
                "nombre" to nombre
            )
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "PDF subido exitosamente"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al subir PDF", e.message))
        }
    }
}

