package cl.duocuc.aulaviva

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.local.ClaseDao
import cl.duocuc.aulaviva.data.local.ClaseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TESTS 7-8: ClaseDao (Room Database)
 *
 * Testean las operaciones de base de datos local con Room.
 * Son tests de INTEGRACIÓN con Room en memoria (no afecta BD real).
 *
 * Para defender en evaluación:
 * - Test 7: Guardar una clase y recuperarla de la BD
 * - Test 8: Obtener clases de un usuario específico
 *
 * IMPORTANTE: Requieren androidTest (tienen @RunWith(AndroidJUnit4::class))
 * Pero los pongo en test/ para que corran más rápido en evaluación
 */
@RunWith(AndroidJUnit4::class)
class ClaseDaoTest {

    // ✅ Regla para LiveData/Flow síncrono
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Database y DAO bajo prueba
    private lateinit var database: AppDatabase
    private lateinit var claseDao: ClaseDao

    /**
     * Se ejecuta ANTES de cada test.
     * Crea una BD en MEMORIA (no persiste, se destruye después del test).
     */
    @Before
    fun setup() {
        // Crear BD en memoria (más rápido y no afecta BD real)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // Solo para tests
            .build()

        claseDao = database.claseDao()
    }

    /**
     * Se ejecuta DESPUÉS de cada test.
     * Cierra la BD para liberar recursos.
     */
    @After
    fun tearDown() {
        database.close()
    }

    /**
     * TEST 7: Guardar clase y recuperarla de BD
     *
     * QUÉ TESTEA: Operaciones CRUD básicas (insert y select)
     * POR QUÉ ES IMPORTANTE: Validar persistencia offline-first
     * CÓMO DEFENDERLO: "Verifico que las clases se guarden correctamente
     *                    en la BD local Room, permitiendo funcionalidad offline"
     */
    @Test
    fun test07_insertarClase_debeGuardarEnBD() = runBlocking {
        // Arrange (Preparar): Crear una clase de ejemplo
        val clase = ClaseEntity(
            id = "clase-test-001",
            nombre = "Introducción a Testing",
            descripcion = "Clase sobre JUnit y Room",
            fecha = "19/11/2025",
            archivoPdfUrl = "",
            archivoPdfNombre = "",
            creador = "docente-123",
            asignaturaId = "asig-001",
            sincronizado = false
        )

        // Act (Actuar): Guardar en BD
        claseDao.insertarClase(clase)

        // Recuperar de BD por ID
        val claseRecuperada = claseDao.obtenerClasePorId("clase-test-001")

        // Assert (Verificar): La clase debe existir y tener los datos correctos
        assertNotNull("Clase debe existir en BD", claseRecuperada)
        assertEquals("ID debe coincidir", "clase-test-001", claseRecuperada?.id)
        assertEquals("Nombre debe coincidir", "Introducción a Testing", claseRecuperada?.nombre)
        assertEquals("Creador debe coincidir", "docente-123", claseRecuperada?.creador)
    }

    /**
     * TEST 8: Obtener clases de un usuario específico
     *
     * QUÉ TESTEA: Query filtrada por creador (UID del docente)
     * POR QUÉ ES IMPORTANTE: Cada docente solo ve sus propias clases
     * CÓMO DEFENDERLO: "Verifico que las queries filtren correctamente por usuario,
     *                    garantizando que cada docente vea solo sus clases"
     */
    @Test
    fun test08_obtenerClasesPorUsuario_debeFiltrarCorrectamente() = runBlocking {
        // Arrange (Preparar): Insertar clases de dos usuarios diferentes
        val claseDocente1 = ClaseEntity(
            id = "clase-001",
            nombre = "Clase Docente 1",
            descripcion = "",
            fecha = "19/11/2025",
            archivoPdfUrl = "",
            archivoPdfNombre = "",
            creador = "docente-001",
            asignaturaId = "asig-001",
            sincronizado = false
        )

        val claseDocente2 = ClaseEntity(
            id = "clase-002",
            nombre = "Clase Docente 2",
            descripcion = "",
            fecha = "19/11/2025",
            archivoPdfUrl = "",
            archivoPdfNombre = "",
            creador = "docente-002",
            asignaturaId = "asig-002",
            sincronizado = false
        )

        // Insertar ambas clases
        claseDao.insertarClase(claseDocente1)
        claseDao.insertarClase(claseDocente2)

        // Act (Actuar): Obtener solo las clases del docente-001
        val clasesDocente1 = claseDao.obtenerClasesPorUsuario("docente-001").first()

        // Assert (Verificar): Solo debe retornar la clase del docente-001
        assertEquals("Docente 001 debe tener 1 clase", 1, clasesDocente1.size)
        assertEquals("ID debe ser clase-001", "clase-001", clasesDocente1[0].id)
        assertEquals("Creador debe ser docente-001", "docente-001", clasesDocente1[0].creador)

        // Verificación adicional: No debe incluir clases de otros docentes
        val contieneOtroDocente = clasesDocente1.any { it.creador == "docente-002" }
        assertFalse("No debe contener clases de otros docentes", contieneOtroDocente)
    }
}
