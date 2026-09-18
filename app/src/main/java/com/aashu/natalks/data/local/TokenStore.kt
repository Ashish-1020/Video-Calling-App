package com.aashu.natalks.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "natalks_session")

data class Session(val userId: String, val username: String, val token: String)

class TokenStore(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val TOKEN = stringPreferencesKey("token")
    }

    val session = context.dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        val username = prefs[Keys.USERNAME]
        val token = prefs[Keys.TOKEN]
        if (userId != null && username != null && token != null) {
            Session(userId, username, token)
        } else {
            null
        }
    }

    suspend fun currentSession(): Session? = session.first()

    suspend fun save(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.TOKEN] = session.token
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
