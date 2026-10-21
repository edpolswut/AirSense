package br.airsense.di

import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.RoomAirSenseDeviceRepository
import br.airsense.data.repository.RoomUserRepository
import br.airsense.data.repository.UserRepository
import br.airsense.security.PasswordHasher
import br.airsense.security.Sha256PasswordHasher
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

    @Provides
    @Singleton
    fun provideUserRepository(
        repository: RoomUserRepository
    ): UserRepository {
        return repository
    }

    @Provides
    @Singleton
    fun providePasswordHasher(
        hasher: Sha256PasswordHasher
    ): PasswordHasher {
        return hasher
    }
}