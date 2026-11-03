package br.airsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.airsense.model.AirSenseDevice
import br.airsense.util.AirQualitySnapshot
import br.airsense.util.AirQualityStatus
import kotlin.math.roundToInt

/** Selo do semáforo: cor + ícone + texto, para não depender só da cor (RNF011). */
@Composable
fun AirQualityStatusBadge(status: AirQualityStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(status.color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = status.icon,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Botão "i" que abre a legenda das cores (US010 cenário 2). */
@Composable
fun AirQualityLegendButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Outlined.Info, contentDescription = "Legenda das cores")
    }
    if (open) AirQualityLegendDialog(onDismiss = { open = false })
}

@Composable
fun AirQualityLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Legenda das cores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AirQualityStatus.entries.forEach { status ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AirQualityStatusBadge(status)
                        Spacer(Modifier.width(12.dp))
                        Text(status.aqiRange, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    text = "A cor usa a média da leitura mais recente de cada dispositivo ativo. " +
                        "Dispositivos desligados ou com defeito ficam fora da média.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

/** Linha com as médias atuais: AQI, temperatura, umidade e CO2 ("--" sem leitura). */
@Composable
fun AirQualityMetricsRow(snapshot: AirQualitySnapshot, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Metric("AQI", snapshot.aqi.orDash(""))
        Metric("Temp.", snapshot.temperature.orDash("°C"))
        Metric("Umidade", snapshot.humidity.orDash("%"))
        Metric("CO2", snapshot.co2.orDash(" ppm"))
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Valor arredondado com unidade, ou "--" quando ainda não há leitura. */
fun Float?.orDash(unit: String): String = this?.let { "${it.roundToInt()}$unit" } ?: "--"

/**
 * Seleção de vários dispositivos de uma vez (US008). [locationOf] diz onde cada um está
 * hoje, para o usuário saber que vai movê-lo.
 */
@Composable
fun AllocateDevicesDialog(
    title: String,
    devices: List<AirSenseDevice>,
    initiallySelected: Set<Int>,
    locationOf: (AirSenseDevice) -> String?,
    onConfirm: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(initiallySelected) { mutableStateOf(initiallySelected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (devices.isEmpty()) {
                Text("Nenhum dispositivo cadastrado. Cadastre dispositivos na aba Dispositivos primeiro.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(devices, key = { it.id }) { device ->
                        val checked = device.id in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = if (checked) selected - device.id else selected + device.id }
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                            Column {
                                Text(device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = listOfNotNull("ID: ${device.deviceId}", locationOf(device)).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }, enabled = devices.isNotEmpty()) {
                Text("Salvar (${selected.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Onde o dispositivo está hoje ("Casa / Sala", "Casa" ou "Sem ambiente"). */
fun deviceLocation(
    device: AirSenseDevice,
    environments: List<br.airsense.model.Environment>,
    rooms: List<br.airsense.model.EnvironmentRoom>
): String {
    val env = environments.firstOrNull { it.id == device.environmentId }?.name ?: return "Sem ambiente"
    val room = rooms.firstOrNull { it.id == device.roomId }?.name
    return if (room != null) "$env / $room" else env
}
