package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class EnviarMensajeChatUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(mensaje: String): String =
        repository.enviarMensajeChat(mensaje)
}
