package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.ApiResponse
import com.renaissance.teacher.data.model.LoginRequest
import com.renaissance.teacher.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/status")
    suspend fun checkStatus(): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
