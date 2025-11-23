package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.model.Clase
import cl.duocuc.aulaviva.data.repository.AsignaturasRepository
import cl.duocuc.aulaviva.data.repository.ClaseRepository
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.utils.IdUtils
import kotlinx.coroutines.launch

class PanelPrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

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

                val claseDemo = Clase(
                    id = IdUtils.generateId(),
                    nombre = "Introducción a Kotlin para Android",
                    descripcion = "Clase demostrativa sobre Kotlin y desarrollo Android",
                    fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    asignaturaId = asignaturaCreada.id,
                    archivoPdfUrl = "https://kotlinlang.org/docs/kotlin-reference.pdf",
                    archivoPdfNombre = "Kotlin_Reference_Demo.pdf",
                    creador = cl.duocuc.aulaviva.data.supabase.SupabaseAuthManager.getCurrentUserId() ?: ""
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

            } catch (e: Exception) {
                _toastMessage.postValue("❌ Error al crear demo: ${e.message}")
            }
        }
    }
}
