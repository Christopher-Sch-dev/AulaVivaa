package cl.duocuc.aulaviva.data.repository

import android.app.Application
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository

object RepositoryProvider {
    // Simple memoization of repositories per process to avoid repeated instantiation.
    // Using the Application instance as the canonical scope (single app process).
    @Volatile
    private var asignaturasRepository: AsignaturasRepository? = null

    @Volatile
    private var alumnoRepository: AlumnoRepository? = null

    @Volatile
    private var claseRepository: ClaseRepository? = null

    @Volatile
    private var storageRepository: StorageRepository? = null

    @Volatile
    private var iaRepository: cl.duocuc.aulaviva.data.repository.IARepository? = null

    @Volatile
    private var authRepository: cl.duocuc.aulaviva.data.repository.AuthRepository? = null

    @Synchronized
    fun provideAsignaturasRepository(application: Application): AsignaturasRepository {
        return asignaturasRepository ?: run {
            val db = AppDatabase.getDatabase(application)
            val repo = AsignaturasRepository(
                asignaturaDao = db.asignaturaDao(),
                alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
                supabaseRepository = SupabaseAsignaturaRepository(db.asignaturaDao(), db.alumnoAsignaturaDao())
            )
            asignaturasRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideAlumnoRepository(application: Application): AlumnoRepository {
        return alumnoRepository ?: run {
            val db = AppDatabase.getDatabase(application)
            val repo = AlumnoRepository(
                alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
                asignaturaDao = db.asignaturaDao(),
                supabaseRepository = cl.duocuc.aulaviva.data.supabase.SupabaseAlumnoRepository(db.alumnoAsignaturaDao(), db.asignaturaDao())
            )
            alumnoRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideClaseRepository(application: Application): ClaseRepository {
        return claseRepository ?: run {
            val repo = ClaseRepository(application)
            claseRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideStorageRepository(application: Application): StorageRepository {
        return storageRepository ?: run {
            val repo = StorageRepository(application.applicationContext)
            storageRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideIARepository(application: Application): cl.duocuc.aulaviva.data.repository.IARepository {
        return iaRepository ?: run {
            val repo = cl.duocuc.aulaviva.data.repository.IARepository(application.applicationContext)
            iaRepository = repo
            repo
        }
    }

    @Synchronized
    fun provideAuthRepository(): cl.duocuc.aulaviva.data.repository.AuthRepository {
        return authRepository ?: run {
            val repo = cl.duocuc.aulaviva.data.repository.AuthRepository()
            authRepository = repo
            repo
        }
    }
}
