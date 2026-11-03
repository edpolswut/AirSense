package br.airsense.data.repository

import br.airsense.model.AirQualityReading
import kotlinx.coroutines.flow.Flow

interface AirQualityRepository {
    fun getAllReadings(): Flow<List<AirQualityReading>>
    fun getReadingsForEnvironment(environmentId: Int): Flow<List<AirQualityReading>>
    suspend fun saveReading(reading: AirQualityReading)
}