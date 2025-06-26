package com.example.airscanner

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthRequest(val email: String, val password: String, val twoFactorCode: String,val twoFactorRecoveryCode: String)
data class AuthResponse(val tokenType: String,val accessToken: String,val expiresIn : Int, val refreshToken: String)

interface AuthService {
    @POST("register")
    suspend fun register(@Body request: AuthRequest): Response<Void>

    @POST("login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>
}
