package com.aashu.natalks.data.repository

import com.aashu.natalks.data.local.Session
import com.aashu.natalks.data.local.SessionHolder
import com.aashu.natalks.data.local.TokenStore
import com.aashu.natalks.data.remote.NaTalksApi
import com.aashu.natalks.data.remote.dto.LoginRequest
import com.aashu.natalks.data.remote.dto.RegisterRequest

class AuthRepository(
    private val api: NaTalksApi,
    private val tokenStore: TokenStore,
    private val sessionHolder: SessionHolder
) {

    suspend fun restoreSession(): Session? {
        val saved = tokenStore.currentSession()
        sessionHolder.set(saved)
        return saved
    }

    suspend fun register(username: String, password: String): Session {
        val response = api.register(RegisterRequest(username, password))
        val session = Session(response.userId, response.username, response.token)
        tokenStore.save(session)
        sessionHolder.set(session)
        return session
    }

    suspend fun login(username: String, password: String): Session {
        val response = api.login(LoginRequest(username, password))
        val session = Session(response.userId, response.username, response.token)
        tokenStore.save(session)
        sessionHolder.set(session)
        return session
    }

    suspend fun logout() {
        tokenStore.clear()
        sessionHolder.set(null)
    }
}
