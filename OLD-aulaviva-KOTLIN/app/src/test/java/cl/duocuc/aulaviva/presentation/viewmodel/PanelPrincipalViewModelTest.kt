package cl.duocuc.aulaviva.presentation.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import cl.duocuc.aulaviva.data.repository.RepositoryProvider
import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import cl.duocuc.aulaviva.domain.usecase.SincronizarAsignaturasUseCase
import cl.duocuc.aulaviva.domain.usecase.SincronizarClasesUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
class PanelPrincipalViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    // Mock dependencies
    private val application: Application = mockk(relaxed = true)
    private val authRepository: IAuthRepository = mockk(relaxed = true)
    private val asignaturasRepo: IAsignaturasRepository = mockk(relaxed = true)
    private val clasesRepo: IClaseRepository = mockk(relaxed = true)
    private val storageRepo: IStorageRepository = mockk(relaxed = true)
    
    // UseCases mocks
    private val syncAsignaturasUseCase: SincronizarAsignaturasUseCase = mockk(relaxed = true)
    private val syncClasesUseCase: SincronizarClasesUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Log to avoid "Method not mocked" error
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        
        // Mock static RepositoryProvider
        mockkObject(RepositoryProvider)
        every { RepositoryProvider.provideAuthRepository() } returns authRepository
        every { RepositoryProvider.provideAsignaturasRepository(any()) } returns asignaturasRepo
        every { RepositoryProvider.provideClaseRepository(any()) } returns clasesRepo
        every { RepositoryProvider.provideStorageRepository(any()) } returns storageRepo

        // Setup mock responses
        coEvery { syncAsignaturasUseCase.invoke() } returns Result.success(Unit)
        coEvery { syncClasesUseCase.invoke() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
        unmockkObject(RepositoryProvider)
    }

    @Test
    fun `refreshData should call sync use cases`() = runTest {
        // Arrange
        // We need to inject our mock UseCases into the ViewModel. 
        // Since they are private vals initialized inline, we use reflection.
        val viewModel = PanelPrincipalViewModel(application)
        
        replacePrivateField(viewModel, "sincronizarAsignaturasUseCase", syncAsignaturasUseCase)
        replacePrivateField(viewModel, "sincronizarClasesUseCase", syncClasesUseCase)

        // Act
        viewModel.refreshData()
        advanceUntilIdle() // Wait for coroutines

        // Assert
        coVerify { syncAsignaturasUseCase.invoke() }
        coVerify { syncClasesUseCase.invoke() }
    }

    @Test
    fun `crearAsignaturaYClaseDemo should not call repository if already loading`() = runTest {
        // Arrange
        val viewModel = PanelPrincipalViewModel(application)
        
        val mockAsignatura = cl.duocuc.aulaviva.data.model.Asignatura(
            id = "A1", 
            nombre = "Demo", 
            descripcion = "Desc", 
            codigoAcceso = "DEMO123", 
            docenteId = "User1"  // Fixed parameter name
        )
        
        replacePrivateField(viewModel, "asignaturasRepo", asignaturasRepo)
        
        // Use slot to allow delay simulation
        coEvery { asignaturasRepo.crearAsignatura(any(), any()) } coAnswers {
            delay(100) 
            Result.success(mockAsignatura)
        }

        // Mock storage to prevent NPE on result handling
        every { RepositoryProvider.provideStorageRepository(any()) } returns storageRepo
        coEvery { storageRepo.subirPdfDesdeUrl(any(), any()) } returns Result.success("http://url.pdf")

        // Act
        val job1 = launch { viewModel.crearAsignaturaYClaseDemo() }
        val job2 = launch { viewModel.crearAsignaturaYClaseDemo() }
        
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { asignaturasRepo.crearAsignatura(any(), any()) }
        
        job1.cancel()
        job2.cancel()
    }

    // Helper to inject mocks into private fields
    private fun replacePrivateField(target: Any, fieldName: String, value: Any) {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
