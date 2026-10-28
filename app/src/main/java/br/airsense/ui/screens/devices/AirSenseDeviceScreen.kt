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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun AirSenseDeviceScreen(
    modifier: Modifier = Modifier,
    viewModel: AirSenseDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                onEditDevice = viewModel::onOpenEditDevice,
                onDeleteDevice = viewModel::onDeleteDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    fun startQrScan() {
        viewModel.onCloseAddOptions()
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let { scannedValue ->
                    viewModel.onQrCodeScanned(scannedValue)
                }
            }
            .addOnFailureListener {
                // Ignore cancellation or failure
            }
    }

    if (uiState.isAddOptionsModalOpen) {
        AddDeviceOptionsSheet(
            onDismiss = viewModel::onCloseAddOptions,
            onSelectQrCode = { startQrScan() },
            onSelectManual = viewModel::onOpenManualRegister
        )
    }

    if (uiState.isFullScreenPopupOpen) {
        DeviceFormDialog(
            isEditing = uiState.deviceBeingEdited != null,
            form = uiState.form,
            onFormChange = viewModel::onFormChange,
            onGenerateRandomDeviceId = viewModel::onGenerateRandomDeviceId,
            onClose = viewModel::onCloseFullScreenPopup,
            onSave = viewModel::onSaveDevice
        )
    }
}
