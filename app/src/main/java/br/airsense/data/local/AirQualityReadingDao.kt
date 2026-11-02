package br.airsense.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.airsense.model.AirQualityReading
import kotlinx.coroutines.flow.Flow

@Dao
interface AirQualityReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirLeitura(reading: AirQualityReading)

    @Query("SELECT * FROM air_quality_reading ORDER BY timestamp DESC")
    fun listarTodasLeituras(): Flow<List<AirQualityReading>>

    @Query("SELECT * FROM air_quality_reading WHERE environmentId = :environmentId ORDER BY timestamp DESC")
    fun listarLeiturasPorAmbiente(environmentId: Int): Flow<List<AirQualityReading>>

    @Query("SELECT * FROM air_quality_reading WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun listarLeiturasPorDispositivo(deviceId: String): Flow<List<AirQualityReading>>
}