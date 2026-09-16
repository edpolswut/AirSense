package br.airsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UserScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("◉", style = MaterialTheme.typography.displayLarge)
        Text("Paulo", style = MaterialTheme.typography.headlineSmall)
        Text("paulo@email.com", style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreferenciaCard("Notificações", "Ativadas")
            PreferenciaCard("Aparência", "Claro")
            PreferenciaCard("Privacidade", "Configurar")
            PreferenciaCard("Sobre o app", "Versão 1.0.0")
        }
    }
}

@Composable
private fun PreferenciaCard(titulo: String, descricao: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(descricao, style = MaterialTheme.typography.bodySmall)
            }
            Text(">")
        }
    }
}