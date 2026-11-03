package br.airsense.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.airsense.ui.components.AirQualityStatusBadge
import br.airsense.ui.components.orDash
import br.airsense.ui.viewmodel.MetricSummary
import br.airsense.ui.viewmodel.ReportResult
import br.airsense.ui.viewmodel.ReportRow
import br.airsense.ui.viewmodel.ReportUiState
import br.airsense.ui.viewmodel.ReportViewModel
import br.airsense.util.AirQualityStatus
import br.airsense.util.ReportCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Relatório por período com filtros (US005) e exportação em CSV (US016). */
@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // O usuário escolhe onde salvar (ex.: Downloads); o arquivo é escrito ali.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = viewModel.buildCsv()
        if (uri == null || csv == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) "Relatório exportado." else "Não foi possível salvar o arquivo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FiltersCard(
                state = state,
                onStartDate = viewModel::onStartDateChange,
                onEndDate = viewModel::onEndDateChange,
                onEnvironment = viewModel::onEnvironmentChange,
                onRoom = viewModel::onRoomChange,
                onDevice = viewModel::onDeviceChange,
                onGenerate = viewModel::onGenerateReport
            )
        }

        val result = state.result
        when {
            state.isEmptyResult -> item {
                // US005 cenário 3
                Text(
                    text = "Nenhum dado encontrado para o período selecionado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            result != null -> {
                item { SummaryCard(result) }
                item {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(ReportCsv.fileName(result)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar CSV")
                    }
                }
                item {
                    Text(
                        text = "Leituras (${result.rows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(result.rows, key = { it.reading.id }) { ReadingRow(it) }
            }
        }
    }
}

@Composable
private fun FiltersCard(
    state: ReportUiState,
    onStartDate: (LocalDate) -> Unit,
    onEndDate: (LocalDate) -> Unit,
    onEnvironment: (Int?) -> Unit,
    onRoom: (Int?) -> Unit,
    onDevice: (Int?) -> Unit,
    onGenerate: () -> Unit
) {
    val draft = state.draft
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Relatório", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Data inicial", draft.startDate, onStartDate, Modifier.weight(1f))
                DateField("Data final", draft.endDate, onEndDate, Modifier.weight(1f))
            }
            SelectField(
                label = "Ambiente",
                selectedId = draft.environmentId,
                options = state.environments.map { it.id to it.name },
                onSelect = onEnvironment
            )
            SelectField(
                label = "Cômodo",
                selectedId = draft.roomId,
                options = state.roomOptions.map { it.id to it.name },
                onSelect = onRoom
            )
            SelectField(
                label = "Dispositivo",
                selectedId = draft.deviceId,
                options = state.deviceOptions.map { it.id to it.name },
                onSelect = onDevice
            )
            state.periodError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) { Text("Gerar relatório") }
        }
    }
}

@Composable
private fun SummaryCard(result: ReportResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${result.filter.startDate.format(dateFormat)} a ${result.filter.endDate.format(dateFormat)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${result.rows.size} leitura(s) • ${result.outOfRangeCount} fora da faixa segura",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AirQualityStatusBadge(result.status)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1.4f))
                listOf("Mín.", "Média", "Máx.").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            }
            MetricLine("AQI", result.aqi, "")
            MetricLine("Temperatura", result.temperature, "°C")
            MetricLine("Umidade", result.humidity, "%")
            MetricLine("CO2", result.co2, " ppm")
        }
    }
}

@Composable
private fun MetricLine(name: String, m: MetricSummary, unit: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
        listOf(m.min, m.avg, m.max).forEach {
            Text(it.orDash(unit), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadingRow(row: ReportRow) {
    val r = row.reading
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.deviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = Instant.ofEpochMilli(r.timestamp).atZone(ZoneId.systemDefault()).format(dateTimeFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "AQI ${r.airQualityIndex.orDash("")} • ${r.temperature.orDash("°C")} • " +
                        "${r.humidity.orDash("%")} • ${r.co2Level.orDash(" ppm")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (row.outOfRange) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Fora da faixa segura",
                    tint = AirQualityStatus.BAD.color
                )
            }
        }
    }
}

/** Campo de data que abre o calendário. O DatePicker trabalha com meia-noite em UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ClickableField(
        label = label,
        value = date.format(dateFormat),
        trailing = { Icon(Icons.Default.DateRange, contentDescription = null) },
        onClick = { open = true },
        modifier = modifier
    )
    if (open) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Lista suspensa com a opção "Todos" (id nulo). */
@Composable
private fun SelectField(
    label: String,
    selectedId: Int?,
    options: List<Pair<Int, String>>,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ClickableField(
            label = label,
            value = options.firstOrNull { it.first == selectedId }?.second ?: "Todos",
            trailing = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { onSelect(null); expanded = false })
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

/** Campo só de leitura que responde ao toque (o TextField sozinho engole o clique). */
@Composable
private fun ClickableField(
    label: String,
    value: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = trailing,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}
