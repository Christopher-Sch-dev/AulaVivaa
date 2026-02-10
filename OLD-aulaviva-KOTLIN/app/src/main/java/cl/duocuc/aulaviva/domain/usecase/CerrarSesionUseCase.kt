package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class CerrarSesionUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(sessionId: Long) {
        repository.cerrarSesion(sessionId)
    }
}
