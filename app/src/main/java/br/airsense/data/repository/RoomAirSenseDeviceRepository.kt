package br.airsense.data.repository

import br.airsense.data.local.AirSenseDeviceDao
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomAirSenseDeviceRepository @Inject constructor(
    private val dao: AirSenseDeviceDao
) : AirSenseDeviceRepository {

    override fun getDevices(): Flow<List<AirSenseDevice>> = dao.listar()

    override fun getDevicesForUser(userId: String): Flow<List<AirSenseDevice>> =
        dao.listarPorUsuario(userId)

    override suspend fun saveDevice(device: AirSenseDevice) {
        if (device.id == 0) {
            dao.inserir(device)
        } else {
            dao.atualizar(device)
        }
    }

    override suspend fun deleteDevice(device: AirSenseDevice) {
        dao.excluir(device)
    }
}
