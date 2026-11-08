package com.renaissance.teacher.data.api

import com.renaissance.teacher.data.model.AppVersionInfo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface AppVersionApi {

    @GET("api/public/app/version")
    suspend fun getLatestVersion(): Response<AppVersionInfo>

    @Streaming
    @GET("api/public/app/download")
    suspend fun downloadApk(): Response<ResponseBody>

    @Streaming
    @GET
    suspend fun downloadApkFromUrl(@Url url: String): Response<ResponseBody>
}
