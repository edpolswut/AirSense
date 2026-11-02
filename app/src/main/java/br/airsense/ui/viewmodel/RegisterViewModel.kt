package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.UserRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AuthError
import br.airsense.model.AuthResult
import br.airsense.util.UserValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun onRegister() {
        val state = _uiState.value
        if (state.isLoading) return

        val nameError = UserValidator.name(state.name)
        val emailError = UserValidator.email(state.email)
        val passwordError = UserValidator.password(state.password)
        val confirmError = UserValidator.confirmPassword(state.password, state.confirmPassword)
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.register(state.name, state.email, state.password)) {
                // Cadastro concluído já inicia a sessão e entra no app.
                is AuthResult.Success -> sessionManager.saveSession(result.data.id)
                is AuthResult.Failure -> _uiState.update {
                    if (result.error == AuthError.EMAIL_ALREADY_EXISTS) {
                        it.copy(emailError = result.error.message)
                    } else {
                        it.copy(errorMessage = result.error.message)
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}