package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.ApiResponse
import com.renaissance.teacher.data.model.AttendanceSummaryItem
import com.renaissance.teacher.data.model.DateWiseRecordItem
import com.renaissance.teacher.data.model.StudentProfileItem
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ReportApi {

    @GET("api/reports/attendance-summary")
    suspend fun getAttendanceSummary(
        @Query("classId") classId: String? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<List<AttendanceSummaryItem>>>

    @GET("api/reports/student-profile/{id}")
    suspend fun getStudentProfile(
        @Path("id") studentId: String
    ): Response<ApiResponse<StudentProfileItem>>

    @GET("api/reports/datewise")
    suspend fun getDateWiseAttendance(
        @Query("classId") classId: String? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<List<DateWiseRecordItem>>>

    @Streaming
    @GET("api/reports/export/summary-csv")
    suspend fun downloadSummaryCsv(
        @Query("classId") classId: String? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("api/reports/export/datewise-csv")
    suspend fun downloadDateWiseCsv(
        @Query("classId") classId: String? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("api/reports/export/student-csv/{id}")
    suspend fun downloadStudentCsv(
        @Path("id") studentId: String
    ): Response<ResponseBody>
}
