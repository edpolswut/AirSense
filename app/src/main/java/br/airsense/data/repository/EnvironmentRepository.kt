package br.airsense.data.repository

import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    fun getEnvironmentsForUser(userId: String): Flow<List<Environment>>
    fun getAllEnvironments(): Flow<List<Environment>>
    fun getRoomsForEnvironment(environmentId: Int): Flow<List<EnvironmentRoom>>
    fun getAllRooms(): Flow<List<EnvironmentRoom>>
    suspend fun findEnvironmentByName(userId: String, name: String): Environment?
    suspend fun findRoomByName(environmentId: Int, name: String): EnvironmentRoom?
    suspend fun saveEnvironment(environment: Environment)
    suspend fun deleteEnvironment(environment: Environment)
    suspend fun saveRoom(room: EnvironmentRoom)
    suspend fun deleteRoom(room: EnvironmentRoom)
}