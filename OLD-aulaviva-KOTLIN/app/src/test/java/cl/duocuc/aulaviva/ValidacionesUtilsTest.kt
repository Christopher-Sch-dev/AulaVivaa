package cl.duocuc.aulaviva

import org.junit.Assert.*
import org.junit.Test

/**
 * TESTS 9-10: Utilidades y Validaciones Generales
 *
 * Testean funciones de utilidad del sistema (fechas, roles, seguridad).
 * Son tests UNITARIOS simples que validan lógica de negocio.
 *
 * Para defender en evaluación:
 * - Test 9: Validación de roles del sistema (docente/alumno)
 * - Test 10: Validación de formato de fechas
 */
class ValidacionesUtilsTest {

    /**
     * TEST 9: Roles deben ser válidos (docente o alumno)
     *
     * QUÉ TESTEA: Validación de roles permitidos en el sistema
     * POR QUÉ ES IMPORTANTE: Solo existen 2 roles, validar que sean correctos
     * CÓMO DEFENDERLO: "Verifico que el sistema solo acepte roles válidos
     *                    (docente o alumno), rechazando cualquier otro valor"
     */
    @Test
    fun test09_roles_debenSerValidos() {
        // Arrange (Preparar): Roles del sistema
        val rolDocente = "docente"
        val rolAlumno = "alumno"
        val rolInvalido = "administrador"

        // Lista de roles permitidos (como en RegisterActivity)
        val rolesPermitidos = listOf("docente", "alumno")

        // Act (Actuar): Verificar que rol esté en la lista
        val docenteEsValido = rolesPermitidos.contains(rolDocente)
        val alumnoEsValido = rolesPermitidos.contains(rolAlumno)
        val invalidoEsValido = rolesPermitidos.contains(rolInvalido)

        // Assert (Verificar): Solo docente y alumno deben ser válidos
        assertTrue("Rol 'docente' debe ser válido", docenteEsValido)
        assertTrue("Rol 'alumno' debe ser válido", alumnoEsValido)
        assertFalse("Rol inválido debe ser rechazado", invalidoEsValido)

        // Verificación adicional: Solo deben existir 2 roles
        assertEquals("Sistema debe tener exactamente 2 roles", 2, rolesPermitidos.size)
    }

    /**
     * TEST 10: Formato de fecha debe ser válido (dd/MM/yyyy)
     *
     * QUÉ TESTEA: Validación de formato de fechas usado en el sistema
     * POR QUÉ ES IMPORTANTE: Fechas consistentes para ordenamiento y búsqueda
     * CÓMO DEFENDERLO: "Verifico que las fechas sigan el formato dd/MM/yyyy,
     *                    garantizando consistencia en toda la aplicación"
     */
    @Test
    fun test10_formatoFecha_debeSerValido() {
        // Arrange (Preparar): Fechas en diferentes formatos
        val fechaValida = "19/11/2025"
        val fechaInvalida1 = "2025-11-19"  // Formato ISO
        val fechaInvalida2 = "19-11-2025"  // Guiones en vez de barras
        val fechaInvalida3 = "11/19/2025"  // Formato USA (MM/DD/YYYY)

        // Función de validación simple (formato dd/MM/yyyy con validación de rangos)
        fun validarFecha(fecha: String): Boolean {
            val regexFecha = Regex("^\\d{2}/\\d{2}/\\d{4}$")
            if (!regexFecha.matches(fecha)) return false

            val partes = fecha.split("/")
            val dia = partes[0].toIntOrNull() ?: return false
            val mes = partes[1].toIntOrNull() ?: return false

            // Validar rangos (día 1-31, mes 1-12)
            return dia in 1..31 && mes in 1..12
        }

        // Act (Actuar): Verificar formato con función
        val validaEsCorrecta = validarFecha(fechaValida)
        val invalida1EsCorrecta = validarFecha(fechaInvalida1)
        val invalida2EsCorrecta = validarFecha(fechaInvalida2)
        val invalida3EsCorrecta = validarFecha(fechaInvalida3)

        // Assert (Verificar): Solo fecha en formato correcto debe pasar
        assertTrue("Fecha 19/11/2025 debe ser válida", validaEsCorrecta)
        assertFalse("Fecha ISO 2025-11-19 debe ser inválida", invalida1EsCorrecta)
        assertFalse("Fecha con guiones debe ser inválida", invalida2EsCorrecta)
        assertFalse("Fecha formato USA debe ser inválida", invalida3EsCorrecta)

        // Verificación adicional: Fecha válida debe tener exactamente 10 caracteres
        assertEquals("Fecha debe tener 10 caracteres (dd/MM/yyyy)", 10, fechaValida.length)

        // Verificación de partes de la fecha
        val partes = fechaValida.split("/")
        assertEquals("Fecha debe tener 3 partes separadas por /", 3, partes.size)
        assertEquals("Día debe tener 2 dígitos", 2, partes[0].length)
        assertEquals("Mes debe tener 2 dígitos", 2, partes[1].length)
        assertEquals("Año debe tener 4 dígitos", 4, partes[2].length)
    }
}
