//package br.airsense.data.local
//
//import androidx.room.*
//import br.airsense.model.Environment
//
//@Dao
//interface EnvironmentDao {
//    @Insert
//    suspend fun inserir(tarefa: Environment)
//
//    @Query("SELECT * FROM environment ORDER BY id DESC")
//    suspend fun listar(): List<Environment>
//
//    @Update
//    suspend fun atualizar(tarefa: Environment)
//
//    @Delete
//    suspend fun excluir(tarefa: Environment)
//}