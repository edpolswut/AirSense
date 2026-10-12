package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.model.AirSenseDevice
import br.airsense.model.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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
    val tempRange: ClosedFloatingPointRange<Float> = 18.0f..26.0f,
    val humidityRange: ClosedFloatingPointRange<Float> = 30.0f..60.0f,
    val airQualityRange: ClosedFloatingPointRange<Float> = 0.0f..100.0f,
    val co2Range: ClosedFloatingPointRange<Float> = 400.0f..1000.0f
)

private data class PopupAndFormState(
    val isAddOptionsModalOpen: Boolean,
    val isFullScreenPopupOpen: Boolean,
    val deviceBeingEdited: AirSenseDevice?,
    val form: FormState,
    val toast: String?
)

data class AirSenseDeviceUiState(
    val devices: List<AirSenseDevice> = emptyList(),
    val selectedStatusFilter: DeviceStatus? = null,
    val selectedSortOption: SortOption = SortOption.REGISTRATION_ORDER,
    val isAddOptionsModalOpen: Boolean = false,
    val isFullScreenPopupOpen: Boolean = false,
    val deviceBeingEdited: AirSenseDevice? = null,

    val formName: String = "",
    val formDeviceId: String = "",
    val formStatus: DeviceStatus = DeviceStatus.ACTIVE,
    val formTempRange: ClosedFloatingPointRange<Float> = 18.0f..26.0f,
    val formHumidityRange: ClosedFloatingPointRange<Float> = 30.0f..60.0f,
    val formAirQualityRange: ClosedFloatingPointRange<Float> = 0.0f..100.0f,
    val formCo2Range: ClosedFloatingPointRange<Float> = 400.0f..1000.0f,

    val toastMessage: String? = null
)

class AirSenseDeviceViewModel(
    private val repository: AirSenseDeviceRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<DeviceStatus?>(null)
    private val _sortOption = MutableStateFlow(SortOption.REGISTRATION_ORDER)
    private val _isAddOptionsModalOpen = MutableStateFlow(false)
    private val _isFullScreenPopupOpen = MutableStateFlow(false)
    private val _deviceBeingEdited = MutableStateFlow<AirSenseDevice?>(null)
    private val _formState = MutableStateFlow(FormState())
    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AirSenseDeviceUiState> = combine(
        combine(
            repository.getDevices(),
            _statusFilter,
            _sortOption
        ) { devices, filter, sort ->
            val filteredAndSorted = devices
                .filter { filter == null || it.deviceStatus == filter }
                .sortedWith { d1, d2 ->
                    when (sort) {
                        SortOption.REGISTRATION_ORDER -> d1.id.compareTo(d2.id)
                        SortOption.NAME -> d1.name.compareTo(d2.name, ignoreCase = true)
                        SortOption.STATUS -> d1.status.compareTo(d2.status)
                        SortOption.ENVIRONMENT -> {
                            val env1 = d1.environmentId ?: Int.MAX_VALUE
                            val env2 = d2.environmentId ?: Int.MAX_VALUE
                            env1.compareTo(env2)
                        }
                    }
                }
            Triple(filteredAndSorted, filter, sort)
        },
        combine(
            _isAddOptionsModalOpen,
            _isFullScreenPopupOpen,
            _deviceBeingEdited,
            _formState,
            _toastMessage
        ) { isModalOpen, isPopupOpen, editingDevice, form, toast ->
            PopupAndFormState(isModalOpen, isPopupOpen, editingDevice, form, toast)
        }
    ) { (devices, filter, sort), popupState ->
        AirSenseDeviceUiState(
            devices = devices,
            selectedStatusFilter = filter,
            selectedSortOption = sort,
            isAddOptionsModalOpen = popupState.isAddOptionsModalOpen,
            isFullScreenPopupOpen = popupState.isFullScreenPopupOpen,
            deviceBeingEdited = popupState.deviceBeingEdited,
            formName = popupState.form.name,
            formDeviceId = popupState.form.deviceId,
            formStatus = popupState.form.status,
            formTempRange = popupState.form.tempRange,
            formHumidityRange = popupState.form.humidityRange,
            formAirQualityRange = popupState.form.airQualityRange,
            formCo2Range = popupState.form.co2Range,
            toastMessage = popupState.toast
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
        _isAddOptionsModalOpen.value = true
    }

    fun onCloseAddOptions() {
        _isAddOptionsModalOpen.value = false
    }

    fun onSelectQrCodeOption() {
        _isAddOptionsModalOpen.value = false
        _toastMessage.value = "Funcionalidade de QR Code em desenvolvimento."
    }

    fun onOpenManualRegister() {
        _isAddOptionsModalOpen.value = false
        _deviceBeingEdited.value = null
        _formState.value = FormState(
            name = "",
            deviceId = generateRandomDeviceId(),
            status = DeviceStatus.ACTIVE,
            tempRange = 18.0f..26.0f,
            humidityRange = 30.0f..60.0f,
            airQualityRange = 0.0f..100.0f,
            co2Range = 400.0f..1000.0f
        )
        _isFullScreenPopupOpen.value = true
    }

    fun onOpenEditDevice(device: AirSenseDevice) {
        _deviceBeingEdited.value = device
        _formState.value = FormState(
            name = device.name,
            deviceId = device.deviceId,
            status = device.deviceStatus,
            tempRange = device.tempMinSafe..device.tempMaxSafe,
            humidityRange = device.humidityMinSafe..device.humidityMaxSafe,
            airQualityRange = device.airQualityMinSafe..device.airQualityMaxSafe,
            co2Range = device.co2MinSafe..device.co2MaxSafe
        )
        _isFullScreenPopupOpen.value = true
    }

    fun onCloseFullScreenPopup() {
        _isFullScreenPopupOpen.value = false
        _deviceBeingEdited.value = null
    }

    fun onFormNameChanged(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun onFormDeviceIdChanged(deviceId: String) {
        _formState.update { it.copy(deviceId = deviceId) }
    }

    fun onFormStatusChanged(status: DeviceStatus) {
        _formState.update { it.copy(status = status) }
    }

    fun onFormTempRangeChanged(range: ClosedFloatingPointRange<Float>) {
        _formState.update { it.copy(tempRange = range) }
    }

    fun onFormHumidityRangeChanged(range: ClosedFloatingPointRange<Float>) {
        _formState.update { it.copy(humidityRange = range) }
    }

    fun onFormAirQualityRangeChanged(range: ClosedFloatingPointRange<Float>) {
        _formState.update { it.copy(airQualityRange = range) }
    }

    fun onFormCo2RangeChanged(range: ClosedFloatingPointRange<Float>) {
        _formState.update { it.copy(co2Range = range) }
    }

    fun onGenerateRandomDeviceId() {
        _formState.update { it.copy(deviceId = generateRandomDeviceId()) }
    }

    private fun generateRandomDeviceId(): String {
        return "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
    }

    fun onSaveDevice() {
        val form = _formState.value
        val name = form.name.trim()
        if (name.isBlank()) return

        val finalDeviceId = form.deviceId.trim().ifBlank { generateRandomDeviceId() }

        val editing = _deviceBeingEdited.value
        val deviceToSave = AirSenseDevice(
            id = editing?.id ?: 0,
            name = name,
            deviceId = finalDeviceId,
            status = form.status.code,
            environmentId = editing?.environmentId,
            roomId = editing?.roomId,
            tempMinSafe = form.tempRange.start,
            tempMaxSafe = form.tempRange.endInclusive,
            humidityMinSafe = form.humidityRange.start,
            humidityMaxSafe = form.humidityRange.endInclusive,
            airQualityMinSafe = form.airQualityRange.start,
            airQualityMaxSafe = form.airQualityRange.endInclusive,
            co2MinSafe = form.co2Range.start,
            co2MaxSafe = form.co2Range.endInclusive
        )

        viewModelScope.launch {
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
        _toastMessage.value = null
    }

    class Factory(
        private val repository: AirSenseDeviceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AirSenseDeviceViewModel::class.java)) {
                return AirSenseDeviceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
