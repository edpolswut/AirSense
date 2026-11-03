package br.airsense.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.DeviceStatus
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.util.AirQualityClassifier
import br.airsense.util.AirQualitySnapshot
import br.airsense.util.AirQualityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Dispositivo do cômodo com o estado atual. */
data class RoomDeviceItem(
    val device: AirSenseDevice,
    val latest: AirQualityReading?
) {
    /** Desligado/com defeito mostra o status do aparelho; ativo mostra o semáforo da última leitura. */
    val airStatus: AirQualityStatus?
        get() = if (device.deviceStatus != DeviceStatus.ACTIVE) null
        else latest?.let { AirQualityClassifier.classify(it.airQualityIndex) } ?: AirQualityStatus.NO_DATA
}

data class RoomConfigForm(
    val name: String = "",
    val area: String = "",
    val useCustomLimits: Boolean = false,
    val tempRange: ClosedFloatingPointRange<Float> = 18f..26f,
    val humidityRange: ClosedFloatingPointRange<Float> = 30f..60f,
    val airQualityRange: ClosedFloatingPointRange<Float> = 0f..100f,
    val co2Range: ClosedFloatingPointRange<Float> = 400f..1000f,
    val nameError: String? = null,
    val areaError: String? = null,
    val limitsError: String? = null
) {
    companion object {
        fun from(room: EnvironmentRoom) = RoomConfigForm(
            name = room.name,
            area = if (room.areaM2 > 0) room.areaM2.toString().removeSuffix(".0") else "",
            useCustomLimits = room.useCustomLimits,
            tempRange = room.tempMinSafe..room.tempMaxSafe,
            humidityRange = room.humidityMinSafe..room.humidityMaxSafe,
            airQualityRange = room.airQualityMinSafe..room.airQualityMaxSafe,
            co2Range = room.co2MinSafe..room.co2MaxSafe
        )
    }
}

data class RoomDetailUiState(
    val isLoading: Boolean = true,
    val room: EnvironmentRoom? = null,
    val environment: Environment? = null,
    val snapshot: AirQualitySnapshot = AirQualitySnapshot(),
    val devices: List<RoomDeviceItem> = emptyList(),
    val allDevices: List<AirSenseDevice> = emptyList(),
    val allEnvironments: List<Environment> = emptyList(),
    val allRooms: List<EnvironmentRoom> = emptyList(),
    val form: RoomConfigForm = RoomConfigForm(),
    val isAllocateOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val isDeleted: Boolean = false,
    val toastMessage: String? = null
)

/** Tela do cômodo: estado, dispositivos e configuração individual (US003, US008, US015). */
@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val environmentRepository: EnvironmentRepository,
    private val deviceRepository: AirSenseDeviceRepository,
    airQualityRepository: AirQualityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val roomId: Int = checkNotNull(savedStateHandle["roomId"])

    private data class Ui(
        val form: RoomConfigForm = RoomConfigForm(),
        val isAllocateOpen: Boolean = false,
        val isDeleteConfirmOpen: Boolean = false,
        val isDeleted: Boolean = false,
        val toastMessage: String? = null
    )

    private val _ui = MutableStateFlow(Ui())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> forUser(query: (String) -> Flow<List<T>>): Flow<List<T>> =
        sessionManager.userIdFlow.flatMapLatest { id -> if (id.isNullOrBlank()) flowOf(emptyList()) else query(id) }

    private val _data = combine(
        forUser(environmentRepository::getEnvironmentsForUser),
        forUser(environmentRepository::getRoomsForUser),
        forUser(deviceRepository::getDevicesForUser),
        forUser(airQualityRepository::getReadingsForUser)
    ) { environments, rooms, devices, readings ->
        val latest = AirQualitySnapshot.latestByDevice(readings)
        // Só encontra cômodos de ambientes do usuário logado.
        val room = rooms.firstOrNull { it.id == roomId }
        val roomDevices = devices.filter { room != null && it.roomId == room.id }
        RoomDetailUiState(
            isLoading = false,
            room = room,
            environment = environments.firstOrNull { it.id == room?.environmentId },
            snapshot = AirQualitySnapshot.of(roomDevices, latest),
            devices = roomDevices.map { RoomDeviceItem(it, latest[it.deviceId.lowercase()]) },
            allDevices = devices,
            allEnvironments = environments,
            allRooms = rooms
        )
    }

    val uiState: StateFlow<RoomDetailUiState> = combine(_data, _ui) { data, ui ->
        data.copy(
            form = ui.form,
            isAllocateOpen = ui.isAllocateOpen,
            isDeleteConfirmOpen = ui.isDeleteConfirmOpen,
            isDeleted = ui.isDeleted,
            toastMessage = ui.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoomDetailUiState()
    )

    init {
        // Preenche o formulário uma vez com os dados salvos do cômodo.
        viewModelScope.launch {
            val room = environmentRepository.observeRoom(roomId).filterNotNull().first()
            _ui.update { it.copy(form = RoomConfigForm.from(room)) }
        }
    }

    fun onFormChange(form: RoomConfigForm) =
        _ui.update { it.copy(form = form.copy(nameError = null, areaError = null, limitsError = null)) }

    fun onSaveConfig() {
        val room = uiState.value.room ?: return
        val form = _ui.value.form
        val name = form.name.trim()
        val nameError = if (name.isEmpty()) "O nome do cômodo é obrigatório" else null
        val areaError = RoomForm.areaError(form.area)
        val ranges = listOf(form.tempRange, form.humidityRange, form.airQualityRange, form.co2Range)
        // US015 cenário 3
        val limitsError = if (form.useCustomLimits && ranges.any { it.start >= it.endInclusive })
            "O valor mínimo deve ser menor que o máximo." else null
        if (nameError != null || areaError != null || limitsError != null) {
            _ui.update {
                it.copy(form = form.copy(nameError = nameError, areaError = areaError, limitsError = limitsError))
            }
            return
        }
        viewModelScope.launch {
            val existing = environmentRepository.findRoomByName(room.environmentId, name)
            if (existing != null && existing.id != room.id) {
                _ui.update { it.copy(form = form.copy(nameError = "Já existe um cômodo com este nome neste ambiente")) }
                return@launch
            }
            environmentRepository.saveRoom(
                room.copy(
                    name = name,
                    areaM2 = RoomForm.parseArea(form.area) ?: room.areaM2,
                    useCustomLimits = form.useCustomLimits,
                    tempMinSafe = form.tempRange.start,
                    tempMaxSafe = form.tempRange.endInclusive,
                    humidityMinSafe = form.humidityRange.start,
                    humidityMaxSafe = form.humidityRange.endInclusive,
                    airQualityMinSafe = form.airQualityRange.start,
                    airQualityMaxSafe = form.airQualityRange.endInclusive,
                    co2MinSafe = form.co2Range.start,
                    co2MaxSafe = form.co2Range.endInclusive
                )
            )
            _ui.update { it.copy(toastMessage = "Configuração do cômodo salva") }
        }
    }

    // ---------- Alocação de vários dispositivos (US008) ----------

    fun onOpenAllocate() = _ui.update { it.copy(isAllocateOpen = true) }

    fun onDismissAllocate() = _ui.update { it.copy(isAllocateOpen = false) }

    /** Marcados vão para este cômodo (e o ambiente dele); desmarcados saem do cômodo e ficam no ambiente. */
    fun onConfirmAllocate(selectedIds: Set<Int>) {
        val state = uiState.value
        val room = state.room ?: return
        _ui.update { it.copy(isAllocateOpen = false) }
        viewModelScope.launch {
            state.allDevices.forEach { device ->
                val isHere = device.roomId == room.id
                val wanted = device.id in selectedIds
                when {
                    wanted && !isHere ->
                        deviceRepository.saveDevice(device.copy(environmentId = room.environmentId, roomId = room.id))
                    !wanted && isHere -> deviceRepository.saveDevice(device.copy(roomId = null))
                }
            }
            _ui.update { it.copy(toastMessage = "Dispositivos atualizados") }
        }
    }

    // ---------- Exclusão ----------

    fun onRequestDelete() = _ui.update { it.copy(isDeleteConfirmOpen = true) }

    fun onDismissDelete() = _ui.update { it.copy(isDeleteConfirmOpen = false) }

    fun onConfirmDelete() {
        val room = uiState.value.room ?: return
        _ui.update { it.copy(isDeleteConfirmOpen = false) }
        viewModelScope.launch {
            environmentRepository.deleteRoom(room)
            _ui.update { it.copy(isDeleted = true, toastMessage = "Cômodo removido") }
        }
    }

    fun onToastShown() = _ui.update { it.copy(toastMessage = null) }
}
