package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.repository.UserRepository
import br.airsense.data.session.SessionManager
import br.airsense.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    /** Lendo o DataStore / Room na abertura do app. */
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: User) : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<SessionState> = sessionManager.userIdFlow
        .flatMapLatest { userId -> observeSession(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading
        )

    private fun observeSession(userId: String?): Flow<SessionState> {
        if (userId == null) return flowOf(SessionState.LoggedOut)
        return repository.observeUser(userId).map { user ->
            if (user == null) {
                // Sessão aponta para um usuário que não existe mais (conta excluída, dados limpos).
                sessionManager.clearSession()
                SessionState.LoggedOut
            } else {
                SessionState.LoggedIn(user)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }
}