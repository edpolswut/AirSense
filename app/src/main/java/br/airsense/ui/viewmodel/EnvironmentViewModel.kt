package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnvironmentDialogState(
    val isEnvironmentDialogOpen: Boolean = false,
    val editingEnvironment: Environment? = null,
    val envName: String = "",
    val envDescription: String = "",
    val envNameError: String? = null,

    val isRoomDialogOpen: Boolean = false,
    val targetEnvironmentForRoom: Environment? = null,
    val editingRoom: EnvironmentRoom? = null,
    val roomName: String = "",
    val roomDescription: String = "",
    val roomNameError: String? = null,

    val isAllocateDevicesDialogOpen: Boolean = false,
    val targetEnvironmentForDevices: Environment? = null,
    val toastMessage: String? = null
)

data class EnvironmentUiState(
    val environments: List<Environment> = emptyList(),
    val roomsByEnvironment: Map<Int, List<EnvironmentRoom>> = emptyMap(),
    val devices: List<AirSenseDevice> = emptyList(),
    val dialog: EnvironmentDialogState = EnvironmentDialogState()
)

@HiltViewModel
class EnvironmentViewModel @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val deviceRepository: AirSenseDeviceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _dialogState = MutableStateFlow(EnvironmentDialogState())

    private val _currentUserId = sessionManager.userIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _environments: Flow<List<Environment>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            environmentRepository.getEnvironmentsForUser(userId)
        } else {
            environmentRepository.getAllEnvironments()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _devices: Flow<List<AirSenseDevice>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            deviceRepository.getDevicesForUser(userId)
        } else {
            deviceRepository.getDevices()
        }
    }

    val uiState: StateFlow<EnvironmentUiState> = combine(
        _environments,
        environmentRepository.getAllRooms(),
        _devices,
        _dialogState
    ) { environments, allRooms, devices, dialog ->
        EnvironmentUiState(
            environments = environments,
            roomsByEnvironment = allRooms.groupBy { it.environmentId },
            devices = devices,
            dialog = dialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EnvironmentUiState()
    )

    // ---------- Ambiente (US002 + US011) ----------

    fun onOpenCreateEnvironment() {
        _dialogState.update {
            it.copy(
                isEnvironmentDialogOpen = true,
                editingEnvironment = null,
                envName = "",
                envDescription = "",
                envNameError = null
            )
        }
    }

    fun onOpenEditEnvironment(environment: Environment) {
        _dialogState.update {
            it.copy(
                isEnvironmentDialogOpen = true,
                editingEnvironment = environment,
                envName = environment.name,
                envDescription = environment.description,
                envNameError = null
            )
        }
    }

    fun onEnvNameChange(value: String) {
        _dialogState.update { it.copy(envName = value, envNameError = null) }
    }

    fun onEnvDescriptionChange(value: String) {
        _dialogState.update { it.copy(envDescription = value) }
    }

    fun onSaveEnvironment() {
        val state = _dialogState.value
        val trimmedName = state.envName.trim()
        val userId = _currentUserId.value.orEmpty()

        when {
            trimmedName.isEmpty() -> {
                _dialogState.update { it.copy(envNameError = "Informe o nome do ambiente") }
                return
            }
            trimmedName.length < 2 -> {
                _dialogState.update { it.copy(envNameError = "O nome deve ter pelo menos 2 caracteres") }
                return
            }
        }

        viewModelScope.launch {
            val existing = environmentRepository.findEnvironmentByName(userId, trimmedName)
            val editing = state.editingEnvironment
            if (existing != null && existing.id != editing?.id) {
                _dialogState.update {
                    it.copy(envNameError = "Já existe um ambiente cadastrado com este nome")
                }
                return@launch
            }

            val environmentToSave = Environment(
                id = editing?.id ?: 0,
                userId = editing?.userId?.ifBlank { userId } ?: userId,
                name = trimmedName,
                description = state.envDescription.trim()
            )
            environmentRepository.saveEnvironment(environmentToSave)
            _dialogState.update {
                it.copy(
                    isEnvironmentDialogOpen = false,
                    editingEnvironment = null,
                    toastMessage = if (editing == null) "Ambiente criado com sucesso" else "Ambiente atualizado"
                )
            }
        }
    }

    fun onDeleteEnvironment(environment: Environment) {
        viewModelScope.launch {
            environmentRepository.deleteEnvironment(environment)
            _dialogState.update { it.copy(toastMessage = "Ambiente removido") }
        }
    }

    // ---------- Cômodo (US003 + US011) ----------

    fun onOpenCreateRoom(environment: Environment) {
        _dialogState.update {
            it.copy(
                isRoomDialogOpen = true,
                targetEnvironmentForRoom = environment,
                editingRoom = null,
                roomName = "",
                roomDescription = "",
                roomNameError = null
            )
        }
    }

    fun onOpenEditRoom(environment: Environment, room: EnvironmentRoom) {
        _dialogState.update {
            it.copy(
                isRoomDialogOpen = true,
                targetEnvironmentForRoom = environment,
                editingRoom = room,
                roomName = room.name,
                roomDescription = room.description,
                roomNameError = null
            )
        }
    }

    fun onRoomNameChange(value: String) {
        _dialogState.update { it.copy(roomName = value, roomNameError = null) }
    }

    fun onRoomDescriptionChange(value: String) {
        _dialogState.update { it.copy(roomDescription = value) }
    }

    fun onSaveRoom() {
        val state = _dialogState.value
        val targetEnv = state.targetEnvironmentForRoom ?: return
        val trimmedName = state.roomName.trim()

        if (trimmedName.isEmpty()) {
            _dialogState.update { it.copy(roomNameError = "Informe o nome do cômodo") }
            return
        }

        viewModelScope.launch {
            val existing = environmentRepository.findRoomByName(targetEnv.id, trimmedName)
            val editing = state.editingRoom
            if (existing != null && existing.id != editing?.id) {
                _dialogState.update {
                    it.copy(roomNameError = "Já existe um cômodo com este nome em ${targetEnv.name}")
                }
                return@launch
            }

            val roomToSave = EnvironmentRoom(
                id = editing?.id ?: 0,
                environmentId = targetEnv.id,
                name = trimmedName,
                description = state.roomDescription.trim()
            )
            environmentRepository.saveRoom(roomToSave)
            _dialogState.update {
                it.copy(
                    isRoomDialogOpen = false,
                    editingRoom = null,
                    targetEnvironmentForRoom = null,
                    toastMessage = if (editing == null) "Cômodo adicionado" else "Cômodo atualizado"
                )
            }
        }
    }

    fun onDeleteRoom(room: EnvironmentRoom) {
        viewModelScope.launch {
            environmentRepository.deleteRoom(room)
            _dialogState.update { it.copy(toastMessage = "Cômodo removido") }
        }
    }

    // ---------- Alocação de Dispositivos (US008) ----------

    fun onOpenAllocateDevices(environment: Environment) {
        _dialogState.update {
            it.copy(
                isAllocateDevicesDialogOpen = true,
                targetEnvironmentForDevices = environment
            )
        }
    }

    fun onToggleDeviceInEnvironment(device: AirSenseDevice, environment: Environment, roomId: Int? = null) {
        viewModelScope.launch {
            val isAlreadyInEnvironment = device.environmentId == environment.id
            val updated = if (isAlreadyInEnvironment && roomId == null) {
                device.copy(environmentId = null, roomId = null)
            } else {
                device.copy(environmentId = environment.id, roomId = roomId)
            }
            deviceRepository.saveDevice(updated)
        }
    }

    fun onDismissDialogs() {
        _dialogState.update {
            it.copy(
                isEnvironmentDialogOpen = false,
                editingEnvironment = null,
                isRoomDialogOpen = false,
                editingRoom = null,
                targetEnvironmentForRoom = null,
                isAllocateDevicesDialogOpen = false,
                targetEnvironmentForDevices = null
            )
        }
    }

    fun onToastShown() {
        _dialogState.update { it.copy(toastMessage = null) }
    }
}