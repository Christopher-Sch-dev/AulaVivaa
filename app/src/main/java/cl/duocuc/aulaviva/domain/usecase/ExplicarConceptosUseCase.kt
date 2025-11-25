package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class ExplicarConceptosUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(nombre: String, descripcion: String, pdfUrl: String?): String =
        repository.explicarConceptosParaAlumno(nombre, descripcion, pdfUrl)
}
