package br.airsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.airsense.model.AirSenseDevice
import br.airsense.model.User

@Database(entities = [AirSenseDevice::class, User::class], version = 3, exportSchema = false)
abstract class AirSenseDatabase : RoomDatabase() {
    abstract fun airSenseDeviceDao(): AirSenseDeviceDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AirSenseDatabase? = null

        /** Cria a tabela de usuários sem tocar nos dispositivos já cadastrados. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `salt` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `syncState` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_email` ON `user` (`email`)"
                )
            }
        }

        fun getDatabase(context: Context): AirSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AirSenseDatabase::class.java,
                    "airsense.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Só a versão 1 (anterior à migração) ainda é descartada.
                    .fallbackToDestructiveMigrationFrom(true, 1)
                    .build().also { INSTANCE = it }
            }
        }
    }
}