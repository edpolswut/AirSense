package br.airsense

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import br.airsense.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirSenseApp() {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route

    Scaffold(
        topBar = { TopAppBar(title = { Text("AirSense") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = rotaAtual == "Home",
                    onClick = { navController.navigate("Home") { launchSingleTop = true } },
                    icon = { Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Home Page",
                        tint = MaterialTheme.colorScheme.primary,
                    ) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "AirSenseDevices",
                    onClick = { navController.navigate("AirSenseDevices") { launchSingleTop = true } },
                    icon = { Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = "AirSenseDevices Page",
                        tint = MaterialTheme.colorScheme.primary,
                    ) },
                    label = { Text("Dispositivos") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "Environment",
                    onClick = { navController.navigate("Environment") { launchSingleTop = true } },
                    icon = { Icon(
                        imageVector = Icons.Outlined.House,
                        contentDescription = "Environment Page",
                        tint = MaterialTheme.colorScheme.primary,
                    ) },
                    label = { Text("Ambientes") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "Report",
                    onClick = { navController.navigate("Report") { launchSingleTop = true } },
                    icon = { Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = "Report Page",
                        tint = MaterialTheme.colorScheme.primary,
                    ) }
                    , label = { Text("Relatório") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "User",
                    onClick = { navController.navigate("User") { launchSingleTop = true } },
                    icon = { Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "User Page",
                        tint = MaterialTheme.colorScheme.primary,
                    ) },
                    label = { Text("Usuário") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "Home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("Home") { HomeScreen() }
            composable("AirSenseDevices") { AirSenseDevicesScreen() }
            composable("Environment") { EnvironmentScreen() }
            composable("Report") { ReportScreen() }
            composable("User") { UserScreen() }
        }
    }
}