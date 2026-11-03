package br.airsense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.airsense.ui.screens.devices.AirSenseDeviceScreen
import br.airsense.ui.screens.EnvironmentDetailScreen
import br.airsense.ui.screens.EnvironmentScreen
import br.airsense.ui.screens.RoomDetailScreen
import br.airsense.ui.screens.HomeScreen
import br.airsense.ui.screens.ReportScreen
import br.airsense.ui.screens.UserScreen
import br.airsense.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirSenseApp(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    // Telas de detalhe (ambiente e cômodo) mostram "voltar" e mantêm a aba Ambientes marcada.
    val isDetailRoute = rotaAtual == "Environment/{environmentId}" || rotaAtual == "Room/{roomId}"
    val isEnvironmentTab = rotaAtual?.startsWith("Environment") == true || rotaAtual == "Room/{roomId}"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showConfigDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "AirSense",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Monitoramento de Qualidade do Ar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showConfigDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Informações") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Sair") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AirSense") },
                    navigationIcon = {
                        if (isDetailRoute) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = rotaAtual == "Home",
                        onClick = { navController.navigateToTab("Home") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home Page",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        //label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = rotaAtual == "AirSenseDevices",
                        onClick = { navController.navigateToTab("AirSenseDevices") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = "AirSenseDevices Page",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        //label = { Text("Dispositivos") }
                    )
                    NavigationBarItem(
                        selected = isEnvironmentTab,
                        onClick = { navController.navigateToTab("Environment") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.House,
                                contentDescription = "Environment Page",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        //label = { Text("Ambientes") }
                    )
                    NavigationBarItem(
                        selected = rotaAtual == "Report",
                        onClick = { navController.navigateToTab("Report") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = "Report Page",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        //label = { Text("Relatório") }
                    )
                    NavigationBarItem(
                        selected = rotaAtual == "User",
                        onClick = { navController.navigateToTab("User") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "User Page",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        //label = { Text("Usuário") }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "Home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("Home") {
                    HomeScreen(
                        onOpenEnvironment = { id -> navController.navigate("Environment/$id") },
                        onCreateEnvironment = {
                            navController.navigate("Environment?create=true") { launchSingleTop = true }
                        }
                    )
                }
                composable("AirSenseDevices") { AirSenseDeviceScreen() }
                composable(
                    route = "Environment?create={create}",
                    arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false })
                ) {
                    EnvironmentScreen(onOpenEnvironment = { id -> navController.navigate("Environment/$id") })
                }
                composable(
                    route = "Environment/{environmentId}",
                    arguments = listOf(navArgument("environmentId") { type = NavType.IntType })
                ) {
                    EnvironmentDetailScreen(onOpenRoom = { id -> navController.navigate("Room/$id") })
                }
                composable(
                    route = "Room/{roomId}",
                    arguments = listOf(navArgument("roomId") { type = NavType.IntType })
                ) {
                    RoomDetailScreen(onDeleted = { navController.popBackStack() })
                }
                composable("Report") { ReportScreen() }
                composable("User") { UserScreen() }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showConfigDialog) {
        ThemeConfigDialog(
            currentTheme = themeMode,
            onThemeChange = onThemeModeChange,
            onDismiss = { showConfigDialog = false }
        )
    }
}

private val TEAM_MEMBERS = listOf(
    "Davi Cagnato Pinto",
    "Edgar Felipe Polswut",
    "Larissa Prescendo Gonçalves Adames",
    "Leonardo Siqueira Hanke",
    "Luis Felipe de Souza Quintiliano da Silva",
    "Nicolas Jacinto Grzebieniak de Oliveira"
)

/** "Informações" do menu: o que é o app, como as cores funcionam e a versão. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { br.airsense.ui.components.AirSenseLogo(modifier = Modifier.width(96.dp)) },
        title = { Text("Sobre o AirSense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Monitoramento da qualidade do ar em ambientes e cômodos, com sensores de temperatura, umidade, AQI e CO2.")
                Text(
                    "Criado para a Disciplina \"Mobile Development: Android\""
                )
                Text(
                    text = "Equipe",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TEAM_MEMBERS.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = "Versão $version • Modo offline (dados salvos neste aparelho)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun ThemeConfigDialog(
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember(currentTheme) {
        mutableFloatStateOf(
            when (currentTheme) {
                AppThemeMode.LIGHT -> 0f
                AppThemeMode.SYSTEM -> 1f
                AppThemeMode.DARK -> 2f
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tema do Aplicativo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Selecione o tema desejado usando o controle abaixo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        sliderPosition = newValue
                        val newTheme = when {
                            newValue < 0.5f -> AppThemeMode.LIGHT
                            newValue < 1.5f -> AppThemeMode.SYSTEM
                            else -> AppThemeMode.DARK
                        }
                        onThemeChange(newTheme)
                    },
                    valueRange = 0f..2f,
                    steps = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Claro",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sliderPosition < 0.5f) FontWeight.Bold else FontWeight.Normal,
                        color = if (sliderPosition < 0.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Padrão",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sliderPosition in 0.5f..<1.5f) FontWeight.Bold else FontWeight.Normal,
                        color = if (sliderPosition in 0.5f..<1.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Escuro",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sliderPosition >= 1.5f) FontWeight.Bold else FontWeight.Normal,
                        color = if (sliderPosition >= 1.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tema Selecionado: ${currentTheme.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Concluído")
            }
        }
    )
}

/** Troca de aba sem empilhar telas: limpa o histórico até a Home e abre a raiz da aba. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}
