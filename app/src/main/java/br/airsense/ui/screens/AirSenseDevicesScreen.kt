package br.airsense.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.airsense.data.local.AirSenseDatabase
import br.airsense.model.AirSenseDevice
import kotlinx.coroutines.launch

@Composable
fun AirSenseDevicesScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val dao = remember { AirSenseDatabase.getDatabase(context).airSenseDeviceDao() }
    val escopo = rememberCoroutineScope()
    var airSenseDevices by remember { mutableStateOf(emptyList<AirSenseDevice>()) }

    var deviceName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { airSenseDevices = dao.listar() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Nova tarefa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                escopo.launch {
                    dao.inserir(AirSenseDevice(name = deviceName.trim()))
                    deviceName = ""
                    airSenseDevices = dao.listar()
                }
            },
            enabled = deviceName.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) { Text("Adicionar") }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(airSenseDevices, key = { it.id }) { tarefa ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tarefa.name, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                escopo.launch {
                                    dao.excluir(tarefa)
                                    airSenseDevices = dao.listar()
                                }
                            }
                        ) { Text("Excluir") }
                    }
                }
            }
        }
    }
}

