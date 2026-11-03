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

    @Insert
    suspend fun inserirLeituras(readings: List<AirQualityReading>)

    @Query("SELECT timestamp FROM air_quality_reading WHERE LOWER(deviceId) = LOWER(:deviceId) AND timestamp >= :from")
    suspend fun horariosDoDispositivo(deviceId: String, from: Long): List<Long>

    // As leituras não guardam o dono; o dono vem do dispositivo com o mesmo identificador.
    @Query(
        """
        SELECT r.* FROM air_quality_reading r
        INNER JOIN device d ON LOWER(d.deviceId) = LOWER(r.deviceId)
        WHERE d.userId = :userId
        ORDER BY r.timestamp DESC
        """
    )
    fun listarLeiturasPorUsuario(userId: String): Flow<List<AirQualityReading>>

    @Query("SELECT * FROM air_quality_reading WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun listarLeiturasPorDispositivo(deviceId: String): Flow<List<AirQualityReading>>

    /** Quando o identificador do dispositivo muda, as leituras dele acompanham. */
    @Query("UPDATE air_quality_reading SET deviceId = :newDeviceId WHERE LOWER(deviceId) = LOWER(:oldDeviceId)")
    suspend fun renomearDispositivo(oldDeviceId: String, newDeviceId: String)

    // Só apaga se nenhum outro dispositivo (de outro usuário) usar o mesmo identificador.
    @Query(
        """
        DELETE FROM air_quality_reading
        WHERE LOWER(deviceId) = LOWER(:deviceId)
          AND NOT EXISTS (SELECT 1 FROM device WHERE LOWER(device.deviceId) = LOWER(:deviceId) AND device.id != :deviceRowId)
        """
    )
    suspend fun excluirLeiturasDoDispositivo(deviceRowId: Int, deviceId: String)
}