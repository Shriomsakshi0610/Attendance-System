package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.ApiResponse
import com.renaissance.teacher.data.model.ClassItem
import com.renaissance.teacher.data.model.SubjectItem
import com.renaissance.teacher.data.model.TeacherProfile
import retrofit2.Response
import retrofit2.http.GET

interface TeacherApi {

    @GET("api/teacher/profile")
    suspend fun getProfile(): Response<ApiResponse<TeacherProfile>>

    @GET("api/teacher/classes")
    suspend fun getAssignedClasses(): Response<ApiResponse<List<ClassItem>>>

    @GET("api/teacher/subjects")
    suspend fun getAssignedSubjects(): Response<ApiResponse<List<SubjectItem>>>
}
