package com.aashu.natalks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aashu.natalks.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object Success : AuthUiState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter a username and password")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.login(username, password)
                _uiState.value = AuthUiState.Success
            } catch (e: HttpException) {
                _uiState.value = AuthUiState.Error(
                    if (e.code() == 401) "Incorrect username or password" else "Login failed (${e.code()})"
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Couldn't reach the server. Check your connection.")
            }
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        if (username.length < 3) {
            _uiState.value = AuthUiState.Error("Username must be at least 3 characters")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords don't match")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.register(username, password)
                _uiState.value = AuthUiState.Success
            } catch (e: HttpException) {
                _uiState.value = AuthUiState.Error(
                    if (e.code() == 409) "That username is already taken" else "Registration failed (${e.code()})"
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Couldn't reach the server. Check your connection.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
