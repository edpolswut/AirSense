package br.airsense.data.repository

import android.database.sqlite.SQLiteConstraintException
import br.airsense.data.local.UserDao
import br.airsense.model.AuthError
import br.airsense.model.AuthResult
import br.airsense.model.SyncState
import br.airsense.model.User
import br.airsense.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private fun normalizeEmail(email: String): String = email.trim().lowercase()

class RoomUserRepository @Inject constructor(
    private val dao: UserDao,
    private val hasher: PasswordHasher
) : UserRepository {

    override fun observeUser(id: String): Flow<User?> = dao.observarPorId(id)

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AuthResult<User> {
        val normalizedEmail = normalizeEmail(email)
        if (dao.buscarPorEmail(normalizedEmail) != null) {
            return AuthResult.Failure(AuthError.EMAIL_ALREADY_EXISTS)
        }

        val salt = hasher.generateSalt()
        val user = User(
            name = name.trim(),
            email = normalizedEmail,
            passwordHash = hasher.hash(password, salt),
            salt = salt
        )
        return try {
            dao.inserir(user)
            AuthResult.Success(user)
        } catch (e: SQLiteConstraintException) {
            AuthResult.Failure(AuthError.EMAIL_ALREADY_EXISTS)
        }
    }

    override suspend fun login(email: String, password: String): AuthResult<User> {
        val user = dao.buscarPorEmail(normalizeEmail(email))
            ?: return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)

        // Mesmo erro para e-mail inexistente e senha errada, para não revelar quais e-mails existem.
        return if (hasher.verify(password, user.salt, user.passwordHash)) {
            AuthResult.Success(user)
        } else {
            AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        name: String,
        email: String
    ): AuthResult<User> {
        val current = dao.buscarPorId(userId)
            ?: return AuthResult.Failure(AuthError.USER_NOT_FOUND)

        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail != current.email) {
            val other = dao.buscarPorEmail(normalizedEmail)
            if (other != null && other.id != current.id) {
                return AuthResult.Failure(AuthError.EMAIL_ALREADY_EXISTS)
            }
        }

        val updated = current.copy(
            name = name.trim(),
            email = normalizedEmail,
            updatedAt = System.currentTimeMillis(),
            syncState = SyncState.PENDING.code
        )
        return try {
            dao.atualizar(updated)
            AuthResult.Success(updated)
        } catch (e: SQLiteConstraintException) {
            AuthResult.Failure(AuthError.EMAIL_ALREADY_EXISTS)
        }
    }

    override suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit> {
        val current = dao.buscarPorId(userId)
            ?: return AuthResult.Failure(AuthError.USER_NOT_FOUND)

        if (!hasher.verify(currentPassword, current.salt, current.passwordHash)) {
            return AuthResult.Failure(AuthError.WRONG_CURRENT_PASSWORD)
        }

        val newSalt = hasher.generateSalt()
        dao.atualizar(
            current.copy(
                passwordHash = hasher.hash(newPassword, newSalt),
                salt = newSalt,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING.code
            )
        )
        return AuthResult.Success(Unit)
    }

    override suspend fun deleteAccount(userId: String): AuthResult<Unit> {
        val user = dao.buscarPorId(userId)
            ?: return AuthResult.Failure(AuthError.USER_NOT_FOUND)
        dao.excluir(user)
        return AuthResult.Success(Unit)
    }
}