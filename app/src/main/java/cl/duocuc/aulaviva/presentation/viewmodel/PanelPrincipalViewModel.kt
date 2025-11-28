package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import cl.duocuc.aulaviva.utils.IdUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PanelPrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    private val authRepository: IAuthRepository = RepositoryProvider.provideAuthRepository()
    // Storage repository available for uploads (centralized)
    private val storageRepository: IStorageRepository = RepositoryProvider.provideStorageRepository(app)

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> = _userEmail

    private val _userId = MutableLiveData<String?>()
    val userId: LiveData<String?> = _userId

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _demoCodigo = MutableLiveData<String?>()
    val demoCodigo: LiveData<String?> = _demoCodigo

    /**
     * Crea una asignatura demo y una clase demo. Public API para la Activity.
     */
    fun crearAsignaturaYClaseDemo() {
        viewModelScope.launch {
            try {
                val asignaturasRepo = RepositoryProvider.provideAsignaturasRepository(app)
                val clasesRepo = RepositoryProvider.provideClaseRepository(app)

                val resultAsignatura = asignaturasRepo.crearAsignatura(
                    nombre = "Programación Móvil DEMO",
                    descripcion = "Asignatura de demostración con clase de prueba incluida"
                )

                if (resultAsignatura.isFailure) {
                    throw resultAsignatura.exceptionOrNull() ?: Exception("Error desconocido al crear asignatura")
                }

                val asignaturaCreada = resultAsignatura.getOrNull()!!
                val codigoFinal = asignaturaCreada.codigoAcceso.uppercase()

                // Descargar y subir el PDF demo al backend de Spring Boot (Storage)
                val pdfUrl = "https://ferestrepoca.github.io/paradigmas-de-programacion/poo/tutoriales/Kotlin/Enlaces/POO-Kotlin.pptx.pdf"
                val pdfNombre = "POO-Kotlin.pptx.pdf"

                android.util.Log.d("PanelPrincipalVM", "📥 Descargando y subiendo PDF demo...")
                val uploadResult = storageRepository.subirPdfDesdeUrl(pdfUrl, pdfNombre)

                uploadResult.fold(
                    onSuccess = { uploadedPdfUrl ->
                        android.util.Log.d("PanelPrincipalVM", "✅ PDF subido: $uploadedPdfUrl")

                        // Crear la clase demo con la URL del PDF subido
                        val claseDemo = Clase(
                            id = IdUtils.generateId(),
                            nombre = "Introducción a Kotlin para Android",
                            descripcion = "Clase demostrativa sobre Kotlin y desarrollo Android",
                            fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                            asignaturaId = asignaturaCreada.id,
                            archivoPdfUrl = uploadedPdfUrl,
                            archivoPdfNombre = pdfNombre,
                            creador = authRepository.getCurrentUserId() ?: ""
                        )

                        clasesRepo.crearClaseAsync(
                            clase = claseDemo,
                            onSuccess = {
                                _toastMessage.postValue("✅ ¡Demo creada! Código: $codigoFinal")
                                _demoCodigo.postValue(codigoFinal)
                            },
                            onError = { error ->
                                _toastMessage.postValue("⚠️ Asignatura creada, pero error en clase: $error")
                            },
                            scope = viewModelScope
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("PanelPrincipalVM", "❌ Error subiendo PDF demo", error)
                        _toastMessage.postValue("⚠️ Asignatura creada, pero error subiendo PDF: ${error.message}")
                    }
                )

            } catch (e: Exception) {
                _toastMessage.postValue("❌ Error al crear demo: ${e.message}")
            }
        }
    }

    init {
        // Inicializar user info desde el repositorio de auth
        // Verificar autenticación antes de obtener datos del usuario
        viewModelScope.launch {
            // Pequeño delay para asegurar que la sesión esté completamente establecida
            delay(200)

            if (authRepository.isLoggedIn()) {
                _userEmail.value = authRepository.getCurrentUserEmail()
                _userId.value = authRepository.getCurrentUserId()
            } else {
                android.util.Log.w("PanelPrincipalVM", "⚠️ Usuario no autenticado, no se pueden obtener datos del usuario")
            }
        }
    }

    fun logout() {
        // Delegar logout al repositorio y notificar a la Activity
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (_: Exception) {
                // ignore errors during logout
            }
            _logoutEvent.postValue(true)
        }
    }

    /**
     * Limpia el mensaje de toast después de mostrarlo.
     */
    fun limpiarToastMessage() {
        _toastMessage.postValue(null)
    }
}
