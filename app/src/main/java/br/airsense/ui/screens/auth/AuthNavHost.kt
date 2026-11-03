package br.airsense.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val REGISTERED_EMAIL_KEY = "registered_email"

/** Fluxo exibido enquanto não há sessão: login e cadastro, sem barras nem menu do app. */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Login") {
        composable("Login") { entry ->
            // E-mail recém-cadastrado, deixado pela tela de cadastro.
            val registeredEmail by entry.savedStateHandle
                .getStateFlow<String?>(REGISTERED_EMAIL_KEY, null)
                .collectAsState()
            LoginScreen(
                onNavigateToRegister = { navController.navigate("Register") },
                registeredEmail = registeredEmail,
                onRegisteredEmailShown = { entry.savedStateHandle[REGISTERED_EMAIL_KEY] = null }
            )
        }
        composable("Register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistered = { email ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(REGISTERED_EMAIL_KEY, email)
                    navController.popBackStack()
                }
            )
        }
    }
}