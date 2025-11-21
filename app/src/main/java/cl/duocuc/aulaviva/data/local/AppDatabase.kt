package cl.duocuc.aulaviva.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de Room para Aula Viva.
 * Esta es la BD que funciona sin internet, guardada en el celular.
 *
 * @Database indica las tablas (entities) y la versión de la BD.
 * version = 3 para incluir asignaturas y alumno_asignaturas.
 * exportSchema = false para no generar archivos de esquema (simplifica el desarrollo).
 *
 * Patrón Singleton: Solo existe UNA instancia de la BD en toda la app.
 * Esto evita problemas de múltiples conexiones y ahorra memoria.
 */
@Database(
    entities = [
        ClaseEntity::class,
        AsignaturaEntity::class,
        AlumnoAsignaturaEntity::class
    ],
    version = 3, // Versión 3: asignaturas + alumno_asignaturas + asignaturaId en clases
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Métodos abstractos que Room implementa automáticamente
    abstract fun claseDao(): ClaseDao
    abstract fun asignaturaDao(): AsignaturaDao
    abstract fun alumnoAsignaturaDao(): AlumnoAsignaturaDao

    companion object {
        // @Volatile asegura que los cambios sean visibles en todos los threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos.
         * Si no existe, la crea. Si ya existe, la retorna.
         *
         * synchronized() evita que dos threads creen la BD al mismo tiempo.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe la instancia, la retorno
            return INSTANCE ?: synchronized(this) {
                // Si no existe, la creo
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aulaviva_database"  // Nombre del archivo de la BD en el celular
                )
                    .fallbackToDestructiveMigration()  // Si cambio el esquema, borra y recrea (OK para desarrollo)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
