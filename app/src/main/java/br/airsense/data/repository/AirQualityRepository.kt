package br.airsense.data.repository

import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

interface AirQualityRepository {
    fun getReadingsForUser(userId: String): Flow<List<AirQualityReading>>
    suspend fun saveReading(reading: AirQualityReading)

    /** Cria o histórico de demonstração do dispositivo; devolve quantas leituras foram criadas. */
    suspend fun generateHistory(device: AirSenseDevice, days: Int): Int
}