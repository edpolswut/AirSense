package br.airsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IndicadorCard("Tarefas", "3 de 8 concluídas", 0.38f)
        IndicadorCard("Hábitos", "1 de 3 concluídos", 0.33f)
        IndicadorCard("Saldo do mês", "R$ 1.250,00 de R$ 3.000,00", 0.42f)
    }
}

@Composable
private fun IndicadorCard(titulo: String, descricao: String, progresso: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(descricao, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { progresso },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${(progresso * 100).toInt()}%")
        }
    }
}