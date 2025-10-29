package cl.duocuc.aulaviva.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.duocuc.aulaviva.data.repository.AuthRepository

/**
 * ViewModel para manejar la lógica de autenticación
 * Separa la lógica de la UI y mantiene el estado
 */
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData para login exitoso
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    // LiveData para registro exitoso
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    // Validar email
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validar password
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // Login
    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value = null

        repository.login(
            email = email,
            password = password,
            onSuccess = {
                _isLoading.value = false
                _loginSuccess.value = true
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = "Error de login: $errorMsg"
            }
        )
    }

    // Registro
    fun register(email: String, password: String) {
        _isLoading.value = true
        _error.value = null

        repository.register(
            email = email,
            password = password,
            onSuccess = {
                _isLoading.value = false
                _registerSuccess.value = true
            },
            onError = { errorMsg ->
                _isLoading.value = false
                _error.value = errorMsg
            }
        )
    }

    // Logout
    fun logout() {
        repository.logout()
    }

    // Obtener usuario actual
    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUser()?.email
    }

    // Resetear mensajes de error
    fun clearError() {
        _error.value = null
    }
}
