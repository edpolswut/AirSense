package br.airsense.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.ui.components.EmptyState

@Composable
fun DeviceList(
    devices: List<AirSenseDevice>,
    environments: List<Environment>,
    rooms: List<EnvironmentRoom>,
    onEditDevice: (AirSenseDevice) -> Unit,
    onDeleteDevice: (AirSenseDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        EmptyState(
            message = "Nenhum dispositivo encontrado.\nClique no botão + para cadastrar.",
            modifier = modifier
        )
    } else {
        val envMap = environments.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                val envName = device.environmentId?.let { envMap[it]?.name }
                val roomName = device.roomId?.let { roomMap[it]?.name }
                val locationLabel = when {
                    envName != null && roomName != null -> "$envName • $roomName"
                    envName != null -> envName
                    else -> "Não definido"
                }

                DeviceCard(
                    device = device,
                    locationLabel = locationLabel,
                    onEdit = { onEditDevice(device) },
                    onDelete = { onDeleteDevice(device) }
                )
            }
        }
    }
}