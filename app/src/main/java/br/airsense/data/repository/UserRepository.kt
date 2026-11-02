package br.airsense.data.repository

import br.airsense.model.AuthResult
import br.airsense.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(id: String): Flow<User?>
    suspend fun register(name: String, email: String, password: String): AuthResult<User>
    suspend fun login(email: String, password: String): AuthResult<User>
    suspend fun updateProfile(userId: String, name: String, email: String): AuthResult<User>
    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit>
    suspend fun deleteAccount(userId: String): AuthResult<Unit>
}