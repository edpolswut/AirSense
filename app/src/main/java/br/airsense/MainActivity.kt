package br.airsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.airsense.ui.screens.auth.AuthNavHost
import br.airsense.ui.theme.AirSenseTheme
import br.airsense.ui.theme.AppThemeMode
import br.airsense.ui.viewmodel.SessionState
import br.airsense.ui.viewmodel.SessionViewModel
import br.airsense.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Tema salvo no aparelho; enquanto é lido, segue o sistema (mostra só o carregando).
            val savedTheme by themeViewModel.themeMode.collectAsState()
            val themeMode = savedTheme ?: AppThemeMode.SYSTEM
            val sessionState by sessionViewModel.sessionState.collectAsState()

            AirSenseTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        savedTheme == null -> LoadingScreen()
                        sessionState == SessionState.Loading -> LoadingScreen()
                        sessionState == SessionState.LoggedOut -> AuthNavHost()
                        else -> AirSenseApp(
                            themeMode = themeMode,
                            onThemeModeChange = themeViewModel::setThemeMode,
                            onLogout = sessionViewModel::logout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}