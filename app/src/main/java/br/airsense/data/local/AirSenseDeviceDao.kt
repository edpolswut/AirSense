package br.airsense.data.local

import androidx.room.*
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface AirSenseDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(device: AirSenseDevice)

    @Query("SELECT * FROM device ORDER BY id ASC")
    fun listar(): Flow<List<AirSenseDevice>>

    @Update
    suspend fun atualizar(device: AirSenseDevice)

    @Delete
    suspend fun excluir(device: AirSenseDevice)
}
