package br.airsense.data.repository

import br.airsense.data.local.AirSenseDeviceDao
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

class AirSenseDeviceRepository(
    private val dao: AirSenseDeviceDao
) {
    fun getDevices(): Flow<List<AirSenseDevice>> = dao.listar()

    suspend fun saveDevice(device: AirSenseDevice) {
        if (device.id == 0) {
            dao.inserir(device)
        } else {
            dao.atualizar(device)
        }
    }

    suspend fun deleteDevice(device: AirSenseDevice) {
        dao.excluir(device)
    }
}
