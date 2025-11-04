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
 * version = 1 porque es la primera versión de mi esquema.
 * exportSchema = false para no generar archivos de esquema (simplifica el desarrollo).
 *
 * Patrón Singleton: Solo existe UNA instancia de la BD en toda la app.
 * Esto evita problemas de múltiples conexiones y ahorra memoria.
 */
@Database(
    entities = [ClaseEntity::class],  // Lista de tablas (por ahora solo clases)
    version = 2, // subo versión a 2 por cambios en el esquema (nuevos campos)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Método abstracto que Room implementa automáticamente
    abstract fun claseDao(): ClaseDao

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
