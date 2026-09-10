package com.example.airsense

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.airsense.ui.theme.AirSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirSenseTheme {
                AirSenseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirSenseApp() {

    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MyTracker") })
        },
        bottomBar = {
            NavigationBar {
//                NavigationBarItem(
//                    selected = telaAtual == "tarefas",
//                    onClick = { telaAtual = "tarefas" },
//                    icon = { Text("T") },
//                    label = { Text("Tarefas") }
//                )
//                NavigationBarItem(
//                    selected = telaAtual == "hábitos",
//                    onClick = { telaAtual = "hábitos" },
//                    icon = { Text("H") },
//                    label = { Text("Hábitos") }
//                )
            }
        }
    ) { innerPadding ->
//        when (telaAtual) {
//            "hábitos" -> HabitosScreen(
//                modifier = Modifier.padding(innerPadding)
//            )
//            else -> HomeScreen(
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen()
            }

            composable("habitos") {
                HabitosScreen()
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {

    var title by rememberSaveable { mutableStateOf("") }
    val tarefas = remember { mutableStateListOf<Tarefa>() }
    var proximoId by rememberSaveable { mutableIntStateOf(0) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title,
                style = MaterialTheme.typography.headlineMedium,
            )
            fun addTarefaEvent(){
                tarefas.add(
                    Tarefa(
                        id = proximoId,
                        titulo = title
                    )
                )
                title = ""
                proximoId += 1
            }
            OutlinedTextField(
                value = title,
                onValueChange = {
                    newTextValue -> title = newTextValue
                },
                label = { Text("Nova tarefa") },
                placeholder = { Text("Ex.: estudar Compose") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp) {
                        addTarefaEvent()
                        true
                    } else {
                        false
                    }
                }
            )
            Button(
                onClick = {
                    addTarefaEvent()
                },
                enabled = !title.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar")
            }
            if (tarefas.isEmpty()) {
                Text("Nenhuma tarefa cadastrada.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = tarefas,
                        key = { tarefa -> tarefa.id }
                    ) { tarefa ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = tarefa.concluida,
                                    onCheckedChange = {
                                            marcada ->
                                        val indice = tarefas.indexOfFirst {
                                            it.id == tarefa.id
                                        }

                                        if (indice >= 0) {
                                            tarefas[indice] = tarefa.copy(
                                                concluida = marcada
                                            )
                                        }
                                    }
                                )
                                Text(
                                    text = tarefa.titulo,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (tarefa.concluida) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                )
                                Spacer(Modifier.width(8.dp))

                                TextButton(
                                    onClick = { tarefas.remove(tarefa) }
                                ) {
                                    Text("Excluir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Tarefa(
    val id: Int,
    val titulo: String,
    var concluida: Boolean = false
)

@Composable
fun HabitosScreen(
    //onAbrirHabitos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habitos = remember {
        mutableStateListOf(
            Habito(1, "Beber água", sequencia = 4),
            Habito(2, "Ler por 20 minutos", sequencia = 2),
            Habito(3, "Caminhar", sequencia = 6)
        )
    }

    val concluidos = habitos.count { it.feitoHoje }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hábitos",
                style = MaterialTheme.typography.headlineMedium
            )
            Text("Progresso de hoje: $concluidos de ${habitos.size}")

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(habitos, key = { it.id }) { habito ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = habito.feitoHoje,
                                onCheckedChange = { marcada ->
                                    val indice = habitos.indexOfFirst {
                                        it.id == habito.id
                                    }
                                    if (indice >= 0) {
                                        habitos[indice] = habito.copy(
                                            feitoHoje = marcada
                                        )
                                    }
                                }
                            )
                            Column {
                                Text(habito.nome)
                                Text("Sequência: ${habito.sequencia} dias")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Habito(
    val id: Int,
    val nome: String,
    val feitoHoje: Boolean = false,
    val sequencia: Int = 0
)