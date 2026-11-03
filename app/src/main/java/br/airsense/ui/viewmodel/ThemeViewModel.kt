package br.airsense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.airsense.data.settings.AppSettings
import br.airsense.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tema escolhido pelo usuário, salvo no aparelho. Null enquanto ainda está sendo lido. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settings: AppSettings
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode?> = settings.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
