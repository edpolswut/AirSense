package br.airsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.airsense.model.AirSenseDevice

@Database(entities = [AirSenseDevice::class], version = 2, exportSchema = false)
abstract class AirSenseDatabase : RoomDatabase() {
    abstract fun airSenseDeviceDao(): AirSenseDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AirSenseDatabase? = null

        fun getDatabase(context: Context): AirSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AirSenseDatabase::class.java,
                    "airsense.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { INSTANCE = it }
            }
        }
    }
}
