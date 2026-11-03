package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.DeviceStatus
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.util.AirQualityClassifier
import br.airsense.util.AirQualitySnapshot
import br.airsense.util.AirQualityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EnvironmentSummary(
    val environment: Environment,
    val rooms: List<EnvironmentRoom>,
    val devicesCount: Int,
    val snapshot: AirQualitySnapshot
) {
    val status: AirQualityStatus get() = snapshot.status
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalDevices: Int = 0,
    val activeDevices: Int = 0,
    val environmentsSummary: List<EnvironmentSummary> = emptyList(),
    val overallStatus: AirQualityStatus = AirQualityStatus.NO_DATA
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    environmentRepository: EnvironmentRepository,
    deviceRepository: AirSenseDeviceRepository,
    airQualityRepository: AirQualityRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.userIdFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> forUser(query: (String) -> Flow<List<T>>): Flow<List<T>> =
        userId.flatMapLatest { id -> if (id.isNullOrBlank()) flowOf(emptyList()) else query(id) }

    val uiState: StateFlow<HomeUiState> = combine(
        forUser(environmentRepository::getEnvironmentsForUser),
        forUser(environmentRepository::getRoomsForUser),
        forUser(deviceRepository::getDevicesForUser),
        forUser(airQualityRepository::getReadingsForUser)
    ) { environments, rooms, devices, readings ->
        val roomsByEnv = rooms.groupBy { it.environmentId }
        val latest = AirQualitySnapshot.latestByDevice(readings)

        val summaries = environments.map { env ->
            val envDevices = devices.filter { it.environmentId == env.id }
            EnvironmentSummary(
                environment = env,
                rooms = roomsByEnv[env.id].orEmpty(),
                devicesCount = envDevices.size,
                snapshot = AirQualitySnapshot.of(envDevices, latest)
            )
        }

        val withData = summaries.mapNotNull { it.snapshot.aqi }
        HomeUiState(
            isLoading = false,
            totalDevices = devices.size,
            activeDevices = devices.count { it.deviceStatus == DeviceStatus.ACTIVE },
            environmentsSummary = summaries,
            overallStatus = if (withData.isEmpty()) AirQualityStatus.NO_DATA
            else AirQualityClassifier.classify(withData.average().toFloat())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
