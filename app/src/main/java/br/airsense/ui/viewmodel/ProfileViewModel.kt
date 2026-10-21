package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.UserRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AuthError
import br.airsense.model.AuthResult
import br.airsense.model.User
import br.airsense.util.UserValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileDialog { NONE, EDIT_PROFILE, CHANGE_PASSWORD, DELETE_ACCOUNT }

data class ProfileUiState(
    val user: User? = null,
    val dialog: ProfileDialog = ProfileDialog.NONE,
    val name: String = "",
    val email: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userIdFlow
                .flatMapLatest { id ->
                    if (id == null) flowOf<User?>(null) else repository.observeUser(id)
                }
                .collect { user -> _uiState.update { it.copy(user = user) } }
        }
    }

    // ---------- Abrir / fechar diálogos ----------

    fun onOpenEditProfile() {
        _uiState.update {
            it.copy(
                dialog = ProfileDialog.EDIT_PROFILE,
                name = it.user?.name.orEmpty(),
                email = it.user?.email.orEmpty(),
                nameError = null,
                emailError = null
            )
        }
    }

    fun onOpenChangePassword() {
        _uiState.update {
            it.copy(
                dialog = ProfileDialog.CHANGE_PASSWORD,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                currentPasswordError = null,
                newPasswordError = null,
                confirmPasswordError = null
            )
        }
    }

    fun onOpenDeleteAccount() {
        _uiState.update { it.copy(dialog = ProfileDialog.DELETE_ACCOUNT) }
    }

    fun onDismissDialog() {
        _uiState.update { it.closeDialog() }
    }

    private fun ProfileUiState.closeDialog() =
        ProfileUiState(user = user, toastMessage = toastMessage)

    // ---------- Campos ----------

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    // ---------- Ações ----------

    fun onSaveProfile() {
        val state = _uiState.value
        val userId = state.user?.id ?: return
        if (state.isLoading) return

        val nameError = UserValidator.name(state.name)
        val emailError = UserValidator.email(state.email)
        if (nameError != null || emailError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateProfile(userId, state.name, state.email)) {
                is AuthResult.Success ->
                    _uiState.update { it.closeDialog().copy(toastMessage = "Perfil atualizado") }
                is AuthResult.Failure -> _uiState.update {
                    if (result.error == AuthError.EMAIL_ALREADY_EXISTS) {
                        it.copy(isLoading = false, emailError = result.error.message)
                    } else {
                        it.copy(isLoading = false, toastMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun onSavePassword() {
        val state = _uiState.value
        val userId = state.user?.id ?: return
        if (state.isLoading) return

        val currentError = if (state.currentPassword.isEmpty()) "Informe a senha atual" else null
        val newError = UserValidator.password(state.newPassword)
        val confirmError = UserValidator.confirmPassword(state.newPassword, state.confirmPassword)
        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    currentPasswordError = currentError,
                    newPasswordError = newError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result =
                repository.changePassword(userId, state.currentPassword, state.newPassword)) {
                is AuthResult.Success ->
                    _uiState.update { it.closeDialog().copy(toastMessage = "Senha alterada") }
                is AuthResult.Failure -> _uiState.update {
                    if (result.error == AuthError.WRONG_CURRENT_PASSWORD) {
                        it.copy(isLoading = false, currentPasswordError = result.error.message)
                    } else {
                        it.copy(isLoading = false, toastMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun onConfirmDeleteAccount() {
        val state = _uiState.value
        val userId = state.user?.id ?: return
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.deleteAccount(userId)) {
                // Limpar a sessão leva o app de volta para a tela de login.
                is AuthResult.Success -> sessionManager.clearSession()
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, toastMessage = result.error.message)
                }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }

    fun onToastMessageShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}