package br.airsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.airsense.model.AirSenseDevice
//import br.airsense.model.Environment

@Database(entities = [AirSenseDevice::class], version = 1, exportSchema = false)
abstract class AirSenseDatabase : RoomDatabase() {
    abstract fun airSenseDeviceDao(): AirSenseDeviceDao
    //abstract fun environmentDao(): EnvironmentDao

    companion object {
        @Volatile
        private var INSTANCE: AirSenseDatabase? = null

        fun getDatabase(context: Context): AirSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AirSenseDatabase::class.java,
                    "airsense.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}