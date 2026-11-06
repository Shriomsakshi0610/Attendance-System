package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.ApiResponse
import com.renaissance.teacher.data.model.AttendanceRecordItem
import com.renaissance.teacher.data.model.AttendanceSessionItem
import com.renaissance.teacher.data.model.CreateSessionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SessionApi {

    @POST("api/sessions/create")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ApiResponse<AttendanceSessionItem>>

    @GET("api/sessions/{id}")
    suspend fun getSessionById(@Path("id") id: String): Response<ApiResponse<AttendanceSessionItem>>

    @POST("api/sessions/{id}/end")
    suspend fun endSession(@Path("id") id: String): Response<ApiResponse<AttendanceSessionItem>>

    @GET("api/sessions/teacher")
    suspend fun getTeacherSessions(): Response<ApiResponse<List<AttendanceSessionItem>>>

    @GET("api/attendance/session/{id}/records")
    suspend fun getSessionRecords(@Path("id") id: String): Response<ApiResponse<List<AttendanceRecordItem>>>
}
