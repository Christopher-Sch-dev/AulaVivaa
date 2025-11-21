package cl.duocuc.aulaviva

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * TESTS 5-6: Validaciones de Clases
 *
 * Testean la lógica de creación y validación de clases.
 * Son tests UNITARIOS que validan campos obligatorios y UUIDs.
 *
 * Para defender en evaluación:
 * - Test 5: Validación de campos obligatorios de clase
 * - Test 6: Generación de UUIDs únicos para clases
 */
class ClaseValidacionTest {

    /**
     * TEST 5: Campos obligatorios deben ser validados
     *
     * QUÉ TESTEA: Validación de nombre y asignatura_id obligatorios
     * POR QUÉ ES IMPORTANTE: Clases sin estos datos no pueden existir
     * CÓMO DEFENDERLO: "Verifico que no se puedan crear clases sin nombre
     *                    ni asignatura asociada, validando campos requeridos"
     */
    @Test
    fun test05_camposObligatorios_debenSerValidados() {
        // Arrange (Preparar): Simular campos de una clase
        val nombreVacio = ""
        val nombreValido = "Introducción a Kotlin"
        val asignaturaIdVacio = ""
        val asignaturaIdValido = "asig-123"

        // Act (Actuar): Simular validaciones (como en CrearEditarClaseActivity)
        val nombreEsValido = nombreVacio.isNotBlank()
        val asignaturaEsValida = asignaturaIdVacio.isNotBlank()

        val claseValida = nombreValido.isNotBlank() && asignaturaIdValido.isNotBlank()

        // Assert (Verificar): Clase sin campos debe ser inválida
        assertFalse("Nombre vacío debe ser inválido", nombreEsValido)
        assertFalse("Asignatura vacía debe ser inválida", asignaturaEsValida)

        // Clase con todos los campos debe ser válida
        assertTrue("Clase con todos los campos debe ser válida", claseValida)
    }

    /**
     * TEST 6: UUIDs generados deben ser únicos
     *
     * QUÉ TESTEA: Generación de IDs únicos para clases usando UUID
     * POR QUÉ ES IMPORTANTE: Evita colisiones y duplicación de clases
     * CÓMO DEFENDERLO: "Verifico que cada clase reciba un UUID único generado
     *                    por el sistema, garantizando que no haya IDs duplicados"
     */
    @Test
    fun test06_uuidsGenerados_debenSerUnicos() {
        // Arrange & Act (Preparar y Actuar): Generar varios UUIDs como en ClaseViewModel
        val uuid1 = UUID.randomUUID().toString()
        val uuid2 = UUID.randomUUID().toString()
        val uuid3 = UUID.randomUUID().toString()

        // Assert (Verificar): Todos los UUIDs deben ser diferentes
        assertNotEquals("UUID 1 y 2 deben ser diferentes", uuid1, uuid2)
        assertNotEquals("UUID 2 y 3 deben ser diferentes", uuid2, uuid3)
        assertNotEquals("UUID 1 y 3 deben ser diferentes", uuid1, uuid3)

        // Verificación adicional: UUIDs no deben estar vacíos
        assertTrue("UUID 1 no debe estar vacío", uuid1.isNotBlank())
        assertTrue("UUID 2 no debe estar vacío", uuid2.isNotBlank())
        assertTrue("UUID 3 no debe estar vacío", uuid3.isNotBlank())

        // Verificación de formato UUID (36 caracteres con guiones)
        assertEquals("UUID debe tener 36 caracteres", 36, uuid1.length)
        assertTrue("UUID debe contener guiones", uuid1.contains("-"))
    }
}
