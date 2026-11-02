package br.airsense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.airsense.ui.components.OptionCard
import br.airsense.ui.components.PasswordTextField
import br.airsense.ui.viewmodel.ProfileDialog
import br.airsense.ui.viewmodel.ProfileUiState
import br.airsense.ui.viewmodel.ProfileViewModel

@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onToastMessageShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.user?.name?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = state.user?.name.orEmpty(),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = state.user?.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionCard(
                title = "Editar perfil",
                description = "Alterar nome e e-mail",
                icon = Icons.Default.Edit,
                onClick = viewModel::onOpenEditProfile
            )
            OptionCard(
                title = "Alterar senha",
                description = "Defina uma nova senha de acesso",
                icon = Icons.Default.Lock,
                onClick = viewModel::onOpenChangePassword
            )
            OptionCard(
                title = "Sair",
                description = "Encerrar a sessão neste aparelho",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = viewModel::onLogout
            )
            OptionCard(
                title = "Excluir conta",
                description = "Remove sua conta e seus dados de acesso",
                icon = Icons.Default.Delete,
                onClick = viewModel::onOpenDeleteAccount
            )
        }
    }

    when (state.dialog) {
        ProfileDialog.EDIT_PROFILE -> EditProfileDialog(
            state = state,
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onSave = viewModel::onSaveProfile,
            onDismiss = viewModel::onDismissDialog
        )
        ProfileDialog.CHANGE_PASSWORD -> ChangePasswordDialog(
            state = state,
            onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSave = viewModel::onSavePassword,
            onDismiss = viewModel::onDismissDialog
        )
        ProfileDialog.DELETE_ACCOUNT -> DeleteAccountDialog(
            isLoading = state.isLoading,
            onConfirm = viewModel::onConfirmDeleteAccount,
            onDismiss = viewModel::onDismissDialog
        )
        ProfileDialog.NONE -> Unit
    }
}

@Composable
private fun EditProfileDialog(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = state.nameError != null,
                    supportingText = { state.nameError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text("E-mail") },
                    singleLine = true,
                    isError = state.emailError != null,
                    supportingText = { state.emailError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.isLoading) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    state: ProfileUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar senha") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordTextField(
                    value = state.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = "Senha atual",
                    error = state.currentPasswordError
                )
                PasswordTextField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "Nova senha",
                    error = state.newPasswordError
                )
                PasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirmar nova senha",
                    error = state.confirmPasswordError,
                    imeAction = ImeAction.Done,
                    onDone = onSave
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.isLoading) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir conta") },
        text = {
            Text("Sua conta será removida deste aparelho e você voltará para a tela de login. Essa ação não pode ser desfeita.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Excluir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}