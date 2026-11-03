package br.airsense.data.repository

import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    fun getEnvironmentsForUser(userId: String): Flow<List<Environment>>
    fun observeEnvironment(id: Int): Flow<Environment?>
    fun observeRoom(id: Int): Flow<EnvironmentRoom?>
    fun getRoomsForEnvironment(environmentId: Int): Flow<List<EnvironmentRoom>>
    fun getRoomsForUser(userId: String): Flow<List<EnvironmentRoom>>
    suspend fun findEnvironmentByName(userId: String, name: String): Environment?
    suspend fun findRoomByName(environmentId: Int, name: String): EnvironmentRoom?
    suspend fun saveEnvironment(environment: Environment)
    suspend fun deleteEnvironment(environment: Environment)
    suspend fun saveRoom(room: EnvironmentRoom)
    suspend fun deleteRoom(room: EnvironmentRoom)
}