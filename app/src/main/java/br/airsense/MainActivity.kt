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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.airsense.ui.screens.auth.AuthNavHost
import br.airsense.ui.theme.AirSenseTheme
import br.airsense.ui.theme.AppThemeMode
import br.airsense.ui.viewmodel.SessionState
import br.airsense.ui.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
            val sessionState by sessionViewModel.sessionState.collectAsState()

            AirSenseTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (sessionState) {
                        SessionState.Loading -> LoadingScreen()
                        SessionState.LoggedOut -> AuthNavHost()
                        is SessionState.LoggedIn -> AirSenseApp(
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
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