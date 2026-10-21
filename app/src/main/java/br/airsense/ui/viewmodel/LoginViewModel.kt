package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.UserRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.AuthResult
import br.airsense.util.UserValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun onLogin() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = UserValidator.email(state.email)
        val passwordError = if (state.password.isEmpty()) "Informe a senha" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.login(state.email, state.password)) {
                // Salvar a sessão faz o SessionViewModel trocar para o app principal.
                is AuthResult.Success -> sessionManager.saveSession(result.data.id)
                is AuthResult.Failure ->
                    _uiState.update { it.copy(errorMessage = result.error.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}