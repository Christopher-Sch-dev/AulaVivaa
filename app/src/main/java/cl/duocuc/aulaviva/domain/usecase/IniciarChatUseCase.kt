package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class IniciarChatUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(nombreClase: String, descripcion: String, pdfUrl: String?, respuestaInicial: String) {
        repository.iniciarChatConContexto(nombreClase, descripcion, pdfUrl, respuestaInicial)
    }
}
