package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class EstructurarClaseUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(nombre: String, descripcion: String, duracion: String, pdfUrl: String?): String =
        repository.estructurarClasePorTiempo(nombre, descripcion, duracion, pdfUrl)
}
