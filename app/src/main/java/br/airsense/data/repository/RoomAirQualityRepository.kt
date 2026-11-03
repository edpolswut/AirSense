package br.airsense.data.repository

import br.airsense.data.local.AirQualityReadingDao
import br.airsense.model.AirQualityReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomAirQualityRepository @Inject constructor(
    private val dao: AirQualityReadingDao
) : AirQualityRepository {
    override fun getAllReadings(): Flow<List<AirQualityReading>> = dao.listarTodasLeituras()

    override fun getReadingsForEnvironment(environmentId: Int): Flow<List<AirQualityReading>> =
        dao.listarLeiturasPorAmbiente(environmentId)

    override suspend fun saveReading(reading: AirQualityReading) {
        dao.inserirLeitura(reading)
    }
}