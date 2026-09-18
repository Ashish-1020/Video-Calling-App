package com.aashu.natalks.data.remote

import com.aashu.natalks.data.remote.dto.AuthResponse
import com.aashu.natalks.data.remote.dto.IceServersResponseDto
import com.aashu.natalks.data.remote.dto.LoginRequest
import com.aashu.natalks.data.remote.dto.RegisterRequest
import com.aashu.natalks.data.remote.dto.UserSummaryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NaTalksApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("api/users")
    suspend fun listUsers(): List<UserSummaryDto>

    @GET("api/users/{userId}/online")
    suspend fun isOnline(@Path("userId") userId: String): Map<String, Boolean>

    @GET("api/ice-servers")
    suspend fun getIceServers(): IceServersResponseDto
}
