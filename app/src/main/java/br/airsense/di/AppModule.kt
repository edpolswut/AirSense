package br.airsense.di

import android.content.Context
import br.airsense.data.local.AirSenseDatabase
import br.airsense.data.local.AirSenseDeviceDao
import br.airsense.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AirSenseDatabase {
        return AirSenseDatabase.getDatabase(context)
    }

    @Provides
    fun provideAirSenseDeviceDao(database: AirSenseDatabase): AirSenseDeviceDao {
        return database.airSenseDeviceDao()
    }

    @Provides
    fun provideUserDao(database: AirSenseDatabase): UserDao {
        return database.userDao()
    }
}