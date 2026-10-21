package br.airsense.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Fluxo exibido enquanto não há sessão: login e cadastro, sem barras nem menu do app. */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Login") {
        composable("Login") {
            LoginScreen(onNavigateToRegister = { navController.navigate("Register") })
        }
        composable("Register") {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }
    }
}