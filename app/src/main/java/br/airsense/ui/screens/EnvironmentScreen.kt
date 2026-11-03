package br.airsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import br.airsense.model.Environment
import br.airsense.ui.components.AirQualityLegendButton
import br.airsense.ui.components.AirQualityMetricsRow
import br.airsense.ui.components.AirQualityStatusBadge
import br.airsense.ui.components.EmptyState
import br.airsense.ui.viewmodel.EnvironmentDialogState
import br.airsense.ui.viewmodel.EnvironmentListItem
import br.airsense.ui.viewmodel.EnvironmentViewModel

/** Tela macro: lista de ambientes (US002). Tocar num ambiente abre o detalhe. */
@Composable
fun EnvironmentScreen(
    onOpenEnvironment: (Int) -> Unit,
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
        if (!uiState.isLoading && uiState.environments.isEmpty()) {
            EmptyState(
                message = "Nenhum ambiente cadastrado.\nToque no botão + para cadastrar (ex.: Casa, Escritório).",
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
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Meus Ambientes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        AirQualityLegendButton()
                    }
                }
                items(uiState.environments, key = { it.environment.id }) { item ->
                    EnvironmentCard(
                        item = item,
                        onClick = { onOpenEnvironment(item.environment.id) },
                        onEdit = { viewModel.onOpenEditEnvironment(item.environment) },
                        onDelete = { viewModel.onRequestDeleteEnvironment(item.environment) }
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

    uiState.dialog.environmentToDelete?.let { env ->
        DeleteEnvironmentDialog(
            environment = env,
            roomsCount = uiState.roomsOfEnvironmentToDelete,
            onConfirm = viewModel::onConfirmDeleteEnvironment,
            onDismiss = viewModel::onDismissDialogs
        )
    }
}

@Composable
private fun EnvironmentCard(
    item: EnvironmentListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.environment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.environment.description.isNotBlank()) {
                        Text(
                            text = item.environment.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${item.roomsCount} cômodo(s) • ${item.devicesCount} dispositivo(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Ambiente", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir Ambiente", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirQualityStatusBadge(item.snapshot.status)
            }
            AirQualityMetricsRow(item.snapshot)
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
        confirmButton = { Button(onClick = onSave) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** US002 cenário 3: avisa sobre os cômodos vinculados e só exclui após confirmação. */
@Composable
private fun DeleteEnvironmentDialog(
    environment: Environment,
    roomsCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir \"${environment.name}\"?") },
        text = {
            Text(
                if (roomsCount > 0) {
                    "Este ambiente possui $roomsCount cômodo(s) vinculado(s), que também serão excluídos. " +
                        "Os dispositivos continuam cadastrados, mas ficam sem ambiente."
                } else {
                    "Os dispositivos deste ambiente continuam cadastrados, mas ficam sem ambiente."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Excluir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
