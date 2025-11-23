package cl.duocuc.aulaviva.data.repository

import android.app.Application
import cl.duocuc.aulaviva.data.local.AppDatabase
import cl.duocuc.aulaviva.data.supabase.SupabaseAsignaturaRepository

object RepositoryProvider {
    fun provideAsignaturasRepository(application: Application): AsignaturasRepository {
        val db = AppDatabase.getDatabase(application)
        return AsignaturasRepository(
            asignaturaDao = db.asignaturaDao(),
            alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
            supabaseRepository = SupabaseAsignaturaRepository(db.asignaturaDao(), db.alumnoAsignaturaDao())
        )
    }

    fun provideAlumnoRepository(application: Application): AlumnoRepository {
        val db = AppDatabase.getDatabase(application)
        return AlumnoRepository(
            alumnoAsignaturaDao = db.alumnoAsignaturaDao(),
            asignaturaDao = db.asignaturaDao(),
            supabaseRepository = cl.duocuc.aulaviva.data.supabase.SupabaseAlumnoRepository(db.alumnoAsignaturaDao(), db.asignaturaDao())
        )
    }

    fun provideClaseRepository(application: Application): ClaseRepository {
        return ClaseRepository(application)
    }

    fun provideStorageRepository(application: Application): StorageRepository {
        return StorageRepository(application.applicationContext)
    }

    fun provideIARepository(application: Application): cl.duocuc.aulaviva.data.repository.IARepository {
        return cl.duocuc.aulaviva.data.repository.IARepository(application.applicationContext)
    }
}
