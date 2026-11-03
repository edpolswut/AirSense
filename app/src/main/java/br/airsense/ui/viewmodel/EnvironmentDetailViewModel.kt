package br.airsense.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.util.AirQualitySnapshot
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomListItem(
    val room: EnvironmentRoom,
    val devicesCount: Int,
    val snapshot: AirQualitySnapshot
)

data class NewRoomForm(
    val isOpen: Boolean = false,
    val name: String = "",
    val area: String = "",
    val nameError: String? = null,
    val areaError: String? = null
)

data class EnvironmentDetailUiState(
    val isLoading: Boolean = true,
    val environment: Environment? = null,
    val snapshot: AirQualitySnapshot = AirQualitySnapshot(),
    val rooms: List<RoomListItem> = emptyList(),
    /** Dispositivos do ambiente que ainda não estão em nenhum cômodo. */
    val devicesWithoutRoom: List<AirSenseDevice> = emptyList(),
    val allDevices: List<AirSenseDevice> = emptyList(),
    val allEnvironments: List<Environment> = emptyList(),
    val allRooms: List<EnvironmentRoom> = emptyList(),
    val roomForm: NewRoomForm = NewRoomForm(),
    val isAllocateOpen: Boolean = false,
    val toastMessage: String? = null
)

/** Tela micro de um ambiente: estado, cômodos e dispositivos (US002, US003, US008). */
@HiltViewModel
class EnvironmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val environmentRepository: EnvironmentRepository,
    private val deviceRepository: AirSenseDeviceRepository,
    airQualityRepository: AirQualityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val environmentId: Int = checkNotNull(savedStateHandle["environmentId"])

    private data class Ui(
        val roomForm: NewRoomForm = NewRoomForm(),
        val isAllocateOpen: Boolean = false,
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
        val envDevices = devices.filter { it.environmentId == environmentId }
        EnvironmentDetailUiState(
            isLoading = false,
            // Só encontra ambientes do usuário logado.
            environment = environments.firstOrNull { it.id == environmentId },
            snapshot = AirQualitySnapshot.of(envDevices, latest),
            rooms = rooms.filter { it.environmentId == environmentId }.map { room ->
                val roomDevices = envDevices.filter { it.roomId == room.id }
                RoomListItem(room, roomDevices.size, AirQualitySnapshot.of(roomDevices, latest))
            },
            devicesWithoutRoom = envDevices.filter { it.roomId == null },
            allDevices = devices,
            allEnvironments = environments,
            allRooms = rooms
        )
    }

    val uiState: StateFlow<EnvironmentDetailUiState> = combine(_data, _ui) { data, ui ->
        data.copy(roomForm = ui.roomForm, isAllocateOpen = ui.isAllocateOpen, toastMessage = ui.toastMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EnvironmentDetailUiState()
    )

    // ---------- Novo cômodo (US003) ----------

    fun onOpenNewRoom() = _ui.update { it.copy(roomForm = NewRoomForm(isOpen = true)) }

    fun onRoomNameChange(value: String) =
        _ui.update { it.copy(roomForm = it.roomForm.copy(name = value, nameError = null)) }

    fun onRoomAreaChange(value: String) =
        _ui.update { it.copy(roomForm = it.roomForm.copy(area = value, areaError = null)) }

    fun onSaveNewRoom() {
        val form = _ui.value.roomForm
        val name = form.name.trim()
        val area = RoomForm.parseArea(form.area)
        val nameError = if (name.isEmpty()) "O nome do cômodo é obrigatório" else null
        val areaError = RoomForm.areaError(form.area)
        if (nameError != null || areaError != null) {
            _ui.update { it.copy(roomForm = form.copy(nameError = nameError, areaError = areaError)) }
            return
        }
        viewModelScope.launch {
            if (environmentRepository.findRoomByName(environmentId, name) != null) {
                _ui.update { it.copy(roomForm = form.copy(nameError = "Já existe um cômodo com este nome neste ambiente")) }
                return@launch
            }
            environmentRepository.saveRoom(
                EnvironmentRoom(environmentId = environmentId, name = name, areaM2 = area!!)
            )
            _ui.update { it.copy(roomForm = NewRoomForm(), toastMessage = "Cômodo adicionado") }
        }
    }

    fun onDismissNewRoom() = _ui.update { it.copy(roomForm = NewRoomForm()) }

    // ---------- Alocação de vários dispositivos (US008) ----------

    fun onOpenAllocate() = _ui.update { it.copy(isAllocateOpen = true) }

    fun onDismissAllocate() = _ui.update { it.copy(isAllocateOpen = false) }

    /** Marcados entram no ambiente (sem cômodo, se vierem de fora); desmarcados saem dele. */
    fun onConfirmAllocate(selectedIds: Set<Int>) {
        val devices = uiState.value.allDevices
        _ui.update { it.copy(isAllocateOpen = false) }
        viewModelScope.launch {
            devices.forEach { device ->
                val isHere = device.environmentId == environmentId
                val wanted = device.id in selectedIds
                when {
                    wanted && !isHere ->
                        deviceRepository.saveDevice(device.copy(environmentId = environmentId, roomId = null))
                    !wanted && isHere ->
                        deviceRepository.saveDevice(device.copy(environmentId = null, roomId = null))
                }
            }
            _ui.update { it.copy(toastMessage = "Dispositivos atualizados") }
        }
    }

    fun onToastShown() = _ui.update { it.copy(toastMessage = null) }
}

/** Regras do formulário de cômodo, usadas no cadastro e na configuração. */
object RoomForm {
    fun parseArea(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    /** Área em m² é obrigatória e maior que zero (igual ao backend). */
    fun areaError(text: String): String? {
        if (text.isBlank()) return "A área do cômodo é obrigatória"
        val value = parseArea(text) ?: return "Informe a área em m² (ex.: 12,5)"
        return if (value <= 0) "A área deve ser maior que zero" else null
    }
}
