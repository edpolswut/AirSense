package br.airsense.di

import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.RoomAirSenseDeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAirSenseDeviceRepository(
        repository: RoomAirSenseDeviceRepository
    ): AirSenseDeviceRepository {
        return repository
    }
}
