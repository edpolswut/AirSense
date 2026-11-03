package br.airsense.ui.screens.devices

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.airsense.model.AirSenseDevice
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.airsense.model.DeviceStatus
import br.airsense.ui.components.FilterChipRow
import br.airsense.ui.components.SortDropdown
import br.airsense.ui.viewmodel.AirSenseDeviceViewModel
import br.airsense.ui.viewmodel.SortOption

@Composable
fun AirSenseDeviceScreen(
    modifier: Modifier = Modifier,
    viewModel: AirSenseDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Dispositivo aguardando confirmação de exclusão (apaga também as leituras dele).
    var deviceToDelete by remember { mutableStateOf<AirSenseDevice?>(null) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.onToastMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onOpenAddOptions,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Dispositivo")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChipRow(
                options = DeviceStatus.entries,
                selected = uiState.selectedStatusFilter,
                label = { it.label },
                onSelected = viewModel::onStatusFilterSelected
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.devices.size} dispositivo(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SortDropdown(
                    options = SortOption.entries,
                    selected = uiState.selectedSortOption,
                    label = { it.label },
                    onSelected = viewModel::onSortOptionSelected
                )
            }

            DeviceList(
                devices = uiState.devices,
                environments = uiState.environments,
                rooms = uiState.rooms,
                onEditDevice = viewModel::onOpenEditDevice,
                onDeleteDevice = { deviceToDelete = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (uiState.isAddOptionsModalOpen) {
        AddDeviceOptionsSheet(
            onDismiss = viewModel::onCloseAddOptions,
            onSelectQrCode = viewModel::onSelectQrCodeOption,
            onSelectManual = viewModel::onOpenManualRegister
        )
    }

    if (uiState.isFullScreenPopupOpen) {
        DeviceFormDialog(
            isEditing = uiState.deviceBeingEdited != null,
            form = uiState.form,
            environments = uiState.environments,
            rooms = uiState.rooms,
            onFormChange = viewModel::onFormChange,
            onGenerateRandomDeviceId = viewModel::onGenerateRandomDeviceId,
            onClose = viewModel::onCloseFullScreenPopup,
            onSave = viewModel::onSaveDevice,
            // Só para dispositivo já salvo. No modo online esta opção não vai existir (D09).
            canGenerateHistory = uiState.deviceBeingEdited != null,
            isGeneratingHistory = uiState.isGeneratingHistory,
            onGenerateHistory = viewModel::onGenerateHistory
        )
    }

    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Excluir \"${device.name}\"?") },
            text = { Text("O dispositivo e todo o histórico de leituras dele serão apagados.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteDevice(device)
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deviceToDelete = null }) { Text("Cancelar") } }
        )
    }
}
