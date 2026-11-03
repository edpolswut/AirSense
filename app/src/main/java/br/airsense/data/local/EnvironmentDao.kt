package br.airsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirAmbiente(environment: Environment): Long

    @Update
    suspend fun atualizarAmbiente(environment: Environment)

    @Delete
    suspend fun excluirAmbiente(environment: Environment)

    @Query("SELECT * FROM environment WHERE userId = :userId ORDER BY id ASC")
    fun listarAmbientesPorUsuario(userId: String): Flow<List<Environment>>

    @Query("SELECT * FROM environment WHERE id = :id LIMIT 1")
    fun observarAmbiente(id: Int): Flow<Environment?>

    @Query("SELECT * FROM room WHERE id = :id LIMIT 1")
    fun observarComodo(id: Int): Flow<EnvironmentRoom?>

    @Query("SELECT * FROM environment WHERE userId = :userId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun buscarAmbientePorNome(userId: String, name: String): Environment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirComodo(room: EnvironmentRoom): Long

    @Update
    suspend fun atualizarComodo(room: EnvironmentRoom)

    @Delete
    suspend fun excluirComodo(room: EnvironmentRoom)

    @Query("SELECT * FROM room WHERE environmentId = :environmentId ORDER BY id ASC")
    fun listarComodosPorAmbiente(environmentId: Int): Flow<List<EnvironmentRoom>>

    @Query(
        """
        SELECT room.* FROM room
        INNER JOIN environment e ON e.id = room.environmentId
        WHERE e.userId = :userId
        ORDER BY room.id ASC
        """
    )
    fun listarComodosPorUsuario(userId: String): Flow<List<EnvironmentRoom>>

    @Query("SELECT * FROM room WHERE environmentId = :environmentId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun buscarComodoPorNome(environmentId: Int, name: String): EnvironmentRoom?
}