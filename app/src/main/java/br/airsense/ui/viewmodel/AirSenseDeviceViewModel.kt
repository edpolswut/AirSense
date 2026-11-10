package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.data.repository.EnvironmentRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AirSenseDevice
import br.airsense.model.DeviceStatus
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
import java.util.UUID
import javax.inject.Inject

enum class SortOption(val label: String) {
    REGISTRATION_ORDER("Ordem de Cadastro"),
    NAME("Nome"),
    STATUS("Status"),
    ENVIRONMENT("Ambiente")
}

data class FormState(
    val name: String = "",
    val deviceId: String = "",
    val status: DeviceStatus = DeviceStatus.ACTIVE,
    val environmentId: Int? = null,
    val roomId: Int? = null,
    val nameError: String? = null,
    val deviceIdError: String? = null,
    val tempRange: ClosedFloatingPointRange<Float> = DEFAULT_TEMP_RANGE,
    val humidityRange: ClosedFloatingPointRange<Float> = DEFAULT_HUMIDITY_RANGE,
    val airQualityRange: ClosedFloatingPointRange<Float> = DEFAULT_AIR_QUALITY_RANGE,
    val co2Range: ClosedFloatingPointRange<Float> = DEFAULT_CO2_RANGE
) {
    companion object {
        val DEFAULT_TEMP_RANGE = 18.0f..26.0f
        val DEFAULT_HUMIDITY_RANGE = 30.0f..60.0f
        val DEFAULT_AIR_QUALITY_RANGE = 0.0f..100.0f
        val DEFAULT_CO2_RANGE = 400.0f..1000.0f

        fun default(randomDeviceId: String = ""): FormState = FormState(
            name = "",
            deviceId = randomDeviceId,
            status = DeviceStatus.ACTIVE,
            environmentId = null,
            roomId = null,
            tempRange = DEFAULT_TEMP_RANGE,
            humidityRange = DEFAULT_HUMIDITY_RANGE,
            airQualityRange = DEFAULT_AIR_QUALITY_RANGE,
            co2Range = DEFAULT_CO2_RANGE
        )
    }
}

data class UiOverlayState(
    val isAddOptionsModalOpen: Boolean = false,
    val isFullScreenPopupOpen: Boolean = false,
    val deviceBeingEdited: AirSenseDevice? = null,
    val form: FormState = FormState(),
    val toastMessage: String? = null
)

data class AirSenseDeviceUiState(
    val devices: List<AirSenseDevice> = emptyList(),
    val environments: List<Environment> = emptyList(),
    val rooms: List<EnvironmentRoom> = emptyList(),
    val selectedStatusFilter: DeviceStatus? = null,
    val selectedSortOption: SortOption = SortOption.REGISTRATION_ORDER,
    val isAddOptionsModalOpen: Boolean = false,
    val isFullScreenPopupOpen: Boolean = false,
    val deviceBeingEdited: AirSenseDevice? = null,
    val form: FormState = FormState(),
    val toastMessage: String? = null
)

@HiltViewModel
class AirSenseDeviceViewModel @Inject constructor(
    private val repository: AirSenseDeviceRepository,
    private val environmentRepository: EnvironmentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<DeviceStatus?>(null)
    private val _sortOption = MutableStateFlow(SortOption.REGISTRATION_ORDER)
    private val _overlayState = MutableStateFlow(UiOverlayState())

    private val _currentUserId = sessionManager.userIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userDevices: Flow<List<AirSenseDevice>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            repository.getDevicesForUser(userId)
        } else {
            repository.getDevices()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userEnvironments: Flow<List<Environment>> = sessionManager.userIdFlow.flatMapLatest { userId ->
        if (!userId.isNullOrBlank()) {
            environmentRepository.getEnvironmentsForUser(userId)
        } else {
            environmentRepository.getAllEnvironments()
        }
    }

    private val _filteredDevices: Flow<List<AirSenseDevice>> = combine(
        _userDevices,
        _statusFilter,
        _sortOption
    ) { devices, filter, sort ->
        devices
            .filter { filter == null || it.deviceStatus == filter }
            .sortedWith { d1, d2 ->
                when (sort) {
                    SortOption.REGISTRATION_ORDER -> d1.id.compareTo(d2.id)
                    SortOption.NAME -> d1.name.compareTo(d2.name, ignoreCase = true)
                    SortOption.STATUS -> d1.deviceStatus.compareTo(d2.deviceStatus)
                    SortOption.ENVIRONMENT -> {
                        val env1 = d1.environmentId ?: Int.MAX_VALUE
                        val env2 = d2.environmentId ?: Int.MAX_VALUE
                        env1.compareTo(env2)
                    }
                }
            }
    }

    val uiState: StateFlow<AirSenseDeviceUiState> = combine(
        _filteredDevices,
        _userEnvironments,
        environmentRepository.getAllRooms(),
        _statusFilter,
        combine(_sortOption, _overlayState) { s, o -> s to o }
    ) { devices, environments, rooms, filter, (sort, overlay) ->
        AirSenseDeviceUiState(
            devices = devices,
            environments = environments,
            rooms = rooms,
            selectedStatusFilter = filter,
            selectedSortOption = sort,
            isAddOptionsModalOpen = overlay.isAddOptionsModalOpen,
            isFullScreenPopupOpen = overlay.isFullScreenPopupOpen,
            deviceBeingEdited = overlay.deviceBeingEdited,
            form = overlay.form,
            toastMessage = overlay.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AirSenseDeviceUiState()
    )

    fun onStatusFilterSelected(status: DeviceStatus?) {
        _statusFilter.value = status
    }

    fun onSortOptionSelected(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun onOpenAddOptions() {
        _overlayState.update { it.copy(isAddOptionsModalOpen = true) }
    }

    fun onCloseAddOptions() {
        _overlayState.update { it.copy(isAddOptionsModalOpen = false) }
    }

    fun onQrCodeScanned(scannedCode: String) {
        val extractedCode = extractCodeFromQr(scannedCode)
        if (extractedCode.length == 6) {
            _overlayState.update {
                it.copy(
                    isAddOptionsModalOpen = false,
                    deviceBeingEdited = null,
                    form = FormState.default(randomDeviceId = extractedCode),
                    isFullScreenPopupOpen = true,
                    toastMessage = "Código QR lido com sucesso!"
                )
            }
        } else {
            _overlayState.update {
                it.copy(
                    isAddOptionsModalOpen = false,
                    toastMessage = "Código QR inválido. O ID deve ter exatamente 6 caracteres."
                )
            }
        }
    }

    private fun extractCodeFromQr(rawInput: String): String {
        val cleaned = rawInput.trim().replace("\r", "").replace("\n", "")
        if (cleaned.startsWith("http://", ignoreCase = true) ||
            cleaned.startsWith("https://", ignoreCase = true) ||
            cleaned.contains("/")
        ) {
            val uri = runCatching { android.net.Uri.parse(cleaned) }.getOrNull()
            val queryId = uri?.getQueryParameter("id") ?: uri?.getQueryParameter("code")
            if (!queryId.isNullOrBlank()) {
                return queryId.trim()
            }
            val lastSegment = uri?.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                return lastSegment.trim()
            }
        }
        return cleaned
    }

    fun onOpenManualRegister() {
        _overlayState.update {
            it.copy(
                isAddOptionsModalOpen = false,
                deviceBeingEdited = null,
                form = FormState.default(randomDeviceId = generateRandomDeviceId()),
                isFullScreenPopupOpen = true
            )
        }
    }

    fun onOpenEditDevice(device: AirSenseDevice) {
        _overlayState.update {
            it.copy(
                deviceBeingEdited = device,
                form = FormState(
                    name = device.name,
                    deviceId = device.deviceId,
                    status = device.deviceStatus,
                    environmentId = device.environmentId,
                    roomId = device.roomId,
                    tempRange = device.tempMinSafe..device.tempMaxSafe,
                    humidityRange = device.humidityMinSafe..device.humidityMaxSafe,
                    airQualityRange = device.airQualityMinSafe..device.airQualityMaxSafe,
                    co2Range = device.co2MinSafe..device.co2MaxSafe
                ),
                isFullScreenPopupOpen = true
            )
        }
    }

    fun onCloseFullScreenPopup() {
        _overlayState.update {
            it.copy(
                isFullScreenPopupOpen = false,
                deviceBeingEdited = null
            )
        }
    }

    fun onFormChange(newForm: FormState) {
        _overlayState.update {
            it.copy(
                form = newForm.copy(
                    nameError = if (newForm.name != it.form.name) null else newForm.nameError,
                    deviceIdError = if (newForm.deviceId != it.form.deviceId) null else newForm.deviceIdError
                )
            )
        }
    }

    fun onGenerateRandomDeviceId() {
        val newId = generateRandomDeviceId()
        _overlayState.update {
            it.copy(form = it.form.copy(deviceId = newId, deviceIdError = null))
        }
    }

    private fun generateRandomDeviceId(): String {
        return "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
    }

    fun onSaveDevice() {
        val form = _overlayState.value.form
        val name = form.name.trim()
        val editing = _overlayState.value.deviceBeingEdited
        val currentUserId = _currentUserId.value ?: ""

        if (name.isEmpty()) {
            _overlayState.update {
                it.copy(form = it.form.copy(nameError = "Informe o nome do dispositivo"))
            }
            return
        }

        val finalDeviceId = form.deviceId.trim().ifBlank { generateRandomDeviceId() }

        viewModelScope.launch {
            val existing = repository.findByDeviceId(currentUserId, finalDeviceId)
            if (existing != null && existing.id != editing?.id) {
                _overlayState.update {
                    it.copy(form = it.form.copy(deviceIdError = "Este ID de dispositivo já está cadastrado"))
                }
                return@launch
            }

            val deviceToSave = AirSenseDevice(
                id = editing?.id ?: 0,
                userId = editing?.userId?.ifBlank { currentUserId } ?: currentUserId,
                name = name,
                deviceId = finalDeviceId,
                status = form.status.code,
                environmentId = form.environmentId,
                roomId = if (form.environmentId != null) form.roomId else null,
                tempMinSafe = form.tempRange.start,
                tempMaxSafe = form.tempRange.endInclusive,
                humidityMinSafe = form.humidityRange.start,
                humidityMaxSafe = form.humidityRange.endInclusive,
                airQualityMinSafe = form.airQualityRange.start,
                airQualityMaxSafe = form.airQualityRange.endInclusive,
                co2MinSafe = form.co2Range.start,
                co2MaxSafe = form.co2Range.endInclusive
            )

            repository.saveDevice(deviceToSave)
            onCloseFullScreenPopup()
        }
    }

    fun onDeleteDevice(device: AirSenseDevice) {
        viewModelScope.launch {
            repository.deleteDevice(device)
        }
    }

    fun onToastMessageShown() {
        _overlayState.update { it.copy(toastMessage = null) }
    }
}