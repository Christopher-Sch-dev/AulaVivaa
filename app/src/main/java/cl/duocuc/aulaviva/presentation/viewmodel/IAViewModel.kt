package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IIARepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import cl.duocuc.aulaviva.domain.usecase.AnalizarPdfUseCase
import cl.duocuc.aulaviva.domain.usecase.CrearResumenEstudioUseCase
import cl.duocuc.aulaviva.domain.usecase.EstructurarClaseUseCase
import cl.duocuc.aulaviva.domain.usecase.EnviarMensajeChatUseCase
import cl.duocuc.aulaviva.domain.usecase.ExplicarConceptosUseCase
import cl.duocuc.aulaviva.domain.usecase.GenerarActividadesInteractivasUseCase
import cl.duocuc.aulaviva.domain.usecase.GenerarEjerciciosUseCase
import cl.duocuc.aulaviva.domain.usecase.GenerarGuiaUseCase
import cl.duocuc.aulaviva.domain.usecase.GenerarIdeasUseCase
import cl.duocuc.aulaviva.domain.usecase.ResumirContenidoPdfUseCase
import cl.duocuc.aulaviva.domain.usecase.SugerirActividadesUseCase
import cl.duocuc.aulaviva.domain.usecase.IniciarChatUseCase
import cl.duocuc.aulaviva.domain.usecase.ReanalizarPdfUseCase

/**
 * ViewModel wrapper para lógica de IA.
 * Centraliza llamadas a `IARepository` y expone resultados como LiveData<Result<*>>
 */
class IAViewModel(application: Application) : AndroidViewModel(application) {
    private val iaRepository: IIARepository = RepositoryProvider.provideIARepository(application)
    // Storage repository for IA-related file access if needed
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(application)

    // UseCases (created lazily here to preserve default ViewModel construction via RepositoryProvider)
    private val generarIdeasUseCase by lazy { GenerarIdeasUseCase(iaRepository) }
    private val sugerirActividadesUseCase by lazy { SugerirActividadesUseCase(iaRepository) }
    private val estructurarClaseUseCase by lazy { EstructurarClaseUseCase(iaRepository) }
    private val analizarPdfUseCase by lazy { AnalizarPdfUseCase(iaRepository) }
    private val resumirContenidoPdfUseCase by lazy { ResumirContenidoPdfUseCase(iaRepository) }
    private val generarGuiaUseCase by lazy { GenerarGuiaUseCase(iaRepository) }
    private val generarActividadesInteractivasUseCase by lazy { GenerarActividadesInteractivasUseCase(iaRepository) }
    private val explicarConceptosUseCase by lazy { ExplicarConceptosUseCase(iaRepository) }
    private val generarEjerciciosUseCase by lazy { GenerarEjerciciosUseCase(iaRepository) }
    private val crearResumenEstudioUseCase by lazy { CrearResumenEstudioUseCase(iaRepository) }
    private val iniciarChatUseCase by lazy { IniciarChatUseCase(iaRepository) }
    private val enviarMensajeChatUseCase by lazy { EnviarMensajeChatUseCase(iaRepository) }
    private val reanalizarPdfUseCase by lazy { ReanalizarPdfUseCase(iaRepository) }
    private val cerrarSesionUseCase by lazy { cl.duocuc.aulaviva.domain.usecase.CerrarSesionUseCase(iaRepository) }

    // Lectura de sesiones/mensajes
    fun obtenerUltimaSesion(nombreClase: String) = liveData(Dispatchers.IO) {
        try {
            val s = iaRepository.obtenerUltimaSesionParaClase(nombreClase)
            emit(Result.success(s))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun obtenerMensajes(sessionId: Long) = liveData(Dispatchers.IO) {
        try {
            val msgs = iaRepository.obtenerMensajesDeSesion(sessionId)
            emit(Result.success(msgs))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun reanalizarPdf(sessionId: Long, pdfUrl: String?) = liveData(Dispatchers.IO) {
        try {
            val r = reanalizarPdfUseCase(sessionId, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun cerrarSesion(sessionId: Long) = liveData(Dispatchers.IO) {
        try {
            cerrarSesionUseCase(sessionId)
            emit(Result.success(Unit))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    ): LiveData<Result<Unit>> = liveData(Dispatchers.IO) {
        try {
            iniciarChatUseCase(nombreClase, descripcion, pdfUrl, respuestaInicial)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun enviarMensajeChat(mensaje: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val respuesta = enviarMensajeChatUseCase(mensaje)
            emit(Result.success(respuesta))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarIdeasParaClase(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = generarIdeasUseCase(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun sugerirActividades(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = sugerirActividadesUseCase(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun estructurarClasePorTiempo(nombre: String, descripcion: String, duracion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = estructurarClaseUseCase(nombre, descripcion, duracion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun analizarPdfConIA(nombre: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = analizarPdfUseCase(nombre, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun resumirContenidoPdf(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = resumirContenidoPdfUseCase(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarGuiaPresentacion(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = generarGuiaUseCase(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarActividadesInteractivas(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = generarActividadesInteractivasUseCase(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun explicarConceptosParaAlumno(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = explicarConceptosUseCase(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun generarEjerciciosParaAlumno(nombre: String, descripcion: String, pdfUrl: String?): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = generarEjerciciosUseCase(nombre, descripcion, pdfUrl)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun crearResumenEstudioParaAlumno(nombre: String, descripcion: String, archivoNombre: String): LiveData<Result<String>> = liveData(Dispatchers.IO) {
        try {
            val r = crearResumenEstudioUseCase(nombre, descripcion, archivoNombre)
            emit(Result.success(r))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
