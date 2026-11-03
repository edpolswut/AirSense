package br.airsense.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.airsense.ui.theme.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferências do aparelho, separadas da sessão: continuam valendo depois de sair da conta.
 * Por enquanto só o tema; o modo offline/online e os endereços dos servidores virão para cá.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeModeFlow: Flow<AppThemeMode> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            AppThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: AppThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }
}
