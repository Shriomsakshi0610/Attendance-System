package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.ApiResponse
import com.renaissance.teacher.data.model.TimetableItem
import retrofit2.Response
import retrofit2.http.GET

interface TimetableApi {

    @GET("api/timetable/teacher")
    suspend fun getTeacherTimetable(): Response<ApiResponse<List<TimetableItem>>>
}
