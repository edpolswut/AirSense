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
import br.airsense.util.ReportCsv
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Filtros do relatório. Ambiente, cômodo e dispositivo nulos = todos. */
data class ReportFilter(
    val startDate: LocalDate = LocalDate.now().minusDays(6),
    val endDate: LocalDate = LocalDate.now(),
    val environmentId: Int? = null,
    val roomId: Int? = null,
    val deviceId: Int? = null
)

/** Mínimo, média e máximo de uma métrica no período. */
data class MetricSummary(val min: Float, val avg: Float, val max: Float)

data class ReportRow(
    val reading: AirQualityReading,
    val deviceName: String,
    val outOfRange: Boolean
)

data class ReportResult(
    val filter: ReportFilter,
    val rows: List<ReportRow>,
    val aqi: MetricSummary,
    val temperature: MetricSummary,
    val humidity: MetricSummary,
    val co2: MetricSummary,
    val outOfRangeCount: Int
) {
    val status: AirQualityStatus get() = AirQualityClassifier.classify(aqi.avg)
}

data class ReportUiState(
    val environments: List<Environment> = emptyList(),
    val rooms: List<EnvironmentRoom> = emptyList(),
    val devices: List<AirSenseDevice> = emptyList(),
    /** Filtros sendo editados na tela. */
    val draft: ReportFilter = ReportFilter(),
    /** Filtros do último "Gerar relatório" válido. */
    val applied: ReportFilter? = null,
    val result: ReportResult? = null,
    val periodError: String? = null
) {
    /** Cômodos e dispositivos que fazem sentido para o que já foi escolhido. */
    val roomOptions: List<EnvironmentRoom>
        get() = rooms.filter { draft.environmentId == null || it.environmentId == draft.environmentId }

    val deviceOptions: List<AirSenseDevice>
        get() = devices.filter {
            (draft.environmentId == null || it.environmentId == draft.environmentId) &&
                (draft.roomId == null || it.roomId == draft.roomId)
        }

    /** Gerou, mas não há leituras: "Nenhum dado encontrado para o período selecionado." */
    val isEmptyResult: Boolean get() = applied != null && result == null
}

/** Relatório por período com filtros (US005) e exportação em CSV (US016). */
@HiltViewModel
class ReportViewModel @Inject constructor(
    airQualityRepository: AirQualityRepository,
    deviceRepository: AirSenseDeviceRepository,
    environmentRepository: EnvironmentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private data class Ui(
        val draft: ReportFilter = ReportFilter(),
        // Já abre com o relatório dos últimos 7 dias.
        val applied: ReportFilter? = ReportFilter(),
        val periodError: String? = null
    )

    private val _ui = MutableStateFlow(Ui())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> forUser(query: (String) -> Flow<List<T>>): Flow<List<T>> =
        sessionManager.userIdFlow.flatMapLatest { id -> if (id.isNullOrBlank()) flowOf(emptyList()) else query(id) }

    val uiState: StateFlow<ReportUiState> = combine(
        forUser(airQualityRepository::getReadingsForUser),
        forUser(deviceRepository::getDevicesForUser),
        forUser(environmentRepository::getEnvironmentsForUser),
        forUser(environmentRepository::getRoomsForUser),
        _ui
    ) { readings, devices, environments, rooms, ui ->
        ReportUiState(
            environments = environments,
            rooms = rooms,
            devices = devices,
            draft = ui.draft,
            applied = ui.applied,
            result = ui.applied?.let { buildResult(it, readings, devices) },
            periodError = ui.periodError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    fun onStartDateChange(date: LocalDate) = updateDraft { it.copy(startDate = date) }

    fun onEndDateChange(date: LocalDate) = updateDraft { it.copy(endDate = date) }

    /** Trocar o ambiente limpa cômodo e dispositivo, que podiam ser de outro ambiente. */
    fun onEnvironmentChange(id: Int?) = updateDraft { it.copy(environmentId = id, roomId = null, deviceId = null) }

    fun onRoomChange(id: Int?) = updateDraft { it.copy(roomId = id, deviceId = null) }

    fun onDeviceChange(id: Int?) = updateDraft { it.copy(deviceId = id) }

    fun onGenerateReport() {
        val draft = _ui.value.draft
        // US005 cenário 2
        if (draft.endDate.isBefore(draft.startDate)) {
            _ui.update { it.copy(periodError = "Período inválido.") }
            return
        }
        _ui.update { it.copy(applied = draft, periodError = null) }
    }

    /** Conteúdo do CSV do relatório na tela, ou null se não há relatório. */
    fun buildCsv(): String? {
        val state = uiState.value
        val result = state.result ?: return null
        return ReportCsv.build(result, state.environments, state.rooms, state.devices)
    }

    private fun updateDraft(change: (ReportFilter) -> ReportFilter) =
        _ui.update { it.copy(draft = change(it.draft), periodError = null) }

    private fun buildResult(
        filter: ReportFilter,
        readings: List<AirQualityReading>,
        devices: List<AirSenseDevice>
    ): ReportResult? {
        // O filtro usa a localização atual do dispositivo (não a gravada em cada leitura).
        val chosen = devices.filter {
            (filter.environmentId == null || it.environmentId == filter.environmentId) &&
                (filter.roomId == null || it.roomId == filter.roomId) &&
                (filter.deviceId == null || it.id == filter.deviceId)
        }.associateBy { it.deviceId.lowercase() }

        val zone = ZoneId.systemDefault()
        val from = filter.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = filter.endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val rows = readings
            .filter { it.timestamp in from until to }
            .mapNotNull { reading ->
                val device = chosen[reading.deviceId.lowercase()] ?: return@mapNotNull null
                ReportRow(reading, device.name, isOutOfRange(reading, device))
            }
            .sortedByDescending { it.reading.timestamp }
        if (rows.isEmpty()) return null

        fun summary(selector: (AirQualityReading) -> Float): MetricSummary {
            val values = rows.map { selector(it.reading) }
            return MetricSummary(values.min(), values.average().toFloat(), values.max())
        }
        return ReportResult(
            filter = filter,
            rows = rows,
            aqi = summary { it.airQualityIndex },
            temperature = summary { it.temperature },
            humidity = summary { it.humidity },
            co2 = summary { it.co2Level },
            outOfRangeCount = rows.count { it.outOfRange }
        )
    }

    /** Fora da faixa segura do dispositivo em qualquer métrica. */
    private fun isOutOfRange(r: AirQualityReading, d: AirSenseDevice): Boolean =
        r.temperature !in d.tempMinSafe..d.tempMaxSafe ||
            r.humidity !in d.humidityMinSafe..d.humidityMaxSafe ||
            r.airQualityIndex !in d.airQualityMinSafe..d.airQualityMaxSafe ||
            r.co2Level !in d.co2MinSafe..d.co2MaxSafe
}
