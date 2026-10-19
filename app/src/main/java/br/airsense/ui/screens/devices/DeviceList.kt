package br.airsense.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.airsense.model.AirSenseDevice
import br.airsense.ui.components.EmptyState

@Composable
fun DeviceList(
    devices: List<AirSenseDevice>,
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
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    onEdit = { onEditDevice(device) },
                    onDelete = { onDeleteDevice(device) }
                )
            }
        }
    }
}
