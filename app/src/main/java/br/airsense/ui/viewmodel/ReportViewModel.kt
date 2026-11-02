package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.model.AirQualityReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReportUiState(
    val readings: List<AirQualityReading> = emptyList(),
    val averageAqi: Float = 0f,
    val averageTemp: Float = 0f,
    val averageHumidity: Float = 0f,
    val averageCo2: Float = 0f
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    repository: AirQualityRepository
) : ViewModel() {

    val uiState: StateFlow<ReportUiState> = repository.getAllReadings()
        .map { readings ->
            val avgAqi = if (readings.isNotEmpty()) readings.map { it.airQualityIndex }.average().toFloat() else 0f
            val avgTemp = if (readings.isNotEmpty()) readings.map { it.temperature }.average().toFloat() else 0f
            val avgHumidity = if (readings.isNotEmpty()) readings.map { it.humidity }.average().toFloat() else 0f
            val avgCo2 = if (readings.isNotEmpty()) readings.map { it.co2Level }.average().toFloat() else 0f

            ReportUiState(
                readings = readings,
                averageAqi = avgAqi,
                averageTemp = avgTemp,
                averageHumidity = avgHumidity,
                averageCo2 = avgCo2
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReportUiState()
        )
}