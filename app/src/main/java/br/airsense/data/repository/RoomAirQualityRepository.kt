package br.airsense.data.repository

import br.airsense.data.local.AirQualityReadingDao
import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.util.ReadingHistoryGenerator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomAirQualityRepository @Inject constructor(
    private val dao: AirQualityReadingDao
) : AirQualityRepository {
    override fun getReadingsForUser(userId: String): Flow<List<AirQualityReading>> =
        dao.listarLeiturasPorUsuario(userId)

    override suspend fun saveReading(reading: AirQualityReading) {
        dao.inserirLeitura(reading)
    }

    override suspend fun generateHistory(device: AirSenseDevice, days: Int): Int {
        val now = System.currentTimeMillis()
        val existing = dao.horariosDoDispositivo(device.deviceId, now - days * 24 * 60 * 60 * 1000L)
        val readings = ReadingHistoryGenerator.generate(device, now, days, existing)
        if (readings.isNotEmpty()) dao.inserirLeituras(readings)
        return readings.size
    }
}