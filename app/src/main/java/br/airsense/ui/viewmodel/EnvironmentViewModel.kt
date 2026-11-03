package br.airsense.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirQualityRepository
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.Environment
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

data class EnvironmentDialogState(
    val isEnvironmentDialogOpen: Boolean = false,
    val editingEnvironment: Environment? = null,
    val envName: String = "",
    val envDescription: String = "",
    val envNameError: String? = null,
    /** Ambiente aguardando confirmação de exclusão (US002 cenário 3). */
    val environmentToDelete: Environment? = null,
    val toastMessage: String? = null
)

data class EnvironmentListItem(
    val environment: Environment,
    val roomsCount: Int,
    val devicesCount: Int,
    val snapshot: AirQualitySnapshot
)

data class EnvironmentUiState(
    val isLoading: Boolean = true,
    val environments: List<EnvironmentListItem> = emptyList(),
    val dialog: EnvironmentDialogState = EnvironmentDialogState()
) {
    /** Quantos cômodos o ambiente em exclusão tem, para o aviso. */
    val roomsOfEnvironmentToDelete: Int
        get() = environments.firstOrNull { it.environment.id == dialog.environmentToDelete?.id }?.roomsCount ?: 0
}

/** Tela macro de ambientes: lista, cadastro, edição e exclusão (US002). */
@HiltViewModel
class EnvironmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val environmentRepository: EnvironmentRepository,
    deviceRepository: AirSenseDeviceRepository,
    airQualityRepository: AirQualityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _dialogState = MutableStateFlow(EnvironmentDialogState())

    init {
        // Vindo do botão "Cadastrar ambiente" da Home: abre o formulário uma única vez.
        if (savedStateHandle.get<Boolean>("create") == true) {
            savedStateHandle["create"] = false
            onOpenCreateEnvironment()
        }
    }

    private val _currentUserId = sessionManager.userIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> forUser(query: (String) -> Flow<List<T>>): Flow<List<T>> =
        sessionManager.userIdFlow.flatMapLatest { id -> if (id.isNullOrBlank()) flowOf(emptyList()) else query(id) }

    private val _items: Flow<List<EnvironmentListItem>> = combine(
        forUser(environmentRepository::getEnvironmentsForUser),
        forUser(environmentRepository::getRoomsForUser),
        forUser(deviceRepository::getDevicesForUser),
        forUser(airQualityRepository::getReadingsForUser)
    ) { environments, rooms, devices, readings ->
        val latest = AirQualitySnapshot.latestByDevice(readings)
        environments.map { env ->
            val envDevices = devices.filter { it.environmentId == env.id }
            EnvironmentListItem(
                environment = env,
                roomsCount = rooms.count { it.environmentId == env.id },
                devicesCount = envDevices.size,
                snapshot = AirQualitySnapshot.of(envDevices, latest)
            )
        }
    }

    val uiState: StateFlow<EnvironmentUiState> = combine(_items, _dialogState) { items, dialog ->
        EnvironmentUiState(isLoading = false, environments = items, dialog = dialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EnvironmentUiState()
    )

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
        val userId = _currentUserId.value
        if (userId.isNullOrBlank()) return

        when {
            trimmedName.isEmpty() -> {
                _dialogState.update { it.copy(envNameError = "O nome do ambiente é obrigatório") }
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

    /** Primeiro pede confirmação; a exclusão só acontece em [onConfirmDeleteEnvironment]. */
    fun onRequestDeleteEnvironment(environment: Environment) {
        _dialogState.update { it.copy(environmentToDelete = environment) }
    }

    fun onConfirmDeleteEnvironment() {
        val environment = _dialogState.value.environmentToDelete ?: return
        _dialogState.update { it.copy(environmentToDelete = null) }
        viewModelScope.launch {
            environmentRepository.deleteEnvironment(environment)
            _dialogState.update { it.copy(toastMessage = "Ambiente removido") }
        }
    }

    fun onDismissDialogs() {
        _dialogState.update {
            it.copy(isEnvironmentDialogOpen = false, editingEnvironment = null, environmentToDelete = null)
        }
    }

    fun onToastShown() {
        _dialogState.update { it.copy(toastMessage = null) }
    }
}
