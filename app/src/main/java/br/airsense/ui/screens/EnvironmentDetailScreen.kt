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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import br.airsense.ui.components.AirQualityLegendButton
import br.airsense.ui.components.AirQualityMetricsRow
import br.airsense.ui.components.AirQualityStatusBadge
import br.airsense.ui.components.AllocateDevicesDialog
import br.airsense.ui.components.EmptyState
import br.airsense.ui.components.deviceLocation
import br.airsense.ui.viewmodel.EnvironmentDetailViewModel
import br.airsense.ui.viewmodel.NewRoomForm
import br.airsense.ui.viewmodel.RoomListItem

/** Tela micro de um ambiente: estado atual, cômodos e dispositivos. */
@Composable
fun EnvironmentDetailScreen(
    onOpenRoom: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvironmentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    val environment = state.environment
    if (environment == null) {
        if (!state.isLoading) {
            EmptyState("Ambiente não encontrado.", modifier = modifier.fillMaxSize())
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = environment.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        AirQualityStatusBadge(state.snapshot.status)
                        AirQualityLegendButton()
                    }
                    if (environment.description.isNotBlank()) {
                        Text(
                            text = environment.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AirQualityMetricsRow(state.snapshot)
                    Text(
                        text = "Média de ${state.snapshot.devicesWithData} dispositivo(s) ativo(s) com leitura.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::onOpenNewRoom, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.MeetingRoom, contentDescription = null)
                    Text("Novo Cômodo", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = viewModel::onOpenAllocate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Devices, contentDescription = null)
                    Text("Dispositivos", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        item {
            Text(
                text = "Cômodos (${state.rooms.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (state.rooms.isEmpty()) {
            item {
                Text(
                    text = "Nenhum cômodo cadastrado neste ambiente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.rooms, key = { it.room.id }) { item ->
            RoomCard(item = item, onClick = { onOpenRoom(item.room.id) })
        }

        item {
            Text(
                text = "Dispositivos sem cômodo (${state.devicesWithoutRoom.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (state.devicesWithoutRoom.isEmpty()) {
            item {
                Text(
                    text = "Todos os dispositivos deste ambiente estão em cômodos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.devicesWithoutRoom, key = { "d${it.id}" }) { device ->
            Text("• ${device.name} (${device.deviceStatus.label})", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (state.roomForm.isOpen) {
        NewRoomDialog(
            form = state.roomForm,
            environmentName = environment.name,
            onNameChange = viewModel::onRoomNameChange,
            onAreaChange = viewModel::onRoomAreaChange,
            onSave = viewModel::onSaveNewRoom,
            onDismiss = viewModel::onDismissNewRoom
        )
    }

    if (state.isAllocateOpen) {
        AllocateDevicesDialog(
            title = "Dispositivos em ${environment.name}",
            devices = state.allDevices,
            initiallySelected = state.allDevices.filter { it.environmentId == environment.id }.map { it.id }.toSet(),
            locationOf = { deviceLocation(it, state.allEnvironments, state.allRooms) },
            onConfirm = viewModel::onConfirmAllocate,
            onDismiss = viewModel::onDismissAllocate
        )
    }
}

@Composable
private fun RoomCard(item: RoomListItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.room.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val area = if (item.room.areaM2 > 0) "${item.room.areaM2.toString().removeSuffix(".0")} m² • " else ""
                    Text(
                        text = "$area${item.devicesCount} dispositivo(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AirQualityStatusBadge(item.snapshot.status)
            }
            AirQualityMetricsRow(item.snapshot)
        }
    }
}

@Composable
private fun NewRoomDialog(
    form: NewRoomForm,
    environmentName: String,
    onNameChange: (String) -> Unit,
    onAreaChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cômodo em $environmentName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text("Nome do Cômodo (ex.: Sala, Quarto)") },
                    singleLine = true,
                    isError = form.nameError != null,
                    supportingText = { form.nameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.area,
                    onValueChange = onAreaChange,
                    label = { Text("Área (m²)") },
                    singleLine = true,
                    isError = form.areaError != null,
                    supportingText = { form.areaError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
