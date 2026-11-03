package br.airsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.airsense.model.ReadingLimits
import br.airsense.ui.components.AirQualityLegendButton
import br.airsense.ui.components.AirQualityMetricsRow
import br.airsense.ui.components.AirQualityStatusBadge
import br.airsense.ui.components.AllocateDevicesDialog
import br.airsense.ui.components.EmptyState
import br.airsense.ui.components.RangeSliderCard
import br.airsense.ui.components.StatusBadge
import br.airsense.ui.components.deviceLocation
import br.airsense.ui.components.orDash
import br.airsense.ui.viewmodel.RoomConfigForm
import br.airsense.ui.viewmodel.RoomDetailViewModel
import br.airsense.ui.viewmodel.RoomDeviceItem

/** Tela do cômodo: estado atual, dispositivos e configuração individual. */
@Composable
fun RoomDetailScreen(
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoomDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    val room = state.room
    if (room == null) {
        if (!state.isLoading && !state.isDeleted) {
            EmptyState("Cômodo não encontrado.", modifier = modifier.fillMaxSize())
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(room.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            state.environment?.let {
                                Text(it.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        AirQualityStatusBadge(state.snapshot.status)
                        AirQualityLegendButton()
                    }
                    AirQualityMetricsRow(state.snapshot)
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dispositivos (${state.devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = viewModel::onOpenAllocate) {
                    Icon(Icons.Default.Devices, contentDescription = null)
                    Text("Alocar", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        if (state.devices.isEmpty()) {
            item {
                Text(
                    text = "Nenhum dispositivo neste cômodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.devices, key = { it.device.id }) { RoomDeviceRow(it) }

        item { HorizontalDivider() }
        item {
            RoomConfigSection(
                form = state.form,
                onFormChange = viewModel::onFormChange,
                onSave = viewModel::onSaveConfig
            )
        }
        item {
            TextButton(
                onClick = viewModel::onRequestDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("Excluir cômodo", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    if (state.isAllocateOpen) {
        AllocateDevicesDialog(
            title = "Dispositivos em ${room.name}",
            devices = state.allDevices,
            initiallySelected = state.devices.map { it.device.id }.toSet(),
            locationOf = { deviceLocation(it, state.allEnvironments, state.allRooms) },
            onConfirm = viewModel::onConfirmAllocate,
            onDismiss = viewModel::onDismissAllocate
        )
    }

    if (state.isDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text("Excluir \"${room.name}\"?") },
            text = { Text("Os dispositivos deste cômodo continuam no ambiente, mas ficam sem cômodo.") },
            confirmButton = {
                Button(
                    onClick = viewModel::onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun RoomDeviceRow(item: RoomDeviceItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val reading = item.latest
                Text(
                    text = if (reading == null) "Sem leituras"
                    else "AQI ${reading.airQualityIndex.orDash("")} • ${reading.temperature.orDash("°C")} • " +
                        "${reading.humidity.orDash("%")} • ${reading.co2Level.orDash(" ppm")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val air = item.airStatus
            if (air != null) {
                AirQualityStatusBadge(air)
            } else {
                // Desligado ou com defeito: fora da média, mostra o estado do aparelho.
                val (bg, fg) = item.device.badgeColors
                StatusBadge(text = item.device.deviceStatus.label, backgroundColor = bg, contentColor = fg)
            }
        }
    }
}

@Composable
private fun RoomConfigSection(
    form: RoomConfigForm,
    onFormChange: (RoomConfigForm) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Configuração do cômodo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = form.name,
            onValueChange = { onFormChange(form.copy(name = it)) },
            label = { Text("Nome") },
            singleLine = true,
            isError = form.nameError != null,
            supportingText = { form.nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.area,
            onValueChange = { onFormChange(form.copy(area = it)) },
            label = { Text("Área (m²)") },
            singleLine = true,
            isError = form.areaError != null,
            supportingText = { form.areaError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Limites personalizados", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (form.useCustomLimits) "Usando as faixas abaixo" else "Usando os limites padrão do sistema",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = form.useCustomLimits,
                onCheckedChange = { onFormChange(form.copy(useCustomLimits = it)) }
            )
        }
        if (form.useCustomLimits) {
            RangeSliderCard(
                title = "Temperatura (°C)",
                valueRange = ReadingLimits.TEMP_BOUNDS,
                selectedRange = form.tempRange,
                unit = "°C",
                onValueChange = { onFormChange(form.copy(tempRange = it)) }
            )
            RangeSliderCard(
                title = "Umidade (%)",
                valueRange = ReadingLimits.HUMIDITY_BOUNDS,
                selectedRange = form.humidityRange,
                unit = "%",
                onValueChange = { onFormChange(form.copy(humidityRange = it)) }
            )
            RangeSliderCard(
                title = "Qualidade do Ar (AQI)",
                valueRange = ReadingLimits.AIR_QUALITY_BOUNDS,
                selectedRange = form.airQualityRange,
                unit = "AQI",
                onValueChange = { onFormChange(form.copy(airQualityRange = it)) }
            )
            RangeSliderCard(
                title = "Nível de CO2 (ppm)",
                valueRange = ReadingLimits.CO2_BOUNDS,
                selectedRange = form.co2Range,
                unit = "ppm",
                onValueChange = { onFormChange(form.copy(co2Range = it)) }
            )
        }
        form.limitsError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Salvar configuração") }
    }
}
