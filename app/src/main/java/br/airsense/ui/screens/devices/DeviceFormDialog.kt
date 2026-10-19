package br.airsense.ui.screens.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.airsense.model.DeviceStatus
import br.airsense.model.ReadingLimits
import br.airsense.ui.components.FullScreenDialog
import br.airsense.ui.components.RangeSliderCard
import br.airsense.ui.viewmodel.FormState

@Composable
fun DeviceFormDialog(
    isEditing: Boolean,
    form: FormState,
    onFormChange: (FormState) -> Unit,
    onGenerateRandomDeviceId: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    FullScreenDialog(
        title = if (isEditing) "Editar Dispositivo" else "Novo Dispositivo",
        onClose = onClose,
        onConfirm = onSave,
        confirmEnabled = form.name.isNotBlank(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Informações Básicas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = form.name,
                onValueChange = { onFormChange(form.copy(name = it)) },
                label = { Text("Nome do Dispositivo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.deviceId,
                onValueChange = { onFormChange(form.copy(deviceId = it)) },
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
                        selected = form.status == st,
                        onClick = { onFormChange(form.copy(status = st)) },
                        label = { Text(st.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RangeSliderCard(
                        title = "Temperatura (°C)",
                        valueRange = ReadingLimits.TEMP_BOUNDS,
                        selectedRange = form.tempRange,
                        unit = "°C",
                        onValueChange = { onFormChange(form.copy(tempRange = it)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    RangeSliderCard(
                        title = "Umidade (%)",
                        valueRange = ReadingLimits.HUMIDITY_BOUNDS,
                        selectedRange = form.humidityRange,
                        unit = "%",
                        onValueChange = { onFormChange(form.copy(humidityRange = it)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    RangeSliderCard(
                        title = "Qualidade do Ar (AQI)",
                        valueRange = ReadingLimits.AIR_QUALITY_BOUNDS,
                        selectedRange = form.airQualityRange,
                        unit = "AQI",
                        onValueChange = { onFormChange(form.copy(airQualityRange = it)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    RangeSliderCard(
                        title = "Nível de CO2 (ppm)",
                        valueRange = ReadingLimits.CO2_BOUNDS,
                        selectedRange = form.co2Range,
                        unit = "ppm",
                        onValueChange = { onFormChange(form.copy(co2Range = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
