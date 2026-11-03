package br.airsense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.airsense.model.AirQualityReading
import br.airsense.ui.components.EmptyState
import br.airsense.ui.components.LabeledValueChip
import br.airsense.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (state.readings.isEmpty()) {
            EmptyState(
                message = "Nenhum relatório de qualidade do ar registado.\nOs dados aparecerão assim que os dispositivos enviarem medições.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de Médias Consolidadas organizado em 2x2 para evitar quebras
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Média Geral dos Indicadores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LabeledValueChip(
                                        label = "AQI Médio",
                                        value = "${state.averageAqi.roundToInt()}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    LabeledValueChip(
                                        label = "Temp. Média",
                                        value = "${state.averageTemp.roundToInt()}°C",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LabeledValueChip(
                                        label = "Umidade",
                                        value = "${state.averageHumidity.roundToInt()}%",
                                        modifier = Modifier.weight(1f)
                                    )
                                    LabeledValueChip(
                                        label = "CO2",
                                        value = "${state.averageCo2.roundToInt()} ppm",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Histórico de Medições (${state.readings.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(state.readings, key = { it.id }) { reading ->
                    ReadingItemCard(reading = reading)
                }
            }
        }
    }
}

@Composable
private fun ReadingItemCard(reading: AirQualityReading) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateString = dateFormat.format(Date(reading.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dispositivo: ${reading.deviceId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chips organizados em 2x2 para o histórico individual
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledValueChip(
                        label = "AQI",
                        value = "${reading.airQualityIndex.roundToInt()}",
                        modifier = Modifier.weight(1f)
                    )
                    LabeledValueChip(
                        label = "Temp",
                        value = "${reading.temperature.roundToInt()}°C",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledValueChip(
                        label = "Umidade",
                        value = "${reading.humidity.roundToInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                    LabeledValueChip(
                        label = "CO2",
                        value = "${reading.co2Level.roundToInt()} ppm",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}