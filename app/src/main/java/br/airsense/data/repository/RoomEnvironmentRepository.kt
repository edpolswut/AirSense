package br.airsense.data.repository

import br.airsense.data.local.AirSenseDeviceDao
import br.airsense.data.local.EnvironmentDao
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomEnvironmentRepository @Inject constructor(
    private val environmentDao: EnvironmentDao,
    private val deviceDao: AirSenseDeviceDao
) : EnvironmentRepository {

    override fun getEnvironmentsForUser(userId: String): Flow<List<Environment>> =
        environmentDao.listarAmbientesPorUsuario(userId)

    override fun observeEnvironment(id: Int): Flow<Environment?> = environmentDao.observarAmbiente(id)

    override fun observeRoom(id: Int): Flow<EnvironmentRoom?> = environmentDao.observarComodo(id)

    override fun getRoomsForEnvironment(environmentId: Int): Flow<List<EnvironmentRoom>> =
        environmentDao.listarComodosPorAmbiente(environmentId)

    override fun getRoomsForUser(userId: String): Flow<List<EnvironmentRoom>> =
        environmentDao.listarComodosPorUsuario(userId)

    override suspend fun findEnvironmentByName(userId: String, name: String): Environment? =
        environmentDao.buscarAmbientePorNome(userId, name.trim())

    override suspend fun findRoomByName(environmentId: Int, name: String): EnvironmentRoom? =
        environmentDao.buscarComodoPorNome(environmentId, name.trim())

    override suspend fun saveEnvironment(environment: Environment) {
        if (environment.id == 0) {
            environmentDao.inserirAmbiente(environment)
        } else {
            environmentDao.atualizarAmbiente(environment)
        }
    }

    override suspend fun deleteEnvironment(environment: Environment) {
        deviceDao.desvincularAmbiente(environment.id)
        environmentDao.excluirAmbiente(environment)
    }

    override suspend fun saveRoom(room: EnvironmentRoom) {
        if (room.id == 0) {
            environmentDao.inserirComodo(room)
        } else {
            environmentDao.atualizarComodo(room)
        }
    }

    override suspend fun deleteRoom(room: EnvironmentRoom) {
        deviceDao.desvincularComodo(room.id)
        environmentDao.excluirComodo(room)
    }
}