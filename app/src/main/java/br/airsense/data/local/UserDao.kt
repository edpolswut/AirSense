package br.airsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query(
        """
        DELETE FROM air_quality_reading WHERE LOWER(deviceId) IN
            (SELECT LOWER(deviceId) FROM device WHERE userId = :userId)
          AND LOWER(deviceId) NOT IN
            (SELECT LOWER(deviceId) FROM device WHERE userId != :userId)
        """
    )
    suspend fun excluirLeiturasDoUsuario(userId: String)

    @Query("DELETE FROM device WHERE userId = :userId")
    suspend fun excluirDispositivosDoUsuario(userId: String)

    // Os cômodos saem junto pela chave estrangeira (CASCADE).
    @Query("DELETE FROM environment WHERE userId = :userId")
    suspend fun excluirAmbientesDoUsuario(userId: String)

    /** Apaga o usuário e tudo o que pertence a ele, de uma vez. */
    @Transaction
    suspend fun excluirComDados(user: User) {
        excluirLeiturasDoUsuario(user.id)
        excluirDispositivosDoUsuario(user.id)
        excluirAmbientesDoUsuario(user.id)
        excluir(user)
    }
}