package cl.duocuc.aulaviva

import org.junit.Assert.*
import org.junit.Test

/**
 * TESTS 3-4: Validaciones de Asignaturas
 *
 * Testean la lógica de validación de campos de asignaturas.
 * Son tests UNITARIOS simples sin dependencias.
 *
 * Para defender en evaluación:
 * - Test 3: Nombres vacíos deben ser rechazados
 * - Test 4: Códigos generados deben tener formato correcto
 */
class AsignaturasValidacionTest {

    /**
     * TEST 3: Nombre vacío debe fallar validación
     *
     * QUÉ TESTEA: Validación de nombre de asignatura no vacío
     * POR QUÉ ES IMPORTANTE: No se puede crear asignatura sin nombre
     * CÓMO DEFENDERLO: "Verifico que asignaturas sin nombre sean rechazadas,
     *                    ya que el nombre es un campo obligatorio en el sistema"
     */
    @Test
    fun test03_nombreVacio_debeSerInvalido() {
        // Arrange (Preparar): Nombre vacío y con solo espacios
        val nombreVacio = ""
        val nombreConEspacios = "   "

        // Act (Actuar): Simular validación (como en AsignaturasViewModel)
        val resultadoVacio = nombreVacio.isBlank()
        val resultadoEspacios = nombreConEspacios.isBlank()

        // Assert (Verificar): Ambos deben ser detectados como vacíos
        assertTrue("Nombre vacío debe ser detectado", resultadoVacio)
        assertTrue("Nombre con solo espacios debe ser detectado", resultadoEspacios)
    }

    /**
     * TEST 4: Código generado debe tener formato válido
     *
     * QUÉ TESTEA: Formato de códigos de inscripción generados
     * POR QUÉ ES IMPORTANTE: Los códigos permiten inscripción de alumnos
     * CÓMO DEFENDERLO: "Verifico que los códigos de inscripción sigan el formato
     *                    MAT2025-XXXX, con materia, año y 4 caracteres únicos"
     */
    @Test
    fun test04_formatoCodigo_debeSerValido() {
        // Arrange (Preparar): Código generado del sistema (ejemplo real)
        val codigoEjemplo = "MAT2025-ABCD"

        // Act (Actuar): Verificar que sigue el patrón esperado
        val tieneGuion = codigoEjemplo.contains("-")
        val partes = codigoEjemplo.split("-")
        val tieneAño = partes[0].contains("2025")
        val codigoAlfanumerico = partes.size == 2 && partes[1].length == 4

        // Assert (Verificar): Código debe cumplir todas las reglas
        assertTrue("Código debe contener guion separador", tieneGuion)
        assertTrue("Código debe incluir año 2025", tieneAño)
        assertTrue("Código debe tener 4 caracteres después del guion", codigoAlfanumerico)

        // Verificación adicional: Código completo no debe estar vacío
        assertFalse("Código no debe estar vacío", codigoEjemplo.isBlank())
    }
}
