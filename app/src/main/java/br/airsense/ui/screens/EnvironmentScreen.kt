package br.airsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.airsense.model.AirSenseDevice
import br.airsense.model.Environment
import br.airsense.model.EnvironmentRoom
import br.airsense.ui.components.EmptyState
import br.airsense.ui.viewmodel.EnvironmentDialogState
import br.airsense.ui.viewmodel.EnvironmentViewModel

@Composable
fun EnvironmentScreen(
    modifier: Modifier = Modifier,
    viewModel: EnvironmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.dialog.toastMessage) {
        uiState.dialog.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenCreateEnvironment,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Ambiente")
            }
        }
    ) { innerPadding ->
        if (uiState.environments.isEmpty()) {
            EmptyState(
                message = "Nenhum ambiente configurado.\nToque no botão + para cadastrar (ex.: Casa, Escritório).",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.environments, key = { it.id }) { env ->
                    val rooms = uiState.roomsByEnvironment[env.id].orEmpty()
                    val envDevices = uiState.devices.filter { it.environmentId == env.id }

                    EnvironmentCard(
                        environment = env,
                        rooms = rooms,
                        devices = envDevices,
                        onEditEnvironment = { viewModel.onOpenEditEnvironment(env) },
                        onDeleteEnvironment = { viewModel.onDeleteEnvironment(env) },
                        onAddRoom = { viewModel.onOpenCreateRoom(env) },
                        onEditRoom = { room -> viewModel.onOpenEditRoom(env, room) },
                        onDeleteRoom = viewModel::onDeleteRoom,
                        onAllocateDevices = { viewModel.onOpenAllocateDevices(env) }
                    )
                }
            }
        }
    }

    if (uiState.dialog.isEnvironmentDialogOpen) {
        EnvironmentFormDialog(
            dialog = uiState.dialog,
            onNameChange = viewModel::onEnvNameChange,
            onDescriptionChange = viewModel::onEnvDescriptionChange,
            onSave = viewModel::onSaveEnvironment,
            onDismiss = viewModel::onDismissDialogs
        )
    }

    if (uiState.dialog.isRoomDialogOpen) {
        RoomFormDialog(
            dialog = uiState.dialog,
            onNameChange = viewModel::onRoomNameChange,
            onDescriptionChange = viewModel::onRoomDescriptionChange,
            onSave = viewModel::onSaveRoom,
            onDismiss = viewModel::onDismissDialogs
        )
    }

    if (uiState.dialog.isAllocateDevicesDialogOpen) {
        val targetEnv = uiState.dialog.targetEnvironmentForDevices
        if (targetEnv != null) {
            AllocateDevicesDialog(
                environment = targetEnv,
                rooms = uiState.roomsByEnvironment[targetEnv.id].orEmpty(),
                allDevices = uiState.devices,
                onToggleDevice = { device, roomId ->
                    viewModel.onToggleDeviceInEnvironment(device, targetEnv, roomId)
                },
                onDismiss = viewModel::onDismissDialogs
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnvironmentCard(
    environment: Environment,
    rooms: List<EnvironmentRoom>,
    devices: List<AirSenseDevice>,
    onEditEnvironment: () -> Unit,
    onDeleteEnvironment: () -> Unit,
    onAddRoom: () -> Unit,
    onEditRoom: (EnvironmentRoom) -> Unit,
    onDeleteRoom: (EnvironmentRoom) -> Unit,
    onAllocateDevices: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = environment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (environment.description.isNotBlank()) {
                        Text(
                            text = environment.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditEnvironment) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Ambiente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteEnvironment) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Ambiente",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Cômodos (${rooms.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (rooms.isEmpty()) {
                Text(
                    text = "Nenhum cômodo cadastrado neste ambiente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rooms.forEach { room ->
                        val roomDevicesCount = devices.count { it.roomId == room.id }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = room.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val sub = buildString {
                                        if (room.description.isNotBlank()) append("${room.description} • ")
                                        append("$roomDevicesCount disp.")
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onEditRoom(room) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Cômodo"
                                        )
                                    }
                                    IconButton(onClick = { onDeleteRoom(room) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir Cômodo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Dispositivos Alocados (${devices.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (devices.isEmpty()) {
                Text(
                    text = "Nenhum dispositivo alocado a este ambiente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    devices.forEach { device ->
                        val roomName = rooms.firstOrNull { it.id == device.roomId }?.name
                        val label = if (roomName != null) "${device.name} ($roomName)" else device.name
                        AssistChip(
                            onClick = onAllocateDevices,
                            label = { Text(label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddRoom,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MeetingRoom, contentDescription = null)
                    Spacer(Modifier.padding(2.dp))
                    Text("Novo Cômodo")
                }
                OutlinedButton(
                    onClick = onAllocateDevices,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Devices, contentDescription = null)
                    Spacer(Modifier.padding(2.dp))
                    Text("Dispositivos")
                }
            }
        }
    }
}

@Composable
private fun EnvironmentFormDialog(
    dialog: EnvironmentDialogState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (dialog.editingEnvironment == null) "Novo Ambiente" else "Editar Ambiente")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialog.envName,
                    onValueChange = onNameChange,
                    label = { Text("Nome do Ambiente (ex.: Casa, Escritório)") },
                    singleLine = true,
                    isError = dialog.envNameError != null,
                    supportingText = { dialog.envNameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialog.envDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun RoomFormDialog(
    dialog: EnvironmentDialogState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val envName = dialog.targetEnvironmentForRoom?.name.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (dialog.editingRoom == null) "Novo Cômodo em $envName" else "Editar Cômodo")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialog.roomName,
                    onValueChange = onNameChange,
                    label = { Text("Nome do Cômodo (ex.: Sala, Quarto)") },
                    singleLine = true,
                    isError = dialog.roomNameError != null,
                    supportingText = { dialog.roomNameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialog.roomDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllocateDevicesDialog(
    environment: Environment,
    rooms: List<EnvironmentRoom>,
    allDevices: List<AirSenseDevice>,
    onToggleDevice: (AirSenseDevice, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alocar Dispositivos • ${environment.name}") },
        text = {
            if (allDevices.isEmpty()) {
                Text("Nenhum dispositivo cadastrado. Cadastre dispositivos na aba Dispositivos primeiro.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(allDevices, key = { it.id }) { device ->
                        val isAllocatedHere = device.environmentId == environment.id
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isAllocatedHere,
                                    onCheckedChange = {
                                        onToggleDevice(device, null)
                                    }
                                )
                                Column {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "ID: ${device.deviceId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isAllocatedHere && rooms.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.padding(start = 36.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = device.roomId == null,
                                        onClick = { onToggleDevice(device, null) },
                                        label = { Text("Sem cômodo") }
                                    )
                                    rooms.forEach { room ->
                                        FilterChip(
                                            selected = device.roomId == room.id,
                                            onClick = { onToggleDevice(device, room.id) },
                                            label = { Text(room.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Concluído") }
        }
    )
}