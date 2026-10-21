package br.airsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface AirSenseDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(device: AirSenseDevice)

    @Query("SELECT * FROM device ORDER BY id ASC")
    fun listar(): Flow<List<AirSenseDevice>>

    @Query("SELECT * FROM device WHERE userId = :userId ORDER BY id ASC")
    fun listarPorUsuario(userId: String): Flow<List<AirSenseDevice>>

    @Update
    suspend fun atualizar(device: AirSenseDevice)

    @Delete
    suspend fun excluir(device: AirSenseDevice)
}
