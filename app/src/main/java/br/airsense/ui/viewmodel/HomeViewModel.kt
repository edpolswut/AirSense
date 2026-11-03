package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.util.AirQualityClassifier
import br.airsense.util.AirQualityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnvironmentSummary(
    val environment: Environment,
    val rooms: List<EnvironmentRoom>,
    val devicesCount: Int,
    val avgAqi: Float,
    val avgTemp: Float,
    val avgHumidity: Float,
    val avgCo2: Float,
    val status: AirQualityStatus
)

data class HomeUiState(
    val totalDevices: Int = 0,
    val activeDevices: Int = 0,
    val environmentsSummary: List<EnvironmentSummary> = emptyList(),
    val overallStatus: AirQualityStatus = AirQualityStatus.GOOD
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val deviceRepository: AirSenseDeviceRepository,
    private val airQualityRepository: AirQualityRepository,
    sessionManager: SessionManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userEnvironments: Flow<List<Environment>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            environmentRepository.getEnvironmentsForUser(userId)
        } else {
            environmentRepository.getAllEnvironments()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userDevices: Flow<List<AirSenseDevice>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            deviceRepository.getDevicesForUser(userId)
        } else {
            deviceRepository.getDevices()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _userEnvironments,
        environmentRepository.getAllRooms(),
        _userDevices,
        airQualityRepository.getAllReadings()
    ) { environments, allRooms, devices, readings ->
        val roomsByEnv = allRooms.groupBy { it.environmentId }
        val readingsByEnv = readings.groupBy { it.environmentId }

        val summaries = environments.map { env ->
            val envRooms = roomsByEnv[env.id].orEmpty()
            val envDevices = devices.filter { it.environmentId == env.id }
            val envReadings = readingsByEnv[env.id].orEmpty()

            // Pega a leitura mais recente em vez de tirar a média de todo o histórico acumulado
            val latestReading = envReadings.firstOrNull()

            val currentAqi = latestReading?.airQualityIndex ?: 25.0f
            val currentTemp = latestReading?.temperature ?: 22.0f
            val currentHumidity = latestReading?.humidity ?: 50.0f
            val currentCo2 = latestReading?.co2Level ?: 450.0f

            EnvironmentSummary(
                environment = env,
                rooms = envRooms,
                devicesCount = envDevices.size,
                avgAqi = currentAqi,
                avgTemp = currentTemp,
                avgHumidity = currentHumidity,
                avgCo2 = currentCo2,
                status = AirQualityClassifier.classify(currentAqi)
            )
        }

        val overallAqi = if (summaries.isNotEmpty()) summaries.map { it.avgAqi }.average().toFloat() else 25.0f

        HomeUiState(
            totalDevices = devices.size,
            activeDevices = devices.count { it.deviceStatus == br.airsense.model.DeviceStatus.ACTIVE },
            environmentsSummary = summaries,
            overallStatus = AirQualityClassifier.classify(overallAqi)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    // Função auxiliar para gerar leituras simuladas e testar o painel/relatório
    fun simularLeituraTeste(environmentId: Int?, deviceId: String) {
        viewModelScope.launch {
            val reading = AirQualityReading(
                deviceId = deviceId,
                environmentId = environmentId,
                temperature = (19..28).random().toFloat(),
                humidity = (40..70).random().toFloat(),
                airQualityIndex = (15..120).random().toFloat(),
                co2Level = (400..1200).random().toFloat()
            )
            airQualityRepository.saveReading(reading)
        }
    }

    fun simularLeituraParaAmbiente(environmentId: Int) {
        viewModelScope.launch {
            // Sorteia aleatoriamente um perfil de qualidade do ar para garantir variedade total
            val aqiSimulado = (5..180).random().toFloat()
            val tempSimulada = (15..35).random().toFloat()
            val umidadeSimulada = (25..90).random().toFloat()
            val co2Simulado = (380..1600).random().toFloat()

            // Gera um ID de dispositivo aleatório para ver múltiplos dispositivos no histórico
            val deviceIdList = listOf("DEV-ALPHA-01", "DEV-BETA-02", "DEV-GAMMA-03", "DEV-DELTA-04")
            val randomDevice = deviceIdList.random()

            val reading = AirQualityReading(
                deviceId = randomDevice,
                environmentId = environmentId,
                temperature = tempSimulada,
                humidity = umidadeSimulada,
                airQualityIndex = aqiSimulado,
                co2Level = co2Simulado
            )
            airQualityRepository.saveReading(reading)
        }
    }
}