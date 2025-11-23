package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IIARepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository

/**
 * ViewModel wrapper para lógica de IA.
 * Centraliza llamadas a `IARepository` y expone resultados como LiveData<Result<*>>
 */
class IAViewModel(application: Application) : AndroidViewModel(application) {

    private val iaRepository: IIARepository = RepositoryProvider.provideIARepository(application)
    // Storage repository for IA-related file access if needed
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(application)

    fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    ): LiveData<Result<Unit>> = liveData(Dispatchers.IO) {
        try {
            iaRepository.iniciarChatConContexto(nombreClase, descripcion, pdfUrl, respuestaInicial)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun enviarMensajeChat(mensaje: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val respuesta = iaRepository.enviarMensajeChat(mensaje)
            emit(Result.success(respuesta))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarIdeasParaClase(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.generarIdeasParaClase(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun sugerirActividades(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.sugerirActividades(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun estructurarClasePorTiempo(nombre: String, descripcion: String, duracion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.estructurarClasePorTiempo(nombre, descripcion, duracion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun analizarPdfConIA(nombre: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.analizarPdfConIA(nombre, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun resumirContenidoPdf(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.resumirContenidoPdf(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarGuiaPresentacion(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.generarGuiaPresentacion(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarActividadesInteractivas(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.generarActividadesInteractivas(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun explicarConceptosParaAlumno(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.explicarConceptosParaAlumno(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarEjerciciosParaAlumno(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.generarEjerciciosParaAlumno(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun crearResumenEstudioParaAlumno(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = iaRepository.crearResumenEstudioParaAlumno(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
