package br.airsense.data.local

import androidx.room.*
import br.airsense.model.AirSenseDevice

@Dao
interface AirSenseDeviceDao {
    @Insert
    suspend fun inserir(tarefa: AirSenseDevice)

    @Query("SELECT * FROM device ORDER BY id DESC")
    suspend fun listar(): List<AirSenseDevice>

    @Update
    suspend fun atualizar(tarefa: AirSenseDevice)

    @Delete
    suspend fun excluir(tarefa: AirSenseDevice)
}