package br.airsense.data.repository

import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

interface AirSenseDeviceRepository {
    fun getDevices(): Flow<List<AirSenseDevice>>
    suspend fun saveDevice(device: AirSenseDevice)
    suspend fun deleteDevice(device: AirSenseDevice)
}
