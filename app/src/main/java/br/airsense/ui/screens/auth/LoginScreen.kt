package br.airsense.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.airsense.ui.components.AuthLayout
import br.airsense.ui.components.PasswordTextField
import br.airsense.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    AuthLayout(
        title = "Entrar",
        subtitle = "Acesse sua conta para continuar",
        modifier = modifier
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("E-mail") },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = { state.emailError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Senha",
            error = state.passwordError,
            imeAction = ImeAction.Done,
            onDone = {
                focusManager.clearFocus()
                viewModel.onLogin()
            }
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.onLogin()
            },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Entrar")
            }
        }

        TextButton(onClick = onNavigateToRegister) {
            Text("Não tem conta? Cadastre-se")
        }
    }
}