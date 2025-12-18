package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IClaseRepository

class SincronizarClasesUseCase(private val repository: IClaseRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.sincronizarDesdeSupabase()
    suspend fun invokePorAsignatura(asignaturaId: String) = repository.sincronizarClasesPorAsignatura(asignaturaId)
}
