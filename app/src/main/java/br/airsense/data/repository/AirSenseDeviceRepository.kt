package br.airsense.data.repository

import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

interface AirSenseDeviceRepository {
    fun getDevicesForUser(userId: String): Flow<List<AirSenseDevice>>
    suspend fun findByDeviceId(deviceId: String): AirSenseDevice?
    suspend fun saveDevice(device: AirSenseDevice)
    suspend fun deleteDevice(device: AirSenseDevice)
}