package cu.uci.cisol.monitoreoambiental.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class DataStoreSessionManager(private val context: Context) : SessionRepository {

    companion object {
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    override val tokenFlow: Flow<String?> =
            context.dataStore.data.map { preferences -> preferences[JWT_TOKEN_KEY] }

    override suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences -> preferences[JWT_TOKEN_KEY] = token }
    }

    override suspend fun clearSession() {
        context.dataStore.edit { preferences -> preferences.remove(JWT_TOKEN_KEY) }
    }

    override suspend fun getToken(): String? {
        return tokenFlow.first()
    }
}
