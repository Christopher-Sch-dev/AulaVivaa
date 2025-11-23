package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class AnalizarPdfUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(nombreClase: String, pdfUrl: String?): String =
        repository.analizarPdfConIA(nombreClase, pdfUrl)
}
