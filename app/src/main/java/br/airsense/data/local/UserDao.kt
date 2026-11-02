package br.airsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.airsense.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(user: User)

    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): User?

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): User?

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun observarPorId(id: String): Flow<User?>

    @Update
    suspend fun atualizar(user: User)

    @Delete
    suspend fun excluir(user: User)
}