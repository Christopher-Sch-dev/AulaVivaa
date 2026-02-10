package cl.duocuc.aulaviva.data.repository

import android.app.Application
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.remote.SpringBootAsignaturaRepository
import cl.duocuc.aulaviva.data.remote.SpringBootAlumnoRepository
import cl.duocuc.aulaviva.data.remote.SpringBootClient
import cl.duocuc.aulaviva.domain.repository.IAsignaturasRepository
import cl.duocuc.aulaviva.domain.repository.IAlumnoRepository
import cl.duocuc.aulaviva.domain.repository.IClaseRepository
import cl.duocuc.aulaviva.domain.repository.IStorageRepository
import cl.duocuc.aulaviva.domain.repository.IIARepository
import cl.duocuc.aulaviva.domain.repository.IAuthRepository

object RepositoryProvider {
    // Simple memoization of repositories per process to avoid repeated instantiation.
    // Using the Application instance as the canonical scope (single app process).
    @Volatile
    private var asignaturasRepository: IAsignaturasRepository? = null

    @Volatile
    private var alumnoRepository: IAlumnoRepository? = null

    @Volatile
    private var claseRepository: IClaseRepository? = null

    @Volatile
    private var storageRepository: IStorageRepository? = null

    @Volatile
    private var iaRepository: IIARepository? = null

    @Volatile
    private var authRepository: IAuthRepository? = null

    @Synchronized
    fun provideAsignaturasRepository(application: Application): IAsignaturasRepository {
        return asignaturasRepository ?: run {
            val db = AppDatabase.getDatabase(application)
            val repo = AsignaturasRepository(
                asignaturaDao = db.asignaturaDao(),
                alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
                springBootRepository = SpringBootAsignaturaRepository(db.asignaturaDao(), db.alumnoAsignaturaDao(), SpringBootClient.apiService)
            )
            asignaturasRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideAlumnoRepository(application: Application): IAlumnoRepository {
        return alumnoRepository ?: run {
            val db = AppDatabase.getDatabase(application)
            val repo = AlumnoRepository(
                alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
                asignaturaDao = db.asignaturaDao(),
                springBootRepository = SpringBootAlumnoRepository(db.alumnoAsignaturaDao(), db.asignaturaDao(), SpringBootClient.apiService)
            )
            alumnoRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideClaseRepository(application: Application): IClaseRepository {
        return claseRepository ?: run {
            // OPTIMIZATION: Using Offline-First Optimized Repository (Phase 3)
            val repo = OfflineClasesRepository(application)
            claseRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideStorageRepository(application: Application): IStorageRepository {
        return storageRepository ?: run {
            val repo = StorageRepository(application)
            storageRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideIARepository(application: Application): IIARepository {
        return iaRepository ?: run {
            val repo = cl.duocuc.aulaviva.data.repository.IARepository(application)
            iaRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideAuthRepository(): IAuthRepository {
        return authRepository ?: run {
            val repo = cl.duocuc.aulaviva.data.repository.AuthRepository()
            authRepository = repo
            repo
        }
    }
}
