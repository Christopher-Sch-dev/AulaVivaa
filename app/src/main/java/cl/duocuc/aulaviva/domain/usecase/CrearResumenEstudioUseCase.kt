package cl.duocuc.aulaviva.domain.usecase

import cl.duocuc.aulaviva.domain.repository.IIARepository

class CrearResumenEstudioUseCase(private val repository: IIARepository) {
    suspend operator fun invoke(nombre: String, descripcion: String, pdfUrl: String?): String =
        repository.crearResumenEstudioParaAlumno(nombre, descripcion, pdfUrl)
}
