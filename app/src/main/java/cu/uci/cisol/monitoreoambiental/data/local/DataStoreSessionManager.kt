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
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    override val accessTokenFlow: Flow<String?> =
            context.dataStore.data.map { preferences -> preferences[ACCESS_TOKEN_KEY] }
            
    override val refreshTokenFlow: Flow<String?> =
            context.dataStore.data.map { preferences -> preferences[REFRESH_TOKEN_KEY] }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences -> 
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    override suspend fun clearSession() {
        context.dataStore.edit { preferences -> 
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }

    override suspend fun getAccessToken(): String? {
        return accessTokenFlow.first()
    }
    
    override suspend fun getRefreshToken(): String? {
        return refreshTokenFlow.first()
    }
}
