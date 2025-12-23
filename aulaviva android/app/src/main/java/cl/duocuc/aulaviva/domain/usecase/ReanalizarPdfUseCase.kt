package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class ReanalizarPdfUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(sessionId: Long, pdfUrl: String?): String =
        repository.reanalizarPdfParaSesion(sessionId, pdfUrl)
}
