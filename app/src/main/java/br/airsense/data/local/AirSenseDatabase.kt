package br.airsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.model.User

@Database(
    entities = [
        AirSenseDevice::class,
        User::class,
        Environment::class,
        EnvironmentRoom::class,
        AirQualityReading::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AirSenseDatabase : RoomDatabase() {
    abstract fun airSenseDeviceDao(): AirSenseDeviceDao
    abstract fun userDao(): UserDao
    abstract fun environmentDao(): EnvironmentDao
    abstract fun airQualityReadingDao(): AirQualityReadingDao

    companion object {
        @Volatile
        private var INSTANCE: AirSenseDatabase? = null

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
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_email` ON `user` (`email`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `device` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_userId` ON `device` (`userId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `environment` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_environment_userId` ON `environment` (`userId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `room` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `environmentId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        FOREIGN KEY(`environmentId`) REFERENCES `environment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_room_environmentId` ON `room` (`environmentId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `air_quality_reading` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `environmentId` INTEGER,
                        `roomId` INTEGER,
                        `temperature` REAL NOT NULL,
                        `humidity` REAL NOT NULL,
                        `airQualityIndex` REAL NOT NULL,
                        `co2Level` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_air_quality_reading_deviceId` ON `air_quality_reading` (`deviceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_air_quality_reading_environmentId` ON `air_quality_reading` (`environmentId`)")
            }
        }

        // Bloqueio de login (US007) e configuração do cômodo: área e limites seguros (US003/US015).
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user` ADD COLUMN `failedLoginAttempts` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user` ADD COLUMN `lockedUntil` INTEGER NOT NULL DEFAULT 0")
                listOf(
                    "`areaM2` REAL NOT NULL DEFAULT 0",
                    "`useCustomLimits` INTEGER NOT NULL DEFAULT 0",
                    "`tempMinSafe` REAL NOT NULL DEFAULT 18",
                    "`tempMaxSafe` REAL NOT NULL DEFAULT 26",
                    "`humidityMinSafe` REAL NOT NULL DEFAULT 30",
                    "`humidityMaxSafe` REAL NOT NULL DEFAULT 60",
                    "`airQualityMinSafe` REAL NOT NULL DEFAULT 0",
                    "`airQualityMaxSafe` REAL NOT NULL DEFAULT 100",
                    "`co2MinSafe` REAL NOT NULL DEFAULT 400",
                    "`co2MaxSafe` REAL NOT NULL DEFAULT 1000"
                ).forEach { db.execSQL("ALTER TABLE `room` ADD COLUMN $it") }
            }
        }

        fun getDatabase(context: Context): AirSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AirSenseDatabase::class.java,
                    "airsense.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationFrom(true, 1)
                    .build().also { INSTANCE = it }
            }
        }
    }
}