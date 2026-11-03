package br.airsense.data.repository

import br.airsense.data.local.AirQualityReadingDao
import br.airsense.data.local.AirSenseDeviceDao
import androidx.room.withTransaction
import br.airsense.data.local.AirSenseDatabase
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomAirSenseDeviceRepository @Inject constructor(
    private val db: AirSenseDatabase,
    private val dao: AirSenseDeviceDao,
    private val readingDao: AirQualityReadingDao
) : AirSenseDeviceRepository {

    override fun getDevicesForUser(userId: String): Flow<List<AirSenseDevice>> =
        dao.listarPorUsuario(userId)

    override suspend fun findByDeviceId(deviceId: String): AirSenseDevice? =
        dao.buscarPorDeviceId(deviceId.trim())

    override suspend fun saveDevice(device: AirSenseDevice) {
        if (device.id == 0) {
            dao.inserir(device)
            return
        }
        db.withTransaction {
            val old = dao.buscarPorId(device.id)
            if (old != null && !old.deviceId.equals(device.deviceId, ignoreCase = true)) {
                readingDao.renomearDispositivo(old.deviceId, device.deviceId)
            }
            dao.atualizar(device)
        }
    }

    override suspend fun deleteDevice(device: AirSenseDevice) {
        db.withTransaction {
            readingDao.excluirLeiturasDoDispositivo(device.id, device.deviceId)
            dao.excluir(device)
        }
    }
}