package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository

class SincronizarAsignaturasUseCase(private val repository: IAsignaturasRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.sincronizarAsignaturas()
}
