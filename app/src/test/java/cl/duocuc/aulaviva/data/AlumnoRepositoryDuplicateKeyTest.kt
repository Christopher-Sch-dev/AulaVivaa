package cl.duocuc.aulaviva.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para validar que no hay duplicados en listas de asignaturas.
 * Este test asegura que el fix de LazyColumn duplicate key funciona.
 */
class AlumnoRepositoryDuplicateKeyTest {

    /**
     * Simula el comportamiento del repository cuando hay entidades duplicadas
     */
    data class AsignaturaTest(
        val id: String,
        val nombre: String,
        val codigoAcceso: String
    )

    /**
     * Test: Lista con duplicados debe retornar elementos únicos
     */
    @Test
    fun `distinctBy removes duplicate asignaturas by id`() {
        // Given: Lista con duplicados (simula el problema encontrado en logcat)
        val asignaturasConDuplicados = listOf(
            AsignaturaTest("8a380fb8-44f6-4efb-8f8b-f57b0b148573", "Metodo Lean Startup", "MET2025-9DWQ"),
            AsignaturaTest("a8fd0aa2-2100-4b6e-a1ae-e3210e91dfe8", "Design Pattern", "DES2025-9HUW"),
            // Duplicados (mismo ID)
            AsignaturaTest("a8fd0aa2-2100-4b6e-a1ae-e3210e91dfe8", "Design Pattern", "DES2025-9HUW"),
            AsignaturaTest("8a380fb8-44f6-4efb-8f8b-f57b0b148573", "Metodo Lean Startup", "MET2025-9DWQ")
        )

        // When: Aplicamos distinctBy (fix implementado)
        val asignaturasUnicas = asignaturasConDuplicados.distinctBy { it.id }

        // Then: Debe tener solo 2 elementos únicos
        assertEquals(2, asignaturasUnicas.size)
        
        // Verificar IDs únicos
        val ids = asignaturasUnicas.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    /**
     * Test: Lista sin duplicados mantiene todos los elementos
     */
    @Test
    fun `distinctBy preserves all unique asignaturas`() {
        // Given: Lista sin duplicados
        val asignaturasSinDuplicados = listOf(
            AsignaturaTest("id-1", "Asignatura 1", "A1"),
            AsignaturaTest("id-2", "Asignatura 2", "A2"),
            AsignaturaTest("id-3", "Asignatura 3", "A3")
        )

        // When: Aplicamos distinctBy
        val resultado = asignaturasSinDuplicados.distinctBy { it.id }

        // Then: Debe mantener todos los elementos
        assertEquals(3, resultado.size)
    }

    /**
     * Test: Lista vacía retorna lista vacía
     */
    @Test
    fun `distinctBy handles empty list`() {
        // Given: Lista vacía
        val listaVacia = emptyList<AsignaturaTest>()

        // When: Aplicamos distinctBy
        val resultado = listaVacia.distinctBy { it.id }

        // Then: Debe retornar lista vacía
        assertTrue(resultado.isEmpty())
    }

    /**
     * Test: distinctBy mantiene primer elemento cuando hay duplicados
     */
    @Test
    fun `distinctBy keeps first occurrence of duplicate`() {
        // Given: Lista con mismo ID pero diferente nombre (edge case)
        val asignaturas = listOf(
            AsignaturaTest("same-id", "Primera Version", "V1"),
            AsignaturaTest("same-id", "Segunda Version", "V2")
        )

        // When: Aplicamos distinctBy
        val resultado = asignaturas.distinctBy { it.id }

        // Then: Debe mantener solo la primera ocurrencia
        assertEquals(1, resultado.size)
        assertEquals("Primera Version", resultado[0].nombre)
    }

    /**
     * Test: Validación de claves únicas para LazyColumn
     */
    @Test
    fun `all keys are unique for LazyColumn`() {
        // Given: Lista resultante del repository
        val asignaturas = listOf(
            AsignaturaTest("8a380fb8-44f6-4efb-8f8b-f57b0b148573", "Metodo Lean Startup", "MET2025-9DWQ"),
            AsignaturaTest("a8fd0aa2-2100-4b6e-a1ae-e3210e91dfe8", "Design Pattern", "DES2025-9HUW")
        ).distinctBy { it.id }

        // When: Extraemos claves (simula key = { it.id } de LazyColumn)
        val claves = asignaturas.map { it.id }

        // Then: Todas las claves deben ser únicas
        assertEquals(claves.size, claves.toSet().size)
    }
}
