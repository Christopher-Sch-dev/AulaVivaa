package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.repository.AsignaturasRepository
import cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de inscritos.
 * Maneja la sincronización de alumnos inscritos.
 */
class InscritosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AsignaturasRepository(
        asignaturaDao = database.asignaturaDao(),
        alumnoAsignaturaDao = database.alumnoAsignaturaDao(),
        supabaseRepository = SupabaseAsignaturaRepository(
            asignaturaDao = database.asignaturaDao(),
            alumnoAsignaturaDao = database.alumnoAsignaturaDao()
        )
    )

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Sincroniza los inscritos desde Supabase.
     */
    fun sincronizarInscritos(asignaturaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.sincronizarInscritos(asignaturaId)
                .onSuccess {
                    android.util.Log.d("InscritosVM", "✅ Sincronización exitosa")
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    android.util.Log.e("InscritosVM", "❌ Error sincronizando", exception)
                }

            _isLoading.value = false
        }
    }
}
