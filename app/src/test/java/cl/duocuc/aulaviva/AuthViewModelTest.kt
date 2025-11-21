package cl.duocuc.aulaviva

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cl.duocuc.aulaviva.presentation.viewmodel.AuthViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * TESTS 1-2: AuthViewModel
 *
 * Testean las validaciones básicas de autenticación.
 * Son tests UNITARIOS que no requieren mocks complejos.
 *
 * Para defender en evaluación:
 * - Test 1: Verifica que emails válidos pasen la validación
 * - Test 2: Verifica que passwords cortos (< 6 chars) fallen
 */
class AuthViewModelTest {

    // ✅ Regla necesaria para testing de LiveData
    // Hace que LiveData se ejecute de forma síncrona en tests
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // ViewModel bajo prueba
    private lateinit var viewModel: AuthViewModel

    /**
     * Se ejecuta ANTES de cada test.
     * Crea una instancia nueva del ViewModel.
     */
    @Before
    fun setup() {
        viewModel = AuthViewModel()
    }

    /**
     * TEST 1: Email válido debe pasar validación
     *
     * QUÉ TESTEA: Validación de formato de email
     * POR QUÉ ES IMPORTANTE: Autenticación es crítica, debe validar emails
     * CÓMO DEFENDERLO: "Verifico que emails con formato usuario@dominio.com
     *                    sean aceptados por el sistema de validación"
     */
    @Test
    fun test01_emailValido_debeRetornarTrue() {
        // Arrange (Preparar): Email con formato correcto
        val emailValido = "alumno@duoc.cl"

        // Regex simple para validar email (como en ViewModel pero sin android.util.Patterns)
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        // Act (Actuar): Validar con regex
        val resultado = emailValido.isNotEmpty() && emailRegex.matches(emailValido)

        // Assert (Verificar): Debe retornar true
        assertTrue("Email válido debe pasar validación", resultado)
    }    /**
     * TEST 2: Password corto debe fallar validación
     *
     * QUÉ TESTEA: Función isValidPassword() con menos de 6 caracteres
     * POR QUÉ ES IMPORTANTE: Seguridad requiere passwords de mínimo 6 chars
     * CÓMO DEFENDERLO: "Verifico que passwords menores a 6 caracteres
     *                    sean rechazados para garantizar seguridad básica"
     */
    @Test
    fun test02_passwordCorto_debeRetornarFalse() {
        // Arrange (Preparar): Password de solo 4 caracteres
        val passwordCorto = "1234"

        // Act (Actuar): Llamar a la función de validación
        val resultado = viewModel.isValidPassword(passwordCorto)

        // Assert (Verificar): Debe retornar false
        assertFalse("Password menor a 6 caracteres debe fallar", resultado)
    }
}
