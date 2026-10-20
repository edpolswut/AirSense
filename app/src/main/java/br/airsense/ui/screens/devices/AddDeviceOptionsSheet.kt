package br.airsense.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.airsense.ui.components.OptionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceOptionsSheet(
    onDismiss: () -> Unit,
    onSelectQrCode: () -> Unit,
    onSelectManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cadastrar Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OptionCard(
                title = "Escanear QR Code",
                description = "Cadastre o dispositivo usando o código impresso nele",
                icon = Icons.Default.QrCodeScanner,
                onClick = onSelectQrCode
            )

            OptionCard(
                title = "Cadastrar Manualmente",
                description = "Informe o nome, ID e configure os limites de leitura",
                icon = Icons.Default.Edit,
                onClick = onSelectManual
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
