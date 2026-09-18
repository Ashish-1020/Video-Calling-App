package com.aashu.natalks.data.remote.dto

data class RegisterRequest(val username: String, val password: String)

data class LoginRequest(val username: String, val password: String)

data class AuthResponse(val userId: String, val username: String, val token: String)
