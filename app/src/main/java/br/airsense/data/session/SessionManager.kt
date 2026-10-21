package br.airsense.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Guarda apenas o id do usuário logado. Null significa "sem sessão". */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val userIdKey = stringPreferencesKey("user_id")

    val userIdFlow: Flow<String?> = context.sessionDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs[userIdKey] }

    suspend fun saveSession(userId: String) {
        context.sessionDataStore.edit { it[userIdKey] = userId }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.remove(userIdKey) }
    }
}