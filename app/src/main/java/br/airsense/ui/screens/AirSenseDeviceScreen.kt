package br.airsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import br.airsense.data.local.AirSenseDatabase
import br.airsense.data.repository.AirSenseDeviceRepository
import br.airsense.model.AirSenseDevice
import br.airsense.model.DeviceStatus
import br.airsense.model.ReadingLimits
import br.airsense.ui.viewmodel.AirSenseDeviceUiState
import br.airsense.ui.viewmodel.AirSenseDeviceViewModel
import br.airsense.ui.viewmodel.SortOption
import kotlin.math.roundToInt

@Composable
fun AirSenseDeviceScreen(
    modifier: Modifier = Modifier,
    viewModel: AirSenseDeviceViewModel = viewModel(
        factory = AirSenseDeviceViewModel.Factory(
            AirSenseDeviceRepository(
                AirSenseDatabase.getDatabase(LocalContext.current.applicationContext).airSenseDeviceDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onToastMessageShown()
        }
    }

    AirSenseDeviceContent(
        uiState = uiState,
        onStatusFilterSelected = viewModel::onStatusFilterSelected,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onOpenAddOptions = viewModel::onOpenAddOptions,
        onCloseAddOptions = viewModel::onCloseAddOptions,
        onSelectQrCodeOption = viewModel::onSelectQrCodeOption,
        onOpenManualRegister = viewModel::onOpenManualRegister,
        onOpenEditDevice = viewModel::onOpenEditDevice,
        onCloseFullScreenPopup = viewModel::onCloseFullScreenPopup,
        onFormNameChanged = viewModel::onFormNameChanged,
        onFormDeviceIdChanged = viewModel::onFormDeviceIdChanged,
        onFormStatusChanged = viewModel::onFormStatusChanged,
        onFormTempRangeChanged = viewModel::onFormTempRangeChanged,
        onFormHumidityRangeChanged = viewModel::onFormHumidityRangeChanged,
        onFormAirQualityRangeChanged = viewModel::onFormAirQualityRangeChanged,
        onFormCo2RangeChanged = viewModel::onFormCo2RangeChanged,
        onGenerateRandomDeviceId = viewModel::onGenerateRandomDeviceId,
        onSaveDevice = viewModel::onSaveDevice,
        onDeleteDevice = viewModel::onDeleteDevice,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirSenseDeviceContent(
    uiState: AirSenseDeviceUiState,
    onStatusFilterSelected: (DeviceStatus?) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onOpenAddOptions: () -> Unit,
    onCloseAddOptions: () -> Unit,
    onSelectQrCodeOption: () -> Unit,
    onOpenManualRegister: () -> Unit,
    onOpenEditDevice: (AirSenseDevice) -> Unit,
    onCloseFullScreenPopup: () -> Unit,
    onFormNameChanged: (String) -> Unit,
    onFormDeviceIdChanged: (String) -> Unit,
    onFormStatusChanged: (DeviceStatus) -> Unit,
    onFormTempRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onFormHumidityRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onFormAirQualityRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onFormCo2RangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onGenerateRandomDeviceId: () -> Unit,
    onSaveDevice: () -> Unit,
    onDeleteDevice: (AirSenseDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddOptions,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Dispositivo")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Linha de filtros por Status
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { onStatusFilterSelected(null) },
                        label = { Text("Todos") }
                    )
                }
                items(DeviceStatus.entries.toTypedArray()) { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { onStatusFilterSelected(status) },
                        label = { Text(status.label) }
                    )
                }
            }

            // Seleção de Ordenação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.devices.size} dispositivo(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedButton(
                        onClick = { sortDropdownExpanded = true }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ordenar: ${uiState.selectedSortOption.label}")
                    }

                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortOptionSelected(option)
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Lista de Dispositivos
            if (uiState.devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum dispositivo encontrado.\nClique no botão + para cadastrar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.devices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            onEdit = { onOpenEditDevice(device) },
                            onDelete = { onDeleteDevice(device) }
                        )
                    }
                }
            }
        }
    }

    // Modal de opções ao clicar no FAB
    if (uiState.isAddOptionsModalOpen) {
        AddDeviceOptionsBottomSheet(
            onDismiss = onCloseAddOptions,
            onSelectQrCode = onSelectQrCodeOption,
            onSelectManual = onOpenManualRegister
        )
    }

    // FullScreenPopup para cadastro/edição do dispositivo
    if (uiState.isFullScreenPopupOpen) {
        DeviceFullScreenPopup(
            isEditing = uiState.deviceBeingEdited != null,
            name = uiState.formName,
            deviceId = uiState.formDeviceId,
            status = uiState.formStatus,
            tempRange = uiState.formTempRange,
            humidityRange = uiState.formHumidityRange,
            airQualityRange = uiState.formAirQualityRange,
            co2Range = uiState.formCo2Range,
            onNameChange = onFormNameChanged,
            onDeviceIdChange = onFormDeviceIdChanged,
            onStatusChange = onFormStatusChanged,
            onTempRangeChange = onFormTempRangeChanged,
            onHumidityRangeChange = onFormHumidityRangeChanged,
            onAirQualityRangeChange = onFormAirQualityRangeChanged,
            onCo2RangeChange = onFormCo2RangeChanged,
            onGenerateRandomDeviceId = onGenerateRandomDeviceId,
            onClose = onCloseFullScreenPopup,
            onSave = onSaveDevice
        )
    }
}

@Composable
fun DeviceCard(
    device: AirSenseDevice,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${device.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = device.deviceStatus)

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Dispositivo", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Dispositivo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Informações do Ambiente/Sala
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ambiente / Sala: Não definido",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Resumo das Faixas de Leitura Segura
            Text(
                text = "Faixas de Leitura Segura:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricSummaryChip(
                    label = "Temp",
                    value = "${device.tempMinSafe.roundToInt()}°C - ${device.tempMaxSafe.roundToInt()}°C"
                )
                MetricSummaryChip(
                    label = "Umidade",
                    value = "${device.humidityMinSafe.roundToInt()}% - ${device.humidityMaxSafe.roundToInt()}%"
                )
                MetricSummaryChip(
                    label = "Qual. Ar",
                    value = "${device.airQualityMinSafe.roundToInt()} - ${device.airQualityMaxSafe.roundToInt()} AQI"
                )
                MetricSummaryChip(
                    label = "CO2",
                    value = "${device.co2MinSafe.roundToInt()} - ${device.co2MaxSafe.roundToInt()} ppm"
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val (bgColor, textColor) = when (status) {
        DeviceStatus.ACTIVE -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        DeviceStatus.OFF -> Color(0xFFECEFF1) to Color(0xFF546E7A)
        DeviceStatus.NOT_WORKING -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricSummaryChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceOptionsBottomSheet(
    onDismiss: () -> Unit,
    onSelectQrCode: () -> Unit,
    onSelectManual: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cadastrar Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Opção 1: QR Code
            Card(
                onClick = onSelectQrCode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Escanear QR Code",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cadastre o dispositivo usando o código impresso nele",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Opção 2: Manual
            Card(
                onClick = onSelectManual,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Cadastrar Manualmente",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Informe o nome, ID e configure os limites de leitura",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceFullScreenPopup(
    isEditing: Boolean,
    name: String,
    deviceId: String,
    status: DeviceStatus,
    tempRange: ClosedFloatingPointRange<Float>,
    humidityRange: ClosedFloatingPointRange<Float>,
    airQualityRange: ClosedFloatingPointRange<Float>,
    co2Range: ClosedFloatingPointRange<Float>,
    onNameChange: (String) -> Unit,
    onDeviceIdChange: (String) -> Unit,
    onStatusChange: (DeviceStatus) -> Unit,
    onTempRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onHumidityRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onAirQualityRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onCo2RangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onGenerateRandomDeviceId: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Garante que o popup ocupe a tela inteira
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(if (isEditing) "Editar Dispositivo" else "Novo Dispositivo")
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        },
                        actions = {
                            Button(
                                onClick = onSave,
                                enabled = name.isNotBlank(),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Salvar")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Seção 1: Informações Básicas
                    Text(
                        text = "Informações Básicas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Nome do Dispositivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = onDeviceIdChange,
                        label = { Text("ID do Dispositivo (DeviceId)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = onGenerateRandomDeviceId) {
                                Icon(Icons.Default.Refresh, contentDescription = "Gerar ID Aleatório")
                            }
                        },
                        supportingText = {
                            Text("Gerado automaticamente se deixado em branco")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Seletor de Status
                    Text(
                        text = "Status do Dispositivo:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeviceStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { onStatusChange(st) },
                                label = { Text(st.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Seção 2: Limites de Leitura Segura
                    Text(
                        text = "Limites de Leitura Segura",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ajuste a faixa (mínimo e máximo) considerada segura para cada indicador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Slider 1: Temperatura (°C)
                    RangeSliderItem(
                        title = "Temperatura (°C)",
                        valueRange = ReadingLimits.TEMP_BOUNDS,
                        selectedRange = tempRange,
                        unit = "°C",
                        onValueChange = onTempRangeChange
                    )

                    // Slider 2: Umidade (%)
                    RangeSliderItem(
                        title = "Umidade (%)",
                        valueRange = ReadingLimits.HUMIDITY_BOUNDS,
                        selectedRange = humidityRange,
                        unit = "%",
                        onValueChange = onHumidityRangeChange
                    )

                    // Slider 3: Qualidade do Ar (AQI)
                    RangeSliderItem(
                        title = "Qualidade do Ar (AQI)",
                        valueRange = ReadingLimits.AIR_QUALITY_BOUNDS,
                        selectedRange = airQualityRange,
                        unit = "AQI",
                        onValueChange = onAirQualityRangeChange
                    )

                    // Slider 4: Nível de CO2 (ppm)
                    RangeSliderItem(
                        title = "Nível de CO2 (ppm)",
                        valueRange = ReadingLimits.CO2_BOUNDS,
                        selectedRange = co2Range,
                        unit = "ppm",
                        onValueChange = onCo2RangeChange
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun RangeSliderItem(
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    selectedRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${selectedRange.start.roundToInt()} $unit — ${selectedRange.endInclusive.roundToInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            RangeSlider(
                value = selectedRange,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${valueRange.start.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Max: ${valueRange.endInclusive.roundToInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
